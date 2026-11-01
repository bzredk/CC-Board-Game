import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import othello.gamelogic.BoardSpace;
import othello.gamelogic.ComputerPlayer;
import othello.gamelogic.TDLearningStrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTDLearningStrategy {
    @Test
    public void testTDLearningStrategyTwoArg1() {
        double gamma = 0.90;
        double alpha = 0.005;
        TDLearningStrategy theStrategy = new TDLearningStrategy(gamma, alpha);
        assertNotNull(theStrategy);
    }

    @Test
    public void testTDLearningStrategyThreeArg1() {
        double gamma = 0.99;
        double alpha = 0.001;
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(6).list()
                .layer(0, new DenseLayer.Builder().nIn(1).nOut(1).activation(Activation.RELU).build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
                        .nOut(1).build())
                .build();

        MultiLayerNetwork testNetwork = new MultiLayerNetwork(configuration);
        testNetwork.init();
        TDLearningStrategy theStrategy = new TDLearningStrategy(testNetwork, gamma, alpha);
        assertNotNull(theStrategy);
    }

    @Test
    public void testComputerMove1() {
        TDLearningStrategy theStrategy  = new TDLearningStrategy(0.90, 0.01);
        ComputerPlayer actingPlayer = new ComputerPlayer("custom");
        actingPlayer.setColor(BoardSpace.SpaceType.BLACK);

        BoardSpace[][] board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
            }
        }

        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);

        BoardSpace theChosen = theStrategy.computerMove(board, actingPlayer);
        assertNotNull(theChosen);
    }

    @Test
    public void testUpdateInternal() {
        TDLearningStrategy theStrategy  = new TDLearningStrategy(0.90, 0.01);
        ComputerPlayer actingPlayer = new ComputerPlayer("custom");
        actingPlayer.setColor(BoardSpace.SpaceType.BLACK);

        BoardSpace[][] board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
            }
        }

        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);

        double testReward = 0.6;
        double testError = theStrategy.updateInternal(board, actingPlayer, testReward, null);
        assertTrue(testError >= 0.0);
    }

    @Test
    public void testGetCNNModel1() {
        TDLearningStrategy theStrategy  = new TDLearningStrategy(0.90, 0.01);
        MultiLayerNetwork testModel = theStrategy.getCNNModel();
        assertNotNull(testModel);
    }


}
