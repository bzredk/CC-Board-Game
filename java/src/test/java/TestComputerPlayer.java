import org.junit.Test;
import othello.gamelogic.*;

import static org.junit.Assert.*;

public class TestComputerPlayer {
    @Test
    public void testComputerPlayerString1() {
        String theStrategy = "custom";
        ComputerPlayer computerPlayer = new ComputerPlayer(theStrategy);
        String expected = "custom";
        String actual = computerPlayer.getStrategy();
        assertEquals(expected, actual);
    }

    @Test
    public void testComputerPlayerString2() {
        String theStrategy = "minimax";
        ComputerPlayer computerPlayer = new ComputerPlayer(theStrategy);
        AIStrategy actual = computerPlayer.getComputerStrategy();
        assertNull(actual);
    }

    @Test
    public void testComputerPlayerString3() {
        String theStrategy = "mcts";
        ComputerPlayer computerPlayer = new ComputerPlayer(theStrategy);
        AIStrategy actual = computerPlayer.getComputerStrategy();
        assertNull(actual);
    }

    @Test
    public void testComputerMoveOneArg1() {
        ComputerPlayer computerPlayer = new ComputerPlayer("mcts");
        BoardSpace[][] board = new BoardSpace[8][8];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
            }
        }
        BoardSpace actual = computerPlayer.computerMove(board);
        assertNull(actual);
    }

    @Test
    public void testComputerMoveTwoArg1() {
        ComputerPlayer computerPlayer = new ComputerPlayer("custom");
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
        computerPlayer.setColor(BoardSpace.SpaceType.BLACK);

        BoardSpace actual = computerPlayer.computerMove(board, computerPlayer);
        assertNotNull(actual);
    }

    @Test
    public void testComputerMoveTwoArg2() {
        ComputerPlayer computerPlayer = new ComputerPlayer("minimax");
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
        computerPlayer.setColor(BoardSpace.SpaceType.BLACK);

        BoardSpace actual = computerPlayer.computerMove(board, computerPlayer);
        assertNull(actual);
    }

    @Test
    public void testComputerMoveThreeArg1() {
        ComputerPlayer self = new ComputerPlayer("minimax");
        self.setColor(BoardSpace.SpaceType.BLACK);
        Player opponent = new HumanPlayer();
        opponent.setColor(BoardSpace.SpaceType.WHITE);

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

        BoardSpace actual = self.computerMove(board, self, opponent, 2);
        assertNotNull(actual);
    }

    @Test
    public void testComputerMoveFiveArg1() {
        ComputerPlayer self = new ComputerPlayer("mcts");
        self.setColor(BoardSpace.SpaceType.BLACK);
        Player opponent = new HumanPlayer();
        opponent.setColor(BoardSpace.SpaceType.WHITE);

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

        BoardSpace actual = self.computerMove(board, self, opponent, -1, 99);
        assertNotNull(actual);
    }

    @Test
    public void testSetCustomStrategy1() {
        ComputerPlayer computerPlayer = new ComputerPlayer("custom");
        TDLearningStrategy theStrategy = new TDLearningStrategy(0.80, 0.001);
        computerPlayer.setCustomStrategy(theStrategy);
        assertSame(theStrategy, computerPlayer.getComputerStrategy());
    }
}
