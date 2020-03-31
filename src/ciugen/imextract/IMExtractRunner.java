/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ciugen.imextract;

import ciugen.preferences.Preferences;
import ciugen.utils.NumberUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * This file is part of TWIMExtract
 * 
 * This object is a utility class for making calls to the imextract.exe in order to extract
 * specific ranges of HDMS data
 * @author Daniel Polasky
 * @author Kieran Neeson 
 */
public class IMExtractRunner {
	
    // The single instance of this object
    private static IMExtractRunner instance;
    
    // Single instance of the preferences
    private static Preferences preferences = Preferences.getInstance();

    // This is the root folder for the current analysis
    private static File root;

    /**
     * Private constructor
     */
    private IMExtractRunner()
    {
    	exeFile = new File(preferences.getLIB_PATH() + File.separator + "imextract.exe");
    	setRoot( preferences.getCIUGEN_HOME() + "\\root");
    }

	/**
     * Returns the single instance of this object
     * @return - singleton IMExtractRunner
     */
    public static IMExtractRunner getInstance()
    {
        if( instance == null )
        {
            instance = new IMExtractRunner();
        }
        return instance;
    }

    /**
     * Runs imextract.exe to determine the full RT, DT & MZ data ranges from the specified data
     * Generates 2 output files:
     * _rt.bin - the binary file containing the chromatogram map for scans to mins
     * _dt.bin - the binary file containing the mobiligram map for bins to millisecs
     * _hdc.bin - the hdc calibration binary file - not sure what this does but it is not relevant to HDMSCompare
     * ranges.txt - the text file with all the initial ranges for the data 
     * @param rawFile - raw file to process
     * @param nFunction - data function to process
     */
    public static void getFullDataRanges(File rawFile, int nFunction)
    {
        try {
            StringBuilder cmdarray = new StringBuilder();
            
            cmdarray.append(exeFile.getCanonicalPath() + " ");
            cmdarray.append("-d ");
            cmdarray.append("\"" + rawFile.getPath() + "\" ");
            cmdarray.append("-f " + nFunction + " ");
            cmdarray.append("-o ");
            cmdarray.append("\"" + getRoot() + File.separator + "ranges.txt\" ");
            cmdarray.append("-t ");
            cmdarray.append("mobilicube");
            
            runIMSExtract(cmdarray.toString());
//            initialiseBinaryMaps();
            
        } catch (IOException ex) {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Read input range file and return an array of the ranges specified.
     * NOTE: max # of bins for each dimension is automatically determined by IMExtractRunner, 
     * and will be set to 1 for the ranges array.
     * RANGE FILE FORMAT:
     * 	MZ_start_(m/z): xxx
		MZ_end_(m/z): xxxx
		RT_start_(minutes): xx
		RT_end_(minutes): xx
		DT_start_(bins): xx
		DT_end_(bins): xxx
     */
    public static double[] readDataRanges(String rangesName, double[] rangesArr){
    	// Data reader
        BufferedReader reader = null;
        String line = null;
        
        // Read the file
        try {
        	File rangesTxt = new File(rangesName);
        	reader = new BufferedReader(new FileReader(rangesTxt));
        	while((line = reader.readLine()) != null){
        		// Skip lines beginning with '#'
        		if (line.startsWith("#")){
        			// do nothing, it's a header
        			continue;
        		}
        		
        		String[] splits = line.split(":");
        		String inputName = splits[0];
        		double inputValue = Double.parseDouble(splits[1]);

        		String[] nameSplits = inputName.split("_");
        		switch(nameSplits[0]){
        		case "MZ":
        			if (nameSplits[1].toLowerCase().matches("start")){
        				minMZ = inputValue;
        				rangesArr[0] = inputValue;
        			} else if (nameSplits[1].toLowerCase().matches("end")){
        				maxMZ = inputValue;
        				rangesArr[1] = inputValue;
        			} else {
        				// Invalid input name
        			}
        			break;    
        		case "RT":
        			if (nameSplits[1].toLowerCase().matches("start")){
        				minRT = inputValue;
        				rangesArr[3] = inputValue;
        			} else if (nameSplits[1].toLowerCase().matches("end")){
        				maxRT = inputValue;
        				rangesArr[4] = inputValue;
        			} else {
        				// Invalid input name
        			}
        			break;

        		case "DT":
        			if (nameSplits[1].toLowerCase().matches("start")){
        				minDT = inputValue;
        				rangesArr[6] = inputValue;
        			} else if (nameSplits[1].toLowerCase().matches("end")){
        				maxDT = inputValue;
        				rangesArr[7] = inputValue;
        			} else {
        				// Invalid input name
        			}
        			break;

        		}
        	}

        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException iox)
        {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, iox);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return rangesArr;
    }

	/**
	 * Read the root/ce.dat file and return the CV information. ce.dat file has individual CV values, 1 per line
	 * and no other information
	 * @return CV list
	 */
	private static ArrayList<Double> getCVfromCEdat() {
    	ArrayList<Double> cvData = new ArrayList<>();
    	File ceFile = new File(preferences.getROOT_PATH() + "\\_ce.dat");
    	BufferedReader reader;
    	try {
    		reader = new BufferedReader(new FileReader(ceFile));
    		String line;
    		while ((line = reader.readLine()) != null){
    			double cv = Double.parseDouble(line);
    			cvData.add(cv);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cvData;
	}

    /**
     * Reads the data ranges from the specified file
     * FOR OLD RANGE FILE FORMAT (updated 11/30/16)
     * @param rangesFilePath: the path to the ranges text file
     * @return - an array of the ranges specified in the ranges file
     */
    public static double[] readDataRangesOld(String rangesName)
    {
        // Data reader
        BufferedReader reader = null;
        String line = null;
        // array to store data values
        double[] rangesArr = new double[9];
        // value counter
        int valueCounter = 0;

        try
        {
           
            //File rangesTxt = new File(getRoot() + File.separator + rangesName);
        	File rangesTxt = new File(rangesName);
            reader = new BufferedReader(new FileReader(rangesTxt));
            while((line = reader.readLine()) != null) {
            	
                String[] splits = line.split(" ");
                for( String split : splits ){
                    double d = Double.parseDouble(split);
                    //rangesArr[valueCounter] = d;
                    switch( valueCounter ){
                        case START_MZ:
                        	minMZ = d;
                        	rangesArr[valueCounter] = minMZ;
                            break;
                            
                        case STOP_MZ:
                        	maxMZ = d;
                        	rangesArr[valueCounter] = maxMZ;
                            break;
                            
                        case MZ_BINS:
                            mzBins = d;
                            rangesArr[valueCounter] = mzBins;
                            break;
                            
                        case START_RT:
                            minRT = d;
                            rangesArr[valueCounter] = minRT;
                            break;
                            
                        case STOP_RT:
                            maxRT = d;
                            rangesArr[valueCounter] = maxRT;
                            break;
                            
                        case RT_BINS:
                            rtBins = d;
                            rangesArr[valueCounter] = rtBins;
                            break;
                            
                        case START_DT:
                            minDT = Math.floor(d);
                            rangesArr[valueCounter] = minDT;
                            break;
                            
                        case STOP_DT:
                            maxDT = Math.ceil(d);
                            rangesArr[valueCounter] = maxDT;
                            break;
                                                        
                        case DT_BINS:
                            dtBins = d;
                            rangesArr[valueCounter] = dtBins;
                            break;
                                    
                    }
                    valueCounter++;
                }
            }
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException iox)
        {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, iox);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }      
        return rangesArr;
    }
 
    /**
     * Print specified ranges
     * @param rangeArr: the range array to be printed
     */
    public static void PrintRanges(double[] rangeArr){
    	// Print the ranges on 3 lines, as they're typically arranged that way in the text file.
    	for(int i=0; i<rangeArr.length;i++){
    		System.out.print(rangeArr[i] + " ");
    		if (i==2 || i==5 || i==rangeArr.length){
    			System.out.println();
    		}
    	}
    }
    
    /**
	 * Returns an array of the last data ranges run
	 * @return 
	 */
	public double[] getLastRanges() {
		double[] ranges = new double[]{minMZ, maxMZ, mzBins, minRT, maxRT, rtBins, minDT, maxDT, dtBins};
		return ranges;
	}

	/**
	 * Gets the root directory
	 * @return
	 */
	private static File getRoot(){
		return root;
	}

	/**
	 * Sets the root directory
	 * @param path 
	 */
	private static void setRoot(String path){
		//System.out.println("Setting root: " + path);
		root = new File(path);
		root.mkdirs();
	}

	/**
	 * Updated extract Mobiligram method that takes a list of functions to analyze (length 1 for single
	 * file analyses), calls the appropriate helper methods based on the extraction mode, then combines
	 * the returned (extracted) data for writing to an output file specified by the output path. 
	 * @param allFunctions = the list of functions (data) to be extracted with all their associated information in DataVectorInfoObject format
	 * @param outputFilePath = where to write the output file
	 * @param ruleMode = whether to use range files or rule files for extracting
	 * @param ruleFile = the rule OR range file being used for the extraction
	 * @param extractionMode = the type of extraction to be done (DT, MZ, RT, or RTDT)
	 */
	public void extractMobiligramOneFile(ArrayList<DataVectorInfoObject> allFunctions, String outputFilePath, boolean ruleMode, File ruleFile, int extractionMode, boolean dt_in_ms){
		String lineSep = System.getProperty("line.separator");

		// Get info types to print from first function (they will be the same for all functions)
		boolean[] infoTypes = allFunctions.get(0).getInfoTypes();
		try {
			// Get data
			ArrayList<MobData> allMobData = extractMobiligramReturn(allFunctions, ruleMode, ruleFile, extractionMode, dt_in_ms);

			// Now, write the output file
			File out = new File(outputFilePath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(out));

			// Get the formatted text output for the appropriate extraction type (RT has to be handled differently from others)
			String[] arraylines = null;
			if (extractionMode == RT_MODE){
				arraylines = rtWriteOutputs(allMobData, infoTypes);
			} else {
				double maxdt = 200; 	// if extracting in bins, maxdt = max bin
				if (extractionMode == DT_MODE){
					if (dt_in_ms){
						// compute max DT using max m/z info from _extern.inf file
						maxdt = get_max_dt(allFunctions.get(0).getRawDataPath());
					}
				}
				arraylines = dtmzWriteOutputs(allMobData, infoTypes, maxdt);
			}

			// Now, write all the lines to file
			for (String line : arraylines){
				writer.write(line);
				writer.write(lineSep);
			}
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException ex) 
		{
			ex.printStackTrace();
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
		}

	}
	
	public void writeExtractSave(ExtractSave saveObj){
		String lineSep = System.getProperty("line.separator");

		// Get info types to print from first function (they will be the same for all functions)
		boolean[] infoTypes = saveObj.getReferenceFunction().getInfoTypes();
		
		// Now, write the output file
		File outFile = new File(saveObj.getOutputFilePath());
		ArrayList<MobData> allMobData = saveObj.getMobData();

		// Get the formatted text output for the appropriate extraction type (RT has to be handled differently from others)
		String[] arraylines = null;
		if (saveObj.getExtractionMode() == RT_MODE){
			arraylines = rtWriteOutputs(allMobData, infoTypes);
		} else {
			double maxdt = 200; 	// if extracting in bins, maxdt = max bin
			if (saveObj.getExtractionMode() == DT_MODE || saveObj.getExtractionMode() == RTDT_MODE){
				if (saveObj.isDT_in_MS()){
					// compute max DT using max m/z info from _extern.inf file
					maxdt = get_max_dt(saveObj.getReferenceFunction().getRawDataPath());
				}
			}
			arraylines = dtmzWriteOutputs(allMobData, infoTypes, maxdt);
		}

		// Now, write all the lines to file
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			for (String line : arraylines){
				writer.write(line);
				writer.write(lineSep);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Extract Mobiligram method for CIU (2D RTDT) extractions. Takes a list of functions to analyze to convenient running with old setup,
	 * but ONLY uses the first function. Calls the new helper methods to perform a SINGLE 2D RTDT extraction and returns
	 * mobData as in the 1D extraction method.
	 * the generated mobdata for output as appropriate.
	 * @param allFunctions = the list of functions (data) to be extracted with all their associated information in DataVectorInfoObject format
	 * @param outputFilePath = where to write the output file
	 * @param ruleMode = whether to use range files or rule files for extracting
	 * @param ruleFile = the rule OR range file being used for the extraction
	 * @param extraction_mode = the type of extraction to be done (DT, MZ, RT, or DTMZ)
	 */
	public ArrayList<MobData> generateMobiligram2D(DataVectorInfoObject function, boolean ruleMode, File rangeFile, int extractionMode, boolean dt_in_ms){
		// Collect mobData for all functions in the list
		ArrayList<MobData> allMobData = new ArrayList<>();

		if (extractionMode != RTDT_MODE){
			System.out.println("ERROR: 2D extraction requested for non-2D mode. Returning no data");
			return allMobData;
		}
//		DataVectorInfoObject function = allFunctions.get(0);
		double maxDT = get_max_dt(function.getRawDataPath());
		allMobData = generateReplicateRTDT(function, rangeFile, ruleMode, dt_in_ms, maxDT);

		return allMobData;
	}
	/**
	 * Updated extract Mobiligram method that takes a list of functions to analyze (length 1 for single
	 * file analyses), calls the appropriate helper methods based on the extraction mode, then returns
	 * the generated mobdata for output as appropriate. 
	 * @param allFunctions = the list of functions (data) to be extracted with all their associated information in DataVectorInfoObject format
	 * @param outputFilePath = where to write the output file
	 * @param ruleMode = whether to use range files or rule files for extracting
	 * @param ruleFile = the rule OR range file being used for the extraction
	 * @param extraction_mode = the type of extraction to be done (DT, MZ, RT, or DTMZ)
	 */
	public ArrayList<MobData> extractMobiligramReturn(ArrayList<DataVectorInfoObject> allFunctions, boolean ruleMode, File rangeFile, int extractionMode, boolean dt_in_ms){
		// Collect mobData for all functions in the list
		ArrayList<MobData> allMobData = new ArrayList<MobData>();
		if (extractionMode == RTDT_MODE){
			for (DataVectorInfoObject function : allFunctions) {
				// TODO: fix or remove
				allMobData = generateMobiligram2D(function, ruleMode, rangeFile, extractionMode, dt_in_ms);
			}
		} else {
			for (DataVectorInfoObject function : allFunctions) {
				String rawDataFilePath = function.getRawDataPath();
				String rawName = function.getRawDataName();

				int functionNum = function.getFunction();
				double conecv = function.getConeCV();
				double trapcv = function.getCollisionEnergy();
				double transfcv = function.getTransfCV();
				double wh = function.getWaveHeight();
				double wv = function.getWaveVel();
				double[] rangeVals = function.getRangeVals();
				String rangeName = function.getRangeName();

				double[][] data = null;
				try {
					if (extractionMode == DT_MODE) {
						data = generateReplicateMobiligram(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, rangeFile, ruleMode);

					} else if (extractionMode == MZ_MODE) {
						data = generateReplicateSpectrum(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, rangeFile, ruleMode);

					} else if (extractionMode == RT_MODE) {
						data = generateReplicateChromatogram(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, rangeFile, ruleMode);

					} else if (extractionMode == DTMZ_MODE) {
						//    				data = generateReplicateDTMZ(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile, ruleMode);
					}
				} catch (FileNotFoundException ex) {
					ex.printStackTrace();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				if (data == null) {
					System.out.println("Error during extraction! Check your raw data - it might be empty or corrupted");
				}

				MobData currentMob = new MobData(data, rawName, rangeName, conecv, trapcv, transfcv, wh, wv);
				allMobData.add(currentMob);
			}
		}
		return allMobData;
	}
	
	/**
	 * Method to manually find the maximum drift time of a file using the max m/z defined in
	 * the acquisition mass range of the file's _extern.inf file. ONLY tested for Synapt G2 so far.
	 * @param rawDataPath
	 * @return max drift time (double)
	 */
	private double get_max_dt(String rawDataPath){
		double max_dt = 0.0;
		double max_mz = 0.0;
		boolean mob_delay = false;
		double delay_time = 0.0;
		
		try {
			
			// read the file
			File rawData = new File(rawDataPath, "_extern.inf");
			BufferedReader reader = new BufferedReader(new FileReader(rawData));
			String line = reader.readLine();
			while (line != null){
				// MS mode
				if (line.toUpperCase().startsWith("END MASS")){
					String[] splits = line.split("\\t");
					String strmz = splits[splits.length - 1];
					max_mz = new Double(strmz);
				}
				// MSMS mode
				if (line.toUpperCase().startsWith("MSMS END MASS")){
					String[] splits = line.split("\\t");
					String strmz = splits[splits.length - 1];
					max_mz = new Double(strmz);
				}
				// check for mobility delays
				if (line.startsWith("Using Mobility Delay after Trap Release")){
					String[] splits = line.split("\\t");
					String strDelay = splits[splits.length - 1];
					mob_delay = Boolean.parseBoolean(strDelay);
				}
				// check for mobility delays
				if (line.startsWith("IMS Wave Delay")){
					String[] splits = line.split("\\t");
					String strDelayTime = splits[splits.length - 1];
					delay_time = Double.parseDouble(strDelayTime);
					// convert to ms. NOTE: dividing by 10,000 because I think the units are incorrect in MassLynx (this gives the correct max DT)
					delay_time = delay_time / 10000.0;
				}
				
				line = reader.readLine();
			}
			
			// convert max m/z to max DT and return it. Account for delay if it was used
			if (mob_delay){
				max_dt = convert_mzdt_max(max_mz, delay_time);
			} else {
				max_dt = convert_mzdt_max(max_mz, 0.0);
			}

			reader.close();
		}	

		catch (IOException ex){
			
		}
		return max_dt;
	}
	
	/**
	 * Convert from maxmium m/z to max drift time for synapt G2 using Waters built-in cutoffs. Accounts
	 * for mobility trapping delay times. 
	 * @param maxMZ
	 * @return max drift time (double)
	 */
	private double convert_mzdt_max(double maxMZ, double delay_time){
		double dtmax = 0;
		if (maxMZ <= 600){
			dtmax = 7.61;
		} else if (maxMZ <= 1200){
			dtmax = 10.8;
		} else if (maxMZ <= 2000){
			dtmax = 13.78;
		} else if (maxMZ <= 5000){
			dtmax = 21.940;
		} else if (maxMZ <= 8000){
			dtmax = 27.513;
		} else if (maxMZ <= 14000){
			dtmax = 36.268;
		} else if (maxMZ <= 32000){
			dtmax = 54.580;
		} else {
			dtmax = 96.743;
		}
		dtmax = dtmax - delay_time;
		return dtmax;
	}
	
	/**
	 * Method to change the DT information of the first MobData array ONLY in a list of mobdata.
	 * Converts to DT using information from file's _extern.inf. 
	 * @param allmobdata arraylist of mobdata containers
	 * @return updated mobdata
	 */
	private ArrayList<MobData> convert_mobdata_to_ms(ArrayList<MobData> allmobdata, double maxDT){		
		// Convert each bin to drift time ((bin - 1) * max_dt / 199)
		// NOTE: For some reason, there are actually only 199 bins, not 200. Doing the conversion by
		// dividing by 199 gives results that match the output from Driftscope/MassLynx. Bin 1 is set
		// to a drift time of 0 (millisecond DTs are 0-indexed, whereas bin numbers are 1-indexed, so all
		// bins have 1 subtracted from them to be converted correctly)
		for (int i=0; i < allmobdata.get(0).getMobdata().length; i++){
			allmobdata.get(0).getMobdata()[i][0] = convertBinToDT(allmobdata.get(0).getMobdata()[i][0], maxDT);
		}
		return allmobdata;
	}

	/**
	 * Convert bin to DT (ms). NOTE: For some reason, there are actually only 199 bins, not 200. Doing the conversion by
	 * dividing by 199 gives results that match the output from Driftscope/MassLynx. Bin 1 is set
	 * to a drift time of 0 (millisecond DTs are 0-indexed, whereas bin numbers are 1-indexed, so all
	 * bins have 1 subtracted from them to be converted correctly)
	 * @param inputBin starting bin number
	 * @param maxDT max DT of the acquisition (ms)
	 * @return DT in ms
	 */
	private static double convertBinToDT(double inputBin, double maxDT) {
		return (inputBin - 1) * maxDT / 199;
	}
	
	/**
	 * Helper method to format text output for MS or DT extractions. Assumes that each function 
	 * (if using combined outputs) has the same bin names (e.g. DT bin 1, 2, 3, ...) and writes
	 * one column per function using the same initial set of bins. Returns String[] that can
	 * be directly written to the output file. 
	 * @param allMobData
	 * @param infoTypes
	 * @return
	 */
	private String[] dtmzWriteOutputs(ArrayList<MobData> allMobData, boolean[] infoTypes, double maxdt){  	
		ArrayList<String> lines = new ArrayList<String>();
		
		// Headers
		// Loop through the list of data, writing each function's value for this CE to the line, and sorting
		int HEADER_LENGTH = 2;
		lines.add("# Range file name:");
		lines.add("# Raw file name:");
		if (infoTypes[USECONE_TYPES]){
			lines.add("$ConeCV:"); 
			HEADER_LENGTH++;
		}
		if (infoTypes[USETRAP_TYPES]){
			lines.add("$TrapCV:"); 
			HEADER_LENGTH++;
			Collections.sort(allMobData, new Comparator<MobData>(){
				public int compare(MobData d1, MobData d2){
					return (int) d1.getTrapCV() - (int) d2.getTrapCV();
				}
			});
		}
		if (infoTypes[USETRANSF_TYPES]){
			lines.add("$TransferCV:");
			HEADER_LENGTH++;
		}
		if (infoTypes[USEWH_TYPES]){
			lines.add("$WaveHt:"); 
			HEADER_LENGTH++;
		}
		if (infoTypes[USEWV_TYPES]){
			lines.add("$WaveVel:"); 
			HEADER_LENGTH++;
		}		

		// ADD HEADER INFORMATION AND BIN NUMBERS (or ms) TO THE LINES
		int lineIndex = 0;
		try {
			// handle writing bin numbers if there's no data in the first file
			if (allMobData.get(0).getMobdata().length == 0){
				for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
					lines.add(String.valueOf(i - HEADER_LENGTH + 1));
					lineIndex++;
				}
			} else {
				// Mobdata is not empty, so write its contents to the array
				if (maxdt != 200 && maxdt != 0){
					// convert DT bins to ms (manually), then write to file
					allMobData = convert_mobdata_to_ms(allMobData, maxdt);
				}
				for (int i = HEADER_LENGTH; i < allMobData.get(0).getMobdata().length + HEADER_LENGTH; i++){
					lines.add(String.valueOf(allMobData.get(0).getMobdata()[lineIndex][0]));
					lineIndex++;
				}
			}
		} catch (NullPointerException ex){
			// mobdata is null - add default header
			for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
				lines.add(String.valueOf(i - HEADER_LENGTH + 1));
				lineIndex++;
			}
		}

		// Convert to array from arraylist
		String[] strings = new String[1];
		String[] arraylines = lines.toArray(strings);
		arraylines[0] = lines.get(0);    	

		// FILL IN THE ARRAY WITH ACTUAL DATA, starting with headers
		for (MobData data : allMobData){
			int lineCounter = 0;
//			// Print the range name only for the first data column
//			if (allMobData.indexOf(data) == 0)
//				arraylines[0] = arraylines[0] + "," + data.getRangeName();
			// Print range/raw name for ALL columns in case ranges are being combined
			arraylines[0] = arraylines[0] + "," + data.getRangeName();
			lineCounter++;
			arraylines[1] = arraylines[1] + "," + data.getRawFileName();
			lineCounter++;

			// Print desired header information for the specified info types
			if (infoTypes[USECONE_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + "," + data.getConeCV();
				lineCounter++;
			}
			if (infoTypes[USETRAP_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + "," + data.getTrapCV();
				lineCounter++;
			}
			if (infoTypes[USETRANSF_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + "," + data.getTransferCV();
				lineCounter++;
			}
			if (infoTypes[USEWH_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + "," + data.getWaveHeight();
				lineCounter++;
			}
			if (infoTypes[USEWV_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + "," + data.getWaveVelocity();
				lineCounter++;
			}

			// WRITE THE ACTUAL DATA
			try{
				// Added catch for null mobdata if there's no (or all 0's) data in the file
				lineIndex = 0;
				// Catch empty mobdata
				if (data.getMobdata().length == 0){
					for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
						arraylines[i] = arraylines[i] + "," + String.valueOf(0);
					}
				}
				for (int i = HEADER_LENGTH; i < data.getMobdata().length + HEADER_LENGTH; i++){
					arraylines[i] = arraylines[i] + "," + String.valueOf(data.getMobdata()[lineIndex][1]);
					lineIndex++;
				}

			} 
			catch (NullPointerException ex){
				// Warn the user that their data is no good
				System.out.println("WARNING: " +
						"No data in " + data.getRawFileName() + ", collision energy " + data.getCollisionEnergy());

				for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
					arraylines[i] = arraylines[i] + "," + String.valueOf(0);
				}

			} catch (ArrayIndexOutOfBoundsException ex){
				System.out.println("\n" + "WARNING: " +
						"(Array index error) " + data.getRawFileName() + ", range File " + data.getRangeName()
						+ "\n" + "Writing all 0's for this range");
				for (int i = HEADER_LENGTH; i < allMobData.get(0).getMobdata().length + HEADER_LENGTH; i++){	
//				for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){	
					arraylines[i] = arraylines[i] + "," + "0";
					lineIndex++;
				}
			}
		}
		return arraylines;
	}

	/**
	 * Alternate method for writing output data. Because RT data never repeats the same 'bins'
	 * (the time keeps increasing by function, unlike DT and MZ, which are the same for all functions),
	 * the data needs to have each function's 'x' data (raw RT) saved as well as 'y' (intensity).
	 * Otherwise, code is identical to dtmzWriteOutputs. Duplicated rather than putting if/else 
	 * at every single line in a single method. 
	 * @param allMobData
	 * @param infoTypes
	 * @return
	 */
	private String[] rtWriteOutputs(ArrayList<MobData> allMobData, boolean[] infoTypes){  	
		ArrayList<String> lines = new ArrayList<String>();

		// Headers
		// Loop through the list of data, writing each function's value for this CE to the line
		int HEADER_LENGTH = 1;
		lines.add("#Range file name:");
		if (infoTypes[USECONE_TYPES]){
			lines.add("$ConeCV:"); 
			HEADER_LENGTH++;
		}
		if (infoTypes[USETRAP_TYPES]){
			lines.add("$TrapCV:"); 
			HEADER_LENGTH++;
		}
		if (infoTypes[USETRANSF_TYPES]){
			lines.add("$TransferCV:");
			HEADER_LENGTH++;
		}
		if (infoTypes[USEWH_TYPES]){
			lines.add("$WaveHt:"); 
			HEADER_LENGTH++;
		}
		if (infoTypes[USEWV_TYPES]){
			lines.add("$WaveVel:"); 
			HEADER_LENGTH++;
		}		

		// ADD HEADER INFORMATION AND BIN NUMBERS TO THE LINES
		int lineIndex = 0;
		try {
			// Mobdata is not empty, so write its contents to the array
			for (int i = HEADER_LENGTH; i < allMobData.get(0).getMobdata().length + HEADER_LENGTH; i++){
				//    				lines.add(String.valueOf(allMobData.get(0).getMobdata()[lineIndex][0]));
				lines.add("");
				lineIndex++;
			}
		} catch (NullPointerException ex){
			for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
				lines.add(String.valueOf(i - HEADER_LENGTH + 1));
				lineIndex++;
			}
		}

		// Convert to array from arraylist
		String[] strings = new String[1];
		String[] arraylines = lines.toArray(strings);
		arraylines[0] = lines.get(0);    	

		// FILL IN THE ARRAY WITH ACTUAL DATA, starting with headers
		for (MobData data : allMobData){
			int lineCounter = 0;
			// Print the range name only for the first data column
			if (allMobData.indexOf(data) == 0)
				arraylines[0] = arraylines[0] + "," + data.getRangeName();
			lineCounter++;

			// Print desired header information for the specified info types
			if (infoTypes[USECONE_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + ",," + data.getConeCV();
				lineCounter++;
			}
			if (infoTypes[USETRAP_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + ",," + data.getTrapCV();
				lineCounter++;
			}
			if (infoTypes[USETRANSF_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + ",," + data.getTransferCV();
				lineCounter++;
			}
			if (infoTypes[USEWH_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + ",," + data.getWaveHeight();
				lineCounter++;
			}
			if (infoTypes[USEWV_TYPES]){
				arraylines[lineCounter] = arraylines[lineCounter] + ",," + data.getWaveVelocity();
				lineCounter++;
			}

			// WRITE THE ACTUAL DATA
			try{
				// Added catch for null mobdata if there's no (or all 0's) data in the file
				lineIndex = 0;
				if (data.getMobdata().length == 0){
					// mobdata is empty! Write all 0's
					for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
						arraylines[i] = arraylines[i] + "," + String.valueOf(0);
					}
				}
				// Otherwise, mobdata exists so write its contents to the lines (BOTH raw RT AND intensity)
				for (int i = HEADER_LENGTH; i < data.getMobdata().length + HEADER_LENGTH - 1; i++){
					arraylines[i] = arraylines[i] + "," + String.valueOf(data.getMobdata()[lineIndex][0]) + "," + String.valueOf(data.getMobdata()[lineIndex][1]);
					lineIndex++;
				}

			} 
			catch (NullPointerException ex){
				// Warn the user that their data is no good
				System.out.println("WARNING: " +
						"No data in " + data.getRawFileName() + ", collision energy " + data.getCollisionEnergy());

				for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
					arraylines[i] = arraylines[i] + "," + String.valueOf(0);
				}

			} catch (ArrayIndexOutOfBoundsException ex){
				System.out.println("\n" + "WARNING: " +
						"(Array index error) " + data.getRawFileName() + ", range File " + data.getRangeName());
				//	    			for (int i = HEADER_LENGTH; i < allMobData.get(0).getMobdata().length + HEADER_LENGTH; i++){	
				//	    				arraylines[i] = arraylines[i] + "," + "0";
				//	    				lineIndex++;
				//	    			}
			}
		}
		return arraylines;
	}


	    
	/**
	 * Rule file spectrum extract method. Passes the extraction argument string to generateMZ. 
	 * @param rawPath
	 * @param nfunction
	 * @param slice
	 * @param selectRegion
	 * @param rangeValues
	 * @param rangeName
	 * @param ruleFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public double[][] generateReplicateSpectrum(String rawPath, int nfunction, int slice, 
			boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile, boolean ruleMode) throws FileNotFoundException, IOException
	{
	    File rawFile = new File(rawPath);
	
	    String rawDataName = rawFile.getName();
		// Get a unique id for the replicate chromatogram
		// Edited to make it actually unique for multiple range files - added name of Range file to it
		String replicateID = null;
		if( slice > 0 )
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + slice + "]";
		else
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + rangeValues[START_MZ] + "_" + rangeValues[STOP_MZ]  + "]";
		
		// Generate a spectrum for the full data
		String specPath = "";
		if (ruleMode){
	    	specPath = generateMZ(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, ruleFile);
		} else {
	    	specPath = generateMZ(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, null);
		}
	
	    double[][] data = getTraceData(specPath, MZ_MODE, rangeValues);
	    
	    // trim data to only includes range specified (by default, IMSExtract writes full m/z range, even if empty)
//	    double mz_low = rangeValues[START_MZ];
//	    double mz_high = rangeValues[STOP_MZ];
//	    
//	    int start_index = java.util.Arrays.binarySearch(data[0], mz_low);
//	    int end_index = java.util.Arrays.binarySearch(data[0], mz_high);
//	    
//	    double[][] final_data = Arrays.copyOfRange(data, start_index, end_index);
	    
	    return data;
	}

	/**
	 * Chomatogram (1D RT) extract method. Passes the extraction argument string to generateRT. 
	 * @param rawPath
	 * @param nfunction
	 * @param slice
	 * @param selectRegion
	 * @param rangeValues
	 * @param rangeName
	 * @param ruleFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public double[][] generateReplicateChromatogram(String rawPath, int nfunction, int slice, 
			boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile, boolean ruleMode) throws FileNotFoundException, IOException
	{
	    File rawFile = new File(rawPath);
	
	    String rawDataName = rawFile.getName();
		// Get a unique id for the replicate chromatogram
		// Edited to make it actually unique for multiple range files - added name of Range file to it
		String replicateID = null;
		if( slice > 0 )
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + slice + "]";
		else
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + rangeValues[START_MZ] + "_" + rangeValues[STOP_MZ]  + "]";
		
		// Generate a spectrum for the full data
		String specPath = "";
		if (ruleMode){
	    	specPath = generateRT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, ruleFile);
		} else {
	    	specPath = generateRT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, null);
		}
	
	    double[][] data = getTraceData(specPath, RT_MODE, rangeValues);
	    
	    return data;
	}

	/**
		 * 1D DT data slice generator. Passes slice name and argument string to generateDT method. 
		 * @param rawPath
		 * @param nfunction
		 * @param slice
		 * @param selectRegion
		 * @param rangeValues
		 * @param rangeName
		 * @param ruleFile
		 * @param ruleMode
		 * @return
		 * @throws FileNotFoundException
		 * @throws IOException
		 */
	private double[][] generateReplicateMobiligram(String rawPath, int nfunction, int slice, 
			boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile, boolean ruleMode)throws FileNotFoundException, IOException {
	
		File rawFile = new File(rawPath);	
		String rawDataName = rawFile.getName();
		// Get a unique id for the replicate chromatogram
		// Edited to make it actually unique for multiple range files - added name of Range file to it
		String replicateID = null;
		if( slice > 0 )
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + slice + "]";
		else
			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + rangeValues[START_DT] + "_" + rangeValues[STOP_DT]  + "]";
	
		// Generate a spectrum for the full data
		String specPath = "";
		if (ruleMode){
			specPath = generateDT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, ruleFile);
		} else {
			specPath = generateDT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], ruleMode, null);
		}
	
		double[][] data = getTraceData(specPath, DT_MODE, rangeValues);
		return data;
	}

	private static ArrayList<MobData> generateReplicateRTDT(DataVectorInfoObject function, File rangeFile, boolean ruleMode, boolean dt_in_ms, double maxDT){

		/* RTDT (2D) */
		// Write range file for IMExtract.exe
		StringBuilder cmdarray = new StringBuilder();
		try
		{
			cmdarray.append(function.getRangeVals()[START_MZ]).append(" ").append(function.getRangeVals()[STOP_MZ]).append(" 1").append(System.getProperty("line.separator"));
			cmdarray.append(function.getRangeVals()[START_RT]).append(" ").append(function.getRangeVals()[STOP_RT]).append(" ").append(String.format("%d", (int) rtBins)).append(System.getProperty("line.separator"));
			cmdarray.append(function.getRangeVals()[START_DT]).append(" ").append(function.getRangeVals()[STOP_DT]).append(" ").append(String.format("%d", (int) dtBins)).append(System.getProperty("line.separator"));

			File dtRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_2dRTDT.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(dtRangeFile));
			writer.write(cmdarray.toString());
			writer.flush();
			writer.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}

		// Run IMExtract.exe
		String path = null;
		String replicateID = function.getRangeName() + "_" + function.getRawDataName() + "_" + function.getFunction() + "[" + function.getRangeVals()[START_DT] + "_" + function.getRangeVals()[STOP_DT]  + "]";
		try
		{
			path = root.getPath() + File.separator + replicateID + ".csv";
			cmdarray.setLength(0);
			cmdarray.append(exeFile.getCanonicalPath()).append(" ");
			cmdarray.append("-d ");
			cmdarray.append("\"").append(function.getRawDataPath()).append("\" ");
//			cmdarray.append("-f ").append(function.getFunction()).append(" ");
			cmdarray.append("-o ");
			cmdarray.append("\"").append(path).append("\" ");
			cmdarray.append("-t ");
			cmdarray.append("mobilicube ");
			cmdarray.append("-p ");
			cmdarray.append("\"").append(preferences.getLIB_PATH()).append("\\ranges_2dRTDT.txt\" ");
			if (ruleMode) {
				cmdarray.append(" -pdtmz ");
				cmdarray.append("\"").append(rangeFile.getAbsolutePath()).append("\"");
			}
			cmdarray.append("-textOut 1");
			runIMSExtract(cmdarray.toString());
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}
		ArrayList<Double> cv_axis = getCVfromCEdat();
		int[] dt_axis_base = IntStream.rangeClosed(1, 200).toArray();
		ArrayList<Double> dtAxisList = new ArrayList<>();
		// convert DT to ms if requested
		for (int dtBin : dt_axis_base) {
			double finalDt = dtBin;		// todo - check this - no need to convert because we make people enter DT range in bins (?)
//			if (dt_in_ms) {
//				finalDt = convertBinToDT(dtBin, maxDT);
//			}
			if (finalDt >= function.getRangeVals()[START_DT] && finalDt <= function.getRangeVals()[STOP_DT]) {
				dtAxisList.add(finalDt);
			}
		}
		double[] dt_axis = new double[dtAxisList.size()];
		for (int i=0; i < dtAxisList.size(); i++) {
			dt_axis[i] = dtAxisList.get(i);
		}

		// Read data from IMExtract.exe and return it
		return getTextData(path, dt_axis, cv_axis, function);
	}

	    /**
	     * Generate a mz data set. We sum over all masses and drift times to generate
	     * a 1 dimensional dataset.
	     */
	    private static String generateMZ(String replicateID, File rawFile, int nFunction,
	            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
	            int mzBins, boolean bSelectRegion, File ruleFile)
	    {
	        StringBuilder cmdarray = new StringBuilder();
	
	        /* MZ plot (1D plot) */
	        try
	        {
	            cmdarray.append(startMZ + " " + stopMZ + " " + mzBins + System.getProperty("line.separator"));
	            cmdarray.append(startRT + " " + stopRT + " 1" + System.getProperty("line.separator"));
	            cmdarray.append(startDT + " " + stopDT + " 1" + System.getProperty("line.separator"));
	            
	            File mzRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_1DMZ.txt");
	            BufferedWriter writer = new BufferedWriter(new FileWriter(mzRangeFile));
	            writer.write(cmdarray.toString());
	            writer.flush();
	            writer.close();
	        }
	        catch(Exception ex)
	        {
	            //_log.writeMessage("Unable to write out MZ range file");
	            ex.printStackTrace();
	//            StackTraceElement[] trace = ex.getStackTrace();
	//            for( int i=0; i<trace.length; i++ )
	//            {
	//                StackTraceElement st = trace[i];
	//            //    _log.writeMessage(st.toString());
	//            }
	            return null;
	        }
	        String path = null;
	        try
	        {
	            path = root.getPath() + File.separator + replicateID + ".1dMZ";
	
	            cmdarray.setLength(0);
	            cmdarray.append(exeFile.getCanonicalPath() + " ");
	            cmdarray.append("-d ");
	            cmdarray.append("\"" + rawFile.getPath() + "\" ");
	            cmdarray.append("-f " + nFunction + " ");
	            cmdarray.append("-o ");
	            cmdarray.append("\"" + path + "\" ");
	            cmdarray.append("-t ");
	            cmdarray.append("mobilicube ");
	            cmdarray.append("-p ");
	            cmdarray.append("\"" + preferences.getLIB_PATH() + "\\ranges_1DMZ.txt\"");
	            if( bSelectRegion )
	            {
	                /* selected region rul files */
	//                File dtmz = new File( preferences.getLIB_PATH() + "\\outDTMZ.txt" );
	                if( ruleFile.exists() )
	                {
	                    cmdarray.append(" -pdtmz ");
	                    cmdarray.append("\"" + ruleFile.getAbsolutePath() + "\"");
	                }
	                File rtdt = new File( preferences.getLIB_PATH() + "\\outRTDT.txt" );
	                if( rtdt.exists() )
	                {
	                    cmdarray.append(" -prtdt ");
	                    cmdarray.append("\"" + rtdt.getAbsolutePath() + "\"");
	                }
	                File rtmz = new File( preferences.getLIB_PATH() + "\\outRTMZ.txt" );
	                if( rtmz.exists() )
	                {
	                    cmdarray.append(" -prtmz ");
	                    cmdarray.append("\"" + rtmz.getAbsolutePath() + "\"");
	                }
	            }
	    //        _log.writeMessage(cmdarray);
	    //        progMon.updateStatusMessage("Generating spectrum");
	            runIMSExtract(cmdarray.toString());
	        }
	        catch( Exception ex )
	        {
	            ex.printStackTrace();
	        }
	        
	        return path;
	    }

	//    /**
		//     * Generate a retention time data set. We sum over all masses and drift times to generate
		//     * a 1 dimensional dataset.
		//     */
			private static String generateRT(String replicateID, File rawFile, int nFunction,
		            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
		            int rtBins, boolean bSelectRegion, File ruleFile)
		    {
		        StringBuilder cmdarray = new StringBuilder();
		
		        /* RT plot (1D plot) */
		        try
		        {
		            cmdarray.append(startMZ + " " + stopMZ + " 1" + System.getProperty("line.separator"));
		            cmdarray.append(startRT + " " + stopRT + " " + rtBins + System.getProperty("line.separator"));
		            cmdarray.append(startDT + " " + stopDT + " 1" + System.getProperty("line.separator"));
		
		            File rtRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_1DRT.txt");
		            BufferedWriter writer = new BufferedWriter(new FileWriter(rtRangeFile));
		            writer.write(cmdarray.toString());
		            writer.flush();
		            writer.close();
		        }
		        catch(Exception ex)
		        {
		//            _log.writeMessage("Unable to write out RT range file");
		            ex.printStackTrace();
		            StackTraceElement[] trace = ex.getStackTrace();
		            for( int i=0; i<trace.length; i++ )
		            {
//		                StackTraceElement st = trace[i];
		//                _log.writeMessage(st.toString());
		            }
		            return null;
		        }
		        
		        String path = null;
		        
		        try
		        {
		            path = root.getPath() + File.separator + replicateID + ".1dRT";
		
		            cmdarray.setLength(0);
		            cmdarray.append(exeFile.getCanonicalPath() + " ");
		            cmdarray.append("-d ");
		            cmdarray.append("\"" + rawFile.getPath() + "\" ");
		            cmdarray.append("-f " + nFunction + " ");
		            cmdarray.append("-o ");
		            cmdarray.append("\"" + path + "\" ");
		            cmdarray.append("-t ");
		            cmdarray.append("mobilicube ");
		            cmdarray.append("-p ");
		            cmdarray.append("\"" + preferences.getLIB_PATH() + "\\ranges_1DRT.txt\"");
		            if( bSelectRegion )
		            {
		                /*cmdarray += " -px ";
		                cmdarray += root.getPath() + File.separator + "selRegion.txt";*/
		                /* selected region rul files */
		            	if( ruleFile.exists() )
		                {
		                    cmdarray.append(" -pdtmz ");
		                    cmdarray.append("\"" + ruleFile.getAbsolutePath() + "\"");
		                }
		                File dtmz = new File( preferences.getLIB_PATH() + "\\outDTMZ.txt" );
		                if( dtmz.exists() )
		                {
		                    cmdarray.append(" -pdtmz ");
		                    cmdarray.append("\"" + dtmz.getAbsolutePath() + "\"");
		                }
		                File rtdt = new File( preferences.getLIB_PATH() + "\\outRTDT.txt" );
		                if( rtdt.exists() )
		                {
		                    cmdarray.append(" -prtdt ");
		                    cmdarray.append("\"" + rtdt.getAbsolutePath() + "\"");
		                }
		                File rtmz = new File( preferences.getLIB_PATH() + "\\outRTMZ.txt" );
		                if( rtmz.exists() )
		                {
		                    cmdarray.append(" -prtmz ");
		                    cmdarray.append("\"" + rtmz.getAbsolutePath() + "\"");
		                }
		            }
		    //        _log.writeMessage(cmdarray);
		//            progMon.updateStatusMessage("Generating chromatogram");
		            runIMSExtract(cmdarray.toString());
		        }
		        catch( Exception ex )
		        {
		            ex.printStackTrace();
		            System.err.println(ex.getMessage());
		            return null;
		        }
		        
		        return path;
		    }

	//
    /**
     * Generate a drift time data set. We sum over all masses and drift times to generate
     * a 1 dimensional dataset.
     */
    private static String generateDT(String replicateID, File rawFile, int nFunction,
            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
            int dtBins, boolean bSelectRegion, File ruleFile)
    {
        /* DT plot (1d plot) */
        StringBuilder cmdarray = new StringBuilder();
        try
        {
            cmdarray.append(startMZ + " " + stopMZ + " 1" + System.getProperty("line.separator"));
            cmdarray.append(startRT + " " + stopRT + " 1" + System.getProperty("line.separator"));
            cmdarray.append(startDT + " " + stopDT + " " + dtBins + System.getProperty("line.separator"));

            File dtRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_1DDT.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(dtRangeFile));
            writer.write(cmdarray.toString());
            writer.flush();
            writer.close();
        }
        catch(Exception ex)
        {
//            _log.writeMessage("Unable to write out DT range file");
            ex.printStackTrace();
            StackTraceElement[] trace = ex.getStackTrace();
            for( int i=0; i<trace.length; i++ )
            {
            }
            return null;
        }
        
        String path = null;
        
        try
        {
            path = root.getPath() + File.separator + replicateID + ".1dDT";

            cmdarray.setLength(0);
            cmdarray.append(exeFile.getCanonicalPath() + " ");
            cmdarray.append("-d ");
            cmdarray.append("\"" + rawFile.getPath() + "\" ");
            cmdarray.append("-f " + nFunction + " ");
            cmdarray.append("-o ");
            cmdarray.append("\"" + path + "\" ");
            cmdarray.append("-t ");
            cmdarray.append("mobilicube ");
            cmdarray.append("-p ");
            cmdarray.append("\"" + preferences.getLIB_PATH() + "\\ranges_1DDT.txt\"");
            if( bSelectRegion )
            {
                /* selected region rul files */
//                File dtmz = new File( preferences.getLIB_PATH() + "\\outDTMZ.txt" );
            	
                if( ruleFile.exists() )
                {
                    cmdarray.append(" -pdtmz ");
                    cmdarray.append("\"" + ruleFile.getAbsolutePath() + "\"");
                }
                File rtdt = new File( preferences.getLIB_PATH() +  "\\outRTDT.txt" );
                if( rtdt.exists() )
                {
                    cmdarray.append(" -prtdt ");
                    cmdarray.append("\"" + rtdt.getAbsolutePath() + "\"");
                }
                File rtmz = new File( preferences.getLIB_PATH() + "\\outRTMZ.txt" );
                if( rtmz.exists() )
                {
                    cmdarray.append(" -prtmz ");
                    cmdarray.append("\"" + rtmz.getAbsolutePath() + "\"");
                }
            }
    //        _log.writeMessage(cmdarray);
            runIMSExtract(cmdarray.toString());
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        
        return path;
    }

	/**
	 * For methods that return text data instead of binary, use this to read the resulting output file.
	 * (this is very convoluted, but allows this method to use all the existing print/etc code so..)
	 * @param path path to output file
	 * @return 2D data array
	 */
	private static ArrayList<MobData> getTextData(String path, double[] dt_axis, ArrayList<Double> cv_axis, DataVectorInfoObject function) {
		ArrayList<MobData> allMobData = new ArrayList<>();

		// Open the text file to read
		File textFile = new File(path);
		textFile.deleteOnExit();
		if( !textFile.exists() ) {
			return null;
		}

		// Read file
		try {
			BufferedReader reader = new BufferedReader(new FileReader(textFile));
			String line;
			int cvIndex = 0;
			while((line = reader.readLine()) != null) {
				// Lines are transposed (each row is all DTs from a given CV)
				String[] splits = line.split(",");
				double[][] data = new double[dt_axis.length][2];

				// Add dt axis into data array
				for (int i=0; i < dt_axis.length; i++) {
					data[i][0] = dt_axis[i];
				}
				// Read intensity into into data array
				for (int i=0; i < splits.length; i++){
					data[i][1] = Double.parseDouble(splits[i]);
				}

				// Generate this mobdata
				MobData currentMob = new MobData(data, function.getRawDataName(), function.getRangeName(), function.getConeCV(), cv_axis.get(cvIndex), function.getTransfCV(), function.getWaveHeight(), function.getWaveVel());
				allMobData.add(currentMob);
				cvIndex++;
			}
			reader.close();
		} catch (IOException ex) {
			System.out.println("Error: could not find text file to extract. No data returned " + path);
			return null;
		}
		return allMobData;
	}


    private synchronized static double[][] getTraceData(String path, int nType, double[] rangeVals) throws FileNotFoundException, IOException
    {
        // Open the binary file as channel
        File binFile = new File(path);

        double data[][] = (double[][])null;
        
        binFile.deleteOnExit();
        if( !binFile.exists() )
        {
            return null;
        }
        RandomAccessFile rafFile = new RandomAccessFile( binFile, "r" );
        FileChannel channel = rafFile.getChannel();
        
        // The memory mapped buffer
        MappedByteBuffer nMbb = null;
        
        //Read number of mass channels

        nMbb = channel.map(FileChannel.MapMode.READ_ONLY,0L,binFile.length());
        nMbb = nMbb.load();
        nMbb.order(ByteOrder.LITTLE_ENDIAN);
              
        /* Get the actual number of bins used */
        // NOTE - this overwrites any bins passed in range files, so I'm removing bin arguments from ranges
        int nMZBins = 0;
        int nRTBins = 0;
        int nDTBins = 0;
        int nBins = 0;
        try{
        	nMZBins = nMbb.getInt();
        	nRTBins = nMbb.getInt();
        	nDTBins = nMbb.getInt();
        	nBins = 0;
        }
        catch(java.nio.BufferUnderflowException ex){
        	System.out.println("Buffer under flow: No data extracted from " + path);
//        	ex.printStackTrace();
        }
        
        if( nType == RT_MODE )
        {
            nBins = nRTBins;
        }
        if( nType == DT_MODE )
        {
            nBins = nDTBins;
        }
        if( nType == MZ_MODE )
        {
            nBins = nMZBins;
        }
        
        // Generate our storage
        if (nType == MZ_MODE){
        	// Load only the data within the specified m/z range into our point array
        	ArrayList<double[]> small_data = new ArrayList<double[]>();
	        for( int nZ = 0; nZ < nBins; nZ++)
	        {
	            float fX = NumberUtils.roundNumber(nMbb.getFloat(), 3);
	            int nCount = nMbb.getInt();
	            if( nCount < 0 ){
	                //_log.writeMessage("Warning -ve counts " + nCount);
	            }
	            else {
	            	// Only add this value to the data array if it's in the desired range
	            	if (fX > rangeVals[START_MZ] && fX < rangeVals[STOP_MZ]){
	            		small_data.add(new double[]{fX, nCount});
//	            		data[nZ] = new double[]{fX, nCount}; 
	            	}
	            }
	        }
	        // Once data is loaded, return as an array of the correct size
	        double[][] data_size = new double[small_data.size()][2];
	        data = small_data.toArray(data_size);
	        
        } else {
        	data = new double[nBins][2];
        	
	        // Load all the data into our point array for DT and RT modes
	        for( int nZ = 0; nZ < nBins; nZ++)
	        {
	            float fX = NumberUtils.roundNumber(nMbb.getFloat(), 3);
	            int nCount = nMbb.getInt();
	            if( nCount < 0 ){
	                //_log.writeMessage("Warning -ve counts " + nCount);
	            }
	            else {
	                data[nZ] = new double[]{fX, nCount}; 
	            }
	        }
        }
        
        channel.close();
        rafFile.close();
        binFile.delete();

        return data;
    }
//    
    
    /**
	 * Runs imextract.exe using the specified command arguments
	 * @param cmdarray - the commandline arguments for imextract.exe
	 */
	private synchronized static void runIMSExtract(String cmdarray)
	{
	    //System.out.println(cmdarray);
	    Process proc = null;
	    Runtime runtime = Runtime.getRuntime();
	
	    try
	    {
	        proc = runtime.exec(cmdarray);
	    }
	    catch(Exception ex)
	    {
	        StackTraceElement trace[] = ex.getStackTrace();
	        for(int i = 0; i < trace.length; i++)
	        {
	        }
	
	        return;
	    }
	    try
	    {
	        InputStream procOut = proc.getInputStream();
	        InputStream procErr = proc.getErrorStream();
	        byte buf[] = new byte[1024];
	        int nRead = 0;
	        String lineSep = System.getProperty("line.separator");
	        do
	        {
	            boolean bHaveOutput = false;
	            if(procOut.available() > 0)
	            {
	                bHaveOutput = true;
	                nRead = procOut.read(buf);
	                String out = new String(buf, 0, nRead);
	                
	                String[] splits = out.split(lineSep);                    
	                for( String split : splits )
	                {
	                    if( split.startsWith("PROGRESS:") )
	                    {
	                        String prog = split.replace("PROGRESS:", "");
	                        if( prog.length() > 0 )
	                        {
	                            Integer.parseInt(prog);
	                        }
	                    }
	                }
	            }
	            if(procErr.available() > 0)
	            {
	                bHaveOutput = true;
	                nRead = procErr.read(buf);
	            }
	            try
	            {
	                proc.exitValue();
	                break;
	            }
	            catch(IllegalThreadStateException itsx)
	            {
	                System.out.print(".");
	                if(!bHaveOutput)
	                    try
	                    {
	                        Thread.sleep(300L);
	                    }
	                    catch(Exception ex) { }
	            }
	        } while(true);
	    }
	    catch(Exception ex)
	    {
	        StackTraceElement trace[] = ex.getStackTrace();
	        for(int i = 0; i < trace.length; i++)
	        {
	            StackTraceElement st = trace[i];
	            System.err.println(st.toString());
	        }
	
	        return;
	    }
	    
	}

	public void writeRawFile(String rawPath, int nfunction, String outputPath, int type, int msUnits, String dtmzRulFilePath, String rtmzRulFilePath, String rtdtRulFilePath)
    {
        try {
            File rawFile = new File(rawPath);
            
            File rangesFile = new File(preferences.getLIB_PATH() + "\\rawRanges.txt");
            
            // The command array
            StringBuilder cmdarray = new StringBuilder();
            // Build up the command
            cmdarray.append(exeFile.getCanonicalPath() + " ");
            cmdarray.append("-d ");
            cmdarray.append("\"" + rawFile.getPath() + "\" ");
            cmdarray.append("-f " + nfunction + " ");
            cmdarray.append("-o ");
            cmdarray.append("\"" + outputPath + "\" ");
            cmdarray.append("-t ");
            if( type == 0 )
            {
                cmdarray.append("imraw ");            
            }
            else
            {
                cmdarray.append("imrawdt ");
                cmdarray.append("-ms " + msUnits + " ");
            }
            cmdarray.append("-p ");
            cmdarray.append("\"" + rangesFile.getCanonicalPath() + "\" ");
            if( dtmzRulFilePath != null )
            {
                cmdarray.append(" -pdtmz ");
                cmdarray.append("\"" + dtmzRulFilePath + "\"");
            }
            if( rtmzRulFilePath != null )
            {
                cmdarray.append(" -prtmz ");
                cmdarray.append("\"" + rtmzRulFilePath + "\"");
            }
            if( rtdtRulFilePath != null )
            {
                cmdarray.append(" -prtdt ");
                cmdarray.append("\"" + rtdtRulFilePath + "\"");
            }
            // Run imextract
//            progMon.updateStatusMessage("Writing raw data");
            runIMSExtract(cmdarray.toString());
        } 
        catch (Exception ex) 
        {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

	// Flags indicating the position of data values in an array
	public static final int START_MZ = 0;
	public static final int STOP_MZ = 1;
	public static final int MZ_BINS = 2;
	public static final int START_RT = 3;
	public static final int STOP_RT = 4;
	public static final int RT_BINS = 5;
	public static final int START_DT = 6;
	public static final int STOP_DT = 7;
	public static final int DT_BINS = 8;

	// Trace data types
	public static int RT_MODE = 0;
	public static int DT_MODE = 1;
	public static int MZ_MODE = 2;
	public static int DTMZ_MODE = 3;
	public static int RTDT_MODE = 4;

	// Range values 
	private static double minMZ = 0.0;
	private static double maxMZ = 0.0;
	private static double mzBins = 0.0;
	private static double minRT = 0.0;
	private static double maxRT = 0.0;
	private static double rtBins = 0.0;
	private static double minDT = 0.0;
	private static double maxDT = 0.0;
	private static double dtBins = 0.0;
	private static double zHigh = Double.MIN_VALUE;
	private static double zLow = Double.MAX_VALUE;
	
	private static File exeFile;
	public static int BPI = 1;
	public static int TIC = 0;
	private static final int USECONE_TYPES = 0;
	private static final int USETRAP_TYPES = 1;
	private static final int USETRANSF_TYPES = 2;
	private static final int USEWH_TYPES = 4;
	private static final int USEWV_TYPES = 3;

	/**
	 * @return the zHigh
	 */
	public static double getzHigh() {
	    return zHigh;
	}

	/**
	 * @return the zLow
	 */
	public static double getzLow() {
	    return zLow;
	}
}