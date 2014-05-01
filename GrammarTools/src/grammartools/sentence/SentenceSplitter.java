package grammartools.sentence;

/**
 * Interface for sentence splitting
 */
public interface SentenceSplitter 
{
    /**
     * Splits text into sentences
     * @param text the string text to split
     * @return an array of sentences
     */
    public String[] split( String text );
}
