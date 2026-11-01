package othello.gamelogic;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.activations.Activation;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;

import org.deeplearning4j.nn.conf.inputs.InputType;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class TDLearningStrategy implements AIStrategy {

    private final double gamma;
    private final double alpha;
    private final MultiLayerNetwork CNN;
    private Random random;
    private boolean training;
    private double explorationRate = 0.1;
    private long updateCount;

    /**
     * Two-argument
     * @param gamma the gamma parameter value
     * @param alpha the alpha parameter value
     */
    public TDLearningStrategy(double gamma, double alpha) {
        this(gamma, alpha, 27L);
    }

    public TDLearningStrategy(double gamma, double alpha, long seed) {
        validateParameters(gamma, alpha);
        this.gamma = gamma;
        this.alpha = alpha;
        this.random = new Random(seed);
        this.CNN = theCNNModel(seed);
    }

    /**
     * Three-argument
     * @param model the model
     * @param gamma the gamma parameter value
     * @param alpha the alpha parameter value
     */
    public TDLearningStrategy(MultiLayerNetwork model, double gamma, double alpha) {
        validateParameters(gamma, alpha);
        this.gamma = gamma;
        this.alpha = alpha;
        this.CNN = Objects.requireNonNull(model, "model");
        this.CNN.setLearningRate(alpha);
        this.random = new Random(27L);
    }

    private static void validateParameters(double gamma, double alpha) {
        if (!Double.isFinite(gamma) || gamma < 0.0 || gamma > 1.0) {
            throw new IllegalArgumentException("gamma must be between 0 and 1");
        }
        if (!Double.isFinite(alpha) || alpha < 0.0) {
            throw new IllegalArgumentException("alpha must be nonnegative and finite");
        }
    }

    private MultiLayerNetwork theCNNModel(long seed) {
        int length = 8;
        int width = 8;
        int channels = 1;

        // Build up the configurations of the model
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(alpha))
                .convolutionMode(ConvolutionMode.Same)
                .list()
                // Use RELU activationi mostly
                .layer(0, new ConvolutionLayer.Builder(3, 3)
                        .nIn(channels)
                        .stride(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(3, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(4, new ConvolutionLayer.Builder(3, 3)
                        .stride(1, 1)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                .layer(5, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(6, new DenseLayer.Builder()
                        .nOut(256)
                        .activation(Activation.RELU)
                        .build())
                .layer(7, new DenseLayer.Builder()
                        .nOut(128)
                        .activation(Activation.RELU)
                        .build())
                // Use linear for the output layer
                .layer(8, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nOut(1)
                        .build())
                .setInputType(InputType.convolutional(length, width, channels))
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        return network;
    }

    /**
     * Decide the best move from future game board status
     * @param board the Othello game board
     * @param actingPlayer the computer player that is moving
     * @return the determined board position for the computer player
     */
    @Override
    public BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer) {
        // Obtain all available moves
        Map<BoardSpace, List<BoardSpace>> availableMoves = actingPlayer.getAvailableMoves(board);
        if (availableMoves == null || availableMoves.isEmpty()) {
            return null;
        }

        List<BoardSpace> choices = new ArrayList<>(availableMoves.keySet());
        choices.sort(Comparator.comparingInt(BoardSpace::getX).thenComparingInt(BoardSpace::getY));
        if (training && random.nextDouble() < explorationRate) {
            return choices.get(random.nextInt(choices.size()));
        }

        int rows = board.length;
        int columns = board[0].length;
        float[] data = new float[choices.size() * rows * columns];
        for (int index = 0; index < choices.size(); index++) {
            BoardSpace moveChoice = choices.get(index);
            BoardSpace[][] boardCopy = makeBoardCopy(board);
            move(boardCopy, actingPlayer, moveChoice, availableMoves.get(moveChoice));
            encode(boardCopy, actingPlayer, data, index * rows * columns);
        }

        // Score legal afterstates in one forward pass.
        try (INDArray inputs = Nd4j.create(data, new long[]{choices.size(), 1, rows, columns}, 'c');
             INDArray output = CNN.output(inputs, false)) {
            int best = 0;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (int index = 0; index < choices.size(); index++) {
                double value = requireFinite(output.getDouble(index), "predicted value");
                if (value > bestValue) {
                    bestValue = value;
                    best = index;
                }
            }
            return choices.get(best);
        }
    }

    /**
     * Make a copy of the board passed into the method
     * @param originalBoard the board that will be copied
     * @return the copied board
     */
    private BoardSpace[][] makeBoardCopy(BoardSpace[][] originalBoard) {
        int rows = originalBoard.length;
        int columns = originalBoard[0].length;
        BoardSpace[][] boardCopy = new BoardSpace[rows][columns];

        // Loop over the board
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                BoardSpace.SpaceType eachCopy = originalBoard[row][column].getType();
                boardCopy[row][column] = new BoardSpace(row, column, eachCopy);
            }
        }

        return boardCopy;
    }

    /**
     * Placing and flipping on a copy board
     * @param boardCopy the copy board
     * @param actingPlayer the specific player moving
     * @param potentials the determined location to place
     * @param startingPoints the origin locations
     */
    private void move(BoardSpace[][] boardCopy, Player actingPlayer, BoardSpace potentials,
                      List<BoardSpace> startingPoints) {
        boardCopy[potentials.getX()][potentials.getY()].setType(actingPlayer.getColor());
        for (BoardSpace startingPoint : startingPoints) {
            int rowChange = Integer.compare(potentials.getX(), startingPoint.getX());
            int columnChange = Integer.compare(potentials.getY(), startingPoint.getY());
            int thisRow = startingPoint.getX();
            int thisColumn = startingPoint.getY();

            while (true) {
                boardCopy[thisRow][thisColumn].setType(actingPlayer.getColor());
                if (thisRow == potentials.getX() && thisColumn == potentials.getY()) {
                    break;
                }

                thisRow += rowChange;
                thisColumn += columnChange;
            }
        }
    }

    /**
     * Run forward CNN and obtain predicted value for board state
     * @param board the standard game board
     * @param actingPlayer current player that is being evaluated
     * @return a single value that shows the player how valuable the board space is
     */
    public double evaluateBoard(BoardSpace[][] board, Player actingPlayer) {
        float[] data = new float[board.length * board[0].length];
        encode(board, actingPlayer, data, 0);
        try (INDArray inputs = Nd4j.create(data, new long[]{1, 1, board.length, board[0].length}, 'c');
             INDArray output = CNN.output(inputs, false)) {
            return requireFinite(output.getDouble(0), "predicted value");
        }
    }

    /**
     *
     * @param thisBoard the current board status
     * @param actingPlayer the player that is moving
     * @param reward terminal win/draw/loss return (+1/0/-1), or zero between afterstates
     * @param nextBoard the next board status
     */
    public double updateInternal(BoardSpace[][] thisBoard, Player actingPlayer,
                                  double reward, BoardSpace[][] nextBoard) {
        // Obtain this board's value
        double currentInternalValue = evaluateBoard(thisBoard, actingPlayer);
        double nextInternalValue = 0.0;
        if (nextBoard != null) {
            nextInternalValue = evaluateBoard(nextBoard, actingPlayer);
        }

        // Terminal returns are in [-1, 1]; bound bootstrapping to the same range.
        double learningTarget = Math.max(-1.0, Math.min(1.0,
                requireFinite(reward, "reward") + gamma * nextInternalValue));
        double learningError = learningTarget - currentInternalValue;
        int rows = thisBoard.length;
        int columns = thisBoard[0].length;
        float[] data = new float[rows * columns];
        encode(thisBoard, actingPlayer, data, 0);
        try (INDArray inputs = Nd4j.create(data, new long[]{1, 1, rows, columns}, 'c');
             INDArray labels = Nd4j.create(new float[]{(float) learningTarget}, new long[]{1, 1}, 'c')) {
            CNN.fit(inputs, labels);
            requireFinite(CNN.score(), "training loss");
            updateCount++;
        }
        return Math.abs(requireFinite(learningError, "TD error"));
    }

    private static void encode(BoardSpace[][] board, Player actingPlayer, float[] output, int offset) {
        BoardSpace.SpaceType opponent = (actingPlayer.getColor() == BoardSpace.SpaceType.BLACK)
                ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;
        for (int row = 0; row < board.length; row++) {
            for (int column = 0; column < board[row].length; column++) {
                BoardSpace.SpaceType thisType = board[row][column].getType();
                if (thisType == actingPlayer.getColor()) {
                    output[offset++] = 1.0f;
                } else if (thisType == opponent) {
                    output[offset++] = -1.0f;
                } else {
                    output[offset++] = 0.0f;
                }
            }
        }
    }

    private static double requireFinite(double value, String description) {
        if (!Double.isFinite(value)) {
            throw new IllegalStateException("Non-finite " + description);
        }
        return value;
    }

    public void setTraining(boolean training) {
        this.training = training;
    }

    public boolean isTraining() {
        return training;
    }

    public void setSeed(long seed) {
        random = new Random(seed);
    }

    public void setExplorationRate(double rate) {
        if (!Double.isFinite(rate) || rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("exploration rate must be between 0 and 1");
        }
        explorationRate = rate;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    /**
     *
     * @return the relevant CNN network
     */
    public MultiLayerNetwork getCNNModel() {
        return CNN;
    }
}
