package grammartools.parser;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.LexicalizedParserQuery;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.ScoredObject;
import java.io.IOException;
import java.util.List;

/**
 * Parser that employs the Stanford NLP parser
 * @author Anthony Penniston
 */
public class StanfordParser 
        implements Parser 
{
    /**
     * @param modelFile location of models
     * @param nested    display trees as nested
     */
    public StanfordParser( String modelFile ) 
    {
        //Options op = new Options();
        //op.setOptions("-maxLength", Integer.toString(maxSentenceLength));
        //op.setOptions( "-retainTmpSubcategories" );
        lexicalizedParser = LexicalizedParser.loadModel(modelFile);
        gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
        this.nested = true;
    }

    /**
     * Parses a sentence and calculates its log probability
     * @param sentence      tokenized sentence to parse
     * @param numParses     maximum number of parses to generate
     * @param probs         generated list of log probabilities for each parse
     * @return              the parsed sentence
     * @throws IOException
     */
    public String parse( String sentence, int numParses, List<Double> probs )
            throws Exception {
        String output = "";
        for( ScoredObject<Tree> parse
                : pparse( sentence, numParses ) ) {
            output += nested ? parse.object().pennString() : parse.object() + "\n";
            if( probs != null ) {
                probs.add( parse.score() );  // may need to mod parseProbs by -1000 if unknown words are present
            }
        }
        return output.trim();
    }


    /**
     * Parses a sentence into a tree
     * @param sentence      tokenized sentence to parse
     * @param numParses     maximum number of parses to generate
     * @return              tree representation of the parse
     */
    private List<ScoredObject<Tree>> pparse( String sentence, int numParses ) 
    {
        LexicalizedParserQuery query = lexicalizedParser.parserQuery();
        if( query.parse( Sentence.toWordList(sentence) ) ) {
            return query.getKBestPCFGParses( numParses );
        } else {
            System.err.println( "parse(): sentence could not be parsed by grammar" );
            return new java.util.LinkedList<ScoredObject<Tree>>();
        }
    }

    @Override
    public StanfordParseTree parse( String[] tokens )
    {
        StanfordParseTree[] ptrees = parse( tokens, 1);
        return ptrees.length == 0 ? null : ptrees[0];
    }

    @Override
    public StanfordParseTree[] parse( String[] tokens, int nBest )
    {
        LexicalizedParserQuery query = lexicalizedParser.parserQuery();

        List<ScoredObject<Tree>> trees = 
            query.parse( Sentence.toWordList(tokens) ) ? 
                query.getKBestPCFGParses( nBest ) : new java.util.LinkedList<ScoredObject<Tree>>();
        
        StanfordParseTree[] ptrees = new StanfordParseTree[ Math.max(trees.size(), nBest) ];
        int i = 0;
        for( ScoredObject<Tree> so : trees )
        {
            ptrees[i++] = new StanfordParseTree(so.object(), so.score());
        }
        
        return ptrees;
    }
    
    public List<TypedDependency> parseDependencies(StanfordParseTree parse)
    {
        return gsf.newGrammaticalStructure(parse.getInternalTree()).typedDependenciesCCprocessed();
    }
    
    /*
    public static java.util.Set<Dependency<Label,Label,java.lang.Object>> getDependencies(Tree parse)
    {
       parse.percolateHeads(new ModCollinsHeadFinder());
       return parse.dependencies();
    }
    */

    private final LexicalizedParser lexicalizedParser;
    private final GrammaticalStructureFactory gsf;
    private final boolean nested;
}
