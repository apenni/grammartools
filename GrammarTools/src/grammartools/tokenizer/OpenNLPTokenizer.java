package grammartools.tokenizer;

/**
 * Tokenizer based on OpenNLP
 */
public class OpenNLPTokenizer
        implements Tokenizer 
{
    public OpenNLPTokenizer( String modelFile )
            throws java.io.IOException 
    {
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        tokenizer = new opennlp.tools.tokenize.TokenizerME( 
                new opennlp.tools.tokenize.TokenizerModel(in) );
        in.close();
    }

    /**
     * Tokenizes a sentence.
     * @param sentence  the sentence to tokenize
     * @return          an array of tokens
     */
    @Override
    public String[] tokenize( String sentence )
    {
        return tokenizer.tokenize(sentence);
    }

    private final opennlp.tools.tokenize.TokenizerME tokenizer;
}
