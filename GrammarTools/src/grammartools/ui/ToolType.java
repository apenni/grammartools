package grammartools.ui;

public enum ToolType 
{
    TAG     ( "Tag"     , "Tag each word in a sentence with its part-of-speech" ),
    CHUNK   ( "Chunk"   , "Tag groups of words in a sentence into constituent phrases" ),
    PARSE   ( "Parse"   , "Parse a sentence into a syntax tree " ),
    XML     ( "XML"     , "Output processed sentences in GrammarTools XML format" ),
    STATS   ( "Stats"   , "Generate statistics from GrammarTools XML sentences" ),
    DCG     ( "DCG"     , "Parse a sentence against a Prolog definite clause grammar" ),
    FUNCTION( "Function", "Define & calculate a function of variable sentence attributes" ),
    DATASET ( "Dataset" , "Generate a dataset of sentences in ARFF or CSV format" ),
    CLASSIFY( "Classify", "Use a trained classifier model to predict grammactical correctness" );
    private ToolType( String name, String desc ) { this.name = name; this.desc = desc; }
    @Override
    public String toString() { return name; }
    public String getDescription()  { return desc; }
    private final String name, desc;
};