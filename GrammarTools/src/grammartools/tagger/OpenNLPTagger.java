package grammartools.tagger;

/**
 * Tagger based on OpenNLP maximum entropy
 */
public class OpenNLPTagger 
        implements Tagger 
{
    public OpenNLPTagger( String modelFile ) 
            throws java.io.IOException 
    {    
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        
        tagger = new opennlp.tools.postag.POSTaggerME( 
                new opennlp.tools.postag.POSModel(in) );
                //, beamSize, cacheSize, new opennlp.tools.postag.POSDictionary( dictionaryFile, caseSensitive ) );

        in.close();
    }
    
    /**
     * Tags a tokenized sentence into its parts-of-speech.
     * @param sentence      the tokenized sentence to tag
     * @return              the tagged sentence
     */
    @Override
    public Tagging tag( String[] tokens )
    {
        String[] tags = tagger.tag(tokens);
        double[] probs = tagger.probs();
        return new Tagging( tokens, tags, probs, desc );
    }
    
    /**
     * Tags a tokenized     sentence into its parts-of-speech.
     * @param sentence      tokenized sentence to tag
     * @param nBest         retrieve the n-best number of taggings
     * @return              the n-best taggings for the sentence
     */
    @Override
    public Tagging[] tag(String[] tokens, int nBest)
    {
        if( nBest < 2 )
        {
            return new Tagging[] { tag(tokens) };
        }
        
        opennlp.tools.util.Sequence[] topk = tagger.topKSequences(tokens);
        Tagging[] taggings = new Tagging[ Math.min(topk.length, nBest) ];
        
        for( int i = 0; i < taggings.length; i++ )
        {
            taggings[i] = new Tagging( tokens, topk[i].getOutcomes().toArray(new String[0]), topk[i].getProbs(), desc );
        }
        
        return taggings;
    }

    private final opennlp.tools.postag.POSTaggerME tagger;
    private final static String desc = "OpenNLP";
}
