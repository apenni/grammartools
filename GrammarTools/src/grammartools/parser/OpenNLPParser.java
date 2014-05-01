package grammartools.parser;

import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.util.Span;

public class OpenNLPParser
        implements Parser 
{
    public OpenNLPParser( String modelFile )
            throws java.io.IOException
    {
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        parser = ParserFactory.create( new ParserModel(in) );
        in.close();
    }

    public OpenNLPParser( String modelFile, int beamSize, double advancePercentage ) 
            throws java.io.IOException 
    {    
        java.io.InputStream in = new java.io.FileInputStream(modelFile);
        parser = ParserFactory.create( new ParserModel(in), beamSize, advancePercentage );
        in.close();
    }
    
    /**
     * Parses a sentence into a tree
     * @param sentence      tokenized sentence to parse
     * @param numParses     maximum number of parses to generate
     * @return              tree representation of the parse
    *
    private static final java.util.regex.Pattern pennTreebankBracketPattern1 = java.util.regex.Pattern.compile( "([^ ])([({)}])" );
    private static final java.util.regex.Pattern pennTreebankBracketPattern2 = java.util.regex.Pattern.compile( "([({)}])([^ ])" );

    private opennlp.tools.parser.Parse[] pparse( String sentence, int numParses ) {
        // do some initial PennTreebank preprocessing
        sentence = pennTreebankBracketPattern1.matcher( sentence ).replaceAll( "$1 $2" );
        sentence = pennTreebankBracketPattern2.matcher( sentence ).replaceAll( "$1 $2" );
        StringBuilder sb = new StringBuilder();
        java.util.ArrayList<String> tokens = new java.util.ArrayList<String>();
        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer( sentence );
        while( tokenizer.hasMoreTokens() ) {
            String token = tokenizer.nextToken();
            if( token.equals( "(" ) ) {
                token = "-LRB-";
            } else if( token.equals( ")" ) ) {
                token = "-RRB-";
            } else if( token.equals( "{" ) ) {
                token = "-LCB-";
            } else if( token.equals( "}" ) ) {
                token = "-RCB-";
            }
            tokens.add( token );
            sb.append( token ).append( " " );
        }

        String text = sb.substring( 0, sb.length() > 0 ? sb.length()-1 : 0 );

        final opennlp.tools.parser.Parse p =
                new opennlp.tools.parser.Parse( text,
                    new opennlp.tools.util.Span(0, text.length()), opennlp.tools.parser.AbstractBottomUpParser.INC_NODE, 1, 0 );

        int i = 0, start = 0;
        for( String token : tokens ) {
            p.insert( new opennlp.tools.parser.Parse( text,
                        new opennlp.tools.util.Span( start, start+token.length() ), opennlp.tools.parser.AbstractBottomUpParser.TOK_NODE, 0, i ) );
            start += token.length() + 1;
            ++i;
        }

        return numParses == 1 ?
            new opennlp.tools.parser.Parse[]{ parser.parse( p ) } :
            parser.parse( p, numParses );
    }
    */
    
   
    @Override
    public OpenNLPParseTree parse( String[] tokens )
    {
        return new OpenNLPParseTree( parser.parse( createInitialParse(tokens) ) );
    }

    @Override
    public OpenNLPParseTree[] parse( String[] tokens, int nBest )
    {
        Parse[] parses = parser.parse( createInitialParse(tokens), nBest );
        OpenNLPParseTree[] ptrees = new OpenNLPParseTree[ Math.min(parses.length, nBest) ];
        
        for( int i = 0; i < ptrees.length; i++ )
            ptrees[i] = new OpenNLPParseTree( parses[i] );
        
        return ptrees;
    }
    
    private Parse createInitialParse( String[] tokens )
    {
        StringBuilder sb = new StringBuilder();
        for( String token : tokens )
            sb.append(token).append(" ");

        String text = sb.substring(0, sb.length() < 1 ? 0 : sb.length() - 1);

        Parse p = new Parse(text, new Span(0, text.length()), AbstractBottomUpParser.INC_NODE, 0, 0);
        int start = 0, i = 0;
        for( String token : tokens )
        {
            p.insert(new Parse(text, new Span(start, start + token.length()), AbstractBottomUpParser.TOK_NODE, 0, i++));
            start += token.length() + 1;
        }

        return p;
    }

    private final opennlp.tools.parser.Parser parser;
}
