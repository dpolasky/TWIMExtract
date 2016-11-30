/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ciugen.imextract;

import ciugen.preferences.Preferences;
import ciugen.utils.NumberUtils;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object is a utility class for making calls to the imextract.exe in order to extract
 * specific ranges of HDMS data
 * @author UKKNEKM
 * v3.3
 */
public class IMExtractRunner
{
    // THe single instance of this object
    private static IMExtractRunner instance;
    
    // Single instance of the preferences
    private static Preferences preferences = Preferences.getInstance();

    // This is the root folder for the current analysis
    private static File root;

    // Line separator
    private static String lineSep = System.getProperty("line.separator");

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
    private static final int USEWH_TYPES = 3;
    private static final int USEWV_TYPES = 4;    
    
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

    /**
     * Private constructor
     */
    private IMExtractRunner()
    {
        exeFile = new File(preferences.getLIB_PATH() + File.separator + "imextract.exe");
        setRoot( preferences.getCIUGEN_HOME() + "\\root");
    }

    /**
	     * Initializes the binary maps for the data model
	     */
	    private static void initialiseBinaryMaps()throws FileNotFoundException, IOException
	    {
	        double[] dtMap = new double[200];
	        
	        /* Generate the file path to our summed and binned data */
	        String strPath = root.getPath();
	        
	        String dtBinPath = strPath + File.separator + "_dt.bin";
	        
	        /* Open the dt binary file as channel */
	        File binFileDT = new File( dtBinPath );
	        binFileDT.deleteOnExit();
	        if( !binFileDT.exists() )
	        {
	            return;
	        }
	        RandomAccessFile rafFileDT = new RandomAccessFile( binFileDT, "r" );
	        FileChannel channelDT = rafFileDT.getChannel();
	        
	        /* The memory mapped buffer for drift time */
	        MappedByteBuffer nMbbDT = null;
	        
	        /* Read number of mass channels */
	        if( nMbbDT != null )
	        {
	            nMbbDT.clear();
	        }
	        
	        nMbbDT = channelDT.map( FileChannel.MapMode.READ_ONLY, 0L, binFileDT.length() );
	        nMbbDT = nMbbDT.load();
	        nMbbDT.order(ByteOrder.LITTLE_ENDIAN);
	        
	        float fDTTime = 0.0f;
	        for( int i=0; i<dtMap.length; i++ )
	        {
	            nMbbDT.getInt();
	            fDTTime = nMbbDT.getFloat();
	            dtMap[ i ] = NumberUtils.roundNumber(fDTTime, 2);
	        }
	        
	//        convertor.setDtMap(dtMap);
	        
	        channelDT.close();
	        rafFileDT.close();
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
            while((line = reader.readLine()) != null)
            {
                String[] splits = line.split(" ");
                for( String split : splits )
                {
                    double d = Double.parseDouble(split);
                    //rangesArr[valueCounter] = d;
                    switch( valueCounter )
                    {
                    	// EDIT - made it so that non-integer ranges can be passed for min and max m/z and RT.
                    	// Not sure why they were rounded in the first place. Should probably also actually make an integer check for DT stuff too.
                        case START_MZ:
//                            minMZ = Math.floor(d);
                        	minMZ = d;
                        	rangesArr[valueCounter] = minMZ;
                            break;
                            
                        case STOP_MZ:
//                            maxMZ = Math.ceil(d);
                        	maxMZ = d;
                        	rangesArr[valueCounter] = maxMZ;
                            break;
                            
                        case MZ_BINS:
                            mzBins = d;
                            rangesArr[valueCounter] = mzBins;
                            break;
                            
                        case START_RT:
//                            minRT = Math.floor(d);
                            minRT = d;
                            rangesArr[valueCounter] = minRT;
                            break;
                            
                        case STOP_RT:
//                            maxRT = Math.ceil(d);
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
        //EDIT - print the ranges used to the terminal
        for (int i=0; i<rangesArr.length;i++){
        	//System.out.print(rangesArr[i]+" ");
        }
        //System.out.println();
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
    	System.out.println("\n" + "Analyzing Raw Files (may take a minute)");
    }
    

    /**
	 * Returns an array of the last data ranges run
	 * @return 
	 */
	public double[] getLastRanges()
	{
		double[] ranges = new double[]{minMZ, maxMZ, mzBins, minRT, maxRT, rtBins, minDT, maxDT, dtBins};
	
		return ranges;
	}


/**
	 * Gets the root directory
	 * @return
	 */
	private static File getRoot()
	{
	    return root;
	}

	/**
 * Sets the root directory
 * @param path 
 */
private static void setRoot(String path)
{
    //System.out.println("Setting root: " + path);
    root = new File(path);
    root.mkdirs();
}

	/**
	     * Updated extract Mobiligram method that takes a list of functions to analyze (length 1 for single
	     * file analyses), calls the appropriate helper methods based on the extraction mode, then combines
	     * the returned (extracted) data for writing to an output file specified by the output path. 
	     * Does NOT use collision energy
	     * @param allFunctions = the list of functions (data) to be extracted with all their associated information in DataVectorInfoObject format
	     * @param outputFilePath = where to write the output file
	     * @param ruleMode = whether to use range files or rule files for extracting
	     * @param ruleFile = the rule OR range file being used for the extraction
	     * @param extraction_mode = the type of extraction to be done (DT, MZ, RT, or DTMZ)
	     */
	    public void extractMobiligramOneFile(ArrayList<DataVectorInfoObject> allFunctions, String outputFilePath, boolean ruleMode, File ruleFile, int extractionMode){
	    	int HEADER_LENGTH = 1;
	    	String lineSep = System.getProperty("line.separator");
	    	
	    	// Get info types to print from first function (they will be the same for all functions)
	    	boolean[] infoTypes = allFunctions.get(0).getInfoTypes();
	    	try {
	    		// Collect mobData for all functions in the list
	    		ArrayList<MobData> allMobData = new ArrayList<MobData>();
	
	    		for (DataVectorInfoObject function : allFunctions){
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
	    			if (extractionMode == DT_MODE){
	        			data = generateReplicateMobiligram(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile, ruleMode);
	
	    			} else if (extractionMode == MZ_MODE){
	    				data = generateReplicateSpectrum(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile, ruleMode);
	
	    			} else if (extractionMode == RT_MODE){
	    				data = generateReplicateChromatogram(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile, ruleMode);
	
	    			} else if (extractionMode == DTMZ_MODE){
	//    				data = generateReplicateDTMZ(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile, ruleMode);
	    			}
	    			
	    			if (data == null){
	    				System.out.println("Error during extraction! Check your raw data - it might be empty or corrupted");
	    			}
	    			MobData currentMob = new MobData(data,rawName,rangeName,conecv,trapcv,transfcv,wh,wv);
	    			allMobData.add(currentMob);
	//    			System.out.println("\n" + "Completed Analysis " + counter + " of " + allFunctions.size() + "\n");
	//    			counter++;
	    		}
	
	
	    		File out = new File(outputFilePath);
	    		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
	//    		String[] lines = new String[allMobData.get(0).getMobdata().length + HEADER_LENGTH];		// +2 for the 2 header lines
	    		ArrayList<String> lines = new ArrayList<String>();
	    		
	    		// Headers
	    		// Loop through the list of data, writing each function's value for this CE to the line
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
	    				lines.add(String.valueOf(allMobData.get(0).getMobdata()[lineIndex][0]));
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
	    		
	    		// FILL IN THE ARRAY WITH ACTUAL DATA
	    		for (MobData data : allMobData){
	    			int lineCounter = 0;
	    			arraylines[0] = arraylines[0] + "," + data.getRangeName();
	    			lineCounter++;
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
	
	    			// Added catch for null mobdata if there's no (or all 0's) data in the file
	    			try{
	    				lineIndex = 0;
	    				// Catch empty mobdata
	    				if (data.getMobdata().length == 0){
	    					for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
	    							arraylines[i] = arraylines[i] + "," + String.valueOf(0);
	//            					lineIndex++;
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
	    					arraylines[i] = arraylines[i] + "," + "0";
	    					lineIndex++;
	    				}
	    			}
	    			
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
	
	    double[][] data = getTraceData(specPath, MZ_MODE);
	    
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
	
	    double[][] data = getTraceData(specPath, RT_MODE);
	    
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
	
		double[][] data = getTraceData(specPath, DT_MODE);
		return data;
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
		                StackTraceElement st = trace[i];
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

    


    private synchronized static double[][] getTraceData(String path, int nType) throws FileNotFoundException, IOException
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
        if( nMbb != null )
        {
            nMbb.clear();
        }
          
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
//        	System.out.println("\n" + "**ERROR: Could not process data**" + "\n"
//        			+ "Something is wrong with the range file used. "
//        			+ "\n" + "Try increasing the RT window, Make sure DT bins is 200, "
//        			+ "and make sure your mass ranges are correct."
//        			);
//        	System.out.println("Buffer under flow error - problem with data processing for " + path);
//        	System.exit(0);
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
        
        // Generate our storage and reset highest number of counts
        data = new double[nBins][2];
        int nHighestCounts = 0;
        
        // Load the data into our point array
        for( int nZ = 0; nZ < nBins; nZ++)
        {
            float fX = NumberUtils.roundNumber(nMbb.getFloat(), 3);
            int nCount = nMbb.getInt();
            if( nCount < 0 )
            {
                //_log.writeMessage("Warning -ve counts " + nCount);
            }
            else 
            {
                data[nZ] = new double[]{fX, nCount};
                if( nCount > nHighestCounts ) 
                {
                    nHighestCounts = nCount;
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
    
    // OLD VERSION
     /**
     * @param args the command line arguments
     */


	/**
     * Generates a DTMZ map for the specified raw file and function
     * @param rawFile - the target raw file
     * @param nFunction - the target data function
     * @param startMZ - the start mz range
     * @param stopMZ - to the stop mz range
     * @param startRT - the start rt range
     * @param stopRT - the stop rt range
     * @param startDT - the start dt range
     * @param stopDT - the stop dt range
     */
//    private static String generateDTMZSlice(String replicateID, File rawFile, int nFunction,
//            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT, 
//            int mzbins, boolean bSelectRegion, File ruleFile){
//    	
//        // A StringBUilder for the command line arguments
//        StringBuilder cmdarray = new StringBuilder();
//        // Get the preferred number of MZ bins
//        int nPrefMaxMZbins = preferences.getnMaxMZBins();
//        
//        // MIGHT NEED TO CHANGE MAX MZ BINS TO ACTUAL MZ BINS FROM RANGE ARRAY
//        
//        // Build the commandline
//        cmdarray.append(startMZ + " " + stopMZ + " " + mzbins + " " + lineSep);
//        cmdarray.append(startRT + " " + stopRT + " " + 1 + " " + lineSep); // Note the number of RT bins here is 1 as we are collapsing the RT range into a single bin
//        cmdarray.append(startDT + " " + stopDT + " " + 200 + " " + lineSep);
//        
//        String path = root.getPath() + File.separator + replicateID + ".2dDTMZ";
//        
//        try
//        {
//            // Write out the 2DDTMZ ranges file...
//            File dtmzRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_2DDTMZ.txt");
//            BufferedWriter writer = new BufferedWriter(new FileWriter(dtmzRangeFile));
//            writer.write(cmdarray.toString());
//            writer.flush();
//            writer.close();
//
//            // Create a 2dDTMZ file for the imextract to write to
//            File file = new File(path);
////            DataModel model = DataModelFactory.getDataModel();
////            DataProcess dp = model.getCurrentProcess();
////            if(dp != null && dp.getProcessType() != 0)
////                file.deleteOnExit();
////            if(file.exists())
////                System.out.println((new StringBuilder()).append("Deleted?").append(file.delete()).toString());
//
//            // Clear the commandarray
//            cmdarray.setLength(0);
//            // Build the command for imextract
//            cmdarray.append(exeFile.getCanonicalPath() + " ");
//            cmdarray.append("-d ");
//            cmdarray.append("\"" + rawFile.getPath() + "\" ");
//            cmdarray.append("-f " + nFunction + " ");
//            cmdarray.append("-o ");
//            cmdarray.append("\"" + getRoot().getPath() + File.separator + replicateID + ".2dDTMZ\" ");
//            cmdarray.append("-t ");
//            cmdarray.append("mobilicube ");
//            cmdarray.append("-p ");
//            cmdarray.append("\"" + preferences.getLIB_PATH() +  "\\ranges_2DDTMZ.txt\"");
//            
//            if (bSelectRegion){
//            	if( ruleFile.exists() )
//                {
//                    cmdarray.append(" -pdtmz ");
//                    cmdarray.append("\"" + ruleFile.getAbsolutePath() + "\"");
//                }
//                File dtmz = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("dtmz_selRegion.txt").toString());
//                if(dtmz.exists())
//                {
//                	cmdarray.append(" -pdtmz ");
//                	cmdarray.append("\"" + dtmz.getAbsolutePath() + "\"");
//                }
//                File rtdt = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("rtdt_selRegion.txt").toString());
//                if(rtdt.exists())
//                {
//                	cmdarray.append(" -prtdt ");
//                	cmdarray.append("\"" + rtdt.getAbsolutePath() + "\"");
//                }
//                File rtmz = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("rtmz_selRegion.txt").toString());
//                if(rtmz.exists())
//                {
//                	cmdarray.append(" -prtmz ");
//                	cmdarray.append("\"" + rtmz.getAbsolutePath() + "\"");
//                }
//            }
//            
//            //log.writeMessage((new StringBuilder()).append("Generating DTMZ map: ").append(cmdarray).toString());
////            progMon.updateStatusMessage("Generating DTMZ map");
//            runIMSExtract(cmdarray.toString());
//        }
//        catch(Exception ex)
//        {
//            //log.writeMessage(ex.getMessage());
//            StackTraceElement trace[] = ex.getStackTrace();
//            for(int i = 0; i < trace.length; i++)
//            {
//                StackTraceElement st = trace[i];
//                //log.writeMessage(st.toString());
//            }
//        }
//        return path;
//    }
	

	//private double[][] generateReplicateDTMZ(String rawPath, int nfunction, int slice, 
//			boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile, boolean ruleMode)throws FileNotFoundException, IOException {
	//
//		File rawFile = new File(rawPath);	
//		String rawDataName = rawFile.getName();
//		// Get a unique id for the replicate chromatogram
//		// Edited to make it actually unique for multiple range files - added name of Range file to it
//		String replicateID = null;
//		if( slice > 0 )
//			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + slice + "]";
//		else
//			replicateID = rangeName + "_" + rawDataName + "_" + nfunction + "[" + rangeValues[START_DT] + "_" + rangeValues[STOP_DT]  + "]";
	//
//		// Generate a spectrum for the full data
//		String specPath = "";
//		if (ruleMode){
//			specPath = generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion, ruleFile);
//		} else {
//			specPath = generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion, null);
//		}
	//
//		double[][] data = getTraceData(specPath, DTMZ_MODE);
//		return data;
	//}
	
//	    /**
//	     * Main class for testing
//	     * @param args the command line arguments
//	     */
//	    public static void main(String[] args)
//	    {
//	        try
//	        {
//	            setRoot( "C:\\Temp\\test\\HDMSCompare\\Analysis 1");
//	             
//	            System.out.println("Running imextract");
//	            String replicatePath = "C:\\Share\\DummyData\\IMS Compare Data\\Jengen\\012508_ri_2.raw";
//	            File rawFile = new File(replicatePath);
//	            String rawDataName = rawFile.getName();
//	            //rawDataName = rawDataName.substring(0, rawDataName.lastIndexOf(".raw") - 1);
//	            int nfunction = 1;
//	            // Get the full data ranges
//	            getFullDataRanges(rawFile, nfunction);
//	            // read the full data ranges
//	            double[] rangeValues = readDataRanges();
//
//	            double startRTInit = rangeValues[START_RT];
//	            double stopRTInit = rangeValues[STOP_RT];
//	            double rtRangeInit = stopRTInit - startRTInit;
//	            int nslices = 10;
//	            double sliceStep = NumberUtils.round(rtRangeInit / nslices, 4);
//
//	            // Generate the DTMZ slice for the full ranges
//	            // Get a unique id for the next slice
//	            String replicateID = rawDataName + "_" + nfunction + "[0]";
//	            // Generate the DTMZ slice of the full data
//	            generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], rangeValues[3], rangeValues[4], rangeValues[6], rangeValues[7], null);
//	            // Generate a chromatogram for the full data
//	            generateRT(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], rangeValues[3], rangeValues[4], rangeValues[6], rangeValues[3], (int)rangeValues[5], false);
//	            // Genereate a mobiligram for the full data
////	            generateDT(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], rangeValues[3], rangeValues[4], rangeValues[6], rangeValues[3], (int)rangeValues[8]);
//	            // Generate a spectrum for the full data
////	            generateMZ(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], rangeValues[3], rangeValues[4], rangeValues[6], rangeValues[3], (int)rangeValues[2]);
	//
//	            System.out.println(replicateID + " = " + rangeValues[3] + " : " + rangeValues[4]);
//	            // now generate the specifed number of DTMZ slices
//	            for( int i=0; i<nslices; i++ )
//	            {
//	                double step = sliceStep * i;
//	                double startRT = NumberUtils.round(startRTInit + step, 4);
//	                double stopRT = NumberUtils.round(startRT + sliceStep, 4);
//	                if( stopRT > stopRTInit )
//	                    stopRT = stopRTInit;
//	                int repIndex = i+1;
//	                replicateID = rawDataName + "_" + nfunction + "[" + repIndex + "]";
	//
//	                // Generate the next DTMZ slice
//	                generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], startRT, stopRT, rangeValues[6], rangeValues[7], null);
//	                // Generate a chromatogram for this slice
//	                generateRT(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], startRT, stopRT, rangeValues[6], rangeValues[3], (int)rangeValues[5], false);
//	                // Genereate a mobiligram for this slice
////	                generateDT(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], startRT, stopRT, rangeValues[6], rangeValues[3], (int)rangeValues[8]);
//	                // Generate a spectrum for this slice
////	                generateMZ(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], startRT, stopRT, rangeValues[6], rangeValues[3], (int)rangeValues[2]);
	//
//	                System.out.println(replicateID + " = " + startRT + " : " + stopRT);
//	            }
	//
//	            System.out.println("Complete");
//	        }
//	        catch( Exception ex )
//	        {
//	            System.out.println("ERROR!");
//	            ex.printStackTrace();
//	        }
	//
//	    }

}
