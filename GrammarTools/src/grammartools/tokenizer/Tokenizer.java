package grammartools.tokenizer;

/**
 * Interface for tokenizing a sentence
 */
public interface Tokenizer 
{
    /**
     * Tokenizes a sentence.
     * @param sentence  the sentence to tokenize
     * @return          an array of tokens
     */
    public String[] tokenize( String sentence );
}
