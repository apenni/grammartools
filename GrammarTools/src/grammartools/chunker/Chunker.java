package grammartools.chunker;

/**
 * Interface for performing phrase chunking
 */
public interface Chunker 
{
    /**
     * Chunks a tokenized and tagged sentence
     * @param tokens    the tokens for the sentence
     * @param tags      the tags for the sentence
     * @return          a chunked sentence
     */
    Chunking chunk( String[] tokens, String[] tags );
    
    /**
     * Chunks a tokenized and tagged sentence
     * @param tokens    the tokens for the sentence
     * @param tags      the tags for the sentence
     * @param nBest     retrieve the n-best possible chunkings
     * @return          the n-best chunkings for the sentence
     */
    Chunking[] chunk( String[] tokens, String[] tags, int nBest );
}
