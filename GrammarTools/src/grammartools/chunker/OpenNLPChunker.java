package grammartools.chunker;

/**
 * Chunker based on OpenNLP
 */
public class OpenNLPChunker
        implements Chunker 
{
    public OpenNLPChunker( String modelFile ) 
            throws java.io.IOException 
    {    
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        chunker = new opennlp.tools.chunker.ChunkerME(
                new opennlp.tools.chunker.ChunkerModel(in));        
        in.close();
    }

    /**
     * Chunks a tokenized and tagged sentence
     * @param tokens    the tokens for the sentence
     * @param tags      the tags for the sentence
     * @return          a chunked sentence
     */
    @Override
    public Chunking chunk( String[] tokens, String[] tags )
    {
        String[] chunks = chunker.chunk(tokens, tags);
        double[] probs = chunker.probs();
        return new Chunking( tokens, tags, chunks, probs );
    }

    /**
     * Chunks a tokenized and tagged sentence
     * @param tokens    the tokens for the sentence
     * @param tags      the tags for the sentence
     * @param nBest     retrieve the n-best possible chunkings
     * @return          the n-best chunkings for the sentence
     */
    @Override
    public Chunking[] chunk( String[] tokens, String[] tags, int nBest )
    {
        opennlp.tools.util.Sequence[] topk = chunker.topKSequences( tokens, tags );
        Chunking[] chunkings = new Chunking[ Math.min(topk.length, nBest) ];
        
        for( int i = 0; i < chunkings.length; i++ )
        {
            chunkings[i] = new Chunking( tokens, tags, topk[i].getOutcomes().toArray(new String[0]), topk[i].getProbs() );
        }
        
        return chunkings;
    }
    
    private final opennlp.tools.chunker.ChunkerME chunker;
}
