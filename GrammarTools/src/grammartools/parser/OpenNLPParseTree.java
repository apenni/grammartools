package grammartools.parser;

import grammartools.tagger.Tagging;

/**
 * Wrapper class for OpenNLP's Parse tree
 * 
 * @author Anthony Penniston
 * @see ParseTree
 */
public class OpenNLPParseTree implements ParseTree
{
    public OpenNLPParseTree( opennlp.tools.parser.Parse parse ) {
        this.parse = parse;
    }
    public static OpenNLPParseTree[] toArray( opennlp.tools.parser.Parse[] parses ) {
        OpenNLPParseTree[] trees = new OpenNLPParseTree[parses.length];
        for( int i = 0; i < trees.length; i++ ) {
            trees[i] = new OpenNLPParseTree( parses[i] );
        }
        return trees;
    }
    @Override
    public OpenNLPParseTree clone() {
        return new OpenNLPParseTree((opennlp.tools.parser.Parse)parse.clone());
    }
    public String getDesc() {
        return DESC;
    }
    public double getProb() {
        return parse.getProb();
    }
    /**
     * Gets the probability associated with the POS tag sequence for this parse.
     * @return The probability associated with the tag sequence of this parse.
     */
    public double getTagSeqProb() {
        return parse.getTagSequenceProb();
    }
    public String getValue() {
        return parse.getChildCount() > 0 ?
            parse.getType() : parse.getHead().toString();
    }
    public String[] getPosTags() {
        opennlp.tools.parser.Parse[] tagNodes = parse.getTagNodes();
        String[] tags = new String[tagNodes.length];
        for( int i = 0; i < tags.length; ++i ) {
            tags[i] = tagNodes[i].getType();
        }
        return tags;
    }
    public Tagging getPosTagging() {
        opennlp.tools.parser.Parse[] tagNodes = parse.getTagNodes();
        String[] tokens= new String[tagNodes.length];
        String[] tags  = new String[tagNodes.length];
        double[] probs = new double[tagNodes.length];
        for( int i = 0; i < tags.length; ++i ) {
            final opennlp.tools.parser.Parse node = tagNodes[i];
            tokens[i] = node.getSpan().getCoveredText(node.getText()).toString();
            tags  [i] = node.getType();
            probs [i] = node.getProb();
        }
        return new Tagging(tokens, tags, probs, DESC);
    }
    public OpenNLPParseTree[] getChildren() {
        return toArray( parse.getChildren() );
    }
    public int getNumChildren() {
        return parse.getChildCount();
    }
    public OpenNLPParseTree getParent() {
        return new OpenNLPParseTree( parse.getParent() );
    }
    public boolean isRoot() {
        return parse.getType().equals( ROOT );
    }
    public boolean isClausal() 
    {
        if(isRoot())
        {
            for( opennlp.tools.parser.Parse p : parse.getChildren() ) 
            {
                String type = p.getType();
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
    /*
    public boolean isPhrase() {
        return parse.isChunk();
    }
    */
    public boolean isPosTag() {
        return parse.isPosTag();
    }
    public boolean isTerminal() {
        return getNumChildren() == 0;
    }
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer( parse.getText().length()*4 );
        parse.show( sb );
        return sb.toString();
    }
    public OpenNLPParseTree valueOf( String strTree ) {
        return new OpenNLPParseTree(parse.parseParse( strTree ));
    }

    public static final String DESC = "OpenNLP";
    public static final String ROOT = "TOP";
    public static final String[] CLAUSES =
            new String[]{ "S", "SBAR" , "SBARQ", "SINV", "SQ" };

    private final opennlp.tools.parser.Parse parse;
}