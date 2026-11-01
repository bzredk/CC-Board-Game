package othello.gamelogic;

public interface AIStrategy {

    /**
     *
     * @param board the Othello game board
     * @param actingPlayer the computer player that is moving
     * @return the determined board position for the computer player
     */
    BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer);
}
