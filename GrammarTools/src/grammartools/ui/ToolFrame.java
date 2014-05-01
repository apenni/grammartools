package grammartools.ui;

import grammartools.ui.options.XmlConfigPanel;
import grammartools.ui.options.StatsConfigPanel;
import grammartools.ui.options.ParseConfigPanel;
import grammartools.ui.options.DatasetConfigPanel;
import grammartools.ui.options.ClassifyConfigPanel;
import grammartools.ui.options.DcgConfigPanel;
import grammartools.ui.options.TagConfigPanel;
import grammartools.ui.options.FunctionConfigPanel;
import grammartools.ui.options.ConfigPanel;
import grammartools.ui.options.ChunkConfigPanel;
import grammartools.GrammarTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class ToolFrame extends javax.swing.JFrame
{
    public ToolFrame(ToolType tool, final String opennlpModelsPath, final String stanfordModelsPath)
    {
        this.tool = tool;        
        
        initComponents();
        this.setTitle(this.getTitle() + " - " + tool.toString());
        updateRunButton();

        switch(this.tool) 
        {
            case TAG        : { TagConfigPanel p = new TagConfigPanel(); config = p; optionsPanel.add(p); break; }
            case CHUNK      : { ChunkConfigPanel p = new ChunkConfigPanel(); config = p; optionsPanel.add(p); break; }
            case PARSE      : { ParseConfigPanel p = new ParseConfigPanel(); config = p; optionsPanel.add(p); break; }
            case XML        : { XmlConfigPanel p = new XmlConfigPanel(); config = p; optionsPanel.add(p); break; }
            case STATS      : { StatsConfigPanel p = new StatsConfigPanel(); config = p; optionsPanel.add(p); break; }
            case FUNCTION   : { FunctionConfigPanel p = new FunctionConfigPanel(); config = p; optionsPanel.add(p); break; }
            case DCG        : { DcgConfigPanel p = new DcgConfigPanel(); config = p; optionsPanel.add(p); break; }
            case DATASET    : { DatasetConfigPanel p = new DatasetConfigPanel(); config = p; optionsPanel.add(p); break; }
            case CLASSIFY   : { ClassifyConfigPanel p = new ClassifyConfigPanel(); config = p; optionsPanel.add(p); break; }
        }
                
        this.pack();
        
        initOpenNLPToolsAsync(opennlpModelsPath);
        initStanfordToolsAsync(stanfordModelsPath);
    }
    
    private void initOpenNLPToolsAsync(final String modelsPath)
    {        
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            private Exception ex;
            public void start() { showIndeterminateProgress("Initializing OpenNLP tools..."); }
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
                updateRunButton();
                showProgress(0, ex == null ? "Ready" : "Error");

                if(ex != null)
                {
                    showWarning("Failed to load OpenNLP tools:\n" + ex.getLocalizedMessage());
                }      
            }
        });
    }
    
    private void initStanfordToolsAsync(final String modelsPath)
    {   
        jobQueue.submit(
        new SwingWorkerQueue.Job()
        {
            private Exception ex;
            public void start() { showIndeterminateProgress("Initializing Stanford tools..."); }
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
                updateRunButton();
                showProgress(0, ex == null ? "Ready" : "Error");

                if(ex != null)
                {
                    showWarning("Failed to load Stanford tools:\n" + ex.getLocalizedMessage());
                }
            }
        });
    }
    
    private void showProgress(double progress, String text)
    {
        final int percent = (int)(100*progress);
        progressBar.setValue(percent);
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setString(text);
    }
    
    private void showIndeterminateProgress(String text)
    {
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString(text);
    }
    
    private void showMessage(String msg)
    {
        SwingUtils.showMessage(this, msg, "Message");
    }

    private void showWarning(String msg)
    {
        SwingUtils.showWarning(this, msg, "Warning");
    }
    
    private void showMessageDialog(String title, String text)
    {
        JFrame frame = new JFrame(this.getTitle() + " - " + title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea));
        java.awt.Point p = this.getLocation();
        p.translate(50,50);
        frame.setLocation(p);
        frame.pack();
        frame.setVisible(true);
    }
    
    private void updateInputFields()
    {
        final boolean isFileInput = inputFileRadioButton.isSelected();
        inputTextArea.setEnabled(!isFileInput);
        browseForInput.setEnabled(isFileInput);
        inputFileTextField.setEnabled(isFileInput);
        ioSplitPane.setDividerLocation(isFileInput ? 0 : ioSplitPane.getLastDividerLocation());
        ioSplitPane.setTopComponent(isFileInput ? null : inputScrollPane);
        ioSplitPane.setOneTouchExpandable(!isFileInput);
    }
    
    private void updateRunButton()
    {
        if(isRunning)
        {
            runButton.setIcon(stopIcon);
            runButton.setText("Cancel");    
        }
        else
        {
            runButton.setIcon(startIcon);
            runButton.setText("Run");
        }
        
        runButton.setEnabled(grammarTools != null 
                             && grammarTools.getOpenNLPToolkit() != null
                             && grammarTools.getStanfordToolkit() != null);
    }
    
    private synchronized void setRunning( boolean isRunning ) 
    {
        this.isRunning = isRunning;
        updateRunButton();
                
        if(!isRunning && toolWorker != null && !toolWorker.isDone())
        {
            showProgress(progressBar.getPercentComplete(), "Cancelling...");
            toolWorker.cancel(true);
        }
    }
    
    private void runTool()
    {
        if(isRunning)
        {
            setRunning(false);
            return;
        }
               
        toolWorker = new ToolSwingWorker(tool, config.getConfig());
        
        if(inputTextRadioButton.isSelected())
        {
            toolWorker.setInput(inputTextArea.getText());
        }
        else
        {
            File inputFile = new File(inputFileTextField.getText());
            if(!inputFile.exists())
            {
                showWarning("Input file does not exist.");
                return;
            }
            
            toolWorker.setInput(new File(inputFileTextField.getText()));
        }
        
        String errors = config.getConfigErrors();
        if(errors != null)
        {
            showWarning("The config for this tool is invalid.\nPlease adjust its configuration:\n" + errors);
            return;
        }

        setRunning(true);
        toolWorker.start();
    }
    
    private final class Result 
    {
        Result(double progress, String output, String status) 
        {
            this.progress = progress;
            this.output = output;
            this.status = status;
        }
        double progress;
        String output, status;
    }

    private final class ToolSwingWorker extends javax.swing.SwingWorker<Result, Result> implements GrammarTools.UIWorker
    {
        private final ToolType tool;
        private final Object config;
        private String inputText;
        private File inputFile;
        private boolean isTextInput;
        private List<Exception> errors = new LinkedList<Exception>();
        
        public ToolSwingWorker(ToolType tool, Object config)
        {
            this.tool = tool;
            this.config = config;
        }
        
        public void setInput(String text)
        {
            isTextInput = true;
            inputText = text;
        }
        
        public void setInput(File file)
        {
            isTextInput = false;
            inputFile = file;
        }
        
        public void start() 
        {
            synchronized (grammarTools)
            {
                grammarTools.setUIWorker(this);
            }
            showIndeterminateProgress("Reading input...");
            execute();
        }
        
        @Override
        public void update(double progress, String message)
        {              
            if(!isRunning)// && !this.isDone())
            {
                this.cancel(true);
                publish(new Result(progress, message, "Cancelled"));
            } 
            else 
            {
                publish(new Result(progress, message, "Running " + tool + "... (" + ((int)(100*progress)) + "%)" ));
            }
        }
            
        @Override
        protected void process(List<Result> results)
        {
            String status = null;
            double progress = -1;
            StringBuilder sb = new StringBuilder(results.size()*256);

            for(Result r : results)
            {
                status = r.status;
                progress = r.progress;
                sb.append(r.output);
            }
           
            outputTextArea.append(sb.toString());
            
            if(progress >= 0)
                showProgress(progress, status);
            else
                showIndeterminateProgress(status);
        }
        
        @Override 
        public Result doInBackground()
        {
            final long startTime = System.currentTimeMillis();
            final String input = (isTextInput ? inputText : 
                    grammartools.util.FileProcessor.buildFileInput(inputFile, errors)).trim();

            if(input.isEmpty())
            {
                return new Result(0, null, "Warning: no input");
            }

            try 
            { 
                synchronized (grammarToolsLock)
                {                    
                    switch (tool)
                    {
                        case TAG:
                        {
                            grammarTools.runTagTool(input, (GrammarTools.TagOptions) config);
                            break;
                        }
                        case CHUNK:
                        {
                            grammarTools.runChunkTool(input, (GrammarTools.ChunkOptions) config);
                            break;
                        }
                        case PARSE:
                        {
                            grammarTools.runParseTool(input, (GrammarTools.ParseOptions) config);
                            break;
                        }

                        case FUNCTION:
                        {
                            grammarTools.runFunctionTool(input, (GrammarTools.FunctionOptions) config);
                            break;
                        }
                        case DATASET:
                        {
                            grammarTools.runDatasetTool(input, (GrammarTools.DatasetOptions) config);
                            break;
                        }
                        case XML:
                        {
                            grammarTools.runXmlTool(input, (GrammarTools.XmlOptions) config);
                            break;
                        }
                        case STATS:
                        {
                            grammarTools.runStatsTool(input, (GrammarTools.StatsOptions) config);
                            break;
                        }
                        case DCG:
                        {
                            grammarTools.runDcgTool(input, (GrammarTools.DcgOptions) config);
                            break;
                        }
                        case CLASSIFY:
                        {
                            grammarTools.runClassifyTool(input, (GrammarTools.ClassifyOptions) config);
                            break;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                errors.add(e);
            }
            
            System.gc();
            final long time = System.currentTimeMillis() - startTime;
            return new Result(1, "\n", String.format("Completed (%.2fs)", time/1000.0));
        }
        
        @Override
        public void done()
        {            
            try
            {
                Result r = this.get();
                showProgress(r.progress, r.status);
                showProgress(0, r.status);
            }
            catch(Exception e) { }
            
            setRunning(false);
            
            if(!errors.isEmpty())
            {
                String text = "";
                for(Exception e : errors)
                {
                    text += e.getLocalizedMessage() + "\n";
                    String depth = "";
                    for(StackTraceElement ste : e.getStackTrace())
                    {
                        text += depth + ste + "\n";
                        depth += " ";
                    }
                    text += "\n\n";
                }
                showMessageDialog("Error Log (" + errors.size() + ")", text);
            }
        }
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        inputButtonGroup = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        inputTextRadioButton = new javax.swing.JRadioButton();
        inputFileRadioButton = new javax.swing.JRadioButton();
        browseForInput = new javax.swing.JButton();
        inputFileTextField = new javax.swing.JTextField();
        ioSplitPane = new javax.swing.JSplitPane();
        inputScrollPane = new javax.swing.JScrollPane();
        inputTextArea = new javax.swing.JTextArea();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        wrapTextCheckBox = new javax.swing.JCheckBox();
        runButton = new javax.swing.JButton();
        optionsPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        clearButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("GrammarTools");
        setLocationByPlatform(true);
        setName("GrammarTools"); // NOI18N

        jLabel1.setText("Input");

        inputButtonGroup.add(inputTextRadioButton);
        inputTextRadioButton.setMnemonic('t');
        inputTextRadioButton.setSelected(true);
        inputTextRadioButton.setText("Text");
        inputTextRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                inputTextRadioButtonActionPerformed(evt);
            }
        });

        inputButtonGroup.add(inputFileRadioButton);
        inputFileRadioButton.setMnemonic('f');
        inputFileRadioButton.setText("File");
        inputFileRadioButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                inputFileRadioButtonActionPerformed(evt);
            }
        });

        browseForInput.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/16/document-open.png"))); // NOI18N
        browseForInput.setToolTipText("Browse");
        browseForInput.setEnabled(wrapTextCheckBox.isSelected());
        browseForInput.setMargin(new java.awt.Insets(0, 0, 0, 0));
        browseForInput.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseForInputActionPerformed(evt);
            }
        });

        inputFileTextField.setFont(inputFileTextField.getFont());
        inputFileTextField.setEnabled(wrapTextCheckBox.isSelected());

        ioSplitPane.setDividerSize(6);
        ioSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        ioSplitPane.setResizeWeight(0.5);
        ioSplitPane.setOneTouchExpandable(true);

        inputTextArea.setFont(inputTextArea.getFont().deriveFont(inputTextArea.getFont().getSize()+1f));
        inputTextArea.setLineWrap(wrapTextCheckBox.isSelected());
        inputTextArea.setWrapStyleWord(true);
        inputScrollPane.setViewportView(inputTextArea);

        ioSplitPane.setTopComponent(inputScrollPane);

        outputTextArea.setEditable(false);
        outputTextArea.setFont(outputTextArea.getFont().deriveFont(outputTextArea.getFont().getSize()+1f));
        outputTextArea.setLineWrap(wrapTextCheckBox.isSelected());
        outputTextArea.setWrapStyleWord(true);
        outputScrollPane.setViewportView(outputTextArea);

        ioSplitPane.setRightComponent(outputScrollPane);

        wrapTextCheckBox.setText("Wrap text");
        wrapTextCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                wrapTextCheckBoxActionPerformed(evt);
            }
        });

        runButton.setIcon(startIcon);
        runButton.setMnemonic('r');
        runButton.setText("Run");
        runButton.setToolTipText("Run this tool");
        runButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        runButton.setIconTextGap(0);
        runButton.setMargin(new java.awt.Insets(4, 4, 4, 4));
        runButton.setPreferredSize(new java.awt.Dimension(49, 63));
        runButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        runButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                runButtonActionPerformed(evt);
            }
        });

        optionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Config"));
        optionsPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        optionsPanel.setLayout(new javax.swing.BoxLayout(optionsPanel, javax.swing.BoxLayout.LINE_AXIS));

        progressBar.setAlignmentY(0.0F);
        progressBar.setFocusable(false);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/16/edit-clear.png"))); // NOI18N
        clearButton.setToolTipText("Clear output");
        clearButton.setPreferredSize(new java.awt.Dimension(40, 25));
        clearButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                clearButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ioSplitPane)
                    .addComponent(optionsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(inputFileRadioButton)
                                    .addComponent(inputTextRadioButton))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(wrapTextCheckBox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(browseForInput)
                                        .addGap(0, 0, 0)
                                        .addComponent(inputFileTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)))))))
                .addGap(4, 4, 4))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {inputFileRadioButton, inputTextRadioButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(inputTextRadioButton)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(inputFileRadioButton))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(wrapTextCheckBox)
                                .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(0, 0, 0)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(browseForInput, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(inputFileTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(optionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(ioSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void browseForInputActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseForInputActionPerformed
    {//GEN-HEADEREND:event_browseForInputActionPerformed
        String file = SwingUtils.browseForFile(
                this, inputFileTextField.getText(), JFileChooser.FILES_AND_DIRECTORIES,
                "Select input file or directory containing input files");
        
        if(file != null && !new File(file).equals(new File(inputFileTextField.getText())))
        {
            inputFileTextField.setText(file);
            inputFileTextField.updateUI();
        }
    }//GEN-LAST:event_browseForInputActionPerformed

    private void wrapTextCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_wrapTextCheckBoxActionPerformed
    {//GEN-HEADEREND:event_wrapTextCheckBoxActionPerformed
        inputTextArea.setLineWrap(wrapTextCheckBox.isSelected());
        outputTextArea.setLineWrap(wrapTextCheckBox.isSelected());
    }//GEN-LAST:event_wrapTextCheckBoxActionPerformed

    private void inputTextRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_inputTextRadioButtonActionPerformed
    {//GEN-HEADEREND:event_inputTextRadioButtonActionPerformed
        updateInputFields();
    }//GEN-LAST:event_inputTextRadioButtonActionPerformed

    private void inputFileRadioButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_inputFileRadioButtonActionPerformed
    {//GEN-HEADEREND:event_inputFileRadioButtonActionPerformed
        updateInputFields();
    }//GEN-LAST:event_inputFileRadioButtonActionPerformed

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_runButtonActionPerformed
    {//GEN-HEADEREND:event_runButtonActionPerformed
        runTool();
    }//GEN-LAST:event_runButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_clearButtonActionPerformed
    {//GEN-HEADEREND:event_clearButtonActionPerformed
        outputTextArea.setText(null);
    }//GEN-LAST:event_clearButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseForInput;
    private javax.swing.JButton clearButton;
    private javax.swing.ButtonGroup inputButtonGroup;
    private javax.swing.JRadioButton inputFileRadioButton;
    private javax.swing.JTextField inputFileTextField;
    private javax.swing.JScrollPane inputScrollPane;
    private javax.swing.JTextArea inputTextArea;
    private javax.swing.JRadioButton inputTextRadioButton;
    private javax.swing.JSplitPane ioSplitPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton runButton;
    private javax.swing.JCheckBox wrapTextCheckBox;
    // End of variables declaration//GEN-END:variables

    private final ToolType tool;
    private final GrammarTools grammarTools = new GrammarTools();
    private final SwingWorkerQueue jobQueue = new SwingWorkerQueue();    
    private final Object grammarToolsLock = new Object();

    private ToolSwingWorker toolWorker;
    private ConfigPanel config;
    private final javax.swing.ImageIcon startIcon = new javax.swing.ImageIcon(getClass().getResource("/img/media-playback-start.png"));
    private final javax.swing.ImageIcon stopIcon  = new javax.swing.ImageIcon(getClass().getResource("/img/media-playback-stop.png"));
    
    private boolean isRunning;
}
