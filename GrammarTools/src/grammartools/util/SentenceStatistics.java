package grammartools.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents statistics over sentences.
 *
 * @author Anthony Penniston
 */
public class SentenceStatistics {
    public long numSentences        = 0;
    public long numGrammatical      = 0;
    public long numParses           = 0;
    public long numOpenNLPParses    = 0;
    public long numStanfordParses   = 0;
    public long numOpenNLPClausal   = 0;
    public long numStanfordClausal  = 0;
    public double avgLength         = 0;
    public double avgOpenNLPParseProb  = 0;
    public double avgStanfordParseProb = 0;
    public final Map<String, Long> lowestTags = new HashMap<String, Long>();
    public final Map<String,Map<List<String>,Long>> constituents = new HashMap<String, Map<List<String>,Long>>();
}