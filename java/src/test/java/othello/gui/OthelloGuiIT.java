package othello.gui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import othello.App;
import othello.gamelogic.BoardSpace;
import othello.gamelogic.OthelloGame;
import othello.gamelogic.ComputerPlayer;
import othello.gamelogic.TDLearningStrategy;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Assume;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/** Run explicitly with -Dtest=othello.gui.OthelloGuiIT on a desktop display. */
public class OthelloGuiIT {
    private static final AtomicReference<Throwable> FX_FAILURE = new AtomicReference<>();
    private static final Pattern TURN = Pattern.compile("(BLACK|WHITE)'s Turn");
    private GameController controller;
    private Stage stage;
    private String blackMode;
    private String whiteMode;

    @BeforeClass
    public static void startJavaFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> FX_FAILURE.set(failure));
            ready.countDown();
        });
        assertTrue("JavaFX did not start", ready.await(20, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void stopJavaFx() {
        Platform.exit();
    }

    @After
    public void closeWindow() throws Exception {
        if (stage != null) {
            onFx(() -> {
                stage.close();
                return null;
            });
        }
    }

    @Test(timeout = 90000)
    public void humanVsHumanCompletes() throws Exception {
        open("human", "human");
        playToEnd(11, "human-human");
    }

    @Test(timeout = 90000)
    public void humanVsMctsCompletes() throws Exception {
        open("human", "mcts");
        playToEnd(12, "human-mcts");
    }

    @Test(timeout = 90000)
    public void humanVsMinimaxCompletes() throws Exception {
        open("human", "minimax");
        playToEnd(13, "human-minimax");
    }

    @Test(timeout = 90000)
    public void minimaxVsMctsCompletes() throws Exception {
        open("minimax", "mcts");
        playToEnd(14, "minimax-mcts");
    }

    @Test(timeout = 90000)
    public void humanVsTrainedCustomCompletes() throws Exception {
        Path checkpoint = Path.of(System.getProperty("othello.model", "models/othello-td.zip"));
        Assume.assumeTrue("Train the TD model before this GUI check", Files.isRegularFile(checkpoint));
        open("human", "custom");
        onFx(() -> {
            ComputerPlayer computer = (ComputerPlayer) game().getPlayerTwo();
            TDLearningStrategy strategy = (TDLearningStrategy) computer.getComputerStrategy();
            assertFalse(strategy.isTraining());
            assertTrue(ModelSerializer.restoreMultiLayerNetwork(checkpoint.toFile()).params()
                    .equalsWithEps(strategy.getCNNModel().params(), 0.0));
            return null;
        });
        playToEnd(16, "human-trained-custom");
    }

    @Test
    public void clickingAnIllegalSquareDoesNotChangeTheBoard() throws Exception {
        open("human", "mcts");
        onFx(() -> {
            char[][] before = readBoard();
            clickSquare(0, 0);
            assertBoardEquals(before, readBoard());
            assertOwnership(before);
            return null;
        });
    }

    @Test
    public void forcedPassUpdatesTheTurnIndicatorAndFinishes() throws Exception {
        open("human", "mcts");
        onFx(() -> {
            char[][] fixture = filled('W');
            fixture[0][0] = '.';
            fixture[0][1] = 'B';
            setPosition(fixture);
            controller.turnText(game().getPlayerOne());
            controller.takeTurn(game().getPlayerOne());
            assertEquals("WHITE", currentTurn());
            Circle indicator = (Circle) stage.getScene().lookup("#turnCircle");
            assertEquals("Turn indicator must follow a forced pass",
                    BoardSpace.SpaceType.WHITE.fill(), indicator.getFill());
            return null;
        });
        playToEnd(15, "forced-pass");
    }

    @Test
    public void aPositionWithNoLegalMovesEndsBeforeTheBoardIsFull() throws Exception {
        open("human", "human");
        onFx(() -> {
            char[][] fixture = filled('B');
            fixture[0][0] = '.';
            setPosition(fixture);
            controller.takeTurn(game().getPlayerOne());
            assertFinished(fixture);
            assertEquals('.', readBoard()[0][0]);
            return null;
        });
    }

    @Test
    public void aFullTiedBoardDisplaysADraw() throws Exception {
        open("human", "human");
        onFx(() -> {
            char[][] fixture = filled('B');
            for (int x = 0; x < 4; x++) {
                Arrays.fill(fixture[x], 'W');
            }
            setPosition(fixture);
            controller.takeTurn(game().getPlayerOne());
            assertFinished(fixture);
            return null;
        });
    }

    private void open(String black, String white) throws Exception {
        blackMode = black;
        whiteMode = white;
        onFx(() -> {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("game-view.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            controller.initGame(black, white);
            stage = new Stage();
            stage.setTitle("Othello GUI verification");
            stage.setScene(new Scene(root, 960, 600));
            stage.show();
            root.applyCss();
            root.layout();
            assertEquals(4, count(readBoard(), 'B') + count(readBoard(), 'W'));
            assertOwnership(readBoard());
            return null;
        });
    }

    private void playToEnd(long seed, String name) throws Exception {
        Random random = new Random(seed);
        for (int step = 0; step < 128; step++) {
            boolean finished = onFx(() -> {
                char[][] before = readBoard();
                if (label().getText().startsWith("GAME OVER!")) {
                    assertFinished(before);
                    saveScreenshot(name);
                    System.out.printf("GUI PASS %s: BLACK=%d WHITE=%d%n",
                            name, count(before, 'B'), count(before, 'W'));
                    return true;
                }
                String turn = currentTurn();
                char color = turn.equals("BLACK") ? 'B' : 'W';
                List<int[]> legal = legalMoves(before, color);
                String mode = color == 'B' ? blackMode : whiteMode;
                if (mode.equals("human")) {
                    assertFalse("Human turn stalled without legal moves", legal.isEmpty());
                    int[] move = legal.get(random.nextInt(legal.size()));
                    clickSquare(move[0], move[1]);
                } else {
                    Button button = (Button) stage.getScene().lookup("#computerTurnBtn");
                    assertTrue("Computer turn button must be visible", button.isVisible());
                    button.fire();
                }
                char[][] after = readBoard();
                if (legal.isEmpty()) {
                    assertBoardEquals(before, after);
                } else {
                    List<int[]> placed = new ArrayList<>();
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            if (before[x][y] == '.' && after[x][y] != '.') {
                                placed.add(new int[]{x, y});
                            }
                        }
                    }
                    assertEquals("Each move must place exactly one disc", 1, placed.size());
                    int[] move = placed.get(0);
                    assertTrue("Selected move must bracket an opponent disc",
                            captures(before, color, move[0], move[1]).size() > 0);
                    char[][] expected = copy(before);
                    for (int[] flipped : captures(before, color, move[0], move[1])) {
                        expected[flipped[0]][flipped[1]] = color;
                    }
                    expected[move[0]][move[1]] = color;
                    assertBoardEquals(expected, after);
                }
                assertOwnership(after);
                return false;
            });
            if (finished) {
                return;
            }
        }
        fail("Game did not finish within 128 UI actions: " + name);
    }

    private void clickSquare(int x, int y) throws Exception {
        GUISpace[][] spaces = field("guiBoard", GUISpace[][].class);
        Pane square = spaces[x][y].getSquare();
        square.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED,
                30, 30, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true,
                new PickResult(square, 30, 30)));
    }

    private void setPosition(char[][] position) throws Exception {
        controller.clearBoard();
        OthelloGame game = game();
        game.getPlayerOne().getPlayerOwnedSpacesSpaces().clear();
        game.getPlayerTwo().getPlayerOwnedSpacesSpaces().clear();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                BoardSpace space = game.getBoard()[x][y];
                space.setType(position[x][y] == 'B' ? BoardSpace.SpaceType.BLACK
                        : position[x][y] == 'W' ? BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.EMPTY);
                if (position[x][y] == 'B') {
                    game.getPlayerOne().getPlayerOwnedSpacesSpaces().add(space);
                } else if (position[x][y] == 'W') {
                    game.getPlayerTwo().getPlayerOwnedSpacesSpaces().add(space);
                }
            }
        }
        controller.displayBoard();
    }

    private char[][] readBoard() throws Exception {
        char[][] result = new char[8][8];
        BoardSpace[][] board = game().getBoard();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                result[x][y] = switch (board[x][y].getType()) {
                    case BLACK -> 'B';
                    case WHITE -> 'W';
                    case EMPTY -> '.';
                };
            }
        }
        return result;
    }

    private void assertOwnership(char[][] board) throws Exception {
        assertEquals(count(board, 'B'), game().getPlayerOne().getPlayerOwnedSpacesSpaces().size());
        assertEquals(count(board, 'W'), game().getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
    }

    private void assertFinished(char[][] board) {
        assertTrue(legalMoves(board, 'B').isEmpty());
        assertTrue(legalMoves(board, 'W').isEmpty());
        String result = label().getText();
        assertTrue("Expected terminal message, got: " + result, result.startsWith("GAME OVER!"));
        int black = count(board, 'B');
        int white = count(board, 'W');
        assertTrue(result.contains("BLACK: " + black + " - WHITE: " + white));
        assertTrue(result.contains(black == white ? "Game Tied"
                : black > white ? "BLACK wins" : "WHITE wins"));
    }

    private String currentTurn() {
        Matcher matcher = TURN.matcher(label().getText());
        assertTrue("No current turn in: " + label().getText(), matcher.find());
        return matcher.group(1);
    }

    private Label label() {
        return (Label) stage.getScene().lookup("#turnLabel");
    }

    private OthelloGame game() throws Exception {
        return field("og", OthelloGame.class);
    }

    private <T> T field(String name, Class<T> type) throws Exception {
        Field field = GameController.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(controller));
    }

    // This oracle deliberately uses no production move-generation or flip methods.
    private static List<int[]> captures(char[][] board, char color, int x, int y) {
        List<int[]> result = new ArrayList<>();
        if (board[x][y] != '.') {
            return result;
        }
        char opponent = color == 'B' ? 'W' : 'B';
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                List<int[]> line = new ArrayList<>();
                int nx = x + dx;
                int ny = y + dy;
                while (nx >= 0 && nx < 8 && ny >= 0 && ny < 8 && board[nx][ny] == opponent) {
                    line.add(new int[]{nx, ny});
                    nx += dx;
                    ny += dy;
                }
                if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8 && board[nx][ny] == color) {
                    result.addAll(line);
                }
            }
        }
        return result;
    }

    private static List<int[]> legalMoves(char[][] board, char color) {
        List<int[]> result = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (!captures(board, color, x, y).isEmpty()) {
                    result.add(new int[]{x, y});
                }
            }
        }
        return result;
    }

    private static char[][] filled(char color) {
        char[][] board = new char[8][8];
        for (char[] row : board) {
            Arrays.fill(row, color);
        }
        return board;
    }

    private static char[][] copy(char[][] board) {
        return Arrays.stream(board).map(char[]::clone).toArray(char[][]::new);
    }

    private static int count(char[][] board, char color) {
        int result = 0;
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == color) {
                    result++;
                }
            }
        }
        return result;
    }

    private static void assertBoardEquals(char[][] expected, char[][] actual) {
        for (int x = 0; x < 8; x++) {
            assertArrayEquals("Board column " + x, expected[x], actual[x]);
        }
    }

    private void saveScreenshot(String name) throws Exception {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        WritableImage image = stage.getScene().snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height,
                PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, width, height, pixels, 0, width);
        Path directory = Path.of(System.getProperty("gui.outputDir", "../.build/verification"));
        Files.createDirectories(directory);
        ImageIO.write(output, "png", directory.resolve(name + ".png").toFile());
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        T result = task.get(30, TimeUnit.SECONDS);
        if (FX_FAILURE.get() != null) {
            throw new AssertionError("Uncaught JavaFX exception", FX_FAILURE.get());
        }
        return result;
    }
}
