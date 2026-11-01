import othello.Constants;
import othello.gamelogic.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OthelloGame
 * JUnit 4.13.2
 */
public class TestMinimax {

    private BoardSpace[][] board;
    private final Player black = new HumanPlayer();
    private final Player white = new HumanPlayer();
    private Minimax mm;

    @Before
    public void setup() {
        mm = new Minimax();
        board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);

        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);

        black.setColor(BoardSpace.SpaceType.BLACK);
        white.setColor(BoardSpace.SpaceType.WHITE);
    }

    // computeWeight: adding a black piece increases score by its weight
    @Test
    public void weightEvalTest() {
        int base = mm.computeWeight(board, black);
        board[2][3].setType(BoardSpace.SpaceType.BLACK);
        int after = mm.computeWeight(board, black);
        assertTrue(after > base);
        assertEquals(Constants.BOARD_WEIGHTS[2][3], after - base);
    }

    // buildTree+minimax: on B-W-W-W vertical, pick the far-right flip at (5,3)
    @Test
    public void twoLayerChoiceTest() {
        board[1][3].setType(BoardSpace.SpaceType.BLACK);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][3].setType(BoardSpace.SpaceType.WHITE);
        Node root = new Node();
        mm.buildTree(board, black, white, 0, root, 2);
        BoardSpace best = mm.minimaxStrategyWithMaxDepth(root);
        assertEquals(5, best.getX());
        assertEquals(3, best.getY());
    }

    // buildTree: empty board generates leaf node with weight 0
    @Test
    public void emptyBoardLeafTest() {
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        Node root = new Node();
        mm.buildTree(empty, black, white, 0, root, 2);
        assertTrue(root.getChildren().isEmpty());
        assertEquals(0, root.getWeight());
    }

    // buildTree: no moves available ⇒ node is leaf with weight 0
    @Test
    public void noMoveTest() {
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        Node root = new Node();
        mm.buildTree(empty, black, white, 0, root, 2);
        assertTrue(root.getChildren().isEmpty());
        assertEquals(0, root.getWeight());
    }

    // computeWeight: empty board evaluates to 0
    @Test
    public void emptyWeightTest() {
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        int w = mm.computeWeight(empty, black);
        assertEquals(0, w);
    }

    // buildTree: at maxDepth, direct children are leaves with non-zero weight
    @Test
    public void depthLeafTest() {
        Node root = new Node();
        mm.buildTree(board, black, white, 0, root, 2);
        for (Node leaf : root.getChildren()) {
            assertTrue(leaf.getChildren().isEmpty());
            assertNotEquals(0, leaf.getWeight());
        }
    }

    // minimaxOneStep: when opponent has no reply, return the only move
    @Test
    public void opponentNoMoveTest() {
        BoardSpace[][] custom = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                custom[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        custom[3][3].setType(BoardSpace.SpaceType.WHITE);
        custom[3][4].setType(BoardSpace.SpaceType.BLACK);
        BoardSpace best = mm.minimaxOneStep(custom, black);
        assertNotNull(best);
        assertEquals(3, best.getX());
        assertEquals(2, best.getY());
    }

    // getCopyBoard & toString: copy is independent and prints identically
    @Test
    public void copyPrintTest() {
        BoardSpace[][] copy = mm.getCopyBoard(board);
        assertNotSame(board, copy);
        assertEquals(mm.toString(board), mm.toString(copy));
    }

    // futureBoard: vertical flip path at (2,3) flips (3,3),(4,3)
    @Test
    public void verticalFlipTest() {
        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        BoardSpace dst = board[2][3];
        BoardSpace[][] next = mm.futureBoard(board, dst, moves, black);
        for (int x : new int[]{2, 3, 4}) {
            assertSame(BoardSpace.SpaceType.BLACK, next[x][3].getType());
        }
    }

    // minimaxOneStep: no legal moves ⇒ return null
    @Test
    public void oneStepNoMoveTest() {
        BoardSpace[][] empty = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                empty[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
        assertNull(mm.minimaxOneStep(empty, black));
    }

    // getCopyBoard: mutating the copy does not affect the original
    @Test
    public void copyIsolationTest() {
        BoardSpace[][] c = mm.getCopyBoard(board);
        c[3][3].setType(BoardSpace.SpaceType.EMPTY);
        assertNotEquals(mm.toString(board), mm.toString(c));
    }

    // futureBoard: passing a non-legal move yields null
    @Test
    public void futureBoardNullOriginTest() {
        Map<BoardSpace, List<BoardSpace>> moves = black.getAvailableMoves(board);
        BoardSpace fake = new BoardSpace(0, 0, BoardSpace.SpaceType.EMPTY);
        assertNull(mm.futureBoard(board, fake, moves, black));
    }

    // ensure pruning works on min branch when alpha > current min
    @Test
    public void BetaPruneTest() {
        Node root = new Node();
        root.setMin(true);
        Node child1 = new Node(); child1.setWeight(-50);
        Node child2 = new Node(); child2.setWeight( 99);  // << should be cut
        root.getChildren().add(child1);
        root.getChildren().add(child2);
        int alpha = -10;
        Node picked = mm.min_node(root, alpha, Integer.MAX_VALUE);
        assertSame(child1, picked);
    }

    // passing maxDepth < 2 should throw IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void BuildDepthErrorTest() {
        Node root = new Node();
        mm.buildTree(board, black, white, 0, root, 1);
    }

    // main(): ensure demo main runs without error
    @Test
    public void mainCoverageTest() {
        Minimax.main(new String[0]);
    }
}