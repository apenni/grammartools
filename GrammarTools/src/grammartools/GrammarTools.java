package grammartools;

import alice.tuprolog.InvalidLibraryException;
import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.lib.DCGLibrary;
import edu.stanford.nlp.util.ArrayUtils;
import grammartools.chunker.Chunker;
import grammartools.chunker.Chunking;
import grammartools.chunker.OpenNLPChunker;
import grammartools.parser.OpenNLPParseTree;
import grammartools.parser.OpenNLPParser;
import grammartools.parser.ParseTree;
import grammartools.parser.Parser;
import grammartools.parser.StanfordParseTree;
import grammartools.parser.StanfordParser;
import grammartools.sentence.OpenNLPSentenceSplitter;
import grammartools.sentence.SentenceSplitter;
import grammartools.sentence.StanfordSentenceSplitter;
import grammartools.tagger.OpenNLPTagger;
import grammartools.tagger.StanfordTagger;
import grammartools.tagger.Tagger;
import grammartools.tagger.Tagging;
import grammartools.tokenizer.OpenNLPTokenizer;
import grammartools.tokenizer.StanfordTokenizer;
import grammartools.tokenizer.Tokenizer;
import grammartools.util.ProcessedSentence;
import grammartools.util.SentenceInstance;
import grammartools.util.SentenceStatistics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.*;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.NumberAgreement;
import simplenlg.features.Person;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.AbstractFileSaver;
import weka.core.converters.Loader;


/**
 * Performs various NLP and grammar analysis tasks
 * @author Anthony Penniston
 */
public class GrammarTools
{
    public interface UIWorker
    {
        void update(double progress, String message);
        boolean isCancelled();
    }

    public void initOpenNLPToolkit( String modelsPath ) 
            throws IOException
    {
        final Toolkit t = new Toolkit("OpenNLP");
        t.sentenceSplitter = new OpenNLPSentenceSplitter( modelsPath + SEP + OPENNLP_SENT_MODEL );
        t.tokenizer = new OpenNLPTokenizer( modelsPath + SEP + OPENNLP_TOKEN_MODEL );
        t.tagger    = new OpenNLPTagger( modelsPath + SEP + OPENNLP_TAG_MODEL );
        t.chunker   = new OpenNLPChunker( modelsPath + SEP + OPENNLP_CHUNK_MODEL );
        t.parser    = new OpenNLPParser( modelsPath + SEP + OPENNLP_PARSE_MODEL );
        opennlpToolkit = t;
    }
    
    public void initStanfordToolkit( String modelsPath ) 
            throws IOException, ClassNotFoundException
    {
        final Toolkit t = new Toolkit("Stanford");
        t.sentenceSplitter = new StanfordSentenceSplitter();
        t.tokenizer = new StanfordTokenizer();
        t.tagger    = new StanfordTagger( modelsPath + SEP + STANFORD_TAG_SUBPATH );
        t.chunker   = null;
        t.parser    = new StanfordParser( modelsPath + SEP + STANFORD_PARSE_SUBPATH );
        stanfordToolkit = t;
    }
        
    public Toolkit getOpenNLPToolkit()
    {
        return opennlpToolkit;
    }
    
    public Toolkit getStanfordToolkit()
    {
        return stanfordToolkit;
    }
        
    public void setUIWorker(UIWorker worker)
    {
        this.uiWorker = worker;
    }
    
    private void updateUI(double progress, String message)
    {
        if(this.uiWorker != null)
        {
            uiWorker.update(progress, message);
        }
    }
    
    private boolean isCancelledUI()
    {
        return this.uiWorker != null && this.uiWorker.isCancelled();
    }
    
    
    public static class TagOptions
    {
        public boolean showProbs;
        public int probPrecision = DEF_PROB_PRECISION;
        public int maxResults = DEF_MAX_RESULTS;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runTagTool(final String input, final TagOptions o)
    {
        for(final Toolkit kit : new Toolkit[]{opennlpToolkit, stanfordToolkit})
        {
            final String[] sentences = kit.sentenceSplitter.split( input );
            final double total = sentences.length;
            long progress = 0;

            updateUI(progress/total, kit + "\n");

            for( final String sentence : sentences )
            {
                if(isCancelledUI())
                    break;

                final Runnable r = 
                new Runnable() 
                {
                    private String output = "";
                    @Override public String toString() { return output; }
                    @Override public void run()
                    {
                        try
                        {
                            String[] tokens = kit.tokenizer.tokenize( sentence );
                            Tagging[] taggings = kit.tagger.tag( tokens, o.maxResults );

                            for( Tagging tagging : taggings )
                            {
                                output += (o.showProbs ? tagging.toString( o.probPrecision ) : tagging.toString()) + "\n";
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
      
                String output = "";
                final Thread t = new Thread(r);
                t.start();
                try 
                {
                    t.join(o.maxItemProcessTime); 
                    output = r.toString();
                    if(t.isAlive())
                    {
                        output += " <operation timed out>";
                        t.interrupt();
                    }
                }
                catch(InterruptedException e) {}
                
                updateUI(++progress/total, output);
            }
        }
    }
    
    
    public static class ChunkOptions
    {
        public boolean showProbs;
        public int probPrecision = DEF_PROB_PRECISION;
        public int maxResults = DEF_MAX_RESULTS;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runChunkTool(final String input, final ChunkOptions o)
    {
        for(final Toolkit kit : new Toolkit[]{opennlpToolkit})
        {
            final String[] sentences = kit.sentenceSplitter.split( input );
            final double total = sentences.length;
            long progress = 0;

            updateUI(progress/total, kit + "\n");

            for( final String sentence : sentences )
            {
                if(isCancelledUI())
                    break;

                final Runnable r = 
                new Runnable() 
                {
                    private String output = "";
                    @Override public String toString() { return output; }
                    @Override public void run()
                    {
                        try
                        {
                            String[] tokens = kit.tokenizer.tokenize( sentence );
                            Tagging[] taggings = kit.tagger.tag( tokens, o.maxResults );

                            for( Tagging tagging : taggings )
                            {
                                Chunking chunking = kit.chunker.chunk( tagging.tokens, tagging.tags );
                                output += (o.showProbs ? chunking.toString( o.probPrecision ) : chunking.toString()) + "\n";
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };

                String output = "";
                final Thread t = new Thread(r);
                t.start();
                try 
                { 
                    t.join(o.maxItemProcessTime); 
                    output = r.toString();
                    if(t.isAlive())
                    {
                        output += " <operation timed out>";
                        t.interrupt();
                    }
                }
                catch(InterruptedException e) {}
                
                updateUI(++progress/total, output);
            }
        }
    }
    
    public static class ParseOptions
    {
        public boolean showProbs;
        public int probPrecision = DEF_PROB_PRECISION;
        public int maxResults = DEF_MAX_RESULTS;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runParseTool(final String input, final ParseOptions o)
    {
        for(final Toolkit kit : new Toolkit[]{opennlpToolkit, stanfordToolkit})
        {
            final String[] sentences = kit.sentenceSplitter.split( input );
            final double total = sentences.length;
            long progress = 0;

            updateUI(progress/total, kit + "\n");

            for( final String sentence : sentences )
            {
                if(isCancelledUI())
                    break;

                final Runnable r = 
                new Runnable() 
                {
                    private String output = "";
                    @Override public String toString() { return output; }
                    @Override public void run()
                    {
                        try
                        {
                            String[] tokens = kit.tokenizer.tokenize( sentence );
                            ParseTree[] ptrees = kit.parser.parse( tokens, o.maxResults );

                            for( ParseTree ptree : ptrees )
                            {
                                if(o.showProbs)
                                {
                                    output += String.format("%." + o.probPrecision + "f", ptree.getProb());
                                }

                                output += ptree.toString() + "\n";
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                
                String output = "";
                final Thread t = new Thread(r);
                t.start();
                try 
                { 
                    t.join(o.maxItemProcessTime); 
                    output = r.toString();
                    if(t.isAlive())
                    {
                        output += " <operation timed out>";
                        t.interrupt();
                    }
                }
                catch(InterruptedException e) {}
                
                updateUI(++progress/total, output+"\n");
            }
        }
    }

    public static class FunctionOptions
    {
        public String functionScript;
        public String grammarFile;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runFunctionTool(final String input, final FunctionOptions o)
    {
        final String[] sentences = stanfordToolkit.sentenceSplitter.split( input );
        final double total = sentences.length;
        final DecimalFormat formatter = new DecimalFormat("0.###");
        long progress = 0;

        Prolog engine = null;
        if(o.grammarFile != null && !o.grammarFile.isEmpty())
        {
            try
            {
                engine = createGrammar(o.grammarFile);
            }
            catch(Exception ex)
            {
                engine = null;
                ex.printStackTrace();
                updateUI( 0, "<Could not load grammar file: " + o.grammarFile 
                             + "\n" + ex.getLocalizedMessage() + "\n(DCG sentence features will be missing)>\n" );
            }
        }

        final Prolog grammar = engine;
        
        for( final String sentence : sentences )
        {
            if(isCancelledUI())
                break;
                
            final Runnable r = 
            new Runnable() 
            {
                private String output = "";
                @Override public String toString() { return output; }
                @Override public void run()
                {
                    try
                    {
                        final SentenceInstance si = generateSentenceInstance(sentence, 0, getDefaultFunctionScript(), grammar);
                        final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
                        scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(si.getAttributes());
                        
                        output += sentence;
                        try
                        {
                            Double result = (Double)scriptEngine.eval(o.functionScript);
                            output += "\nvalue: " 
                                  + (result == null ? "null" : result)
                                  + "\nvars: ";

                            for(Map.Entry<String, Object> entry : scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).entrySet())
                            {
                                Object value = entry.getValue();
                                String s = value.toString();
                                // don't include internal variables
                                if(!s.matches(".+\\..+@\\w+$"))
                                {
                                    if(value instanceof Double && !((Double)value).isNaN())
                                        s = formatter.format(value);
                                    output += entry.getKey() + " = " + s + ", ";
                                }
                            }
                        }
                        catch(javax.script.ScriptException ex)
                        {
                            output += "\n" + ex.getLocalizedMessage();
                        }
                        
                        output += "\n\n";
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            String output = "";
            final Thread t = new Thread(r);
            t.start();
            try 
            { 
                t.join(o.maxItemProcessTime); 
                output = r.toString();
                if(t.isAlive())
                {
                    output += " <operation timed out>";
                    t.interrupt();
                }
            }
            catch(InterruptedException e) {}

            updateUI(++progress/total, output);
        }
    }
               
    public static String getDefaultFunctionScript()
            throws NoSuchFieldException
    {
        // force reflection to make sure the variable name and actual field correspond
        final String opennlpParseProbVar = SentenceInstance.class.getField("opennlpParseProb" ).getName();
        final String stanfordParseProbVar = SentenceInstance.class.getField("stanfordParseProb").getName();
        final String sentenceLengthVar = SentenceInstance.class.getField("nTokens").getName();
        final String minPTagProbVar = SentenceInstance.class.getField("minPTagProb").getName();
        final String kVar = "k";
       
        //(parseProb - 1) / (Math.pow( kValue, sentenceLength ) * minTagProb);
        return "// the last executed line is the returned value of the function"
               + "\nvar " + kVar + " = " + DEF_K_VALUE + ";"
               + "\n(" + opennlpParseProbVar + " - 1) / (Math.pow(" + kVar + ", " + sentenceLengthVar + ") * " + minPTagProbVar + ");";
    }
    
    public static class DatasetOptions
    {
        public static enum OutputFormat { ARFF, CSV }
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
        public double nGrammaticalErrorsPerSentence;   // if using binary attributes: 0 = correct, otherwise incorrect
        public String functionScript;
        public String grammarFile;
        public boolean useNumericAttrib;
        public boolean useBinaryAttrib;
        public boolean isAppending;
        public OutputFormat outputFormat;
        public String outputFile;
    }
    public void runDatasetTool(final String input, final DatasetOptions o)
            throws IOException
    {
        final double nGrammaticalErrors = o.nGrammaticalErrorsPerSentence;
        boolean isAppending = o.isAppending;
        final String outputFile = o.outputFile;

        final String[] sentences = stanfordToolkit.sentenceSplitter.split( input );
        final Instances data;
        final FileOutputStream fos;
        final AbstractFileSaver saver = 
                o.outputFormat == DatasetOptions.OutputFormat.ARFF ?
                new weka.core.converters.ArffSaver() :
                new weka.core.converters.CSVSaver();
        
        final File file = new File( outputFile );
        if( isAppending && file.exists() ) 
        {
            // obtain training data
            Loader loader = 
                o.outputFormat == DatasetOptions.OutputFormat.ARFF ?
                new weka.core.converters.ArffLoader() :
                new weka.core.converters.CSVLoader();

            loader.setSource(file);
            data = loader.getDataSet();
            // setting class attribute if the data format does not provide this information
            // For example, the XRFF format saves the class attribute information as well
            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);
            
            fos = new FileOutputStream(file, true);
        } 
        else 
        {
            isAppending = false;
            data = SentenceInstance.createWekaHeader(o.useBinaryAttrib, o.useNumericAttrib);
            fos = new FileOutputStream(file, false);
        }

        saver.setStructure(data);
        saver.setRetrieval(weka.core.converters.Saver.INCREMENTAL);
        
        // if appending, we don't want the saver to write the header, so dump it to a dummy stream
        if(isAppending)
        {
            OutputStream sink = new OutputStream() { @Override public void write( int b ) throws IOException { } };
            saver.setDestination(sink);
            saver.writeIncremental(new SentenceInstance().toWekaInstance(data));
            saver.getWriter().close();
            sink.close();
        }

        saver.setDestination(fos);
        
        Prolog engine = null;
        if(o.grammarFile != null && !o.grammarFile.isEmpty())
        {
            try
            {
                engine = createGrammar(o.grammarFile);
            }
            catch(Exception ex)
            {
                engine = null;
                ex.printStackTrace();
                updateUI( 0, "<Could not load grammar file: " + o.grammarFile 
                             + "\n" + ex.getLocalizedMessage() + "\n(DCG sentence features will be missing)>\n" );
            }
        }

        final Prolog grammar = engine;
        final double total = sentences.length;
        long progress = 0;

        updateUI(progress/total, "");

        for( final String sentence : sentences )
        {
            if(isCancelledUI())
                break;

            final Runnable r = 
            new Runnable() 
            {
                private String output = "";
                @Override public String toString() { return output; }
                @Override public void run()
                {
                    try
                    {
                        final SentenceInstance si = generateSentenceInstance(sentence, nGrammaticalErrors, o.functionScript, grammar);
                        final Instance i = si.toWekaInstance(data);
                        i.setDataset(data);
                        data.add(i);

                        output += sentence + "\n" + si;

                        //saver.writeBatch();
                        saver.writeIncremental(i);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace(System.err);
                    }
                }
            };

            String output = "";
            final Thread t = new Thread(r);
            t.start();
            try 
            { 
                t.join(o.maxItemProcessTime); 
                output = r.toString();
                if(t.isAlive())
                {
                    output += " <operation timed out>";
                    t.interrupt();
                }
            }
            catch(InterruptedException e) {}

            updateUI(++progress/total, output + "\n\n");
        }
        
        saver.getWriter().close();
        fos.close();
    }
    
    
    /**
     * Create a set of sentence features from a sentence.
     * @param sentence  the sentence to create features for
     * @param nGrammaticalErrors number of grammatical errors in the sentence
     * (values of zero vs non-zero can be used if using only for binary class)
     * @return  the Weka Instance generated from sentence features
     */
    private SentenceInstance generateSentenceInstance(String sentence, double nGrammaticalErrors, String functionScript, Prolog grammar)
    {
        final Tokenizer   tokenizer         = stanfordToolkit.tokenizer; // opennlpToolkit.tokenizer;
        final String[]    tokens            = tokenizer.tokenize(sentence);
        final Tagging[]   opennlpTaggings   = opennlpToolkit.tagger.tag(tokens, 2);
        final Tagging[]   stanfordTaggings  = stanfordToolkit.tagger.tag(tokens, 2);
        final ParseTree[] stanfordParses    = stanfordToolkit.parser.parse(tokens, 2);
        final ParseTree[] opennlpParses     = opennlpToolkit.parser.parse(tokens, 2);
        final ScriptEngine jsEngine         = new ScriptEngineManager().getEngineByName("JavaScript");
        final SentenceInstance si           = new SentenceInstance();

        si.isGrammatical = SentenceInstance.toBoolNominalIndex(nGrammaticalErrors == 0);
        si.nGrammaticalErrors = nGrammaticalErrors;
        si.nTokens = tokens.length;

        if(si.nTokens == 0 || opennlpTaggings.length == 0)
            return si;

        // find min, max, and first tag probs
        if(opennlpTaggings[0].tags.length > 0)
        {
            final Tagging tagging = opennlpTaggings[0];

            si.firstTag = SentenceInstance.toTagNominalIndex(tagging.tags[0]);
            si.firstTagProb = tagging.probs[0];

            int iMinTag = tagging.iMin;
            int iMaxTag = tagging.iMax;
            
            if(iMinTag >= 0)
            {
                si.minTagProb = tagging.probs[iMinTag];
                si.minTag = SentenceInstance.toTagNominalIndex(tagging.tags[iMinTag]);
            }
            if(iMaxTag >= 0)
            {
                si.maxTagProb = tagging.probs[iMaxTag];
                si.maxTag = SentenceInstance.toTagNominalIndex(tagging.tags[iMaxTag]);
            }
            
            // calculate difference in probabilities for the min & max tag probs
            si.deltaMinTagProb = opennlpTaggings.length < 2 ? 0 : opennlpTaggings[0].probs[iMinTag] - opennlpTaggings[1].probs[iMinTag];
            si.deltaMaxTagProb = opennlpTaggings.length < 2 ? 0 : opennlpTaggings[0].probs[iMaxTag] - opennlpTaggings[1].probs[iMaxTag];
            si.rangeTagProb = tagging.probs[iMaxTag] - tagging.probs[iMinTag];
        }
        
        // find min, max, and first parser tag probs
        int iMinPTag = -1, iMaxPTag = -1;
        if(opennlpParses.length > 0)
        {
            final Tagging ptagging = ((OpenNLPParseTree)opennlpParses[0]).getPosTagging();
            final Tagging ptagging2 = opennlpParses.length < 2 ? null :
                    ((OpenNLPParseTree)opennlpParses[1]).getPosTagging();
            
            si.firstPTag = SentenceInstance.toTagNominalIndex(ptagging.tags[0]);
            si.firstPTagProb = ptagging.probs[0];

            double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
            for(int i = 0; i < ptagging.tags.length; i++)
            {
                final double p = ptagging.probs[i];
                if(p < min)
                {
                    min = p;
                    iMinPTag = i;
                    si.minPTag = SentenceInstance.toTagNominalIndex(ptagging.tags[i]);
                    si.minPTagProb = p;
                }
                if(p > max)
                {
                    max = p;
                    iMaxPTag = i;
                    si.maxPTag = SentenceInstance.toTagNominalIndex(ptagging.tags[i]);
                    si.maxPTagProb = p;
                }
            }
            
            // calculate difference in probabilities for the min & max parser tag probs
            si.deltaMinPTagProb = opennlpParses.length < 2 ? 0 : ptagging.probs[iMinPTag] - ptagging2.probs[iMinPTag];
            si.deltaMaxPTagProb = opennlpParses.length < 2 ? 0 : ptagging.probs[iMaxPTag] - ptagging2.probs[iMaxPTag];
            si.rangePTagProb = ptagging.probs[iMaxPTag] - ptagging.probs[iMinPTag];
        }
        
        // check if first, min, max tags match between tagger and parser
        si.isFirstTagMismatch = SentenceInstance.toBoolNominalIndex(si.firstTag != si.firstPTag);
        si.isMinTagMismatch   = SentenceInstance.toBoolNominalIndex(si.minTag   != si.minPTag);
        si.isMaxTagMismatch   = SentenceInstance.toBoolNominalIndex(si.maxTag   != si.maxPTag);

        // ratio of opennlp tagger tags not matching parser tags,
        // avg delta between tagger tag and parser tag probs.
        if(opennlpParses.length > 0 && opennlpTaggings.length > 0)
        {
            int i;
            final Tagging tagging = opennlpTaggings[0];
            final Tagging ptagging = ((OpenNLPParseTree)opennlpParses[0]).getPosTagging();
            final String[] tags = opennlpTaggings[0].tags;
            final String[] ptags = ptagging.tags;
            si.opennlpTagMismatchTotal = 0;
            si.totDeltaTagPTagProb = 0;
            for(i = 0; i < tags.length && i < ptags.length; i++)
            {
                double d = ptagging.probs[i] - tagging.probs[i];
                if(!Double.isNaN(d) && !Double.isInfinite(d))
                {
                    si.totDeltaTagPTagProb += d;
                    si.minDeltaTagPTagProb = SentenceInstance.minOrValue(si.minDeltaTagPTagProb, d);
                    si.maxDeltaTagPTagProb = SentenceInstance.maxOrValue(si.maxDeltaTagPTagProb, d);
                }
                if(!ptags[i].equals(tags[i]))
                    si.opennlpTagMismatchTotal++;
            }
            si.avgDeltaTagPTagProb = si.totDeltaTagPTagProb / i;
            si.opennlpTagMismatchRatio = si.opennlpTagMismatchTotal / i;
        }
        // ratio of stanford tagger tags not matching parser tags
        if(stanfordParses.length > 0 && stanfordTaggings.length > 0)
        {
            int i;
            final String[] tags = stanfordTaggings[0].tags;
            final String[] ptags = stanfordParses[0].getPosTags();
            si.stanfordTagMismatchTotal = 0;
            for(i = 0; i < tags.length && i < ptags.length; i++)
            {
                if(!ptags[i].equals(tags[i]))
                    si.stanfordTagMismatchTotal++;
            }
            si.stanfordTagMismatchRatio = si.stanfordTagMismatchTotal / i;
        }
        
        // ratio of tagger tags not matching between opennlp and stanford
        if(opennlpTaggings.length > 0 && stanfordTaggings.length > 0)
        {
            int i;
            final String[] opennlpTags  = opennlpTaggings[0].tags;
            final String[] stanfordTags = stanfordTaggings[0].tags;
            si.opennlpStanfordCrossTagMismatchTotal = 0;
            for(i = 0; i < opennlpTags.length && i < stanfordTags.length; i++)
            {
                if(!opennlpTags[i].equals(stanfordTags[i]))
                    si.opennlpStanfordCrossTagMismatchTotal++;
            }
            si.opennlpStanfordCrossTagMismatchRatio = si.opennlpStanfordCrossTagMismatchTotal / i;
        }
        // ratio of parser tags not matching between opennlp and stanford
        if(opennlpParses.length > 0 && stanfordParses.length > 0)
        {
            int i;
            final String[] opennlpPTags  = opennlpParses[0].getPosTags();
            final String[] stanfordPTags = stanfordParses[0].getPosTags();
            si.opennlpStanfordCrossPTagMismatchTotal = 0;
            for(i = 0; i < opennlpPTags.length && i < stanfordPTags.length; i++)
            {
                if(!opennlpPTags[i].equals(stanfordPTags[i]))
                    si.opennlpStanfordCrossPTagMismatchTotal++;
            }
            si.opennlpStanfordCrossPTagMismatchRatio = si.opennlpStanfordCrossPTagMismatchTotal / i;
        }
        
        // ratio of parse constituents not matching between opennlp and stanford parse trees
        if(opennlpParses.length > 0 && stanfordParses.length > 0)
        {
            si.setParseTreeMismatches(opennlpParses[0], stanfordParses[0]);
        }
        
        // opennlp parse attributes
        if(opennlpParses.length > 0)
        {
            si.tagSeqProb = ((OpenNLPParseTree)opennlpParses[0]).getTagSeqProb();
            double d = opennlpParses.length < 2 ? 0 : ((OpenNLPParseTree)opennlpParses[0]).getTagSeqProb() - ((OpenNLPParseTree)opennlpParses[1]).getTagSeqProb();
            if(!Double.isNaN(d) && !Double.isInfinite(d))
                si.deltaTagSeqProb = d;

            si.opennlpParseProb = opennlpParses[0].getProb();
            si.isOpennlpClause = SentenceInstance.toBoolNominalIndex(opennlpParses[0].isClausal());
            
            d = opennlpParses.length < 2 ? 0 : opennlpParses[0].getProb() - opennlpParses[1].getProb();
            if(!Double.isNaN(d) && !Double.isInfinite(d))
                si.opennlpDeltaParseProb = d;
        }
        
        // stanford parse attributes
        if(stanfordParses.length > 0)
        {
            si.stanfordParseProb = stanfordParses[0].getProb();
            si.isStanfordClause = SentenceInstance.toBoolNominalIndex(stanfordParses[0].isClausal());
            double d = stanfordParses.length < 2 ? 0 : stanfordParses[0].getProb() - stanfordParses[1].getProb();
            if(!Double.isNaN(d) && !Double.isInfinite(d))
                si.stanfordDeltaParseProb = d;
        }
                
        // delta between opennlp and stanfard parses
        if(opennlpParses.length > 0 && stanfordParses.length > 0)
        {
            si.opennlpStanfordCrossDeltaParseProb = opennlpParses[0].getProb() - stanfordParses[0].getProb();
        }
        
        // calculate probs after various sentence mutations
        if(opennlpParses.length > 0)
        {
            generateSentenceInstanceVerbChangeAttributes(
                    si, opennlpTaggings[0],
                    (OpenNLPParseTree)opennlpParses[0],
                    (StanfordParseTree)(stanfordParses.length == 0 ? null : stanfordParses[0]));

            generateSentenceInstanceMinChangeAttributes(
                    si, iMinPTag, 
                    (OpenNLPParseTree)opennlpParses[0],
                    (StanfordParseTree)(stanfordParses.length == 0 ? null : stanfordParses[0]));

            // opennlp simplified clause attributes
            generateSentenceInstanceSimplifiedClauseAttributes(si, opennlpParses[0]);
        }
        
        // swaps & omits
        if(tokens.length > 1)
        {
            final int i = iMinPTag;
            
            // swap min tag left
            if(i > 0)
            {
                final String[] newTokens = tokens.clone();
                final String min = newTokens[i];
                newTokens[i] = newTokens[i-1];
                newTokens[i-1] = min;
                
                // ensure proper capitalization
                if(i == 1)
                {
                    final String t0 = newTokens[0];
                    final String t1 = newTokens[1];
                    newTokens[0] = Character.toUpperCase(t0.charAt(0)) + (t0.length() < 2 ? "" : t0.substring(1));
                    newTokens[1] = Character.toLowerCase(t1.charAt(0)) + (t1.length() < 2 ? "" : t1.substring(1));
                }
                                
                ParseTree newParse;
                newParse = opennlpToolkit.parser.parse(newTokens);
                if(newParse != null && opennlpParses.length > 0)
                    si.opennlpDeltaParseProbSwapMinLeft = opennlpParses[0].getProb() - newParse.getProb();
                
                newParse = stanfordToolkit.parser.parse(newTokens);
                if(newParse != null && stanfordParses.length > 0)
                    si.stanfordDeltaParseProbSwapMinLeft = stanfordParses[0].getProb() - newParse.getProb();
            }
            // swap min tag right
            if(i < tokens.length-1)
            {
                final String[] newTokens = tokens.clone();
                final String min = newTokens[i];
                newTokens[i] = newTokens[i+1];
                newTokens[i+1] = min;
                
                // ensure proper capitalization
                if(i == 0)
                {
                    final String t0 = newTokens[0];
                    final String t1 = newTokens[1];
                    newTokens[0] = Character.toUpperCase(t0.charAt(0)) + (t0.length() < 2 ? "" : t0.substring(1));
                    newTokens[1] = Character.toLowerCase(t1.charAt(0)) + (t1.length() < 2 ? "" : t1.substring(1));
                }
                
                ParseTree newParse;
                newParse = opennlpToolkit.parser.parse(newTokens);
                if(newParse != null && opennlpParses.length > 0)
                    si.opennlpDeltaParseProbSwapMinRight = opennlpParses[0].getProb() - newParse.getProb();
                
                newParse = stanfordToolkit.parser.parse(newTokens);
                if(newParse != null && stanfordParses.length > 0)
                    si.stanfordDeltaParseProbSwapMinRight = stanfordParses[0].getProb() - newParse.getProb();
            }
            // omit min tag
            if(tokens.length > 1)
            {
                final List<String> newTokens = new ArrayList<String>(Arrays.asList(tokens));
                newTokens.remove(i);

                // ensure proper capitalization
                if(i == 0)
                {
                    final String t0 = newTokens.get(0);
                    newTokens.set(0, Character.toUpperCase(t0.charAt(0)) + (t0.length() < 2 ? "" : t0.substring(1)));
                }

                final String[] t = newTokens.toArray(new String[0]);
                ParseTree newParse;
                newParse = opennlpToolkit.parser.parse(t);
                if(newParse != null && opennlpParses.length > 0)
                    si.opennlpDeltaParseProbOmitMin = opennlpParses[0].getProb() - newParse.getProb();
                
                newParse = stanfordToolkit.parser.parse(t);
                if(newParse != null && stanfordParses.length > 0)
                    si.stanfordDeltaParseProbOmitMin = stanfordParses[0].getProb() - newParse.getProb();
            }
            // omit left of min tag
            if(i > 0)
            {
                final List<String> newTokens = new ArrayList<String>(Arrays.asList(tokens));
                newTokens.remove(i-1);

                // ensure proper capitalization
                if(i == 1)
                {
                    final String t0 = newTokens.get(0);
                    newTokens.set(0, Character.toUpperCase(t0.charAt(0)) + (t0.length() < 2 ? "" : t0.substring(1)));
                }
                
                final String[] t = newTokens.toArray(new String[0]);
                ParseTree newParse;
                newParse = opennlpToolkit.parser.parse(t);
                if(newParse != null && opennlpParses.length > 0)
                    si.opennlpDeltaParseProbOmitMinLeft = opennlpParses[0].getProb() - newParse.getProb();
                
                newParse = stanfordToolkit.parser.parse(t);
                if(newParse != null && stanfordParses.length > 0)
                    si.stanfordDeltaParseProbOmitMinLeft = stanfordParses[0].getProb() - newParse.getProb();
            }
            // omit right of min tag
            if(i < tokens.length-1)
            {
                final List<String> newTokens = new ArrayList<String>(Arrays.asList(tokens));
                newTokens.remove(i+1);
                
                final String[] t = newTokens.toArray(new String[0]);
                ParseTree newParse;
                newParse = opennlpToolkit.parser.parse(t);
                if(newParse != null && opennlpParses.length > 0)
                    si.opennlpDeltaParseProbOmitMinRight = opennlpParses[0].getProb() - newParse.getProb();
                
                newParse = stanfordToolkit.parser.parse(t);
                if(newParse != null && stanfordParses.length > 0)
                    si.stanfordDeltaParseProbOmitMinRight = stanfordParses[0].getProb() - newParse.getProb();
            }
        }
        
        // calculate function values
        try
        {
            jsEngine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(si.getAttributes());
            Double result = (Double)jsEngine.eval(functionScript);
            if(result != null)
                si.funcValue = result;
        }
        catch(Exception e) { e.printStackTrace(); }
        
        // query all POS tags against grammar
        
        if(opennlpParses.length > 0 && grammar != null)
        {
            try
            {
                ParseTree p = opennlpParses[0];
                SolveInfo query = queryGrammar(grammar, p.getValue(), p.getPosTags());
                si.isDCGParsable = SentenceInstance.toBoolNominalIndex(query.isSuccess());
            }
            catch(Exception e) { e.printStackTrace(); }
        }
        /*
        if(stanfordParses.length > 0)
        {
            ParseTree p = stanfordParses[0];
            boolean isDCGparsable = queryGrammar(
                GrammarTools.toGrammarTerm(p.getValue()),
                GrammarTools.toGrammarTerms(p.getPosTags()));
            si.isDCGParsable = SentenceInstance.toBoolNominalIndex(isDCGparsable);
        }
        */
                
        return si;
    }
    
    private void setSentenceInstanceMinChangeAttributes(SentenceInstance si, String[] changes, int iMin, OpenNLPParseTree opennlpParse, StanfordParseTree stanfordParse)
    {
        final Tagging tagging = opennlpParse.getPosTagging();
        final String minToken = tagging.tokens[iMin];
        
        // generate changes               
        for(String t : changes)
        {    
            if(t != null && !t.equalsIgnoreCase(minToken))
            {
                ParseTree newParse;
                String[] newTokens = tagging.tokens.clone();
                newTokens[iMin] = t;

                // choose the best change seen so far
                newParse = opennlpToolkit.parser.parse(newTokens);
                if(newParse != null && opennlpParse != null)
                {
                    double d = opennlpParse.getProb() - newParse.getProb();
                    if(!Double.isNaN(d) && !Double.isInfinite(d))
                        si.deltaMinChangeOpennlpParseProb = SentenceInstance.minOrValue(si.deltaMinChangeOpennlpParseProb, d);
                    
                    d = opennlpParse.getTagSeqProb() - ((OpenNLPParseTree)newParse).getTagSeqProb();
                    if(!Double.isNaN(d) && !Double.isInfinite(d))
                        si.deltaMinChangeTagSeqProb = SentenceInstance.minOrValue(si.deltaMinChangeTagSeqProb, d);
                }

                newParse = stanfordToolkit.parser.parse(newTokens);
                if(newParse != null && stanfordParse != null)
                {
                    double d = stanfordParse.getProb() - newParse.getProb();
                    if(!Double.isNaN(d) && !Double.isInfinite(d))
                        si.deltaMinChangeStanfordParseProb = SentenceInstance.minOrValue(si.deltaMinChangeStanfordParseProb, d);
                }
            }
        }
    }
            
    // calculate probs after various sentence mutations
    private void generateSentenceInstanceMinChangeAttributes(SentenceInstance si, int iMin, OpenNLPParseTree opennlpParse, StanfordParseTree stanfordParse)
    {        
        final Tagging tagging = opennlpParse.getPosTagging();
        final String minToken = tagging.tokens[iMin];
        final String minTag   = tagging.tags[iMin];
        
        switch(getPOSType(minTag))
        {
            case NOUN: 
            {
                // change plurality
                setSentenceInstanceMinChangeAttributes(si, new String[] { changePlurality(minToken, minTag) }, iMin, opennlpParse, stanfordParse);
                
                // insert articles before the noun
                String[] insertTokens = new String[] { "a", "the" };
                for(String it : insertTokens)
                {
                    List<String> l = new ArrayList<String>(Arrays.asList(tagging.tokens));
                    l.add(iMin, it);
                    String[] newTokens = l.toArray(new String[0]);

                    // ensure proper capitalization
                    if(iMin == 0)
                    {
                        String t0 = newTokens[0];
                        String t1 = newTokens[1];
                        newTokens[0] = Character.toUpperCase(t0.charAt(0)) + (t0.length() < 2 ? "" : t0.substring(1));
                        newTokens[1] = Character.toLowerCase(t1.charAt(0)) + (t1.length() < 2 ? "" : t1.substring(1));
                    }

                    // choose the best change seen so far
                    ParseTree newParse;
                    newParse = opennlpToolkit.parser.parse(newTokens);
                    if(newParse != null && opennlpParse != null)
                    {
                        double d = opennlpParse.getProb() - newParse.getProb();
                        if(!Double.isNaN(d) && !Double.isInfinite(d))
                            si.deltaMinChangeOpennlpParseProb = SentenceInstance.minOrValue(si.deltaMinChangeOpennlpParseProb, d);
                        
                        d = opennlpParse.getTagSeqProb() - ((OpenNLPParseTree)newParse).getTagSeqProb();
                        if(!Double.isNaN(d) && !Double.isInfinite(d))
                            si.deltaMinChangeTagSeqProb = SentenceInstance.minOrValue(si.deltaMinChangeTagSeqProb, d);
                    }
                    
                    newParse = stanfordToolkit.parser.parse(newTokens);
                    if(newParse != null && stanfordParse != null)
                    {
                        double d = stanfordParse.getProb() - newParse.getProb();
                        if(!Double.isNaN(d) && !Double.isInfinite(d))
                            si.deltaMinChangeStanfordParseProb = SentenceInstance.minOrValue(si.deltaMinChangeStanfordParseProb, d);
                    }
                }
            }
            break;
                
            case PRONOUN: 
            {
                // generate pronoun changes
                setSentenceInstanceMinChangeAttributes(si,                
                new String[]
                {
                    changePlurality(minToken, minTag),
                    changePronounObjectivity(minToken),
                    changePerson(minToken, minTag),
                },
                iMin, opennlpParse, stanfordParse);
                
            }
            break;
                
            case DETERMINER:
            {
                // change plurality
                setSentenceInstanceMinChangeAttributes(si, new String[] { changePlurality(minToken, minTag) }, iMin, opennlpParse, stanfordParse);
            }
            break;
                
            case VERB:
            {
                // this is already generated by generateSentenceInstanceVerbChangeAttributes when iMin is a verb
                //setSentenceInstanceMinChangeAttributes(si, getUniqueVerbForms(minToken), iMin, opennlpParse, stanfordParse);
            }
            break;
        }
    }
    
    // calculate probs after various sentence mutations
    private void generateSentenceInstanceVerbChangeAttributes(SentenceInstance si, Tagging tagging, OpenNLPParseTree opennlpParse, StanfordParseTree stanfordParse)
    {        
        final Tagging ptagging = opennlpParse.getPosTagging();            

        // initialize totals & counts 
        si.nVerbs = 0;
        si.totDeltaVerbChangeTagProb            = 0;
        si.totDeltaVerbChangePTagProb           = 0;
        si.totDeltaVerbChangeTagSeqProb         = 0;
        si.totDeltaVerbChangeOpennlpParseProb   = 0;
        si.totDeltaVerbChangeStanfordParseProb  = 0;
        si.totVerbChangeImproveRatio            = 0;
        si.totVerbChangeTagImprove              = 0;
        si.totVerbChangePTagImprove             = 0;
        si.totVerbChangeTagSeqImprove           = 0;
        si.totVerbChangeOpennlpParseImprove     = 0;
        si.totVerbChangeStanfordParseImprove    = 0;
        
        double minPTagProb = Double.MAX_VALUE;

        // calculate changes to probabilities after changing tokens
        for(int i = 0; i < ptagging.tags.length; i++)
        {
            final String ptoken   = ptagging.tokens[i];
            final String ptag     = ptagging.tags  [i];
            final double pTagProb = ptagging.probs [i];

            // skip if neither the ptag or tag is a verb (and they agree on the same token)
            if(getPOSType(ptag) == POSType.VERB
            || i < tagging.tokens.length 
               && ptoken.equals(tagging.tokens[i]) 
               && getPOSType(tagging.tags[i]) == POSType.VERB)
            { 
            }
            else
            {
                continue;
            }

            si.nVerbs++;

            // generate all forms of this verb
            final String[] forms        = getUniqueVerbForms(ptoken);
            final String[] newTokens    = ptagging.tokens.clone();
            double maxTagProb           = Double.NEGATIVE_INFINITY;
            double maxPTagProb          = Double.NEGATIVE_INFINITY;
            double maxTagSeqProb        = Double.NEGATIVE_INFINITY;
            double maxOpenNLPParseProb  = Double.NEGATIVE_INFINITY;
            double maxStanfordParseProb = Double.NEGATIVE_INFINITY;

            // calculate the changes in probs from changing the verb form
            for(final String verb : forms)
            {
                // skip if original form
                if(verb.equalsIgnoreCase(ptoken))
                    continue;

                // replace with new form
                newTokens[i] = verb;

                // update max tag prob
                Tagging newTagging = opennlpToolkit.tagger.tag(newTokens);
                if(newTagging != null && newTagging.probs[i] > maxTagProb)
                    maxTagProb = newTagging.probs[i];

                ParseTree newParse;

                newParse = opennlpToolkit.parser.parse(newTokens);
                if(newParse != null)
                {
                    // update parser tags prob
                    Tagging newPTagging = ((OpenNLPParseTree)newParse).getPosTagging();
                    if(newPTagging.probs[i] > maxPTagProb)
                        maxPTagProb = newPTagging.probs[i];

                    // update tag sequence probabiltiy
                    double tagSeqProb = ((OpenNLPParseTree)newParse).getTagSeqProb();
                    if(tagSeqProb > maxTagSeqProb)
                        maxTagSeqProb = tagSeqProb;

                    // update max parse prob
                    if(newParse.getProb() > maxOpenNLPParseProb)
                        maxOpenNLPParseProb = newParse.getProb();
                }

                newParse = stanfordToolkit.parser.parse(newTokens);
                // update stanford parse probability
                if(newParse != null && newParse.getProb() > maxStanfordParseProb)
                    maxStanfordParseProb = newParse.getProb();
            }
            
            // if lowest probability seen so far, record the change in form
            if(pTagProb < minPTagProb)
            {
                minPTagProb = pTagProb;
                si.deltaMinChangeOpennlpParseProb  = opennlpParse.getProb()  - maxOpenNLPParseProb;
                si.deltaMinChangeTagSeqProb = opennlpParse.getTagSeqProb() - maxTagSeqProb;
                if(stanfordParse != null)
                    si.deltaMinChangeStanfordParseProb = stanfordParse.getProb() - maxStanfordParseProb;
            }

            double nVerbFormImprovements = 0, maxVerbFormImprovements = 0;

            // tag
            if(i < tagging.tokens.length && tagging.tokens[i].equals(ptoken))
            {
                double d = tagging.probs[i] - maxTagProb;
                if(!Double.isInfinite(d) && !Double.isNaN(d))
                {
                    si.totDeltaVerbChangeTagProb += d;
                    si.minDeltaVerbChangeTagProb = SentenceInstance.minOrValue(si.minDeltaVerbChangeTagProb, d);
                    si.maxDeltaVerbChangeTagProb = SentenceInstance.maxOrValue(si.maxDeltaVerbChangeTagProb, d);
                    // check for an improvement
                    maxVerbFormImprovements++;
                    if(d < 0) 
                    {
                        si.totVerbChangeTagImprove++;
                        nVerbFormImprovements++;
                    }
                }
            }

            // parser tag
            {
                double d = pTagProb - maxPTagProb;
                if(!Double.isInfinite(d) && !Double.isNaN(d))
                {
                    si.totDeltaVerbChangePTagProb += d;
                    si.minDeltaVerbChangePTagProb = SentenceInstance.minOrValue(si.minDeltaVerbChangePTagProb, d);
                    si.maxDeltaVerbChangePTagProb = SentenceInstance.maxOrValue(si.maxDeltaVerbChangePTagProb, d);
                    // check for an improvement
                    maxVerbFormImprovements++;
                    if(d < 0)
                    {
                        si.totVerbChangePTagImprove++;
                        nVerbFormImprovements++;
                    } 
                }
            }

            // tag sequence
            {
                double d = opennlpParse.getTagSeqProb() - maxTagSeqProb;
                if(!Double.isInfinite(d) && !Double.isNaN(d))
                {
                    si.totDeltaVerbChangeTagSeqProb += d;
                    si.minDeltaVerbChangeTagSeqProb = SentenceInstance.minOrValue(si.minDeltaVerbChangeTagSeqProb, d);
                    si.maxDeltaVerbChangeTagSeqProb = SentenceInstance.maxOrValue(si.maxDeltaVerbChangeTagSeqProb, d);
                    // check for an improvement
                    maxVerbFormImprovements++;
                    if(d < 0)
                    {
                        si.totVerbChangeTagSeqImprove++;
                        nVerbFormImprovements++;
                    }
                }
            }

            // opennlp parse
            {
                double d = opennlpParse.getProb() - maxOpenNLPParseProb;
                if(!Double.isInfinite(d) && !Double.isNaN(d))
                {
                    si.totDeltaVerbChangeOpennlpParseProb += d;
                    si.minDeltaVerbChangeOpennlpParseProb = SentenceInstance.minOrValue(si.minDeltaVerbChangeOpennlpParseProb, d);
                    si.maxDeltaVerbChangeOpennlpParseProb = SentenceInstance.maxOrValue(si.maxDeltaVerbChangeOpennlpParseProb, d);
                    // check for an improvement
                    maxVerbFormImprovements++;
                    if(d < 0)
                    {
                        si.totVerbChangeOpennlpParseImprove++;
                        nVerbFormImprovements++;
                    }
                }
            }

            // stanford parse
            if(stanfordParse != null)
            {
                double d = stanfordParse.getProb() - maxStanfordParseProb;
                if(!Double.isInfinite(d) && !Double.isNaN(d))
                {
                    si.totDeltaVerbChangeStanfordParseProb += d;
                    si.minDeltaVerbChangeStanfordParseProb = SentenceInstance.minOrValue(si.minDeltaVerbChangeStanfordParseProb, d);
                    si.maxDeltaVerbChangeStanfordParseProb = SentenceInstance.maxOrValue(si.maxDeltaVerbChangeStanfordParseProb, d);
                    // check for an improvement
                    maxVerbFormImprovements++;
                    if(d < 0)
                    {
                        si.totVerbChangeStanfordParseImprove++;
                        nVerbFormImprovements++;
                    }
                }
            }

            // how many imrpovements were made? (ratio of improvments over total possible improvements)
            double r = nVerbFormImprovements / maxVerbFormImprovements;
            si.totVerbChangeImproveRatio += r;
            si.minVerbChangeImproveRatio = SentenceInstance.minOrValue(si.minVerbChangeImproveRatio, r);
            si.maxVerbChangeImproveRatio = SentenceInstance.maxOrValue(si.maxVerbChangeImproveRatio, r);
        }

        // calculate averages of verb form changes
        if(si.nVerbs > 0)
        {
            si.avgDeltaVerbChangeTagProb            = si.totDeltaVerbChangeTagProb              / si.nVerbs;
            si.avgDeltaVerbChangePTagProb           = si.totDeltaVerbChangePTagProb             / si.nVerbs;
            si.avgDeltaVerbChangeTagSeqProb         = si.totDeltaVerbChangeTagSeqProb           / si.nVerbs;
            si.avgDeltaVerbChangeOpennlpParseProb   = si.totDeltaVerbChangeOpennlpParseProb     / si.nVerbs;
            si.avgDeltaVerbChangeStanfordParseProb  = si.totDeltaVerbChangeStanfordParseProb    / si.nVerbs;
            si.avgVerbChangeTagImprove              = si.totVerbChangeTagImprove                / si.nVerbs;
            si.avgVerbChangePTagImprove             = si.totVerbChangePTagImprove               / si.nVerbs;
            si.avgVerbChangeTagSeqImprove           = si.totVerbChangeTagSeqImprove             / si.nVerbs;
            si.avgVerbChangeOpennlpParseImprove     = si.totVerbChangeOpennlpParseImprove       / si.nVerbs;
            si.avgVerbChangeStanfordParseImprove    = si.totVerbChangeStanfordParseImprove      / si.nVerbs;
            si.avgVerbChangeImproveRatio            = si.totVerbChangeImproveRatio              / si.nVerbs;
        }
    }
    
    
    private void generateSentenceInstanceSimplifiedClauseAttributes(SentenceInstance si, ParseTree parse)
    {
        // sentence instance for simplified clauses
        final SentenceInstance s_si = new SentenceInstance();   
        
        // generate simplified clauses
        final List<Tagging> clauses = toSimplifiedClauses(parse);

        // initialize totals
        si.s_nClauses                           = 0;
        si.s_totOpennlpParseProb                = 0;
        si.s_totStanfordParseProb               = 0;
        si.s_totDeltaVerbChangeTagProb            = 0;
        si.s_totDeltaVerbChangePTagProb           = 0;
        si.s_totDeltaVerbChangeTagSeqProb         = 0;
        si.s_totDeltaVerbChangeOpennlpParseProb   = 0;
        si.s_totDeltaVerbChangeStanfordParseProb  = 0;
        si.s_totVerbChangeTagImprove              = 0;
        si.s_totVerbChangePTagImprove             = 0;
        si.s_totVerbChangeTagSeqImprove           = 0;
        si.s_totVerbChangeOpennlpParseImprove     = 0;
        si.s_totVerbChangeStanfordParseImprove    = 0;
        si.s_totVerbChangeImproveRatio            = 0;
        si.s_totDeltaMinChangeTagSeqProb          = 0;
        si.s_totDeltaMinChangeOpennlpParseProb    = 0;
        si.s_totDeltaMinChangeStanfordParseProb   = 0;

        for(Tagging clause : clauses)
        {
            final Tagging tagging = opennlpToolkit.tagger.tag(clause.tokens);
            final OpenNLPParseTree opt  = (OpenNLPParseTree) opennlpToolkit.parser.parse(clause.tokens);
            final StanfordParseTree spt = (StanfordParseTree) stanfordToolkit.parser.parse(clause.tokens);

            s_si.opennlpParseProb  = opt.getProb();
            s_si.stanfordParseProb = spt.getProb();

            generateSentenceInstanceVerbChangeAttributes(s_si, tagging, opt, spt);
            generateSentenceInstanceMinChangeAttributes(si, tagging.iMin, opt, spt);

            // totals
            si.s_nClauses++;
            si.s_totOpennlpParseProb                    += s_si.opennlpParseProb;
            si.s_totStanfordParseProb                   += s_si.stanfordParseProb;
            si.s_totDeltaVerbChangeTagProb              += s_si.totDeltaVerbChangeTagProb;
            si.s_totDeltaVerbChangePTagProb             += s_si.totDeltaVerbChangePTagProb;
            si.s_totDeltaVerbChangeTagSeqProb           += s_si.totDeltaVerbChangeTagSeqProb;
            si.s_totDeltaVerbChangeOpennlpParseProb     += s_si.totDeltaVerbChangeOpennlpParseProb;
            si.s_totDeltaVerbChangeStanfordParseProb    += s_si.totDeltaVerbChangeStanfordParseProb;
            si.s_totVerbChangeTagImprove                += s_si.totVerbChangeTagImprove;
            si.s_totVerbChangePTagImprove               += s_si.totVerbChangePTagImprove;
            si.s_totVerbChangeTagSeqImprove             += s_si.totVerbChangeTagSeqImprove;
            si.s_totVerbChangeOpennlpParseImprove       += s_si.totVerbChangeOpennlpParseImprove;
            si.s_totVerbChangeStanfordParseImprove      += s_si.totVerbChangeStanfordParseImprove;
            si.s_totVerbChangeImproveRatio              += s_si.totVerbChangeImproveRatio;
            si.s_totDeltaMinChangeTagSeqProb            += s_si.deltaMinChangeTagSeqProb;
            si.s_totDeltaMinChangeOpennlpParseProb      += s_si.deltaMinChangeOpennlpParseProb;
            si.s_totDeltaMinChangeStanfordParseProb     += s_si.deltaMinChangeStanfordParseProb;


            // min values
            si.s_minStanfordParseProb                   = SentenceInstance.minOrValue(si.s_minStanfordParseProb,                s_si.stanfordParseProb);
            si.s_minOpennlpParseProb                    = SentenceInstance.minOrValue(si.s_minOpennlpParseProb,                 s_si.opennlpParseProb);
            si.s_minDeltaVerbChangeTagProb              = SentenceInstance.minOrValue(si.s_minDeltaVerbChangeTagProb,           s_si.minDeltaVerbChangeTagProb);
            si.s_minDeltaVerbChangePTagProb             = SentenceInstance.minOrValue(si.s_minDeltaVerbChangePTagProb,          s_si.minDeltaVerbChangePTagProb);
            si.s_minDeltaVerbChangeTagSeqProb           = SentenceInstance.minOrValue(si.s_minDeltaVerbChangeTagSeqProb,        s_si.minDeltaVerbChangeTagSeqProb);
            si.s_minDeltaVerbChangeOpennlpParseProb     = SentenceInstance.minOrValue(si.s_minDeltaVerbChangeOpennlpParseProb,  s_si.minDeltaVerbChangeOpennlpParseProb);
            si.s_minDeltaVerbChangeStanfordParseProb    = SentenceInstance.minOrValue(si.s_minDeltaVerbChangeStanfordParseProb, s_si.minDeltaVerbChangeStanfordParseProb);                
            si.s_minVerbChangeImproveRatio              = SentenceInstance.minOrValue(si.s_minVerbChangeImproveRatio,           s_si.minVerbChangeImproveRatio);
            si.s_minDeltaMinChangeTagSeqProb            = SentenceInstance.minOrValue(si.s_minDeltaMinChangeTagSeqProb,         s_si.deltaMinChangeTagSeqProb);
            si.s_minDeltaMinChangeOpennlpParseProb      = SentenceInstance.minOrValue(si.s_minDeltaMinChangeOpennlpParseProb,   s_si.deltaMinChangeOpennlpParseProb);
            si.s_minDeltaMinChangeStanfordParseProb     = SentenceInstance.minOrValue(si.s_minDeltaMinChangeStanfordParseProb,  s_si.deltaMinChangeStanfordParseProb);


            // max values
            si.s_maxStanfordParseProb                   = SentenceInstance.maxOrValue(si.s_maxStanfordParseProb,                s_si.stanfordParseProb);
            si.s_maxOpennlpParseProb                    = SentenceInstance.maxOrValue(si.s_maxOpennlpParseProb,                 s_si.opennlpParseProb);
            si.s_maxDeltaVerbChangeTagProb              = SentenceInstance.maxOrValue(si.s_maxDeltaVerbChangeTagProb,           s_si.maxDeltaVerbChangeTagProb);
            si.s_maxDeltaVerbChangePTagProb             = SentenceInstance.maxOrValue(si.s_maxDeltaVerbChangePTagProb,          s_si.maxDeltaVerbChangePTagProb);
            si.s_maxDeltaVerbChangeTagSeqProb           = SentenceInstance.maxOrValue(si.s_maxDeltaVerbChangeTagSeqProb,        s_si.maxDeltaVerbChangeTagSeqProb);
            si.s_maxDeltaVerbChangeOpennlpParseProb     = SentenceInstance.maxOrValue(si.s_maxDeltaVerbChangeOpennlpParseProb,  s_si.maxDeltaVerbChangeOpennlpParseProb);
            si.s_maxDeltaVerbChangeStanfordParseProb    = SentenceInstance.maxOrValue(si.s_maxDeltaVerbChangeStanfordParseProb, s_si.maxDeltaVerbChangeStanfordParseProb);
            si.s_maxVerbChangeImproveRatio              = SentenceInstance.maxOrValue(si.s_maxVerbChangeImproveRatio,           s_si.maxVerbChangeImproveRatio);
            si.s_maxDeltaMinChangeTagSeqProb            = SentenceInstance.maxOrValue(si.s_maxDeltaMinChangeTagSeqProb,         s_si.deltaMinChangeTagSeqProb);
            si.s_maxDeltaMinChangeOpennlpParseProb      = SentenceInstance.maxOrValue(si.s_maxDeltaMinChangeOpennlpParseProb,   s_si.deltaMinChangeOpennlpParseProb);
            si.s_maxDeltaMinChangeStanfordParseProb     = SentenceInstance.maxOrValue(si.s_maxDeltaMinChangeStanfordParseProb,  s_si.deltaMinChangeStanfordParseProb);
        }

        // averages
        if(si.s_nClauses > 0)
        {
            si.s_avgOpennlpParseProb                    = si.s_totOpennlpParseProb                  / si.s_nClauses;
            si.s_avgStanfordParseProb                   = si.s_totStanfordParseProb                 / si.s_nClauses;
            si.s_avgDeltaVerbChangeTagProb              = si.s_totDeltaVerbChangeTagProb            / si.s_nClauses;
            si.s_avgDeltaVerbChangePTagProb             = si.s_totDeltaVerbChangePTagProb           / si.s_nClauses;
            si.s_avgDeltaVerbChangeTagSeqProb           = si.s_totDeltaVerbChangeTagSeqProb         / si.s_nClauses;
            si.s_avgDeltaVerbChangeOpennlpParseProb     = si.s_totDeltaVerbChangeOpennlpParseProb   / si.s_nClauses;
            si.s_avgDeltaVerbChangeStanfordParseProb    = si.s_totDeltaVerbChangeStanfordParseProb  / si.s_nClauses;
            si.s_avgVerbChangeTagImprove                = si.s_totVerbChangeTagImprove              / si.s_nClauses;
            si.s_avgVerbChangePTagImprove               = si.s_totVerbChangePTagImprove             / si.s_nClauses;
            si.s_avgVerbChangeTagSeqImprove             = si.s_totVerbChangeTagSeqImprove           / si.s_nClauses;
            si.s_avgVerbChangeOpennlpParseImprove       = si.s_totVerbChangeOpennlpParseImprove     / si.s_nClauses;
            si.s_avgVerbChangeStanfordParseImprove      = si.s_totVerbChangeStanfordParseImprove    / si.s_nClauses;
            si.s_avgVerbChangeImproveRatio              = si.s_totVerbChangeImproveRatio            / si.s_nClauses;
            si.s_avgDeltaMinChangeTagSeqProb            = si.s_totDeltaMinChangeTagSeqProb          / si.s_nClauses;
            si.s_avgDeltaMinChangeOpennlpParseProb      = si.s_totDeltaMinChangeOpennlpParseProb    / si.s_nClauses;
            si.s_avgDeltaMinChangeStanfordParseProb     = si.s_totDeltaMinChangeStanfordParseProb   / si.s_nClauses;
        }   
    }
    

    /**
     * Extracts the main noun and verb from a parse tree.
     */
    private List<Tagging> toSimplifiedClauses(ParseTree parse)
    {
        final List<Tagging> result = new LinkedList<Tagging>();
        final List<String> npTags = Arrays.asList(PennTreebankNounPhraseTags),
                           vpTags = Arrays.asList(PennTreebankVerbPhraseTags);
        
        final Map<String, String> tokenMap = new HashMap<String, String>();
        tokenMap.put("'s", "is");
        
        
        ParseTree np = null, vp = null;
        List<ParseTree> nodes = new LinkedList<ParseTree>(Arrays.asList(parse.getChildren()));
        
        // breadth first search for NP, VP; recurse into clauses
        while(!nodes.isEmpty())
        {
            final List<ParseTree> children = new LinkedList<ParseTree>();
            for(ParseTree n : nodes)
            {
                // recurse into clauses
                if(n.isClausal())
                {
                    result.addAll(toSimplifiedClauses(n));
                }
                // otherwise check if np/vp, add children
                else
                {                   
                    if(np == null && npTags.contains(n.getValue()))
                    {
                        np = n;
                    }
                    // only add vp if a np has been found first
                    else if(vp == null && np != null && vpTags.contains(n.getValue()))
                    {
                        vp = n;
                    }
                    
                    children.addAll(Arrays.asList(n.getChildren()));
                }
            }
            
            nodes = children;
        }
        
        final List<ParseTree> tagNodes = new LinkedList<ParseTree>();
        boolean hasNoun = false;
        boolean hasVerb = false;

        // collect consecutive noun nodes
        if(np != null)
        {
            final List<ParseTree> t = new LinkedList<ParseTree>();
            nodes.clear();
            nodes.addAll(Arrays.asList(np.getChildren()));
            
            boolean done = false;
            while(!done && !nodes.isEmpty())
            {
                List<ParseTree> children = new LinkedList<ParseTree>();    
                for(ParseTree n : nodes)
                {
                    if(n.isPosTag())
                    {
                        POSType posType = getPOSType(n.getValue());
                        // add noun
                        if(posType == POSType.NOUN
                           // or pronoun or determiner (if list is empty)
                            || (t.isEmpty() && (posType == POSType.PRONOUN || posType == POSType.DETERMINER )))
                        {
                            t.add(n);
                        }                        
                        // if a noun has already been added, and this is not one, stop adding
                        else if(!t.isEmpty())
                        {
                            done = true;
                            break;
                        }
                    }
                    children.addAll(Arrays.asList(n.getChildren()));
                }

                nodes = children;
            }
            
            hasNoun = !t.isEmpty();
            tagNodes.addAll(t);
        }
        
        // collect consecutive verb nodes
        if(vp != null)
        {
            final List<ParseTree> t = new LinkedList<ParseTree>();
            nodes.clear();
            nodes.addAll(Arrays.asList(vp.getChildren()));
            
            boolean done = false;
            while(!done && !nodes.isEmpty())
            {
                List<ParseTree> children = new LinkedList<ParseTree>();    
                for(ParseTree n : nodes)
                {
                    if(n.isPosTag())
                    {
                        POSType posType = getPOSType(n.getValue());
                        if(posType == POSType.VERB)
                        {
                            t.add(n);
                        }
                        // if a verb has already been added, and this is not one, stop adding
                        else if(!t.isEmpty())
                        {
                            done = true;
                            break;
                        }
                    }
                    // if vp encountered, constrain search to that vp only
                    else if(vpTags.contains(n.getValue()))
                    {
                        children.clear();
                        children.addAll(Arrays.asList(n.getChildren()));
                        break;
                    }
                    else
                    {
                        children.addAll(Arrays.asList(n.getChildren()));
                    }
                }

                nodes = children;
            }
            
            hasVerb = !t.isEmpty();
            tagNodes.addAll(t);
        }
        
        if(!tagNodes.isEmpty() && hasVerb)
        {
            List<String> tokens = new LinkedList<String>();
            List<String> tags = new LinkedList<String>();
            List<Double> probs = new LinkedList<Double>();

            for(ParseTree n : tagNodes)
            {
                ParseTree[] c = n.getChildren();
                if(c.length > 0)
                {
                    String t = c[0].getValue();
                    String tt = tokenMap.get(t);
                    t = tt == null ? t : tt;

                    // ignore non-words
                    if(t != null && t.matches("(\\s*[\\w,]+\\s*)+"))
                    {
                        tokens.add(t);
                        tags.add(n.getValue());
                        probs.add(n.getProb());
                    }
                }
            }
            if(tokens.size() > 0)
            {
                String first = tokens.get(0);
                first = first.substring(0,1).toUpperCase() + first.substring(1);
                tokens.set(0, first);
                tokens.add(".");
                tags.add(".");
            
                result.add(new Tagging(tokens.toArray(new String[0]), 
                        tags.toArray(new String[0]), 
                        ArrayUtils.toPrimitive(probs.toArray(new Double[0])),
                        null));
            }
        }
        
        return result;
    }
   
    
    /**
     * Extracts the main noun and verb from a parse tree.
     *
    private List<String[]> toSimplifieddClausesFromDependencies(StanfordParseTree parse)
    {  
        final List<String> clausalDependencies = Arrays.asList(new String[] { "nsubj", "aux", "root", "xsubj", "partmod" });
        final String[] tags = parse.getPosTags();
        LinkedList<String[]> result = new LinkedList<String[]>();
        List<TypedDependency> dependencies = ((StanfordParser)stanfordToolkit.parser).parseDependencies(parse);
        for(TypedDependency td : dependencies)
        {
            System.out.println(td.reln() + ": " + td.gov() + "-> " + td.dep());
            if(clausalDependencies.contains(td.reln().getShortName()))
                result.add(new String[]{td.dep().value(), td.gov().value()});
            
        }
        return result;
    }
    */
    
    public static class XmlOptions
    {
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
        public int nGrammaticalErrorsPerSentence;   // if using binary attributes: 0 = correct, otherwise incorrect
        public boolean useBinaryErrorOnly;
        public boolean isAppending;
        public String outputFile;
    }
    public void runXmlTool(String input, final XmlOptions o)
    {
        final String outputFile = o.outputFile;
        final String[] sentences = stanfordToolkit.sentenceSplitter.split( input );
        final List<ProcessedSentence> processed = new LinkedList<ProcessedSentence>();
        final long cacheSize = 200;
        final int maxResults = 2;  // top 2 taggings & parses

        final double total = sentences.length;
        long progress = 0;
        updateUI(progress, "");

        for( final String sentence : sentences )
        {                               
            final Runnable r = 
            new Runnable() 
            {
                private String output = "";
                @Override public String toString() { return output; }
                @Override public void run()
                {
                    try
                    {
                        final String[] tokens = stanfordToolkit.tokenizer.tokenize( sentence );
                        final Tagging[] taggings = opennlpToolkit.tagger.tag( tokens, maxResults );

                        // parse
                        List<ParseTree> parses = new LinkedList<ParseTree>();
                        parses.addAll( Arrays.asList( stanfordToolkit.parser.parse( tokens, maxResults ) ) );
                        parses.addAll( Arrays.asList( opennlpToolkit.parser.parse( tokens, maxResults ) ) );

                        ParseTree[] arrParses = parses.toArray( new ParseTree[0] );

                        // save sentence features
                        processed.add( 
                                o.useBinaryErrorOnly ? 
                                new ProcessedSentence( tokens, arrParses, taggings, o.nGrammaticalErrorsPerSentence == 0 ) :
                                new ProcessedSentence( tokens, arrParses, taggings, o.nGrammaticalErrorsPerSentence ) );

                        if( !processed.isEmpty() 
                         && (processed.size() >= cacheSize || sentence == sentences[sentences.length-1]) )
                        {
                            writeXmlFile( processed.toArray(new ProcessedSentence[0]), outputFile, o.isAppending );

                            o.isAppending = true;
                            processed.clear();
                        }
                        
                        output += sentence;
                        if(taggings.length > 0 && taggings[0] != null)
                            output += "\n" + taggings[0].toString();
                        if(arrParses.length > 0 && arrParses[0] != null)
                            output += "\n" + arrParses[0].toString();
                        output += "\n\n";
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace(System.err);
                    }
                }
            };
            
            String output = "";
            final Thread t = new Thread(r);
            t.start();
            try 
            { 
                t.join(o.maxItemProcessTime); 
                output = r.toString();
                if(t.isAlive())
                {
                    output += " <operation timed out>";
                    t.interrupt();
                }
            }
            catch(InterruptedException e) {}

            updateUI(++progress/total, output);
        }
    }
    

    public static class StatsOptions
    {
        public String[] constituents;
    }
    public void runStatsTool(final String input, final StatsOptions o)
            throws IOException
    {
        if(!isValidXml(new java.io.ByteArrayInputStream(input.getBytes("UTF-8"))))
        {
            updateUI( 1, "<Invalid input: input must be valid GrammarTools XML>" );
            return;
        }
        
        SentenceStatistics stats = new SentenceStatistics();
        try 
        { 
            stats = GrammarTools.generateStatistics( 
                new java.io.ByteArrayInputStream(input.getBytes("UTF-8")),
                Arrays.asList(o.constituents) ); 
        }
        catch(SAXException e){}
        catch(ParserConfigurationException e){}

        String output =
        "Total sentences: "         + stats.numSentences
        + "\nTotal grammatical: "   + stats.numGrammatical
        + "\nAvg. length: "         + String.format("%.2f", stats.avgLength)
        + "\nTotal parses: "        + stats.numParses
        + "\nStanford parses: "     + stats.numStanfordParses
        + "\nOpenNLP parses: "      + stats.numOpenNLPParses
        + "\nStanford clausals: "   + stats.numStanfordClausal
        + "\nOpenNLP clausals: "    + stats.numOpenNLPClausal
        + "\nOpenNLP parse prob avg: "  + String.format("%.4f", stats.avgOpenNLPParseProb)
        + "\nStanford parse prob avg: " + String.format("%.4f", stats.avgStanfordParseProb);

        // sort and output lowest tags
        if( !stats.lowestTags.isEmpty() ) 
        {
            output += "\n\nLowest-probability tags and their frequencies:\n";
            List<Map.Entry<String,Long>> entries = new ArrayList<Map.Entry<String,Long>>
                    (stats.lowestTags.entrySet());
            Collections.sort( entries, 
            new Comparator<Map.Entry<String,Long>>() 
            {
                @Override
                public int compare( Map.Entry<String,Long> me1, Map.Entry<String,Long> me2 ) 
                {
                     long d = me1.getValue() - me2.getValue();
                     return d < 0 ? 1 : d > 0 ? -1 : 0;
                }
            });

            for( Map.Entry<String,Long> me : entries )
            {
                output += me.getKey() + ": " + me.getValue() + "\n";
            }
        }

        // sort and output most frequent parse constituents
        if( !stats.constituents.isEmpty() ) 
        {
            output += "\nTracked constituents and their frequencies:\n";
            for( Map.Entry<String, Map<List<String>,Long>> me : stats.constituents.entrySet() ) 
            {
                List<Map.Entry<List<String>,Long>> entries = new ArrayList<Map.Entry<List<String>,Long>>
                        (me.getValue().entrySet());
                Collections.sort( entries, 
                new Comparator<Map.Entry<List<String>,Long>>() 
                {
                    @Override
                    public int compare( Map.Entry<List<String>,Long> me1, Map.Entry<List<String>,Long> me2 ) 
                    {
                         long d = me1.getValue() - me2.getValue();
                         return d < 0 ? 1 : d > 0 ? -1 : 0;
                    }
                });

                for( Map.Entry<List<String>,Long> me2 : entries ) 
                {
                    output += me.getKey() + "\t--> " + " ";
                    for(String parse : me2.getKey())
                        output += parse + " ";
                    output += "\t(" + me2.getValue() + ")\n";
                }
            }
        }

        updateUI( 1, output );
    }
    
     
    public static class DcgOptions
    {
        public boolean showRules;
        public String grammarFile;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runDcgTool(String input, final DcgOptions o)
    {          
        Prolog engine;
        try
        {
            engine = createGrammar(o.grammarFile);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            updateUI( 1, "<Could not load grammar file: " + o.grammarFile 
                         + "\n" + ex.getLocalizedMessage() );
            return;
        }

        final Prolog grammar = engine;
                
        for( final Toolkit kit : new Toolkit[]{opennlpToolkit, stanfordToolkit} )
        {
            final String[] sentences = kit.sentenceSplitter.split( input );
            final double total = sentences.length;
            long progress = 0;

            updateUI( progress/total, kit + "\n" );

            for( final String sentence : sentences )
            {
                final Runnable r = 
                new Runnable() 
                {
                    private String output = "";
                    @Override public String toString() { return output; }
                    @Override public void run()
                    {
                        try
                        {
                            final String[] tokens = kit.tokenizer.tokenize( sentence );
                            final ParseTree parse = kit.parser.parse( tokens );
                            
                            output += sentence;

                            SolveInfo query = queryGrammar(grammar, parse.getValue(), parse.getPosTags());
                            if(o.showRules)
                                output += "\n" + query.getQuery();
                            
                            output += "\nin grammar? " + query.isSuccess() + "\n\n";
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace(System.err);
                        }
                    }
                };

                String output = "";
                final Thread t = new Thread(r);
                t.start();
                try 
                { 
                    t.join(o.maxItemProcessTime); 
                    output = r.toString();
                    if(t.isAlive())
                    {
                        output += " <operation timed out>";
                        t.interrupt();
                    }
                }
                catch(InterruptedException e) {}
                
                updateUI( ++progress/total, output );
            }
        }
    }
    
    public static class ClassifyOptions
    {  
        public double errorThreshold;
        public String functionScript;
        public String grammarFile;
        public String modelFile;
        public boolean isNumericClass;
        public boolean showCorrect;
        public boolean showIncorrect;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    public void runClassifyTool(String input, final ClassifyOptions o)
    {
        final String[] sentences = stanfordToolkit.sentenceSplitter.split( input );
        final long total = sentences.length;
        long totalGrammatical = 0;
        double progress = 0;

        Prolog engine = null;
        if(o.grammarFile != null && !o.grammarFile.isEmpty())
        {
            try
            {
                engine = createGrammar(o.grammarFile);
            }
            catch(Exception ex)
            {
                engine = null;
                ex.printStackTrace();
                updateUI( 0, "<Could not load grammar file: " + o.grammarFile 
                             + "\n" + ex.getLocalizedMessage() + "\n(DCG sentence features will be missing)>\n" );
            }
        }
        
        final Prolog grammar = engine;
                
        updateUI( 0, "" );
        
        final Classifier classifier;
        Instances data;
        try
        {
            //classifier = (Classifier) weka.core.SerializationHelper.read(o.modelFile);
            InputStream is = new java.io.FileInputStream(o.modelFile);
            
            if (o.modelFile.endsWith(".gz"))
                is = new java.util.zip.GZIPInputStream(is);

            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(is);
            classifier = (Classifier) ois.readObject();
            try 
            { 
                // see if we can load the header
                data = (Instances) ois.readObject();
            } 
            catch (Exception e) 
            {
                data = SentenceInstance.createWekaHeader(!o.isNumericClass, o.isNumericClass);
            }
            ois.close();         
        }
        catch (Exception ex)
        {
            updateUI(1, "<Failed to load classifier model>\n" + ex.getLocalizedMessage() +"\n");
            return;
        }

        // Test the model
        //weka.classifiers.Evaluation eval = new weka.classifiers.Evaluation(data);
        //eval.evaluateModel(classifier, data);
        //output += eval.toSummaryString("\nResults\n======\n", false);
                                
        if(data == null)
        {
            updateUI(1, "<Invalid classifier model: unsupported class attributes>\n" + classifier + "\n");
            return;
        }
                
        updateUI(0, "classifier: " + classifier.getClass().getSimpleName() + "\n\n" );
                
        for( final String sentence : sentences )
        {
            final StringBuffer sb =  new StringBuffer();            
            final Instance i = generateSentenceInstance(sentence, 0, o.functionScript, grammar).toWekaInstance(data);
            i.setDataset(data);
            data.add(i);
            
            try
            {
                double result = classifier.classifyInstance(i);
                if(i.classAttribute().isNominal())
                {
                    boolean isGrammatical = SentenceInstance.fromBoolNominalIndex(result);
                    
                    if(isGrammatical)
                        ++totalGrammatical;
                    
                    if(isGrammatical && o.showCorrect || !isGrammatical && o.showIncorrect)
                        sb.append( sentence + "\n[correct: " + isGrammatical + "]\n\n" );
                }
                else
                {
                    boolean isGrammatical = result <= o.errorThreshold;
                    
                    if(isGrammatical)
                        ++totalGrammatical;
                    
                    if(isGrammatical && o.showCorrect || !isGrammatical && o.showIncorrect)
                        sb.append( sentence + "\n[correct: " + isGrammatical + ", errors: " + result + "]\n\n" );
                }
            }
            catch(Exception e)
            {
                sb.append(sentence + "\n<error: " + e.getLocalizedMessage() + ">\n\n");
            }
            
            data.clear();

            updateUI( ++progress/total, sb.toString() );
        }

        updateUI( 1, "Total sentences: " + total 
                     + "\nGrammatically correct: " + totalGrammatical
                     + "\nGrammatically incorrect: " + (total - totalGrammatical) + "\n\n" );
    }
    
    /**
     * Generates XML file describing a sentence and its attributes.
     * @param sentences sentence features that describe sentences
     * @param filename  output file name
     * @param append    if true, data is appended to a pre-existing file, otherwise the file is replaced with new data
     */
    public static void writeXmlFile(
            ProcessedSentence[] sentences,
            String filename, boolean append )
            throws java.io.IOException, JDOMException
    {
        Document doc;

        java.io.File file = new java.io.File( filename );
        if( append && file.exists() ) {
            doc = new SAXBuilder().build( file );
        } else {
            // Add root element and comment
            doc = new Document( new Element( XML_ROOT ) );
            doc.addContent( 0, new Comment( XML_COMMENT ) );
        }

        // add elements for each sentence
        for( ProcessedSentence s : sentences ) {
            // create sentence element and specify grammaticality
            Element sentence = new Element( XML_ELEM_SENT );
            doc.getRootElement().addContent( sentence );
            sentence.setAttribute( new org.jdom.Attribute(
                    XML_ATTRIB_GRAMMATICAL,
                    Boolean.toString( s.grammatical )) );
            sentence.setAttribute( new org.jdom.Attribute(
                    XML_ATTRIB_NUM_ERRORS,
                    s.nGrammaticalErrors == ProcessedSentence.UNKNOWN_ERRORS ? 
                    "?" : Integer.toString( s.nGrammaticalErrors )) );


            // create tokens element to hold each token
            Element tokens = new Element( XML_ELEM_TOKENS );
            sentence.addContent( tokens );
            for( String t : s.tokens )
            {
                tokens.addContent( new Element( XML_ELEM_TOKEN ).addContent(t) );
            }

            // create tagging elements and add each to sentence
            for( Tagging t : s.taggings ) 
            {
                Element tagging = new Element( XML_ELEM_TAGGING );
                sentence.addContent( tagging );
                tagging.setAttribute( new org.jdom.Attribute( XML_ATTRIB_DESC, t.desc ));
                for( int i = 0; i < t.tags.length; i++ ) {
                    Element tag = new Element( toXmlName( t.tags[i] ) );
                    tagging.addContent( tag );
                    tag.addContent( t.tokens[i] );
                    if( t.hasProbs() ) 
                    {
                        tag.setAttribute( new org.jdom.Attribute( XML_ATTRIB_PROB,
                                Double.toString(t.probs[i] )));
                    }
                }
            }

            // create parse elements and add each to sentence
            for( ParseTree p : s.parses ) {
                Element parse = new Element( XML_ELEM_PARSE );
                sentence.addContent( parse );
                parse.setAttribute( new org.jdom.Attribute( XML_ATTRIB_DESC,
                        p.getDesc() ));
                addElementRecursive( parse, p );
                if( p instanceof OpenNLPParseTree ) {
                    parse.setAttribute( new org.jdom.Attribute( XML_ATTRIB_TAGSEQPROB,
                            Double.toString( ((OpenNLPParseTree)p).getTagSeqProb() ) ) );
                }
            }
        }

        // output
        java.io.FileWriter writer = new java.io.FileWriter( file );
        XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
        outputter.output( doc, writer );
        writer.close();
    }

    private static void addElementRecursive( Element e, ParseTree p )
    {
        if( p.isTerminal() ) {
            e.addContent( p.getValue() );
        } else {
            Element tag = new Element( toXmlName( p.getValue() ) );
            for( ParseTree child : p.getChildren() ) {
                addElementRecursive( tag, child );
            }
            e.addContent( tag );
            if( !Double.isNaN( p.getProb() ) ) {
                e.setAttribute( new org.jdom.Attribute(
                        XML_ATTRIB_PROB, Double.toString( p.getProb() )));
            }
        }
    }
    

    public static boolean isValidXml(InputStream input)
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        try
        {
            SAXParser parser = factory.newSAXParser();

            XMLReader reader = parser.getXMLReader();
            reader.setErrorHandler(new DefaultHandler());
            reader.parse(new InputSource(input));
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    /**
     * Verifies and cleans a name to match XML conventions.
     * Any non-valid characters are converted to their hex-equivalent
     * using the notation _xHH_ where 'HH' is hexadecimal representation of the character.
     * @param name  the name to be verified/cleaned.
     * @return      a valid XML name.
     */
    private static String toXmlName( String name )
    {
        char[] chstr = name.toCharArray();
        StringBuilder sb = new StringBuilder( name.length() );

        // first char must be letter or _
        if( Character.isLetter( chstr[0] )
         || chstr[0] == '_' ) {
            sb.append(chstr[0]);
        } else {
            sb.append( PRE_HEX ).append( Integer.toHexString(chstr[0]).toUpperCase() ).append( POST_HEX);
        }
        
        // others must be letter, digit, . - _
        for( int i = 1; i < chstr.length; i++ ) {
            if( Character.isLetter(chstr[i])
             || Character.isDigit(chstr[i])
             || chstr[i] == '.'
             || chstr[i] == '-'
             || chstr[i] == '_' )
            {
                sb.append(chstr[i]);
            } else {
                sb.append( PRE_HEX ).append( Integer.toHexString(chstr[i]).toUpperCase() ).append( POST_HEX);
            }
        }
        return sb.toString();
    }

    /**
     * Retrieves original element name from a clean XML name.
     * @param name  the name whose original is to be retrieved
     * @return      The original element name.
     */
    private static String fromXmlName( String name )
    {
        StringBuilder sb = new StringBuilder( name.length() );
        for( int i = 0, j = PRE_HEX.length(); i < name.length(); i++, j++ ) {
            if( j < name.length() && name.substring(i,j).equals( PRE_HEX ) ) {
                sb.append( Character.toChars(
                        Integer.valueOf(
                        name.substring( j, i = name.indexOf( POST_HEX, j ) ),
                        16 )));
                j = i + PRE_HEX.length();
            } else {
                sb.append( name.charAt(i) );
            }
        }
        return sb.toString();
    }

    /**
     * Gathers statistics from a GrammarTools Sentences XML file.
     * @param filename  A valid GrammarTools Sentences XML file
     * @param validate  Whether to perform XML validation.
     * @param constituents A list of parse constituents whose parse trees will be tracked (or null to disable tracking).
     * @return  The gathered statistics
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static SentenceStatistics generateStatistics(
            InputStream input, final List<String> constituents )
            throws IllegalArgumentException, IOException, SAXException, ParserConfigurationException
    {
        // use SAX stream parser 
        // stream parsing is more effeceitn than building a DOM
        SAXParserFactory factory = SAXParserFactory.newInstance();
        //factory.setValidating( true );
        SAXParser parser = factory.newSAXParser();

        final SentenceStatistics stats = new SentenceStatistics();

        parser.parse( input, new DefaultHandler()
        {
            String chars = null;
            String prevElem = null;
            String prevDesc = null;
            boolean inTagging = false;
            long   numTokenizations = 0;
            long   numTokens = 0;
            double totalOpenNLPParseProb  = 0;
            double totalStanfordParseProb = 0;
            Double lowTagProb = null;
            String lowTag = null;
            boolean inParse = false;
            boolean isContentElement = false;
            long parseDepth = 0;

            class TrackedParse {
                TrackedParse( String head, long depth ) {
                    this.head = head;
                    this.depth = depth;
                    this.constituents = new LinkedList<String>();
                }
                String head;
                long depth;
                List<String> constituents;
            }
            Stack<TrackedParse> trackedParses = new Stack<TrackedParse>();

            @Override
            public void startElement( String uri, String localName, String qName, org.xml.sax.Attributes attributes )
            {
                // get the original name
                qName = fromXmlName( qName );

                // sentence
                if( qName.equals( XML_ELEM_SENT ) ) {
                    stats.numSentences++;
                    String attrib = attributes.getValue( XML_ATTRIB_GRAMMATICAL );
                    // grammatical
                    if(attrib != null && Boolean.parseBoolean(attrib))
                        stats.numGrammatical ++;
                }
                // parse
                else if( qName.equals( XML_ELEM_PARSE ) ) {
                    inParse = true;
                    stats.numParses++;
                    String attrib = attributes.getValue( XML_ATTRIB_DESC );
                    // OpenNLP
                    if( attrib != null && attrib.equals( OpenNLPParseTree.DESC ) ) {
                        prevDesc = attrib;
                        stats.numOpenNLPParses++;
                        if( (attrib = attributes.getValue( XML_ATTRIB_PROB )) != null )
                            totalOpenNLPParseProb += Double.parseDouble(attrib);
                    }
                    // Stanford
                    else if( attrib != null && attrib.equals( StanfordParseTree.DESC ) ) {
                        prevDesc = attrib;
                        stats.numStanfordParses++;
                        if( (attrib = attributes.getValue( XML_ATTRIB_PROB )) != null )
                            totalStanfordParseProb += Double.parseDouble(attrib);
                    }
                }
                // clausal
                else if( prevElem != null && prevElem.equals( OpenNLPParseTree.ROOT ) ) {
                    for( String s : OpenNLPParseTree.CLAUSES ) {
                        if( qName.equals(s) ) {
                            stats.numOpenNLPClausal++;
                            break;
                        }
                    }
                }
                else if( prevElem != null && prevElem.equals( StanfordParseTree.ROOT )) {
                    for( String s : StanfordParseTree.CLAUSES ) {
                        if( qName.equals(s) ) {
                            stats.numStanfordClausal++;
                            break;
                        }
                    }
                }
                // tokens
                else if( qName.equals( XML_ELEM_TOKENS ) ) {
                    numTokenizations++;
                }
                // token
                else if( qName.equals( XML_ELEM_TOKEN ) ) {
                    numTokens++;
                }
                // tagging
                else if( qName.equals( XML_ELEM_TAGGING ) ) {
                    inTagging = true;
                    lowTagProb = null;
                }
                // tag
                else if( inTagging ) {
                    String attrib = attributes.getValue( XML_ATTRIB_PROB );
                    Double prob = attrib == null ? null : Double.parseDouble( attrib );
                    if( lowTagProb == null || (prob != null && prob < lowTagProb) ) {
                            lowTagProb = prob;
                            lowTag = qName;
                    }
                }

                // parse constituent
                if( inParse ) {
                    // maintain a stack for currently tracked parse constituents
                    if( constituents != null ) {
                        if( trackedParses.size() > 0
                         && parseDepth == trackedParses.peek().depth + 1 ) {
                            trackedParses.peek().constituents.add( qName );
                        }

                        if( constituents.contains( qName ) ) {
                            trackedParses.push( new TrackedParse( qName, parseDepth ) );
                        }
                    }
                    parseDepth++;
                }

                prevElem = qName;
                isContentElement = true;
                chars = null;
            }
            
            @Override
            public void characters (char ch[], int start, int length)
                    throws SAXException
            {
                if(inParse && isContentElement)
                {
                    chars = (chars == null ? "" : chars) + String.valueOf(ch, start, length);
                }
            }

            @Override
            public void endElement( String uri, String localName, String qName )
            {
                qName = fromXmlName( qName );

                if( qName.equals( XML_ELEM_TAGGING ) ) {
                    inTagging = false;
                    // record lowest tag and frequency
                    stats.lowestTags.put( lowTag, 1 +
                            ( stats.lowestTags.containsKey( lowTag ) ?
                                stats.lowestTags.get( lowTag ) : 0 ));
                }
                else if( qName.equals( XML_ELEM_PARSE ) ) {
                    inParse = false;
                    parseDepth = 0;
                }
                else if( inParse ) {
                    parseDepth--;
                    if( constituents != null ) {
                        if( constituents.contains( qName ) ) {
                            TrackedParse parse = trackedParses.pop();
                            if(isContentElement && chars != null) {
                                parse.constituents.add(chars);
                            }
                            Map<List<String>,Long> info = stats.constituents.get( qName );
                            if( info == null  ) {
                                info = new HashMap<List<String>,Long>();
                                info.put( parse.constituents, 1L );
                                stats.constituents.put( qName, info );
                            }
                            else {
                                Long count = info.get( parse.constituents );
                                if( count == null ) {
                                    info.put( parse.constituents, 1L );
                                } else {
                                    info.put( parse.constituents, ++count );
                                }
                            }
                        }
                    }
                }

                chars = null;
                isContentElement = false;
            }

            @Override
            public void endDocument() {
                // calculate averages
                stats.avgLength = (double)numTokens / numTokenizations;
                stats.avgOpenNLPParseProb  = totalOpenNLPParseProb  / stats.numOpenNLPParses;
                stats.avgStanfordParseProb = totalStanfordParseProb / stats.numStanfordParses;
            }
        } );

        return stats;
    }
   
    public static enum POSType
    {
        NOUN        (LexicalCategory.NOUN,          new String[] {"NN", "NNS", "NNP", "NNPS"}),
        VERB        (LexicalCategory.VERB,          new String[] {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "MD"}),
        DETERMINER  (LexicalCategory.DETERMINER,    new String[] {"DT", "WDT", "PDT"}),
        ADJECTIVE   (LexicalCategory.ADJECTIVE,     new String[] {"JJ", "JJR", "JJS"}),
        ADVERB      (LexicalCategory.ADVERB,        new String[] {"RB", "RBR", "RBS", "WRB"}),
        PREPOSITION (LexicalCategory.PREPOSITION,   new String[] {"IN", "TO"}),
        PRONOUN     (LexicalCategory.PRONOUN,       new String[] {"PRP", "PRP$", "WP", "WP$"}),
        PUNCTUATION (LexicalCategory.SYMBOL,        new String[] {"NN", "NNS", "NNP", "NNPS"}),
        OTHER       (LexicalCategory.ANY,           new String[0]);
        
        private POSType(LexicalCategory lc, String[] tags)
        {
            this.category = lc;
            this.tags = tags;
        }        
        final private LexicalCategory category;
        final private String[] tags;
    }
    
    public static POSType getPOSType(String tag)
    {
        tag = tag.toUpperCase();
        for(POSType type : POSType.values())
            if(Arrays.asList(type.tags).contains(tag))
                return type;

        return POSType.OTHER;
    }
        
    public boolean isPluralNounTag(String tag)
    {
        return pluralNounTags.contains(tag);
    }
    
    public Form getVerbForm(String tag)
    {
        Form form = verbForm.get(tag);
        return form != null ? form : Form.NORMAL;
    }

    public static enum ErrorType 
    {
        INSERTION    (new POSType[] {POSType.NOUN, POSType.VERB, POSType.DETERMINER, POSType.ADJECTIVE, POSType.ADVERB, POSType.PREPOSITION, POSType.PRONOUN, POSType.PUNCTUATION, POSType.OTHER}),
        OMISSION     (new POSType[] {POSType.NOUN, POSType.VERB, POSType.DETERMINER, POSType.ADJECTIVE, POSType.ADVERB, POSType.PREPOSITION, POSType.PRONOUN, POSType.PUNCTUATION, POSType.OTHER}),
        DISPLACEMENT (new POSType[] {POSType.NOUN, POSType.VERB, POSType.DETERMINER, POSType.ADJECTIVE, POSType.ADVERB, POSType.PREPOSITION, POSType.PRONOUN, POSType.PUNCTUATION, POSType.OTHER}),
        NUMBER       (new POSType[] {POSType.NOUN, POSType.DETERMINER, POSType.PRONOUN, /* TODO: POSType.VERB */}),
        TENSE        (new POSType[] {POSType.VERB, }),
        //PERSON       (new POSType[] {POSType.PRONOUN}),
        OBJECTIVITY  (new POSType[] {POSType.PRONOUN}),
        CASE         (new POSType[] {POSType.NOUN, }),
        //REDUNANCE,
        ;
        private ErrorType(POSType[] posTypes)
        {
            this.posTypes = posTypes;
            weight = 1;
        }
        public POSType[] getPOSTypes() { return posTypes.clone(); }
        public void setPOSTypes(POSType[] types) { posTypes = types; }
        public int getWeight() { return weight; }
        public void setWeight(int w) { weight = w; }
        private POSType[] posTypes;
        private int weight;
    }

    private void initSimpleNLG()
    {
        if(realiser == null)
        {
            lexicon = Lexicon.getDefaultLexicon();
            nlgfactory = new NLGFactory(lexicon);
            realiser = new Realiser(lexicon);

            // plural noun tags
            pluralNounTags = new TreeSet<String>() ;
            pluralNounTags.add("NNS");
            pluralNounTags.add("NNPS");
            
            // map tag to verb form
            verbForm = new TreeMap<String, Form>();
            verbForm.put("VB",     Form.BARE_INFINITIVE);
            verbForm.put("VBD",    Form.PAST_PARTICIPLE);
            verbForm.put("VBG",    Form.PRESENT_PARTICIPLE);
            verbForm.put("VBN",    Form.BARE_INFINITIVE);
            verbForm.put("VBP",    Form.BARE_INFINITIVE);
            verbForm.put("VBZ",    Form.NORMAL);
            
            personChange = new EnumMap<Person, Person>(Person.class);
            personChange.put(Person.FIRST, Person.THIRD);
            personChange.put(Person.SECOND, Person.THIRD);
            personChange.put(Person.THIRD, Person.FIRST);
            
            determinerChange = new TreeMap<String, String>();
            determinerChange.put("the", "a");
            determinerChange.put("some", "a");
            determinerChange.put("a", "the");
            determinerChange.put("an", "the");
            determinerChange.put("this", "these");
            determinerChange.put("these", "this");
            determinerChange.put("that", "those");
            determinerChange.put("those", "that");
            determinerChange.put("what", "which");
            determinerChange.put("which", "what");
            determinerChange.put("whatever", "whichever");
            determinerChange.put("whichever", "whatever");
            determinerChange.put("all", "a");
            
            
            // bimap of subjective to objective pronouns
            objectivityChange = new TreeMap<String, String>();
            objectivityChange.put("I", "me");      objectivityChange.put("me", "I");
            objectivityChange.put("he", "him");    objectivityChange.put("him", "he");
            objectivityChange.put("she", "her");   objectivityChange.put("her", "she");
            objectivityChange.put("it", "me");     objectivityChange.put("me", "it");
            objectivityChange.put("we", "us");     objectivityChange.put("us", "we");
            objectivityChange.put("you", "me");    objectivityChange.put("me", "you");
            objectivityChange.put("they", "them"); objectivityChange.put("them", "they");
            objectivityChange.put("who", "him");   objectivityChange.put("him", "who");
            objectivityChange.put("what", "me");   objectivityChange.put("me", "what");

            // map verb form to bad/opposite verb form
            verbFormChange = new EnumMap<Form, Form>(Form.class);
            verbFormChange.put(Form.BARE_INFINITIVE,   Form.PRESENT_PARTICIPLE);
            verbFormChange.put(Form.INFINITIVE,        Form.PRESENT_PARTICIPLE);
            verbFormChange.put(Form.IMPERATIVE,        Form.PRESENT_PARTICIPLE);
            verbFormChange.put(Form.GERUND,            Form.BARE_INFINITIVE);
            verbFormChange.put(Form.PRESENT_PARTICIPLE,Form.BARE_INFINITIVE);
            verbFormChange.put(Form.PAST_PARTICIPLE,   Form.BARE_INFINITIVE);
            verbFormChange.put(Form.NORMAL,            Form.BARE_INFINITIVE);
        }
    }
    
    private String changePlurality(String word, String tag)
    {
        initSimpleNLG();
        final LexicalCategory lc = getPOSType(tag).category;
        final NLGElement e = nlgfactory.createWord(word, lc);
        String result = null;
       
        if(word.length() < 2)
            return word;
        
        switch(lc)
        {
            case NOUN:
                e.setFeature(Feature.NUMBER, isPluralNounTag(tag) ? NumberAgreement.PLURAL : NumberAgreement.SINGULAR);
                result = realiser.realise(e).getRealisation();
                break;
                
            case PRONOUN:
                Object nf = e.getFeature(Feature.NUMBER);
                if(nf != null)
                {
                    // switch up number
                    NumberAgreement na = (NumberAgreement)nf == NumberAgreement.SINGULAR ? NumberAgreement.PLURAL : NumberAgreement.SINGULAR;
                    e.setFeature(Feature.NUMBER, na);
                }
                result = realiser.realise(e).getRealisation();
                break;
                
            case DETERMINER:
            {
                String bd = determinerChange.get(word.toLowerCase());     
                result = bd == null ? word : bd;
            }
        }

        e.setFeature(Feature.NUMBER, null);
                
        return 
        Character.isUpperCase(word.charAt(0)) ? 
        Character.toUpperCase(result.charAt(0)) + result.substring(1) :
        Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }
    
    private String changePerson(String word, String tag)
    {        
        initSimpleNLG();
        final LexicalCategory lc = getPOSType(tag).category;
        final NLGElement e = nlgfactory.createWord(word, lc);

        Object nf = e.getFeature(Feature.PERSON);
        if(nf != null)
        {
            Person p = personChange.get((Person)nf);
            if(p != null)
            {
                e.setFeature(Feature.PERSON, p);
            }
        }
        
        String result = realiser.realise(e).getRealisation();
        e.setFeature(Feature.PERSON, null);
        return result;
    }
    
    private String changePronounObjectivity(String pronoun)
    {
        initSimpleNLG();
        boolean capital = Character.isUpperCase(pronoun.charAt(0));
        String bo = objectivityChange.get(pronoun.toLowerCase());     
        if(bo == null)
            return pronoun;
        else if(capital)
            return Character.toUpperCase(bo.charAt(0)) + bo.substring(1);
        else
            return bo;
    }
    
    private String changeVerbForm(String verb, String tag)
    {
        initSimpleNLG();
        boolean capital = Character.isUpperCase(verb.charAt(0));
        Form badForm = verbFormChange.get(getVerbForm(tag)); 
        if(badForm == null)
            badForm = Form.PRESENT_PARTICIPLE;
        
        NLGElement e = nlgfactory.createWord(verb, getPOSType(tag).category);
        e.setFeature(Feature.FORM, badForm);
        String result = realiser.realise(e).getRealisation();
        e.setFeature(Feature.FORM, null); // clear the feature we just set
        if(capital)
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        
        return result;
    }
    
    private String[] getUniqueVerbForms(String verb)
    {
        initSimpleNLG();
        final boolean capital = Character.isUpperCase(verb.charAt(0));
        
        Set<String> forms = new HashSet<String>();
        for(Form form : Form.values())
        {        
            NLGElement e = nlgfactory.createWord(verb, LexicalCategory.VERB);
            e.setFeature(Feature.FORM, form);
            String result = realiser.realise(e).getRealisation();
            e.setFeature(Feature.FORM, null); // clear the feature we just set
            if(capital)
            {
                result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
            }
            forms.add(result);
        }
        
        return forms.toArray(new String[0]);
    }

    public static class GenerateErrorOptions
    {  
        public GrammarTools.Toolkit toolkit;
        public Random random;
        public int errorsPerSentence = DEF_ERRORS_PER_SENTENCE;
        public boolean exactNumberOfErrorsOnly;
        public ErrorType[] errorTypes;
        public long maxItemProcessTime = DEF_MAX_ITEM_PROCESS_TIME; // millseconds, 0 is infinite
    }
    
    public static class ErrorSentence
    {
        private ErrorSentence(String originalForm, String errorForm, Map<Integer, ErrorType> errorTypes)
        {
            this.originalForm = originalForm;
            this.errorForm = errorForm;
            this.errorTypes = errorTypes;
        }
        public final String originalForm;
        public final String errorForm;
        // maps error position to error type
        public final Map<Integer, ErrorType> errorTypes;
    }
    
    public ErrorSentence[] generateErrorSentences(String[] sentences, GenerateErrorOptions o)
    {
        final List<ErrorSentence> result = new LinkedList<ErrorSentence>();
        final double total = sentences.length;
        long progress = 0;
        
        // represents a tagged word, it's index and whether it's been used in creating an error yet
        final class MarkedTagging
        {
            public MarkedTagging(int index, String token, String tag)
            {
                this.token = token;
                this.tag = tag;
                this.origIndex = index;
                this.used = false;
            }
            public final int origIndex;
            public boolean used;
            public String token;
            public String tag;
        }

        for(final String sentence : sentences)
        {
            final Tagging tagging = o.toolkit.tagger.tag(o.toolkit.tokenizer.tokenize(sentence));
            final Map<Integer, ErrorType> errorsIndices = new HashMap<Integer, ErrorType>(o.errorsPerSentence);
            final List<MarkedTagging> errorTags = new ArrayList<MarkedTagging>(tagging.tokens.length);
            
            for(int i = 0; i < tagging.tokens.length; i++)
            {
                errorTags.add(new MarkedTagging(i, tagging.tokens[i], tagging.tags[i]));
            }
            
            for(int n = o.errorsPerSentence; n > 0; n--)
            {
                // gather all error types that can be applied to any of the available tokens
                final Set<ErrorType> validErrorTypes = EnumSet.noneOf(ErrorType.class);
                final List<String> tags = new LinkedList<String>();
                for(MarkedTagging mt : errorTags)
                {
                    if(!mt.used)
                        tags.add(mt.tag);
                }
                
                if(tags.isEmpty())
                    break;
                
                int totalWeight = 0;
                for(ErrorType et : o.errorTypes)
                {
                    if(validErrorTypes.contains(et))
                        continue;

                    // for every pos type, check if any of its tags are in availableTags
                    for(POSType pt : et.posTypes)
                    {
                        if(!Collections.disjoint(Arrays.asList(pt.tags), tags)
                           || pt == POSType.OTHER)
                        {
                            validErrorTypes.add(et);
                            Integer w = et.getWeight();
                            if(w != null)
                                totalWeight += w;
                            break;
                        }
                    }
                }

                // sort error types by weight
                Arrays.sort(o.errorTypes,
                new Comparator<ErrorType>()
                {
                    @Override public int compare(ErrorType o1, ErrorType o2)
                    {
                        return o1.getWeight() - o2.getWeight();
                    }
                });

                // choose an error type randomly based on weights
                ErrorType errorType = null;
                double r = o.random.nextDouble();
                for(ErrorType e : o.errorTypes)
                {
                    if(!validErrorTypes.contains(e))
                        continue;

                    Integer w = e.getWeight();
                    if(w != null)
                    {
                        double wr = (double)w/totalWeight;
                        if(r < wr)
                        {
                            errorType = e;
                            break;
                        }
                        r -= wr;
                    }
                }

                if(errorType == null)
                    continue;

                // gather all tags that apply to this error type
                final ArrayList<Integer> indexes = new ArrayList<Integer>();
                for(int i = 0; i < errorTags.size(); i++)
                {
                    final MarkedTagging et = errorTags.get(i);
                    
                    if(et.used)
                        continue;
                    
                    for(POSType pt : errorType.posTypes)
                    {
                        if(Arrays.asList(pt.tags).contains(et.tag) || pt == POSType.OTHER)
                        {
                            indexes.add(i);
                            break;
                        }
                    }
                }

                // pick a random tag
                final int ir = o.random.nextInt(indexes.size());
                final int i = indexes.get(ir);
                final MarkedTagging errorTag = errorTags.get(i);

                try
                {
                    switch(errorType)
                    {
                        case INSERTION:
                        {
                            int iInsert = o.random.nextInt(errorTags.size());
                            MarkedTagging error = new MarkedTagging(iInsert, errorTag.token, errorTag.tag);
                            error.used = true;
                            errorTags.add(iInsert, error);
                            errorTag.used = true;
                            break;
                        }
                        case OMISSION:
                        {
                            errorTags.remove(i);
                            errorTag.used = true;
                            break;
                        }
                        case DISPLACEMENT:
                        {
                            int iSwap = o.random.nextBoolean() && i < errorTags.size()-1 ? i+1 : i > 0 ? i-1 : i;
                            MarkedTagging mt = errorTags.get(iSwap);
                            errorTags.set(i, mt);
                            errorTags.set(iSwap, errorTag);
                            errorTag.used = !mt.token.equals(errorTag.token);
                            break;
                        }
                        case NUMBER:
                        {
                            String t = changePlurality(errorTag.token, errorTag.tag);
                            if(t != null && !errorTag.token.equals(t))
                            {
                                errorTag.token = t;
                                errorTag.used = true;
                            }
                            break;
                        }
                        case TENSE:
                        {
                            String t = changeVerbForm(errorTag.token, errorTag.tag);
                            if(t != null && !errorTag.token.equals(t))
                            {
                                errorTag.token = t;
                                errorTag.used = true;
                            }
                            break;
                        }
                        case OBJECTIVITY:
                        {
                            String t = changePronounObjectivity(errorTag.token);
                            if(t != null && !errorTag.token.equals(t))
                            {
                                errorTag.token = t;
                                errorTag.used = true;
                            }
                            break;                    
                        }
                        case CASE:
                        {
                            char c = errorTag.token.charAt(0);
                            c = Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c);
                            String t = errorTag.token;
                            t = c + (t.length() > 1 ? t.substring(1) : "");
                            if(!errorTag.token.equals(t))
                            {
                                errorTag.token = t;
                                errorTag.used = true;
                            }
                            break;
                        }
                    }
                }
                catch(Exception e)
                {
                    System.out.println(errorTag.token + " : " + errorTag.tag + " : " + errorType);
                    e.printStackTrace();
                }
                
                // if error tag was unused, do not count it as an error
                if(!errorTag.used)
                {
                    errorTag.used = true;
                    n++;
                    continue;
                }
                else
                {
                    errorsIndices.put(errorTag.origIndex, errorType);                    
                }
            }

            // rebuild a sentence from the error tokens
            String errorWords = "";
            for(MarkedTagging mt : errorTags)
            {
                errorWords += mt.token + " ";
            }

            if(errorsIndices.size() > 0 
            && (!o.exactNumberOfErrorsOnly || errorsIndices.size() == o.errorsPerSentence))
            {
                result.add(new ErrorSentence(sentence, errorWords.trim(), errorsIndices));
            }
            
            updateUI( ++progress/total, "" );
        }
        
        return result.toArray(new ErrorSentence[0]);
    }
    
    private static int findimin( double[] d )
    {
        double min = Double.MAX_VALUE;
        int iMin = -1;
        for( int i = 0; i < d.length; i++ )
        {
            if( d[i] < min )
            {
                min = d[i];
                iMin = i;
            }
        }
        return iMin;
    }
   
    private static Prolog createGrammar(String grammarFile) 
            throws InvalidTheoryException, InvalidLibraryException, FileNotFoundException, IOException
    {
        Prolog engine = new Prolog();
        engine.loadLibrary(new DCGLibrary());
        engine.setTheory(new alice.tuprolog.Theory(new java.io.FileInputStream(grammarFile)));
        return engine;
    }
    
    private static SolveInfo queryGrammar(Prolog engine, String goal, String[] terms) 
            throws MalformedGoalException
    {
        goal = goal.toLowerCase();
        
        String termlist = "[";
        for( int i = 0; i < terms.length; ++i )
        {
            String t = terms[i].toLowerCase();
            if(!t.matches("[a-z]\\w*"))
                t = "'" + t.replaceAll( "'", "''" ) + "'";

            termlist += t + (i < terms.length - 1 ?  "," : "");
        }
        termlist += "]";
        
        return engine.solve("phrase(" + goal + ", " + termlist + ").");
    }
  
    private static final String PRE_HEX  = "_x";
    private static final String POST_HEX = "_";
    private static final String XML_ROOT         = "sentences";
    private static final String XML_ELEM_SENT    = "sentence";
    private static final String XML_ELEM_TOKENS  = "tokens";
    private static final String XML_ELEM_TOKEN   = "token";
    private static final String XML_ELEM_TAGGING = "tagging";
    private static final String XML_ELEM_PARSE   = "parse";
    private static final String XML_ATTRIB_GRAMMATICAL = "grammatical";
    private static final String XML_ATTRIB_NUM_ERRORS = "num_errors";
    private static final String XML_ATTRIB_TAGSEQPROB = "tagseqprob";
    private static final String XML_ATTRIB_PROB  = "prob";
    private static final String XML_ATTRIB_DESC  = "desc";
    private static final String XML_COMMENT      = "GrammarTools :: Sentences :: v1.0";
    

    private static final String SEP = File.separator;
    private static final String OPENNLP_TOKEN_MODEL = "english" + SEP + "en-token.bin";
    private static final String OPENNLP_SENT_MODEL  = "english" + SEP + "en-sent.bin";
    private static final String OPENNLP_TAG_MODEL   = "english" + SEP + "en-pos-maxent.bin";
    //private static final String OPENNLP_TAG_MODEL = "english" + SEP + "en-pos-perceptron";
    //private static final String OPENNLP_TAG_DICT    = "english" + SEP + "tagdict";
    private static final String OPENNLP_CHUNK_MODEL = "english" + SEP + "en-chunker.bin";
    private static final String OPENNLP_PARSE_MODEL = "english" + SEP + "en-parser-chunking.bin";
    
    private static final String STANFORD_TAG_SUBPATH    = "english" + SEP + "postag" + SEP + "left3words-distsim.tagger";
    private static final String STANFORD_PARSE_SUBPATH  = "english" + SEP + "parser" + SEP + "englishPCFG.ser.gz";
    
    private static final double DEF_K_VALUE = 1.15;
    private static final int DEF_ERRORS_PER_SENTENCE = 1;
    private static final int DEF_PROB_PRECISION = 2;
    private static final int DEF_MAX_RESULTS = 1;
    private static final long DEF_MAX_ITEM_PROCESS_TIME = 0;
    
    private Lexicon lexicon;
    private NLGFactory nlgfactory;
    private Realiser realiser;
    private Map<Form, Form> verbFormChange;
    private Map<Person, Person> personChange;
    private Map<String, String> objectivityChange;
    private Map<String, String> determinerChange;
    private Map<String, Form> verbForm;
    private Set<String> pluralNounTags;
    
    private Toolkit opennlpToolkit;
    private Toolkit stanfordToolkit;
    
    private UIWorker uiWorker;
    
    public static class Toolkit
    {
        public Toolkit(String name) { this.name = name; }
        @Override
        public String toString() { return "--- " + name + " ---"; }
        public final String name;
        public SentenceSplitter sentenceSplitter;
        public Tokenizer tokenizer;
        public Tagger tagger;
        public Chunker chunker;
        public Parser parser;
    };
    
    public static final String[] PennTreebankClauseTags =
            new String[]
            {
                "S", "SBAR", "SBARQ", "SINV", "SQ",
            };
    
    public static final String[] PennTreebankPhraseTags = 
            new String[]
            {
                "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST", "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP",
                "RRC", "UCP", "VP", "WHADJP", "WHAVP", "WHNP", "WHPP", "X",
            };

    public static final String[] PennTreebankNounPhraseTags = new String[]{ "NP", "WHNP", };
    public static final String[] PennTreebankVerbPhraseTags = new String[]{ "VP", };

    public static final String[] PennTreebankWordTags = 
            new String[]
            {
                "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT",
                "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH",
                "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB",
                ",", ":", ".", 
            };
    
    public static final String[] PennTreebankAllTags =
            new String[] 
            {
                "ADJP"      ,
                "-ADV"      ,
                "ADVP"      ,
                "-BNF"      ,
                "CC"        ,
                "CD"        ,
                "-CLF"      ,
                "-CLR"      ,
                "CONJP"     ,
                "-DIR"      ,
                "DT"        ,
                "-DTV"      ,
                "EX"        ,
                "-EXT"      ,
                "FRAG"      ,
                "FW"        ,
                "-HLN"      ,
                "IN"        ,
                "INTJ"      ,
                "JJ"        ,
                "JJR"       ,
                "JJS"       ,
                "-LGS"      ,
                "-LOC"      ,
                "LS"        ,
                "LST"       ,
                "MD"        ,
                "-MNR"      ,
                "NAC"       ,
                "NN"        ,
                "NNS"       ,
                "NNP"       ,
                "NNPS"      ,
                "-NOM"      ,
                "NP"        ,
                "NX"        ,
                "PDT"       ,
                "POS"       ,
                "PP"        ,
                "-PRD"      ,
                "PRN"       ,
                "PRP"       ,
                "-PRP"      ,
                "PRP$"      ,
                "PRP-S"     ,
                "PRT"       ,
                "-PUT"      ,
                "QP"        ,
                "RB"        ,
                "RBR"       ,
                "RBS"       ,
                "RP"        ,
                "RRC"       ,
                "S"         ,
                "SBAR"      ,
                "SBARQ"     ,
                "-SBJ"      ,
                "SINV"      ,
                "SQ"        ,
                "SYM"       ,
                "-TMP"      ,
                "TO"        ,
                "-TPC"      ,
                "-TTL"      ,
                "UCP"       ,
                "UH"        ,
                "VB"        ,
                "VBD"       ,
                "VBG"       ,
                "VBN"       ,
                "VBP"       ,
                "VBZ"       ,
                "-VOC"      ,
                "VP"        ,
                "WDT"       ,
                "WHADJP"    ,
                "WHADVP"    ,
                "WHNP"      ,
                "WHPP"      ,
                "WP"        ,
                "WP$"       ,
                "WP-S"      ,
                "WRB"       ,
                "X"         ,
            };
}
