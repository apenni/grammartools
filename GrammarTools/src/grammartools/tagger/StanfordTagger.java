package grammartools.tagger;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import java.util.List;

/**
 * Tagger based on Stanford NLP maximum entropy
 */
public class StanfordTagger 
        implements Tagger 
{
    public StanfordTagger( String modelFile ) 
            throws java.io.IOException, java.lang.ClassNotFoundException 
    {
        tagger = new edu.stanford.nlp.tagger.maxent.MaxentTagger( modelFile );
    }
  
    @Override
    public Tagging tag( String[] tokens )
    {
        List<HasWord> words = Sentence.toWordList(tokens);
        List<TaggedWord> taggedWords = tagger.tagSentence(words);
        String[] tags = new String[taggedWords.size()];
        
        int i = 0;
        for(TaggedWord tw : taggedWords)
        {
            tags[i++] = tw.tag();
        }
        
        return new Tagging( tokens, tags , null, desc );
    }

    /**
     * Tags a tokenized     sentence into its parts-of-speech.
     * @param sentence      tokenized sentence to tag
     * @param nBest         retrieve the n-best number of taggings (this parameter is ignored for Stanford NLP)
     * @return              the n-best taggings for the sentence
     */
    @Override
    public Tagging[] tag( String[] tokens, int nBest )
    {
        return new Tagging[] { tag(tokens) };
    }
   
    private final edu.stanford.nlp.tagger.maxent.MaxentTagger tagger;
    private final static String desc = "Stanford";
}
