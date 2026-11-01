package othello.gamelogic;

import java.util.*;

/**
 * Models a board of Othello.
 * Includes methods to get available moves and take spaces.
 */
public class OthelloGame {
    public static final int GAME_BOARD_SIZE = 8;

    private BoardSpace[][] board;
    private final Player playerOne;
    private final Player playerTwo;

    public OthelloGame(Player playerOne, Player playerTwo) {
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        initBoard();
    }

    public BoardSpace[][] getBoard() {
        return board;
    }

    public Player getPlayerOne() {
        return playerOne;
    }

    public Player getPlayerTwo() {
        return  playerTwo;
    }

    /**
     * Returns the available moves for a player.
     * Used by the GUI to get available moves each turn.
     * @param player player to get moves for
     * @return the map of available moves,that maps destination to list of origins
     */
    public Map<BoardSpace, List<BoardSpace>> getAvailableMoves(Player player) {
        return player.getAvailableMoves(board);
    }

    /**
     * Initializes the board at the start of the game with all EMPTY spaces.
     */
    public void initBoard() {
        board = new BoardSpace[GAME_BOARD_SIZE][GAME_BOARD_SIZE];
        for (int x = 0; x < GAME_BOARD_SIZE; x++) {
            for (int y = 0; y < GAME_BOARD_SIZE; y++) {
                board[x][y] = new BoardSpace(x, y, BoardSpace.SpaceType.EMPTY);
            }
        }

        // Modifying provided starter code
        board[3][3].setType(BoardSpace.SpaceType.WHITE);
        board[3][4].setType(BoardSpace.SpaceType.BLACK);
        board[4][3].setType(BoardSpace.SpaceType.BLACK);
        board[4][4].setType(BoardSpace.SpaceType.WHITE);

        if (playerOne.getColor() == BoardSpace.SpaceType.BLACK) {
            playerOne.getPlayerOwnedSpacesSpaces().add(board[3][4]);
            playerOne.getPlayerOwnedSpacesSpaces().add(board[4][3]);
            playerTwo.getPlayerOwnedSpacesSpaces().add(board[3][3]);
            playerTwo.getPlayerOwnedSpacesSpaces().add(board[4][4]);
        } else {
            playerOne.getPlayerOwnedSpacesSpaces().add(board[3][3]);
            playerOne.getPlayerOwnedSpacesSpaces().add(board[4][4]);
            playerTwo.getPlayerOwnedSpacesSpaces().add(board[3][4]);
            playerTwo.getPlayerOwnedSpacesSpaces().add(board[4][3]);
        }
    }

    /**
     * PART 1
     * TODO: Implement this method
     * Claims the specified space for the acting player.
     * Should also check if the space being taken is already owned by the acting player,
     * should not claim anything if acting player already owns space at (x,y)
     * @param actingPlayer the player that will claim the space at (x,y)
     * @param opponent the opposing player, will lose a space if their space is at (x,y)
     * @param x the x-coordinate of the space to claim
     * @param y the y-coordinate of the space to claim
     */
    public void takeSpace(Player actingPlayer, Player opponent, int x, int y) {
        BoardSpace thePosition = board[x][y];
        // If the target position already belongs to the acting player,
        // then simply return
        if (thePosition.getType() == actingPlayer.getColor()) {
            return;
        }

        // If the target position belongs to the opponent,
        // then remove it from the opponent's ownership
        // And give its ownership to the acting player
        if (thePosition.getType() == opponent.getColor()) {
            opponent.getPlayerOwnedSpacesSpaces().remove(thePosition);
            actingPlayer.getPlayerOwnedSpacesSpaces().add(thePosition);
            thePosition.setType(actingPlayer.getColor());
        }
        // Else if the target position does not belong to anyone,
        // then also gives its ownership to the acting player
        else if (thePosition.getType() == BoardSpace.SpaceType.EMPTY) {
            actingPlayer.getPlayerOwnedSpacesSpaces().add(thePosition);
            thePosition.setType(actingPlayer.getColor());
        }
    }

    /**
     * PART 1
     * TODO: Implement this method
     * Claims spaces from all origins that lead to a specified destination.
     * This is called when a player, human or computer, selects a valid destination.
     * @param actingPlayer the player that will claim spaces
     * @param opponent the opposing player, that may lose spaces
     * @param availableMoves map of the available moves, that maps destination to list of origins
     * @param selectedDestination the specific destination that a HUMAN player selected
     */
    public void takeSpaces(Player actingPlayer, Player opponent, Map<BoardSpace,
            List<BoardSpace>> availableMoves, BoardSpace selectedDestination) {
        // Obtain the list of origin positions given the selected destination
        List<BoardSpace> theOrigins = availableMoves.get(selectedDestination);
        if (theOrigins == null || theOrigins.isEmpty()) {
            return;
        }

        for (BoardSpace eachOrigin : theOrigins) {
            // First find the direction to the destination
            int rowChange = Integer.compare(selectedDestination.getX(), eachOrigin.getX());
            int columnChange = Integer.compare(selectedDestination.getY(), eachOrigin.getY());
            // Then actually take the steps
            int newRow = eachOrigin.getX();
            int newColumn = eachOrigin.getY();
            while (true) {
                takeSpace(actingPlayer, opponent, newRow, newColumn);
                // If already reached the destination, break
                if (newRow == selectedDestination.getX() &&
                        newColumn == selectedDestination.getY()) {
                    break;
                }
                // Else, continue moving in that direction towards the destination
                newRow += rowChange;
                newColumn += columnChange;
            }
        }
    }

    /**
     * PART 2
     * TODO: Implement this method
     * Gets the computer decision for its turn.
     * Should call a method within the ComputerPlayer class that returns a BoardSpace using a specific strategy.
     * @param computer computer player that is deciding their move for their turn
     * @return the BoardSpace that was decided upon
     */
    public BoardSpace computerDecision(ComputerPlayer computer) {
        Player self = (computer == playerOne ? playerOne : playerTwo);
        Player opponent = (computer == playerOne ? playerTwo : playerOne);

        String theStrategy = computer.getStrategy();
        Map<BoardSpace, List<BoardSpace>> move = computer.getAvailableMoves(board);

        if (theStrategy.equals("minimax")) {
            System.out.println((self == playerOne ? "Player one" : "Player Two")
                    + " just took a turn, " + computer.getColor() + " minimax strategy");
            BoardSpace next = computer.computerMove(board, self, opponent, 3);

            if (next != null) {
                System.out.println("The placed value is : " + next.getX() + " " + next.getY());
                int x_axis = next.getX();
                int y_axis = next.getY();
                for (BoardSpace i : move.keySet()) {
                    if (i.getX() == x_axis && i.getY() == y_axis) {
                        return i;
                    }
                }
            }
        } else if (theStrategy.equals("custom")) {
            System.out.println((self == playerOne ? "Player one" : "Player Two")
                    + " just took a turn, " + computer.getColor() + " ML strategy");
            BoardSpace next = computer.computerMove(board, self);

            if (next != null) {
                System.out.println("The placed value is : " + next.getX() + " " + next.getY());
                int x_axis = next.getX();
                int y_axis = next.getY();
                for (BoardSpace i : move.keySet()) {
                    if (i.getX() == x_axis && i.getY() == y_axis) {
                        return i;
                    }
                }
            }
        } else if (theStrategy.equals("mcts")) {
            System.out.println((self == playerOne ? "Player one" : "Player Two")
                    + " just took a turn, " + computer.getColor() + " MCTS strategy");
            BoardSpace next = computer.computerMove(board, self, opponent, -1, 100);

            if (next != null) {
                System.out.println("The placed value is : " + next.getX() + " " + next.getY());
                int x_axis = next.getX();
                int y_axis = next.getY();
                for (BoardSpace i : move.keySet()) {
                    if (i.getX() == x_axis && i.getY() == y_axis) {
                        return i;
                    }
                }
            }
        }

        System.out.println("Null arrived!");
        return null;
    }

}
