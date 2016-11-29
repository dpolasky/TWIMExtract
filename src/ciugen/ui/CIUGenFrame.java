/**
 * TWIMExtract v0.1
 * 
 */

package ciugen.ui;

import ciugen.imextract.DataVectorInfoObject;
import ciugen.imextract.IMExtractRunner;
import ciugen.preferences.Preferences;
import ciugen.ui.utils.RawFileFilter;
import ciugen.ui.utils.TextFileFilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
//import com.dyson.chart.data.xyz.DoubleXYZDataSeries;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.util.logging.*;

/**
 * General tool for extracting 1 dimensional DT and MZ datasets from Waters .raw files using specified
 * ranges or selection rules. Includes basic user interface (this class) and wrapper to handle running 
 * IMSExtract.exe with appropriate settings. 
 * @author EIKNEE, 
 * @author dpolasky
 * @version Unified v3.6.3, last edited 10/4/16
 *
 */
public class CIUGenFrame extends javax.swing.JFrame 
{	
	private static final String TITLE = "TWIMExtract v0.1";
	
	private static final long serialVersionUID = -1971838044338723234L;
	private JFileChooser fc = null;
    private JFileChooser rangefc = null;
    private JFileChooser batchfc = null;
    private JFileChooser rulefc = null;
    
    private String rawFileDirectory;
    private String rangeFileDirectory;
    private String outputDirectory; 
	private String batchFileDirectory;
	private String ruleFileDirectory;
	
    //single instance of preferences
    private static Preferences preferences = Preferences.getInstance();
      
    
    // CheckBox booleans
    private boolean useTrapCV;
    private boolean useTransfCV;
    private boolean useConeCV;
    private boolean useWavevel;
    private boolean useWaveht;
    
    // Mode settings
    private boolean printRanges = false;
    private boolean ruleMode = false;
    private int chosen_extraction_mode;
    private boolean combine_outputs;

    // global file information
    private ArrayList<String> rawPaths;
    
    // String locations for function strings from getAllFunctionInfo
    private static final int TRAPCV_SPLITS = 3;
    private static final int WH_SPLITS = 5;
    private static final int WV_SPLITS = 6;
    private static final int TRANSFCV_SPLITS = 4;
    private static final int CONECV_SPLITS = 2;
    private static final int SELECTED_SPLITS  = 1;
    private static final int FN_SPLITS = 0;
    
//    // Mode identifier constants
//    private static final int DT_MODE = 0;
//    private static final int MZ_MODE = 1;
//    private static final int RT_MODE = 2;
//    private static final int DTMZ_MODE = 3;
    
    // String locations for table model (includes filename) - has to be in this order because that's the order it displays in the GUI
    private static final int FN_TABLE = 0;
    private static final int FILENAME_TABLE = 1;
    private static final int SELECT_TABLE = 2;
    private static final int CONECV_TABLE = 3;
    private static final int TRAPCV_TABLE = 4;
    private static final int TRANSFCV_TABLE = 5;
    private static final int WH_TABLE = 6;
    private static final int WV_TABLE = 7;
    
    // Menu components
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu batchMenu;
	private JMenu optionMenu;
	private JMenu selectionMenu;
	private JMenu helpMenu;
	private JMenuItem helpItem;
	private JMenuItem exampleItem;
	private JMenuItem aboutItem;
	private JMenuItem rawDirItem;
	private JMenuItem rangeDirItem;
	private JMenuItem outDirItem;
	private JMenuItem runBatchItem;
	private JMenuItem batchDirItem;
	private JMenuItem ruleDirItem;
	private JMenuItem printRangeOptionItem;
	private JMenuItem toggleRulesItem;
	private MenuActions menuActionListener = new MenuActions();
	private runButtonActions runButtonActionListener = new runButtonActions();
	
    /**
     * Creates new form CIUGenFrame GUI
     */
    public CIUGenFrame() 
    {
    	rawPaths = new ArrayList<String>();
    	
        initGUIComponents();
        initFileChoosers();

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));     
    }
    
    
    /**
     * Listener class for menu items
     * @author Dan
     *
     */
    public class MenuActions implements ActionListener{

		public void actionPerformed(ActionEvent e) {
			// Determine which menu item was clicked and implement appropriate behavior
			if (e.getSource() == rawDirItem){
				// Open a filechooser to set the default directory to look for raw files
				JFileChooser choosedir = new JFileChooser();
				choosedir.setDialogTitle("Choose the default starting directory for raw files");
				choosedir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				choosedir.setCurrentDirectory(new File(rawFileDirectory));
				if (choosedir.showDialog(new JFrame(), "OK") == 0){
					//Get the chosen directory and set it as the default raw directory
					File directory = choosedir.getSelectedFile();
					rawFileDirectory = directory.getAbsolutePath();	
					preferences.setRawDir(rawFileDirectory);
					preferences.writeConfig();
				}
				
			} else if (e.getSource() == rangeDirItem){
				// Open a filechooser to set the default directory to look for range files
				JFileChooser choosedir = new JFileChooser();
				choosedir.setDialogTitle("Choose the default starting directory for raw files");
				choosedir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				choosedir.setCurrentDirectory(new File(rangeFileDirectory));
				if (choosedir.showDialog(new JFrame(), "OK") == 0){
					//Get the chosen directory and set it as the default raw directory
					File directory = choosedir.getSelectedFile();
					rangeFileDirectory = directory.getAbsolutePath();	
					preferences.setRangeDir(rangeFileDirectory);
					preferences.writeConfig();
				}
				
			} else if (e.getSource() == outDirItem){
				// Open a filechooser to set the default directory for writing output files
				JFileChooser choosedir = new JFileChooser();
				choosedir.setDialogTitle("Choose the default starting directory for raw files");
				choosedir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				choosedir.setCurrentDirectory(new File(outputDirectory));
				if (choosedir.showDialog(new JFrame(), "OK") == 0){
					//Get the chosen directory and set it as the default raw directory
					File directory = choosedir.getSelectedFile();
					outputDirectory = directory.getAbsolutePath();	
					preferences.setOutDir(outputDirectory);
					preferences.writeConfig();
				}
				
			} else if (e.getSource() == batchDirItem){
				// Open a filechooser to set the default directory for writing output files
				JFileChooser choosedir = new JFileChooser();
				choosedir.setDialogTitle("Choose the default directory to find batch files");
				choosedir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				choosedir.setCurrentDirectory(new File(batchFileDirectory));
				if (choosedir.showDialog(new JFrame(), "OK") == 0){
					//Get the chosen directory and set it as the default raw directory
					File directory = choosedir.getSelectedFile();
					batchFileDirectory = directory.getAbsolutePath();	
					preferences.setBatchDir(batchFileDirectory);
					preferences.writeConfig();
				}
				
			} else if (e.getSource() == ruleDirItem){
				// Open a filechooser to set the default directory for writing output files
				JFileChooser choosedir = new JFileChooser();
				choosedir.setDialogTitle("Choose the default directory to find rule files");
				choosedir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				choosedir.setCurrentDirectory(new File(ruleFileDirectory));
				if (choosedir.showDialog(new JFrame(), "OK") == 0){
					//Get the chosen directory and set it as the default raw directory
					File directory = choosedir.getSelectedFile();
					ruleFileDirectory = directory.getAbsolutePath();	
					preferences.setRuleDir(ruleFileDirectory);
					preferences.writeConfig();
				}
			} else if (e.getSource() == runBatchItem){
				// Run the batch from the user's selected file
				runBatchCSV();
				
			} else if (e.getSource() == printRangeOptionItem){
				// Change (toggle) the range printing behavior
				if (printRanges){
					printRanges = false;
					System.out.println("Range printing has been turned OFF");
				} else {
					printRanges = true;
					System.out.println("Range printing has been turned ON");
				}
				
			} else if ((e.getSource()) == toggleRulesItem){
				// Toggle spectrum extract when range files are selected rather than DT extract
				if (ruleMode){
					ruleMode = false;
					statusTextBar.setText("Range File Mode (.txt)");
					ruleModeTextField.setText("Range Mode");
				} else {
					ruleMode = true;
					statusTextBar.setText("Selection Rule Mode (.rul)");
					ruleModeTextField.setText("Rule Mode");
				}
				
			} else if (e.getSource() == helpItem){
				// Open the help file included in the main CIUGen directory on install
				ProcessBuilder helpRunner = new ProcessBuilder("notepad.exe", preferences.getHELP_PATH());
				try {
					helpRunner.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			} else if (e.getSource() == exampleItem){
				// Open the example range file to show the user how to make range files
				ProcessBuilder exampleRunner = new ProcessBuilder("notepad.exe", preferences.getEXAMPLE_PATH());
				try {
					exampleRunner.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			} else if (e.getSource() == aboutItem){
				// Open the 'about' information tab with version, author, etc. Arbitrarily using JPanel1 as the parent component.
				JOptionPane.showMessageDialog(jPanel1_top, "By Dan Polasky");
			}
		}
    	
    }
    public class runButtonActions implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == runButton_DT) {
				runExtractorButton(e, IMExtractRunner.DT_MODE);
			} else if (e.getSource() == runButton_MZ){
				runExtractorButton(e, IMExtractRunner.MZ_MODE);
			} else if (e.getSource() == runButton_RT){
				runExtractorButton(e, IMExtractRunner.RT_MODE);
			} 
//			else if (e.getSource() == runButton_DTMZ){
//				runExtractorButton(e, IMExtractRunner.DTMZ_MODE);
//			}
		}
		
	}
	/**
     * Initialize primary GUI components
     */    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initGUIComponents() {
    	// initialize menus
    	initMenus();
    	this.setJMenuBar(menuBar);
    	
    	// Initialize other GUI components
    	topPanel = new javax.swing.JPanel();
    	jPanel1_top = new javax.swing.JPanel();
    	jPanel1_bottom = new javax.swing.JPanel();
    	jLabel1 = new javax.swing.JLabel();
    	statusTextBar = new javax.swing.JTextField("Press the Browse button to select raw files");
    	statusTextBar.setHorizontalAlignment(JTextField.CENTER);
    	browseDataButton = new javax.swing.JButton();
    	jPanel2 = new javax.swing.JPanel();
    	jScrollPane2 = new javax.swing.JScrollPane();
    	functionsTable = new javax.swing.JTable();
    	jPanel3 = new javax.swing.JPanel();
    	runPanel = new javax.swing.JPanel();
    	jLabel2 = new javax.swing.JLabel();
    	tabbedPane = new javax.swing.JTabbedPane();
    	runButton_DT = new javax.swing.JButton(); 
    	runButton_MZ = new javax.swing.JButton();
    	runButton_RT = new javax.swing.JButton();
//    	runButton_DTMZ = new javax.swing.JButton();
    	
    	trapcvCheckBox = new javax.swing.JCheckBox();
    	transfcvCheckBox = new javax.swing.JCheckBox();
    	conecvCheckBox = new javax.swing.JCheckBox();
    	whCheckBox = new javax.swing.JCheckBox();
    	wvCheckBox = new javax.swing.JCheckBox();
    	
    	checkBoxPanel = new javax.swing.JPanel();
    	setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    	setTitle(TITLE);

    	// jPanel 1:
    	topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
//    	topPanel.setLayout(new javax.swing.BoxLayout(jPanel1_top, javax.swing.BoxLayout.LINE_AXIS));
    	topPanel.setLayout(new java.awt.BorderLayout());

    	jLabel1.setText("Status:");
    	jPanel1_top.add(jLabel1);
    	jPanel1_top.add(statusTextBar);

    	browseDataButton.setText("Browse Data");
    	browseDataButton.addActionListener(new java.awt.event.ActionListener() {
    		public void actionPerformed(java.awt.event.ActionEvent evt) {
    			browseDataButtonActionPerformed(evt);
    		}
    	});
    	browseDataButton.setToolTipText("Opens a file chooser to select the raw data files from which to extract. Selected files will be displayed in the table below");
    	jPanel1_top.add(browseDataButton);
    	
    	ruleModeLabel = new javax.swing.JLabel("Range/Rule Mode:");
    	ruleModeTextField = new javax.swing.JTextField("Range Mode");
    	combineModeLabel = new javax.swing.JLabel("Combine Outputs?");
    	combineModeTextField = new javax.swing.JTextField("Yes");
    	
    	jPanel1_bottom.add(ruleModeLabel);
    	jPanel1_bottom.add(ruleModeTextField);
    	jPanel1_bottom.add(combineModeLabel);
    	jPanel1_bottom.add(combineModeTextField);
    	
    	topPanel.add(jPanel1_top, java.awt.BorderLayout.NORTH);
    	topPanel.add(jPanel1_bottom, java.awt.BorderLayout.SOUTH);
    	getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);

    	// jPanel 2:
    	jPanel2.setLayout(new java.awt.BorderLayout(10, 10));

    	functionsTable.setModel(new javax.swing.table.DefaultTableModel(
    			new Object [][] {},
    			new String [] {
    					"Func", "File", "Extract?" , "cone(V)", "trap(V)","transfer(V)", "WH(V)", "WV(m/s)"
    			}
    			) {
  
    		private static final long serialVersionUID = -9142278489188055889L;
    		Class[] types = new Class [] {
//    				java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class
    				java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Double.class,java.lang.Double.class,java.lang.Double.class,java.lang.Double.class,java.lang.Double.class
    		};
    		boolean[] canEdit = new boolean [] {
//    				false, false, true, true, false
    				false, false, true, false,false,false,false,false

    		};

    		public Class<?> getColumnClass(int columnIndex) {
    			return types [columnIndex];
    		}

    		public boolean isCellEditable(int rowIndex, int columnIndex) {
    			return canEdit [columnIndex];
    		}
    	}); 	// END functionsTable.setModel
    	
    	jScrollPane2.setViewportView(functionsTable);
    	jPanel2.add(jScrollPane2, java.awt.BorderLayout.CENTER);

    	// jPanel 3:
    	jPanel3.setLayout(new java.awt.BorderLayout());
    	jLabel2.setText("Choose an Extraction Mode to run:");
    	jPanel3.add(jLabel2, java.awt.BorderLayout.WEST);

    	// initialize the various extract data (run) buttons
    	initRunButtons();
    	runPanel.add(runButton_MZ);
    	runPanel.add(runButton_DT);
    	runPanel.add(runButton_RT);
//    	runPanel.add(runButton_DTMZ);
    	jPanel3.add(runPanel, java.awt.BorderLayout.EAST);
    	
    	// Initialize check boxes
    	initCheckBoxes();
 
    	jPanel3.add(checkBoxPanel, java.awt.BorderLayout.SOUTH);
//    	jPanel2.add(jPanel3, java.awt.BorderLayout.NORTH);
    	
    	getContentPane().add(jPanel2, java.awt.BorderLayout.WEST);
    	getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);
    	getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

    	pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void initRunButtons(){
    	runButton_DT.addActionListener(runButtonActionListener);
    	runButton_DT.setText("Extract DT");
    	runButton_DT.setToolTipText("Extracts a 1-dimensional drift time distribution (collapses all MS and RT information). "
    			+ "Will open a dialog to select the range or rule file with the range(s) to extract. ");
    	
    	runButton_MZ.addActionListener(runButtonActionListener);
    	runButton_MZ.setText("Extract MS");
    	runButton_MZ.setToolTipText("Extracts a 1-dimensional mass spectrum (collapses all RT and DT information). "
    			+ "Will open a dialog to select the range or rule file with the range(s) to extract.");
    	
    	runButton_RT.addActionListener(runButtonActionListener);
    	runButton_RT.setText("Extract RT");
    	runButton_RT.setToolTipText("Extracts a 1-dimensional retention time chromatogram (collapses all MZ and DT information). "
    			+ "Will open a dialog to select the range or rule file with the range(s) to extract.");
    	
//    	runButton_DTMZ.addActionListener(runButtonActionListener);
//    	runButton_DTMZ.setText("Extract 2D DT+MS");
//    	runButton_DTMZ.setToolTipText("Extracts 2-dimensional drift time/mass spectrum data (collapses RT only). "
//    			+ "Will open a dialog to select the range or rule file with the range(s) to extract.");
    	
    }
    
    private void initMenus(){
		// Menu items
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_A);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu");
		menuBar.add(fileMenu);
		batchMenu = new JMenu("Batch");
		batchMenu.setMnemonic(KeyEvent.VK_A);
		batchMenu.getAccessibleContext().setAccessibleDescription("Batch menu");
		menuBar.add(batchMenu);
		optionMenu = new JMenu("Options");
		optionMenu.setMnemonic(KeyEvent.VK_A);
		optionMenu.getAccessibleContext().setAccessibleDescription("Options menu");
		menuBar.add(optionMenu);
		selectionMenu = new JMenu("Selection Rules");
		selectionMenu.setMnemonic(KeyEvent.VK_A);
		selectionMenu.getAccessibleContext().setAccessibleDescription("Extract using selection rules instead of range files");
		menuBar.add(selectionMenu);
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_A);
		helpMenu.getAccessibleContext().setAccessibleDescription("Help menu");
		menuBar.add(helpMenu);
		
		// File menu items
		rawDirItem = new JMenuItem("Set default raw file directory");
		rawDirItem.addActionListener(menuActionListener);
		rangeDirItem = new JMenuItem("Set default Range file directory");
		rangeDirItem.addActionListener(menuActionListener);
		outDirItem = new JMenuItem("Set the directory for output files");
		outDirItem.addActionListener(menuActionListener);
		ruleDirItem = new JMenuItem("Set the directory for finding rule files");
		ruleDirItem.addActionListener(menuActionListener);
		fileMenu.add(rawDirItem);
		fileMenu.add(rangeDirItem);
		fileMenu.add(outDirItem);  	
		fileMenu.add(ruleDirItem);
		
		// Batch menu items
		runBatchItem = new JMenuItem("Select csv batch file and run batch");
		runBatchItem.addActionListener(menuActionListener);
		batchDirItem = new JMenuItem("Select default directory to look for batch files");
		batchDirItem.addActionListener(menuActionListener);
		batchMenu.add(batchDirItem);   	
		batchMenu.add(runBatchItem);

		// Option menu items
		printRangeOptionItem = new JMenuItem("Toggle range printing on or off");
		printRangeOptionItem.addActionListener(menuActionListener);
		optionMenu.add(printRangeOptionItem);
		
		// Spectrum menu items
		toggleRulesItem = new JMenuItem("Toggle using selection rules or range files");
		toggleRulesItem.addActionListener(menuActionListener);
		selectionMenu.add(toggleRulesItem);
		
		// Help menu items
		helpItem = new JMenuItem("Open help file");
		helpItem.addActionListener(menuActionListener);
		helpMenu.add(helpItem);
		exampleItem = new JMenuItem("Open range file example");
		exampleItem.addActionListener(menuActionListener);
		helpMenu.add(exampleItem);
		aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(menuActionListener);
		helpMenu.add(aboutItem);
		
	}
	/**
     * Initialize filechoosers for raw and range files using preferences information
     */
    private void initFileChoosers(){
        fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Select the data file(s) to analyze");
        fc.setFileFilter(new RawFileFilter());
        fc.setFileSelectionMode(2);
        
        // Initialize all directories
        rawFileDirectory = preferences.getRawDir();
        rangeFileDirectory = preferences.getRangeDir();
        outputDirectory = preferences.getOutDir();
        batchFileDirectory = preferences.getBatchDir();
        ruleFileDirectory = preferences.getRuleDir();
              
        File wdir = new File(rawFileDirectory);
        File rdir = new File(rangeFileDirectory);
        File bdir = new File(batchFileDirectory);
        File ruledir = new File(ruleFileDirectory);

        fc.setCurrentDirectory(wdir);
        fc.setMultiSelectionEnabled(true);
        
        rangefc = new JFileChooser();
        rangefc.setDialogTitle("Select the range text files to use");
        rangefc.setFileSelectionMode(2);              
        rangefc.setCurrentDirectory(rdir);
        rangefc.setMultiSelectionEnabled(true);
        rangefc.setFileFilter(new FileNameExtensionFilter("Text files only", "txt"));
        
        batchfc = new JFileChooser();
        batchfc.setDialogTitle("Select the batch csv files to use");
        batchfc.setFileSelectionMode(JFileChooser.FILES_ONLY);              
        batchfc.setCurrentDirectory(bdir);
        batchfc.setMultiSelectionEnabled(false);
        batchfc.setFileFilter(new FileNameExtensionFilter("CSV files only", "csv"));
        
        rulefc = new JFileChooser();
        rulefc.setDialogTitle("Select the Rule files to use");
        rulefc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        rulefc.setCurrentDirectory(ruledir);
        rulefc.setMultiSelectionEnabled(true);
        rulefc.setFileFilter(new FileNameExtensionFilter("rul files only","rul"));
        
    }
    
    /**
     * Method to initialize all the checkbox components - compartmentalized for easier access. 
     */
    private void initCheckBoxes(){ 	
    	// Listener for check boxes
    	ItemListener checkListener = new ItemListener(){
    		public void itemStateChanged(ItemEvent e) {
    			// Which box was clicked?
    			Object source = e.getItemSelectable();
    			int selection = e.getStateChange();
    			boolean selected = (selection == ItemEvent.SELECTED);

    			// Set global booleans to match selection state of the box
    			if (source == trapcvCheckBox){
    				if (selected){ useTrapCV = true; }
    				else { useTrapCV = false;}
    			} else if (source == transfcvCheckBox){
    				if (selected){ useTransfCV = true; }
    				else { useTransfCV = false;}
    			} else if (source == conecvCheckBox){
    				if (selected){ useConeCV = true; }
    				else { useConeCV = false;}
    			} else if (source == whCheckBox){
    				if (selected){ useWaveht = true; }
    				else { useWaveht = false;}
    			} else if (source == wvCheckBox){
    				if (selected){ useWavevel = true; }
    				else { useWavevel = false;}
    			} 
    		}
    	};
    	checkBoxLabel = new javax.swing.JLabel();
    	checkBoxLabel.setText("Save info:");
    	checkBoxPanel.add(checkBoxLabel);
    	
    	// Initialize check box GUI components to starting states and set tool tip information
    	trapcvCheckBox.setText("Trap CV");
    	trapcvCheckBox.setSelected(true);
    	useTrapCV = true;
    	trapcvCheckBox.setToolTipText("If selected, Trap CV information will be stored in the header of the final output csv");
    	
    	transfcvCheckBox.setText("Transfer CV");
    	transfcvCheckBox.setSelected(false);
    	transfcvCheckBox.setToolTipText("If selected, Transfer CV information will be stored in the header of the final output csv");
    	
    	conecvCheckBox.setText("Cone CV");
    	conecvCheckBox.setSelected(false);
    	conecvCheckBox.setToolTipText("If selected, cone voltage information will be stored in the header of the final output csv");
    	
    	whCheckBox.setText("Wave Ht");
    	whCheckBox.setSelected(false);
    	whCheckBox.setToolTipText("If selected, IMS wave height information will be stored in the header of the final output csv");
    	
    	wvCheckBox.setText("Wave Vel");
    	wvCheckBox.setSelected(false);
    	wvCheckBox.setToolTipText("If selected, IMS wave velocity information will be stored in the header of the final output csv");
  
    	trapcvCheckBox.addItemListener(checkListener);
    	transfcvCheckBox.addItemListener(checkListener);
    	conecvCheckBox.addItemListener(checkListener);
    	whCheckBox.addItemListener(checkListener);
    	wvCheckBox.addItemListener(checkListener);
    	
    	// Also add  check buttons to panel for addition to JPanel 3
    	checkBoxPanel.add(trapcvCheckBox);
    	checkBoxPanel.add(transfcvCheckBox);
    	checkBoxPanel.add(conecvCheckBox);
    	checkBoxPanel.add(whCheckBox);
    	checkBoxPanel.add(wvCheckBox);
    }
       
    /**
     * Opens filechooser for the user to choose the raw files they'd like to extract, then loads those
     * files into the function table using the getAllFunctionInfo parsing method. 
     * @param evt
     */
    private void browseDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDataButtonActionPerformed
        // Clear the old raw data paths out of memory, if applicable
    	rawPaths.clear();
    	cleanRoot();
    	
    	fc.setCurrentDirectory(new File(rawFileDirectory));
    	// First, clear the current data table (EDIT - moved up from further down in method)
    	DefaultTableModel tblModel = (DefaultTableModel)functionsTable.getModel();
        int rowCount = tblModel.getRowCount();
        for( int i=0; i<rowCount; i++ ){
            tblModel.removeRow(0);
        }

        if(fc.showDialog(this, "OK") == 0)
        {
        	File[] rawFiles = fc.getSelectedFiles();
        	openBrowsedData(rawFiles,tblModel);           
        }
    }//GEN-LAST:event_browseDataButtonActionPerformed

    /**
	 * Helper method that handles the actual opening of raw files for import into the table model. 
	 * @param rawFiles
	 * @param tblModel
	 */
	private void openBrowsedData(File[] rawFiles, DefaultTableModel tblModel){
		try {
			// Clear the global data array
			rawPaths.clear();
			cleanRoot();
			
			//EDIT - multiple files version
			for (File rawFile : rawFiles){
				String rawPath = rawFile.getCanonicalPath();
				String rawName = rawFile.getName();
				statusTextBar.setText("Now check any desired boxes and use the extract button to analyze");
	
				Vector<String> functions = getAllFunctionInfo(rawPath);      
	
				// Get filename here from splits array, stripping off only the '.raw' at the end
				String filename = "";
				try{
					filename = rawName.substring(0, rawName.lastIndexOf("."));
				} catch (StringIndexOutOfBoundsException ex){
					System.out.println(".raw files only. Please press the Browse button again to pick .raw file(s)");
				}
	
				String[] splits = null;
				for( String function : functions )
				{
					splits = function.split(",");
					rawPaths.add(rawPath);
	
					// Add all information to the table model
					//                     tblModel.addRow(new Object[]{Integer.parseInt(splits[0]), filename, Boolean.parseBoolean(splits[1]), Boolean.parseBoolean(splits[2]),rawPath});
					tblModel.addRow(new Object[]{Integer.parseInt(splits[FN_SPLITS]), filename, Boolean.parseBoolean(splits[SELECTED_SPLITS]),Double.parseDouble(splits[CONECV_SPLITS]),Double.parseDouble(splits[TRAPCV_SPLITS]),Double.parseDouble(splits[TRANSFCV_SPLITS]),Double.parseDouble(splits[WH_SPLITS]),Double.parseDouble(splits[WV_SPLITS])});
	
				}
				functionsTable.revalidate();
				functionsTable.repaint();          
			}
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}
	}

	/**
     * Method to extract specified data when the user hits one of the data extraction/run buttons.
     * Determines which range/rule mode has been specified and passes the set of range/rule files
     * on to the loop helper. 
     * @param evt
     */
    private void runExtractorButton(java.awt.event.ActionEvent evt, int extractionMode){
    	statusTextBar.setText("...Analyzing Data (may take a minute)...");
		System.out.println("Analyzing data (may take some time)");

    	rangefc.setCurrentDirectory(new File(rangeFileDirectory));
    	rulefc.setCurrentDirectory(new File(ruleFileDirectory));

    	if (ruleMode){
    		// Choose rule files for spectrum if in rule mode, or run extractor without if not
    		if(rulefc.showDialog(this,"OK") == 0){
    			File[] ruleFiles = rulefc.getSelectedFiles();
    			runExtraction(ruleFiles, extractionMode);
    		}   
    	} else {
    		// open file chooser to pick the range files
    		if(rangefc.showDialog(this, "OK") == 0)	
    		{
    			File[] rangeFiles = rangefc.getSelectedFiles();
    			// Run regular 1DDT extraction unless we're in spectrum mode, in which case run 1DMS extraction
    			runExtraction(rangeFiles, extractionMode);
    		}	
    	}
    	System.out.println("Done!");
		statusTextBar.setText("Done!");
	
		//One last thing - clean all the temp files out of the root folder
		cleanRoot();
    }  

    /*
	 * Execute the extraction loop - count through the range/rule files to be handled
	 */
	private void runExtraction(File[] rangeORruleFiles, int extractionMode){	
		int counter = 0;
		for (File rangeFile : rangeORruleFiles){
			combinedLoopHelper(rangeFile, rangeORruleFiles.length, extractionMode);
			counter++;
			System.out.println("\n" + "Completed Range/Rule File " + counter + " of " + rangeORruleFiles.length + "\n");
		}
	}

	/*
	 * Input and output file handling and calls the actual extractor (IMExtractRunner) to do the 
	 * extractions specified by the range/rule files and extraction_mode. 
	 */
	private void combinedLoopHelper(File rangeFile, int length, int extraction_mode){
		ArrayList<DataVectorInfoObject> allFunctions = new ArrayList<DataVectorInfoObject>();
		String rangePath = "";
		try {
			rangePath = rangeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String rangeFileName = rangeFile.getName();     				
		String csvOutName = rangeFileName + "#";       				

		// run imextract to extract a full mobiligram for each selected function in the functions table
		DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
		Vector<?> dataVector = funcsModel.getDataVector();
		IMExtractRunner imextractRunner = IMExtractRunner.getInstance();
		String rawName = rawPaths.get(0);
		String trimmedName = rawName; 

		for( int i=0; i<dataVector.size(); i++ ) {
			// Get the current data table row and its function information
			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			boolean selected = (boolean)row.get(SELECT_TABLE);
			rawName = (String)row.get(FILENAME_TABLE);
			trimmedName = rawName;
			String rawDataPath = rawPaths.get(i);
			File rawFile = new File(rawDataPath);
			double conecv = (double) row.get(CONECV_TABLE);
			double trapcv = (double)row.get(TRAPCV_TABLE);
			double transfcv = (double) row.get(TRANSFCV_TABLE);
			double wv = (double) row.get(WV_TABLE);
			double wh = (double) row.get(WH_TABLE);
			boolean[] infoTypes = {useConeCV,useTrapCV,useTransfCV,useWavevel,useWaveht};

			//Now get the ranges from each range file
			double[] rangesArr = null;
			if (ruleMode){
				// Call IMExtractRunner to generate a ranges.txt file in the root directory with the full ranges
				IMExtractRunner.getFullDataRanges(rawFile, function);
				// Read the full ranges.txt file generated
				String rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
				rangesArr = IMExtractRunner.readDataRanges(rangeLocation);
			} else {
				rangesArr = IMExtractRunner.readDataRanges(rangePath);
				System.out.println("Using Range File '" + rangeFileName);
				
			}
			if (printRanges){
				IMExtractRunner.PrintRanges(rangesArr);
			}

			// Single file output mode: create the output file and call the extractor inside the loop
			if (selected){
				DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawDataPath, rawName,function,selected,conecv,trapcv,transfcv,wh,wv,rangesArr,rangeFileName,infoTypes);
				allFunctions.add(functionInfo);

				if (! combine_outputs){
					// Pass a new list with only one function's info to the extractor
					ArrayList<DataVectorInfoObject> singleFunctionVector = new ArrayList<DataVectorInfoObject>();
					singleFunctionVector.add(functionInfo);
					
					// Make output directory folder to save files into if needed		
					File outputDir = new File(outputDirectory + File.separator + trimmedName);
					if (!outputDir.exists()){
						outputDir.mkdirs();
					}
					csvOutName = outputDir + File.separator + rangeFileName + "_#" + rawName + "_raw.csv";						

					// Call the extractor
					imextractRunner.extractMobiligramOneFile(singleFunctionVector, csvOutName, ruleMode, rangeFile, extraction_mode);
				}
			}
		}
		
		// Combine outputs mode: make directories and call extractor after loop is finished
		if (combine_outputs){
			// Make output directory folder to save files into if needed		
			File outputDir = new File(outputDirectory + File.separator + trimmedName);
			if (!outputDir.exists()){
				outputDir.mkdirs();
			}
			csvOutName = outputDir + File.separator + rangeFileName + "_#" + rawName + "_raw.csv";						
			
			//Once all function info has been gathered, send it to IMExtract
			imextractRunner.extractMobiligramOneFile(allFunctions,csvOutName, ruleMode, rangeFile, extraction_mode);
		}
	}


	

	/**
	 * Method opens a filechooser for the user to select their desired batch csv file, then uses
	 * the runBatch method to execute the batch run. 
	 * NOTE: All files will be extracted with the same settings, and all settings (checkboxes/etc)
	 * must be selected before starting the batch
	 */
	private void runBatchCSV(){
		batchfc.setCurrentDirectory(new File(batchFileDirectory));
		
		if (batchfc.showDialog(this, "OK") == 0){
			// Counters for printing status
			int batchCounter = 1;
			int linecounter = 0;
			
			try {			
				// User has chosen a csv file containing the desired lists
	    		File csvFile = batchfc.getSelectedFile();
	    		
	    		// Quickly read the file to get the number of lines present
	    		BufferedReader quickreader = new BufferedReader(new FileReader(csvFile));
	    		String quickline = quickreader.readLine();
	    		while (quickline != null){
	    			linecounter++;
	    			quickline = quickreader.readLine();
	    		}
	    		quickreader.close();
	    		
	    		// Read the file
	    		BufferedReader reader = new BufferedReader(new FileReader(csvFile));
	    		
	    		// loop through the file and analyze each line as a separate extraction
	    		String line = reader.readLine();
	    		while ( line != null){
	    			String[] splits = line.split(",");
	        		String rawfolderpath = splits[0];
	        		String rangefolderpath = splits[1];
	        		
	    			// **************** Get the associated files into an array to pass to the extractor **********************
	    			File rawTopFolder = new File(rawfolderpath);
	    			File[] rawFiles = rawTopFolder.listFiles(new RawFileFilter());
	
	    			// **************** Once files are ready, get the associated ranges to pass to the extractor *****************
	    			File rangeFileTopFolder = new File(rangefolderpath);
	    			File[] rangeFiles = rangeFileTopFolder.listFiles(new TextFileFilter());
	
	    			// ******** Once both raw and range files are ready, extract the data! *******
	    			extractBatchData(rawFiles, rangeFiles);
	    			System.out.println("*******************************************************");
	        		System.out.println("Done with Batch " + batchCounter + " of " + linecounter);
	        		System.out.println("*******************************************************");
	        		batchCounter++;
	        		
	    			line = reader.readLine();
	    		}
	    		
	    		reader.close();
	
			} catch (IOException ex) {
	            ex.printStackTrace();
	        }
		
			// Once finished, clean out temp files from CIUGen/root directory
			cleanRoot();
			System.out.println("DONE!");
		} 	
	}

	/**
	 * Method to run a batch extraction. Takes raw and range file arrays as input, loads raw function
	 * information into the data table, then extracts using the range files for each raw file. Method 
	 * should be called once per line of the batch (i.e. runs one set of raw files with their corresponding
	 * range files). 
	 * @param rawFiles: File array of raw files
	 * @param rangeFiles: File array of range files
	 */
	public void extractBatchData(File[] rawFiles, File[] rangeFiles){
		// First, clear the current data table (EDIT - moved up from further down in method)
		DefaultTableModel tblModel = (DefaultTableModel)functionsTable.getModel();
	    int rowCount = tblModel.getRowCount();
	    for( int i=0; i<rowCount; i++ ){
	        tblModel.removeRow(0);
	    }
	    
	    // Load the data into the table model and update it
	    openBrowsedData(rawFiles, tblModel);
	    
	    // Extract the data using the range files
	    runExtractHelper(rangeFiles);
	}

	/**
	 * Method that actually runs the code for extracting DT files. Allows multiple ways to get the range files
	 * (e.g. batched vs filechooser) and still have everything go to the same place. 
	 * @param rangeORruleFiles
	 */
	private void runExtractHelper(File[] rangeORruleFiles){
		statusTextBar.setText("...Analyzing Data (may take a minute)...");
		System.out.println("Analyzing data (may take some time)");
	
		// No lockmass check, so extract data and whatever information desired by the user
		statusTextBar.setText("...Analyzing Data (may take a minute)...");
		int counter = 1;
		for (File rangeFile : rangeORruleFiles){
//			DTLoopHelper(rangeFile, rangeORruleFiles.length);
			System.out.println("\n" + "Completed Range File " + counter + " of " + rangeORruleFiles.length + "\n");
			counter++;
		}
		System.out.println("Done!");
		statusTextBar.setText("Done!");
	
		//One last thing - clean all the temp files out of the root folder
		cleanRoot();
	}

	/*
     * Removes all temporary ".1dDT" files in the root directory once the program has finished.
     */
    public static void cleanRoot()
    {
    	String rootPath = preferences.getROOT_PATH();
    	File rootDir = new File(rootPath);
    	File[] allFiles = rootDir.listFiles();

    	for( File f : allFiles )
    		if (RawFileFilter.acceptdDT(f) || RawFileFilter.acceptMZ(f)){
    			f.delete();
    		}

    }

    /**
     * Determination of function information for Waters raw data files. NOTE: Different Waters
     * instruments record their information with VARYING NAMES, meaning instrument type needs to
     * be determined before function info can be gathered. 
     * NOTE: Only set up to work for Synapt HDMS and G2, no support for G2-S or G2-Si (for now)
     * @param rawDataPath
     * @return
     */
    private static Vector<String> getAllFunctionInfo(String rawDataPath)
    {
    	BufferedReader reader = null;
    	BufferedReader firstReader = null;

    	int numFunctions = 0;

    	Vector<String> functions = new  Vector<String>();

    	try 
    	{
    		File rawData = new File(rawDataPath, "_extern.inf");

    		// First, determine the instrument type (G1 or G2 for now - may add others if I can get their information)
    		firstReader = new BufferedReader(new FileReader(rawData));
    		String firstline = firstReader.readLine();
    		int instrumentType = 0;
    		while (firstline !=null){
    			if( firstline.toUpperCase().startsWith("ACQUISITION DEVICE") ){
    				// G1 doesn't have this line, so we know we're using a G2 if it appears
    				instrumentType = 1;
    			}   
    			firstline = firstReader.readLine();
    		}
    		firstReader.close();

    		reader = new BufferedReader(new FileReader(rawData));
    		String line = reader.readLine();
    		String[] splits = null;
    		boolean reachedFunctions = false;

    		// Initial values - output -1.0 if the information is not found
            double trapCV = -1.0;
            double transfCV = -1.0;
            double coneCV = -1.0;
            double wh = -1.0;
            double wv = -1.0;
            
            // Manual values at top of the file - initialize and record, to be used if info is not in the function parameters section
            double manualTrap = 0.0;
            double manualTransf = 0.0;
            double manualCone = 0.0;
            
            if (instrumentType == 1){
            	// Synapt G2

                // Read through the file for function information
                while( line != null ){
                	
                	// Determine non-function parameters (Wave ht, wave vel)
                    if( line.toUpperCase().startsWith("IMS WAVE VELOCITY") ){
                    	splits = line.split("\\t");
                    	String strWaveVel = splits[splits.length - 1];
                    	wv = new Double(strWaveVel);
                    } if( line.toUpperCase().startsWith("IMS WAVE HEIGHT")){
                    	splits = line.split("\\t");
                    	String strWaveHt = splits[splits.length - 1];
                    	wh = new Double(strWaveHt);
                    } 
                    
                    // Record the manual trap/transfer/cone settings, only use if that info can't be found in the function
                    if (line.startsWith("Sampling Cone") && !reachedFunctions){
                    	splits = line.split("\\t");
                    	String strCE = splits[splits.length - 1];
                    	manualCone = new Double(strCE);
                    } if (line.startsWith("Trap Collision Energy") && !reachedFunctions){
                    	splits = line.split("\\t");
                    	String strCE = splits[splits.length - 1];
                    	manualTrap = new Double(strCE);
                    } if (line.startsWith("Transfer Collision Energy") && !reachedFunctions){
                    	splits = line.split("\\t");
                    	String strCE = splits[splits.length - 1];
                    	manualTransf = new Double(strCE);
                    } 
                    
                	// wait until we've reached the function information to start recording collision voltage values
                    if(line.toUpperCase().startsWith("FUNCTION PARAMETERS") ){
                    	splits = line.split("\\t");
                    	if (reachedFunctions){
                    		// This is not the first function, so write out the previous function info and start fresh
                    		String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
                        	functions.add(function);
                        	numFunctions++;
                    	} else {
                    		reachedFunctions = true;
                            numFunctions++;
                    	}              
                    } 
                    
                   // Record CV information. It will be exported to a function when we reach the next function or end of the file. Handles several types of names
                    if( line.startsWith("Trap Collision Energy (eV)") && reachedFunctions){
                        splits = line.split("\\t");
                        String strCE = splits[splits.length - 1];
                        trapCV = new Double(strCE);
                    } if ( line.startsWith("Transfer Collision Energy (eV)") && reachedFunctions){
                    	splits = line.split("\\t");
                        String strCE = splits[splits.length - 1];
                        transfCV = new Double(strCE);
                    } if (line.startsWith("Cone Voltage (V)") && reachedFunctions){
                    	splits = line.split("\\t");
                        String strCE = splits[splits.length - 1];
                        coneCV = new Double(strCE);
                    }
                    
                    // Last line of all files is always "calibration". If we've reached that line with no 
                    // collision information, read it from the top
                    if (line.startsWith("Calibration")){
                    	if (trapCV == -1.0){
                    		// No trap information read, use manual info from top of file
                    		trapCV = manualTrap;
                    	} if (transfCV == -1.0){
                    		transfCV = manualTransf;
                    	} if (coneCV == -1.0){
                    		coneCV = manualCone;
                    	}
                    }
                    
                    
                    line = reader.readLine();          
                } // End - read fn info loop G2 (reached the end of the file)
                
                // Output the final function information to a function
        		String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
            	functions.add(function);
            	            	
            } else if (instrumentType == 0) {
            	// Synapt G1
            	// Read through the file for function information
            	while( line != null ){  	
            		// Determine non-function parameters (Wave ht, wave vel)
            		if( line.toUpperCase().startsWith("IMS WAVE VELOCITY") ){
            			splits = line.split("\\t");
            			String strWaveVel = splits[splits.length - 1];
            			wv = new Double(strWaveVel);
            		} if( line.toUpperCase().startsWith("IMS WAVE HEIGHT")){
            			splits = line.split("\\t");
            			String strWaveHt = splits[splits.length - 1];
            			wh = new Double(strWaveHt);
            		}     
            		// Record the manual trap/transfer/cone settings, only use if that info can't be found in the function
            		if (line.startsWith("Sampling Cone") && !reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			manualCone = new Double(strCE);
            		} if (line.startsWith("Trap Collision Energy") && !reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			manualTrap = new Double(strCE);
            		} if (line.startsWith("Transfer Collision Energy") && !reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			manualTransf = new Double(strCE);
            		} 


            		// wait until we've reached the function information to start recording collision voltage values
            		if(line.toUpperCase().startsWith("FUNCTION PARAMETERS") ){
            			splits = line.split("\\t");
            			if (reachedFunctions){
            				// This is not the first function, so write out the previous function info and start fresh
            				String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
            				functions.add(function);
            				numFunctions++;
            			} else {
            				reachedFunctions = true;
            				numFunctions++;
            			}              
            		} 

            		// Record CV information. It will be exported to a function when we reach the next function or end of the file
            		if( line.startsWith("Collision Energy (eV)") && reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			trapCV = new Double(strCE);
            		} if (line.startsWith("Collision Energy2 (eV)") && reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			transfCV = new Double(strCE);
            		} if (line.startsWith("Cone Voltage (V)") && reachedFunctions){
            			splits = line.split("\\t");
            			String strCE = splits[splits.length - 1];
            			coneCV = new Double(strCE);
            		}
            		// Last line of all files is always "calibration". If we've reached that line with no 
            		// collision information, read it from the top
            		if (line.startsWith("Calibration")){
            			if (trapCV == -1.0){
            				// No trap information read, use manual info from top of file
            				trapCV = manualTrap;
            			} if (transfCV == -1.0){
            				transfCV = manualTransf;
            			} if (coneCV == -1.0){
            				coneCV = manualCone;
            			}
            		}

            		line = reader.readLine();          
            	} // End - read fn info loop G1 (reached the end of the file)

            	// Output the final function information to a function
            	String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
            	functions.add(function);	

            } else {
            	System.out.println("Instrument type not determined, unable to extract function information");
            }  

            reader.close();

    	} 
    	catch (FileNotFoundException ex) 
    	{
    		ex.printStackTrace();
    	} 
    	catch (IOException ex) 
    	{
            ex.printStackTrace();
        } 
        finally 
        {
            try 
            {
                reader.close();
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
        return functions;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CIUGenFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CIUGenFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CIUGenFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CIUGenFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CIUGenFrame().setVisible(true);
            }
        });
    }


	// Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDataButton;
    private javax.swing.JTable functionsTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel checkBoxLabel;
    private javax.swing.JPanel topPanel;
    private javax.swing.JPanel jPanel1_top;
    private javax.swing.JPanel jPanel1_bottom;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel runPanel;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField statusTextBar;
    private javax.swing.JLabel ruleModeLabel;
    private javax.swing.JTextField ruleModeTextField;
    private javax.swing.JLabel combineModeLabel;
	private javax.swing.JTextField combineModeTextField;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton runButton_DT; 
    private javax.swing.JButton runButton_MZ;
    private javax.swing.JButton runButton_RT;
//    private javax.swing.JButton runButton_DTMZ;
    private javax.swing.JCheckBox trapcvCheckBox;
    private javax.swing.JCheckBox transfcvCheckBox;
    private javax.swing.JCheckBox conecvCheckBox;
    private javax.swing.JCheckBox whCheckBox;
    private javax.swing.JCheckBox wvCheckBox;
    private javax.swing.JPanel checkBoxPanel;
    // End of variables declaration//GEN-END:variables
   
}


///**
//* Gets the function information for the specified raw data file
//*/
//private static Vector<String> getFunctionInfo2(String rawDataPath)
//{
//  File rawData = null;
//  Vector<String> m_functions = new Vector<String>(); 
//  
//  
////  try
////  {
//      try
//      {
//          // Function Types
//          final int	FT_MS = 0;
//          final int	FT_SIR = 1;
//          final int	FT_DLY = 2;
//          final int	FT_CAT = 3;
//          final int	FT_OFF = 4;
//          final int	FT_PAR = 5;
//          final int	FT_DAU = 6;
//          final int   FT_NL = 7;
//          final int   FT_NG = 8;
//          final int	FT_MRM = 9;
//          final int	FT_Q1F = 10;
//          final int	FT_MS2 = 11;
//          final int	FT_DAD = 12;
//          final int	FT_TOF = 13;
//          final int	FT_PSD = 14;
//          final int	FT_TOFS = 15;	// QTOF MS Survey scan
//          final int	FT_TOFD = 16;	// QTOF MSMS scan (daughter )
//          final int	FT_MTOF = 17;	// Maldi-Tof function superseeds RAW_FUNC_TOF and RAW_FUNC_PSD
//          final int	FT_TOFM = 18;	// QTOF MS scan
//          final int	FT_TOFP	= 19;	// QTOF Parent scan
//          final int	FT_ASVS	= 20;	// AutoSpec Voltage Scan
//          final int	FT_ASMS	= 21;	// AutoSpec Magnet Scan
//          final int	FT_ASVSIR = 22;	// AutoSpec Voltage SIR
//          final int	FT_ASMSIR = 23;	// AutoSpec Magnet SIR
//          final int	FT_QUADD = 24;	// Quad Automated daughter scanning
//
//
//          // Ion Modes
//          final int	IM_EIPOS = 0;
//          final int	IM_EIMIN = 1;
//          final int	IM_CIPOS = 2;
//          final int	IM_CIMIN = 3;
//          final int	IM_FBPOS = 4;
//          final int	IM_FBMIN = 5;
//          final int	IM_TSPOS = 6;
//          final int	IM_TSMIN = 7;
//          final int	IM_ESPOS = 8;
//          final int	IM_ESMIN = 9;
//          final int	IM_AIPOS = 10;
//          final int	IM_AIMIN = 11;
//          final int	IM_LDPOS = 12;
//          final int	IM_LDMIN = 13;
//
//          rawData = new File( rawDataPath);
//          File functionsFile = new File( rawData, "_functns.inf" );
//          RandomAccessFile rafFile = new RandomAccessFile( functionsFile, "r" );
//          FileChannel channel = rafFile.getChannel();
//
//          // The memory mapped buffer for function info
//          MappedByteBuffer nMbb = null;
//          nMbb = channel.map( FileChannel.MapMode.READ_ONLY, 0L, functionsFile.length() );
//          nMbb = nMbb.load();
//          nMbb.order(ByteOrder.LITTLE_ENDIAN);
//
//          while( nMbb.position() < functionsFile.length() )
//          {
//              // Do the reading here
//              short sVal = nMbb.getShort();
//              int nFunctionType = (int)(sVal &0x001F);
//              int nIonMode = (int)((sVal >> 5) &0x001F);
//              int nDataType = (int)((sVal >> 10) &0x001F);
//
//              float fCycleTime = nMbb.getFloat();
//              float fInterScanDelay = nMbb.getFloat();
//              float fRTStart = nMbb.getFloat();
//              float fRTEnd = nMbb.getFloat();
//              int nTotalScans = nMbb.getInt();
//
//              sVal = nMbb.getShort();
//              int nCollisionEnergy = (int)(sVal &0x00FF);
//              int nSegmentCount = (int)((sVal >> 8) &0x00FF);
//
//              float fSetMass = nMbb.getFloat();
//              float fInterSegTime = nMbb.getFloat();
//
//              int MAX_SEGMENTS = 32;
//              float[] fSegScanTimes = new float[ MAX_SEGMENTS ];
//              float[] fStartMass = new float[ MAX_SEGMENTS ];
//              float[] fEndMass = new float[ MAX_SEGMENTS ];
//
//              for( int i=0; i<MAX_SEGMENTS; i++ )
//              {
//                  fSegScanTimes[ i ] = nMbb.getFloat();
//              }
//
//              for( int i=0; i<MAX_SEGMENTS; i++ )
//              {
//                  fStartMass[ i ] = nMbb.getFloat();
//              }
//
//              for( int i=0; i<MAX_SEGMENTS; i++ )
//              {
//                  fEndMass[ i ] = nMbb.getFloat();
//              }
//
//              StringBuffer buffer = new StringBuffer();
//              
//              int functionNumber = m_functions.size() + 1;
//              buffer.append( functionNumber );
//
//              // Get the type 
////              switch(nFunctionType)
////              {
////                  case FT_MS:
////                          buffer.append(  "MS" );
////                          break;
////                  case FT_SIR:
////                          buffer.append(  "SIR" );
////                          break;
////                  case FT_DLY:
////                          buffer.append(  "DLY" );
////                          break;
////                  case FT_CAT:
////                          buffer.append(  "CAT" );
////                          break;
////                  case FT_OFF:
////                          buffer.append(  "OFF" );
////                          break;
////                  case FT_PAR:
////                          buffer.append(  "PAR" );
////                          break;
////                  case FT_DAU:
////                          buffer.append(  "MSMS" );
////                          break;
////                  case FT_NL:
////                          buffer.append(  "NL" );
////                          break;
////                  case FT_NG:
////                          buffer.append(  "NG" );
////                          break;
////                  case FT_MRM:
////                          buffer.append(  "MRM" );
////                          break;
////                  case FT_Q1F:
////                          buffer.append(  "Q1F" );
////                          break;
////                  case FT_MS2:
////                          buffer.append(  "MS2" );
////                          break;
////                  case FT_DAD:
////                          buffer.append(  "DAD" );
////                          break;
////                  case FT_TOF:
////                          buffer.append(  "TOF" );
////                          break;
////                  case FT_PSD:
////                          buffer.append(  "PSD" );
////                          break;
////                  case FT_TOFS:
////                          buffer.append(  "TOFS" );
////                          break;
////                  case FT_TOFD:
////                          buffer.append(  "TOFD" );
////                          break;
////                  case FT_MTOF:// Maldi-Tof function superseeds RAW_FUNC_TOF and RAW_FUNC_PSD
////                          buffer.append(  "MTOF" );
////                          break;
////                  case FT_TOFM:// QTOF MS scan
////                          buffer.append(  "TOF MS" );
////                          break;
////                  case FT_TOFP:// QTOF Parent scan
////                          buffer.append(  "TOT P" );
////                          break;
////                  case FT_ASVS:// AutoSpec Voltage Scan
////                          buffer.append(  "TOT P" );
////                          break;
////                  case FT_ASMS:// AutoSpec Magnet Scan
////                          buffer.append(  "TOT P" );
////                          break;
////                  case FT_ASVSIR:// AutoSpec Voltage SIR
////                          buffer.append(  "TOT P" );
////                          break;
////                  case FT_ASMSIR:// AutoSpec Magnet SIR
////                          buffer.append(  "TOT P" );
////                          break;
////                  case FT_QUADD:// Quad Automated daughter scanning
////                          buffer.append(  "TOT P" );
////              }
//
////              // Insert a space
////              buffer.append( " " );
////              // Include the mass range
////              buffer.append( "(" + fStartMass[ 0 ] + ":" + fEndMass[ 0 ] + ")" );
//              // Include the collision energy
//              buffer.append( "," + nCollisionEnergy);
//              // Insert a space
//              buffer.append( ",false" );
//
////              // Get the ion mode
////              switch(nIonMode)
////              {
////                  case IM_EIPOS:
////                          buffer.append(  "EI+" );
////                          break;
////                  case IM_EIMIN:
////                          buffer.append(  "EI-" );
////                          break;
////                  case IM_CIPOS:
////                          buffer.append(  "CI+" );
////                          break;
////                  case IM_CIMIN:
////                          buffer.append(  "CI-" );
////                          break;
////                  case IM_FBPOS:
////                          buffer.append(  "FB+" );
////                          break;
////                  case IM_FBMIN:
////                          buffer.append(  "FB-" );
////                          break;
////                  case IM_TSPOS:
////                          buffer.append(  "TS+" );
////                          break;
////                  case IM_TSMIN:
////                          buffer.append(  "TS-" );
////                          break;
////                  case IM_ESPOS:
////                          buffer.append(  "ES+" );
////                          break;
////                  case IM_ESMIN:
////                          buffer.append(  "ES-" );
////                          break;
////                  case IM_AIPOS:
////                          buffer.append(  "AI+" );
////                          break;
////                  case IM_AIMIN:
////                          buffer.append(  "AI-" );
////                          break;
////                  case IM_LDPOS:
////                          buffer.append(  "LD+" );
////                          break;
////                  case IM_LDMIN:
////                          buffer.append(  "LD-" );
////                          break;
////              }
//
//              m_functions.add( buffer.toString() );
//          }
//      }
//      catch( Exception ex )
//      {
//          ex.printStackTrace();
//          return m_functions;
//      }
//
////      // Get all cdt file names
////      String[] cdtFiles = rawData.list( new FilenameFilter()
////      {
////          public boolean accept(File dir, String name)
////          {
////              return name.endsWith( ".cdt" );
////          }
////      });
////
////      // Place all cdt file name in a container
////      Vector v = new Vector();
////
////      if( cdtFiles != null )
////      {
////          for( int n=0; n<cdtFiles.length; n++ )
////          {
////              v.add( v.size(), cdtFiles[ n ] );
////          }
////      }
//
////      // Loop through the functions
////      for( int i=0; i<v.size(); i++ )
////      {
////          // Check if the next function has an associated cdt file
////          String cdtFileName = String.format("_func%03d.cdt",i+1); 
////          if( v.contains( cdtFileName ) )
////          {
////              if( i <= m_functions.size() - 1 )
////              {
////                  getReplicateFunctions().add((i + 1) + ": " + m_functions.get( i ));
////              }
////              else
////              {
////                  getReplicateFunctions().add((i + 1) + ": " + "function " + ( i + 1 ));
////              }
////          }
////      }
////  
//      return m_functions;
//}

