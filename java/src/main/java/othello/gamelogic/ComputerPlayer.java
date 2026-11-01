package othello.gamelogic;

import org.deeplearning4j.util.ModelSerializer;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Represents a computer player that will make decisions autonomously during their turns.
 * Employs a specific computer strategy passed in through program arguments.
 */
public class ComputerPlayer extends Player{
    private AIStrategy theComputerStrategy;
    private String strategy;

    public ComputerPlayer(String strategyName) {
        // PART 2
        // TODO: Use the strategyName input to create a specific strategy class for this computer
        // This input should match the ones specified in App.java!

        // First convert the strategy name to lowercase
        this.strategy = strategyName.toLowerCase();

        // For the custom ML strategy, specify the gamma and alpha parameter values
        if (this.strategy.equals("custom")) {
            theComputerStrategy = loadCustomStrategy();
        } else if (this.strategy.equals("minimax")) {
            theComputerStrategy = null;
        } else if (this.strategy.equals("mcts")) {
            theComputerStrategy = null;
        }
    }

    public ComputerPlayer(TDLearningStrategy strategy) {
        this.strategy = "custom";
        this.theComputerStrategy = Objects.requireNonNull(strategy, "strategy");
    }

    private static TDLearningStrategy loadCustomStrategy() {
        String configured = System.getProperty("othello.model");
        File model = new File(configured == null ? "models/othello-td.zip" : configured);
        if (model.isFile()) {
            try {
                TDLearningStrategy strategy = new TDLearningStrategy(
                        ModelSerializer.restoreMultiLayerNetwork(model), 0.99, 0.001);
                System.out.println("Loaded TD model: " + model.getPath());
                return strategy;
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot load TD model: " + model.getPath(), failure);
            }
        }
        if (configured != null) {
            throw new IllegalArgumentException("TD model does not exist: " + configured);
        }
        return new TDLearningStrategy(0.99, 0.001);
    }


    // PART 2
    // TODO: implement a method that returns a BoardSpace that a strategy selects

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * The actual move will be delegated by this method to the specific AI strategy
     * @param board the Othello game board
     * @return BoardSpace by AI
     */
    public BoardSpace computerMove(BoardSpace[][] board) {
        if (theComputerStrategy == null) {
            return null;
        }

        return theComputerStrategy.computerMove(board, this);
    }


    // The two argument version -- convenient for the ML custom strategy
    public BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer) {
        if (theComputerStrategy == null) {
            return null;
        }

        if (strategy.equals("custom")) {
            return theComputerStrategy.computerMove(board, actingPlayer);
        }

        return null;
    }


    // Changed March's selectedStrategy to computerMove -- only a name change
    // The four-argument version -- convenient for minimax
    public BoardSpace computerMove(BoardSpace[][] board, Player self, Player op, int maxDepth) {
        if (this.strategy.equals("minimax")) {
            Minimax minimax = new Minimax();
            Node root = new Node();
            minimax.buildTree(board, self, op,0, root, maxDepth);
            BoardSpace next = minimax.minimaxStrategyWithMaxDepth(root);

            if (next == null) {
                System.out.println("There is no valid move for the minimax strategy");
                return null;
            }

            return next;
        }
        return null;
    }


    // Changed March's selectedStrategy to computerMove -- only a name change
    // The five-argument version -- convenient for MCTS
    public BoardSpace computerMove(BoardSpace[][] board, Player self, Player op, int maxDepth, int epoch) {
        if (this.strategy.equals("mcts")) {
            MCTS mcts = new MCTS(epoch, self, op, board);
            BoardSpace next = mcts.MCTS_Strategy();
            return next;
        }
        return null;
    }


    public AIStrategy getComputerStrategy() {
        return theComputerStrategy;
    }

    public void setCustomStrategy(TDLearningStrategy customStrategy) {
        this.theComputerStrategy = customStrategy;
    }

    public String getStrategy() {
        return this.strategy;
    }
}
