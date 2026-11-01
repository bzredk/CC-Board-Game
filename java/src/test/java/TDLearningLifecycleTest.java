import othello.gamelogic.BoardSpace;
import othello.gamelogic.ComputerPlayer;
import othello.gamelogic.HumanPlayer;
import othello.gamelogic.OthelloGame;
import othello.gamelogic.TDLearningStrategy;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.List;

import static org.junit.Assert.*;

public class TDLearningLifecycleTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void realCnnUpdatesAndResumesAfterSerialization() throws Exception {
        TDLearningStrategy strategy = new TDLearningStrategy(0.99, 0.001, 42L);
        ComputerPlayer black = new ComputerPlayer(strategy);
        OthelloGame game = initialGame(black);
        Map<BoardSpace, List<BoardSpace>> moves = game.getAvailableMoves(black);
        BoardSpace move = moves.keySet().stream()
                .min(Comparator.comparingInt(BoardSpace::getX).thenComparingInt(BoardSpace::getY)).orElseThrow();
        game.takeSpaces(black, game.getPlayerTwo(), moves, move);
        try (INDArray before = strategy.getCNNModel().params().dup()) {
            assertTrue(Double.isFinite(strategy.updateInternal(game.getBoard(), black, 1.0, null)));
            assertEquals(1L, strategy.getUpdateCount());
            assertFalse(before.equalsWithEps(strategy.getCNNModel().params(), 1e-9));
        }
        double prediction = strategy.evaluateBoard(game.getBoard(), black);
        File file = temporary.newFile("real-othello.zip");
        ModelSerializer.writeModel(strategy.getCNNModel(), file, true);
        TDLearningStrategy restored = new TDLearningStrategy(
                ModelSerializer.restoreMultiLayerNetwork(file), 0.99, 0.001);
        assertEquals(prediction, restored.evaluateBoard(game.getBoard(), black), 1e-6);
        assertTrue(strategy.getCNNModel().params().equalsWithEps(restored.getCNNModel().params(), 0.0));

        // Restoring must retain Adam's state as well as the network weights.
        strategy.updateInternal(game.getBoard(), black, -1.0, null);
        restored.updateInternal(game.getBoard(), black, -1.0, null);
        assertTrue(strategy.getCNNModel().params().equalsWithEps(restored.getCNNModel().params(), 1e-6));
    }

    @Test
    public void inferenceDoesNotExploreOrUpdateTheModel() {
        TDLearningStrategy strategy = new TDLearningStrategy(0.99, 0.001, 43L);
        ComputerPlayer black = new ComputerPlayer(strategy);
        OthelloGame game = initialGame(black);
        assertFalse(strategy.isTraining());
        strategy.setExplorationRate(1.0);
        try (INDArray before = strategy.getCNNModel().params().dup()) {
            BoardSpace expected = strategy.computerMove(game.getBoard(), black);
            for (int i = 0; i < 5; i++) {
                BoardSpace actual = strategy.computerMove(game.getBoard(), black);
                assertEquals(expected.getX(), actual.getX());
                assertEquals(expected.getY(), actual.getY());
            }
            assertTrue(before.equalsWithEps(strategy.getCNNModel().params(), 0.0));
            assertEquals(0L, strategy.getUpdateCount());
            assertEquals(2, black.getPlayerOwnedSpacesSpaces().size());
            assertEquals(BoardSpace.SpaceType.EMPTY, game.getBoard()[expected.getX()][expected.getY()].getType());
        }
    }

    @Test
    public void customPlayerLoadsTheRequestedWeights() throws Exception {
        TDLearningStrategy strategy = new TDLearningStrategy(0.99, 0.001, 44L);
        File file = temporary.newFile("requested-model.zip");
        ModelSerializer.writeModel(strategy.getCNNModel(), file, true);
        String previous = System.getProperty("othello.model");
        try {
            System.setProperty("othello.model", file.getAbsolutePath());
            ComputerPlayer player = new ComputerPlayer("custom");
            TDLearningStrategy loaded = (TDLearningStrategy) player.getComputerStrategy();
            assertFalse(loaded.isTraining());
            assertTrue(strategy.getCNNModel().params().equalsWithEps(loaded.getCNNModel().params(), 0.0));
        } finally {
            restoreModelProperty(previous);
        }
    }

    @Test
    public void anExplicitMissingModelDoesNotSilentlyUseRandomWeights() {
        String previous = System.getProperty("othello.model");
        try {
            System.setProperty("othello.model", new File(temporary.getRoot(), "missing.zip").getAbsolutePath());
            assertThrows(IllegalArgumentException.class, () -> new ComputerPlayer("custom"));
        } finally {
            restoreModelProperty(previous);
        }
    }

    @Test
    public void invalidRewardsFailBeforeChangingWeights() {
        TDLearningStrategy strategy = new TDLearningStrategy(0.99, 0.001, 45L);
        ComputerPlayer black = new ComputerPlayer(strategy);
        OthelloGame game = initialGame(black);
        try (INDArray before = strategy.getCNNModel().params().dup()) {
            assertThrows(IllegalStateException.class,
                    () -> strategy.updateInternal(game.getBoard(), black, Double.NaN, null));
            assertTrue(before.equalsWithEps(strategy.getCNNModel().params(), 0.0));
            assertEquals(0L, strategy.getUpdateCount());
        }
    }

    @Test
    public void bundledWeightsAreFiniteAndHaveChangedFromTheirInitialization() throws Exception {
        File checkpoint = new File("models/othello-td.zip");
        Assume.assumeTrue("Generate the baseline checkpoint first", checkpoint.isFile());
        TDLearningStrategy initial = new TDLearningStrategy(0.99, 0.001, 27L);
        var trained = ModelSerializer.restoreMultiLayerNetwork(checkpoint);
        assertFalse(initial.getCNNModel().params().equalsWithEps(trained.params(), 1e-8));
        for (float weight : trained.params().toFloatVector()) {
            assertTrue(Float.isFinite(weight));
        }
        ComputerPlayer black = new ComputerPlayer(new TDLearningStrategy(trained, 0.99, 0.001));
        OthelloGame game = initialGame(black);
        assertTrue(Double.isFinite(((TDLearningStrategy) black.getComputerStrategy())
                .evaluateBoard(game.getBoard(), black)));
    }

    private static OthelloGame initialGame(ComputerPlayer black) {
        black.setColor(BoardSpace.SpaceType.BLACK);
        HumanPlayer white = new HumanPlayer();
        white.setColor(BoardSpace.SpaceType.WHITE);
        return new OthelloGame(black, white);
    }

    private static void restoreModelProperty(String previous) {
        if (previous == null) {
            System.clearProperty("othello.model");
        } else {
            System.setProperty("othello.model", previous);
        }
    }
}
