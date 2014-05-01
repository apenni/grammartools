package grammartools.tagger;

/**
 * Represents a tagged sentence.
 */
public class Tagging
{
    public Tagging( String[] tokens, String[] tags, double[] probs, String desc )
    {
        this.tokens = tokens.clone();
        this.tags   = tags.clone();
        this.probs  = probs == null ? null : probs.clone();
        this.desc   = desc;
        
        int iMin = -1, iMax = -1;
        if(this.probs != null)
        {            
            double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
            for(int i = 0; i < this.probs.length; i++)
            {
                double p = this.probs[i];
                if(p < min)
                {
                    min = p;
                    iMin = i;
                }
                if(p > max)
                {
                    max = p;
                    iMax = i;
                }
            }
        }
        this.iMin = iMin;
        this.iMax = iMax;
    }
    
    public Tagging( String[] tokens, String[] tags )
    {
        this(tokens, tags, null, null);
    }
        
    public boolean hasProbs()
    {
        return probs != null;
    }
        
    @Override
    public String toString()
    {
        String s = "";
        for( int i = 0; i < tags.length; i++ )
        {
            s += tokens[i] + "/" + tags[i];           
            if(i < tags.length-1)
                s += " ";
        }
        return s;
    }
    
    /**
     * Outputs tagging with probabilities.
     * @param probPrecision number of decimal places to show for probabilities
     * @return a string with tokens, tags, and their probabilities if they exist.
     */
    public String toString( int probPrecision )
    {
        String s = "";
        for( int i = 0; i < tags.length; i++ )
        {
            s += tokens[i] + "/" + tags[i];
            if( probs != null )
                s += "/" + new java.math.BigDecimal( probs[i] ).setScale( probPrecision, java.math.RoundingMode.UP );
            
            if(i < tags.length-1)
                s += " ";
        }
        return s;
    }
    
    public final String[] tokens;
    public final String[] tags;
    public final double[] probs;
    public final int iMin;
    public final int iMax;
    public final String desc;
}
