package grammartools.tagger;

/**
 * Interface for tagging sentences
 */
public interface Tagger 
{
    /**
     * Tags a tokenized     sentence into its parts-of-speech.
     * @param sentence      the tokenized sentence to tag
     * @return              the tagged sentence
     */
    Tagging tag( String[] tokens );
    
    /**
     * Tags a tokenized     sentence into its parts-of-speech.
     * @param sentence      tokenized sentence to tag
     * @param nBest         retrieve the n-best possible taggings
     * @return              the n-best taggings for the sentence
     */
    Tagging[] tag( String[] tokens, int nBest );
}
