import org.junit.Test;
import othello.gamelogic.*;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestMCTS {

    @Test
    public void testMCTS_Strategy() {
        MCTSNode node = new MCTSNode();
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        node.setBoard(board);
        Player one = new HumanPlayer();
        Player two = new HumanPlayer();
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        MCTS tester = new MCTS(100, one, two, node.getBoard());
        BoardSpace next_best = tester.MCTS_Strategy();
        assertTrue(one.getAvailableMoves(board).containsKey(next_best));
    }

    @Test
    public void testUCT() {
        MCTSNode node = new MCTSNode();
        MCTS tester = new MCTS();
        MCTSNode parent = new MCTSNode();
        node.setParent(parent);
        assertEquals(Double.POSITIVE_INFINITY, tester.UCT(node), 0);
        node.setTotalSimulations(1);
        node.setNumberOfWin(1);
        parent.setTotalSimulations(10);
        assertNotEquals(1, tester.UCT(node), 0);
    }

    @Test
    public void testSelect() {
        MCTS tester = new MCTS();
        MCTSNode node = new MCTSNode();
        assertEquals(node, tester.select(node));
        node.getMctsChildren().add(new MCTSNode());
        assertEquals(node.getMctsChildren().get(0), tester.select(node));
    }

    @Test
    public void testExpansion() {
        MCTSNode node = new MCTSNode();
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        node.setBoard(board);
        Player one = new HumanPlayer();
        Player two = new HumanPlayer();
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        MCTS tester = new MCTS(100, one, two, node.getBoard());
        node.setDepth(0);
        MCTSNode expandNode = tester.expansion(node, board);
        assertEquals(4 , node.getMctsChildren().size());
    }

    @Test
    public void testSimulation() {
        MCTSNode node = new MCTSNode();
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        node.setBoard(board);
        Player one = new HumanPlayer();
        Player two = new HumanPlayer();
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        MCTS tester = new MCTS(100, one, two, node.getBoard());
        node.setDepth(0);
        MCTSNode expandNode = tester.expansion(node, board);
        try {
            boolean simulation = tester.simulation(expandNode);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCountSpaces() {
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        Player one = new HumanPlayer();
        Player two = new HumanPlayer();
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        MCTS tester = new MCTS(100, one, two, board);
        assertEquals(2, tester.countSpaces(board, BoardSpace.SpaceType.BLACK));
    }

    @Test
    public void testBackPropagation() {
        MCTSNode node = new MCTSNode();
        MCTS tester = new MCTS();
        assertEquals(0 , node.getNumberOfWin());
        tester.backpropagation(node, true);
        assertEquals(1 , node.getNumberOfWin());
    }

    @Test
    public void testFutureBoard() {
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        Player one = new HumanPlayer();
        Player two = new HumanPlayer();
        one.setColor(BoardSpace.SpaceType.BLACK);
        two.setColor(BoardSpace.SpaceType.WHITE);
        MCTS tester = new MCTS();
        Map<BoardSpace, List<BoardSpace>> moves = one.getAvailableMoves(board);
        for (BoardSpace i : moves.keySet()) {
            assertEquals(4, tester.countSpaces(tester.futureBoard(board, i, moves, one), BoardSpace.SpaceType.BLACK));
        }
    }

    @Test
    public void testGetCopyBoard() {
        MCTS tester = new MCTS();
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        assertEquals(8 ,tester.getCopyBoard(board).length);
        assertEquals(BoardSpace.SpaceType.WHITE ,tester.getCopyBoard(board)[3][3].getType());
    }

    @Test
    public void testToString() {
        MCTS tester = new MCTS();
        BoardSpace[][] board;
        board = new BoardSpace[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
            }
        }
        assertNotEquals("", tester.toString(board));
    }
}