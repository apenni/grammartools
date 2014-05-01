package grammartools.parser;

/**
 * Wrapper class for Stanford's Tree tree
 *
 * @author Anthony Penniston
 * @see ParseTree
 */
public class StanfordParseTree
        implements ParseTree
{
    public StanfordParseTree( edu.stanford.nlp.trees.Tree tree, double score ) {
        this.tree = tree;
        this.score = score;
    }
    public static StanfordParseTree[] toArray( edu.stanford.nlp.trees.Tree[] trees ) {
        StanfordParseTree[] ptrees = new StanfordParseTree[trees.length];
        for( int i = 0; i < ptrees.length; i++ ) {
            ptrees[i] = new StanfordParseTree( trees[i], trees[i].score() );
        }
        return ptrees;
    }
    public static StanfordParseTree[] toArray(
            java.util.List<edu.stanford.nlp.util.ScoredObject<edu.stanford.nlp.trees.Tree>> trees ) {
        StanfordParseTree[] ptrees = new StanfordParseTree[trees.size()];
        int i = 0;
        for( edu.stanford.nlp.util.ScoredObject<edu.stanford.nlp.trees.Tree> t : trees ) {
            ptrees[i] = new StanfordParseTree( t.object(), t.score() );
            i++;
        }
        return ptrees;
    }
    @Override
    public StanfordParseTree clone() {
        return new StanfordParseTree( tree.deepCopy(), score );
    }
    public String getDesc() {
        return DESC;
    }
    public double getProb() {
        return score;
    }
    public String getValue() {
        return tree.value();
    }
    public String[] getPosTags() {
        java.util.List<edu.stanford.nlp.ling.Label> labels = tree.preTerminalYield();
        String[] tags = new String[labels.size()];
        int i = 0;
        for( edu.stanford.nlp.ling.Label label : labels ) {
            tags[i] = label.value();
            i++;
        }
        return tags;
    }
    public StanfordParseTree[] getChildren() {
        return StanfordParseTree.toArray( tree.children() );
    }
    public int getNumChildren() {
        return tree.numChildren();
    }
    public StanfordParseTree getParent() {
        return new StanfordParseTree( tree.parent(), tree.parent().score() );
    }
    public boolean isRoot() {
        return tree.value().equals( ROOT );
    }
    public boolean isClausal() 
    {
        if(isRoot())
        {
            for( edu.stanford.nlp.trees.Tree t : tree.children() ) 
            {
                String type = t.value();
                for( String clause : CLAUSES )
                    if( type.equals( clause ) )
                        return true;
            }
        }
        else
        {
            for( String clause : CLAUSES )
                if( getValue().equals( clause ) )
                    return true;
        }
        return false;
    }
    public boolean isPhrase() {
        return tree.isPhrasal();
    }
    public boolean isPosTag() {
        return tree.isPreTerminal();
    }
    public boolean isTerminal() {
        return getNumChildren() == 0;
    }
    @Override
    public String toString() {
        return tree.toString();
    }
    public StanfordParseTree valueOf( String strTree )
        throws java.io.IOException {
        edu.stanford.nlp.trees.Tree t = tree.valueOf( strTree );
        return new StanfordParseTree( t, t.score() );
    }

    edu.stanford.nlp.trees.Tree getInternalTree()
    {
        return this.tree;
    }
        
    public static final String DESC = "Stanford";
    public static final String ROOT = "ROOT";
    public static final String[] CLAUSES =
            new String[]{ "S", "SBAR" , "SBARQ", "SINV", "SQ" };

    private final edu.stanford.nlp.trees.Tree tree;
    private final double score;
}