package grammartools.ui;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class SwingUtils
{
    public static String browseForFile(Window owner, String file, int selectionMode, String title) 
    {
        return browseForFile(owner, file, selectionMode, title, null);
    }
    public static String browseForFile(Window owner, String file, int selectionMode, String title, FileFilter filter) 
    {
        final String curDir = System.getProperty( "user.dir" );
        JFileChooser chooser = new JFileChooser( curDir );
        chooser.setDialogType( JFileChooser.OPEN_DIALOG );
        chooser.setFileSelectionMode( selectionMode );
        chooser.setApproveButtonText( "Select" );
        chooser.setApproveButtonMnemonic( 's' );
        chooser.setDialogTitle(title);
        if(filter != null)
            chooser.setFileFilter(filter);
        
        if(file != null && !file.isEmpty())
        {
            File curFile = new File(file);
            
            chooser.setCurrentDirectory(curFile.getAbsoluteFile().getParentFile());
            
            if(curFile.isDirectory())
            {
                try { chooser.setSelectedFile(curFile.getCanonicalFile()); }
                catch (IOException ex) { }
            }
            else
            {
                chooser.setSelectedFile(curFile);
            }
        }
        
        if( chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION ) 
        {
            String path = chooser.getSelectedFile().getPath();
            try { path = new File(path).getCanonicalPath(); } catch(IOException e){}
            // make path relative if possible
            if( path.startsWith( curDir ) ) 
            {
                path = "." + path.substring( curDir.length() );
            }

            return path;
        }
        return null;
    }    
    
    public static void showMessage(Window owner, String msg, String title)
    {
        JOptionPane.showMessageDialog(owner, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showWarning(Window owner, String msg, String title)
    {
        JOptionPane.showMessageDialog(owner, msg, title, JOptionPane.WARNING_MESSAGE);
    }
}
