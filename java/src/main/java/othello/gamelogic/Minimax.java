

package othello.gamelogic;

import java.util.List;
import java.util.Map;
import othello.Constants;

public class Minimax implements AIStrategy {

    public static void main(String[] args) {
//        Minimax test = new Minimax();
//        BoardSpace[][] board;
//        Player pc = new HumanPlayer();
//        pc.setColor(BoardSpace.SpaceType.BLACK);
//        board = new BoardSpace[8][8];
//        for (int i = 0; i < 8; i++) {
//            for (int j = 0; j < 8; j++) {
//                board[i][j] = new BoardSpace(i, j, BoardSpace.SpaceType.EMPTY);
//            }
//        }
//        board[3][3].setType(BoardSpace.SpaceType.WHITE);
//        board[4][4].setType(BoardSpace.SpaceType.WHITE);
//        board[3][4].setType(BoardSpace.SpaceType.BLACK);
//        board[4][3].setType(BoardSpace.SpaceType.BLACK);
//        //System.out.println(test.toString(board));
//        Node root = new Node();
//        Player one = new HumanPlayer();
//        Player two = new HumanPlayer();
//        one.setColor(BoardSpace.SpaceType.BLACK);
//        two.setColor(BoardSpace.SpaceType.WHITE);
//        test.buildTree(board, one, two, 0, root, 2);
//        System.out.println(test.toString(board));
//        BoardSpace best  = test.minimaxStrategyWithMaxDepth(root);
//        System.out.println(best.getX() + " " + best.getY());
    }

    //Default constructor
    public Minimax() {}

    //This method is for testing the minimax with only one-step depth
    //It is for comparing with other AI strategy and itself with other number of depth
    public BoardSpace minimaxOneStep(BoardSpace[][] board, Player pc) {
        Map<BoardSpace, List<BoardSpace>> move = pc.getAvailableMoves(board);
        BoardSpace max_boardspace = null;
        int max_value = Integer.MIN_VALUE;
        for (BoardSpace i : move.keySet()) {
            BoardSpace[][] futrue = futureBoard(board, i,move, pc);
            if (computeWeight(futrue,pc) >= max_value) {
                max_boardspace = new BoardSpace(i);
                max_value = computeWeight(futrue,pc);
            }
        }
        return max_boardspace;
    }


    //This method is the next step after buildTree()
    //Return the next optimal BoardSpace to move based on minimax algo
    //This will return the next best move with alpha-beta pruning to enhance the efficiency
    public BoardSpace minimaxStrategyWithMaxDepth(Node node) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        Node next_node = value(node, alpha, beta);
        System.out.println(next_node.getWeight());
        BoardSpace next_move = next_node.getBoardspace();
        return next_move;
    }

    //If isMin, then we need to find the min value of all its children
    //If isMax, then we need to find the max value of all its children
    public Node value(Node node, int alpha, int beta) {
        if (node.getChildren().isEmpty()) {
            return node;
        }
        if (node.isMax()) {
            return max_node(node, alpha, beta);
        } else if (node.isMin()){
            return min_node(node, alpha, beta);
        }
        return node;
    }

    //In order to get the max node in the branch
    public Node max_node(Node node, int alpha, int beta) {
        int max = Integer.MIN_VALUE;
        Node max_node = null;
        if (node.getChildren().isEmpty()) {
            return node;
        }
        for (Node successor : node.getChildren()) {
            if (value(successor, alpha, alpha).getWeight() >= max) {
                max = value(successor, alpha, beta).getWeight();
                max_node = successor;
                max_node.setWeight(max);
            }
            //Alpha-beta pruning to cut the unnecessary nodes
            if (max > beta) {
                return max_node;
            }
            //Update the new alpha if possible
            alpha = Math.max(alpha, max);
        }
        return max_node;
    }

    //In order to get the min node in the branch
    public Node min_node(Node node, int alpha, int beta) {
        int min = Integer.MAX_VALUE;
        Node min_node = null;
        if (node.getChildren().isEmpty()) {
            return node;
        }
        for (Node successor : node.getChildren()) {
            if (value(successor, alpha, beta).getWeight() <= min) {
                min = value(successor, alpha, beta).getWeight();
                min_node = successor;
                min_node.setWeight(min);
            }
            //Alpha-beta pruning to cut the unnecessary nodes
            if (min < alpha) {
                return min_node;
            }
            //Update the new beta if possible
            beta = Math.min(beta, min);
        }
        return min_node;
    }


    //Depth start from 0
    //MaxDepth starts from 2 to satisfy the minimax, at least two
    //This method is to build all the future state tree from current board state
    public void buildTree(BoardSpace[][] board, Player pc, Player op, int depth, Node root, int maxDepth) {
        if (maxDepth < 2) {
            throw new IllegalArgumentException("Error: Max Depth should be at least 2");
        }
        if (depth > maxDepth) {
            return;
        }
        BoardSpace[][] copyBoard = this.getCopyBoard(board);
        //If current node is root, set it to be root and depth 0, and go to next node
        if (depth == 0) {
            root.setRoot(true);
            root.setDepth(depth);
            root.setMax(true);
            buildTree(board, pc, op, depth + 1, root, maxDepth);
            return;
        }
        if (depth % 2 != 0) {
            //This is pc (self)
            Map<BoardSpace, List<BoardSpace>> availableMoves = pc.getAvailableMoves(copyBoard);
            if (availableMoves.isEmpty() || depth + 1 > maxDepth) {
                root.setWeight(computeWeight(copyBoard,pc));
                return;
            }
            //Add all the future moves of self player to the current node as children
            for (BoardSpace i : availableMoves.keySet()) {
                Node temp = new Node();
                temp.setBoardSpace(i);
                temp.setDepth(depth);
                temp.setMin(true);
                BoardSpace[][] future = futureBoard(copyBoard,i,availableMoves,pc);
                root.getChildren().add(temp);
                buildTree(future,pc,op,depth + 1,temp, maxDepth);
            }
        } else {
            //This is op
            Map<BoardSpace, List<BoardSpace>> availableMoves = op.getAvailableMoves(copyBoard);
            if (availableMoves.isEmpty() || depth + 1 > maxDepth) {
                root.setWeight(computeWeight(copyBoard,pc));
                return;
            }
            //Add all the future moves of opponent player to the current node as children
            for (BoardSpace i : availableMoves.keySet()) {
                Node temp = new Node();
                temp.setBoardSpace(i);
                temp.setDepth(depth);
                temp.setMax(true);
                BoardSpace[][] future = futureBoard(copyBoard,i,availableMoves,op);
                root.getChildren().add(temp);
                buildTree(future,pc,op,depth + 1,temp, maxDepth);
            }
        }
    }


    //This method is for creating a copy of current state of board
    //In order to prevent any modification for original board
    public BoardSpace[][] getCopyBoard(BoardSpace[][] original) {
        BoardSpace[][] copyBoard = new BoardSpace[original.length][original[0].length];
        for (int i = 0; i < copyBoard.length; i++) {
            for (int j = 0; j < copyBoard[0].length; j++) {
                copyBoard[i][j] = new BoardSpace(original[i][j]);
            }
        }
        return copyBoard;
    }

    //This method is to compute the weight of each node of current board state
    public int computeWeight(BoardSpace[][] board, Player pc) {
        int self_score = 0;
        int opponent_score = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j].getType() == pc.getColor()) {
                    self_score += Constants.BOARD_WEIGHTS[i][j];
                } else if (board[i][j].getType() == BoardSpace.SpaceType.EMPTY) {
                    //Do nothing
                } else {
                    opponent_score += Constants.BOARD_WEIGHTS[i][j];
                }
            }
        }
        return (self_score - opponent_score);
    }

    //This method is to show the next stage of the board by taking space on destination
    public BoardSpace[][] futureBoard(BoardSpace[][] board, BoardSpace destination, Map<BoardSpace,
            List<BoardSpace>> availableMoves, Player pc) {
        BoardSpace[][] copyBoard = getCopyBoard(board);
        List<BoardSpace> theOrigins = availableMoves.get(destination);
        if (theOrigins == null || theOrigins.isEmpty()) {
            System.out.println("Origins is null");
            return null;
        }
        for (BoardSpace eachOrigin : theOrigins) {
            // First find the direction to the destination
            int rowChange = Integer.compare(destination.getX(), eachOrigin.getX());
            int columnChange = Integer.compare(destination.getY(), eachOrigin.getY());
            // Then actually take the steps
            int newRow = eachOrigin.getX();
            int newColumn = eachOrigin.getY();
            while (true) {
                //Take space for each boardspace
                BoardSpace thePosition = copyBoard[newRow][newColumn];
                // If already reached the destination, break
                if (thePosition.getType() != pc.getColor()) {
                    thePosition.setType(pc.getColor());
                }
                if (newRow == destination.getX() &&
                        newColumn == destination.getY()) {
                    break;
                }
                // Else, continue moving in that direction towards the destination
                newRow += rowChange;
                newColumn += columnChange;
            }
        }
        return copyBoard;
    }

    //This method is for showing the board state in a clear way
    public String toString(BoardSpace[][] board) {
        String board_str = "";
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board_str += board[i][j].getType().toString().toLowerCase() + "  |";
            }
            board_str += "\n";
        }
        return board_str;
    }

    @Override
    public BoardSpace computerMove(BoardSpace[][] board, Player actingPlayer) {
        return null;
    }
}