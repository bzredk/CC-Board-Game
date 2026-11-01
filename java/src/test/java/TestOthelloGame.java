import othello.gamelogic.*;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for OthelloGame
 * JUnit 4.13.2
 */
public class TestOthelloGame {

    private Player black;     // acts first
    private Player white;
    private OthelloGame game;
    private BoardSpace[][] board;


    /**
     * identical test fixture for each test – reproducible and small
     */
    @Before
    public void setUp() {
        black = new HumanPlayer();
        white = new HumanPlayer();
        black.setColor(BoardSpace.SpaceType.BLACK);
        white.setColor(BoardSpace.SpaceType.WHITE);
        game = new OthelloGame(black, white);
        board = game.getBoard();
    }

    @Test
    public void gameOthelloInitTest() {
        // black owns the two outer centre squares
        assertEquals(
                Set.of(board[3][4], board[4][3]),
                new HashSet<>(black.getPlayerOwnedSpacesSpaces())
        );

        // white owns the two inner centre squares
        assertEquals(
                Set.of(board[3][3], board[4][4]),
                new HashSet<>(white.getPlayerOwnedSpacesSpaces())
        );
    }

    @Test
    public void gameOthelloBasicsTest() {
        Map<BoardSpace, List<BoardSpace>> moves = game.getAvailableMoves(black);
        // expected squares in reading-order
        Set<BoardSpace> expected = new HashSet<>(Arrays.asList(
                board[2][3], board[3][2], board[4][5], board[5][4]));

        assertEquals("black should have 4 legal moves on move 1",
                4, moves.size());
        assertTrue(moves.keySet().containsAll(expected));
    }

    @Test
    public void whiteFirstMoveTest() {
        // black plays (2,3)
        game.takeSpaces(
                black, white,
                game.getAvailableMoves(black),
                board[2][3]);

        Map<BoardSpace,List<BoardSpace>> wMoves = game.getAvailableMoves(white);
        assertEquals(3, wMoves.size());
        assertTrue(wMoves.keySet().containsAll(
                Set.of(board[2][2], board[2][4], board[4][2])));
    }

    @Test
    public void gameOthelloFlipTest() {
        // Pick the opening move at (2,3), which should flip (3,3)
        BoardSpace dest = board[2][3];

        Map<BoardSpace, List<BoardSpace>> moves =
                game.getAvailableMoves(black);
        assertTrue(moves.containsKey(dest));

        game.takeSpaces(black, white, moves, dest);
        // After the move, (2,3), (3,3), and (4,3) must all be BLACK
        Set<BoardSpace> blacks = Set.of(board[2][3], board[3][3], board[4][3]);
        for (BoardSpace c : blacks) {
            assertSame(BoardSpace.SpaceType.BLACK, c.getType());
        }
        assertSame(BoardSpace.SpaceType.WHITE, board[4][4].getType());

        Map<BoardSpace, List<BoardSpace>> next = game.getAvailableMoves(white);
        Set<BoardSpace> expected = Set.of(board[2][2], board[2][4], board[4][2]);
        assertEquals(3, next.size());
        assertTrue(next.keySet().containsAll(expected));
    }

    @Test
    public void frontNBackFlipTest() {
        // clear the whole board
        for (int destX : new int[]{2, 6}) {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    board[x][y].setType(BoardSpace.SpaceType.EMPTY);

            // build W-W-W chain + B anchor
            int anchorX = (destX == 2) ? 6 : 2;
            board[3][4].setType(BoardSpace.SpaceType.WHITE);
            board[4][4].setType(BoardSpace.SpaceType.WHITE);
            board[5][4].setType(BoardSpace.SpaceType.WHITE);
            board[anchorX][4].setType(BoardSpace.SpaceType.BLACK);

            // only (destX,3) is legal
            Map<BoardSpace, List<BoardSpace>> moves = game.getAvailableMoves(black);
            assertEquals(1, moves.size());
            assertTrue(moves.containsKey(board[destX][4]));

            game.takeSpaces(black, white, moves, board[destX][4]);

            // verify that x=2..6 at y=3 are black, rest empty
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    if (y == 4 && x >= 2 && x <= 6) {
                        assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                    } else {
                        assertSame(BoardSpace.SpaceType.EMPTY, board[x][y].getType());
                    }
                }
            }
        }
    }

    @Test
    public void leftRightFlipTest() {
        for (BoardSpace[] row : board)
            for (BoardSpace c : row)
                c.setType(BoardSpace.SpaceType.EMPTY);

        // row y = 4 :  . B W W · W W B
        board[1][4].setType(BoardSpace.SpaceType.BLACK);
        board[2][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.WHITE);
        board[5][4].setType(BoardSpace.SpaceType.WHITE);
        board[6][4].setType(BoardSpace.SpaceType.WHITE);
        board[7][4].setType(BoardSpace.SpaceType.BLACK);

        Map<BoardSpace,List<BoardSpace>> moves = game.getAvailableMoves(black);
        assertEquals(Set.of(board[4][4]), moves.keySet());

        game.takeSpaces(black, white, moves, board[4][4]);

        // row 4, cols 1..7 should all be black
        for (int x = 1; x <= 7; x++)
            assertSame(BoardSpace.SpaceType.BLACK, board[x][4].getType());
    }

    @Test
    public void takeNSplitTest() {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y].setType(BoardSpace.SpaceType.EMPTY);
            }
        }

        // build B-W- -W-B chain + B anchor
        board[2][4].setType(BoardSpace.SpaceType.BLACK);
        board[3][4].setType(BoardSpace.SpaceType.WHITE);
        board[5][4].setType(BoardSpace.SpaceType.WHITE);
        board[6][4].setType(BoardSpace.SpaceType.BLACK);

        Map<BoardSpace, List<BoardSpace>> moves = game.getAvailableMoves(black);

        // Only one valid move is expected: (4,3)
        List<BoardSpace> expectedMoves = new ArrayList<>();
        expectedMoves.add(board[4][4]);

        // Verify that only (4,3) is a legal move
        for (BoardSpace b : moves.keySet()) {
            assertTrue(expectedMoves.contains(b));
            expectedMoves.remove(b);
        }
        assertTrue(expectedMoves.isEmpty());

        game.takeSpaces(black, white, moves, board[4][4]);

        // verify that the pieces are correctly flipped to black, B-B-B-B-B
        for (int x = 0; x < board.length; ++x) {
            for (int y = 0; y < board[x].length; ++y) {
                if (x == 2 && y == 4) {
                    assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                } else if (x == 3 && y == 4) {
                    assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                } else if (x == 4 && y == 4) {
                    assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                } else if (x == 5 && y == 4) {
                    assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                } else if (x == 6 && y == 4) {
                    assertSame(BoardSpace.SpaceType.BLACK, board[x][y].getType());
                } else {
                    assertSame(BoardSpace.SpaceType.EMPTY, board[x][y].getType());
                }
            }
        }
    }


    @Test
    public void diagonalFlipTest() {
        for (BoardSpace[] row : board)
            for (BoardSpace c : row)
                c.setType(BoardSpace.SpaceType.EMPTY);

        // diagonal ↘ : B W W · W W B
        board[1][1].setType(BoardSpace.SpaceType.BLACK);
        board[2][2].setType(BoardSpace.SpaceType.WHITE);
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[5][5].setType(BoardSpace.SpaceType.WHITE);
        board[6][6].setType(BoardSpace.SpaceType.WHITE);
        board[7][7].setType(BoardSpace.SpaceType.BLACK);

        Map<BoardSpace,List<BoardSpace>> moves = game.getAvailableMoves(black);
        assertEquals(Set.of(board[4][4]), moves.keySet());

        game.takeSpaces(black, white, moves, board[4][4]);

        // cells (1,1)…(7,7) now all black
        for (int i = 1; i <= 7; i++)
            assertSame(BoardSpace.SpaceType.BLACK, board[i][i].getType());
    }

    @Test
    public void multiDirectionFlipTest() {
        for (BoardSpace[] row : board)
            for (BoardSpace c : row)
                c.setType(BoardSpace.SpaceType.EMPTY);

        // set up front chain: B at (3,1), W at (3,2)
        board[3][1].setType(BoardSpace.SpaceType.BLACK);
        board[3][2].setType(BoardSpace.SpaceType.WHITE);

        // set up left chain: B at (1,3), W at (2,3)
        board[1][3].setType(BoardSpace.SpaceType.BLACK);
        board[2][3].setType(BoardSpace.SpaceType.WHITE);

        // only legal move should be at (3,3)
        Map<BoardSpace,List<BoardSpace>> moves = game.getAvailableMoves(black);
        assertEquals(Set.of(board[3][3]), moves.keySet());

        // perform the move and flip both chains
        game.takeSpaces(black, white, moves, board[3][3]);

        // verify front-left chain flipped
        assertSame(BoardSpace.SpaceType.BLACK, board[3][1].getType());
        assertSame(BoardSpace.SpaceType.BLACK, board[3][2].getType());
        assertSame(BoardSpace.SpaceType.BLACK, board[2][3].getType());
        assertSame(BoardSpace.SpaceType.BLACK, board[1][3].getType());

        // verify the played cell is also black
        assertSame(BoardSpace.SpaceType.BLACK, board[3][3].getType());
    }

    @Test
    public void takeSpaceNoOpTest() {
        // calling takeSpace on a cell you already own shouldn’t change anything
        int before = black.getPlayerOwnedSpacesSpaces().size();
        game.takeSpace(black, white, 3, 4);  // (3,4) is already BLACK
        assertEquals(before, black.getPlayerOwnedSpacesSpaces().size());
    }

    @Test
    public void twoSkipsThenGameOver() {
        for (BoardSpace[] row : board) {
            for (BoardSpace cell : row) {
                cell.setType(BoardSpace.SpaceType.EMPTY);
            }
        }
        black.getPlayerOwnedSpacesSpaces().clear();
        white.getPlayerOwnedSpacesSpaces().clear();

        // place three black stones and record them
        int[][] blacks = {{0,0},{0,1},{1,0}};
        for (int[] pos : blacks) {
            BoardSpace bs = board[pos[0]][pos[1]];
            bs.setType(BoardSpace.SpaceType.BLACK);
            black.getPlayerOwnedSpacesSpaces().add(bs);
        }

        // both players have no moves
        int skipped = 0;
        if (game.getAvailableMoves(black).isEmpty()) skipped++;
        if (game.getAvailableMoves(white).isEmpty()) skipped++;

        assertEquals(2, skipped);

        // now counts line up with the board
        int blackCount = black.getPlayerOwnedSpacesSpaces().size();
        int whiteCount = white.getPlayerOwnedSpacesSpaces().size();
        assertTrue(blackCount > whiteCount);
    }

    @Test
    // invalid destination shouldn’t change the board
    public void takeSpacesWithInvalidDestinationTest() {
        BoardSpace dest = board[0][0];
        Map<BoardSpace, List<BoardSpace>> moves = Collections.emptyMap();
        game.takeSpaces(black, white, moves, dest);
        assertSame(BoardSpace.SpaceType.EMPTY, dest.getType());
    }

    @Test
    // full board leaves no legal moves for either player
    public void fullBoardNoMovesTest() {
        for (BoardSpace[] row : board) {
            for (BoardSpace c : row) {
                c.setType(BoardSpace.SpaceType.BLACK);
                black.getPlayerOwnedSpacesSpaces().add(c);
            }
        }
        assertTrue(game.getAvailableMoves(black).isEmpty());
        assertTrue(game.getAvailableMoves(white).isEmpty());
    }

    @Test
    // claim an empty cell and add to player’s stones
    public void takeSpaceEmptyClaimTest() {
        BoardSpace empty = board[0][0];
        assertSame(BoardSpace.SpaceType.EMPTY, empty.getType());
        game.takeSpace(black, white, 0, 0);
        assertSame(BoardSpace.SpaceType.BLACK, empty.getType());
        assertTrue(black.getPlayerOwnedSpacesSpaces().contains(empty));
    }

    @Test
    // capture opponent’s white stone and transfer ownership
    public void takeSpaceCaptureOpponentTest() {
        BoardSpace target = board[3][3];               // starts WHITE
        assertSame(BoardSpace.SpaceType.WHITE, target.getType());
        game.takeSpace(black, white, 3, 3);
        assertSame(BoardSpace.SpaceType.BLACK, target.getType());
        assertFalse(white.getPlayerOwnedSpacesSpaces().contains(target));
        assertTrue(black.getPlayerOwnedSpacesSpaces().contains(target));
    }

    @Test
    // minimax branch should return the planned move
    public void computerDecisionMinimaxTest() {
        BoardSpace move = board[2][3];                 // any legal square
        ComputerPlayer cpu = new DummyComputerPlayer("minimax", move);
        assertSame("minimax branch failed", move, game.computerDecision(cpu));
    }

    @Test
    // custom strategy branch should return the planned move
    public void computerDecisionCustomTest() {
        BoardSpace move = board[3][2];
        ComputerPlayer cpu = new DummyComputerPlayer("custom", move);
        assertSame("custom branch failed", move, game.computerDecision(cpu));
    }

    @Test
    // mcts branch should return the planned move
    public void computerDecisionMCTSTest() {
        BoardSpace move = board[4][5];
        ComputerPlayer cpu = new DummyComputerPlayer("mcts", move);
        assertSame("mcts branch failed", move, game.computerDecision(cpu));
    }

    @Test
    // unknown strategy returns null
    public void computerDecisionUnknownStrategyTest() {
        BoardSpace move = board[5][4];
        ComputerPlayer cpu = new DummyComputerPlayer("random", move);
        assertNull("unexpected move for unknown strategy", game.computerDecision(cpu));
    }

    /**
     * Minimal stub – just enough behaviour for computerDecision().
     */
    // DummyComputerPlayer is a bare‐bones ComputerPlayer for tests.
    // It takes a strategy name and a fixed move in its constructor,
    // then overrides getStrategy(), getAvailableMoves() and all computerMove()
    // methods to always return that move, so we can force each branch
    // in computerDecision() without running the real AI logic.
    private static class DummyComputerPlayer extends ComputerPlayer {
        private final String strategy;                 // "minimax", "custom", or "mcts"
        private final BoardSpace plannedMove;          // the square we pretend to choose
        private final Map<BoardSpace,List<BoardSpace>> legalMoves;

        DummyComputerPlayer(String strategy, BoardSpace plannedMove) {
            super("dummy");                            // supply required name arg
            this.strategy = strategy;
            this.plannedMove = plannedMove;
            this.legalMoves = new HashMap<>();
            this.legalMoves.put(plannedMove, Collections.emptyList());
            setColor(BoardSpace.SpaceType.BLACK);      // any colour is fine for tests
        }

        // strategy getter used by computerDecision()
        @Override public String getStrategy() { return strategy; }

        // used by computerDecision() to retrieve moves
        @Override
        public Map<BoardSpace, List<BoardSpace>> getAvailableMoves(BoardSpace[][] ignored) {
            return legalMoves;
        }

        // ↓ All three overloads just hand back the pre-chosen square
        @Override
        public BoardSpace computerMove(BoardSpace[][] b, Player s, Player o, int depth) {
            return plannedMove;
        }
        @Override
        public BoardSpace computerMove(BoardSpace[][] b, Player s) {
            return plannedMove;
        }
        @Override
        public BoardSpace computerMove(BoardSpace[][] b, Player s, Player o,
                                       int dummy1, int dummy2) {
            return plannedMove;
        }
    }
}