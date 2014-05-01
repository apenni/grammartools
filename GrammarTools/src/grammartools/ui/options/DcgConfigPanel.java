package grammartools.ui.options;

import grammartools.GrammarTools.DcgOptions;
import grammartools.ui.SwingUtils;
import java.io.File;
import javax.swing.JFileChooser;

@SuppressWarnings("serial")
public class DcgConfigPanel extends javax.swing.JPanel implements ConfigPanel
{
    public DcgConfigPanel()
    {
        initComponents();
    }
    
    @Override
    public DcgOptions getConfig()
    {
        DcgOptions o = new DcgOptions();
        o.grammarFile = inputGrammarFileTextField.getText();
        o.showRules = true;
        o.grammarFile = inputGrammarFileTextField.getText();
        return o;
    }
    
    @Override
    public String getConfigErrors()
    {
        String errors = "";

        DcgOptions o = getConfig();
        if(!new File(o.grammarFile).isFile()) errors += "prolog grammar file does not exist\n";
        
        return errors.isEmpty() ? null : errors;
    }
   
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        browseForInput = new javax.swing.JButton();
        inputGrammarFileTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();

        browseForInput.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/16/document-open.png"))); // NOI18N
        browseForInput.setToolTipText("Browse");
        browseForInput.setMargin(new java.awt.Insets(0, 0, 0, 0));
        browseForInput.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                browseForInputActionPerformed(evt);
            }
        });

        inputGrammarFileTextField.setFont(inputGrammarFileTextField.getFont());
        inputGrammarFileTextField.setText(DEF_INPUT_GRAMMAR_FILE);
        inputGrammarFileTextField.setToolTipText("The location of the prolog file containing DCG rules");

        jLabel4.setDisplayedMnemonic('o');
        jLabel4.setLabelFor(inputGrammarFileTextField);
        jLabel4.setText("Prolog grammar file:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(browseForInput)
                        .addGap(0, 0, 0)
                        .addComponent(inputGrammarFileTextField))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(browseForInput, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(inputGrammarFileTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseForInputActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseForInputActionPerformed
    {//GEN-HEADEREND:event_browseForInputActionPerformed
        javax.swing.filechooser.FileNameExtensionFilter filter = 
            new javax.swing.filechooser.FileNameExtensionFilter("Prolog Files [.pl]", "pl");
        
        String input = inputGrammarFileTextField.getText();
        String file = SwingUtils.browseForFile(
                (java.awt.Window)this.getTopLevelAncestor(),
                (input.isEmpty() ? DEF_INPUT_GRAMMAR_FILE : input), JFileChooser.FILES_ONLY,
            "Select an input file",
            filter);

        if(file != null)
        {
            if(new File(file).isFile())
            {
                inputGrammarFileTextField.setText(file);
            }
            else
            {
                SwingUtils.showWarning((java.awt.Window)this.getTopLevelAncestor(),
                                   "File does not exist." , "Warning");
            }
        }
    }//GEN-LAST:event_browseForInputActionPerformed
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseForInput;
    private javax.swing.JTextField inputGrammarFileTextField;
    private javax.swing.JLabel jLabel4;
    // End of variables declaration//GEN-END:variables

    private static final String DEF_INPUT_GRAMMAR_FILE = "grammar.pl";
}
