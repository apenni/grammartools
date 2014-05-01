package corpustools;

public class CorpusToolsApp 
{
    public static void main( String[] args ) 
    {
        // set native Look&Feel
        try { javax.swing.UIManager.setLookAndFeel( javax.swing.UIManager.getSystemLookAndFeelClassName() ); }
        catch( Exception e ) {}

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                new corpustools.CorpusToolsFrame().setVisible(true);
            }
        });
    }
}
