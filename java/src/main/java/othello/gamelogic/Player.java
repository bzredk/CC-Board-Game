package othello.gamelogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract Player class for representing a player within the game.
 * All types of Players have a color and a set of owned spaces on the game board.
 */
public abstract class Player {
    private final List<BoardSpace> playerOwnedSpaces = new ArrayList<>();
    public List<BoardSpace> getPlayerOwnedSpacesSpaces() {
        return playerOwnedSpaces;
    }

    private BoardSpace.SpaceType color;
    public void setColor(BoardSpace.SpaceType color) {
        this.color = color;
    }
    public BoardSpace.SpaceType getColor() {
        return color;
    }

    /**
     * PART 1
     * TODO: Implement this method
     * Gets the available moves for this player given a certain board state.
     * This method will find destinations, empty spaces that are valid moves,
     * and map them to a list of origins that can traverse to those destinations.
     * @param board the board that will be evaluated for possible moves for this player
     * @return a map with a destination BoardSpace mapped to a List of origin BoardSpaces.
     */
    public Map<BoardSpace, List<BoardSpace>> getAvailableMoves(BoardSpace[][] board) {
        Map<BoardSpace, List<BoardSpace>> allAvailableMoves = new HashMap<>();

        // Check if a valid game board exists
        // If doesn't exist, return the empty map
        if (board == null || board.length == 0) {
            return allAvailableMoves;
        }

        // Obtain the opponent's color
        BoardSpace.SpaceType opponent = (this.getColor() == BoardSpace.SpaceType.BLACK) ?
                BoardSpace.SpaceType.WHITE : BoardSpace.SpaceType.BLACK;

        // The set of available directions that we can make a move in the game
        int[][] availableDirections = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                // If specific position is empty ...
                if (board[x][y].getType() == BoardSpace.SpaceType.EMPTY) {
                    List<BoardSpace> theOrigins = new ArrayList<>();
                    for (int[] eachDirection : availableDirections) {
                        int xChange = eachDirection[0];
                        int yChange = eachDirection[1];

                        int newX = x + xChange;
                        int newY = y + yChange;

                        List<BoardSpace> opponentPositions = new ArrayList<>();
                        // Check the new position -- must be both within valid bounds
                        // and belong to the opponent
                        while (checkBounds(newX, newY, board.length) &&
                        board[newX][newY].getType() == opponent) {
                            // Add the opponent's positions to the list
                            opponentPositions.add(board[newX][newY]);
                            // Then we continue moving further in this direction to check more positions
                            newX += xChange;
                            newY += yChange;
                        }

                        // When we complete checking the series of opponent positions,
                        // we check if the ending position belongs to this player
                        if (!opponentPositions.isEmpty() && checkBounds(newX, newY, board.length) &&
                        board[newX][newY].getType() == this.getColor()) {
                            // Add the origin position to the list
                            theOrigins.add(board[newX][newY]);
                        }
                    }

                    // If in the end, there exists valid direction for our destination,
                    // then put the (destination, list of origin positions) into the map
                    if (!theOrigins.isEmpty()) {
                        allAvailableMoves.put(board[x][y], theOrigins);
                    }
                }
            }
        }

        return allAvailableMoves;
    }

    /**
     * Create a helper method to help us determine whether we are staying within valid board bounds
     * @param row the row that we are potentially on
     * @param col the column that we are potentially on
     * @param boardSize the size of the game board (standard Othello square game board)
     * @return the helper method returns true if we are within valid bounds, and false otherwise
     */
    private boolean checkBounds(int row, int col, int boardSize) {
        return (row >= 0 && row < boardSize && col >= 0 && col < boardSize);
    }
}
