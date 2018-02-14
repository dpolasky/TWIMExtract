package ciugen.imextract;

/**
 *  This file is part of TWIMExtract
 *  
 * Class to hold all information from the data vector/table used in CIUGEN UI. Each object represents
 * a single analysis (what used to be a single csv file), so each object is one function in MassLynx
 * @author Dan
 *
 */
public class DataVectorInfoObject {
	private String rawDataPath;
	private int function;
	private boolean selected;
	
	private double coneCV;
	private double trapCV;
	private double transfCV;
	private double waveHeight;
	private double waveVel;
	
	private double[] rangeVals;
	
	private String rangeName;
	private String rawDataName;
	private boolean[] infoTypes;
	
	private double functionStartTime;

	
	//(rawDataPath, rawName,function,selected,lockmass,conecv,trapcv,transfcv,wh,wv,rangesArr,rangeFileName
	
	public DataVectorInfoObject(String myRawDataPath, String myRawName, int myFunction, boolean mySelected,
			double myConeCV, double myTrapCV, double myTransfCV, double myWH, double myWV,
			double[] myRangesArr, String myRangeFileName, boolean[] myInfoTypes, double myStartTime){
		
		this.setRawDataPath(myRawDataPath);
		this.setRawDataName(myRawName);
		this.setFunction(myFunction);
		this.setSelected(mySelected);
		
		this.setConeCV(myConeCV);
		this.setCollisionEnergy(myTrapCV);
		this.setTransfCV(myTransfCV);
		this.setWaveHeight(myWH);
		this.setWaveVel(myWV);
		
		this.setRangeVals(myRangesArr);
		this.setRangeName(myRangeFileName);
		this.setInfoTypes(myInfoTypes);
		
		this.setFunctionStartTime(myStartTime);
//		this.adjustRangeVals();
	}
	
	public void adjustRangeVals(){
		/*
		 * Adjust the provided RT range to the actual start time of the function to allow 
		 * extraction of subsets of data from multi-function files. 
		 */
		double newStartTime = this.functionStartTime + this.rangeVals[IMExtractRunner.START_RT];
		double newEndTime = this.functionStartTime + this.rangeVals[IMExtractRunner.STOP_RT];
		
		double[] newRanges = this.getRangeVals();
		newRanges[IMExtractRunner.START_RT] = newStartTime;
		newRanges[IMExtractRunner.STOP_RT] = newEndTime;
		this.setRangeVals(newRanges);
		
//		this.rangeVals[IMExtractRunner.START_RT] = newStartTime;
//		this.rangeVals[IMExtractRunner.STOP_RT] = newEndTime;
	}
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getRawDataPath() {
		return rawDataPath;
	}

	public void setRawDataPath(String rawDataPath) {
		this.rawDataPath = rawDataPath;
	}

	public int getFunction() {
		return function;
	}

	public void setFunction(int function) {
		this.function = function;
	}

	public double getCollisionEnergy() {
		return trapCV;
	}

	public void setCollisionEnergy(double collisionEnergy) {
		this.trapCV = collisionEnergy;
	}

	public double[] getRangeVals() {
		return rangeVals;
	}

	public void setRangeVals(double[] rangeVals) {
		this.rangeVals = rangeVals;
	}

	public double getWaveHeight() {
		return waveHeight;
	}

	public void setWaveHeight(double waveHeight) {
		this.waveHeight = waveHeight;
	}

	public double getWaveVel() {
		return waveVel;
	}

	public void setWaveVel(double waveVel) {
		this.waveVel = waveVel;
	}


	public String getRangeName() {
		return rangeName;
	}


	public void setRangeName(String rangeName) {
		this.rangeName = rangeName;
	}


	public String getRawDataName() {
		return rawDataName;
	}


	public void setRawDataName(String rawDataName) {
		this.rawDataName = rawDataName;
	}

	public double getConeCV() {
		return coneCV;
	}

	public void setConeCV(double coneCV) {
		this.coneCV = coneCV;
	}

	public double getTrapCV() {
		return trapCV;
	}

	public void setTrapCV(double trapCV) {
		this.trapCV = trapCV;
	}

	public double getTransfCV() {
		return transfCV;
	}

	public void setTransfCV(double transfCV) {
		this.transfCV = transfCV;
	}

	public boolean[] getInfoTypes() {
		return infoTypes;
	}

	public void setInfoTypes(boolean[] infoTypes) {
		this.infoTypes = infoTypes;
	}

	public double getFunctionStartTime() {
		return functionStartTime;
	}

	public void setFunctionStartTime(double functionStartTime) {
		this.functionStartTime = functionStartTime;
	}
	

}
