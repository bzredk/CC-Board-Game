package othello.gamelogic;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestLearningTrain {
    @Rule
    public TemporaryFolder files = new TemporaryFolder(new File("target"));

    @Test
    public void savingWithoutModelExplainsMissingModel() throws Exception {
        String output = tapSystemOut(() -> new LearningTrain().saveTrainedModel("unused.zip"));
        assertTrue(output.contains("There is no model"));
    }

    @Test
    public void loadingMissingModelDoesNotCreateOne() throws Exception {
        LearningTrain trainer = new LearningTrain();
        File missing = new File(files.getRoot(), "missing.zip");
        String output = tapSystemOut(() -> trainer.loadTrainedModel(missing.getPath(), 0.99, 0.001));
        assertTrue(output.contains("cannot be found"));
        assertNull(strategyOf(trainer));
    }

    @Test
    public void boardCopyDoesNotShareSquares() throws Exception {
        BoardSpace[][] original = {{new BoardSpace(0, 0, BoardSpace.SpaceType.EMPTY)}};
        Method method = LearningTrain.class.getDeclaredMethod("getCopy", BoardSpace[][].class);
        method.setAccessible(true);
        BoardSpace[][] copy = (BoardSpace[][]) method.invoke(new LearningTrain(), (Object) original);
        assertNotSame(original, copy);
        assertNotSame(original[0][0], copy[0][0]);
        copy[0][0].setType(BoardSpace.SpaceType.BLACK);
        assertEquals(BoardSpace.SpaceType.EMPTY, original[0][0].getType());
    }

    @Test
    public void zeroTrainingGamesHasFiniteSummaryWithoutInitializingNetwork() throws Exception {
        LearningTrain trainer = new LearningTrain();
        String output = tapSystemOut(() -> trainer.repeatGame(0));
        assertTrue(output.contains("games=0 updates=0"));
        assertTrue(output.contains("finiteErrors=true"));
        assertFalse(output.contains("NaN"));
        assertNull(strategyOf(trainer));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeTrainingGamesAreRejected() {
        new LearningTrain().repeatGame(-1);
    }

    @Test
    public void trainingContinuesOnSameNetworkAcrossCalls() throws Exception {
        TDLearningStrategy strategy = tinyStrategy();
        LearningTrain trainer = trainerWith(strategy);
        INDArray before = strategy.getCNNModel().params().dup();
        tapSystemOut(() -> trainer.repeatGame(1));
        long firstUpdates = strategy.getUpdateCount();
        assertTrue(firstUpdates > 0);
        assertFalse(before.equalsWithEps(strategy.getCNNModel().params(), 1e-10));
        assertFalse(strategy.isTraining());

        tapSystemOut(() -> trainer.repeatGame(1));
        assertSame(strategy, strategyOf(trainer));
        assertTrue(strategy.getUpdateCount() > firstUpdates);
        for (double parameter : strategy.getCNNModel().params().toDoubleVector()) {
            assertTrue(Double.isFinite(parameter));
        }
    }

    @Test
    public void checkpointIsSavedAfterEveryGameAndReloadsForInference() throws Exception {
        TDLearningStrategy strategy = tinyStrategy();
        LearningTrain trainer = trainerWith(strategy);
        File checkpoint = new File(files.getRoot(), "models/othello-td.zip");
        Method train = LearningTrain.class.getDeclaredMethod("trainGames", int.class, String.class);
        train.setAccessible(true);
        String output = tapSystemOut(() -> train.invoke(trainer, 2, checkpoint.getPath()));
        assertEquals(2, output.split("Successfully saved the model", -1).length - 1);
        assertTrue(checkpoint.length() > 0);
        assertTrue(output.contains("games=2 updates="));
        assertTrue(output.contains("finiteErrors=true"));

        LearningTrain loaded = new LearningTrain(41);
        tapSystemOut(() -> loaded.loadTrainedModel(checkpoint.getPath(), 0.99, 0.001));
        TDLearningStrategy restored = strategyOf(loaded);
        assertTrue(strategy.getCNNModel().params().equalsWithEps(restored.getCNNModel().params(), 1e-7));
        ComputerPlayer black = player(restored, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(restored, BoardSpace.SpaceType.WHITE);
        OthelloGame game = new OthelloGame(black, white);
        assertEquals(strategy.evaluateBoard(game.getBoard(), black),
                restored.evaluateBoard(game.getBoard(), black), 1e-6);
        BoardSpace move = restored.computerMove(game.getBoard(), black);
        assertNotNull(move);
        assertTrue(black.getAvailableMoves(game.getBoard()).containsKey(move));
        assertFalse(restored.isTraining());
        assertEquals(0, restored.getUpdateCount());
    }

    @Test
    public void updatesConnectCompleteAfterstatesFromSamePlayersPerspective() {
        List<Transition> transitions = new ArrayList<>();
        TDLearningStrategy strategy = recordingStrategy(transitions);
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        OthelloGame game = new OthelloGame(black, white);
        LearningTrain.GameResult result = new LearningTrain().playGame(game, black, white, true);
        assertTrue(result.moves() > 0 && result.moves() <= 60);
        assertEquals(4 + result.moves(), result.blackDiscs() + result.whiteDiscs());
        assertEquals(result.moves(), result.updates());
        assertEquals(result.updates(), transitions.size());
        assertOwnership(black, white, result);

        for (BoardSpace.SpaceType color : List.of(BoardSpace.SpaceType.BLACK, BoardSpace.SpaceType.WHITE)) {
            List<Transition> playerTransitions = transitions.stream().filter(t -> t.color() == color).toList();
            assertFalse(playerTransitions.isEmpty());
            BoardSpace[][] previousNext = null;
            for (int i = 0; i < playerTransitions.size(); i++) {
                Transition transition = playerTransitions.get(i);
                assertTrue(occupied(transition.before()) >= 5);
                if (previousNext != null) {
                    assertEquals(boardText(previousNext), boardText(transition.before()));
                }
                if (transition.after() != null) {
                    assertEquals(0.0, transition.reward(), 0.0);
                    assertTrue(occupied(transition.after()) > occupied(transition.before()));
                    previousNext = transition.after();
                } else {
                    assertEquals(playerTransitions.size() - 1, i);
                    double reward = Integer.compare(result.blackDiscs(), result.whiteDiscs());
                    assertEquals(color == BoardSpace.SpaceType.BLACK ? reward : -reward,
                            transition.reward(), 0.0);
                }
            }
            assertNull(playerTransitions.get(playerTransitions.size() - 1).after());
        }
    }

    @Test
    public void forcedPassDoesNotCreateFakeTransitionAndAwardsActualWinner() {
        List<Transition> transitions = new ArrayList<>();
        TDLearningStrategy strategy = recordingStrategy(transitions);
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        OthelloGame game = new OthelloGame(black, white);
        fill(game.getBoard(), BoardSpace.SpaceType.WHITE);
        game.getBoard()[0][0].setType(BoardSpace.SpaceType.EMPTY);
        game.getBoard()[0][1].setType(BoardSpace.SpaceType.BLACK);

        LearningTrain.GameResult result = new LearningTrain().playGame(game, black, white, true);
        assertEquals(1, result.moves());
        assertTrue(result.passes() > 0);
        assertEquals(0, result.blackDiscs());
        assertEquals(64, result.whiteDiscs());
        assertEquals(2, transitions.size());
        assertEquals(-1.0, transitions.get(0).reward(), 0.0);
        assertEquals(1.0, transitions.get(1).reward(), 0.0);
        assertNull(transitions.get(0).after());
        assertNull(transitions.get(1).after());
        assertOwnership(black, white, result);
    }

    @Test
    public void fullDrawUsesRealFixtureAndZeroTerminalRewards() {
        List<Transition> transitions = new ArrayList<>();
        TDLearningStrategy strategy = recordingStrategy(transitions);
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        OthelloGame game = new OthelloGame(black, white);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                game.getBoard()[x][y].setType((x + y) % 2 == 0
                        ? BoardSpace.SpaceType.BLACK : BoardSpace.SpaceType.WHITE);
            }
        }
        LearningTrain.GameResult result = new LearningTrain().playGame(game, black, white, true);
        assertEquals(0, result.moves());
        assertEquals(32, result.blackDiscs());
        assertEquals(32, result.whiteDiscs());
        assertEquals(2, transitions.size());
        for (Transition transition : transitions) {
            assertEquals(0.0, transition.reward(), 0.0);
            assertNull(transition.after());
        }
    }

    @Test
    public void bothPlayersHavingNoMovesEndsBeforeBoardIsFull() {
        List<Transition> transitions = new ArrayList<>();
        TDLearningStrategy strategy = recordingStrategy(transitions);
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        OthelloGame game = new OthelloGame(black, white);
        fill(game.getBoard(), BoardSpace.SpaceType.BLACK);
        game.getBoard()[0][0].setType(BoardSpace.SpaceType.EMPTY);
        LearningTrain.GameResult result = new LearningTrain().playGame(game, black, white, true);
        assertEquals(0, result.moves());
        assertEquals(63, result.blackDiscs());
        assertEquals(0, result.whiteDiscs());
        assertEquals(1.0, transitions.get(0).reward(), 0.0);
        assertEquals(-1.0, transitions.get(1).reward(), 0.0);
    }

    @Test
    public void reusedPlayersDoNotAccumulateOwnershipAcrossGames() throws Exception {
        TDLearningStrategy strategy = recordingStrategy(new ArrayList<>());
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        LearningTrain trainer = new LearningTrain();
        Method singleGame = LearningTrain.class.getDeclaredMethod(
                "singleGame", ComputerPlayer.class, ComputerPlayer.class);
        singleGame.setAccessible(true);
        singleGame.invoke(trainer, black, white);
        int blackCount = black.getPlayerOwnedSpacesSpaces().size();
        int whiteCount = white.getPlayerOwnedSpacesSpaces().size();
        singleGame.invoke(trainer, black, white);
        assertEquals(blackCount, black.getPlayerOwnedSpacesSpaces().size());
        assertEquals(whiteCount, white.getPlayerOwnedSpacesSpaces().size());
        assertEquals(blackCount, new HashSet<>(black.getPlayerOwnedSpacesSpaces()).size());
        assertEquals(whiteCount, new HashSet<>(white.getPlayerOwnedSpacesSpaces()).size());
        assertTrue(blackCount + whiteCount <= 64);
    }

    @Test
    public void minimaxEvaluationCompletesWithoutUpdatingModel() throws Exception {
        TDLearningStrategy strategy = tinyStrategy();
        strategy.setTraining(true);
        LearningTrain trainer = trainerWith(strategy);
        INDArray before = strategy.getCNNModel().params().dup();
        long updates = strategy.getUpdateCount();
        String output = tapSystemOut(() -> trainer.trainVsMinimax(1));
        assertTrue(output.contains("Evaluation summary: games=1"));
        assertTrue(output.contains("updates=0"));
        assertEquals(updates, strategy.getUpdateCount());
        assertArrayEquals(before.toDoubleVector(), strategy.getCNNModel().params().toDoubleVector(), 0.0);
        assertTrue(strategy.isTraining());
    }

    @Test
    public void zeroEvaluationGamesDoesNotDivideByZero() throws Exception {
        LearningTrain trainer = trainerWith(recordingStrategy(new ArrayList<>()));
        String output = tapSystemOut(() -> trainer.trainVsMinimax(0));
        assertTrue(output.contains("games=0"));
        assertFalse(output.contains("NaN"));
        assertFalse(output.contains("Infinity"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeEvaluationGamesAreRejected() {
        new LearningTrain().trainVsMinimax(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void nonFiniteTrainingErrorFailsInsteadOfSavingBadCheckpoint() {
        TDLearningStrategy strategy = recordingStrategy(new ArrayList<>());
        doReturn(Double.NaN).when(strategy).updateInternal(any(BoardSpace[][].class), any(Player.class),
                anyDouble(), nullable(BoardSpace[][].class));
        ComputerPlayer black = player(strategy, BoardSpace.SpaceType.BLACK);
        ComputerPlayer white = player(strategy, BoardSpace.SpaceType.WHITE);
        new LearningTrain().playGame(new OthelloGame(black, white), black, white, true);
    }

    @Test
    public void helpDocumentsTrainingOptionsWithoutStartingTraining() throws Exception {
        String output = tapSystemOut(() -> LearningTrain.main(new String[]{"--help"}));
        assertTrue(output.contains("--games N"));
        assertTrue(output.contains("--model PATH"));
        assertTrue(output.contains("--seed N"));
    }

    private static TDLearningStrategy tinyStrategy() {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(41).weightInit(WeightInit.XAVIER).updater(new Adam(0.001)).list()
                .layer(new DenseLayer.Builder().nIn(64).nOut(8).activation(Activation.TANH).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nOut(1).activation(Activation.IDENTITY).build())
                .setInputType(InputType.convolutional(8, 8, 1)).build();
        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        TDLearningStrategy strategy = new TDLearningStrategy(network, 0.99, 0.001);
        strategy.setSeed(41);
        return strategy;
    }

    private static LearningTrain trainerWith(TDLearningStrategy strategy) throws Exception {
        LearningTrain trainer = new LearningTrain(41);
        Field field = LearningTrain.class.getDeclaredField("trained");
        field.setAccessible(true);
        field.set(trainer, strategy);
        return trainer;
    }

    private static TDLearningStrategy strategyOf(LearningTrain trainer) throws Exception {
        Field field = LearningTrain.class.getDeclaredField("trained");
        field.setAccessible(true);
        return (TDLearningStrategy) field.get(trainer);
    }

    private static ComputerPlayer player(TDLearningStrategy strategy, BoardSpace.SpaceType color) {
        ComputerPlayer player = new ComputerPlayer(strategy);
        player.setColor(color);
        return player;
    }

    private static TDLearningStrategy recordingStrategy(List<Transition> transitions) {
        TDLearningStrategy strategy = mock(TDLearningStrategy.class);
        when(strategy.computerMove(any(BoardSpace[][].class), any(Player.class))).thenAnswer(call -> {
            BoardSpace[][] board = call.getArgument(0);
            Player player = call.getArgument(1);
            return player.getAvailableMoves(board).keySet().stream()
                    .min(Comparator.comparingInt(BoardSpace::getX).thenComparingInt(BoardSpace::getY))
                    .orElse(null);
        });
        when(strategy.updateInternal(any(BoardSpace[][].class), any(Player.class), anyDouble(),
                nullable(BoardSpace[][].class))).thenAnswer(call -> {
                    BoardSpace[][] before = call.getArgument(0);
                    Player player = call.getArgument(1);
                    double reward = call.getArgument(2);
                    BoardSpace[][] after = call.getArgument(3);
                    transitions.add(new Transition(copy(before), player.getColor(), reward,
                            after == null ? null : copy(after)));
                    return 0.25;
                });
        return strategy;
    }

    private static BoardSpace[][] copy(BoardSpace[][] board) {
        BoardSpace[][] result = new BoardSpace[board.length][];
        for (int x = 0; x < board.length; x++) {
            result[x] = new BoardSpace[board[x].length];
            for (int y = 0; y < board[x].length; y++) {
                result[x][y] = new BoardSpace(x, y, board[x][y].getType());
            }
        }
        return result;
    }

    private static void fill(BoardSpace[][] board, BoardSpace.SpaceType type) {
        for (BoardSpace[] row : board) {
            for (BoardSpace square : row) {
                square.setType(type);
            }
        }
    }

    private static int occupied(BoardSpace[][] board) {
        int count = 0;
        for (BoardSpace[] row : board) {
            for (BoardSpace square : row) {
                if (square.getType() != BoardSpace.SpaceType.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String boardText(BoardSpace[][] board) {
        StringBuilder text = new StringBuilder();
        for (BoardSpace[] row : board) {
            for (BoardSpace square : row) {
                text.append(square.getType().ordinal());
            }
        }
        return text.toString();
    }

    private static void assertOwnership(ComputerPlayer black, ComputerPlayer white,
                                        LearningTrain.GameResult result) {
        assertEquals(result.blackDiscs(), new HashSet<>(black.getPlayerOwnedSpacesSpaces()).size());
        assertEquals(result.whiteDiscs(), new HashSet<>(white.getPlayerOwnedSpacesSpaces()).size());
        for (BoardSpace square : black.getPlayerOwnedSpacesSpaces()) {
            assertEquals(BoardSpace.SpaceType.BLACK, square.getType());
        }
        for (BoardSpace square : white.getPlayerOwnedSpacesSpaces()) {
            assertEquals(BoardSpace.SpaceType.WHITE, square.getType());
        }
    }

    private record Transition(BoardSpace[][] before, BoardSpace.SpaceType color,
                              double reward, BoardSpace[][] after) {
    }
}
