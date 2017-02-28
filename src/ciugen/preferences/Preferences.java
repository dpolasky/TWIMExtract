package ciugen.preferences;

/**
 * This file is part of TWIMExtract
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Preferences
{
  private static Preferences m_instance;
  private String strVersion;
  private int nBuildNumber;
  private int nMaxDTBins;
  private int nMaxMZBins;

  private String m_strWorkingDir;
  private File xml;
  private File txtconfig;
  
  private String rawDir;
  private String rangeDir;
  private String outDir;
  private String batchDir;
  private String ruleDir;
  
  private final String CIUGEN_HOME = "C:\\TWIMExtract";	
//  private final String CIUGEN_HOME = "C:\\CIUGen";	

  private String LIB_PATH;
  private String BIN_PATH;
  private String CONFIG_PATH;
  private String ROOT_PATH;
  private String HELP_PATH;
  private String EXAMPLE_PATH;
  private String RULE_EX_PATH;
  
  private static final String RAW_TITLE = "Raw_Directory";
  private static final String RANGE_TITLE = "Range_Directory";
  private static final String OUT_TITLE = "Output_Directory";
  private static final String BATCH_TITLE = "Batch_Directory";
  private static final String RULE_TITLE = "Rule_Directory";
  
  public boolean haveConfig;
  
  private Preferences()
  {
    this.BIN_PATH = (this.CIUGEN_HOME + "\\bin");
    this.LIB_PATH = (this.CIUGEN_HOME + "\\lib");
    this.CONFIG_PATH = (this.CIUGEN_HOME + "\\config");
    this.m_strWorkingDir = System.getProperty("user.home");	
    this.xml = new File(this.CONFIG_PATH + "\\preferences.xml");
    this.ROOT_PATH = (this.CIUGEN_HOME + "\\root");
    this.txtconfig = new File (this.CONFIG_PATH + "\\config.txt");
    this.HELP_PATH = (this.CIUGEN_HOME + "\\TWIMExtract_help.txt");
    this.EXAMPLE_PATH = (this.CIUGEN_HOME + "\\_EXAMPLES\\Range_and_Rule_Examples\\RangeExample.txt");
    this.setRULE_EX_PATH((this.CIUGEN_HOME + "\\_EXAMPLES\\Range_and_Rule_Examples\\How to generate Rule Files (Driftscope).txt"));
    
    // Read any existing configuration preferences. Store whether or not a config file was found for later reference
    haveConfig = readConfig(txtconfig);
  }
 
  /**
   * Reads in the information from the config file, to be called in the constructor. Handles a lack of 
   * config file by initializing to default values. Returns True if config file exists, false if no config
   * file could be found or if an error occurred. 
   */
  private boolean readConfig(File configFile){
	  // Read in information from existing config file if found, otherwise initialize to defaults
	  try {
		  BufferedReader reader = new BufferedReader(new FileReader(configFile));
		  String line = reader.readLine();

		  while (line != null){
			  String[] splits = line.split(",");
			  if (line.startsWith(RAW_TITLE)){
				  rawDir = splits[1];
			  } else if (line.startsWith(RANGE_TITLE)){
				  rangeDir = splits[1];
			  } else if (line.startsWith(OUT_TITLE)){
				  outDir = splits[1];
			  } else if (line.startsWith(BATCH_TITLE)){
				  batchDir = splits[1];
			  } else if (line.startsWith(RULE_TITLE)){
				  ruleDir = splits[1];
			  }
			  line = reader.readLine();
		  }
		  
		  // Catch partial config files
		  if (rawDir == null){
			  rawDir = m_strWorkingDir;
		  } if (rangeDir == null){
			  rangeDir =  this.CIUGEN_HOME + "\\_EXAMPLES\\Range_and_Rule_Files";
		  } if (outDir == null){
			  outDir =  m_strWorkingDir;
		  } if (batchDir == null){
			  batchDir = this.CIUGEN_HOME + "\\_EXAMPLES\\Batches";
		  } if (ruleDir == null){
			  ruleDir = this.CIUGEN_HOME + "\\_EXAMPLES\\Range_and_Rule_Files";
		  }
		  reader.close();
		  return true;
		  
	  } catch (FileNotFoundException ex){
		  System.out.println("No config file found, initializing to defaults");
		  rawDir = m_strWorkingDir;
		  rangeDir =  m_strWorkingDir;
		  outDir =  m_strWorkingDir;
		  batchDir = m_strWorkingDir;
		  ruleDir = m_strWorkingDir;
		  return false;
		  
	  } catch (IOException ex){
		  rawDir = m_strWorkingDir;
		  rangeDir =  m_strWorkingDir;
		  outDir =  m_strWorkingDir;
		  batchDir = m_strWorkingDir;
		  ruleDir = m_strWorkingDir;
		  ex.printStackTrace();
		  return false;
	  }
  }

  /**
   * Method to write the user's preferences to the config text file in the CIUGEN/Config folder
   * @param rawDir
   * @param rangeDir
   * @param outDir
   */
  public void writeConfig(){
	  try {
		BufferedWriter writer = new BufferedWriter(new FileWriter(txtconfig));
		String linesep = System.getProperty("line.separator");
		
		ArrayList<String> lines = new ArrayList<String>();
		String rawLine = RAW_TITLE + "," + rawDir;
		String rangeLine = RANGE_TITLE + "," + rangeDir;
		String outLine = OUT_TITLE + "," + outDir;
		String batchLine = BATCH_TITLE + "," + batchDir;
		String ruleLine = RULE_TITLE + "," + ruleDir;
		lines.add(rawLine);
		lines.add(rangeLine);
		lines.add(outLine);
		lines.add(batchLine);
		lines.add(ruleLine);
		
		// Print all config lines to the text file
		for (String line : lines){
			writer.write(line);
			writer.write(linesep);
		}
		
		writer.flush();
		writer.close();
		
		// Note that we now have a config file available for reading
		haveConfig = true;
		
	} catch (IOException e) {
		e.printStackTrace();
	}	  
  }
  
  public static Preferences getInstance()
  {
    if (m_instance == null) {
      m_instance = new Preferences();
    }
    return m_instance;
  }
  
  public String getStrVersion()
  {
    return this.strVersion;
  }
  
  public void setStrVersion(String strVersion)
  {
    this.strVersion = strVersion;
  }
  
  public int getnBuildNumber()
  {
    return this.nBuildNumber;
  }
  
  public void setnBuildNumber(int nBuildNumber)
  {
    this.nBuildNumber = nBuildNumber;
  }
  
  public String getM_strWorkingDir()
  {
    return this.m_strWorkingDir;
  }
  
  public void setM_strWorkingDir(String m_strWorkingDir)
  {
    this.m_strWorkingDir = m_strWorkingDir;
  }
  
  public File getXml()
  {
    return this.xml;
  }
  
  public void setXml(File xml)
  {
    this.xml = xml;
  }
  
  public int getnMaxDTBins()
  {
    return this.nMaxDTBins;
  }
  
  public void setnMaxDTBins(int nMaxDTBins)
  {
    this.nMaxDTBins = nMaxDTBins;
  }
  
  public String getCIUGEN_HOME()
  {
    return this.CIUGEN_HOME;
  }
  
  public String getLIB_PATH()
  {
    return this.LIB_PATH;
  }
  
  public String getBIN_PATH()
  {
    return this.BIN_PATH;
  }
  
  public String getCONFIG_PATH()
  {
    return this.CONFIG_PATH;
  }
  public String getROOT_PATH(){
	  return this.ROOT_PATH;
  }

public String getRawDir() {
	return rawDir;
}

public void setRawDir(String rawDir) {
	this.rawDir = rawDir;
}

public String getRangeDir() {
	return rangeDir;
}

public void setRangeDir(String rangeDir) {
	this.rangeDir = rangeDir;
}

public String getOutDir() {
	return outDir;
}

public void setOutDir(String outDir) {
	this.outDir = outDir;
}

public String getBatchDir() {
	return batchDir;
}

public void setBatchDir(String batchDir) {
	this.batchDir = batchDir;
}

public int getnMaxMZBins() {
	// TODO Auto-generated method stub
	return this.nMaxMZBins;
}
public void setnMaxMZBins(int bins){
	this.nMaxMZBins = bins;
}

public String getRuleDir() {
	return ruleDir;
}

public void setRuleDir(String ruleDir) {
	this.ruleDir = ruleDir;
}

public String getHELP_PATH() {
	return HELP_PATH;
}

public void setHELP_PATH(String hELP_PATH) {
	HELP_PATH = hELP_PATH;
}

public String getEXAMPLE_PATH() {
	return EXAMPLE_PATH;
}

public void setEXAMPLE_PATH(String eXAMPLE_PATH) {
	EXAMPLE_PATH = eXAMPLE_PATH;
}

public String getRULE_EX_PATH() {
	return RULE_EX_PATH;
}

public void setRULE_EX_PATH(String rULE_EX_PATH) {
	RULE_EX_PATH = rULE_EX_PATH;
}
}
