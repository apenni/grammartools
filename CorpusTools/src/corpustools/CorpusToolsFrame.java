package corpustools;

import grammartools.GrammarTools;
import grammartools.sentence.SentenceSplitter;
import grammartools.ui.SwingUtils;
import grammartools.ui.SwingWorkerQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Random;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;

@SuppressWarnings("serial")
public class CorpusToolsFrame extends javax.swing.JFrame 
{
    private static class Sentence
    {
        private enum Type { NONE, CORRECT, INCORRECT }
        public Sentence(String text)
        {
            this.text = text;
            type = Type.NONE;
        }
        public Sentence(String text, boolean isCorrect)
        {
            this(text);
            this.type = isCorrect ? Type.CORRECT : Type.INCORRECT;
        }
        public String getText() { return text; }
        public boolean isCorrect() { return type == Type.CORRECT; }
        public boolean isIncorrect() { return type == Type.INCORRECT; }
        public boolean isUnclassified() { return type == Type.NONE; }
        public void setCorrect(boolean correct) { type = correct ? Type.CORRECT : Type.INCORRECT; }
        public void setText(String text) { this.text = text; }

        private String text;
        private Type type;
    }

    public CorpusToolsFrame()
    {
        this(null);
    }
    
    public CorpusToolsFrame(GrammarTools tools) 
    {
        super();
                
        grammarTools = tools != null ? tools : new GrammarTools();
                
        initComponents();
        initErrorTypesDialog();
        
        currentlyOpenWindows.add(this);
        
        enableButtonsPanel(hasSentences());
        enableButtonsToolbar(true);
                
        if(useOpenNLPRadioButton.isSelected())
        {
            useOpenNLPRadioButton.doClick();
        }
        else if(useStanfordRadioButton.isSelected())
        {
            useStanfordRadioButton.doClick();
        }
        
        sentenceTextPane.getDocument().addDocumentListener(
                new DocumentListener() 
                {
                    private void updateSentence()
                    {
                        if(sentences != null && sentences.length > 0)
                            sentences[iSentence].setText(sentenceTextPane.getText());
                    }
                    public void insertUpdate(DocumentEvent e) { updateSentence(); }
                    public void removeUpdate(DocumentEvent e) { updateSentence(); }
                    public void changedUpdate(DocumentEvent e){ }
                });
        
        if(ttsToggleButton.isSelected())
        {
            initTTS();
        }
    }
    
    // set up the error types dialog
    private void initErrorTypesDialog()
    {
        GrammarTools.ErrorType[] errors = GrammarTools.ErrorType.values();
        
        errorWeightSpinners = new EnumMap<GrammarTools.ErrorType, JSpinner>(GrammarTools.ErrorType.class);
        errorTypeTextFields = new EnumMap<GrammarTools.ErrorType, JTextField>(GrammarTools.ErrorType.class);
        
        generateWeightsPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1,1,1,1);
        c.weightx = 0; c.weighty = 1;
        c.gridx = 0; c.gridy = 0;
        generateWeightsPanel.add(new JLabel("Error Type:"), c);
        ++c.gridx;
        generateWeightsPanel.add(new JLabel("Weight:"), c);
        ++c.gridx;
        generateWeightsPanel.add(new JLabel("Apply error to these types of words:"), c);
                
        for(GrammarTools.ErrorType e : errors)
        {
            JLabel label = new JLabel(e.name());
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(DEF_ERROR_WEIGHT, 0, Integer.MAX_VALUE, 1));
            ((DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);

            spinner.setToolTipText("Weight for " + e.name() +  " errors");
            
            String types = "";
            for(GrammarTools.POSType t : e.getPOSTypes())
            {
                types += ", " + t;
            }
            JTextField textfield = new JTextField(types.substring(1).trim());
            textfield.setToolTipText("Generate " + e.name() + " errors for these types of words");
            
            errorWeightSpinners.put(e, spinner);
            errorTypeTextFields.put(e, textfield);
            
            ++c.gridy; c.gridx = 0;
            c.weightx = 0; c.gridwidth = 1;
            generateWeightsPanel.add(label, c);
            ++c.gridx;
            generateWeightsPanel.add(spinner, c);
            ++c.gridx;
            c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
            generateWeightsPanel.add(textfield, c);
        }
        
        String types = "";
        for (GrammarTools.POSType t : GrammarTools.POSType.values())
        {
            types += ", " + t;
        }
        JLabel label = new JLabel("(Word types: " + types.substring(1).trim() + ")");
        ++c.gridy; c.gridx = 1;
        c.weightx = 0; c.gridwidth = 3;
        generateWeightsPanel.add(label, c);
        
        ((DefaultEditor)numErrorsSpinner.getEditor()).getTextField().setColumns(4);
    }
    
    private void initOpenNLPToolsAsync(final boolean setCurrentToolkit)
    {
        final String modelsPath = opennlpModelsTextField.getText();
        
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            private Exception ex;
            public void start() { showProgress("Initializing OpenNLP tools...", true); }
            public void perform()
            {
                try  
                { 
                    synchronized(grammarToolsLock)
                    {
                        grammarTools.initOpenNLPToolkit( modelsPath ); 
                    }
                }
                catch(Exception e) { ex = e; }
            }
            public void complete()
            {
                showProgress(ex == null ? "Ready" : "Error", false);

                if(ex == null && setCurrentToolkit)
                {
                    currentToolkit = grammarTools.getOpenNLPToolkit();
                }
                else if(ex != null)
                {
                    showWarning("Failed to load OpenNLP tools:\n" + ex.getLocalizedMessage());
                }      
            }
        });
    }
    
    private void initStanfordToolsAsync(final boolean setCurrentToolkit)
    {
        final String modelsPath = stanfordModelsTextField.getText();
        
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            Exception ex;
            public void start() { showProgress("Initializing Stanford tools...", true); }
            public void perform()
            {
                try 
                { 
                    synchronized(grammarToolsLock)
                    {
                        grammarTools.initStanfordToolkit( modelsPath ); 
                    }
                }
                catch(Exception e) { ex = e; }
            }
            public void complete()
            {
                showProgress(ex == null ? "Ready" : "Error", false);

                if(ex == null && setCurrentToolkit)
                {
                    currentToolkit = grammarTools.getStanfordToolkit();
                }
                else if(ex != null)
                {
                    showWarning("Failed to load Stanford tools:\n" + ex.getLocalizedMessage());
                }
            }
        });
    }
    
    private String browseForFile(String file, int selectionMode, String title ) 
    {
        return SwingUtils.browseForFile(this, file, selectionMode, title);
    }
        
    private void loadSentencesAsync(final String filename)
    {                
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            Sentence[] result;
            Exception ex;
            public void start() 
            { 
                enableButtonsToolbar(false);
                enableButtonsPanel(false);
                sentenceTextPane.setText(null);
                showProgress("Loading input...", true); 
            }
            public void perform()
            {
                try 
                { 
                    synchronized(sentencesLock)
                    {
                        result = load(filename, currentToolkit.sentenceSplitter); 
                    }
                }
                catch(Exception e) {ex = e; }
            }
            public void complete()
            {
                showProgress("Ready", false);
                
                if(result != null)
                {
                    sentences = result;
                    updateSentence(0);
                }
                else if(ex != null)
                {
                    showWarning("Error occured while reading input:\n" + ex.getLocalizedMessage());
                }
                
                enableSentencesTextPane(enableEditingCheckBox.isSelected());
                enableButtonsToolbar(true);
                enableButtonsPanel(hasSentences());
            }
        });
    }
    
    private void saveSentencesAsync(final String correctFilename, final String incorrectFilename, final String unclassifiedFilename)
    {
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            Exception ex;
            public void start() 
            { 
                enableButtonsPanel(false);
                enableButtonsToolbar(false);
                showProgress("Saving output...", true); 
            }
            public void perform()
            {
                try 
                { 
                    synchronized(sentencesLock)
                    {
                        save(sentences, correctFilename, incorrectFilename, unclassifiedFilename); 
                    }
                }
                catch(Exception e) { ex = e; }
            }
            public void complete()
            {
                updateSentence(iSentence);
                enableButtonsPanel(true);
                enableButtonsToolbar(true);
                if(ex == null)
                {
                    unsavedSentences = 0;
                    if(exitOnSave)
                    {
                        close();
                    }
                }
                if(ex != null)
                {        
                    showWarning("Error occurred while writing to file:\n" + ex.getLocalizedMessage());
                }
            }
        });
    }
       
    private boolean hasSentences()
    {
        return sentences != null && sentences.length > 0;
    }
    
    private Sentence getCurrentSentence()
    {
        return sentences[iSentence];
    }
    
    private void updateSentence(int i)
    {
        if(!hasSentences())
        {
            showProgress("No sentences loaded.", false);
            return;
        }
        
        iSentence = i < 0 ? 0 : i > sentences.length-1 ? sentences.length-1 : i;       
        
        sentenceTextPane.setText(getCurrentSentence().getText());
        
        stopVoice();
        if(ttsToggleButton.isSelected())
        {
            playTTS(getCurrentSentence().getText());
        }

        if(getCurrentSentence().isUnclassified())
        {
            classifyButtonGroup.clearSelection();
        }
        else
        {
            (getCurrentSentence().isCorrect() ? correctToggleButton : incorrectToggleButton).setSelected(true);
        }
        
        int n = iSentence+1;
        statusProgressBar.setMinimum(1);
        statusProgressBar.setMaximum(sentences.length);
        statusProgressBar.setValue(n);
        showProgress(n + " / " + sentences.length, false);
        
        sentenceNumberTextField.setText("" + n);
    }
    
    private void classifySentence(int i, boolean correct)
    {
        ++unsavedSentences;
        sentences[i].setCorrect(correct);
    }
    
    private void showGenerateDialog(boolean show)
    {
        if(show)
        {
            generateDialog.pack();
            java.awt.Point p = this.getLocation();
            java.awt.Dimension d = this.getSize();
            java.awt.Dimension d2 = generateDialog.getSize();
            generateDialog.setLocation(p.x + (d.width-d2.width)/2, p.y + (d.height-d2.height)/2);
        }
        generateDialog.setVisible(show);
    }
    
    private void generateErrorSentencesAsync(final GrammarTools.GenerateErrorOptions o)
    {
        if(!hasSentences())
            return;
                
        final String[] origSentences = new String[sentences.length];
        for(int i = 0; i < origSentences.length; i++)
        {
            origSentences[i] = sentences[i].getText();
        }
        
        class UIJobWorker implements SwingWorkerQueue.Job, GrammarTools.UIWorker
        {
            GrammarTools.ErrorSentence[] result;
            Exception ex;
            int progress = 0;
            Runnable updater = new Runnable() 
                {
                    public void run()
                    {
                        showProgress("Generating incorrect sentences... " + progress + "%", false);
                        statusProgressBar.setValue(progress);
                    }
                };
            
            public boolean isCancelled()
            {
                return false;
            }
            public void update(final double progress, String msg)
            {
                this.progress = (int)(100 * progress);
                javax.swing.SwingUtilities.invokeLater(updater);
            }
            
            public void start() 
            { 
                enableButtonsPanel(false);
                enableButtonsToolbar(false);
                showProgress("Generating incorrect sentences...", true); 
                grammarTools.setUIWorker(this);
            }
            public void perform()
            {
                synchronized(grammarToolsLock)
                {
                    result = grammarTools.generateErrorSentences(origSentences, o);
                }
            }
            public void complete()
            {
                if(result != null)
                {
                    final Sentence[] s = new Sentence[result.length];
                    for(int i = 0; i < s.length; i++)
                    {
                        s[i] = new Sentence(result[i].errorForm, false);
                    }
                    sentences = s;
                    updateSentence(iSentence);
                }
                if(ex != null)
                {        
                    showWarning("Error occurred while generating sentences:\n" + ex.getLocalizedMessage());
                }
                
                enableButtonsPanel(true);
                enableButtonsToolbar(true);
            }
        }

        jobQueue.submit(new UIJobWorker());
    }
    
    private void goNextSentence()
    {
        if (iSentence + 1 < sentences.length)
        {
            updateSentence(iSentence + 1);
        }
        else
        {
            showMessage("Reached end of sentences.");
        }
    }
    
    private void goPrevSentence()
    {
        if(iSentence - 1 >= 0)
        {
            updateSentence(iSentence - 1);
        }
        else
        {
            showMessage("Reached beginning of sentences.");
        }    
    }
    
    private void enableSentencesTextPane(boolean enable)
    {
        sentenceTextPane.setEditable(enable && sentences != null && sentences.length > 0);
    }
    
    private void enableButtonsPanel(boolean enable)
    {
        for(java.awt.Component c : buttonsPanel.getComponents())
        {
            c.setEnabled(enable);
            c.setVisible(enable);
        }
    }
    
    private void enableButtonsToolbar(boolean enable)
    {
        for(java.awt.Component c : buttonsToolbar.getComponents())
        {
            c.setEnabled(enable);
        }
        saveButton.setEnabled(enable && hasSentences());
        infoButton.setEnabled(enable && hasSentences());
        generateDialogButton.setEnabled(enable && hasSentences());
    }
    
    private void showProgress(String msg, boolean indeterminate)
    {
        if(indeterminate)
        {
            statusProgressBar.setValue(0);
        }
        statusProgressBar.setIndeterminate(indeterminate);
        statusProgressBar.setStringPainted(true);
        statusProgressBar.setString(msg);
    }
    
    private void showMessage(String msg)
    {
        SwingUtils.showMessage(this, msg, "Message");
    }

    private void showWarning(String msg)
    {
        SwingUtils.showWarning(this, msg, "Warning");
    }
       
    private void close()
    {
        deinitTTS();
        
        this.dispose();
        currentlyOpenWindows.remove(this);
        
        if(currentlyOpenWindows.isEmpty())
        {
            System.exit(0);
        }
    }
    
    private void stopVoice()
    {
        if(freetts != null)
        {
            freetts.getOutputQueue().removeAll();
            freetts.getAudioPlayer().cancel();
        }

        synchronized(maryttsLock)
        {
            if(maryClip != null && maryClip.isOpen())
            {
                maryClip.stop();
                maryClip.close();
            }
        }
    }
    
    private void initTTS()
    {
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {   
            public void start() {}
            public void complete() {}
            public void perform()
            {
                synchronized(freettsLock)
                {
                    freetts = com.sun.speech.freetts.VoiceManager.getInstance().getVoice(DEF_FREETTS_VOICE);
                    freetts.allocate();
                }
                try 
                {
                    synchronized(maryttsLock)
                    {
                        marytts = new marytts.LocalMaryInterface();
                        maryClip = AudioSystem.getClip();
                    }
                }
                catch (LineUnavailableException ex) {}
                catch (MaryConfigurationException ex) {}
            }
        });   
    }

    private void deinitTTS()
    {
        if(freetts != null)
        {
            freetts.deallocate();
        }
        
        freetts = null;
        marytts = null;
    }

    private void playTTS(final String text)
    {
        stopVoice();
        if(useFreeTTSRadioButton.isSelected())
        {
            freetts.setRate((Integer)ttsRateSpinner.getValue());
            new Thread()
            {
                @Override public void run() { freetts.speak(text); }
            }.start();
        }
        else
        {
            new Thread()
            {
                @Override public void run()
                {
                    try
                    {
                        synchronized(maryttsLock)
                        {
                            maryClip.open(marytts.generateAudio(text));
                            maryClip.start();
                        }
                    }
                    catch(SynthesisException ex) {}
                    catch(IOException ex) {}
                    catch(LineUnavailableException ex) {}
                    catch(NullPointerException ex) {}
                }
            }.start();
        }
    }
    
    void updateTTSControls()
    {
        boolean enabled = ttsToggleButton.isSelected();
        useMaryTTSRadioButton.setEnabled(enabled);
        useFreeTTSRadioButton.setEnabled(enabled);

        boolean selected = useFreeTTSRadioButton.isSelected();
        ttsRateLabel.setEnabled(enabled && selected);
        ttsRateSpinner.setEnabled(enabled && selected);
        
        if(enabled)
        {
            initTTS();
            if(hasSentences())
            {
                playTTS(getCurrentSentence().getText());
            }
        }
        else
        {        
            stopVoice();
            deinitTTS();
        }
    }
    
    static Sentence[] load(String filename, SentenceSplitter sentenceSplitter) 
            throws java.io.IOException
    {
        final File inFile = new File(filename);
        final LinkedList<File> files = new LinkedList<File>();
        final StringBuilder sb = new StringBuilder();

        if( inFile.isFile() )
        {
            files.add( inFile );
        }
        // if input is a directory, gather all text files from it
        else
        {
            buildFileList( new File[]{ inFile }, files,
                    new FileFilter() {
                        public boolean accept(File pathname) 
                        {
                            return pathname.isDirectory() || pathname.getName().endsWith( ".txt" );
                        }
                    });
        }

        for( File file : files )
        {
            // attempt to guess file encoding (Byte Order Mark)
            final FileInputStream fis = new FileInputStream( file );
            final String encoding = ( fis.read() == 0xFE && fis.read() == 0xFF ) ? "UTF-16" : "UTF-8";
            fis.close();

            final BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), encoding ) );
            final char[] buf = new char[4096];
            final String lineSeperator = System.getProperty( "line.separator" );
            int numRead;
            while( (numRead = in.read( buf )) != -1 )
            {
                // treat line endings
                sb.append( String.valueOf(buf, 0, numRead).replaceAll("[\r\n]+", lineSeperator) );
            }
            in.close();
        }

        String[] t = sentenceSplitter.split(sb.toString());
        Sentence[] s = new Sentence[t.length];
        for(int i = 0; i < t.length; i++)
        {
            String clean = t[i].trim().replaceAll("[\\s]+", " ");
            s[i] = new Sentence(clean);
        }
        
        return s;
    }
    
    static void buildFileList(File[] files, LinkedList<File> fileList, FileFilter filter)
    {
        for( File file : files )
        {
            if( file.isDirectory() )
            {
                buildFileList( file.listFiles( filter ), fileList, filter );
            }
            else
            {
                fileList.add( file );
            }
        }
    }
    
    static void save(Sentence[] sentences, String correctFilename, String incorrectFilename, String unclassifiedFilename)
            throws java.io.FileNotFoundException, java.io.IOException
    {
        BufferedWriter correctWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(correctFilename), "UTF-8"));
        BufferedWriter incorrectWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(incorrectFilename), "UTF-8"));
        BufferedWriter unclassifiedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(unclassifiedFilename), "UTF-8"));
        
        try
        {
            for(Sentence s : sentences)
            {
                BufferedWriter bw = s.isCorrect() ? correctWriter : s.isIncorrect() ? incorrectWriter : unclassifiedWriter;
                bw.write(s.getText());
                bw.newLine();
            }
        }
        finally
        {
            correctWriter.close();
            incorrectWriter.close();
            unclassifiedWriter.close();
        }
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        toolsButtonGroup = new javax.swing.ButtonGroup();
        classifyButtonGroup = new javax.swing.ButtonGroup();
        ttsButtonGroup = new javax.swing.ButtonGroup();
        optionsDialog = new javax.swing.JDialog();
        useOpenNLPRadioButton = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        opennlpModelsTextField = new javax.swing.JTextField();
        browseForOpenNLPModelsButton = new javax.swing.JButton();
        useStanfordRadioButton = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        stanfordModelsTextField = new javax.swing.JTextField();
        browseForStanfordNLPModels = new javax.swing.JButton();
        ttsToggleButton = new javax.swing.JToggleButton();
        useMaryTTSRadioButton = new javax.swing.JRadioButton();
        useFreeTTSRadioButton = new javax.swing.JRadioButton();
        ttsRateLabel = new javax.swing.JLabel();
        ttsRateSpinner = new javax.swing.JSpinner();
        closeButton = new javax.swing.JButton();
        enableEditingCheckBox = new javax.swing.JCheckBox();
        generateDialog = new javax.swing.JDialog();
        generateWeightsPanel = new javax.swing.JPanel();
        generateButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        numErrorsSpinner = new javax.swing.JSpinner();
        exactErrorsCheckBox = new javax.swing.JCheckBox();
        clearWeightsButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        randomSeedNumberTextField = new javax.swing.JFormattedTextField();
        buttonsToolbar = new javax.swing.JToolBar();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        generateDialogButton = new javax.swing.JButton();
        infoButton = new javax.swing.JButton();
        optionsButton = new javax.swing.JButton();
        textScrollPane = new javax.swing.JScrollPane();
        sentenceTextPane = new javax.swing.JTextPane();
        statusProgressBar = new javax.swing.JProgressBar();
        buttonsPanel = new javax.swing.JPanel();
        correctToggleButton = new javax.swing.JToggleButton();
        incorrectToggleButton = new javax.swing.JToggleButton();
        previousButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        gotoSentenceButton = new javax.swing.JButton();
        sentenceNumberTextField = new javax.swing.JTextField();
        randomSentenceButton = new javax.swing.JButton();

        optionsDialog.setTitle("Options");
        optionsDialog.setLocationByPlatform(true);
        optionsDialog.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                optionsDialogWindowOpened(evt);
            }
        });

        toolsButtonGroup.add(useOpenNLPRadioButton);
        useOpenNLPRadioButton.setFont(useOpenNLPRadioButton.getFont());
        useOpenNLPRadioButton.setSelected(true);
        useOpenNLPRadioButton.setText("Use OpenNLP tools");
        useOpenNLPRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useOpenNLPRadioButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("OpenNLP models path:");

        opennlpModelsTextField.setEditable(false);
        opennlpModelsTextField.setFont(opennlpModelsTextField.getFont());
        opennlpModelsTextField.setText(DEF_OPENNLP_MODEL_PATH);

        browseForOpenNLPModelsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/16/document-open.png"))); // NOI18N
        browseForOpenNLPModelsButton.setToolTipText("Browse");
        browseForOpenNLPModelsButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        browseForOpenNLPModelsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseForOpenNLPModelsButtonActionPerformed(evt);
            }
        });

        toolsButtonGroup.add(useStanfordRadioButton);
        useStanfordRadioButton.setFont(useStanfordRadioButton.getFont());
        useStanfordRadioButton.setText("Use Stanford tools");
        useStanfordRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useStanfordRadioButtonActionPerformed(evt);
            }
        });

        jLabel5.setText("Stanford models path:");

        stanfordModelsTextField.setEditable(false);
        stanfordModelsTextField.setFont(stanfordModelsTextField.getFont());
        stanfordModelsTextField.setText(DEF_STANFORD_MODEL_PATH);

        browseForStanfordNLPModels.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/16/document-open.png"))); // NOI18N
        browseForStanfordNLPModels.setToolTipText("Browse");
        browseForStanfordNLPModels.setMargin(new java.awt.Insets(0, 0, 0, 0));
        browseForStanfordNLPModels.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseForStanfordNLPModelsActionPerformed(evt);
            }
        });

        ttsToggleButton.setFont(ttsToggleButton.getFont());
        ttsToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/audio-card.png"))); // NOI18N
        ttsToggleButton.setMnemonic('t');
        ttsToggleButton.setText("Text-to-Speech");
        ttsToggleButton.setToolTipText("Enable or disable text-to-speech");
        ttsToggleButton.setMargin(new java.awt.Insets(6, 12, 6, 12));
        ttsToggleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ttsToggleButtonActionPerformed(evt);
            }
        });

        ttsButtonGroup.add(useMaryTTSRadioButton);
        useMaryTTSRadioButton.setFont(useMaryTTSRadioButton.getFont());
        useMaryTTSRadioButton.setSelected(true);
        useMaryTTSRadioButton.setText("Use MARY TTS");
        useMaryTTSRadioButton.setEnabled(ttsToggleButton.isSelected());
        useMaryTTSRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useMaryTTSRadioButtonActionPerformed(evt);
            }
        });

        ttsButtonGroup.add(useFreeTTSRadioButton);
        useFreeTTSRadioButton.setFont(useFreeTTSRadioButton.getFont());
        useFreeTTSRadioButton.setText("Use FreeTTS");
        useFreeTTSRadioButton.setEnabled(ttsToggleButton.isSelected());
        useFreeTTSRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useFreeTTSRadioButtonActionPerformed(evt);
            }
        });

        ttsRateLabel.setFont(ttsRateLabel.getFont());
        ttsRateLabel.setText("Rate:");
        ttsRateLabel.setEnabled(ttsToggleButton.isSelected());

        ttsRateSpinner.setFont(ttsRateSpinner.getFont());
        ttsRateSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(50), null, Integer.valueOf(500), Integer.valueOf(1)));
        ttsRateSpinner.setToolTipText("Text-to-Speech rate (words-per-minute)");
        ttsRateSpinner.setEnabled(ttsToggleButton.isSelected());
        ttsRateSpinner.setValue(DEF_TTS_RATE);

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                closeButtonActionPerformed(evt);
            }
        });

        enableEditingCheckBox.setText("Allow editing of input sentences");
        enableEditingCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                enableEditingCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout optionsDialogLayout = new javax.swing.GroupLayout(optionsDialog.getContentPane());
        optionsDialog.getContentPane().setLayout(optionsDialogLayout);
        optionsDialogLayout.setHorizontalGroup(
            optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(optionsDialogLayout.createSequentialGroup()
                        .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(optionsDialogLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(optionsDialogLayout.createSequentialGroup()
                                        .addComponent(browseForStanfordNLPModels)
                                        .addGap(0, 0, 0)
                                        .addComponent(stanfordModelsTextField))
                                    .addGroup(optionsDialogLayout.createSequentialGroup()
                                        .addComponent(browseForOpenNLPModelsButton)
                                        .addGap(0, 0, 0)
                                        .addComponent(opennlpModelsTextField))
                                    .addGroup(optionsDialogLayout.createSequentialGroup()
                                        .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel5)
                                            .addComponent(jLabel4))
                                        .addGap(0, 0, Short.MAX_VALUE))))
                            .addGroup(optionsDialogLayout.createSequentialGroup()
                                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(useStanfordRadioButton)
                                    .addComponent(useOpenNLPRadioButton)
                                    .addGroup(optionsDialogLayout.createSequentialGroup()
                                        .addGap(24, 24, 24)
                                        .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(optionsDialogLayout.createSequentialGroup()
                                                .addComponent(ttsRateLabel)
                                                .addGap(0, 0, 0)
                                                .addComponent(ttsRateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(useMaryTTSRadioButton)
                                                .addComponent(useFreeTTSRadioButton)))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 344, Short.MAX_VALUE))
                            .addComponent(closeButton))
                        .addGap(10, 10, 10))
                    .addGroup(optionsDialogLayout.createSequentialGroup()
                        .addComponent(ttsToggleButton)
                        .addGap(70, 70, 70)
                        .addComponent(enableEditingCheckBox)
                        .addContainerGap())))
        );
        optionsDialogLayout.setVerticalGroup(
            optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(useOpenNLPRadioButton)
                .addGap(0, 0, 0)
                .addComponent(jLabel4)
                .addGap(0, 0, 0)
                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(browseForOpenNLPModelsButton)
                    .addComponent(opennlpModelsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(useStanfordRadioButton)
                .addGap(0, 0, 0)
                .addComponent(jLabel5)
                .addGap(0, 0, 0)
                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(browseForStanfordNLPModels)
                    .addComponent(stanfordModelsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ttsToggleButton)
                    .addComponent(enableEditingCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useMaryTTSRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useFreeTTSRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(optionsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(ttsRateLabel)
                    .addComponent(ttsRateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(closeButton)
                .addContainerGap())
        );

        generateDialog.setTitle("Generate Errors");
        generateDialog.setLocationByPlatform(true);

        javax.swing.GroupLayout generateWeightsPanelLayout = new javax.swing.GroupLayout(generateWeightsPanel);
        generateWeightsPanel.setLayout(generateWeightsPanelLayout);
        generateWeightsPanelLayout.setHorizontalGroup(
            generateWeightsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 135, Short.MAX_VALUE)
        );
        generateWeightsPanelLayout.setVerticalGroup(
            generateWeightsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 89, Short.MAX_VALUE)
        );

        generateButton.setText("Generate");
        generateButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                generateButtonActionPerformed(evt);
            }
        });

        jLabel1.setLabelFor(numErrorsSpinner);
        jLabel1.setText("Errors per sentence:");

        numErrorsSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(0), null, Integer.valueOf(1)));
        numErrorsSpinner.setToolTipText("Number of errors to generate in each sentence");

        exactErrorsCheckBox.setText("Generate exact number of errors only");
        exactErrorsCheckBox.setToolTipText("Exclude sentences which cannot be generated with exactly this number of errors ");

        clearWeightsButton.setText("Clear Weights");
        clearWeightsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearWeightsButtonActionPerformed(evt);
            }
        });

        jLabel2.setLabelFor(randomSeedNumberTextField);
        jLabel2.setText("Seed:");

        randomSeedNumberTextField.setColumns(3);
        randomSeedNumberTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        randomSeedNumberTextField.setText(""+DEF_RNG_SEED);
        randomSeedNumberTextField.setToolTipText("used to seed the random number generator");

        javax.swing.GroupLayout generateDialogLayout = new javax.swing.GroupLayout(generateDialog.getContentPane());
        generateDialog.getContentPane().setLayout(generateDialogLayout);
        generateDialogLayout.setHorizontalGroup(
            generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generateDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generateDialogLayout.createSequentialGroup()
                        .addComponent(generateButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearWeightsButton))
                    .addGroup(generateDialogLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(numErrorsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(exactErrorsCheckBox)
                    .addGroup(generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, generateDialogLayout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(randomSeedNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(generateWeightsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        generateDialogLayout.setVerticalGroup(
            generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generateDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(numErrorsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exactErrorsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generateWeightsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(randomSeedNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(generateDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generateButton)
                    .addComponent(clearWeightsButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CorpusTools");
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(500, 500));
        setName("CorpusTools"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt)
            {
                formWindowOpened(evt);
            }
        });

        buttonsToolbar.setFloatable(false);
        buttonsToolbar.setRollover(true);
        buttonsToolbar.setFont(buttonsToolbar.getFont());

        openButton.setFont(openButton.getFont());
        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/document-open.png"))); // NOI18N
        openButton.setMnemonic('o');
        openButton.setText("Open");
        openButton.setToolTipText("Open input file or folder");
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        openButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                openButtonActionPerformed(evt);
            }
        });
        buttonsToolbar.add(openButton);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/document-save.png"))); // NOI18N
        saveButton.setMnemonic('s');
        saveButton.setText("Save");
        saveButton.setToolTipText("Save all marked sentences to files (alt+s)");
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                saveButtonActionPerformed(evt);
            }
        });
        buttonsToolbar.add(saveButton);

        generateDialogButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/accessories-text-editor.png"))); // NOI18N
        generateDialogButton.setMnemonic('g');
        generateDialogButton.setText("Generate");
        generateDialogButton.setToolTipText("Generate errors in sentences");
        generateDialogButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        generateDialogButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        generateDialogButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        generateDialogButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                generateDialogButtonActionPerformed(evt);
            }
        });
        buttonsToolbar.add(generateDialogButton);

        infoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/dialog-information.png"))); // NOI18N
        infoButton.setText("Info");
        infoButton.setToolTipText("Info");
        infoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        infoButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        infoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        infoButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                infoButtonActionPerformed(evt);
            }
        });
        buttonsToolbar.add(infoButton);

        optionsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/22/applications-system.png"))); // NOI18N
        optionsButton.setMnemonic('t');
        optionsButton.setText("Options");
        optionsButton.setToolTipText("Options");
        optionsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        optionsButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        optionsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        optionsButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                optionsButtonActionPerformed(evt);
            }
        });
        buttonsToolbar.add(optionsButton);

        sentenceTextPane.setEditable(enableEditingCheckBox.isSelected());
        textScrollPane.setViewportView(sentenceTextPane);

        classifyButtonGroup.add(correctToggleButton);
        correctToggleButton.setFont(correctToggleButton.getFont());
        correctToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/correct.png"))); // NOI18N
        correctToggleButton.setMnemonic('c');
        correctToggleButton.setText("Correct");
        correctToggleButton.setToolTipText("Mark the current sentence as grammatically correct");
        correctToggleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                correctToggleButtonActionPerformed(evt);
            }
        });

        classifyButtonGroup.add(incorrectToggleButton);
        incorrectToggleButton.setFont(incorrectToggleButton.getFont());
        incorrectToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/incorrect.png"))); // NOI18N
        incorrectToggleButton.setMnemonic('i');
        incorrectToggleButton.setText("Incorrect");
        incorrectToggleButton.setToolTipText("Mark the current sentence as grammatically incorrect");
        incorrectToggleButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        incorrectToggleButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                incorrectToggleButtonActionPerformed(evt);
            }
        });

        previousButton.setMnemonic('p');
        previousButton.setText("<");
        previousButton.setToolTipText("Previous (alt+p)");
        previousButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        previousButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                previousButtonActionPerformed(evt);
            }
        });

        nextButton.setMnemonic('n');
        nextButton.setText(">");
        nextButton.setToolTipText("Next (alt+n)");
        nextButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        nextButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                nextButtonActionPerformed(evt);
            }
        });

        gotoSentenceButton.setText("Go to:");
        gotoSentenceButton.setToolTipText("Jump to sentence #");
        gotoSentenceButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        gotoSentenceButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                gotoSentenceButtonActionPerformed(evt);
            }
        });

        sentenceNumberTextField.setColumns(5);
        sentenceNumberTextField.setFont(sentenceNumberTextField.getFont());
        sentenceNumberTextField.setText("0");
        sentenceNumberTextField.setMaximumSize(new java.awt.Dimension(600, 24));
        sentenceNumberTextField.setMinimumSize(new java.awt.Dimension(25, 2));
        sentenceNumberTextField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sentenceNumberTextFieldActionPerformed(evt);
            }
        });

        randomSentenceButton.setText("Random");
        randomSentenceButton.setToolTipText("Jump to random sentence");
        randomSentenceButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        randomSentenceButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                randomSentenceButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout buttonsPanelLayout = new javax.swing.GroupLayout(buttonsPanel);
        buttonsPanel.setLayout(buttonsPanelLayout);
        buttonsPanelLayout.setHorizontalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(correctToggleButton)
                .addGap(0, 0, 0)
                .addComponent(incorrectToggleButton)
                .addGap(18, 18, 18)
                .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(nextButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gotoSentenceButton)
                .addGap(1, 1, 1)
                .addComponent(sentenceNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(randomSentenceButton)
                .addContainerGap())
        );

        buttonsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {correctToggleButton, incorrectToggleButton});

        buttonsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {nextButton, previousButton});

        buttonsPanelLayout.setVerticalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(correctToggleButton)
                    .addComponent(incorrectToggleButton)
                    .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nextButton)
                    .addComponent(gotoSentenceButton)
                    .addComponent(sentenceNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(randomSentenceButton))
                .addGap(0, 0, 0))
        );

        buttonsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {correctToggleButton, incorrectToggleButton});

        buttonsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {gotoSentenceButton, sentenceNumberTextField});

        buttonsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {nextButton, previousButton});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonsToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(buttonsToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(textScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addComponent(buttonsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void browseForOpenNLPModelsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseForOpenNLPModelsButtonActionPerformed
        
        String file = SwingUtils.browseForFile(optionsDialog, opennlpModelsTextField.getText(), JFileChooser.DIRECTORIES_ONLY, "Select directory of OpenNLP models");
        if(file != null && !new File(file).equals(new File(opennlpModelsTextField.getText())))
        {
            opennlpModelsTextField.setText(file);
            opennlpModelsTextField.updateUI();
            initOpenNLPToolsAsync(false);
        }
    }//GEN-LAST:event_browseForOpenNLPModelsButtonActionPerformed

    private void browseForStanfordNLPModelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseForStanfordNLPModelsActionPerformed
        
        String file = SwingUtils.browseForFile(optionsDialog, stanfordModelsTextField.getText(), JFileChooser.DIRECTORIES_ONLY, "Select directory of Stanford models");
        if(file != null && !new File(file).equals(new File(stanfordModelsTextField.getText())))
        {
            stanfordModelsTextField.setText(file);
            stanfordModelsTextField.updateUI();
            initStanfordToolsAsync(false);
        }
    }//GEN-LAST:event_browseForStanfordNLPModelsActionPerformed

    private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousButtonActionPerformed
        goPrevSentence();
    }//GEN-LAST:event_previousButtonActionPerformed

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        goNextSentence();
    }//GEN-LAST:event_nextButtonActionPerformed

    private void gotoSentenceButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_gotoSentenceButtonActionPerformed
    {//GEN-HEADEREND:event_gotoSentenceButtonActionPerformed
        updateSentence(Integer.parseInt(sentenceNumberTextField.getText())-1);
    }//GEN-LAST:event_gotoSentenceButtonActionPerformed

    private void sentenceNumberTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_sentenceNumberTextFieldActionPerformed
    {//GEN-HEADEREND:event_sentenceNumberTextFieldActionPerformed
        updateSentence(Integer.parseInt(sentenceNumberTextField.getText())-1);
    }//GEN-LAST:event_sentenceNumberTextFieldActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveButtonActionPerformed
    {//GEN-HEADEREND:event_saveButtonActionPerformed
  
        String name = new File(inputFile).getName();
        int iExt = name.lastIndexOf('.');
        if(iExt >= 0)
            name = name.substring(0, iExt);
        
        String file = SwingUtils.browseForFile(
                this, outputFile != null ? outputFile : name, JFileChooser.FILES_ONLY,
                "Select output file for sentences");
        
        if(file == null || file.isEmpty())
            return;

        outputFile = file;
        saveSentencesAsync(file + "." + DEF_CORRECT_SENTENCES_FILE,
                           file + "." + DEF_INCORRECT_SENTENCES_FILE,
                           file + "." + DEF_UNCLASSIFIED_SENTENCES_FILE);

    }//GEN-LAST:event_saveButtonActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_openButtonActionPerformed
    {//GEN-HEADEREND:event_openButtonActionPerformed
        if(unsavedSentences > 0)
        {
            switch(JOptionPane.showConfirmDialog(this,
                    "There are unsaved sentences, do you wish to save them before closing?",
                    "Save", JOptionPane.YES_NO_CANCEL_OPTION))
            {
                case JOptionPane.YES_OPTION: saveButton.doClick(); break;
                case JOptionPane.NO_OPTION:  break;
                case JOptionPane.CANCEL_OPTION:  return;
            }
        }
        
        String file = browseForFile(inputFile, JFileChooser.FILES_AND_DIRECTORIES, "Select input file or folder");
        if(file == null || file.isEmpty())
            return;
        
        inputFile = file;
        outputFile = null;
        loadSentencesAsync(inputFile);
    }//GEN-LAST:event_openButtonActionPerformed

    private void useStanfordRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useStanfordRadioButtonActionPerformed
    {//GEN-HEADEREND:event_useStanfordRadioButtonActionPerformed
        if(grammarTools.getStanfordToolkit() == null)
        {
            initStanfordToolsAsync(true);
        }
        else
        {
            currentToolkit = grammarTools.getStanfordToolkit();
        }
    }//GEN-LAST:event_useStanfordRadioButtonActionPerformed

    private void useOpenNLPRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useOpenNLPRadioButtonActionPerformed
    {//GEN-HEADEREND:event_useOpenNLPRadioButtonActionPerformed
        if(grammarTools.getOpenNLPToolkit() == null)
        {
            initOpenNLPToolsAsync(true);
        }
        else
        {
            currentToolkit = grammarTools.getOpenNLPToolkit();
        }
    }//GEN-LAST:event_useOpenNLPRadioButtonActionPerformed

    private void correctToggleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_correctToggleButtonActionPerformed
    {//GEN-HEADEREND:event_correctToggleButtonActionPerformed
        classifySentence(iSentence, true);
        goNextSentence();
    }//GEN-LAST:event_correctToggleButtonActionPerformed

    private void incorrectToggleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_incorrectToggleButtonActionPerformed
    {//GEN-HEADEREND:event_incorrectToggleButtonActionPerformed
        classifySentence(iSentence, false);
        goNextSentence();
    }//GEN-LAST:event_incorrectToggleButtonActionPerformed

    private void ttsToggleButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ttsToggleButtonActionPerformed
    {//GEN-HEADEREND:event_ttsToggleButtonActionPerformed
        updateTTSControls();
    }//GEN-LAST:event_ttsToggleButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        if(unsavedSentences == 0)
        {
            close();
        }
        else 
        {
            switch(JOptionPane.showConfirmDialog(this,
                    "There are unsaved sentences, do you wish to save them before closing?",
                    "Save", JOptionPane.YES_NO_CANCEL_OPTION))
            {
                case JOptionPane.YES_OPTION:
                    exitOnSave = true;
                    saveButton.doClick();
                    break;
                case JOptionPane.NO_OPTION: 
                    close();
                    break;
                case JOptionPane.CANCEL_OPTION: 
                    break;
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void useFreeTTSRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useFreeTTSRadioButtonActionPerformed
    {//GEN-HEADEREND:event_useFreeTTSRadioButtonActionPerformed
        updateTTSControls();
    }//GEN-LAST:event_useFreeTTSRadioButtonActionPerformed

    private void useMaryTTSRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_useMaryTTSRadioButtonActionPerformed
    {//GEN-HEADEREND:event_useMaryTTSRadioButtonActionPerformed
        updateTTSControls();
    }//GEN-LAST:event_useMaryTTSRadioButtonActionPerformed

    private void optionsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_optionsButtonActionPerformed
    {//GEN-HEADEREND:event_optionsButtonActionPerformed
        optionsDialog.setVisible(true);
    }//GEN-LAST:event_optionsButtonActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowOpened
    {//GEN-HEADEREND:event_formWindowOpened
        if(!hasSentences())
        {
            openButton.doClick();
        }
    }//GEN-LAST:event_formWindowOpened

    private void optionsDialogWindowOpened(java.awt.event.WindowEvent evt)//GEN-FIRST:event_optionsDialogWindowOpened
    {//GEN-HEADEREND:event_optionsDialogWindowOpened
        optionsDialog.pack();
    }//GEN-LAST:event_optionsDialogWindowOpened

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_closeButtonActionPerformed
    {//GEN-HEADEREND:event_closeButtonActionPerformed
        optionsDialog.setVisible(false);
    }//GEN-LAST:event_closeButtonActionPerformed

    private void generateDialogButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_generateDialogButtonActionPerformed
    {//GEN-HEADEREND:event_generateDialogButtonActionPerformed
        showGenerateDialog(true); 
    }//GEN-LAST:event_generateDialogButtonActionPerformed

    private void infoButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_infoButtonActionPerformed
    {//GEN-HEADEREND:event_infoButtonActionPerformed
        int nCorrect = 0, nIncorrect = 0, nUnclassified = 0;
        
        for(Sentence s : sentences)
        {
            if(s.isCorrect())
            {
                nCorrect++;
            }
            else if(s.isIncorrect())
            {
                nIncorrect++;
            }
            else
            {
                nUnclassified++;
            }
        }
        
        showMessage(
                "Input:" 
                + "\n  " + inputFile
                + "\nTotal sentences: " + sentences.length
                + "\n  correct: " + nCorrect
                + "\n  incorrect: " + nIncorrect
                + "\n  unclassified: " + nUnclassified
                );
    }//GEN-LAST:event_infoButtonActionPerformed

    private void generateButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_generateButtonActionPerformed
    {//GEN-HEADEREND:event_generateButtonActionPerformed
        final GrammarTools.GenerateErrorOptions o = new GrammarTools.GenerateErrorOptions();
        
        long seed;
        try { seed = Long.parseLong(randomSeedNumberTextField.getText()); }
        catch (NumberFormatException e) 
        {
            showWarning("Invalid Seed");
            return;
        }
       
        o.toolkit = currentToolkit;
        o.random = new java.util.Random(seed);
        o.errorsPerSentence = (Integer)numErrorsSpinner.getValue();
        o.exactNumberOfErrorsOnly = exactErrorsCheckBox.isSelected();
        
        o.errorTypes = errorWeightSpinners.keySet().toArray(new GrammarTools.ErrorType[0]);
        for(GrammarTools.ErrorType et : o.errorTypes)
        {
            JSpinner spinner = errorWeightSpinners.get(et);
            JTextField textfield = errorTypeTextFields.get(et);
            if(spinner != null) 
            {
                et.setWeight((Integer)spinner.getValue());
            }
            if(textfield != null)
            {
                String[] typesText = textfield.getText().split("\\W+");
                java.util.List<GrammarTools.POSType> types = new LinkedList<GrammarTools.POSType>();

                for(String t : typesText)
                {
                    GrammarTools.POSType p = GrammarTools.POSType.valueOf(t.toUpperCase());
                    if(p != null)
                        types.add(p);
                }
                et.setPOSTypes(types.toArray(new GrammarTools.POSType[0]));
            }
        }

        CorpusToolsFrame frame = new CorpusToolsFrame(grammarTools);
        frame.sentences = sentences;
        frame.inputFile = inputFile;
        frame.outputFile = outputFile;
        //frame.optionsButton.setVisible(false);
        frame.generateErrorSentencesAsync(o);
        frame.setVisible(true);
        showGenerateDialog(false);
    }//GEN-LAST:event_generateButtonActionPerformed

    private void enableEditingCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_enableEditingCheckBoxActionPerformed
    {//GEN-HEADEREND:event_enableEditingCheckBoxActionPerformed
        enableSentencesTextPane(enableEditingCheckBox.isSelected());
    }//GEN-LAST:event_enableEditingCheckBoxActionPerformed

    private void clearWeightsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearWeightsButtonActionPerformed
    {//GEN-HEADEREND:event_clearWeightsButtonActionPerformed
        for(JSpinner s : errorWeightSpinners.values())
        {
            s.setValue(0);
        }
    }//GEN-LAST:event_clearWeightsButtonActionPerformed

    private void randomSentenceButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_randomSentenceButtonActionPerformed
    {//GEN-HEADEREND:event_randomSentenceButtonActionPerformed
        updateSentence(random.nextInt(sentences.length));
    }//GEN-LAST:event_randomSentenceButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseForOpenNLPModelsButton;
    private javax.swing.JButton browseForStanfordNLPModels;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JToolBar buttonsToolbar;
    private javax.swing.ButtonGroup classifyButtonGroup;
    private javax.swing.JButton clearWeightsButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JToggleButton correctToggleButton;
    private javax.swing.JCheckBox enableEditingCheckBox;
    private javax.swing.JCheckBox exactErrorsCheckBox;
    private javax.swing.JButton generateButton;
    private javax.swing.JDialog generateDialog;
    private javax.swing.JButton generateDialogButton;
    private javax.swing.JPanel generateWeightsPanel;
    private javax.swing.JButton gotoSentenceButton;
    private javax.swing.JToggleButton incorrectToggleButton;
    private javax.swing.JButton infoButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton nextButton;
    private javax.swing.JSpinner numErrorsSpinner;
    private javax.swing.JButton openButton;
    private javax.swing.JTextField opennlpModelsTextField;
    private javax.swing.JButton optionsButton;
    private javax.swing.JDialog optionsDialog;
    private javax.swing.JButton previousButton;
    private javax.swing.JFormattedTextField randomSeedNumberTextField;
    private javax.swing.JButton randomSentenceButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JTextField sentenceNumberTextField;
    private javax.swing.JTextPane sentenceTextPane;
    private javax.swing.JTextField stanfordModelsTextField;
    private javax.swing.JProgressBar statusProgressBar;
    private javax.swing.JScrollPane textScrollPane;
    private javax.swing.ButtonGroup toolsButtonGroup;
    private javax.swing.ButtonGroup ttsButtonGroup;
    private javax.swing.JLabel ttsRateLabel;
    private javax.swing.JSpinner ttsRateSpinner;
    private javax.swing.JToggleButton ttsToggleButton;
    private javax.swing.JRadioButton useFreeTTSRadioButton;
    private javax.swing.JRadioButton useMaryTTSRadioButton;
    private javax.swing.JRadioButton useOpenNLPRadioButton;
    private javax.swing.JRadioButton useStanfordRadioButton;
    // End of variables declaration//GEN-END:variables
    
    private static final java.util.List<javax.swing.JFrame> currentlyOpenWindows = new java.util.LinkedList<javax.swing.JFrame>();
    
    private EnumMap<GrammarTools.ErrorType, JSpinner> errorWeightSpinners;
    private EnumMap<GrammarTools.ErrorType, JTextField> errorTypeTextFields;
    
    GrammarTools grammarTools;
    GrammarTools.Toolkit currentToolkit;
    private Sentence[] sentences;
    private int iSentence;
    
    private final SwingWorkerQueue jobQueue = new SwingWorkerQueue();
    private final Random random = new Random();
    
    private com.sun.speech.freetts.Voice freetts;
    private marytts.MaryInterface marytts;
    private Clip maryClip;

    private final Object sentencesLock = new Object();
    private final Object grammarToolsLock = new Object();
    private final Object freettsLock = new Object();
    private final Object maryttsLock = new Object();

    private int unsavedSentences;
    private boolean exitOnSave;
    private String inputFile;
    private String outputFile;
    
    private static final String SEP = System.getProperty( "file.separator" );
    private static final String DEF_GRAMMARTOOLS_PATH = ".." + SEP + "GrammarTools";
    private static final String DEF_OPENNLP_MODEL_PATH  = DEF_GRAMMARTOOLS_PATH + SEP + "models" + SEP + "opennlp";
    private static final String DEF_STANFORD_MODEL_PATH = DEF_GRAMMARTOOLS_PATH + SEP + "models" + SEP + "stanford";
    private static final String DEF_CORRECT_SENTENCES_FILE = "correct.txt";
    private static final String DEF_INCORRECT_SENTENCES_FILE = "incorrect.txt";
    private static final String DEF_UNCLASSIFIED_SENTENCES_FILE = "unclassified.txt";
    private static final String DEF_FREETTS_VOICE = "kevin16"; //kevin, alan
    private static final int DEF_TTS_RATE = 135; // words per minute
    private static final int DEF_ERROR_WEIGHT = 1;
    private static final int DEF_RNG_SEED = 1;
}
