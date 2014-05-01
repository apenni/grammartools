package grammartools.parser;

/**
 * Interface for handling parse trees
 *
 * @author Anthony Penniston
 */
public interface ParseTree
{
    /**
     * Performs a deep-copy of the tree.
     * @return  a cloned tree.
     */
    ParseTree   clone();
    /**
     * Gets description of parse tree, usually the name of the tool that generated it.
     * @return String description of parse tree.
     */
    String      getDesc();
    /**
     * Gets the probability associated with this parse.
     * @return The parse probability.
     */
    double      getProb();
    /**
     * Gets the node label or contents if it is a terminal
     * @return The string label or value of this node.
     */
    String      getValue();
    /**
     * Gets the labels of all child nodes which are POS tags
     * @return String labels of all nodes that are POS tags
     */
    String[]    getPosTags();
    /**
     * Gets the child nodes for this node.
     * @return The child nodes of this node.
     */
    ParseTree[] getChildren();
    /**
     * Gets number of children for this node.
     * @return The number of children in this node.
     */
    int         getNumChildren();
    /**
     * Gets the parent node for this node.
     * @return The parent node of this node.
     */
    ParseTree   getParent();
    /**
     * Determines if this tree is a root node.
     * @return Whether this node is root.
     */
    boolean     isRoot();
    /**
     * Determines if this node contains a child  node that is a clause.
     * @return Whether this node contains a clause as child.
     */
    boolean     isClausal();
    /**
     * Determines if this node is a phrase node.
     * @return Whether this node is a phrase.
     */
    //boolean     isPhrase();
    /**
     * Determines if this node is a part-of-speech node.
     * @return Whether this node is a POS tag.
     */
    boolean     isPosTag();
    /**
     * Determines if this node is a leaf. (in most cases, the same as isPosTag)
     * @return Whether this node is terminal.
     */
    boolean     isTerminal();
    /**
     * Returns a formatted string representation of the tree.
     * @return A formatted string representation of the tree.
     */
    @Override 
    String      toString();
    /**
     * Parses a string representation into a tree.
     * @param strTree the string representation of a tre
     * @return The tree represented by the string.
     * @throws java.io.IOException
     */
    ParseTree   valueOf( String strTree )
            throws java.io.IOException;
}