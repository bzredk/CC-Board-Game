package othello;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import othello.gui.GameController;


/**
 * Controller used to manipulate the GUI of the game.
 */
public class App extends javafx.application.Application {

    // The program arguments MUST match one of these items!
    // Edit this list to add more items!
    private final List<String> acceptedArgs = List.of("human", "minimax", "expectimax", "mcts", "custom");

    @Override
    public void start(Stage stage) throws IOException {
        Parameters params = getParameters();
        List<String> argList = params.getRaw();
        FXMLLoader fxmlLoader;
        fxmlLoader = new FXMLLoader(App.class.getResource("game-view.fxml"));
        Parent root = fxmlLoader.load();
        GameController controller = fxmlLoader.getController();
        if (argList.size() != 2) {
            System.err.println("Error: Did not provide 2 program arguments");
            System.exit(1);
        }
        if (!acceptedArgs.contains(argList.get(0)) || !acceptedArgs.contains(argList.get(1))) {
            System.err.println("Error: Arguments don't match either 'human' or a computer strategy.");
            System.exit(1);
        }
        controller.initGame(argList.get(0), argList.get(1));
        Scene scene = new Scene(root, 960, 600);
        stage.setTitle("Othello Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}