




package othello.gamelogic;

import java.util.ArrayList;

public class MCTSNode {
    private BoardSpace space;
    private BoardSpace[][] board;
    private int numberOfWin;
    private int totalSimulations;
    private int parentTotalSimulation;
    private MCTSNode parent;
    private boolean isLeaf = false;
    private ArrayList<MCTSNode> mctsChildren = new ArrayList<>();
    private int depth;


    //Set the children nodes for current MCTSNode
    public void setMctsChildren(ArrayList<MCTSNode> children) {
        this.mctsChildren = children;
    }

    //Return the children nodes for current MCTSNode
    public ArrayList<MCTSNode> getMctsChildren() {
        return this.mctsChildren;
    }

    //Set true if the node is a leaf node, otherwise false
    public void setIsLeaf(boolean leaf) {
        this.isLeaf = leaf;
    }

    //Return true if node is a leaf node, otherwise false
    public boolean isLeaf() {
        return this.isLeaf;
    }

    //Set the number of wins for the node
    public void setNumberOfWin(int num) {
        this.numberOfWin = num;
    }

    //Return the number of wins for the node
    public int getNumberOfWin() {
        return this.numberOfWin;
    }

    //Set the total simulations for the node
    public void setTotalSimulations(int num) {
        this.totalSimulations = num;
    }

    //Return the total simulations for the node
    public int getTotalSimulations() {
        return this.totalSimulations;
    }

    //Set the parent node of this node
    public void setParent(MCTSNode node) {
        this.parent = node;
    }

    //Return the parent node of this node
    //If the node is root, there is no parent node, parent node = null
    public MCTSNode getParent() {
        return this.parent;
    }

    //Return the total simulation of node's parent node
    //This value is for UCT formula
    public int getParentTotalSimulation() {
        if (this.parent != null) {
            return this.parent.getTotalSimulations();
        }
        return 0;
    }

    //Set the board state represented by this node
    public void setBoard(BoardSpace[][] board) {
        this.board = board;
    }

    //Return the board state represented by this node
    public BoardSpace[][] getBoard() {
        return this.board;
    }

    //Set the depth of this node
    public void setDepth(int depth) {
        this.depth = depth;
    }

    //Return the depth of this node
    public int getDepth() {
        return this.depth;
    }

    //Set the boardSpace represented by this node
    public void setSpace(BoardSpace space) {
        this.space = space;
    }

    //Return the boardSpace represented by this node
    public BoardSpace getSpace() {
        return this.space;
    }
}