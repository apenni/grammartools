package grammartools.sentence;

/**
 * Sentence splitter based on OpenNLP
 */
public class OpenNLPSentenceSplitter
        implements SentenceSplitter 
{
    public OpenNLPSentenceSplitter( String modelFile )
            throws java.io.IOException 
    {
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        sentenceDetector = new opennlp.tools.sentdetect.SentenceDetectorME( 
                new opennlp.tools.sentdetect.SentenceModel(in) );
        in.close();
    }

    /**
     * Splits text into sentences
     * @param text the string text to split
     * @param sentenceDelimiter a string to split on, or null for to use default delimiters
     * @return an array of sentences
     */
    @Override
    public String[] split(String text)
    {
        return sentenceDetector.sentDetect( text );
    }
    
    private final opennlp.tools.sentdetect.SentenceDetectorME sentenceDetector;
}
