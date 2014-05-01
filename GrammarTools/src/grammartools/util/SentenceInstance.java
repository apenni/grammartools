package grammartools.util;

import grammartools.parser.ParseTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

/**
 * Represents an instance of attribute values for classification.
 */
public class SentenceInstance
{
    // An attribute is a field exported as part of the sentence instance data
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface Attribute
    {
       enum Type {NUMERIC, BOOL, TAG}
       // index order of the attribute, or -1 for alphabetical ordering
       // once a type of ordering is chosen (numbered or alphabetical), it should be used throughout for consistency
       int index() default -1;
       // name of the attribute, or empty string to use field name
       String name() default "";
       // type of the attribute, numeric, boolean nominal, or tag nominal
       Type type() default Type.NUMERIC;
       // if the field is a class attribute or not
       boolean isClass() default false;       
    }

    // Class Attributes ------------------------------------------------------------------
    @Attribute(isClass=true)                            public double nGrammaticalErrors;
    @Attribute(isClass=true, type=Attribute.Type.BOOL)  public double isGrammatical;

    // Attrributes ------------------------------------------------------------------------
    @Attribute() public double nTokens;
    @Attribute() public double nVerbs;

    @Attribute() public double funcValue;
        
    @Attribute() public double opennlpParseProb;
    @Attribute() public double opennlpDeltaParseProb;
    @Attribute() public double opennlpTagMismatchTotal;
    @Attribute() public double opennlpTagMismatchRatio;

    @Attribute() public double stanfordParseProb;
    @Attribute() public double stanfordDeltaParseProb;
    @Attribute() public double stanfordTagMismatchTotal;
    @Attribute() public double stanfordTagMismatchRatio;    

    @Attribute() public double opennlpStanfordCrossDeltaParseProb;
    @Attribute() public double opennlpStanfordCrossTagMismatchTotal;
    @Attribute() public double opennlpStanfordCrossTagMismatchRatio;
    @Attribute() public double opennlpStanfordCrossPTagMismatchTotal;
    @Attribute() public double opennlpStanfordCrossPTagMismatchRatio;
    @Attribute() public double opennlpStanfordCrossParseMismatchTotal;
    @Attribute() public double opennlpStanfordCrossParseMismatchRatio;
    
    @Attribute() public double tagSeqProb;
    @Attribute() public double deltaTagSeqProb;
    
    @Attribute() public double firstTagProb;
    @Attribute() public double minTagProb;
    @Attribute() public double maxTagProb;
    @Attribute() public double deltaMinTagProb;
    @Attribute() public double deltaMaxTagProb;
    @Attribute() public double rangeTagProb;
    
    @Attribute() public double firstPTagProb;
    @Attribute() public double minPTagProb;
    @Attribute() public double maxPTagProb;
    @Attribute() public double deltaMinPTagProb;
    @Attribute() public double deltaMaxPTagProb;
    @Attribute() public double rangePTagProb;

    @Attribute() public double minDeltaTagPTagProb;
    @Attribute() public double maxDeltaTagPTagProb;
    @Attribute() public double totDeltaTagPTagProb;
    @Attribute() public double avgDeltaTagPTagProb;
    
    @Attribute() public double minDeltaVerbChangeTagProb;
    @Attribute() public double maxDeltaVerbChangeTagProb;
    @Attribute() public double totDeltaVerbChangeTagProb;
    @Attribute() public double avgDeltaVerbChangeTagProb;
    
    @Attribute() public double minDeltaVerbChangePTagProb;
    @Attribute() public double maxDeltaVerbChangePTagProb;
    @Attribute() public double totDeltaVerbChangePTagProb;
    @Attribute() public double avgDeltaVerbChangePTagProb;
    
    @Attribute() public double minDeltaVerbChangeTagSeqProb;
    @Attribute() public double maxDeltaVerbChangeTagSeqProb;
    @Attribute() public double totDeltaVerbChangeTagSeqProb;
    @Attribute() public double avgDeltaVerbChangeTagSeqProb;
    
    @Attribute() public double minDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double maxDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double totDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double avgDeltaVerbChangeOpennlpParseProb;
    
    @Attribute() public double minDeltaVerbChangeStanfordParseProb;
    @Attribute() public double maxDeltaVerbChangeStanfordParseProb;
    @Attribute() public double totDeltaVerbChangeStanfordParseProb;
    @Attribute() public double avgDeltaVerbChangeStanfordParseProb;

    @Attribute() public double totVerbChangeTagImprove;
    @Attribute() public double avgVerbChangeTagImprove;

    @Attribute() public double totVerbChangePTagImprove;
    @Attribute() public double avgVerbChangePTagImprove;

    @Attribute() public double totVerbChangeTagSeqImprove;
    @Attribute() public double avgVerbChangeTagSeqImprove;

    @Attribute() public double totVerbChangeOpennlpParseImprove;
    @Attribute() public double avgVerbChangeOpennlpParseImprove;

    @Attribute() public double totVerbChangeStanfordParseImprove;
    @Attribute() public double avgVerbChangeStanfordParseImprove;
    
    @Attribute() public double minVerbChangeImproveRatio;
    @Attribute() public double maxVerbChangeImproveRatio;
    @Attribute() public double totVerbChangeImproveRatio;
    @Attribute() public double avgVerbChangeImproveRatio;
    
    @Attribute() public double deltaMinChangeTagSeqProb;
    @Attribute() public double deltaMinChangeOpennlpParseProb;
    @Attribute() public double deltaMinChangeStanfordParseProb;
    
    @Attribute() public double opennlpDeltaParseProbSwapMinLeft;
    @Attribute() public double opennlpDeltaParseProbSwapMinRight;
    @Attribute() public double opennlpDeltaParseProbOmitMin;
    @Attribute() public double opennlpDeltaParseProbOmitMinLeft;
    @Attribute() public double opennlpDeltaParseProbOmitMinRight;
    
    @Attribute() public double stanfordDeltaParseProbSwapMinLeft;
    @Attribute() public double stanfordDeltaParseProbSwapMinRight;
    @Attribute() public double stanfordDeltaParseProbOmitMin;
    @Attribute() public double stanfordDeltaParseProbOmitMinLeft;
    @Attribute() public double stanfordDeltaParseProbOmitMinRight;
    
    @Attribute(type=Attribute.Type.TAG) public double firstTag;
    @Attribute(type=Attribute.Type.TAG) public double minTag;
    @Attribute(type=Attribute.Type.TAG) public double maxTag;
    
    @Attribute(type=Attribute.Type.TAG) public double firstPTag;
    @Attribute(type=Attribute.Type.TAG) public double minPTag;
    @Attribute(type=Attribute.Type.TAG) public double maxPTag;
    
    @Attribute(type=Attribute.Type.BOOL) public double isFirstTagMismatch;
    @Attribute(type=Attribute.Type.BOOL) public double isMinTagMismatch;
    @Attribute(type=Attribute.Type.BOOL) public double isMaxTagMismatch;
    
    @Attribute(type=Attribute.Type.BOOL) public double isStanfordClause;
    @Attribute(type=Attribute.Type.BOOL) public double isOpennlpClause;
    
    @Attribute(type=Attribute.Type.BOOL) public double isDCGParsable;
    
    // Simplified clausal attributes --------------------------------------------------------
    @Attribute() public double s_nClauses;
    
    @Attribute() public double s_minOpennlpParseProb;
    @Attribute() public double s_maxOpennlpParseProb;
    @Attribute() public double s_totOpennlpParseProb;
    @Attribute() public double s_avgOpennlpParseProb;

    @Attribute() public double s_minStanfordParseProb;
    @Attribute() public double s_maxStanfordParseProb;    
    @Attribute() public double s_totStanfordParseProb;
    @Attribute() public double s_avgStanfordParseProb;

    @Attribute() public double s_minDeltaVerbChangeTagProb;
    @Attribute() public double s_maxDeltaVerbChangeTagProb;
    @Attribute() public double s_totDeltaVerbChangeTagProb;
    @Attribute() public double s_avgDeltaVerbChangeTagProb;

    @Attribute() public double s_minDeltaVerbChangePTagProb;
    @Attribute() public double s_maxDeltaVerbChangePTagProb;
    @Attribute() public double s_totDeltaVerbChangePTagProb;
    @Attribute() public double s_avgDeltaVerbChangePTagProb;

    @Attribute() public double s_minDeltaVerbChangeTagSeqProb;
    @Attribute() public double s_maxDeltaVerbChangeTagSeqProb;
    @Attribute() public double s_totDeltaVerbChangeTagSeqProb;
    @Attribute() public double s_avgDeltaVerbChangeTagSeqProb;

    @Attribute() public double s_minDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double s_maxDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double s_totDeltaVerbChangeOpennlpParseProb;
    @Attribute() public double s_avgDeltaVerbChangeOpennlpParseProb;

    @Attribute() public double s_minDeltaVerbChangeStanfordParseProb;
    @Attribute() public double s_maxDeltaVerbChangeStanfordParseProb;
    @Attribute() public double s_totDeltaVerbChangeStanfordParseProb;
    @Attribute() public double s_avgDeltaVerbChangeStanfordParseProb;

    @Attribute() public double s_totVerbChangeTagImprove;
    @Attribute() public double s_avgVerbChangeTagImprove;

    @Attribute() public double s_totVerbChangePTagImprove;
    @Attribute() public double s_avgVerbChangePTagImprove;

    @Attribute() public double s_totVerbChangeTagSeqImprove;
    @Attribute() public double s_avgVerbChangeTagSeqImprove;

    @Attribute() public double s_totVerbChangeOpennlpParseImprove;
    @Attribute() public double s_avgVerbChangeOpennlpParseImprove;

    @Attribute() public double s_totVerbChangeStanfordParseImprove;
    @Attribute() public double s_avgVerbChangeStanfordParseImprove;

    @Attribute() public double s_minVerbChangeImproveRatio;
    @Attribute() public double s_maxVerbChangeImproveRatio;
    @Attribute() public double s_totVerbChangeImproveRatio;
    @Attribute() public double s_avgVerbChangeImproveRatio;
    
    @Attribute() public double s_minDeltaMinChangeTagSeqProb;
    @Attribute() public double s_maxDeltaMinChangeTagSeqProb;
    @Attribute() public double s_totDeltaMinChangeTagSeqProb;
    @Attribute() public double s_avgDeltaMinChangeTagSeqProb;
    
    @Attribute() public double s_minDeltaMinChangeOpennlpParseProb;
    @Attribute() public double s_maxDeltaMinChangeOpennlpParseProb;
    @Attribute() public double s_totDeltaMinChangeOpennlpParseProb;
    @Attribute() public double s_avgDeltaMinChangeOpennlpParseProb;
    
    @Attribute() public double s_minDeltaMinChangeStanfordParseProb;
    @Attribute() public double s_maxDeltaMinChangeStanfordParseProb;
    @Attribute() public double s_totDeltaMinChangeStanfordParseProb;
    @Attribute() public double s_avgDeltaMinChangeStanfordParseProb;
    
    // ------------------------------------------------------------------------


    public SentenceInstance()
    {
        // build the attribute list if not already
        buildAttributeList();
        
        // initialize attributes to unknown value
        for(Field f : attributes)
        {
            try { f.setDouble(this, NOVALUE); }
            catch(IllegalAccessException e){}
        }
        for(Field f : classes)
        {
            try { f.setDouble(this, NOVALUE); }
            catch(IllegalAccessException e){}
        }
    }
    
    private static void buildAttributeList()
    {
        // build attribute & class collection, if not built already
        if(attributes.isEmpty() && classes.isEmpty())
        {
            for(Field f : SentenceInstance.class.getFields())
            {
                Attribute a = f.getAnnotation(Attribute.class);
                if(a != null && f.getType() == double.class)
                {
                    (a.isClass() ? classes : attributes).add(f);
                }
            }
        }        
    }
    
    /**
     * Converts the SentenceInstance into a Weka Instance suitable for use with Weka APIs.
     * @param useBinaryClass whether to include grammatical correctness (yes/no) as a class attribute
     * @param useNumericClass whether to include number of grammatical errors as a class attribute
     * @return a weka instance representing this SentenceInstance's attribute and class values
     */
    public weka.core.Instance toWekaInstance(boolean useBinaryClass, boolean useNumericClass)
    {       
        int i = 0;
        double[] values = new double[attributes.size() + classes.size()];
        
        for(Field f : attributes)
        {
            try { values[i++] = f.getDouble(this); }
            catch(IllegalAccessException e){}
        }        
        
        for(Field f : classes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(useNumericClass && a != null && a.type() == Attribute.Type.NUMERIC)
            {
                try { values[i++] = f.getDouble(this); }
                catch(IllegalAccessException e){}
            }
            if(useBinaryClass && a != null && a.type() == Attribute.Type.BOOL)
            {
                try { values[i++] = f.getDouble(this); }
                catch(IllegalAccessException e){}
            }
        }
        
        return new weka.core.DenseInstance(1.0, Arrays.copyOf(values, i));
    }
    
    /**
     * Converts the SentenceInstance into a Weka Instance suitable for use with Weka APIs.
     * @param header weka header specifying dataset format
     * @return a weka instance representing this SentenceInstance's attribute and class values
     */
    public weka.core.Instance toWekaInstance(weka.core.Instances header)
    {       
        int i = 0;
        final double[] values = new double[header.numAttributes()];
        Arrays.fill(values, NOVALUE);
        
        for(Field f : attributes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                String name = a.name().isEmpty() ? f.getName() : a.name();
                weka.core.Attribute att = header.attribute(name);
                if(att != null && att.index() > 0)
                {
                    try { values[att.index()] = f.getDouble(this); }
                    catch(IllegalAccessException e){}
                }
                else if(i < values.length)
                {
                    try { values[i++] = f.getDouble(this); }
                    catch(IllegalAccessException e){}
                }
            }
        }        
        
        for(Field f : classes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                String name = a.name().isEmpty() ? f.getName() : a.name();
                weka.core.Attribute att = header.attribute(name);
                if(att != null && att.index() > 0)
                {
                    try { values[att.index()] = f.getDouble(this); }
                    catch(IllegalAccessException e){}
                }
                else if(i < values.length)
                {
                    try { values[i++] = f.getDouble(this); }
                    catch(IllegalAccessException e){}
                }
            }
        }
        
        return new weka.core.DenseInstance(1.0, values);
    }

    /**
     * Creates an empty set of weka Instances that specify the header format (number & type of attributes) of the relation.
     * @param useBinaryGrammaticalAttrib whether to include grammatical correctness (yes/no) as a class attribute
     * @param useNumericGrammaticalAttrib whether to include number of grammatical errors as a class attribute
     * @return an empty set of weka instances with the header format specified.
     */
    public static weka.core.Instances createWekaHeader(boolean useBinaryGrammaticalAttrib, boolean useNumericGrammaticalAttrib)
    {
        buildAttributeList();
        
        final java.util.ArrayList<weka.core.Attribute> atts = new java.util.ArrayList<weka.core.Attribute>();
        
        for(Field f : attributes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                String name = a.name().isEmpty() ? f.getName() : a.name();
                switch(a.type())
                {
                    case NUMERIC:
                        atts.add(new weka.core.Attribute(name));
                        break;
                    case BOOL:
                        atts.add(new weka.core.Attribute(name, NOMBOOLS));
                        break;
                    case TAG:
                        atts.add(new weka.core.Attribute(name, NOMWORDTAGS));
                        break;
                }
            }
        }
            
        for(Field f : classes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                String name = a.name().isEmpty() ? f.getName() : a.name();
                switch(a.type())
                {
                    case NUMERIC:
                        if(useNumericGrammaticalAttrib)
                            atts.add(new weka.core.Attribute(name));
                        break;
                    case BOOL:
                        if(useBinaryGrammaticalAttrib)
                            atts.add(new weka.core.Attribute(name, NOMBOOLS));
                        break;
                    case TAG:
                        atts.add(new weka.core.Attribute(name, NOMWORDTAGS));
                        break;
                }
            }
        }
               
        weka.core.Instances header = new weka.core.Instances("Sentence", atts, 0);
        header.setClassIndex(header.numAttributes()-1);
        return header;
    }
        
    /**
     * Get all attribute names and values (excluding class attributes).
     * Attribute values that are indexes into a nominal set will be replaced by their actual nominal value.
     * @return map of attribute names and values (excluding class attributes).
     */
    public java.util.Map<String, Object> getAttributes()
    {
        final java.util.Map<String, Object> m = new java.util.HashMap<String, Object>(attributes.size());
        
        for(Field f : attributes)
        {           
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                try
                {
                    Object v = null;
                    double d = f.getDouble(this);
                    switch(a.type())
                    {
                        case NUMERIC:
                            v = d;
                            break;
                        case BOOL:
                            v = fromBoolNominalIndex(d);
                            break;
                        case TAG:
                            v = fromTagNominalIndex(d);
                            break;
                    }
                    String name = a.name().isEmpty() ? f.getName() : a.name();
                    m.put(name, v);
                }
                catch (IllegalArgumentException ex) {}
                catch (IllegalAccessException ex) {}
            }
        }        
        return m;
    }
    
/**
     * Get all attribute names (excluding class attributes).
     * @return array of attribute names not including class attributes.
     */
    public static List<String> getAttributeNames()
    {
        buildAttributeList();
        
        List<String> names = new LinkedList<String>();
        for(Field f : attributes)
        {
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
                names.add(a.name().isEmpty() ? f.getName() : a.name());
        }
        
        return names;
    }
     
    /**
     * Determines if a value is valid or unknown.
     * @param value
     * @return true if the value is specified, false otherwise.
     */
    public static boolean hasValue(double value)
    {
        return Double.compare(value, NOVALUE) != 0;
    }
    /**
     * Chooses the maximum of two values, or the second value if the first value is an unknown value.
     * @param v1
     * @param v2
     * @return 
     */
    public static double maxOrValue(double v1, double v2)
    {
        return hasValue(v1) ? Math.max(v1, v2) : v2;
    }
    /**
     * Chooses the minimum of two values, or the second value if the first value is an unknown value.
     * @param v1
     * @param v2
     * @return 
     */
    public static double minOrValue(double v1, double v2)
    {
        return hasValue(v1) ? Math.min(v1, v2) : v2;
    }
    
    /**
     * Calculates and stores the number of disagreements in constituents between two trees.
     * @param p1
     * @param p2 
     */
    public void setParseTreeMismatches(ParseTree p1, ParseTree p2)
    {
        int[] c = getParseTreeMismatches(p1, p2);
        this.opennlpStanfordCrossParseMismatchTotal = c[0];
        this.opennlpStanfordCrossParseMismatchRatio = (double)c[0] / c[1];
    }
    
    private static int[] getParseTreeMismatches(ParseTree p1, ParseTree p2)
    {
        int[] c = 
        { 
            // mismatched comparisons
            (p1.isRoot() && p2.isRoot() || p1.getValue().equals(p2.getValue())) ? 0 : 1,
            // total comparisons
            1
        };
        if(p1.getNumChildren() > 0 && p2.getNumChildren() > 0)
        {
            ParseTree[] p1Children = p1.getChildren();
            ParseTree[] p2Children = p2.getChildren();
            for(int i = 0; i < p1Children.length && i < p2Children.length; i++)
            {
                int[] r = getParseTreeMismatches(p1Children[i], p2Children[i]);
                c[0] += r[0];
                c[1] += r[1];
            }
        }
        return c;
    }
    
    /**
     * Converts a boolean value to an index into its nominal set.
     * @param value
     * @return the index of the value in the nominal set
     */
    public static double toBoolNominalIndex(boolean value)
    {
        return NOMBOOLS.indexOf(""+value);
    }
    
    /**
     * Converts a tag to an index into its nominal set.
     * @param value
     * @return the index of the value in the nominal set
     */
    public static double toTagNominalIndex(String tag)
    {
        int i = NOMWORDTAGS.indexOf(tag);
        return i == -1 ? NOVALUE : i;
    }
    
    public static boolean fromBoolNominalIndex(double index)
    {
        return Boolean.valueOf(NOMBOOLS.get((int)index));
    }
    
    public static String fromTagNominalIndex(double index)
    {
        return index == NOVALUE || index < 0 ? null : NOMWORDTAGS.get((int)index);
    }
    
    @Override
    public String toString()
    {
        return toString(3,4);
    }
    public String toString(int width, int precision)
    {
        String s = "[" + this.getClass().getSimpleName() + ": ";
        
        for(Field f : attributes)
        {
            s += f.getName() + " = "; 
            
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                try
                {
                    double v = f.getDouble(this);
                    switch(a.type())
                    {
                        case NUMERIC:
                            s += String.format("% " + width + "." + precision + "f", v);
                            break;
                        case BOOL:
                            s += fromBoolNominalIndex(v);
                            break;
                        case TAG:
                            s += fromTagNominalIndex(v);
                            break;
                    }
                }
                catch (IllegalArgumentException ex) { s += "[error]"; }
                catch (IllegalAccessException ex) { s += "[error]"; }
            }
            
            s += ", ";
        }
        
        for(Field f : classes)
        {
            s += f.getName() + " = "; 
            
            Attribute a = f.getAnnotation(Attribute.class);
            if(a != null)
            {
                try
                {
                    double v = f.getDouble(this);
                    switch(a.type())
                    {
                        case NUMERIC:
                            s += String.format("% " + width + "." + precision + "f", v);
                            break;
                        case BOOL:
                            s += fromBoolNominalIndex(v);
                            break;
                        case TAG:
                            s += fromTagNominalIndex(v);
                            break;
                    }
                }
                catch (IllegalArgumentException ex) { s += "[error]"; }
                catch (IllegalAccessException ex) { s += "[error]"; }
            }
            
            s += ", ";
        }   

        return s.replaceFirst(", \\z", "]");
    }
    
    private static final Comparator<Field> AttributeComparator = new Comparator<Field>() 
    {
        @Override
        public int compare(Field o1, Field o2)
        {
            Attribute o1a = o1.getAnnotation(Attribute.class);
            Attribute o2a = o2.getAnnotation(Attribute.class);
            
            // should not be comparing non-attributes
            if(o1a == null && o2a == null)
                return 0;
            
            // if both unordered or have same index, order by name
            if(o1a.index() == o2a.index())
            {
                return o1.getName().compareTo(o2.getName());
            }
            
            // otherwise order by index
            int o1i = o1a.index() < 0 ? Integer.MAX_VALUE : o1a.index();
            int o2i = o2a.index() < 0 ? Integer.MAX_VALUE : o2a.index();
            return o1i - o2i;
        }
    }; 

    private static final SortedSet<Field> attributes= new java.util.TreeSet<Field>(AttributeComparator);
    private static final SortedSet<Field> classes   = new java.util.TreeSet<Field>(AttributeComparator);
    private static final double         NOVALUE     = weka.core.Utils.missingValue();
    private static final List<String>   NOMBOOLS    = Arrays.asList(new String[] { ""+true, ""+false });
    private static final List<String>   NOMWORDTAGS = Arrays.asList(grammartools.GrammarTools.PennTreebankWordTags);
}
