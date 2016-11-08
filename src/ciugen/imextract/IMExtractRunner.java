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
    public static int nDATA_1D_RT = 0;
    public static int nDATA_1D_DT = 1;
    public static int nDATA_1D_MZ = 2;
    
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
    private static final int USELOCKMASS_TYPES = 5;
    
    
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
            initialiseBinaryMaps();
            
        } catch (IOException ex) {
            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

  
    /**
     * EDIT: new
     * Reads the data ranges from the specified file
     * @param rangesFilePath: the path to the ranges text file
     * @return - an array of the ranges specified in the ranges file
     */
    public static double[] readDataRanges(String rangesName)
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
     * EDIT: new
     * Reads the data ranges from the specified file
     * @param rangesFilePath: the path to the ranges text file
     * @return - an array of the ranges specified in the ranges file
     */
    public static double[] editDataRanges(String rangesName, double correction)
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
                        	
                        	minMZ = d + correction;
                        	rangesArr[valueCounter] = minMZ;
                            break;
                            
                        case STOP_MZ:
//                            maxMZ = Math.ceil(d);
                        	maxMZ = d + correction;
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
     * EDIT - new method - prints ranges used to the terminal, intended for call in
     * the main method of CIUGenFrame to verify ranges for the user.
     *  
     */
//    public void PrintRanges(){
//    	// Get the ranges specified by the _rangesDan.txt file in the root directory
//    	double[] rangesArr = readDataRanges();
//    	
//    	// Print the ranges on 3 lines, as they're typically arranged that way in the text file.
//    	for(int i=0; i<rangesArr.length;i++){
//    		System.out.print(rangesArr[i] + " ");
//    		if (i==2 || i==5 || i==rangesArr.length){
//    			System.out.println();
//    		}
//    	}
//    	System.out.println("\n\n" + "Analyzing Raw Files (may take a minute)");
//    }
    
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
     * Print only mass range line from specified ranges array
     * @param rangeArr: the range array to be printed
     */
    public static void PrintMassRanges(double[] rangeArr){
    	for(int i=0; i<2;i++){
    		System.out.print(rangeArr[i] + " ");	
    	}
    	System.out.println("\n" + "Analyzing Raw Files (may take a minute)");
    }

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
    private static String generateDTMZSlice(String replicateID, File rawFile, int nFunction,
            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT, String dtmzRuleFilePath)
    {
        // A StringBUilder for the command line arguments
        StringBuilder cmdarray = new StringBuilder();
        // Get the preferred number of MZ bins
        int nPrefMaxMZbins = preferences.getnMaxMZBins();
        
        // MIGHT NEED TO CHANGE MAX MZ BINS TO ACTUAL MZ BINS FROM RANGE ARRAY
        
        // Build the commandline
        cmdarray.append(startMZ + " " + stopMZ + " " + nPrefMaxMZbins + " " + lineSep);
        cmdarray.append(startRT + " " + stopRT + " " + 1 + " " + lineSep); // Note the number of RT bins here is 1 as we are collapsing the RT range into a single bin
        cmdarray.append(startDT + " " + stopDT + " " + 200 + " " + lineSep);
        
        String path = root.getPath() + File.separator + replicateID + ".2dDTMZ";
        
        try
        {
            // Write out the 2DDTMZ ranges file...
            File dtmzRangeFile = new File(preferences.getLIB_PATH() + "\\ranges_2DDTMZ.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(dtmzRangeFile));
            writer.write(cmdarray.toString());
            writer.flush();
            writer.close();

            // Create a 2dDTMZ file for the imextract to write to
            File file = new File(path);
//            DataModel model = DataModelFactory.getDataModel();
//            DataProcess dp = model.getCurrentProcess();
//            if(dp != null && dp.getProcessType() != 0)
//                file.deleteOnExit();
//            if(file.exists())
//                System.out.println((new StringBuilder()).append("Deleted?").append(file.delete()).toString());

            // Clear the commandarray
            cmdarray.setLength(0);
            // Build the command for imextract
            cmdarray.append(exeFile.getCanonicalPath() + " ");
            cmdarray.append("-d ");
            cmdarray.append("\"" + rawFile.getPath() + "\" ");
            cmdarray.append("-f " + nFunction + " ");
            cmdarray.append("-o ");
            cmdarray.append("\"" + getRoot().getPath() + File.separator + replicateID + ".2dDTMZ\" ");
            cmdarray.append("-t ");
            cmdarray.append("mobilicube ");
            cmdarray.append("-p ");
            cmdarray.append("\"" + preferences.getLIB_PATH() +  "\\ranges_2DDTMZ.txt\"");
            if( dtmzRuleFilePath != null && dtmzRuleFilePath.length() > 0)
            {
                File dtmz = new File(dtmzRuleFilePath);
                if(dtmz.exists())
                {
                    cmdarray.append(" -pdtmz ");
                    cmdarray.append("\"" + dtmz.getAbsolutePath() + "\"");
                }
            }
//            if(roiRegion)
//            {
//                File dtmz = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("dtmz_selRegion.txt").toString());
//                if(dtmz.exists())
//                {
//                    cmdarray.append(" -pdtmz ");
//                    cmdarray.append("\"" + dtmz.getAbsolutePath() + "\"");
//                }
//                File rtdt = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("rtdt_selRegion.txt").toString());
//                if(rtdt.exists())
//                {
//                    cmdarray.append(" -prtdt ");
//                    cmdarray.append("\"" + rtdt.getAbsolutePath() + "\"");
//                }
//                File rtmz = new File((new StringBuilder()).append(getRoot().getPath()).append(File.separator).append("rtmz_selRegion.txt").toString());
//                if(rtmz.exists())
//                {
//                    cmdarray.append(" -prtmz ");
//                    cmdarray.append("\"" + rtmz.getAbsolutePath() + "\"");
//                }
//            }
            //log.writeMessage((new StringBuilder()).append("Generating DTMZ map: ").append(cmdarray).toString());
//            progMon.updateStatusMessage("Generating DTMZ map");
            runIMSExtract(cmdarray.toString());
        }
        catch(Exception ex)
        {
            //log.writeMessage(ex.getMessage());
            StackTraceElement trace[] = ex.getStackTrace();
            for(int i = 0; i < trace.length; i++)
            {
                StackTraceElement st = trace[i];
                //log.writeMessage(st.toString());
            }
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
     * Generate a mz data set. We sum over all masses and drift times to generate
     * a 1 dimensional dataset.
     */
    private static String generateMZ(String replicateID, File rawFile, int nFunction,
            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
            int mzBins, boolean bSelectRegion)
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
    //        progMon.updateStatusMessage("Generating spectrum");
            runIMSExtract(cmdarray.toString());
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        
        return path;
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
    

    public double[][] generateReplicateSpectrum(String rawPath, int nfunction, int slice, 
    		boolean selectRegion, double[] rangeValues) throws FileNotFoundException, IOException
    {
        File rawFile = new File(rawPath);
     
        String rawDataName = rawFile.getName();
    	// Get a unique id for the replicate chromatogram
    	// Edited to make it actually unique for multiple range files - added name of Range file to it
    	String replicateID = null;
    	if( slice > 0 )
    		replicateID = rawDataName + "_" + nfunction + "[" + slice + "]";
    	else
    		replicateID = rawDataName + "_" + nfunction + "[" + rangeValues[START_MZ] + "_" + rangeValues[STOP_MZ]  + "]";
    	// Generate a spectrum for the full data
    	String specPath = generateMZ(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion);

        double[][] data = getTraceData(specPath, nDATA_1D_MZ);
        
        return data;
    }
    
    public double[][] generateReplicateSpectrum(String rawPath, int nfunction, int slice, 
    		boolean selectRegion, double[] rangeValues, String rangeName) throws FileNotFoundException, IOException
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
    	String specPath = generateMZ(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion);

        double[][] data = getTraceData(specPath, nDATA_1D_MZ);
        
        return data;
    }
    
    public double[][] generateReplicateSpectrum(String rawPath, int nfunction, int slice, 
    		boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile) throws FileNotFoundException, IOException
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
    	String specPath = generateMZ(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion, ruleFile);

        double[][] data = getTraceData(specPath, nDATA_1D_MZ);
        
        return data;
    }
    
   
    
    
private double[][] generateReplicateMobiligram(String rawPath, int nfunction, int slice, 
		boolean selectRegion, double[] rangeValues, String rangeName)throws FileNotFoundException, IOException {
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
	String specPath = generateDT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion, null);

	double[][] data = getTraceData(specPath, nDATA_1D_DT);

	return data;
	}

// for rule mode
private double[][] generateReplicateMobiligram(String rawPath, int nfunction, int slice, 
		boolean selectRegion, double[] rangeValues, String rangeName, File ruleFile)throws FileNotFoundException, IOException {
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
	String specPath = generateDT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion, ruleFile);

	double[][] data = getTraceData(specPath, nDATA_1D_DT);

	return data;
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
        
        if( nType == nDATA_1D_RT )
        {
            nBins = nRTBins;
        }
        if( nType == nDATA_1D_DT )
        {
            nBins = nDTBins;
        }
        if( nType == nDATA_1D_MZ )
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
     * Initialises the binary maps for the data model
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

    /*
     * Method to extract MS data using the a lockmass spectrum in function 3 to correct the range file
     */
//    public void extractLockMassSpectrum(String rawDataFilePath, int function, String outputFilePath, double[] rangeVals, String rangeName) 
//    {
//        String lineSep = System.getProperty("line.separator");
//        
//        try 
//        {
//            double[][] msData = generateReplicateSpectrum(rawDataFilePath, function, 0, false, rangeVals, rangeName);
//    
//            File out = new File(outputFilePath);
//            BufferedWriter writer = new BufferedWriter(new FileWriter(out));
//            
//            // Added catch for null mobdata if there's no (or all 0's) data in the file
//            try{
//            	for( int ii=0; ii<msData.length; ii++ )
//                {
//                    double[] entry = msData[ii];
//                    writer.write(entry[0] + ", " + entry[1]);
//                    writer.write(lineSep);
//                }
//                writer.flush();
//                writer.close();
//            } catch (NullPointerException ex){
//            	// Warn the user that their data is no good
//            	System.out.println("\n" + "WARNING: " +
//            			"No data (or it's all 0's) in " + rawDataFilePath + ", function " + function
//            			 + "\n" + "Writing all 0's for this lock mass spectrum generation");
//            	
//            	//initialize an array of 0's for mobData, then continue as before
//            	msData = new double[(int) rangeVals[MZ_BINS]][2];
//            	for( int ii=0; ii<rangeVals[MZ_BINS]; ii++ )
//            	{           		
//            		double[] entry = msData[ii];
//            		writer.write(entry[0] + ", " + entry[1]);
//            		writer.write(lineSep);
//            	}
//            	writer.flush();
//            	writer.close(); 
//            }
//        } 
//        catch (FileNotFoundException ex) 
//        {
//            ex.printStackTrace();
//        } 
//        catch (IOException ex) 
//        {
//            ex.printStackTrace();
//        }
//    }
    
    /*
     * Method for writing 1D MS data for an output csv file based on a range file
     */
    public void extractSpectrum(String rawDataFilePath, int function, String outputFilePath, double[] rangeVals, String rangeName, File ruleFile) 
    {
        String lineSep = System.getProperty("line.separator");
        
        try 
        {
        	double[][] msData;
        	if (ruleFile == null){
                msData = generateReplicateSpectrum(rawDataFilePath, function, 0, false, rangeVals, rangeName);
        	} else {
        		msData = generateReplicateSpectrum(rawDataFilePath, function, 0, true, rangeVals, rangeName, ruleFile);
        	}
    
            File out = new File(outputFilePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(out));
            
            // Added catch for null mobdata if there's no (or all 0's) data in the file
            try{
            	for( int ii=0; ii<msData.length; ii++ )
                {
                    double[] entry = msData[ii];
                    writer.write(entry[0] + ", " + entry[1]);
                    writer.write(lineSep);
                }
                writer.flush();
                writer.close();
            } catch (NullPointerException ex){
            	// Warn the user that their data is no good
            	System.out.println("\n" + "WARNING: " +
            			"No data (or it's all 0's) in " + rawDataFilePath + ", function " + function
            			 + "\n" + "Writing all 0's for this mass spectrum");
            	
            	//initialize an array of 0's for msData, then continue as before
            	msData = new double[(int) rangeVals[MZ_BINS]][2];
            	for( int ii=0; ii<rangeVals[MZ_BINS]; ii++ )
            	{           		
            		double[] entry = msData[ii];
            		writer.write(entry[0] + ", " + entry[1]);
            		writer.write(lineSep);
            	}
            	writer.flush();
            	writer.close(); 
            }
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
    
    
    /*
     * Method for finding the lockmass peak and returning it - for use with Martin lab type data
     */
    public double extractSpectrumForLockmass(String rawDataFilePath, int function, double[] rangeVals, double minMZLockmass, double maxMZLockmass) 
    {
        double lockmassMZ = 0; 
        // First entry is mz, second is intensity
    	int MZ_DATA = 0;
    	int INT_DATA = 1;
    	
        try 
        {
            double[][] msData = generateReplicateSpectrum(rawDataFilePath, function, 0, false, rangeVals);
            
            // Loop through the replicate spectrum and find the highest intensity peak between 500 and 900
            double maxValue = 0;
            double mzValue = 0;
            for (double[] row : msData){
            	// Find highest intensity value in correct range
            	if (mzValue > 849.9){
            		boolean debug = true;
            	}
            	
            	if (row[INT_DATA] > maxValue){
            		if (row[MZ_DATA] > minMZLockmass && row[MZ_DATA] < maxMZLockmass){
            			maxValue = row[INT_DATA];
                		mzValue = row[MZ_DATA];
                		maxValue = row[INT_DATA];
            		}
            	}	
            }
            
            // Return the lockmass
            lockmassMZ = mzValue;
            System.out.println("\n" + "Lockmass mz: " + lockmassMZ + " intensity: " + maxValue);

        } 
        catch (FileNotFoundException ex) 
        {
            ex.printStackTrace();
        } 
        catch (IOException ex) 
        {
            ex.printStackTrace();
        }
        
        return lockmassMZ;
    }
    
    
    /**
     * Version for only writing data with no header
     */
    public void extractMobiligram(String rawDataFilePath, int function, String outputFilePath, double[] rangeVals, String rangeName) 
    {
        String lineSep = System.getProperty("line.separator");
        
        try 
        {
            double[][] mobData = generateReplicateMobiligram(rawDataFilePath, function, 0, false, rangeVals, rangeName);
    
            File out = new File(outputFilePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(out));
            
            // Added catch for null mobdata if there's no (or all 0's) data in the file
            try{
            	for( int ii=0; ii<mobData.length; ii++ )
                {
                    double[] entry = mobData[ii];
                    writer.write(entry[0] + ", " + entry[1]);
                    writer.write(lineSep);
                }
                writer.flush();
                writer.close();
            } catch (NullPointerException ex){
            	// Warn the user that their data is no good
//            	System.out.println("\n" + "WARNING: " +
//            			"No data (or it's all 0's) in " + rawDataFilePath + ", function " + function
//            			 + "\n" + "Writing all 0's for this collision voltage");
            	
            	//initialize an array of 0's for mobData, then continue as before
            	mobData = new double[200][2];
            	for( int ii=0; ii<rangeVals[DT_BINS]; ii++ )
            	{           		
            		double[] entry = mobData[ii];
            		writer.write(entry[0] + ", " + entry[1]);
            		writer.write(lineSep);
            	}
            	writer.flush();
            	writer.close(); 
            }
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
     * Second extract Mobiligram method that takes an additional input (the ranges file) and extracts
     * a specfic set of ranges.
     * @param rawDataFilePath: the system path to the raw data
     * @param function: the MassLynx function to be read
     * @param outputFilePath: the name of the output file
     * @param waveVel: the wave velocity of this mobiligram
     * @param waveHt: the wave height of this mobiligram
     * @param rangeVals: the ranges array used to generate this mobiligram slice
     * @param rangeName: the name of the range file used - passed to create new .1dDT file to prevent strange bugs on overwriting
     */
    public void extractMobiligram(String rawDataFilePath, int function, String outputFilePath, double waveVel, double waveHt, double[] rangeVals, String rangeName) 
    {
        String lineSep = System.getProperty("line.separator");
        
        try 
        {


            double[][] mobData = generateReplicateMobiligram(rawDataFilePath, function, 0, false, rangeVals, rangeName);
    
            File out = new File(outputFilePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(out));
            
            writer.write(Double.toString(waveVel));
            writer.write(",");
            writer.write(Double.toString(waveHt));
            writer.write(lineSep);
            
            // Added catch for null mobdata if there's no (or all 0's) data in the file
            try{
            	for( int ii=0; ii<mobData.length; ii++ )
                {
                    double[] entry = mobData[ii];
                    writer.write(entry[0] + ", " + entry[1]);
                    writer.write(lineSep);
                }
                writer.flush();
                writer.close();
            } catch (NullPointerException ex){
            	// Warn the user that their data is no good
            	System.out.println("\n" + "WARNING: " +
            			"No data (or it's all 0's) in " + rawDataFilePath + ", function " + function
            			 + "\n" + "Writing all 0's for this collision voltage");
            	
            	//initialize an array of 0's for mobData, then continue as before
            	mobData = new double[200][2];
            	for( int ii=0; ii<rangeVals[DT_BINS]; ii++ )
            	{           		
            		double[] entry = mobData[ii];
            		writer.write(entry[0] + ", " + entry[1]);
            		writer.write(lineSep);
            	}
            	writer.flush();
            	writer.close(); 
            }
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
     * Fourth (third is CIUGenDan Version) extract Mobiligram method that takes an additional input (the ranges file) and extracts
     * a specfic set of ranges. Saves all outputs to a single file specified by outputfilepath
     * Does NOT use collision energy
     * @param rawDataFilePath: the system path to the raw data
     * @param function: the MassLynx function to be read
     * @param outputFilePath: the name of the output file
     * @param collisionEnergy: the collision energy of this mobiligram
     * @param rangeVals: the ranges array used to generate this mobiligram slice
     * @param rangeName: the name of the range file used - passed to create new .1dDT file to prevent strange bugs on overwriting
     */
    public void extractMobiligramOneFile(ArrayList<DataVectorInfoObject> allFunctions, String outputFilePath, boolean ruleMode, File ruleFile) 
    {
    	int HEADER_LENGTH = 1;
    	String lineSep = System.getProperty("line.separator");
    	// Get info types to print from first function (they will be the same for all functions)
    	boolean[] infoTypes = allFunctions.get(0).getInfoTypes();
    	try 
    	{

    		// Collect mobData for all functions in the list
    		ArrayList<MobData> allMobData = new ArrayList<MobData>();

    		int counter = 1;
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
    			if (ruleMode){
        			data = generateReplicateMobiligram(rawDataFilePath, functionNum, 0, true, rangeVals, rangeName, ruleFile);
    			} else {
        			data = generateReplicateMobiligram(rawDataFilePath, functionNum, 0, false, rangeVals, rangeName);
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
    		
    		
    		
    		
    		// NEW TEST VERSION - looks like this actually works. Will test for a while before deleted old version
    		
    		// allMobData.get(0) is fine, but .getMobdata() returns null, so get length fails
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
    		
    		// OLD VERSION
    		
    		// Catch empty mobdata to ensure appropriate array dimensions (200 bins)
//    		if (allMobData.get(0).getMobdata().length == 0){
//    			for (int i = HEADER_LENGTH; i < 200 + HEADER_LENGTH; i++){
//    				lines.add(String.valueOf(i - HEADER_LENGTH + 1));
//    				lineIndex++;
//        		}	
//    		} else {
//    			// Mobdata is not empty, so write its contents to the array
//    			for (int i = HEADER_LENGTH; i < allMobData.get(0).getMobdata().length + HEADER_LENGTH; i++){
//    				lines.add(String.valueOf(allMobData.get(0).getMobdata()[lineIndex][0]));
//            		lineIndex++;
//        		}
//    		}
    		
    		
    		
    		
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
//    			lines[1] = lines[1] + Double.toString(data.getCollisionEnergy()) + ",";

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

	//    
	    
	    //    
	
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
	     * This method is used to collapse a mobility raw file
	     * retaining the drift time dimension. This will be used on infusion mobility data.
	     * The collapsed data is then peak detected
	     */
	    public void collapseData(File rawFile, int function, String collapasedPath)
	    {
	        try
	        {
	            // update the rawRanges.txt file with the data ranges from the selected raw file
	            getFullDataRanges(rawFile, function);
	            
	            File rangesFile = new File(preferences.getLIB_PATH() + "\\rawRanges.txt");
	
	//            String name = rawFile.getName();
	//            name = name.substring(0, name.lastIndexOf(".raw"));
	//            String collapsedDataName = name + "_dt.raw";
	//
	//            String collapsedRawDataPath = rawFile.getParent() + File.separator + collapsedDataName;
	
	            // Build the command to imextract to collpase the data retaining drift time
	            StringBuilder command = new StringBuilder();
	
	            command.append(exeFile.getCanonicalPath() + " ");
	            command.append("-d \"" + rawFile.getCanonicalPath() + "\" ");
	            command.append("-o \"" + collapasedPath + "\" ");
	            command.append("-f " + function + " ");
	            command.append("-t imrawdt ");
	            command.append("-ms 0 ");
	            command.append("-p ");
	            command.append("\"" + rangesFile.getCanonicalPath() + "\" ");
	
	//            progMon.updateStatusMessage("Collapsing " + rawFile.getName());
	            runIMSExtract(command.toString());
	//            progMon.updateStatusMessage("");
	        }
	        catch( Exception ex )
	        {
	            ex.printStackTrace();
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

	@SuppressWarnings("unused")
		private static String generateRT(String replicateID, File rawFile, int nFunction,
	            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
	            int rtBins, String dtmzRuleFilePath, int type)
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
	//            StackTraceElement[] trace = ex.getStackTrace();
	//            for( int i=0; i<trace.length; i++ )
	//            {
	//                StackTraceElement st = trace[i];
	////                _log.writeMessage(st.toString());
	//            }
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
	            cmdarray.append("-b ");
	            cmdarray.append(type + " ");
	            cmdarray.append("-p ");
	            cmdarray.append("\"" + preferences.getLIB_PATH() + "\\ranges_1DRT.txt\"");
	            if( dtmzRuleFilePath != null && dtmzRuleFilePath.length() > 0 )
	            {
	                File dtmz = new File(dtmzRuleFilePath);
	                if( dtmz.exists() )
	                {
	                    cmdarray.append(" -pdtmz ");
	                    cmdarray.append("\"" + dtmz.getAbsolutePath() + "\"");
	                }
	            }
	    //        _log.writeMessage(cmdarray);
	//            progMon.updateStatusMessage("Generating chromatogram");
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
	    @SuppressWarnings("unused")
		private static String generateRT(String replicateID, File rawFile, int nFunction,
	            double startMZ, double stopMZ, double startRT, double stopRT, double startDT, double stopDT,
	            int rtBins, boolean bSelectRegion)
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
	    
	    // OLD VERSION
	     /**
	     * @param args the command line arguments
	     */
//	    public void extractMobiligram(String rawDataFilePath, int function, String outputFilePath, double collisionEnergy) 
//	    {
//	        String lineSep = System.getProperty("line.separator");
//	        
//	        try 
//	        {
//	            getFullDataRanges(new File(rawDataFilePath), function);
//	            double[] rangeVals = readDataRanges();
	//
//	            double[][] mobData = generateReplicateMobiligram(rawDataFilePath, function, 0, false, rangeVals);
//	            File out = new File(outputFilePath);
//	            BufferedWriter writer = new BufferedWriter(new FileWriter(out));
//	            
//	            writer.write(Double.toString(collisionEnergy));
//	            writer.write(lineSep);
//	            
//	            //catch for null mobData - if the datafile is all 0's, mobData doesn't get initialized
//	            try{
//	            	for( int ii=0; ii<mobData.length; ii++ )
//	            	{
//	            		double[] entry = mobData[ii];
//	            		writer.write(entry[0] + ", " + entry[1]);
//	            		writer.write(lineSep);
//	            	}
//	            	writer.flush();
//	            	writer.close();
//	            } catch(NullPointerException e){
//	            	// Warn the user that their data is no good
//	            	System.out.println("\n" + "WARNING: " +
//	            			"No data (or it's all 0's) in " + rawDataFilePath + ", function " + function
//	            			 + "\n" + "Writing all 0's for this collision voltage");
//	            	
//	            	//initialize an array of 0's for mobData, then continue as before
//	            	mobData = new double[200][2];
//	            	for( int ii=0; ii<rangeVals[DT_BINS]; ii++ )
//	            	{           		
//	            		double[] entry = mobData[ii];
//	            		writer.write(entry[0] + ", " + entry[1]);
//	            		writer.write(lineSep);
//	            	}
//	            	writer.flush();
//	            	writer.close();
//	            }
//	        } 
//	        catch (FileNotFoundException ex) 
//	        {
//	            ex.printStackTrace();
//	        } 
//	        catch (IOException ex) 
//	        {
//	            ex.printStackTrace();
//	        }
//	    }
	//    
//	    /**
//	     * Clean unwanted temp file from lib folder
//	     */
//	    public static void cleanLib()
//	    {
//	        File libDir = new File(preferences.getLIB_PATH());
//	        File[] txtFiles = libDir.listFiles(new TxtFileFilter());
//	        for( File f : txtFiles )
//	            f.delete();
//	    }
	//    
//	        /**
//	     * Clean unwanted temp file from root folder
//	     */
//	    public static void cleanRoot()
//	    {
//	        File rootDir = getRoot();
//	        File[] allFiles = rootDir.listFiles();
//	        for( File f : allFiles )
//	            f.delete();
//	    }
	//
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
		//    public double[][] generateReplicateHeatmap( String rawPath, int nfunction, double startRT, double stopRT, int slice, String dtmzRuleFilePath )
//	    {
//	        File rawFile = new File(rawPath);
//	        // Get the full data ranges
//	        //getFullDataRanges(rawFile, nfunction);
//	        // read the full data ranges
//	        double[] rangeValues = readDataRanges();
	//
//	        String rawDataName = rawFile.getName();
//	        // Get a unique id for the replicate chromatogram
//	        String replicateID = rawDataName + "_" + nfunction + "[" + slice + "]";
//	        // Generate a chromatogram for the full data
//	        String dtmzPath = null;
//	        if( startRT < 0 && stopRT < 0 )
//	            dtmzPath = generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], minRT, maxRT, rangeValues[6], rangeValues[7], dtmzRuleFilePath);
//	        else
//	            dtmzPath = generateDTMZSlice(replicateID, rawFile, nfunction, rangeValues[0], rangeValues[1], startRT, stopRT, rangeValues[6], rangeValues[7], dtmzRuleFilePath);
//	        
//	        double[][] data = getHeatMapData(dtmzPath);
//	        
//	        return data;
//	    }
	//    
//	    /**
//	     * Returns a a 2-dimensional array of data points for the DTMZ heat map specified by the path
//	     * @param data 
//	     */
//	    private static double[][] getHeatMapData(String path)
//	    {
//	        /* Open the binary file as channel */
//	        File binFile = new File(path);
	//
//	        double dataGrid[][] = (double[][])null;
//	        double fRange = 0.0D;
//	        double fXOffset = 0.0D;
//	        double fYOffset = 0.0D;
//	        double fSum = 0.0D;
//	        if(!binFile.exists())
//	        {
//	            return null;
//	        }
//	        try
//	        {
//	            RandomAccessFile rafFile = new RandomAccessFile(binFile, "r");
//	            FileChannel channel = rafFile.getChannel();
//	            ByteBuffer bb = ByteBuffer.allocate(0x4c4b40);
//	            int position = 0;
//	            channel.read(bb, position);
//	            bb.order(ByteOrder.LITTLE_ENDIAN);
//	            bb.position(position);
//	            int nXDimension = preferences.getnMaxDTBins();
//	            int nYDimension = preferences.getnMaxMZBins();
//	            int nMZBins = bb.getInt();
//	            int nRTBins = bb.getInt();
//	            int nDTBins = bb.getInt();
	//
//	            if(nXDimension != nDTBins)
//	                nXDimension = nDTBins;
//	            if(nYDimension != nMZBins)
//	                nYDimension = nMZBins;
	//
//	            int nXSize = 0;
//	            int nYSize = 0;
//	            double fXRes = 0.0D;
//	            double fYRes = 0.0D;
//	            fRange = 200.0 - 0.0;
//	            fXRes = (double)nXDimension / fRange;
//	            nXSize = (int)((double)(fRange) * fXRes + 0.5D);
//	            fRange = 1900.0432 - 99.9984;
//	            fYRes = (double)nYDimension / fRange;
//	            nYSize = (int)((double)(fRange) * fYRes + 0.5D);
	//
//	            fXOffset = 0.0;
//	            fYOffset = 99.9984;
	//
//	            zHigh = Double.MIN_VALUE;
//	            zLow = Double.MAX_VALUE;
//	            
//	            int nHighX = -1;
//	            int nHighY = -1;
//	            int loops = 0;
//	            dataGrid = new double[nXDimension][nYDimension];
//	            for(int ii = 0; ii < nXDimension; ii++)
//	            {
//	                for(int jj = 0; jj < nYDimension; jj++)
//	                {
//	                    if(bb.position() >= bb.limit())
//	                    {
//	                        loops++;
//	                        bb.clear();
//	                        position = bb.limit() * loops;
//	                        channel.read(bb, position);
//	                        bb.order(ByteOrder.LITTLE_ENDIAN);
//	                        bb.position(0);
//	                    }
//	                    int nCount = bb.getInt();
//	                    
//	                    if(nCount < 0)
//	                        continue;
	//
////	                    if(preferences.getUseLogScale())
////	                        fSum = Math.log((double)nCount + 1.0D);
////	                    else
////	                    if(preferences.getUseSquareRootScale())
////	                        fSum = Math.sqrt((double)nCount + 1.0D);
////	                    else
////	                        fSum = nCount;
//	                    fSum = Math.log((double)nCount + 1.0D);
//	                    
//	                    dataGrid[ii][jj] = nCount;
//	                    //dataGrid[ii][jj] = fSum;
//	                    
//	                    if(fSum > getzHigh())
//	                    {
//	                        zHigh = fSum;
//	                        nHighX = ii;
//	                        nHighY = jj;
//	                    }
//	                    if(fSum < getzLow())
//	                        zLow = fSum;
//	                }
	//
//	            }
	//
//	            bb.clear();
//	            channel.close();
//	            rafFile.close();
//	        }
//	        catch(Exception ex)
//	        {
////	            log.writeMessage(ex.getMessage());
//	            StackTraceElement trace[] = ex.getStackTrace();
//	            for(int i = 0; i < trace.length; i++)
//	            {
//	                StackTraceElement st = trace[i];
////	                log.writeMessage(st.toString());
//	            }
//	            return null;
//	        }
//	        return dataGrid;
//	    }
	//    
//	    /**
//	     * 
//	     * @param img
//	     * @return 
//	     */
//	    public static double[][] getHeatMapDataFromImage(BufferedImage img)
//	    {
//	       int nXDimension = img.getWidth();
//	       int nYDimension = img.getHeight();
//	       
//	       double[][] dataGrid = new double[nXDimension][nYDimension];
//	        for(int ii = 0; ii < nXDimension; ii++)
//	        {
//	            for(int jj = 0; jj < nYDimension; jj++)
//	            {
//	                dataGrid[ii][jj] = img.getRGB(ii, jj);
//	            }
//	        }
//	       
//	       return dataGrid;
//	    }
	//    
	    // Old version
//	    public double[][] generateReplicateMobiligram(String rawPath, int nfunction, int slice, boolean selectRegion, double[] rangeValues) throws FileNotFoundException, IOException
//	    {
//	        File rawFile = new File(rawPath);
//	        // Get the full data ranges
////	        getFullDataRanges(rawFile, nfunction);
////	        // read the full data ranges
////	        double[] rangeValues = readDataRanges();
	//
//	        String rawDataName = rawFile.getName();
//	        // Get a unique id for the replicate chromatogram
//	        String replicateID = null;
//	        if( slice > 0 )
//	            replicateID = rawDataName + "_" + nfunction + "[" + slice + "]";
//	        else
//	            replicateID = rawDataName + "_" + nfunction + "[" + rangeValues[START_DT] + "_" + rangeValues[STOP_DT]  + "]";
//	        // Generate a spectrum for the full data
//	        String specPath = generateDT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.DT_BINS], selectRegion);
//	        
//	        double[][] data = getTraceData(specPath, nDATA_1D_DT);
//	        
//	        return data;
//	    }
	//    
//	    /**
//	     * Generates the chromatogram trace for the specified raw data and function and data slice
//	     * @param rawFile - the target raw file
//	     * @param nfunction - the target data function
//	     * @param slice - the target data slice
//	     * @return - a 2D array of the chromatogram trace data
//	     * @throws FileNotFoundException
//	     * @throws IOException 
//	     */
//	    public double[][] generateReplicateChromatogram(String rawPath, int nfunction, int slice, boolean selectRegion, double[] rangeValues) throws FileNotFoundException, IOException
//	    {
//	        File rawFile = new File(rawPath);
//	        // Get the full data ranges
//	        //getFullDataRanges(rawFile, nfunction);
//	        // read the full data ranges
////	        double[] rangeValues = readDataRanges();
	//
//	        String rawDataName = rawFile.getName();
//	        // Get a unique id for the replicate chromatogram
//	        String replicateID = null;
//	        if( slice > 0 )
//	            replicateID = rawDataName + "_" + nfunction + "[" + slice + "]";
//	        else
//	            replicateID = rawDataName + "_" + nfunction + "[" + rangeValues[START_RT] + "_" + rangeValues[STOP_RT]  + "]";
//	        // Generate a chromatogram for the full data
//	        String chromPath = generateRT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.RT_BINS], selectRegion);
	//
//	        double[][] data = getTraceData(chromPath, nDATA_1D_RT);
//	        
//	        return data;
//	    }
	//    
//	    public double[][] generateReplicateChromatogram(String rawPath, int nfunction, int slice, String dtmzRuleFilePath, int type) throws FileNotFoundException, IOException
//	    {
//	        File rawFile = new File(rawPath);
//	        // Get the full data ranges
//	        getFullDataRanges(rawFile, nfunction);
//	        // read the full data ranges
//	        double[] rangeValues = readDataRanges();
	//
//	        String rawDataName = rawFile.getName();
//	        // Get a unique id for the replicate chromatogram
//	        String replicateID = null;
//	        if( slice > 0 )
//	            replicateID = rawDataName + "_" + nfunction + "[" + slice + "]";
//	        else
//	            replicateID = rawDataName + "_" + nfunction + "[" + rangeValues[START_RT] + "_" + rangeValues[STOP_RT]  + "]";
//	        
//	        
//	        // Generate a chromatogram for the full data
//	        String chromPath = generateRT(replicateID, rawFile, nfunction, rangeValues[IMExtractRunner.START_MZ], rangeValues[IMExtractRunner.STOP_MZ], rangeValues[IMExtractRunner.START_RT], rangeValues[IMExtractRunner.STOP_RT], rangeValues[IMExtractRunner.START_DT], rangeValues[IMExtractRunner.STOP_DT], (int)rangeValues[IMExtractRunner.RT_BINS], dtmzRuleFilePath, type);
	//
//	        double[][] data = getTraceData(chromPath, nDATA_1D_RT);
//	        
//	        return data;
//	    }
	    /**
	     * Reads the data ranges from the _rangesDan.txt file
	     * @return - an array of the initial data ranges
	     */
//	    public static double[] readDataRanges()
//	    {
//	        // Data reader
//	        BufferedReader reader = null;
//	        String line = null;
//	        // array to store data values
//	        double[] rangesArr = new double[9];
//	        // value counter
//	        int valueCounter = 0;
	//
//	        try
//	        {
//	            
//	            File rangesTxt = new File(getRoot() + File.separator + "_rangesDan.txt");
//	            reader = new BufferedReader(new FileReader(rangesTxt));
//	            while((line = reader.readLine()) != null)
//	            {
//	                String[] splits = line.split(" ");
//	                for( String split : splits )
//	                {
//	                    double d = Double.parseDouble(split);
//	                    //rangesArr[valueCounter] = d;
//	                    switch( valueCounter )
//	                    {
//	                        case START_MZ:
//	                            minMZ = Math.floor(d);
//	                            rangesArr[valueCounter] = minMZ;
//	                            break;
//	                            
//	                        case STOP_MZ:
//	                            maxMZ = Math.ceil(d);
//	                            rangesArr[valueCounter] = maxMZ;
//	                            break;
//	                            
//	                        case MZ_BINS:
//	                            mzBins = d;
//	                            rangesArr[valueCounter] = mzBins;
//	                            break;
//	                            
//	                        case START_RT:
//	                            minRT = Math.floor(d);
//	                            rangesArr[valueCounter] = minRT;
//	                            break;
//	                            
//	                        case STOP_RT:
//	                            maxRT = Math.ceil(d);
//	                            rangesArr[valueCounter] = maxRT;
//	                            break;
//	                            
//	                        case RT_BINS:
//	                            rtBins = d;
//	                            rangesArr[valueCounter] = rtBins;
//	                            break;
//	                            
//	                        case START_DT:
//	                            minDT = Math.floor(d);
//	                            rangesArr[valueCounter] = minDT;
//	                            break;
//	                            
//	                        case STOP_DT:
//	                            maxDT = Math.ceil(d);
//	                            rangesArr[valueCounter] = maxDT;
//	                            break;
//	                                                        
//	                        case DT_BINS:
//	                            dtBins = d;
//	                            rangesArr[valueCounter] = dtBins;
//	                            break;
//	                                    
//	                    }
//	                    valueCounter++;
//	                }
//	            }
//	        }
//	        catch (FileNotFoundException ex)
//	        {
//	            //Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
//	            System.out.println("\n" + "No ranges file found in root directory! There must be a file named"
//	            		+ " '_rangesDan.txt' in your 'C:/CIUGen/root' folder for this method to work.");
//	            System.out.println("Alternately, use the 'Run Multiple Ranges' button to choose your desired range file");
//	            System.out.println("\n" + "Ending Program");
//	            System.exit(0);
//	        }
//	        catch (IOException iox)
//	        {
//	            Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, iox);
//	        }
//	        finally
//	        {
//	            try
//	            {
//	                reader.close();
//	            }
//	            catch (IOException ex)
//	            {
//	                Logger.getLogger(IMExtractRunner.class.getName()).log(Level.SEVERE, null, ex);
//	            }
//	        }
//	        //EDIT - print the ranges used to the terminal
//	        for (int i=0; i<rangesArr.length;i++){
//	        	//System.out.print(rangesArr[i]+" ");
//	        }
//	        //System.out.println();
//	        return rangesArr;
//	    }
}
