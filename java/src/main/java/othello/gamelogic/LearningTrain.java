package othello.gamelogic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

public class LearningTrain {
    private static final double GAMMA = 0.99;
    private static final double ALPHA = 0.001;
    private static final int MAX_MOVES = 60;

    private final long seed;
    private TDLearningStrategy trained;

    public LearningTrain() {
        this(27);
    }

    public LearningTrain(long seed) {
        this.seed = seed;
    }

    private OthelloGame newGame(ComputerPlayer black, ComputerPlayer white) {
        black.setColor(BoardSpace.SpaceType.BLACK);
        white.setColor(BoardSpace.SpaceType.WHITE);
        black.getPlayerOwnedSpacesSpaces().clear();
        white.getPlayerOwnedSpacesSpaces().clear();
        return new OthelloGame(black, white);
    }

    private double singleGame(ComputerPlayer black, ComputerPlayer white) {
        return playGame(newGame(black, white), black, white, true).meanError();
    }

    GameResult playGame(OthelloGame game, ComputerPlayer black, ComputerPlayer white,
                        boolean learning) {
        rebuildOwnership(game.getBoard(), black, white);
        BoardSpace[][] blackAfterstate = null;
        BoardSpace[][] whiteAfterstate = null;
        ComputerPlayer current = black;
        int moves = 0;
        int passes = 0;
        int consecutivePasses = 0;
        long updates = 0;
        double error = 0.0;

        while (consecutivePasses < 2) {
            ComputerPlayer opponent = current == black ? white : black;
            Map<BoardSpace, List<BoardSpace>> legalMoves = game.getAvailableMoves(current);
            if (legalMoves.isEmpty()) {
                passes++;
                consecutivePasses++;
                current = opponent;
                continue;
            }
            if (moves >= MAX_MOVES) {
                throw new IllegalStateException("A game exceeded 60 legal placements");
            }
            consecutivePasses = 0;
            BoardSpace chosen = chooseMove(game.getBoard(), current, opponent);
            BoardSpace destination = canonicalMove(legalMoves, chosen);
            BoardSpace[][] before = getCopy(game.getBoard());
            game.takeSpaces(current, opponent, legalMoves, destination);
            if (occupied(game.getBoard()) != occupied(before) + 1) {
                throw new IllegalStateException("A legal move must fill exactly one empty square");
            }
            moves++;

            if (learning && current.getComputerStrategy() instanceof TDLearningStrategy strategy) {
                BoardSpace[][] afterstate = getCopy(game.getBoard());
                BoardSpace[][] previous = current == black ? blackAfterstate : whiteAfterstate;
                if (previous != null) {
                    error += update(strategy, previous, current, 0.0, afterstate);
                    updates++;
                }
                if (current == black) {
                    blackAfterstate = afterstate;
                } else {
                    whiteAfterstate = afterstate;
                }
            }
            current = opponent;
        }

        int blackCount = black.getPlayerOwnedSpacesSpaces().size();
        int whiteCount = white.getPlayerOwnedSpacesSpaces().size();
        if (learning) {
            double blackReward = Integer.compare(blackCount, whiteCount);
            if (black.getComputerStrategy() instanceof TDLearningStrategy strategy) {
                BoardSpace[][] last = blackAfterstate == null ? getCopy(game.getBoard()) : blackAfterstate;
                error += update(strategy, last, black, blackReward, null);
                updates++;
            }
            if (white.getComputerStrategy() instanceof TDLearningStrategy strategy) {
                BoardSpace[][] last = whiteAfterstate == null ? getCopy(game.getBoard()) : whiteAfterstate;
                error += update(strategy, last, white, -blackReward, null);
                updates++;
            }
        }
        return new GameResult(moves, passes, blackCount, whiteCount, updates, error);
    }

    private BoardSpace chooseMove(BoardSpace[][] board, ComputerPlayer current, ComputerPlayer opponent) {
        return switch (current.getStrategy()) {
            case "custom" -> current.computerMove(board, current);
            case "minimax" -> current.computerMove(board, current, opponent, 3);
            case "mcts" -> current.computerMove(board, current, opponent, -1, 100);
            default -> throw new IllegalArgumentException("Unsupported training player: " + current.getStrategy());
        };
    }

    private BoardSpace canonicalMove(Map<BoardSpace, List<BoardSpace>> legalMoves, BoardSpace chosen) {
        if (chosen != null) {
            for (BoardSpace destination : legalMoves.keySet()) {
                if (destination.getX() == chosen.getX() && destination.getY() == chosen.getY()) {
                    return destination;
                }
            }
        }
        throw new IllegalStateException("A player failed to select a legal move");
    }

    private double update(TDLearningStrategy strategy, BoardSpace[][] before, Player player,
                          double reward, BoardSpace[][] after) {
        double error = strategy.updateInternal(before, player, reward, after);
        if (!Double.isFinite(error)) {
            throw new IllegalStateException("Training produced a non-finite TD error");
        }
        return error;
    }

    private void rebuildOwnership(BoardSpace[][] board, ComputerPlayer black, ComputerPlayer white) {
        black.getPlayerOwnedSpacesSpaces().clear();
        white.getPlayerOwnedSpacesSpaces().clear();
        for (BoardSpace[] row : board) {
            for (BoardSpace square : row) {
                if (square.getType() == BoardSpace.SpaceType.BLACK) {
                    black.getPlayerOwnedSpacesSpaces().add(square);
                } else if (square.getType() == BoardSpace.SpaceType.WHITE) {
                    white.getPlayerOwnedSpacesSpaces().add(square);
                }
            }
        }
    }

    private int occupied(BoardSpace[][] board) {
        int count = 0;
        for (BoardSpace[] row : board) {
            for (BoardSpace square : row) {
                if (square.getType() == BoardSpace.SpaceType.BLACK
                        || square.getType() == BoardSpace.SpaceType.WHITE) {
                    count++;
                }
            }
        }
        return count;
    }

    public void repeatGame(int iterations) {
        try {
            trainGames(iterations, null);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void trainGames(int iterations, String checkpointPath) throws IOException {
        if (iterations < 0) {
            throw new IllegalArgumentException("The number of games must not be negative");
        }
        if (trained == null && iterations > 0) {
            trained = new TDLearningStrategy(GAMMA, ALPHA, seed);
        }
        boolean wasTraining = trained != null && trained.isTraining();
        long updates = 0;
        double totalError = 0.0;
        if (trained != null) {
            trained.setTraining(true);
        }
        try {
            for (int i = 1; i <= iterations; i++) {
                ComputerPlayer black = new ComputerPlayer(trained);
                ComputerPlayer white = new ComputerPlayer(trained);
                GameResult result = playGame(newGame(black, white), black, white, true);
                updates += result.updates();
                totalError += result.totalError();
                System.out.printf(Locale.ROOT,
                        "Game %d/%d: moves=%d passes=%d black=%d white=%d updates=%d meanAbsTdError=%.8f finiteErrors=true%n",
                        i, iterations, result.moves(), result.passes(), result.blackDiscs(),
                        result.whiteDiscs(), result.updates(), result.meanError());
                if (checkpointPath != null) {
                    saveTrainedModel(checkpointPath);
                }
            }
        } finally {
            if (trained != null) {
                trained.setTraining(wasTraining);
            }
        }
        System.out.printf(Locale.ROOT,
                "Training summary: games=%d updates=%d meanAbsTdError=%.8f finiteErrors=true seed=%d%n",
                iterations, updates, updates == 0 ? 0.0 : totalError / updates, seed);
    }

    public void saveTrainedModel(String filePath) throws IOException {
        if (trained == null) {
            System.out.println("There is no model to be saved");
            return;
        }
        Path destination = Path.of(filePath).toAbsolutePath();
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "othello-td-", ".zip");
        try {
            ModelSerializer.writeModel(trained.getCNNModel(), temporary.toFile(), true);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        System.out.println("Successfully saved the model to the specified path: " + filePath);
    }

    public void loadTrainedModel(String filePath, double gamma, double alpha) throws IOException {
        File file = new File(filePath);
        if (!file.isFile()) {
            System.out.println("The specified model cannot be found at: " + filePath);
            return;
        }
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(file, true);
        trained = new TDLearningStrategy(model, gamma, alpha);
        trained.setSeed(seed);
        System.out.println("Successfully loaded model from the specified path: " + filePath);
    }

    private BoardSpace[][] getCopy(BoardSpace[][] originalBoard) {
        BoardSpace[][] copy = new BoardSpace[originalBoard.length][];
        for (int x = 0; x < originalBoard.length; x++) {
            copy[x] = new BoardSpace[originalBoard[x].length];
            for (int y = 0; y < originalBoard[x].length; y++) {
                copy[x][y] = new BoardSpace(x, y, originalBoard[x][y].getType());
            }
        }
        return copy;
    }

    public void trainVsMinimax(int iterations) {
        if (iterations < 0) {
            throw new IllegalArgumentException("The number of games must not be negative");
        }
        if (trained == null) {
            System.out.println("There is no trained model to play against minimax");
            return;
        }
        boolean wasTraining = trained.isTraining();
        trained.setTraining(false);
        int wins = 0;
        int draws = 0;
        try {
            for (int i = 0; i < iterations; i++) {
                ComputerPlayer black = new ComputerPlayer(trained);
                ComputerPlayer white = new ComputerPlayer("minimax");
                GameResult result = playGame(newGame(black, white), black, white, false);
                if (result.blackDiscs() > result.whiteDiscs()) {
                    wins++;
                } else if (result.blackDiscs() == result.whiteDiscs()) {
                    draws++;
                }
            }
        } finally {
            trained.setTraining(wasTraining);
        }
        double winRate = iterations == 0 ? 0.0 : 100.0 * wins / iterations;
        System.out.println("The ML strategy wins a total of: " + wins + " games");
        System.out.printf(Locale.ROOT,
                "Evaluation summary: games=%d wins=%d draws=%d winRate=%.2f%% updates=0%n",
                iterations, wins, draws, winRate);
    }

    public static void main(String[] args) throws IOException {
        int games = 20;
        long seed = 27;
        String modelPath = "models/othello-td.zip";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                System.out.println("Usage: LearningTrain [--games N] [--model PATH] [--seed N]");
                return;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + args[i]);
            }
            String option = args[i];
            String value = args[++i];
            switch (option) {
                case "--games" -> games = Integer.parseInt(value);
                case "--model" -> modelPath = value;
                case "--seed" -> seed = Long.parseLong(value);
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        if (games < 0) {
            throw new IllegalArgumentException("The number of games must not be negative");
        }
        LearningTrain trainer = new LearningTrain(seed);
        if (new File(modelPath).isFile()) {
            trainer.loadTrainedModel(modelPath, GAMMA, ALPHA);
        }
        trainer.trainGames(games, modelPath);
    }

    record GameResult(int moves, int passes, int blackDiscs, int whiteDiscs,
                      long updates, double totalError) {
        double meanError() {
            return updates == 0 ? 0.0 : totalError / updates;
        }
    }
}
