import org.junit.Test;
import othello.gamelogic.BoardSpace;
import othello.gamelogic.MCTSNode;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class TestMCTSNode {

    @Test
    public void testSetMctsChildren() {
        MCTSNode test = new MCTSNode();
        ArrayList<MCTSNode> children = new ArrayList<>();
        children.add(new MCTSNode());
        test.setMctsChildren(children);
        assertEquals(1, test.getMctsChildren().size());
    }

    @Test
    public void testGetMctsChildren() {
        MCTSNode test = new MCTSNode();
        assertEquals(0, test.getMctsChildren().size());
    }

    @Test
    public void testSetNumberOfWin() {
        MCTSNode test = new MCTSNode();
        test.setNumberOfWin(10);
        assertEquals(10, test.getNumberOfWin());
    }

    @Test
    public void testGetNumberOfWin() {
        MCTSNode test = new MCTSNode();
        assertEquals(0, test.getNumberOfWin());
    }


    @Test
    public void testSetTotalSimulations() {
        MCTSNode test = new MCTSNode();
        test.setTotalSimulations(10);
        assertEquals(10, test.getTotalSimulations());
    }

    @Test
    public void testGetTotalSimulations() {
        MCTSNode test = new MCTSNode();
        assertEquals(0, test.getTotalSimulations());
    }

    @Test
    public void testSetParent() {
        MCTSNode test = new MCTSNode();
        MCTSNode parent = new MCTSNode();
        test.setParent(parent);
        assertNotNull(test.getParent());
    }

    @Test
    public void testGetParentTotalSimulation() {
        MCTSNode test = new MCTSNode();
        MCTSNode parent = new MCTSNode();
        parent.setTotalSimulations(100);
        test.setParent(parent);
        assertEquals(100, test.getParentTotalSimulation());
    }

    @Test
    public void testSetBoard() {
        MCTSNode test = new MCTSNode();
        test.setBoard(new BoardSpace[8][8]);
        assertNotNull(test.getBoard());
    }

    @Test
    public void testGetBoard() {
        MCTSNode test = new MCTSNode();
        test.setBoard(new BoardSpace[8][8]);
        assertEquals(8, test.getBoard().length);
    }

    @Test
    public void testSetDepth() {
        MCTSNode test = new MCTSNode();
        test.setDepth(1);
        assertEquals(1, test.getDepth());
    }

    @Test
    public void testGetDepth() {
        MCTSNode test = new MCTSNode();
        test.setDepth(5);
        assertEquals(5, test.getDepth());
    }

    @Test
    public void testSetSpace() {
        MCTSNode test = new MCTSNode();
        test.setSpace(new BoardSpace(7,7, BoardSpace.SpaceType.WHITE));
        assertEquals(BoardSpace.SpaceType.WHITE, test.getSpace().getType());
    }

    @Test
    public void testGetSpace() {
        MCTSNode test = new MCTSNode();
        test.setSpace(new BoardSpace(6,6, BoardSpace.SpaceType.BLACK));
        assertEquals(6 , test.getSpace().getX());
        assertEquals(6 , test.getSpace().getY());
    }

    @Test
    public void testSetLeaf() {
        MCTSNode test = new MCTSNode();
        test.setIsLeaf(true);
        assertTrue(test.isLeaf());
    }

    @Test
    public void testIsLeaf() {
        MCTSNode test = new MCTSNode();
        assertFalse(test.isLeaf());
    }

}