package ciugen.imextract;

import java.util.ArrayList;

/**
 * Class for saving extracted data. Holds an ArrayList of MobData containers for the data to output to
 * a single file and the full file path at which to save the extracted data. 
 * @author dpolasky
 *
 */
public class ExtractSave {
	
	private ArrayList<MobData> mobData;
	private String outputFilePath;
	private int extractionMode;
	private DataVectorInfoObject referenceFunction;
	private boolean DT_in_MS;
	
	public ExtractSave(ArrayList<MobData> mobData, String outputFilePath, int extractionMode, DataVectorInfoObject refFunc, boolean dt_in_ms){
		this.setMobData(mobData);
		this.setOutputFilePath(outputFilePath);
		this.setExtractionMode(extractionMode);
		this.setReferenceFunction(refFunc);
		this.setDT_in_MS(dt_in_ms);
	}
	
	public ArrayList<MobData> getMobData() {
		return mobData;
	}
	public void setMobData(ArrayList<MobData> mobData) {
		this.mobData = mobData;
	}
	public String getOutputFilePath() {
		return outputFilePath;
	}
	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public int getExtractionMode() {
		return extractionMode;
	}

	public void setExtractionMode(int extractionMode) {
		this.extractionMode = extractionMode;
	}

	public DataVectorInfoObject getReferenceFunction() {
		return referenceFunction;
	}

	public void setReferenceFunction(DataVectorInfoObject referenceFunction) {
		this.referenceFunction = referenceFunction;
	}

	public boolean isDT_in_MS() {
		return DT_in_MS;
	}

	public void setDT_in_MS(boolean dt_in_ms) {
		this.DT_in_MS = dt_in_ms;
	}
	
}
