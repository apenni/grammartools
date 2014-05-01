package grammartools.sentence;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordTokenFactory;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Sentence splitter based on Stanford NLP
 */
public class StanfordSentenceSplitter
        implements SentenceSplitter
{  
    /**
     * Splits text into sentences
     * @param text the string text to split
     * @param sentenceDelimiter a string to split on, or null for to use default delimiters
     * @return an array of sentences
     */
    @Override
    public String[] split(String text)
    {
        DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
        //TokenizerFactory tf = edu.stanford.nlp.process.PTBTokenizer.

        dp.setTokenizerFactory(PTBTokenizer.factory(factory, options));
        //dp.setEscaper(escaper);
        
        List<String> sentences = new LinkedList<String>();
        for(List<HasWord> sentence : dp)
        {
            sentences.add( Sentence.listToString(sentence) );
        }
        
        return sentences.toArray(new String[0]);
    }
    
    //private static final edu.stanford.nlp.util.Function<List<HasWord>, List<HasWord>> escaper = null; //edu.stanford.nlp.process.PTBEscapingProcessor;    
    
    private final WordTokenFactory factory = new WordTokenFactory();
    private final String options = "ptb3Escaping=false,normalizeSpace=true,normalizeAmpersandEntity=true";
/*
 *  New code is encouraged to use the PTBTokenizer(Reader,LexedTokenFactory,String) constructor. The other constructors are historical. You specify the type of result tokens with a LexedTokenFactory, and can specify the treatment of tokens by mainly boolean options given in a comma separated String options (e.g., "invertible,normalizeParentheses=true"). If the String is null or empty, you get the traditional PTB3 normalization behaviour (i.e., you get ptb3Escaping=true). If you want no normalization, then you should pass in the String "ptb3Escaping=false". The known option names are:
    invertible: Store enough information about the original form of the token and the whitespace around it that a list of tokens can be faithfully converted back to the original String. Valid only if the LexedTokenFactory is an instance of CoreLabelTokenFactory. The keys used in it are: TextAnnotation for the tokenized form, OriginalTextAnnotation for the original string, BeforeAnnotation and AfterAnnotation for the whitespace before and after a token, and perhaps CharacterOffsetBeginAnnotation and CharacterOffsetEndAnnotation to record token begin/after end character offsets, if they were specified to be recorded in TokenFactory construction. (Like the String class, begin and end are done so end - begin gives the token length.)
    tokenizeNLs: Whether end-of-lines should become tokens (or just be treated as part of whitespace)
    ptb3Escaping: Enable all traditional PTB3 token transforms (like parentheses becoming -LRB-, -RRB-). This is a macro flag that sets or clears all the options below.
    americanize: Whether to rewrite common British English spellings as American English spellings
    normalizeSpace: Whether any spaces in tokens (phone numbers, fractions get turned into U+00A0 (non-breaking space). It's dangerous to turn this off for most of our Stanford NLP software, which assumes no spaces in tokens.
    normalizeAmpersandEntity: Whether to map the XML &amp; to an ampersand
    normalizeCurrency: Whether to do some awful lossy currency mappings to turn common currency characters into $, #, or "cents", reflecting the fact that nothing else appears in the old PTB3 WSJ. (No Euro!)
    normalizeFractions: Whether to map certain common composed fraction characters to spelled out letter forms like "1/2"
    normalizeParentheses: Whether to map round parentheses to -LRB-, -RRB-, as in the Penn Treebank
    normalizeOtherBrackets: Whether to map other common bracket characters to -LCB-, -LRB-, -RCB-, -RRB-, roughly as in the Penn Treebank
    asciiQuotes Whether to map quote characters to the traditional ' and "
    latexQuotes: Whether to map to ``, `, ', '' for quotes, as in Latex and the PTB3 WSJ (though this is now heavily frowned on in Unicode). If true, this takes precedence over the setting of unicodeQuotes; if both are false, no mapping is done.
    unicodeQuotes: Whether to map quotes to the range U+2018 to U+201D, the preferred unicode encoding of single and double quotes.
    ptb3Ellipsis: Whether to map ellipses to three dots (...), the old PTB3 WSJ coding of an ellipsis. If true, this takes precedence over the setting of unicodeEllipsis; if both are false, no mapping is done.
    unicodeEllipsis: Whether to map dot and optional space sequences to U+2026, the Unicode ellipsis character
    ptb3Dashes: Whether to turn various dash characters into "--", the dominant encoding of dashes in the PTB3 WSJ
    escapeForwardSlashAsterisk: Whether to put a backslash escape in front of / and * as the old PTB3 WSJ does for some reason (something to do with Lisp readers??).
    untokenizable: What to do with untokenizable characters (ones not known to the tokenizer). Six options combining whether to log a warning for none, the first, or all, and whether to delete them or to include them as single character tokens in the output: noneDelete, firstDelete, allDelete, noneKeep, firstKeep, allKeep. The default is "firstDelete".
    strictTreebank3: PTBTokenizer deliberately deviates from strict PTB3 WSJ tokenization in two cases. Setting this improves compatibility for those cases. They are: (i) When an acronym is followed by a sentence end, such as "U.S." at the end of a sentence, the PTB3 has tokens of "U.S" and ".", while by default PTBTokenizer duplicates the period returning tokens of "U.S." and ".", and (ii) PTBTokenizer will return numbers with a whole number and a fractional part like "5 7/8" as a single token (with a non-breaking space in the middle), while the PTB3 separates them into two tokens "5" and "7/8". 
 */    
}
