package grammartools.util;

/**
 * Represents features of a sentence
 *
 * @author Anthony Penniston
 */
public class ProcessedSentence {
    /**
     * Constructs ProcessedSentence with the specified features.
     * @param tokens        Array of string tokens.
     * @param parses        Array of parse trees.
     * @param taggings      Array of taggings.
     * @param grammatical   Whether or not the sentence is grammatical
     */
    public ProcessedSentence(
            String[] tokens,
            grammartools.parser.ParseTree[] parses,
            grammartools.tagger.Tagging[] taggings,
            boolean grammatical ) {
        this.tokens = tokens.clone();
        this.parses = parses.clone();
        this.taggings = taggings.clone();
        this.grammatical = grammatical;
        this.nGrammaticalErrors = UNKNOWN_ERRORS;
    }
    public ProcessedSentence(
            String[] tokens,
            grammartools.parser.ParseTree[] parses,
            grammartools.tagger.Tagging[] taggings,
            int nErrors ) {
        this.tokens = tokens.clone();
        this.parses = parses.clone();
        this.taggings = taggings.clone();
        this.nGrammaticalErrors = nErrors;
        this.grammatical = nErrors == 0;
    }

    public final String[] tokens;
    public final grammartools.parser.ParseTree[] parses;
    public final grammartools.tagger.Tagging[] taggings;
    public final boolean grammatical;
    public final int nGrammaticalErrors;
    public static final int UNKNOWN_ERRORS = -1;
}
