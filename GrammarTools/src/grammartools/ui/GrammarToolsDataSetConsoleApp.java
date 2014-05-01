/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package grammartools.ui;

import grammartools.GrammarTools;
import grammartools.util.FileProcessor;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Anthony
 */
public class GrammarToolsDataSetConsoleApp
{
    public static void main( final String[] args )
    {
        System.out.println("GrammarTools - DataSet Generator");
        
        if(args.length < 3 || !args[0].trim().matches("\\d+"))
        {
            System.out.println("Warning: invalid arguments!");
        }

        final double nErrors = Double.valueOf(args[0].trim());
        final File  inputFile = new File(args[1]);
        String outputFile = args[2];
        final List<Exception> errors = new LinkedList<Exception>();
        
        if(!inputFile.exists())
        {
            System.out.println("Input file does not exist!");
        }
        if(outputFile.isEmpty())
        {
            outputFile = DEF_OUTPUT_DATASET_FILE;
        }
        
        final GrammarTools grammarTools = new GrammarTools();
        
        try
        {
            System.out.println("Initializing OpenNLP tools...");
            grammarTools.initOpenNLPToolkit( DEF_OPENNLP_MODEL_PATH ); 
            
            System.out.println("Initializing Stanford tools...");
            grammarTools.initStanfordToolkit( DEF_STANFORD_MODEL_PATH ); 
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        
        grammarTools.setUIWorker(
            new GrammarTools.UIWorker() 
            {
                @Override
                public void update(double progress, String message)
                {
                    System.out.printf("\n%.3f%%", 100 * progress);
                }
                @Override
                public boolean isCancelled()
                {
                    return false;
                }
            });
        
        final String input = FileProcessor.buildFileInput(inputFile, errors);
        
        final GrammarTools.DatasetOptions options = new GrammarTools.DatasetOptions();
        options.grammarFile = DEF_INPUT_GRAMMAR_FILE;
        options.isAppending = false;
        options.useBinaryAttrib = true;
        options.useNumericAttrib= true;
        options.outputFormat = GrammarTools.DatasetOptions.OutputFormat.ARFF;
        options.outputFile = outputFile;
        options.nGrammaticalErrorsPerSentence = nErrors;
        
        try
        {
            options.functionScript = GrammarTools.getDefaultFunctionScript();
        }
        catch(NoSuchFieldException ex)
        {
            ex.printStackTrace();
        }
        
        final long start = System.currentTimeMillis()/1000;
        
        try
        {
            grammarTools.runDatasetTool(input, options);
            new File(outputFile + ".done").createNewFile();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

        final long end = System.currentTimeMillis()/1000;
        
        System.out.println("\nTotal time: " + (end - start) + " seconds");
    }
    
    private static final String SEP = File.separator;
    private static final String DEF_OPENNLP_MODEL_PATH  = "models" + SEP + "opennlp";
    private static final String DEF_STANFORD_MODEL_PATH = "models" + SEP + "stanford";
    private static final String DEF_INPUT_GRAMMAR_FILE = "grammar.pl";
    private static final String DEF_OUTPUT_DATASET_FILE = "sentences.arff";    
}
