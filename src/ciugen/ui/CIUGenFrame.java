
package ciugen.ui;

import ciugen.imextract.DataVectorInfoObject;
import ciugen.imextract.IMExtractRunner;
import ciugen.preferences.Preferences;
import ciugen.ui.utils.RawFileFilter;
import ciugen.ui.utils.RuleFileFilter;
import ciugen.ui.utils.TextFileFilter;
import ciugen.ui.Options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

/**
 * This file is part of TWIMExtract 
 * 
 * TWIMExtract is a general tool for extracting 1 dimensional retention time (RT), drift time (DT) and mass spectral
 * (MZ) datasets from Waters' .raw to text format. This CIUGenFrame object provides user interface and 
 * handles calls to IMExtractRunner and utilities for extracting data using the IMSExtract.exe 
 * executable from Waters with appropriate settings. Please refer to the user manual for more information.
 * 
 * License information: (BSD)
   Copyright 2016 Daniel Polasky

	Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
	
	1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
	
	2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
	
	3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
	THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
	GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
	OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * IF YOU USE TWIMEXTRACT, PLEASE CITE: Haynes, S. E.; Polasky, D. A.; Dixit, S. M.; Majmudar, J. D.; Neeson, K.; Ruotolo, B. T.; Martin, B. R. 
 * "Variable-Velocity Traveling-Wave Ion Mobility Separation Enhancing Peak Capacity for Data-Independent 
 * Acquisition Proteomics". Anal. Chem. 2017, acs.analchem.7b00112.
 * 
 * 
 * @author Daniel Polasky 
 * @author Keiran Neeson
 * @version TWIMExtract v1.4
 *
 *
 */
public class CIUGenFrame extends javax.swing.JFrame {	
	
	private static final String TITLE = "TWIMExtract v1.4";

	
	private static void print_help(){
		System.out.println("**********TWIMExtract 1.4 help*********** \n"
				+ "If you use TWIMExtract, please cite: Haynes, S.E., Polasky D. A., Majmudar, J. D., Dixit, S. M., Ruotolo, B. T., Martin, B. R. \n"
				+ "'Variable-velocity traveling-wave ion mobility separation enhances peak capacity for data-independent acquisition proteomics'. Manuscript in preparation \n"
				+ "*****************************************\n"
				+ "The following inputs are required: \n"
				+ "-i INPUT: complete system path to the raw data file of interest \n"
				+ "-o OUTPUT: directory in which to place the output file(s) \n"
				+ "-m MODE: 0=RT, 1=DT, 2=MZ. Which dimension to preserve when collapsing data \n"
				+ "Optional arguments: \n"
				+ "-f FUNCTION: the function number to extract. If not supplied, default is to extract all functions \n"
				+ "-r RANGE: full system path to the range (or rule) file specifying ranges to extract. If not specified, the full ranges available in the file will be used \n"
				+ "-rulemode RULEMODE: true or false. Must be true if using a rule file instead of a range file. Default: false \n"
				+ "-combinemode COMBINEMODE: true or false. If true, multiple outputs from the same raw file (e.g. multiple functions) will be combined into one output file. Default: false \n"
				+ "-ms DT_EXTRACTMODE: true or false. DT extractions will be saved in milliseconds if true and bins if false. Default = true");
	}
	
	private static Options parse_args(String args[]){
		String input_path = null;
		String output_path = null;
		int extract_mode = -1;
		int parsed_func = -1;
		String range_path = null;
		Boolean rule_mode = false;
		Boolean combine_mode = false;
		String ext_string = null;
		Boolean extract_in_ms_args = true;
		String func_string = "";

		// Parse args
		for(int count=0; count < args.length; count++){
			// If args[count] matchs a flag, the following arg contains the value for that flag
			if(args[count].equals("-i")) input_path = args[++count];
			if(args[count].equals("-o")) output_path = args[++count];
			if(args[count].equals("-m")) ext_string = args[++count];
			if(args[count].equals("-r")) range_path = args[++count];
			if(args[count].equals("-f")) func_string = args[++count];
			if(args[count].equals("-rulemode")) rule_mode = Boolean.parseBoolean(args[++count]);
			if(args[count].equals("-combinemode")) combine_mode = Boolean.parseBoolean(args[++count]);
			if(args[count].equals("-ms")) extract_in_ms_args = Boolean.parseBoolean(args[++count]);
			if(args[count].equals("-h")){
				print_help();
				System.exit(0);
			}

			if (count % 2 == 0){
				// Even numbered args must start with '-' for the flag
				if (! args[count].startsWith("-")){
					System.out.println("Invalid arg syntax: " + args[count] + "\n Args must start with '-");
				}
			}
		}

		// Parse non-strings and handle exceptions
		try{
			extract_mode = Integer.parseInt(ext_string.trim());
		} catch (NumberFormatException ex){
			System.out.println("Invalid mode entered. Must enter 0 (RT), 1 (DT), or 2 (MZ) for mode");
			System.exit(1);
		}
		try{
			parsed_func = Integer.parseInt(func_string);
		} catch (NullPointerException ex){
			// No function passed, do nothing (-1 default value will be used to indicate reading all functions)
		} catch (NumberFormatException ex2){
			// No function passed, do nothing (-1 default value will be used to indicate reading all functions)
//			System.out.println("No function int entered, Reading all functions instead");
		}


		// Make sure all required arguments are present
		if (input_path == null || output_path == null || extract_mode == -1){
			System.out.println("Not all required arguments passed! Must have -i, -o, and -m. See -h for help");
			System.exit(1);
		}
		// Set range to 'FULL' if it is currently null, to specify passing the entire range available
		if (range_path == null){
			range_path = "FULL";
		}

		Options options = new Options(input_path, output_path, range_path, extract_mode, parsed_func, rule_mode, combine_mode, extract_in_ms_args);
//		options.print_options();
		return options;
	}
	
//	private static Options parse_args_old(String args[]){
//		// Combine arg array, since we are parsing on '>' characters instead of spaces (to allow spaces in filenames)
//		String arg_string = "";
//		for (String arg : args){
//			arg_string = arg_string + arg + " ";
//		}
//		String[] arg_splits = arg_string.split(">");
//		
//		String input_path = null;
//		String output_path = null;
//		int extract_mode = -1;
//		int parsed_func = -1;
//		String range_path = null;
//		Boolean rule_mode = false;
//		Boolean combine_mode = false;
//		String ext_string = null;
//		String func_string = "";
//		
//		// Parse args
//		for(int count=0; count < arg_splits.length; count++){
//			// If args[count] matchs a flag, the following entry contains the value for that flag
//			String[] inner_splits = arg_splits[count].split("<");
//			if(arg_splits[count].startsWith("i<")) input_path = inner_splits[1].trim();
//			if(arg_splits[count].startsWith("o<")) output_path = inner_splits[1].trim();
//			if(arg_splits[count].startsWith("m<")) ext_string = inner_splits[1].trim();
//			if(arg_splits[count].startsWith("f<")) func_string = inner_splits[1].trim();
//			if(arg_splits[count].startsWith("r<")) range_path = inner_splits[1].trim();
//			if(arg_splits[count].startsWith("rulemode<")) rule_mode = Boolean.parseBoolean(inner_splits[1].trim());
//			if(arg_splits[count].startsWith("combinemode<")) combine_mode = Boolean.parseBoolean(inner_splits[1].trim());
//			if(arg_splits[count].startsWith("h")){
//				print_help();
//				System.exit(0);
//			}
//		}
//		// Parse non-strings and handle exceptions
//		try{
//			extract_mode = Integer.parseInt(ext_string.trim());
//		} catch (NumberFormatException ex){
//			System.out.println("Invalid mode entered. Must enter 0 (RT), 1 (DT), or 2 (MZ) for mode");
//			System.exit(1);
//		}
//		try{
//			parsed_func = Integer.parseInt(func_string);
//		} catch (NullPointerException ex){
//			// No function passed, do nothing (-1 default value will be used to indicate reading all functions)
//		} catch (NumberFormatException ex2){
//			System.out.println("Invalid function entered. Must be an integer. Reading all functions instead");
//		}
//		
//		
//		// Make sure all required arguments are present
//		if (input_path == null || output_path == null || extract_mode == -1){
//			System.out.println("Not all required arguments passed! Must have -i, -o, and -m. See -h for help");
//			System.exit(1);
//		}
//		// Set range to 'FULL' if it is currently null, to specify passing the entire range available
//		if (range_path == null){
//			range_path = "FULL";
//		}
//		
//		Options options = new Options(input_path, output_path, range_path, extract_mode, parsed_func, rule_mode, combine_mode);
////		options.print_options();
//		return options;
//	}
	
	/*
	 * Method to run extractor from command line. Roughly duplicates the 'combinedLoopHelper'
	 * method, but couldn't be easily combined due to the structure of the GUI (which uses
	 * the actual GUI elements to store information, and thus can't be used outside the GUI). 
	 * Have plans to fix eventually.
	 */
	private static void run_command_args(String[] args){
		// Parse args, returning options object containing extract information
		Options arg_opts = parse_args(args);

		// Get basic file and range information
//		File rawFile = new File(arg_opts.input);
		
		// initialize extractor
		IMExtractRunner imextractRunner = IMExtractRunner.getInstance();
		ArrayList<DataVectorInfoObject> allfuncs = new ArrayList<DataVectorInfoObject>();
		
		String rawFilePaths = arg_opts.input;
		String[] splits = rawFilePaths.split(",");
		for (String rawFilePath : splits){
			
			File rawFile = new File(rawFilePath);
			String rawPath = null;
			try { rawPath = rawFile.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not find file " + rawFile.toString() + " Please check the file and try again");
				System.exit(1);
			}
			String rawName = rawFile.getName();
			String rangeName = "FULL";
			try{
				File rangeFile = new File(arg_opts.range);
				rangeName = rangeFile.getName();
			} catch (NullPointerException ex){}
	
			// Get necessary function info for the given raw path
			Vector<String> functions = getAllFunctionInfo(rawPath);
	
			// Use default infotypes for now - might add options for them later
			// types: coneCV, trapCV, transfCV, WV, WH
			boolean[] infoTypes = {false, true , false, false, false};

			
			// Read desired function OR all functions if no func specified into a DataVector
			if (arg_opts.function == -1){
				// No function specified; read all functions
				for(String function : functions ){
					DataVectorInfoObject functionInfo = makeFunctionInfo(function, arg_opts, rawFile, rawPath,
							rawName, rangeName, infoTypes);
					allfuncs.add(functionInfo);
				}
			} else {
				// Read only specified function
				String desired_func = functions.get(arg_opts.function - 1);
				DataVectorInfoObject functionInfo = makeFunctionInfo(desired_func, arg_opts, rawFile, rawPath,
						rawName, rangeName, infoTypes);
				allfuncs.add(functionInfo);
			}
		}
		
		// Get mode name
		String extr_mode_name = "";
		if (arg_opts.mode == IMExtractRunner.DT_MODE){
			extr_mode_name = "DT";
		} else if(arg_opts.mode == IMExtractRunner.MZ_MODE){
			extr_mode_name = "MZ";
		} else if(arg_opts.mode == IMExtractRunner.RT_MODE){
			extr_mode_name = "RT";
		}

		// All info has been gathered, so run the extractor
		if (! arg_opts.combineMode){
			// Pass a new list with only one function's info to the extractor
			for (DataVectorInfoObject functionInfo : allfuncs){
				ArrayList<DataVectorInfoObject> singleFunctionVector = new ArrayList<DataVectorInfoObject>();
				singleFunctionVector.add(functionInfo);

				// Make output directory folder to save files into if needed		
//				File outputDir = new File(arg_opts.output + File.separator + rawName);
				File outputDir = new File(arg_opts.output);
				if (!outputDir.exists()){
					outputDir.mkdirs();
				}
				String rawName = singleFunctionVector.get(0).getRawDataName();
				String rangeName = singleFunctionVector.get(0).getRangeName();
				String csvOutName = outputDir + File.separator + extr_mode_name +  "_" + rawName + "_fn-" + functionInfo.getFunction() + "_#" + rangeName + "_raw.csv";						

				// Prep rule file if in rule mode:
				File ruleFile = null;
				if (arg_opts.ruleMode){
					ruleFile = new File(arg_opts.range);
				}

				// Call the extractor
				imextractRunner.extractMobiligramOneFile(singleFunctionVector, csvOutName, arg_opts.ruleMode, ruleFile, arg_opts.mode, arg_opts.extract_in_ms);
			} 

		} else {
			// Combined mode, so pass all info at once
//			File outputDir = new File(arg_opts.output + File.separator + rawName);
			File outputDir = new File(arg_opts.output);
			if (!outputDir.exists()){
				outputDir.mkdirs();
			}
			String rawName = allfuncs.get(0).getRawDataName();
			String rangeName = allfuncs.get(0).getRangeName();
			String csvOutName = outputDir + File.separator + extr_mode_name +  "_" + rawName  +  "_#" + rangeName  + "_raw.csv";						

			// Prep rule file if in rule mode:
			File ruleFile = null;
			if (arg_opts.ruleMode){
				ruleFile = new File(arg_opts.range);
			}

			// Call the extractor
			imextractRunner.extractMobiligramOneFile(allfuncs, csvOutName, arg_opts.ruleMode, ruleFile, arg_opts.mode, arg_opts.extract_in_ms);

		}
		cleanRoot();
		System.out.println("Done!");
	}
	
	/*
	 * Helper method to assemble all function information into a DataVectorInfoObject. Handles
	 * both specified and unspecified range files from argument Options
	 */
	private static DataVectorInfoObject makeFunctionInfo(String function, Options arg_opts, File rawFile, String rawPath, 
			String rawName, String rangeName, boolean[] infoTypes){
		String[] splits = function.split(",");

		// Read range info from file, or get full ranges from raw file if no range specified
		double[] rangesArr = new double[9];
		if (arg_opts.range == "FULL"){
			// No range file was specified, so use the full data ranges available for this function
			IMExtractRunner.getFullDataRanges(rawFile, Integer.parseInt(splits[FN_SPLITS]));
			String rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
			rangesArr = IMExtractRunner.readDataRangesOld(rangeLocation);
		} else {
			// Read the specified range file
			rangesArr = IMExtractRunner.readDataRanges(arg_opts.range, rangesArr);
		}

		DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawPath, rawName, 
				Integer.parseInt(splits[FN_SPLITS]),
				Boolean.parseBoolean(splits[SELECTED_SPLITS]),Double.parseDouble(splits[CONECV_SPLITS]),
				Double.parseDouble(splits[TRAPCV_SPLITS]), Double.parseDouble(splits[TRANSFCV_SPLITS]),
				Double.parseDouble(splits[WH_SPLITS]),Double.parseDouble(splits[WV_SPLITS]),
				rangesArr,rangeName,infoTypes,
				Double.parseDouble(splits[FN_START_SPLITS]));
		
		return functionInfo;
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		
		// Command line args: if present, parse and run. Otherwise, run the GUI
		if (! (args.length == 0)){
			run_command_args(args);

		} else {
			// no command line args, so run the GUI
			
			/* Set the Nimbus look and feel */
			//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
			/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
			 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
			 */
			startTime = System.nanoTime();
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

	}


	/**
	 * Creates new form CIUGenFrame GUI
	 */
	public CIUGenFrame() 
	{
		rawPaths = new ArrayList<String>();
		fnStarts = new ArrayList<Double>();

		initGUIComponents();
		initFileChoosers();

		tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));     
	}


	/**
	 * Initialize primary GUI components
	 */    
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
		combineModeTextField = new javax.swing.JTextField("       Yes       ");

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
//		jLabel2.setText("<html>Choose an Extraction Mode to run:<br>To extract DT in ms, see options menu</html>");
		jLabel2.setText("Choose an Extraction Mode to run:");
		jPanel3.add(jLabel2, java.awt.BorderLayout.WEST);

		// initialize the various extract data (run) buttons
		initRunButtons();
		runPanel.add(runButton_MZ);
		runPanel.add(runButton_DT);
		runPanel.add(runButton_RT);
		jPanel3.add(runPanel, java.awt.BorderLayout.EAST);

		// Initialize check boxes
		initCheckBoxes();

		jPanel3.add(checkBoxPanel, java.awt.BorderLayout.SOUTH);

		getContentPane().add(jPanel2, java.awt.BorderLayout.WEST);
		getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);
		getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

		pack();

		if (verboseMode){
			initComps = System.nanoTime();
			System.out.println("init comps time: " + (initComps - startTime)/1000000);	
		}

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
		advancedMenu = new JMenu("Advanced");
		advancedMenu.setMnemonic(KeyEvent.VK_A);
		advancedMenu.getAccessibleContext().setAccessibleDescription("Advanced options");
		menuBar.add(advancedMenu);
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_A);
		helpMenu.getAccessibleContext().setAccessibleDescription("Help menu");
		menuBar.add(helpMenu);

		// File menu items
		rawDirItem = new JMenuItem("Set the default directory for browsing .raw data");
		rawDirItem.addActionListener(menuActionListener);
		rangeDirItem = new JMenuItem("Set the default directory to select Range files");
		rangeDirItem.addActionListener(menuActionListener);
		outDirItem = new JMenuItem("Select Output Directory");
		outDirItem.addActionListener(menuActionListener);
		ruleDirItem = new JMenuItem("Set the default directory to select Rule files");
		ruleDirItem.addActionListener(menuActionListener);
		fileMenu.add(outDirItem);  	
		fileMenu.add(rawDirItem);
		fileMenu.add(rangeDirItem);
		fileMenu.add(ruleDirItem);

		// Batch menu items
		runBatchItem = new JMenuItem("Select csv batch file and run batch");
		runBatchItem.addActionListener(menuActionListener);
		batchDirItem = new JMenuItem("Select default directory to look for batch files");
		batchDirItem.addActionListener(menuActionListener);
		batchMenu.add(batchDirItem);   	
		batchMenu.add(runBatchItem);

		// Option menu items
		printRangeOptionItem = new JMenuItem("Toggle verbose output mode");
		printRangeOptionItem.addActionListener(menuActionListener);
		toggleRulesItem = new JMenuItem("Toggle using Rule or Range files");
		toggleRulesItem.addActionListener(menuActionListener);
		toggleCombineItem = new JMenuItem("Toggle Combined or Individual outputs");
		toggleCombineItem.addActionListener(menuActionListener);
		toggleCombineRawItem = new JMenuItem("Toggle combining outputs by rawfile (combines all functions). Supersedes regular combined outputs");
		toggleCombineRawItem.addActionListener(menuActionListener);
		dtBinModeItem = new JMenuItem("Toggle DT extraction in ms/bins");
		dtBinModeItem.addActionListener(menuActionListener);
		optionMenu.add(toggleRulesItem);
		optionMenu.add(toggleCombineItem);
		optionMenu.add(toggleCombineRawItem);
		optionMenu.add(dtBinModeItem);
		optionMenu.add(printRangeOptionItem);
		
		// advanced menu items
		fastModeItem = new JMenuItem("Toggle standard (fast) or careful range file mode");
		fastModeItem.setToolTipText("ONLY needed if your functions have significantly varying numbers of bins");
		fastModeItem.addActionListener(menuActionListener);
		legacyRangeItem = new JMenuItem("Toggle standard or legacy range files");
		legacyRangeItem.addActionListener(menuActionListener);
		toggleUnderscoreItem = new JMenuItem("Toggle removing final underscore in individual rawfile combine");
		toggleUnderscoreItem.addActionListener(menuActionListener);
		advancedMenu.add(fastModeItem);
		advancedMenu.add(legacyRangeItem);
		advancedMenu.add(toggleUnderscoreItem);

		// Help menu items
		helpItem = new JMenuItem("Open help file");
		helpItem.addActionListener(menuActionListener);
		helpMenu.add(helpItem);
		exampleItem = new JMenuItem("Open range file example");
		exampleItem.addActionListener(menuActionListener);
		helpMenu.add(exampleItem);
		ruleHelpItem = new JMenuItem("Open rule file help");
		ruleHelpItem.addActionListener(menuActionListener);
		helpMenu.add(ruleHelpItem);
		aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(menuActionListener);
		helpMenu.add(aboutItem);

	}


	/**
	 * Initialize filechoosers for raw and range files using preferences information
	 */
	private void initFileChoosers(){
		// browse data filechooser
		fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);
		fc.setDialogTitle("Select the raw data file(s) to analyze");
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
		rangefc.setDialogTitle("Select the RANGE (.txt) file(s) to use for data extraction");
		rangefc.setFileSelectionMode(2);              
		rangefc.setCurrentDirectory(rdir);
		rangefc.setMultiSelectionEnabled(true);
		rangefc.setFileFilter(new FileNameExtensionFilter("Text files only (.txt)", "txt"));

		batchfc = new JFileChooser();
		batchfc.setDialogTitle("Select the batch csv file to use");
		batchfc.setFileSelectionMode(JFileChooser.FILES_ONLY);              
		batchfc.setCurrentDirectory(bdir);
		batchfc.setMultiSelectionEnabled(false);
		batchfc.setFileFilter(new FileNameExtensionFilter("CSV files only (.csv)", "csv"));

		rulefc = new JFileChooser();
		rulefc.setDialogTitle("Select the RULE (.rul) file(s) to use for data extraction");
		rulefc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		rulefc.setCurrentDirectory(ruledir);
		rulefc.setMultiSelectionEnabled(true);
		rulefc.setFileFilter(new FileNameExtensionFilter("rul files only (.rul)","rul"));

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
	 * Listener class for menu items
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
					fc.setCurrentDirectory(directory);
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
					rangefc.setCurrentDirectory(directory);
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
					batchfc.setCurrentDirectory(directory);
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
					rulefc.setCurrentDirectory(directory);
				}
			} else if (e.getSource() == runBatchItem){
				// Run the batch from the user's selected file
				runBatchCSV();

			} else if (e.getSource() == printRangeOptionItem){
				// Change (toggle) the range and time printing to terminal
				if (verboseMode){
					verboseMode = false;
					System.out.println("Verbose mode has been turned OFF");
				} else {
					verboseMode = true;
					System.out.println("Verbose mode has been turned ON");
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

			} else if (e.getSource() == toggleCombineItem){
				// Toggle (switch) the combined outputs mode flag
				if (combine_outputs){
					combine_outputs = false;
					statusTextBar.setText("Now using individual outputs mode");
					combineModeTextField.setText("        No    ");
				} else {
					combine_outputs = true;
					statusTextBar.setText("Now using combined output mode");
					combineModeTextField.setText("        Yes    ");
				}			
				
			} else if (e.getSource() == toggleCombineRawItem){
				// Toggle (switch) the combined outputs mode flag
				if (combine_outputs_by_rawname){
					combine_outputs_by_rawname = false;
					statusTextBar.setText("Not combining by raw file");
					combineModeTextField.setText("Not by Raw");
				} else {
					combine_outputs_by_rawname = true;
					statusTextBar.setText("Now combining by raw file");
					combineModeTextField.setText("By Raw");
				}	 
			
			} else if (e.getSource() == dtBinModeItem){
				// Toggle between saving extraction information in ms or bins
				if (extract_in_ms){
					extract_in_ms = false;
					statusTextBar.setText("DT will be saved in bins");
				} else {
					extract_in_ms = true;
					statusTextBar.setText("DT will be saved in milliseconds (ms)");
				}
				
			} else if (e.getSource() == fastModeItem){
				if (fastMode){
					fastMode = false;
					statusTextBar.setText("Careful/slower mode enabled");
				} else {
					fastMode = true;
					statusTextBar.setText("Standard (fast) mode enabled");
				}
			} else if (e.getSource() == legacyRangeItem){
				if (newRangefileMode){
					newRangefileMode = false;
					statusTextBar.setText("Now using LEGACY range files (9 fields)");
				} else {
					newRangefileMode = true;
					statusTextBar.setText("Now using standard range files (6 fields)");
				}
			} else if (e.getSource() == toggleUnderscoreItem){
				// Toggle (switch) the combined outputs mode flag
				if (trimFinalUnderscore){
					trimFinalUnderscore = false;
					statusTextBar.setText("Not not trimming final underscore");
				} else {
					trimFinalUnderscore = true;
					statusTextBar.setText("Now trimming final underscore");
				}	 
			}

			else if (e.getSource() == helpItem){
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

			} else if (e.getSource() == ruleHelpItem){
				// Open the rule help file to show the user how to make rule files
				ProcessBuilder exampleRunner = new ProcessBuilder("notepad.exe", preferences.getRULE_EX_PATH());
				try {
					exampleRunner.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			} else if (e.getSource() == aboutItem){
				// Open the 'about' information tab with version, author, etc. Arbitrarily using JPanel1 as the parent component.
				JOptionPane.showMessageDialog(jPanel1_top, "*** TWIMExtract v1.0 *** \n"
						+ "Please cite: Haynes, S.E., Polasky D. A., Majmudar, J. D., Dixit, S. M., Ruotolo, B. T., Martin, B. R. "
						+ "\n Variable-velocity traveling-wave ion mobility separation enhances peak capacity for "
						+ "\n data-independent acquisition proteomics. Manuscript in preparation");
			}
		}

	}

	// Determine which run button the user clicked and run the appropriate extraction method
	public class runButtonActions implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == runButton_DT) {
				runExtractorButton(e, IMExtractRunner.DT_MODE);
			} else if (e.getSource() == runButton_MZ){
				runExtractorButton(e, IMExtractRunner.MZ_MODE);
			} else if (e.getSource() == runButton_RT){
				runExtractorButton(e, IMExtractRunner.RT_MODE);
			} 
		}
	}

	/**
	 * Opens filechooser for the user to choose the raw files they'd like to extract, then loads those
	 * files into the function table using the getAllFunctionInfo parsing method. 
	 * @param evt
	 */
	private void browseDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDataButtonActionPerformed
		// Clear the old raw data paths out of memory, if applicable
		rawPaths.clear();
		fnStarts.clear();

		// First, make sure the user has chosen an output directory, and prompt them to choose one if not
		if (! preferences.haveConfig){
			// No config file! Prompt the user to select default directories and stop (return)
			warnConfig();
			return;
		} 
		
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
			String browsedDir = rawFiles[0].getParent();
			preferences.setRawDir(browsedDir);
			preferences.writeConfig();
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
			fnStarts.clear();
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
					fnStarts.add(Double.parseDouble(splits[FN_START_SPLITS]));
					
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
		// Make sure there is data in the table before running
		if (functionsTable.getModel().getRowCount() == 0) {
			JOptionPane.showMessageDialog(statusTextBar, "No data selected for analysis. \n"
					+ "Please use the 'Browse data' button to select data for extraction.");
			// Exit the extraction
			return;
		}

		statusTextBar.setText("...Analyzing Data (may take a minute)...");
		System.out.println("Analyzing data (may take some time)");
		
		if (ruleMode){
			// Choose rule files for spectrum if in rule mode, or run extractor without if not
			if(rulefc.showDialog(this,"Select these rule files and extract") == 0){
				File[] ruleFiles = rulefc.getSelectedFiles();
				runExtraction(ruleFiles, extractionMode);
				// update directory
				String browsedDir = ruleFiles[0].getParent();
				preferences.setRuleDir(browsedDir);
				preferences.writeConfig();
			}   
		} else {
			// open file chooser to pick the range files
			if(rangefc.showDialog(this, "Select these range files and extract") == 0)	
			{
				File[] rangeFiles = rangefc.getSelectedFiles();
				runExtraction(rangeFiles, extractionMode);
				// update directory
				String browsedDir = rangeFiles[0].getParent();
				preferences.setRangeDir(browsedDir);
				preferences.writeConfig();
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
		extrStart = System.nanoTime();

		ArrayList<DataVectorInfoObject> allFunctions = new ArrayList<DataVectorInfoObject>();
		String rangePath = "";
		try {
			rangePath = rangeFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// File output naming information
		String rangeFileName = rangeFile.getName();     				
		String csvOutName = rangeFileName + "#";     
		String extr_mode_name = "";
		if (extraction_mode == IMExtractRunner.DT_MODE){
			extr_mode_name = "DT";
		} else if(extraction_mode == IMExtractRunner.MZ_MODE){
			extr_mode_name = "MZ";
		} else if(extraction_mode == IMExtractRunner.RT_MODE){
			extr_mode_name = "RT";
		}

		// run imextract to extract a full mobiligram for each selected function in the functions table
		DefaultTableModel funcsModel = (DefaultTableModel)functionsTable.getModel();
		Vector<?> dataVector = funcsModel.getDataVector();
		IMExtractRunner imextractRunner = IMExtractRunner.getInstance();
		String rawName = rawPaths.get(0);
		String trimmedName = rawName; 

		//Now get the ranges from each range file. Initialize bin numbers FROM RAW FILE info
		double[] rangesArr = new double[9];
		double fileStartTime = 0.0;		// marker for actual file starting time, used for adjusting range files
		String rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
		if (newRangefileMode){
			oldRangesBefore = System.nanoTime();
			// Call IMExtractRunner to generate a ranges.txt file in the root directory with the full ranges
			IMExtractRunner.getFullDataRanges(new File(rawPaths.get(0)), 1);
			// Read the full ranges.txt file generated
			rangesArr = IMExtractRunner.readDataRangesOld(rangeLocation);
			fileStartTime = rangesArr[IMExtractRunner.START_RT];
			if (verboseMode)
				System.out.println("full data ranges time: " + (System.nanoTime() - oldRangesBefore)/1000000);
		} 

		for( int i=0; i<dataVector.size(); i++ ) {
			long extrStartLoop = System.nanoTime();

			// Get the current data table row and its function information
			Vector<?> row = (Vector<?>)dataVector.get(i);
			int function = (int)row.get(FN_TABLE);  
			boolean selected = (boolean)row.get(SELECT_TABLE);
			rawName = (String)row.get(FILENAME_TABLE);
			trimmedName = rawName;
			String rawDataPath = rawPaths.get(i);
			double fnStart = fnStarts.get(i);
			
			File rawFile = new File(rawDataPath);
			double conecv = (double) row.get(CONECV_TABLE);
			double trapcv = (double)row.get(TRAPCV_TABLE);
			double transfcv = (double) row.get(TRANSFCV_TABLE);
			double wv = (double) row.get(WV_TABLE);
			double wh = (double) row.get(WH_TABLE);
			boolean[] infoTypes = {useConeCV,useTrapCV,useTransfCV,useWavevel,useWaveht};

			// Careful mode: fast mode assumes same bin size for each function. Careful mode recalculates each time. 
			if (! fastMode){
				IMExtractRunner.getFullDataRanges(rawFile, function);
				System.out.println("full data ranges time: " + (System.nanoTime() - oldRangesBefore)/1000000);

				rangeLocation = preferences.getROOT_PATH() + File.separator + "ranges.txt";
				rangesArr = IMExtractRunner.readDataRangesOld(rangeLocation);
				System.out.println("full data ranges + read them time: " + (System.nanoTime() - oldRangesBefore)/1000000);
			}

			if (! ruleMode){
				if (newRangefileMode){
					rangesArr = IMExtractRunner.readDataRanges(rangePath, rangesArr);
				} else {
					rangesArr = IMExtractRunner.readDataRangesOld(rangePath);
				}
			}
			if (verboseMode){
				IMExtractRunner.PrintRanges(rangesArr);
			}

			// Single file output mode: create the output file and call the extractor inside the loop
			if (selected){
				// adjust range values to account for function start time in multi-function data files
				double[] adjRangeVals = adjustRangeVals(fnStart, rangesArr, fileStartTime);
				DataVectorInfoObject functionInfo = new DataVectorInfoObject(rawDataPath, rawName,function,selected,conecv,trapcv,transfcv,wh,wv,adjRangeVals,rangeFileName,infoTypes, fnStart);
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
					csvOutName = outputDir + File.separator + extr_mode_name +  "_" + rawName + "_fn-" + functionInfo.getFunction() + "_#" + rangeFileName + "_raw.csv";						

					// Call the extractor
					imextractRunner.extractMobiligramOneFile(singleFunctionVector, csvOutName, ruleMode, rangeFile, extraction_mode, extract_in_ms);

					if (verboseMode){
						extrEnd = System.nanoTime();
						System.out.println("that extr time: " + (extrEnd - extrStartLoop)/1000000);
					}
				}
			}
		}
		// combine outputs by individual raw files mode
		if (combine_outputs_by_rawname){
			
			ArrayList<ArrayList<DataVectorInfoObject>> sortedFuncs = sortFuncsByFile(allFunctions);
			for (ArrayList<DataVectorInfoObject> rawfuncs : sortedFuncs){
				rawName = rawfuncs.get(0).getRawDataName();
				
				// Make output directory folder to save files into if needed		
				File outputDir = new File(outputDirectory + File.separator + rawName);
				if (!outputDir.exists()){
					outputDir.mkdirs();
				}
				csvOutName = outputDir + File.separator + extr_mode_name +  "_" + rawName  +  "_#" + rangeFileName  + "_raw.csv";						

				//Once all function info has been gathered, send it to IMExtract
				imextractRunner.extractMobiligramOneFile(rawfuncs, csvOutName, ruleMode, rangeFile, extraction_mode, extract_in_ms);
				if (verboseMode){
					extrEnd = System.nanoTime();
					System.out.println("that extr time: " + (extrEnd - extrStart)/1000000);
				}
			}
		} 
		
		// Combine outputs mode: make directories and call extractor after loop is finished
		else if (combine_outputs)
		{
			// Make output directory folder to save files into if needed		
			File outputDir = new File(outputDirectory + File.separator + trimmedName);
			if (!outputDir.exists()){
				outputDir.mkdirs();
			}
			csvOutName = outputDir + File.separator + extr_mode_name +  "_" + trimmedName  +  "_#" + rangeFileName  + "_raw.csv";						

			//Once all function info has been gathered, send it to IMExtract
			imextractRunner.extractMobiligramOneFile(allFunctions, csvOutName, ruleMode, rangeFile, extraction_mode, extract_in_ms);
			if (verboseMode){
				extrEnd = System.nanoTime();
				System.out.println("that extr time: " + (extrEnd - extrStart)/1000000);
			}
		}

	}
	
	/**
	 * Sort an input list of functions by rawname so that all data from one raw file is grouped together.
	 * @param allfuncs
	 * @return
	 */
	private ArrayList<ArrayList<DataVectorInfoObject>> sortFuncsByFile(ArrayList<DataVectorInfoObject> allfuncs){
		ArrayList<ArrayList<DataVectorInfoObject>> sortedFuncs = new ArrayList<ArrayList<DataVectorInfoObject>>();
		
		// Create a Map (dictionary) to hold found rawnames and lists of functions with them
		HashMap<String, ArrayList<DataVectorInfoObject>> rawNameLists = new HashMap<String, ArrayList<DataVectorInfoObject>>();
		
		for (DataVectorInfoObject func: allfuncs){
			String rawname = func.getRawDataName();
			
			if (trimFinalUnderscore){
				String[] splits = rawname.split("_");
				String newRawname = "";
				for (int i=0; i < splits.length - 1; i++){
					newRawname = newRawname + splits[i];
				}
				rawname = newRawname;
			}
			
			if (rawNameLists.containsKey(rawname)){
				// This raw file is already present; add this function to the associated list
				ArrayList<DataVectorInfoObject> currentList = rawNameLists.get(rawname);
				currentList.add(func);
				rawNameLists.put(rawname, currentList);
			}
			else 
			{
				// Raw file not yet present - create a new list for it
				ArrayList<DataVectorInfoObject> currentList = new ArrayList<DataVectorInfoObject>();
				currentList.add(func);
				rawNameLists.put(rawname, currentList);
			}
		}
		
		// Once all files have been sorted, return the sorted lists
		sortedFuncs = new ArrayList<ArrayList<DataVectorInfoObject>>(rawNameLists.values());
		
		return sortedFuncs;
	}

	/**
	 * Adjust range values to account for function start time
	 * @param functionStartTime: start time of the function (minutes)
	 * @param ranges: initial (unadjusted) ranges array
	 * @param fileStartTime: start time of the overall file, read from fullDataRanges. Used to offset all start times
	 */
	private double[] adjustRangeVals(double functionStartTime, double[] rangeVals, double fileStartTime){
		double newStartTime = functionStartTime + rangeVals[IMExtractRunner.START_RT] + fileStartTime;
		double newEndTime = functionStartTime + rangeVals[IMExtractRunner.STOP_RT] + fileStartTime;
		
		double[] newRanges = new double[9];
		for (int i = 0; i < rangeVals.length; i++){
			newRanges[i] = rangeVals[i];
		}
		newRanges[IMExtractRunner.START_RT] = newStartTime;
		newRanges[IMExtractRunner.STOP_RT] = newEndTime;
		return newRanges;
	}
	
	
	/**
	 * Method opens a filechooser for the user to select their desired batch csv file, then uses
	 * the runBatch method to execute the batch run. 
	 * NOTE: All files will be extracted with the same settings, and all settings (checkboxes/etc)
	 * must be selected before starting the batch
	 */
	private void runBatchCSV(){
		// First, get mode arguments (range/rule, mz/dt/rt, and combined/individual) by making a popup
		Object[] modeOptions = {"RT", "DT", "MZ"};
		int extractionMode = JOptionPane.showOptionDialog(jPanel2, 
				"Please select the extraction mode for this batch:"
						+ "\n NOTE: the range/rule and combine outputs modes \n "
						+ "on the main interface will be used for the batch", 
						"BATCH MODE", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, 
						null, modeOptions, modeOptions[1]);

		// Prompt the user to select the csv containing the batch file information
		if (batchfc.showDialog(this, "OK") == 0){
			// Counters for printing status
			int batchCounter = 1;
			int linecounter = 0;

			try {			
				// User has chosen a csv file containing the desired lists
				File csvFile = batchfc.getSelectedFile();
				
				// update directory
				String browsedDir = csvFile.getParent();
				preferences.setBatchDir(browsedDir);
				preferences.writeConfig();
				
				// Do an initial read to get the number of lines present
				BufferedReader quickreader = new BufferedReader(new FileReader(csvFile));
				String quickline = quickreader.readLine();
				while (quickline != null){
					// ignore header lines marked with '#'
					if (! quickline.startsWith("#"))
						linecounter++;
					quickline = quickreader.readLine();
				}
				quickreader.close();

				// Read the file
				BufferedReader reader = new BufferedReader(new FileReader(csvFile));

				// loop through the file and analyze each line as a separate extraction
				String line = reader.readLine();
				while ( line != null){
					// Ignore header lines marked with '#'
					if (! line.startsWith("#")){
						String[] splits = line.split(",");
						String rawfolderpath = splits[0];
						String rangefolderpath = splits[1];

						// **************** Get the associated files into an array to pass to the extractor **********************
						File rawTopFolder = new File(rawfolderpath);
						File[] rawFiles = rawTopFolder.listFiles(new RawFileFilter());

						// **************** Once files are ready, get the associated ranges to pass to the extractor *****************
						File rangeFileTopFolder = new File(rangefolderpath);
						File[] rangeFiles = null;
						if (ruleMode){
							rangeFiles = rangeFileTopFolder.listFiles(new RuleFileFilter());
						} else {
							rangeFiles = rangeFileTopFolder.listFiles(new TextFileFilter());
						}

						// ******** Once both raw and range files are ready, extract the data! *******
						extractBatchData(rawFiles, rangeFiles, extractionMode);
						System.out.println("*******************************************************");
						System.out.println("Done with Batch line " + batchCounter + " of " + linecounter);
						System.out.println("*******************************************************");
						batchCounter++;
					}
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
	public void extractBatchData(File[] rawFiles, File[] rangeFiles, int extractionMode){
		// First, clear the current data table in case there's anything in it
		DefaultTableModel tblModel = (DefaultTableModel)functionsTable.getModel();
		int rowCount = tblModel.getRowCount();
		for( int i=0; i<rowCount; i++ ){
			tblModel.removeRow(0);
		}

		// Load the data into the table model and update it
		openBrowsedData(rawFiles, tblModel);

		// Run the extraction on the raw data with the specified mode and range or rule files
		runExtraction(rangeFiles, extractionMode);
	}

	/*
	 * Removes all temporary binary files in the root directory once the program has finished.
	 */
	public static void cleanRoot()
	{
		String rootPath = preferences.getROOT_PATH();
		File rootDir = new File(rootPath);
		File[] allFiles = rootDir.listFiles();
		
		// Only delete IMSExtract binary files (.1dDT, .1dMZ, and .1dRT)
		for( File f : allFiles )
			if (RawFileFilter.acceptdDT(f) || RawFileFilter.acceptMZ(f) || RawFileFilter.acceptRT(f)){
				f.delete();
			}

	}

	/**
	 * GUI method to prompt the user for config information (default input and output directories)
	 * when they try to use the tool without a config file (e.g. first time use). Only the output
	 * directory is required (otherwise outputs will be saved to default directory, which can
	 * be hard to find sometimes). 
	 */
	private void warnConfig(){
		// Show a dialog informing the user that no config files have been generated
		JOptionPane.showMessageDialog(browseDataButton, "Warning: No output directory selected. \n Please"
				+ " use 'File/Select Output Directory' to choose where your extracted data \n will be saved before running"
				+ " the extractor. Thank you!");
	}

	/**
	 * Determination of function information for Waters raw data files. NOTE: Different Waters
	 * instruments record their information with VARYING NAMES, meaning instrument type needs to
	 * be determined before function info can be gathered. 
	 * NOTE: Only intentionally set up to work for Synapt HDMS (aka G1) and G2. Seems to
	 * work fine for G2-S, has not been tested for G2-Si (for now)
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
			
			double fnStartTime = -1.0;

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
							String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv + "," + fnStartTime; 
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
					} if (line.startsWith("Start Time (mins)") && reachedFunctions){
						splits = line.split("\\t");
						String stTime = splits[splits.length - 1];
						fnStartTime = new Double(stTime);
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
				String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv + "," + fnStartTime; 
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
							String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv + "," + fnStartTime; 
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
					} if (line.startsWith("Start Time (mins)") && reachedFunctions){
						splits = line.split("\\t");
						String stTime = splits[splits.length - 1];
						fnStartTime = new Double(stTime);
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
				String function = numFunctions + ",true" + "," + coneCV + "," + trapCV + "," + transfCV + "," + wh + "," + wv + "," + fnStartTime; 
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

	// timing check globals
	private static long startTime;
	private long initComps;
	private long extrStart;
	private long extrEnd;
	private long oldRangesBefore;

	private static final long serialVersionUID = -1971838044338723234L;

	// File choosers and directories
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
	private boolean extract_in_ms = true;	// save information in bins if true, ms if false
	private boolean verboseMode = false;
	private boolean ruleMode = false;
	private boolean combine_outputs = true;
	private boolean combine_outputs_by_rawname = false;
	private boolean trimFinalUnderscore = false;
	private boolean newRangefileMode = true;	// if false, allows legacy format range files (9 fields) to be used instead of new range files (6 fields)
	private boolean fastMode = true;	// Determines how often to check the # of bins (fast = at the start of a new raw file, otherwise it's done for each function)

	// global file information
	private ArrayList<String> rawPaths;
	private ArrayList<Double> fnStarts;

	// String locations for function strings from getAllFunctionInfo
	private static final int TRAPCV_SPLITS = 3;
	private static final int WH_SPLITS = 5;
	private static final int WV_SPLITS = 6;
	private static final int TRANSFCV_SPLITS = 4;
	private static final int CONECV_SPLITS = 2;
	private static final int SELECTED_SPLITS  = 1;
	private static final int FN_SPLITS = 0;
	private static final int FN_START_SPLITS = 7;

	// String locations for table model (includes filename) - has to be in this order because that's the order it displays in the GUI
	private static final int FN_TABLE = 0;
	private static final int FILENAME_TABLE = 1;
	private static final int SELECT_TABLE = 2;
	private static final int CONECV_TABLE = 3;
	private static final int TRAPCV_TABLE = 4;
	private static final int TRANSFCV_TABLE = 5;
	private static final int WH_TABLE = 6;
	private static final int WV_TABLE = 7;

	// Menu components - new
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu batchMenu;
	private JMenu optionMenu;
	private JMenu advancedMenu;
	private JMenu helpMenu;
	private JMenuItem helpItem;
	private JMenuItem exampleItem;
	private JMenuItem ruleHelpItem;
	private JMenuItem aboutItem;
	private JMenuItem rawDirItem;
	private JMenuItem rangeDirItem;
	private JMenuItem outDirItem;
	private JMenuItem runBatchItem;
	private JMenuItem batchDirItem;
	private JMenuItem ruleDirItem;
	private JMenuItem printRangeOptionItem;
	private JMenuItem toggleRulesItem;
	private JMenuItem toggleCombineItem;
	private JMenuItem toggleCombineRawItem;
	private JMenuItem toggleUnderscoreItem;
	private JMenuItem dtBinModeItem;
	private JMenuItem fastModeItem;
	private JMenuItem legacyRangeItem;
	private MenuActions menuActionListener = new MenuActions();
	private runButtonActions runButtonActionListener = new runButtonActions();

	// Variables declaration - original
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
	private javax.swing.JCheckBox trapcvCheckBox;
	private javax.swing.JCheckBox transfcvCheckBox;
	private javax.swing.JCheckBox conecvCheckBox;
	private javax.swing.JCheckBox whCheckBox;
	private javax.swing.JCheckBox wvCheckBox;
	private javax.swing.JPanel checkBoxPanel;
	// End of variables declaration

}