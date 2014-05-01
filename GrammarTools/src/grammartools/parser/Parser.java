package grammartools.parser;

/**
 * Interface for parsing
 */
public interface Parser 
{
    /**
     * Parses a sentence into a tree
     * @param sentence      tokenized sentence to parse
     * @return              tree representation of the parse
     */
     ParseTree   parse( String[] tokens );
     
    /**
     * Parses a sentence into a tree
     * @param sentence      tokenized sentence to parse
     * @param numParses     maximum number of parses to generate
     * @return              tree representations of the parses
     */
     ParseTree[] parse( String[] tokens, int nBest );
}
