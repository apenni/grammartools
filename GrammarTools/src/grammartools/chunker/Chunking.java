package grammartools.chunker;

/**
 * Represents a chunked sentence.
 */
public class Chunking
{
    public Chunking( String[] tokens, String[] tags, String[] chunks, double[] probs )
    {
        this.tokens = tokens.clone();
        this.tags   = tags.clone();
        this.chunks = chunks.clone();
        this.probs  = probs == null ? null : probs.clone();
    }
    
    public boolean hasProbs()
    {
        return probs != null;
    }
    
    @Override
    public String toString()
    {
        String output = "";
        for( int i = 0; i < chunks.length; ++i ) 
        {
            /* convert internal chunk delimiters to brackets */
            if( i > 0 && !chunks[i].startsWith("I-") && !chunks[i-1].equals("O") ) 
            {
                output += "]";
            }
            
            if( chunks[i].startsWith("B-") ) 
            {
                if( i > 0 ) 
                    output += " ";

                output += "[" + chunks[i].substring( 2 );
            }
            
            output += " " + tokens[i] + "/" + tags[i];
        }
        
        if( !chunks[chunks.length-1].equals("O") ) 
        {
            output += "]";
        }

        return output;
    }
    
    
    /**
     * Outputs chunking with probabilities.
     * @param probPrecision number of decimal places to show for probabilities
     * @return a string with tokens, tags, chunks and their probabilities if they exist.
     */
    public String toString( int probPrecision )
    {
        String output = "";
        for( int i = 0; i < chunks.length; ++i ) 
        {
            /* convert internal chunk delimiters to brackets */
            if( i > 0 && !chunks[i].startsWith("I-") && !chunks[i-1].equals("O") ) 
            {
                output += "]";
            }
            
            if( chunks[i].startsWith("B-") ) 
            {
                if( i > 0 ) 
                    output += " ";

                output += "[" + chunks[i].substring( 2 ) + "/"
                       + new java.math.BigDecimal( probs[i] ).setScale( probPrecision, java.math.RoundingMode.UP );
            }
            
            output += " " + tokens[i] + "/" + tags[i];
        }
        
        if( !chunks[chunks.length-1].equals("O") ) 
        {
            output += "]";
        }

        return output;
    }
    
    public final String[] tokens;
    public final String[] tags;
    public final String[] chunks;
    public final double[] probs;
}
