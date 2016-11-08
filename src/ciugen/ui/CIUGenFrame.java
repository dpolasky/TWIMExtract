/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

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
	private static final String TITLE = "Unified Raw Data Extraction Tool - v3.6.3";
	
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

    // Constants for computing lockmass correction, based on Glu-Fib 2+ reference (exact mass) used in Martin lab
    private static final double LOCKMASS_REFERENCE = 785.8426;
    private static final double[] LOCKMASS_RANGES = {50.0,2000.0,100000,0.0,200.0,200,1,200,200};
    private static final double LOCKMASS_MIN_MZ = 750.0;
    private static final double LOCKMASS_MAX_MZ = 825.0;
      
    // CheckBox booleans
    private boolean useTrapCV;
    private boolean useTransfCV;
    private boolean useConeCV;
    private boolean useLockmass;
    private boolean useWavevel;
    private boolean useWaveht;
    
    // Stores user's desired behavior
    private boolean printRanges = false;
//    private boolean spectrumMode = false;
    private boolean ruleMode = false;
    
    // global file information
    private ArrayList<String> rawPaths;
    
    // String locations for function strings from getAllFunctionInfo
    private static final int TRAPCV_SPLITS = 4;
    private static final int WH_SPLITS = 6;
    private static final int WV_SPLITS = 7;
    private static final int TRANSFCV_SPLITS = 5;
    private static final int CONECV_SPLITS = 3;
//    private static final int SELECTED_SPLITS  = 1;
    
    // String locations for table model (includes filename) - has to be in this order because that's the order it displays in the GUI
    private static final int FN_TABLE = 0;
    private static final int FILENAME_TABLE = 1;
    private static final int SELECT_TABLE = 2;
    private static final int LOCKMASS_TABLE = 3;
    private static final int CONECV_TABLE = 4;
    private static final int TRAPCV_TABLE = 5;
    private static final int TRANSFCV_TABLE = 6;
    private static final int WH_TABLE = 7;
    private static final int WV_TABLE = 8;
    
    // Menu components
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu batchMenu;
	private JMenu optionMenu;
	private JMenu selectionMenu;
	private JMenuItem rawDirItem;
	private JMenuItem rangeDirItem;
	private JMenuItem outDirItem;
	private JMenuItem runBatchItem;
	private JMenuItem batchDirItem;
	private JMenuItem ruleDirItem;
	private JMenuItem printRangeOptionItem;
	private JMenuItem toggleRulesItem;
	private MenuActions menuActionListener = new MenuActions();
	
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
					rawDataTextField.setText("Range File Mode (.txt)");

				} else {
					ruleMode = true;
					rawDataTextField.setText("Selection Rule Mode (.rul)");
				}
				
			}
		}
    	
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initGUIComponents() {
    	// initialize menus
    	initMenus();
    	this.setJMenuBar(menuBar);
    	
    	// Initialize other GUI components
    	jPanel1 = new javax.swing.JPanel();
    	jLabel1 = new javax.swing.JLabel();
    	rawDataTextField = new javax.swing.JTextField("Press the Browse button to select raw files");
    	rawDataTextField.setHorizontalAlignment(JTextField.CENTER);
    	browseDataButton = new javax.swing.JButton();
    	jPanel2 = new javax.swing.JPanel();
    	jScrollPane2 = new javax.swing.JScrollPane();
    	functionsTable = new javax.swing.JTable();
    	jPanel3 = new javax.swing.JPanel();
    	jLabel2 = new javax.swing.JLabel();
//    	runButton = new javax.swing.JButton();
    	tabbedPane = new javax.swing.JTabbedPane();
    	multiRangeRunButton = new javax.swing.JButton(); //EDIT - added
    	spectrumButton = new javax.swing.JButton();
    	
    	trapcvCheckBox = new javax.swing.JCheckBox();
    	transfcvCheckBox = new javax.swing.JCheckBox();
    	conecvCheckBox = new javax.swing.JCheckBox();
    	whCheckBox = new javax.swing.JCheckBox();
    	wvCheckBox = new javax.swing.JCheckBox();
    	lockmassCheckBox = new javax.swing.JCheckBox();
    	
    	checkBoxPanel = new javax.swing.JPanel();
    	setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    	setTitle(TITLE);

    	// jPanel 1:
    	jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    	jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

    	jLabel1.setText("Status:");
    	jPanel1.add(jLabel1);
    	jPanel1.add(rawDataTextField);

    	browseDataButton.setText("Browse Data");
    	browseDataButton.addActionListener(new java.awt.event.ActionListener() {
    		public void actionPerformed(java.awt.event.ActionEvent evt) {
    			browseDataButtonActionPerformed(evt);
    		}
    	});
    	jPanel1.add(browseDataButton);
    	getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

    	// jPanel 2:
    	jPanel2.setLayout(new java.awt.BorderLayout(10, 10));

    	functionsTable.setModel(new javax.swing.table.DefaultTableModel(
    			new Object [][] {},
//    			new String [] {
//    					"Func", "Filename", "Raw MS1" , "Lock Mass", "rawPath"
//    			}
    			new String [] {
    					"Func", "Name", "Raw MS1" , "Lock Mass", "cone(V)", "trap(V)","transfer(V)", "WH(V)", "WV(m/s)"
    			}
    			) {
    		/**
    		 * 
    		 */
    		private static final long serialVersionUID = -9142278489188055889L;
    		Class[] types = new Class [] {
//    				java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class
    				java.lang.Integer.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Double.class,java.lang.Double.class,java.lang.Double.class,java.lang.Double.class,java.lang.Double.class
    		};
    		boolean[] canEdit = new boolean [] {
//    				false, false, true, true, false
    				false, false, true, true, false,false,false,false,false

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

    	jLabel2.setText("Available Functions:");
    	jPanel3.add(jLabel2, java.awt.BorderLayout.WEST);

    	multiRangeRunButton.addActionListener(new java.awt.event.ActionListener() {
    		public void actionPerformed(java.awt.event.ActionEvent evt) {
    			runExtractorButton(evt);
    		}
    	});
    	multiRangeRunButton.setText("Extract DT");
    	spectrumButton.addActionListener(new java.awt.event.ActionListener(){
    		public void actionPerformed(java.awt.event.ActionEvent evt){
    			runSpectrumExtractorButton(evt);
    		}
    	});
    	spectrumButton.setText("Extract MS");
    	
    	
    	jPanel3.add(spectrumButton, java.awt.BorderLayout.EAST);
    	jPanel3.add(multiRangeRunButton, java.awt.BorderLayout.CENTER);

    	// Initialize check boxes
    	initCheckBoxes();
 
    	jPanel3.add(checkBoxPanel, java.awt.BorderLayout.SOUTH);
    	
    	jPanel2.add(jPanel3, java.awt.BorderLayout.NORTH);
    	
    	getContentPane().add(jPanel2, java.awt.BorderLayout.WEST);
    	getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);
//    	getContentPane().add(jPanel3, java.awt.BorderLayout.EAST);

    	pack();
    }// </editor-fold>//GEN-END:initComponents
    
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
    			} else if (source == lockmassCheckBox){
    				if (selected){ useLockmass = true; }
    				else { useLockmass = false;}
    			} 
    		}
    	};
    	
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
    	
    	lockmassCheckBox.setText("Use Lockmass Fn 3");
    	lockmassCheckBox.setSelected(false);
    	lockmassCheckBox.setToolTipText("If selected, function 3 MS data will be searched for lockmass and used to correct raw data in function 1.");
    
    	trapcvCheckBox.addItemListener(checkListener);
    	transfcvCheckBox.addItemListener(checkListener);
    	conecvCheckBox.addItemListener(checkListener);
    	whCheckBox.addItemListener(checkListener);
    	wvCheckBox.addItemListener(checkListener);
    	lockmassCheckBox.addItemListener(checkListener);
    	
    	// Also add  check buttons to panel for addition to JPanel 3
    	checkBoxPanel.add(trapcvCheckBox);
    	checkBoxPanel.add(transfcvCheckBox);
    	checkBoxPanel.add(conecvCheckBox);
    	checkBoxPanel.add(whCheckBox);
    	checkBoxPanel.add(wvCheckBox);
    	checkBoxPanel.add(lockmassCheckBox);
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
				rawDataTextField.setText("Now check any desired boxes and use the extract button to analyze");
	
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
					tblModel.addRow(new Object[]{Integer.parseInt(splits[0]), filename, Boolean.parseBoolean(splits[1]), Boolean.parseBoolean(splits[2]),Double.parseDouble(splits[CONECV_SPLITS]),Double.parseDouble(splits[TRAPCV_SPLITS]),Double.parseDouble(splits[TRANSFCV_SPLITS]),Double.parseDouble(splits[WH_SPLITS]),Double.parseDouble(splits[WV_SPLITS])});
	
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
     * Method to extract specified data when the user hits the 'extract data' button. If lockmass correction
     * is being used, handles that; otherwise just extracts data. Passes the raw files to extract, range files,
     * and desired outputs to IMExtractRunner.
     * @param evt
     */
    private void runExtractorButton(java.awt.event.ActionEvent evt){
    	rawDataTextField.setText("...Analyzing Data (may take a minute)...");
    	rangefc.setCurrentDirectory(new File(rangeFileDirectory));
    	rulefc.setCurrentDirectory(new File(ruleFileDirectory));

    	if (ruleMode){
    		// Choose rule files for spectrum if in rule mode, or run extractor without if not
    		if(rulefc.showDialog(this,"OK") == 0){
    			File[] ruleFiles = rulefc.getSelectedFiles();
    			runExtractHelper(ruleFiles);
    		}   
    	} else {
    		// open file chooser to pick the range files
    		if(rangefc.showDialog(this, "OK") == 0)	
    		{
    			File[] rangeFiles = rangefc.getSelectedFiles();
    			// Run regular 1DDT extraction unless we're in spectrum mode, in which case run 1DMS extraction
    			runExtractHelper(rangeFiles);

    		}	
    	}
    }  
    
    /**
     * Handler for 1D MS mode extractions from "MS" button. Passes to helper methods that handle extraction
     * of 1D MS data, using rule files or range files as indicated by the user. 
     * @param evt
     */
    private void runSpectrumExtractorButton(java.awt.event.ActionEvent evt){
    	rawDataTextField.setText("...Analyzing Data (may take a minute)...");
    	rangefc.setCurrentDirectory(new File(rangeFileDirectory));
    	rulefc.setCurrentDirectory(new File(ruleFileDirectory));

    	// open file chooser to pick the range files
    	// Choose rule files for spectrum
    	if (ruleMode){
    		if(rulefc.showDialog(this,"OK") == 0){
    			File[] ruleFiles = rulefc.getSelectedFiles();
    			runSpectrumExtractHelper(ruleFiles);
    		}
    	} else {
    		if(rangefc.showDialog(this, "OK") == 0)	{
    			File[] rangeFiles = rangefc.getSelectedFiles();
    			runSpectrumExtractHelper(rangeFiles);
    		}
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
	 * Helper method to get correction factor for lockmass data
	 * @param rawFilesWithLockmass
	 * @param lockmassflag
	 * @param dataVector
	 * @param sampleRawDataPath
	 * @param imextractRunner
	 * @return
	 */
	private double getCorrectionFactor(ArrayList<String> rawFilesWithLockmass, boolean lockmassflag, Vector<?> dataVector, String sampleRawDataPath, IMExtractRunner imextractRunner){
		double correctionFactor = 0;
		double lockmassMZ = 0;
		for (int i=0; i<dataVector.size(); i++){

			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			//        					boolean selected = (boolean)row.get(SELECT_TABLE);
			boolean lockmass = (boolean) row.get(LOCKMASS_TABLE);
			String rawName = (String)row.get(FILENAME_TABLE);
			String rawDataPath = rawPaths.get(i);

			if (rawDataPath.equals(sampleRawDataPath)){
				if (lockmass){
					// Only compute lockmass if we haven't already for this sample
					if (!rawFilesWithLockmass.contains(rawDataPath)){
						System.out.println("\n" + "Getting lockmass correction for sample "  + rawName);
						lockmassMZ = imextractRunner.extractSpectrumForLockmass(rawDataPath, function, LOCKMASS_RANGES, LOCKMASS_MIN_MZ, LOCKMASS_MAX_MZ);								

						// Update flags so we don't compute this lockmass again
						lockmassflag = true;
						rawFilesWithLockmass.add(rawDataPath);

						// Compute correction factor
						//    									correctionFactor = 0;
						correctionFactor = LOCKMASS_REFERENCE - lockmassMZ;
						System.out.println("Correction Factor (Da) = " + correctionFactor + "\n" + "**************"); 		    			
					}
				}
			}			
		}
		return correctionFactor;
	}
	
	/**
	 * Method that actually runs the code for extracting DT files. Allows multiple ways to get the range files
	 * (e.g. batched vs filechooser) and still have everything go to the same place. 
	 * @param rangeORruleFiles
	 */
	private void runExtractHelper(File[] rangeORruleFiles){
		rawDataTextField.setText("...Analyzing Data (may take a minute)...");
		System.out.println("Analyzing data (may take some time)");
		// Determine lockmass or not based on user's input
		if (useLockmass){
			// *************** LOCKMASS SECTION - RANGE FILES LOOPED FIRST FOR SINGLE LOCKMASS CALC ***************
			//iterator for the range file number
			String csvOutName = "";

			// Object to hold all info for the data vector
			ArrayList<DataVectorInfoObject> allFunctions = new ArrayList<DataVectorInfoObject>();
			DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
			Vector<?> dataVector = funcsModel.getDataVector();
			IMExtractRunner imextractRunner = IMExtractRunner.getInstance();   			

			// Loop through RAW DATA, not range files first so that we can compute lockmass correction only once
			// Iterate through the samples, computing lockmass and then analyzing data using the calculated correction factor
			ArrayList<String> rawFilesWithLockmass = new ArrayList<String>();
			for (String sampleRawDataPath : rawPaths){
				// Get the lockmass function row in the table
				boolean lockmassflag = false;
				double correctionFactor = getCorrectionFactor(rawFilesWithLockmass, lockmassflag, dataVector, sampleRawDataPath, imextractRunner);

				// Loop through the range files
				for (File rangeFile : rangeORruleFiles){		
					lockmassDTLoopHelper(rangeFile, dataVector, correctionFactor, allFunctions, lockmassflag, sampleRawDataPath);
				}	
			}	

			// UPDATED - Once all function info has been gathered, send it to IMExtract
			// DOES NOT HANDLE RULE FILES FOR NOW
			imextractRunner.extractMobiligramOneFile(allFunctions,csvOutName, false, null);
			System.out.println("Done!");
			rawDataTextField.setText("Done!");

			// END: ************** LOCKMASS LOOP (ALTERNATE ORDER) ***************
		} else {
			// No lockmass check, so extract data and whatever information desired by the user
			rawDataTextField.setText("...Analyzing Data (may take a minute)...");
			int counter = 1;
			for (File rangeFile : rangeORruleFiles){
				DTLoopHelper(rangeFile, rangeORruleFiles.length);
				System.out.println("\n" + "Completed Range File " + counter + " of " + rangeORruleFiles.length + "\n");
				counter++;
			}
			System.out.println("Done!");
			rawDataTextField.setText("Done!");
		}	
		//One last thing - clean all the temp files out of the root folder
		cleanRoot();
	}

	/**
	 * Loop running helper for 1d DT extractions. Note - rangeFile can refer to range OR rule files
	 * @param rangeFile
	 * @param length
	 */
	private void DTLoopHelper(File rangeFile, int length){
		//iterator for the range file number
		File ruleFile = rangeFile; 
		ArrayList<DataVectorInfoObject> allFunctions = new ArrayList<DataVectorInfoObject>();

		String rangePath = "";
		try {
			rangePath = rangeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String rangeFileName = rangeFile.getName();     				
		String csvOutName = rangeFileName + "#";       				

		//Now get the ranges from each range file
		double[] rangesArr = null;
		if (ruleMode){
			// hold off on getting ranges until we have a raw file to read them from
		} else {
			rangesArr = IMExtractRunner.readDataRanges(rangePath);
			System.out.println("Using Range File '" + rangeFileName);
			if (printRanges){
				IMExtractRunner.PrintRanges(rangesArr);
			}
		}
		
		// run imextract to extract a full mobiligram for each selected function in the functions table
		DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
		Vector<?> dataVector = funcsModel.getDataVector();
		IMExtractRunner imextractRunner = IMExtractRunner.getInstance();
		String rawName = rawPaths.get(0);
		String trimmedName = rawName; 

		for( int i=0; i<dataVector.size(); i++ )
		{
			// Get the current data table row and its function information
			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			boolean selected = (boolean)row.get(SELECT_TABLE);
			boolean lockmass = (boolean) row.get(LOCKMASS_TABLE);
			// Catch lockmass when not in lockmass mode:
			if (lockmass){
				System.out.println("not in lockmass mode! Select the lockmass fn 3 check box before using this table option");
				break;
			}
			rawName = (String)row.get(FILENAME_TABLE);
			String rawDataPath = rawPaths.get(i);
			File rawFile = new File(rawDataPath);
			double conecv = (double) row.get(CONECV_TABLE);
			double trapcv = (double)row.get(TRAPCV_TABLE);
			double transfcv = (double) row.get(TRANSFCV_TABLE);
			double wv = (double) row.get(WV_TABLE);
			double wh = (double) row.get(WH_TABLE);
			boolean[] infoTypes = {useConeCV,useTrapCV,useTransfCV,useWavevel,useWaveht,useLockmass};

			if (ruleMode){
				// Call IMExtractRunner to generate a ranges.txt file in the root directory with the full ranges
				IMExtractRunner.getFullDataRanges(rawFile, function);
				// Read the full ranges.txt file generated
				String rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
				rangesArr = IMExtractRunner.readDataRanges(rangeLocation);
			}
			
			// Use the intact complex rangefile's charge state as the folder name, not the raw data's in case of mistakes
			try {
				trimmedName = rawName.substring(0,rawName.lastIndexOf("_"));  
//				if (charge != ""){
//					trimmedName = trimmedName.substring(0,trimmedName.lastIndexOf("_")) + "_" + charge;     						
//				}
			} catch (Exception ex){
				try {
					trimmedName = rawName.substring(0,rawName.lastIndexOf("_"));    
				} catch (Exception ex2){
					trimmedName = rawName;
				}			
			}

			
			// UPDATED - Create the data vector object to hold this info
			if (selected){
				DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawDataPath, rawName,function,selected,lockmass,conecv,trapcv,transfcv,wh,wv,rangesArr,rangeFileName,infoTypes);
				allFunctions.add(functionInfo);
			}
		}
		// Added output directory folder to save files into		
		File outputDir = new File(outputDirectory + File.separator + trimmedName);
		if (!outputDir.exists()){
			outputDir.mkdirs();
		}

		csvOutName = outputDir + File.separator + rangeFileName + "_#" + rawName + "_raw.csv";						

		// UPDATED - Once all function info has been gathered, send it to IMExtract
		imextractRunner.extractMobiligramOneFile(allFunctions,csvOutName, ruleMode, ruleFile);

	}
	
	
	/**
	 * Helper method - runs inner loop (range files) for lockmass method. DOES NOT HANDLE RULE FILES 
	 * AT THIS TIME.
	 * @param rangeFile
	 * @param dataVector
	 * @param correctionFactor
	 * @param allFunctions
	 * @param lockmassflag
	 * @param sampleRawDataPath
	 */
	private void lockmassDTLoopHelper(File rangeFile, Vector<?> dataVector, double correctionFactor, 
			ArrayList<DataVectorInfoObject> allFunctions, boolean lockmassflag, String sampleRawDataPath){
		
		String rangePath = "";
		try {
			rangePath = rangeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String rangeFileName = rangeFile.getName();
	
		//Now get the ranges from each range file
		double[] rangesArr = IMExtractRunner.readDataRanges(rangePath);
		//    					System.out.println("\n" + "Original Ranges from Range File '" + rangeFileName + "' are:");
		//    					IMExtractRunner.PrintRanges(rangesArr);
	
		// Once lockmass is computed, correct each range file for it and analyze data
		// Now apply the correction factor to all the range files before extracting the data
		rangesArr = IMExtractRunner.editDataRanges(rangePath, correctionFactor);
		//    					System.out.println("Corrected Masses from Range File '" + rangeFileName + "' are:");
		//    					IMExtractRunner.PrintMassRanges(rangesArr);
	
	
		// Once lockmass is computed and corrected for, analyze data
		for (int i=0; i<dataVector.size(); i++){
			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			boolean selected = (boolean)row.get(SELECT_TABLE);
			boolean lockmass = (boolean) row.get(LOCKMASS_TABLE);
			String rawName = (String)row.get(FILENAME_TABLE);
			String rawDataPath = rawPaths.get(i);
			double conecv = (double) row.get(CONECV_TABLE);
			double trapcv = (double)row.get(TRAPCV_TABLE);
			double transfcv = (double) row.get(TRANSFCV_TABLE);
			double wv = (double) row.get(WV_TABLE);
			double wh = (double) row.get(WH_TABLE);
			boolean[] infoTypes = {useConeCV,useTrapCV,useTransfCV,useWavevel,useWaveht,useLockmass};
	
			// Only analyze data if we've already computed the lockmass
			if (lockmassflag){
				if (rawDataPath.equals(sampleRawDataPath)){
					// New Version - saves to default output directory
					String trimmedName = rawName;
					try{
						trimmedName = rawName.substring(0,rawName.lastIndexOf("_"));
					} catch(Exception ex){
						// Leave the raw name alone
					}
					File outputDir = new File(outputDirectory + File.separator + trimmedName);
					if (!outputDir.exists()){
						outputDir.mkdirs();
					}
					// CSV name does NOT include range file name for lockmass, since the range files are all inside
					String csvOutName = outputDir + File.separator + rawName + "_raw.csv";						
	
					if( selected )
					{
						// UPDATED - Create the data vector object to hold this info (might need to check selected)
						DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawDataPath, rawName,function,selected,lockmass,conecv,trapcv,transfcv,wh,wv,rangesArr,rangeFileName,infoTypes);
						allFunctions.add(functionInfo);
					}
				}
			}
		}
	}

	/**
	 * Method to extract 1D MS spectra rather than 1DDT files, using the same conventions and setup
	 * as the more common 1D DT file method (runExtactHelper). No support for lockmass (though it
	 * could be implemented later if needed)
	 * @param rangeFiles
	 */
	private void runSpectrumExtractHelper(File[] rangeFiles){
		// No lockmass check, so extract data and whatever information desired by the user
		rawDataTextField.setText("...Analyzing Data (may take a minute)...");
		System.out.println("Analyzing Data (may take some time)");
		//iterator for the range file number
		int rangeFileNumber = 1;

		//get path strings for each file
		for (File rangeFile : rangeFiles){
			spectrumLoopHelper(rangeFile);	
			
			System.out.println("\n" + "Completed Range File " + rangeFileNumber + " of " + rangeFiles.length + "\n");
			rangeFileNumber++;
		}

		System.out.println("Done!");
		rawDataTextField.setText("Done!");

		//One last thing - clean all the temp files out of the root folder
		cleanRoot();
	}
	
//	private void runSpectrumExtractHelper(File[] rangeFiles, File[] ruleFiles){
//		// No lockmass check, so extract data and whatever information desired by the user
//		rawDataTextField.setText("...Analyzing Data (may take a minute)...");
//		//iterator for the range file number
//		int rangeFileNumber = 1;
//
//		String charge = "";
//		try{
//			// Get the charge state of the last range file (highest oligo should be the intact complex)
//			String[] rangesplits = rangeFiles[rangeFiles.length - 1].getName().split("_");
//			charge = rangesplits[1];
//		} catch (Exception ex) {
//			charge = "";
//		}
//
//		//get path strings for each file
//		for (File rangeFile : rangeFiles){
//			// Do each rule file on each range file
//			for (File ruleFile : ruleFiles){
//				runSpectrumLoopHelper(rangeFile, charge, ruleFile);	
//			}
//			System.out.println("\n" + "Completed Range File " + rangeFileNumber + " of " + rangeFiles.length + "\n");
//			rangeFileNumber++;
//		}
//
//		System.out.println("Done!");
//		rawDataTextField.setText("Done!");
//
//		//One last thing - clean all the temp files out of the root folder
//		cleanRoot();
//	}
	/**
	 * Helper method for runSpectrumExtractHelper - runs the inner loop where extraction happens. 
	 * Allows for both rule file and non-rule file extractions
	 * @param rangeFile
	 * @param charge
	 * @param ruleFile
	 */
	private void spectrumLoopHelper(File rangeFile){
		// UPDATED
//		ArrayList<DataVectorInfoObject> allFunctions = new ArrayList<DataVectorInfoObject>();

		String rangePath = null;
		try {
			rangePath = rangeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String rangeFileName = rangeFile.getName();     				
		String csvOutName = rangeFileName + "#";  

		double[] rangesArr = null;
		if (ruleMode){
			// hold off on getting ranges until we have a raw file to read them from
		} else {
			rangesArr = IMExtractRunner.readDataRanges(rangePath);
			System.out.println("Using Range File '" + rangeFileName);
			if (printRanges){
				IMExtractRunner.PrintRanges(rangesArr);
			}
		}

		// run imextract to extract a full mobiligram for each selected function in the functions table
		DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
		Vector<?> dataVector = funcsModel.getDataVector();
		IMExtractRunner imextractRunner = IMExtractRunner.getInstance();

		for( int i=0; i<dataVector.size(); i++ )
		{
			// Get the current data table row and its function information
			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			boolean selected = (boolean)row.get(SELECT_TABLE);
			boolean lockmass = (boolean) row.get(LOCKMASS_TABLE);
			// Catch lockmass when not in lockmass mode:
			if (lockmass){
				System.out.println("not in lockmass mode! Select the lockmass fn 3 check box before using this table option");
				break;
			}
			String rawName = (String)row.get(FILENAME_TABLE);
			String rawDataPath = rawPaths.get(i);
			File rawFile = new File(rawDataPath);
			if (ruleMode){
				// Call IMExtractRunner to generate a ranges.txt file in the root directory with the full ranges
				IMExtractRunner.getFullDataRanges(rawFile, function);
				// Read the full ranges.txt file generated by IMSExtract
				String rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
				rangesArr = IMExtractRunner.readDataRanges(rangeLocation);
			}
			//					double conecv = (double) row.get(CONECV_TABLE);
			//					double trapcv = (double)row.get(TRAPCV_TABLE);
			//					double transfcv = (double) row.get(TRANSFCV_TABLE);
			//					double wv = (double) row.get(WV_TABLE);
			//					double wh = (double) row.get(WH_TABLE);
			//					boolean[] infoTypes = {useConeCV,useTrapCV,useTransfCV,useWavevel,useWaveht,useLockmass};

			// Use the intact complex rangefile's charge state as the folder name, not the raw data's in case of mistakes
			String trimmedName;
			try {
				trimmedName = rawName.substring(0,rawName.lastIndexOf("_"));  
			} catch (Exception ex){
					trimmedName = rawName;			
			}

			// Added output directory folder to save files into		
			File outputDir = new File(outputDirectory + File.separator + trimmedName + "_MS");
			if (!outputDir.exists()){
				outputDir.mkdirs();
			}
			csvOutName = outputDir + File.separator + rangeFileName + "_#" + rawName + "_msraw.csv";						
			// For now, will output single spectrum files, but could use this code to combine them
			//					if (selected){
			//						DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawDataPath, rawName,function,selected,lockmass,conecv,trapcv,transfcv,wh,wv,rangesArr,rangeFileName,infoTypes);
			//						allFunctions.add(functionInfo);
			//					}
			if (selected){
				imextractRunner.extractSpectrum(rawDataPath, function, csvOutName, rangesArr, rangeFileName, rangeFile);
			}
		}
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
                    		String function = numFunctions + ",true" + ",false" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
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
        		String function = numFunctions + ",true" + ",false" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
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
            				String function = numFunctions + ",true" + ",false" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
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
            	String function = numFunctions + ",true" + ",false" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv; 
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField rawDataTextField;
//    private javax.swing.JButton runButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton multiRangeRunButton; //EDIT - added
    private javax.swing.JButton spectrumButton;
    private javax.swing.JCheckBox trapcvCheckBox;
    private javax.swing.JCheckBox transfcvCheckBox;
    private javax.swing.JCheckBox conecvCheckBox;
    private javax.swing.JCheckBox whCheckBox;
    private javax.swing.JCheckBox wvCheckBox;
    private javax.swing.JCheckBox lockmassCheckBox;
    private javax.swing.JPanel checkBoxPanel;
    // End of variables declaration//GEN-END:variables
   
}



/***
 * CUT from line 259, after the end of the datavector.size() for loop. 
 */
// collate all the generated mobiligrams into a composite CIU grid

//String[] columnNames = new String[]{
//  "CE",
//  "1","2","3","4","5","6","7","8","9","10",
//  "11","12","13","14","15","16","17","18","19","20",
//  "21","22","23","24","25","26","27","28","29","30",
//  "31","32","33","34","35","36","37","38","39","40",
//  "41","42","43","44","45","46","47","48","49","50",
//  "51","52","53","54","55","56","57","58","59","60",
//  "61","62","63","64","65","66","67","68","69","70",
//  "71","72","73","74","75","76","77","78","79","80",
//  "81","82","83","84","85","86","87","88","89","90",
//  "91","92","93","94","95","96","97","98","99","100",
//  "101","102","103","104","105","106","107","108","109","110",
//  "111","112","113","114","115","116","117","118","119","120",
//  "121","122","123","124","125","126","127","128","129","130",
//  "131","132","133","134","135","136","137","138","139","140",
//  "141","142","143","144","145","146","147","148","149","150",
//  "151","152","153","154","155","156","157","158","159","160",
//  "161","162","163","164","165","166","167","168","169","170",
//  "171","172","173","174","175","176","177","178","179","180",
//  "181","182","183","184","185","186","187","188","189","190",
//  "191","192","193","194","195","196","197","198","199","200"                
//};
//DefaultTableModel matrixModel = new DefaultTableModel(columnNames, outFiles.size());
//Object[] rowData = new Object[201];
//String[] splits = null;
//for(String csvout : outFiles )
//{
//  try 
//  {
//      BufferedReader reader = new BufferedReader(new FileReader(csvout));
//      String collisionEnergy = reader.readLine();
//      rowData[0] = collisionEnergy;
//      String line = reader.readLine();
//      int i=1;
//      while( line != null )
//      {
//          splits = line.split(",");
//          rowData[i] = splits[1];
//          i++;
//          line = reader.readLine();
//      }
//      matrixModel.addRow(rowData);
//  } 
//  catch (FileNotFoundException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  catch (IOException ex) 
//  {
//      ex.printStackTrace();
//  }
//}


//private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
//	rawDataTextField.setText("...Analyzing Data (may take a minute)...");
//	
//	// Open the user's chosen file
//	try{
//
//		// open file chooser to pick the range files
//		if(singlerangefc.showDialog(this, "OK") == 0)	
//		{
//			File rangeFile = singlerangefc.getSelectedFile();
//
//			//get path strings for each file
//			String rangePath = rangeFile.getCanonicalPath();
//			String rangeFileName = rangeFile.getName();
//
//			//Now get the ranges from each range file
//			double[] rangesArr = IMExtractRunner.readDataRanges(rangePath);
//			System.out.println("Ranges from Range File '" + rangeFileName + "' are:");
//			IMExtractRunner.PrintRanges(rangesArr);
//
//			// run imextract to extract a full mobiligram for each
//			// selected function in the functions table
//			// Get the analyte data
//			DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
//			Vector<?> dataVector = funcsModel.getDataVector();
//			IMExtractRunner imextractRunner = IMExtractRunner.getInstance();
//
//			Vector<String> outFiles = new Vector<String>();
//			File parentFolder = null;
//			for( int i=0; i<dataVector.size(); i++ )
//			{
//				Vector<?> row = (Vector<?>)dataVector.get(i);
//				int function = (int)row.get(0);
//				boolean selected = (boolean)row.get(2);
//				boolean lockmass = (boolean)row.get(3);
//				String rawDataPath = (String)row.get(4);
//
//				File rawFile = new File(rawDataPath);
//				parentFolder = rawFile.getParentFile();
//				String rawFileName = rawFile.getName();
//				
//				// Naming convention - with underscore as delimiter. Date = first 3 splits
//				String[] namesplits = rawFileName.split("_");
//				String date = namesplits[0] + namesplits[1] + namesplits[2];   			   				
//				String rawName = rawFile.getName().substring(0, rawFileName.lastIndexOf(".raw"));
//				String trimmedName = rawName.substring(0,rawName.lastIndexOf("_"));
//				
//				// Added output directory folder to save files into		
//				File outputDir = new File(parentFolder + File.separator + "Output Files");
////				File outputDir = new File(parentFolder + File.separator + "Output Files" + File.separator + date);
//				if (!outputDir.exists()){
//					outputDir.mkdirs();
//				}
//				// ONE FILE AT A TIME ONLY
////				String csvOutName = outputDir + File.separator + function + "_out.csv";	
//				// Multiple files (standard)
//				String csvOutName = outputDir + File.separator + rawName + "_" + function + "_out.csv";				
//				
//				
//				//String csvOutName = parentFolder + File.separator  + rawName + "_" + function +  "_out" + ".csv"; 				
//
//				if( selected )
//				{
//					imextractRunner.extractMobiligram(rawDataPath, function, csvOutName, rangesArr, rangeFileName);
//					outFiles.add(csvOutName);
//
//				}
//			}
//		}
//
//		//One last thing - clean all the temp files out of the root folder
//		cleanRoot();
//
//		System.out.println("\n" + "Done!");
//		rawDataTextField.setText("Done!");
//
//	}
//	catch (FileNotFoundException ex) 
//	{
//		ex.printStackTrace();
//	} 
//	catch (IOException ex) 
//	{
//		ex.printStackTrace();
//	}
//  
//}//GEN-LAST:event_runButtonActionPerformed

//private static Vector<String> getWaveHtFunctionInfo(String rawDataPath)
//{
//  BufferedReader reader = null;
//  ArrayList<Double> waveHts = new ArrayList<Double>();
//  try 
//  {
//      File rawData = new File(rawDataPath, "_extern.inf");
//      reader = new BufferedReader(new FileReader(rawData));
//      String line = reader.readLine();
//      String[] splits = null;
//      while( line != null )
//      {
//      	if( line.startsWith("IMS WAVE HEIGHT (V)"))
//          {
//              splits = line.split("\\t");
//              String strWaveHt = splits[splits.length - 1];
//              Double dWaveHt = new Double(strWaveHt);
//              waveHts.add(dWaveHt);  
//          }
//          line = reader.readLine();
//      }
//      reader.close();    
//  } 
//  catch (FileNotFoundException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  catch (IOException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  finally 
//  {
//      try 
//      {
//          reader.close();
//      } 
//      catch (IOException ex) 
//      {
//          ex.printStackTrace();
//      }
//  }
//  Vector<String> waveHtfunctions = new  Vector<String>();
//  int n=0;
//  for( Double dwaveHt : waveHts )
//  {
//  	/*
//  	 * EDIT - removed the call to add "HE" functions - only adds the ones with usable drift
//  	 * time information and trap CE. If you need all 10 functions, just uncomment the 3
//  	 * commented lines below.
//  	 * EDIT - also defaulted to having the lines selected. If you don't want them to be
//  	 * auto-selected, change the "true" to "false" in the functionLE definition.
//  	 */
//      n++;
//      double d = dwaveHt.doubleValue();
//      String functionLE = n + "," + d + ",true";
//      //n++;
//      //String functionHE = n + "," + d + ",false";
//      waveHtfunctions.add(functionLE);
//      //functions.add(functionHE);
//  }
//  return waveHtfunctions;
//}

//private static Vector<String> getFunctionInfo(String rawDataPath)
//{
//  BufferedReader reader = null;
//  ArrayList<Double> ceValues = new ArrayList<Double>();
//  try 
//  {
//      File rawData = new File(rawDataPath, "_extern.inf");
//      reader = new BufferedReader(new FileReader(rawData));
//      String line = reader.readLine();
//      String[] splits = null;
//      boolean readingFunction = false;
//      while( line != null )
//      {
//          if( line.toUpperCase().startsWith("FUNCTION PARAMETERS") )
//              readingFunction = true;
//          else if( line.startsWith("Trap Collision Energy (eV)") && readingFunction)
//          {
//              splits = line.split("\\t");
//              String strCE = splits[splits.length - 1];
//              Double dCE = new Double(strCE);
//              ceValues.add(dCE);
//              readingFunction = false;
//          }
//          line = reader.readLine();
//      }
//      reader.close();
//      // EDIT - Added catch method for files with only a single function (old files and repeats) (?)
//      if(ceValues.isEmpty()){	//if CEValues is empty, nothing has been read from the file
//      	reader = new BufferedReader(new FileReader(rawData));
//          line = reader.readLine();
//          splits = null;
//          
//          while( line != null ){
//          	if(line.startsWith("Trap Collision Energy")){
//          		splits = line.split("\\t");
//                  String strCE = splits[splits.length - 1];
//                  Double dCE = new Double(strCE);
//                  ceValues.add(dCE);
//          	}
//          	line = reader.readLine();
//          }
//          reader.close();
//      }
//  } 
//  catch (FileNotFoundException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  catch (IOException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  finally 
//  {
//      try 
//      {
//          reader.close();
//      } 
//      catch (IOException ex) 
//      {
//          ex.printStackTrace();
//      }
//  }
//  Vector<String> functions = new  Vector<String>();
//  int n=0;
//  for( Double dce : ceValues )
//  {
//  	/*
//  	 * EDIT - removed the call to add "HE" functions - only adds the ones with usable drift
//  	 * time information and trap CE. If you need all 10 functions, just uncomment the 3
//  	 * commented lines below.
//  	 * EDIT - also defaulted to having the lines selected. If you don't want them to be
//  	 * auto-selected, change the "true" to "false" in the functionLE definition.
//  	 */
//      n++;
//      double d = dce.doubleValue();
//      String functionLE = n + "," + d + ",true";
//      //n++;
//      //String functionHE = n + "," + d + ",false";
//      functions.add(functionLE);
//      //functions.add(functionHE);
//  }
//  return functions;
//}
//

//private static Vector<String> getFunctionInfo(String rawDataPath)
//{
//  BufferedReader reader = null;
//  int numFunctions = 0;
//  
//  ArrayList<Double> fnwaveVels = new ArrayList<Double>();
//  ArrayList<Double> fnwaveHts = new ArrayList<Double>();
//  
//  try 
//  {
//      File rawData = new File(rawDataPath, "_extern.inf");
//      reader = new BufferedReader(new FileReader(rawData));
//      String line = reader.readLine();
//      String[] splits = null;
//      while( line != null )
//      {
//      	// Determine # of fns
//      	if( line.toUpperCase().startsWith("FUNCTION PARAMETERS") ){
//      		splits = line.split("\\t");
//      		numFunctions++;
//      	}          
//      	
//      	line = reader.readLine();
//      }
//      reader.close();
//  } 
//  catch (FileNotFoundException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  catch (IOException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  finally 
//  {
//      try 
//      {
//          reader.close();
//      } 
//      catch (IOException ex) 
//      {
//          ex.printStackTrace();
//      }
//  }
//  Vector<String> functions = new  Vector<String>();
//  int n=0;
//  for( int i = 0; i < numFunctions; i++ )
//  {
//  	// Add an info string for each function - default to selecting only func #1. To select others, change 'false' to 'true'
//      n++;
//      if (n == 1){
//      	String functionOne = n + ",true" + ",false";     
//          functions.add(functionOne);
//      } else if (n == 3){
//      	String functionThree = n + ",false" + ",true";     
//          functions.add(functionThree);
//      }
//      else {
//      	String functionOther = n + ",false" + ",false";     
//          functions.add(functionOther);
//      }
//      
//  }
//  return functions;
//}
//
//// Function info for Sugyan - based on Wave Height and Wave velocity instead of trap CE
//private static Vector<String> getWaveFunctionInfo(String rawDataPath)
//{
//  BufferedReader reader = null;
//  ArrayList<Double> fnwaveVels = new ArrayList<Double>();
//  ArrayList<Double> fnwaveHts = new ArrayList<Double>();
//  try 
//  {
//      File rawData = new File(rawDataPath, "_extern.inf");
//      reader = new BufferedReader(new FileReader(rawData));
//      String line = reader.readLine();
//      String[] splits = null;
//      while( line != null )
//      {
//      	// Set wave velocity
//          if( line.toUpperCase().startsWith("IMS WAVE VELOCITY (M/S)") ){
//          	splits = line.split("\\t");
//              String strWaveVel = splits[splits.length - 1];
//              Double dWaveVel = new Double(strWaveVel);
////              waveVels.add(dWaveVel);  
//          }    
//          if( line.toUpperCase().startsWith("IMS WAVE HEIGHT"))
//          {
//              splits = line.split("\\t");
//              String strWaveHt = splits[splits.length - 1];
//              Double dWaveHt = new Double(strWaveHt);
////              waveHts.add(dWaveHt);  
//          }
//          line = reader.readLine();
//      }
//      reader.close();
//  } 
//  catch (FileNotFoundException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  catch (IOException ex) 
//  {
//      ex.printStackTrace();
//  } 
//  finally 
//  {
//      try 
//      {
//          reader.close();
//      } 
//      catch (IOException ex) 
//      {
//          ex.printStackTrace();
//      }
//  }
//  Vector<String> wavefunctions = new  Vector<String>();
//  int n=0;
//  for( Double dwaveVel : waveVels )
//  {
//  	// Method assumes same number of wave vels and hts - should always be true instrumentally    
//      n++;
//      double waveVel = dwaveVel.doubleValue();
//      for ( Double dwaveHt : waveHts){
//      	//n++;
//      	double waveHt = dwaveHt.doubleValue();
//      	String functionLE = n + "," + waveVel + "," + waveHt + ",true";     
//          wavefunctions.add(functionLE);
//      }
//      
//  }
//  return wavefunctions;
//}
//
//


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

////Old - from function info
//reader = new BufferedReader(new FileReader(rawData));
//String line = reader.readLine();
//String[] splits = null;
//boolean readingFunction = false;
//while( line != null )
//{
//	// Determine # of fns
//  if( line.toUpperCase().startsWith("FUNCTION PARAMETERS") ){
//  	splits = line.split("\\t");
//  	readingFunction = true;
//      numFunctions++;
//  }       
//  // Determine WH, WV, Trap, Transfer, and cone
//  else if( line.toUpperCase().startsWith("IMS WAVE VELOCITY (M/S)") ){
//  	splits = line.split("\\t");
//  	String strWaveVel = splits[splits.length - 1];
//  	Double dWaveVel = new Double(strWaveVel);
//  	fnwaveVels.add(dWaveVel);  
//  } else if( line.toUpperCase().startsWith("IMS WAVE HEIGHT")){
//  	splits = line.split("\\t");
//  	String strWaveHt = splits[splits.length - 1];
//  	Double dWaveHt = new Double(strWaveHt);
//  	fnwaveHts.add(dWaveHt);  
//  } else if( line.startsWith("Trap Collision Energy (eV)") && readingFunction){
//      splits = line.split("\\t");
//      String strCE = splits[splits.length - 1];
//      Double dCE = new Double(strCE);
//      trapCvVals.add(dCE);
//      readingFunction = false;
//  } else if (line.startsWith("Collision Energy (eV)") && readingFunction){
//  	// Then this is a G1 method - read the appropriate line for G1 files
//  	splits = line.split("\\t");
//      String strCE = splits[splits.length - 1];
//      Double dCE = new Double(strCE);
//      trapCvVals.add(dCE);
//      readingFunction = false;
//  }
//  line = reader.readLine();
//}
//int n=0;
//for( int i = 0; i < numFunctions; i++ )
//{
//	// Add an info string for each function - default to selecting only func #1. To select others, change 'false' to 'true'
//  n++;
//  // Get info for each function
//  double trapCV = 0;
//  double wh = 0;
//  double wv = 0;
//  try {
//  	trapCV = trapCvVals.get(i);  
//  } catch (IndexOutOfBoundsException ex){
//  	System.out.println("no trapCV found");
//  }
//  try {
//  	wh = fnwaveHts.get(i);
//  } catch (IndexOutOfBoundsException ex){
//  	System.out.println("no wave height found");
//  }
//  try {
//  	wv = fnwaveVels.get(i);
//  } catch (IndexOutOfBoundsException ex){
//  	System.out.println("no wave velocity found");
//  }
//  if (n == 1){
//  	String functionOne = n + ",true" + ",false" + "," + trapCV + "," + wh + "," + wv;     
//      functions.add(functionOne);
//  } else if (n == 3){
//  	String functionThree = n + ",false" + ",true" + "," + trapCV + "," + wh + "," + wv;     
//      functions.add(functionThree);
//  }
//  else {
//  	String functionOther = n + ",false" + ",false" + "," + trapCV + "," + wh + "," + wv;     
//      functions.add(functionOther);
//  }
//  System.out.println(functions.get(i));
//}