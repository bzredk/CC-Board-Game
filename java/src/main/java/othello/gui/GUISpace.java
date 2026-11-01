package othello.gui;

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import othello.gamelogic.BoardSpace;

/**
 * Represents a space on the GUI board.
 * Includes a Pane that hosts a Rectangle for a background and a Circle for the colored disc.
 */
public class GUISpace {
    public static final int SQUARE_SIZE = 60;

    private Pane squarePane;
    private Rectangle bg;
    private Circle disc;
    private BoardSpace.SpaceType type;
    private final int x;
    private final int y;

    public GUISpace(int x, int y, BoardSpace.SpaceType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        setSquare();
        addOrUpdateDisc(type);
    }

    /**
     * Sets the position of the space in the GUI.
     */
    public void setSquare() {
        Point2D location = new Point2D(x * SQUARE_SIZE, y * SQUARE_SIZE);
        squarePane = new Pane();
        squarePane.setPrefHeight(SQUARE_SIZE);
        squarePane.setPrefWidth(SQUARE_SIZE);
        squarePane.setLayoutX(location.getX());
        squarePane.setLayoutY(location.getY());
        bg = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);
        bg.setStroke(Color.BLACK);
        bg.setFill(Color.LIMEGREEN);
        squarePane.getChildren().add(bg);
    }

    /**
     * @return the visual square Pane
     */
    public Pane getSquare() {
        return squarePane;
    }

    /**
     * Sets the fill of the background Pane
     * @param color color given to the background Pane
     */
    public void setBgColor(Color color) {
        bg.setFill(color);
    }

    /**
     * Adds a visual disc to this Pane, replacing the current disc or adding it to an empty space
     * @param type type or color of disc to add
     */
    public void addOrUpdateDisc(BoardSpace.SpaceType type) {
        if (squarePane.getChildren().contains(disc)) {
            squarePane.getChildren().remove(disc);
        }
        this.type = type;
        if (this.type == BoardSpace.SpaceType.BLACK || this.type == BoardSpace.SpaceType.WHITE) {
            disc = new Circle();
            int squareCenter = SQUARE_SIZE / 2;
            disc.setRadius(squareCenter - 5);
            disc.setFill(this.type.fill());
            disc.setStroke(Color.BLACK);
            disc.setCenterX(squareCenter);
            disc.setCenterY(squareCenter);
            squarePane.getChildren().add(disc);
        }
    }
}
