/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package grammartools.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Anthony
 */
public class FileProcessor
{
    private static void collectInputFiles(File[] files, List<File> fileList, FileFilter filter)
    {
        for (File file : files)
        {
            if (file.isDirectory())
            {
                collectInputFiles(file.listFiles(filter), fileList, filter);
            }
            else
            {
                fileList.add(file);
            }
        }
    }

    public static String buildFileInput(File inputFile, List<Exception> errors)
    {
        final LinkedList<File> files = new LinkedList<File>();
        final StringBuilder sb = new StringBuilder();

        if(inputFile.isFile())
        {
            files.add(inputFile);
        }
        // if input is a directory, gather all text files from it
        else if(inputFile.isDirectory())
        {
            collectInputFiles( new File[]{ inputFile }, files,
                    new FileFilter() 
                    {
                        @Override 
                        public boolean accept(File pathname) 
                        {
                            return pathname.isDirectory() || pathname.getName().endsWith( ".txt" );
                        }
                    });
        }

        for( File file : files )
        {
            try
            {
                // attempt to guess file encoding (Byte Order Mark)
                final FileInputStream fis = new FileInputStream(file);
                final String encoding = (fis.read() == 0xFE && fis.read() == 0xFF) ? "UTF-16" : "UTF-8";
                fis.close();

                final BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), encoding ) );
                String line;
                while((line = in.readLine()) != null)
                {
                    sb.append(line.replaceAll("[^\\p{Print}]+", ""));
                }
                in.close();
            }
            catch(IOException e)
            {
                if(errors != null)
                    errors.add(e);
            }
        }

        return sb.toString();
    }
}
