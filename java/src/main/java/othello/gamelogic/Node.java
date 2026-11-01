

package othello.gamelogic;

import java.util.ArrayList;

public class Node {

    private BoardSpace boardspace;
    private int weight;
    private Node parent;
    private ArrayList<Node> children = new ArrayList<>();
    private boolean isRoot = false;
    private boolean max = false;
    private boolean min = false;
    private int depth;
    private boolean visited = false;

    //Default constructor
    //Initialize a new node
    public Node() {}

    //Set the BoardSpace the node represented
    public void setBoardSpace(BoardSpace bs) {
        this.boardspace = bs;
    }

    //Return the BoardSpace the node currently represented
    public BoardSpace getBoardspace() {
        return this.boardspace;
    }

    //Set the weight of node
    public void setWeight(int wt) {
        this.weight = wt;
    }

    //Return the weight of node
    public int getWeight() {
        return this.weight;
    }

    //Set the children arraylist of the node
    public void setChildren(ArrayList<Node> ref) {
        this.children = ref;
    }

    //Return the children nodes of this parent node
    public ArrayList<Node> getChildren() {
        return this.children;
    }

    //Return if this node is root
    public boolean isRoot() {
        return isRoot;
    }

    //Set the node to be true if it is node, otherwise false
    public void setRoot(boolean root) {
        isRoot = root;
    }

    //Set the parent node of this node
    public void setParent(Node parent) {
        this.parent = parent;
    }

    //Return the parent node of this node
    public Node getParent() {
        return this.parent;
    }

    //Set if the node is a minimum node
    public void setMin(boolean min) {
        this.min = min;
    }

    //Return true if node is a minimum node, otherwise false
    public boolean isMin() {
        return this.min;
    }

    //Set if the node is a maximum node
    public void setMax(boolean max) {
        this.max = max;
    }

    //Return true if node is a maximum node, otherwise false
    public boolean isMax() {
        return this.max;
    }

    //Set the depth of current node
    public void setDepth(int d) {
        this.depth = d;
    }

    //Return the depth of current node
    public int getDepth() {
        return this.depth;
    }

    //Set if the node is visited to be true, otherwise false
    public void setVisited(boolean v) {
        this.visited = v;
    }

    //Return true if node is visited, otherwise false
    public boolean isVisited() {
        return this.visited;
    }
}