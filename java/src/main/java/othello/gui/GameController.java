package othello.gui;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javafx.scene.shape.Circle;
import othello.gamelogic.*;

import java.util.List;
import java.util.Map;

/**
 * Manages the interaction between model and view of the game.
 */
public class GameController  {

    // FXML Variables to manipulate UI components
    @FXML
    private Label turnLabel;

    @FXML
    private Pane gameBoard;

    @FXML
    private Circle turnCircle;

    @FXML
    private Button computerTurnBtn;

    // Private variables
    private OthelloGame og;
    private int skippedTurns;
    private GUISpace[][] guiBoard;

    /**
     * Starts the game, called after controller initialization  in start method of App.
     * Sets the 2 players, their colors, and creates an OthelloGame for logic handling.
     * Then, shows the first move, beginning the game "loop".
     * @param arg1 type of player for player 1, either "human" or some computer strategy
     * @param arg2 type of player for player 2, either "human" or some computer strategy
     */
    public void initGame(String arg1, String arg2) {
        Player playerOne;
        Player playerTwo;
        // Player 1
        if (arg1.equals("human")) {
            playerOne = new HumanPlayer();
        } else {
            playerOne = new ComputerPlayer(arg1);
        }

        // Player 2
        if (arg2.equals("human")) {
            playerTwo = new HumanPlayer();
        } else {
            playerTwo = new ComputerPlayer(arg2);
        }

        // Set Colors
        playerOne.setColor(BoardSpace.SpaceType.BLACK);
        playerTwo.setColor(BoardSpace.SpaceType.WHITE);

        // Make a new game, create the visual board and display it with initial spaces
        og = new OthelloGame(playerOne, playerTwo);
        guiBoard = new GUISpace[8][8];
        displayBoard();
        initSpaces();

        // Player 1 starts the game
        turnText(playerOne);
        takeTurn(playerOne);
    }

    /**
     * Displays the board initially, adding the GUI squares into the window.
     * Also adds the initial state of the board with black and white taking spaces at the center.
     */
    @FXML
    protected void displayBoard() {
        BoardSpace[][] board = og.getBoard();
        for (BoardSpace[] spaces : board) {
            for (BoardSpace space : spaces) {
                GUISpace guiSpace = new GUISpace(space.getX(), space.getY(), space.getType());
                Pane square = guiSpace.getSquare();
                gameBoard.getChildren().add(square);
                guiBoard[space.getX()][space.getY()] = guiSpace;
            }
        }
    }

    /**
     * Clears the board visually, called every time the board is redisplayed after the first time
     */
    @FXML
    protected void clearBoard() {
        BoardSpace[][] board = og.getBoard();
        for (BoardSpace[] spaces : board) {
            for (BoardSpace space : spaces) {
                GUISpace guiSpace = guiBoard[space.getX()][space.getY()];
                Pane square = guiSpace.getSquare();
                gameBoard.getChildren().remove(square);
            }
        }
    }

    /**
     * Sets the initial state of the Othello board
     */
    @FXML
    protected void initSpaces(){
        // Initial spaces
        guiBoard[3][3].addOrUpdateDisc(BoardSpace.SpaceType.WHITE);
        guiBoard[4][4].addOrUpdateDisc(BoardSpace.SpaceType.WHITE);
        guiBoard[3][4].addOrUpdateDisc(BoardSpace.SpaceType.BLACK);
        guiBoard[4][3].addOrUpdateDisc(BoardSpace.SpaceType.BLACK);
    }

    /**
     * Displays the score of the board and the current turn.
     */
    @FXML
    protected void turnText(Player player) {
        String humanOrCom = player instanceof HumanPlayer ? "(Human)\n" : "(Computer)\n";
        turnCircle.setFill(player.getColor().fill());
        turnLabel.setText(
                player.getColor() + "'s Turn\n" + humanOrCom + "Score: \n" +
                        og.getPlayerOne().getColor() + ": " + og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + " - " +
                        og.getPlayerTwo().getColor() + ": " + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
    }

    /**
     * Displays the score of the board.
     */
    @FXML
    protected void skipTurnText(Player player) {
        turnCircle.setFill(otherPlayer(player).getColor().fill());
        turnLabel.setText(
                "Skipped " + player.getColor() + " due to no moves available! " + otherPlayer(player).getColor() + "'s Turn\n" +
                        og.getPlayerOne().getColor() + ": " + og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + " - " +
                        og.getPlayerTwo().getColor() + ": " + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
    }

    /**
     * Either shows moves for humans or makes a decision for a computer.
     * @param player player to take a turn for, whether its human or computer
     */
    @FXML
    protected void takeTurn(Player player) {
        if (player instanceof HumanPlayer human) {
            computerTurnBtn.setVisible(false);
            showMoves(human);
        } else if (player instanceof ComputerPlayer computer) {
            computerTurnBtn.setVisible(true);
            computerTurnBtn.setOnAction(actionEvent -> {
                computerDecision(computer);
            });
        }
    }

    /**
     * Shows the current moves possible for the current board configuration.
     * If availableMoves is null, the getAvailableMoves method is likely not implemented yet.
     * If availableMoves is empty (no moves found, or full board), the game ends.
     * @param player player to show moves for
     */
    @FXML
    protected void showMoves(HumanPlayer player) {
        Map<BoardSpace, List<BoardSpace>> availableMoves = og.getAvailableMoves(player);
        if (availableMoves == null) {
            turnLabel.setText("Null move found for \n" + player.getColor() + "! \n Please implement \ngetAvailableMoves()!");
        } else if (availableMoves.size() == 0) {
            if (og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size() != 64 && skippedTurns != 2) {
                skipTurnText(player);

                skippedTurns++; // Modified the starter code

                takeTurn(otherPlayer(player));
                //skippedTurns++;
            } else if (skippedTurns == 2 || og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size() == 64){
                gameOver();
            }

        } else {
            skippedTurns = 0;
            for (BoardSpace destination : availableMoves.keySet()) {
                // Attach hover listener (Enter, Exit) to each Pane
                GUISpace guiSpace = guiBoard[destination.getX()][destination.getY()];
                Pane currPane = guiSpace.getSquare();
                guiSpace.setBgColor(Color.LIGHTYELLOW);
                EventHandler<MouseEvent> enter = event -> guiSpace.setBgColor(Color.LIME);
                EventHandler<MouseEvent> exit = event -> guiSpace.setBgColor(Color.LIGHTYELLOW);
                currPane.addEventHandler(MouseEvent.MOUSE_ENTERED, enter);
                currPane.addEventHandler(MouseEvent.MOUSE_EXITED, exit);
                // Click removes hovers
                currPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    currPane.removeEventHandler(MouseEvent.MOUSE_ENTERED, enter);
                    currPane.removeEventHandler(MouseEvent.MOUSE_EXITED, exit);
                    selectSpace(player, availableMoves, destination);
                });
            }
        }

    }

    /**
     * Gets the computer decision, then selects the space.
     * @param player a reference to the current computer player (could be player 1 or 2)
     */
    @FXML
    protected void computerDecision(ComputerPlayer player) {
        Map<BoardSpace, List<BoardSpace>> availableMoves = og.getAvailableMoves(player);
        if (availableMoves == null) {
            turnLabel.setText("Null move found for \n" + player.getColor() + "! \n Please implement \ncomputerDecision()!");
            return;
        } else if (availableMoves.size() == 0) {
            int totalPositions = og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() +
                    og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size();
            if (totalPositions == 64) {
                gameOver();
                return;
            }

            if (skippedTurns == 2) {
                gameOver();
                return;
            } else {
                skipTurnText(player);
                skippedTurns++;
                takeTurn(otherPlayer(player));
            }
            return;
        } else {
            skippedTurns = 0;
            BoardSpace selectedDestination = og.computerDecision(player);

            if (selectedDestination != null) {
                // From all origins, path to the destination and take spaces
                og.takeSpaces(player, otherPlayer(player), availableMoves, selectedDestination);
                updateGUIBoard(player, availableMoves, selectedDestination);

                int totalPositions = og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() +
                        og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size();
                if (totalPositions == 64) {
                    gameOver();
                    return;
                }

                // Redisplay the new board
                clearBoard();
                displayBoard();

                // Next opponent turn
                turnText(otherPlayer(player));
                takeTurn(otherPlayer(player));
            }

//            // From all origins, path to the destination and take spaces
//            og.takeSpaces(player, otherPlayer(player), availableMoves, selectedDestination);
//            updateGUIBoard(player, availableMoves, selectedDestination);

//            // Redisplay the new board
//            clearBoard();
//            displayBoard();
//
//            // Next opponent turn
//            turnText(otherPlayer(player));
//            takeTurn(otherPlayer(player));
        }

    }

    /**
     * Handles what happens when a player chooses to select a certain space during their turn.
     * @param player current turn's player
     * @param availableMoves the available moves gotten from showMoves earlier
     * @param selectedDestination the selected destination space that was clicked on
     */
//    @FXML
//    protected void selectSpace(Player player, Map<BoardSpace, List<BoardSpace>> availableMoves, BoardSpace selectedDestination) {
//        // Remove other handlers by reinitializing empty spaces where they are
//        for (BoardSpace destination : availableMoves.keySet()) {
//            GUISpace guiSpace = guiBoard[destination.getX()][destination.getY()];
//            if (destination != selectedDestination) {
//                // Reinit unselected spaces, to remove event handlers
//                og.getBoard()[destination.getX()][destination.getY()] =
//                        new BoardSpace(destination.getX(), destination.getY(), BoardSpace.SpaceType.EMPTY);
//                gameBoard.getChildren().remove(guiSpace.getSquare());
//                GUISpace newGuiSpace = new GUISpace(destination.getX(), destination.getY(), BoardSpace.SpaceType.EMPTY);
//                Pane newSquare = newGuiSpace.getSquare();
//                gameBoard.getChildren().add(newSquare);
//                guiBoard[destination.getX()][destination.getY()] = guiSpace;
//            } else {
//                og.getBoard()[destination.getX()][destination.getY()] =
//                        new BoardSpace(destination.getX(), destination.getY(), player.getColor());
//                gameBoard.getChildren().remove(guiSpace.getSquare());
//                GUISpace newGuiSpace = new GUISpace(destination.getX(), destination.getY(), player.getColor());
//                Pane newSquare = newGuiSpace.getSquare();
//                gameBoard.getChildren().add(newSquare);
//                guiBoard[destination.getX()][destination.getY()] = guiSpace;
//            }
//        }
//
//        // Recolor the bg of the destination
//        GUISpace guiSpace = guiBoard[selectedDestination.getX()][selectedDestination.getY()];
//        guiSpace.setBgColor(Color.LIMEGREEN);
//
//        // From all origins, path to the destination and take spaces
//        og.takeSpaces(player, otherPlayer(player), availableMoves, selectedDestination);
//        updateGUIBoard(player, availableMoves, selectedDestination);
//
//        // Redisplay the new board
//        clearBoard();
//        displayBoard();
//
//        // Next opponent turn
//        turnText(otherPlayer(player));
//        takeTurn(otherPlayer(player));
//    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @FXML
    protected void selectSpace(Player player,
                               Map<BoardSpace,List<BoardSpace>> availableMoves,
                               BoardSpace selectedDestination) {
        // wipe hover handlers / yellow tint on the other candidate squares
        for (BoardSpace dst : availableMoves.keySet()) {
            if (dst == selectedDestination) continue;           // leave target untouched

            GUISpace gui = guiBoard[dst.getX()][dst.getY()];
            og.getBoard()[dst.getX()][dst.getY()].setType(BoardSpace.SpaceType.EMPTY);
            gameBoard.getChildren().remove(gui.getSquare());

            GUISpace fresh = new GUISpace(dst.getX(), dst.getY(), BoardSpace.SpaceType.EMPTY);
            gameBoard.getChildren().add(fresh.getSquare());
            guiBoard[dst.getX()][dst.getY()] = fresh;
        }

        // visual cue on the clicked square
        guiBoard[selectedDestination.getX()][selectedDestination.getY()]
                .setBgColor(Color.LIMEGREEN);

        // apply move + flips; this also updates playerOwnedSpaces
        og.takeSpaces(player, otherPlayer(player), availableMoves, selectedDestination);
        updateGUIBoard(player, availableMoves, selectedDestination);

        // board full -> end
        if (og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() +
                og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size() == 64) {
            gameOver();
            return;
        }

        // redraw
        clearBoard();
        displayBoard();

        // next turn
        Player opp = otherPlayer(player);
        turnText(opp);
        takeTurn(opp);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Updates the GUI Board by adding or updating discs from all origins to a given destination
     * @param player player that is taking a turn
     * @param availableMoves the list of all available destinations and origins
     * @param selectedDestination the selected destination from either user input or computer decision
     */
    @FXML
    protected void updateGUIBoard(Player player, Map<BoardSpace, List<BoardSpace>> availableMoves, BoardSpace selectedDestination) {
        List<BoardSpace> selectedOrigins = availableMoves.get(selectedDestination);
        for (BoardSpace selectedOrigin : selectedOrigins) {
            int offsetX = selectedDestination.getX() - selectedOrigin.getX();
            int offsetY = selectedDestination.getY() - selectedOrigin.getY();
            // Intercardinals
            if (Math.abs(offsetX) > 0 && Math.abs(offsetY) > 0) {
                for (int i = 0; i < Math.abs(offsetX) + 1; i++) {
                    int x = (int) (selectedOrigin.getX() + (i * Math.signum((offsetX))));
                    int y = (int) (selectedOrigin.getY() + (i * Math.signum((offsetY))));
                    guiBoard[x][y].addOrUpdateDisc(player.getColor());
                }
            } else { // Cardinals
                // Origin -> Destination
                for (int i = 0; i < Math.abs(offsetX) + 1; i++) {
                    for (int j = 0; j < Math.abs(offsetY) + 1; j++) {
                        int x = (int) (selectedOrigin.getX() + (i * Math.signum((offsetX))));
                        int y = (int) (selectedOrigin.getY() + (j * Math.signum((offsetY))));
                        guiBoard[x][y].addOrUpdateDisc(player.getColor());
                    }
                }
            }
        }
    }

    /**
     * Returns the other player given one of the player fields
     */
    @FXML
    protected Player otherPlayer(Player player) {
        if (player == og.getPlayerOne()) {
            return og.getPlayerTwo();
        } else {
            return og.getPlayerOne();
        }
    }

    /**
     * Ends the game.
     */
    @FXML
    protected void gameOver() {
        boolean p1Victory = false;
        boolean tie = false;
        if (og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() > og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size()) {
            p1Victory = true;
        } else if (og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() == og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size()) {
            tie = true;
        }
        if (tie) {
            turnLabel.setText("GAME OVER! Game Tied with scores: \n " +
                    og.getPlayerOne().getColor() + ": " + og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + " - " +
                    og.getPlayerTwo().getColor() + ": " + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
        } else if (p1Victory) {
            turnLabel.setText("GAME OVER! BLACK wins with scores: \n " +
                    og.getPlayerOne().getColor() + ": " + og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + " - " +
                    og.getPlayerTwo().getColor() + ": " + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
        } else {
            turnLabel.setText("GAME OVER! WHITE wins with scores: \n " +
                    og.getPlayerOne().getColor() + ": " + og.getPlayerOne().getPlayerOwnedSpacesSpaces().size() + " - " +
                    og.getPlayerTwo().getColor() + ": " + og.getPlayerTwo().getPlayerOwnedSpacesSpaces().size());
        }
    }
}
