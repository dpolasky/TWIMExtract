package ciugen.imextract;

/**
 *  * This file is part of TWIMExtract
 *  
 * Object to store mobility data and header information for writing to file
 * @author Dan
 *
 */
public class MobData {
	private double[][] mobdata;
	private String rawFileName;
	private double trapCV;
	private double coneCV;
	private double transferCV;
	private double waveHeight;
	private double waveVelocity;
	private String rangeName;
	
	public static final int TRAP_CV = 0;
	public static final int CONE_CV = 1;
	public static final int TRANSF_CV = 2;
	public static final int WAVE_HT = 3;
	public static final int WAVE_VEL = 4;
	
	
//	public int compareTo(MobData d){
//		if (compareField == TRAP_CV){
//			return (trapCV - d.trapCV);
//		} else if (compareField == CONE_CV){
//			
//		} else if (compareField == TRANSF_CV){
//			
//		} else if (compareField == WAVE_HT){
//			
//		} else if (compareField == WAVE_VEL){
//			
//		} 
//		// return trap comparison if not provided
//		return (trapCV - d.trapCV);
//	}
//	
//	@Override
//	public int compareTo(Object arg0) {
//		// TODO Auto-generated method stub
//		return 0;
//	}

	public MobData(double[][] myData, String myName, double myCE){
		this.setCollisionEnergy(myCE);
		this.setMobdata(myData);
		this.setRawFileName(myName);
	}
	
	public MobData(double[][] myData, String myName, String myRange){
		this.setMobdata(myData);
		this.setRawFileName(myName);
		this.setRangeName(myRange);
	}

	public MobData(double[][] myData, String myName,String myRangeName,double myConeCV, double myTrapCV, double myTransfCV, double myHt, double myVel){
		this.setMobdata(myData);
		
		this.setCollisionEnergy(myTrapCV);
		this.setRangeName(myRangeName);
		this.setMobdata(myData);
		this.setRawFileName(myName);
		this.setWaveHeight(myHt);
		this.setWaveVelocity(myVel);
		this.setConeCV(myConeCV);
		this.setTransferCV(myTransfCV);
	}
	
	public double[][] getMobdata() {
		return mobdata;
	}

	public void setMobdata(double[][] mobdata) {
		this.mobdata = mobdata;
	}

	public String getRawFileName() {
		return rawFileName;
	}

	public void setRawFileName(String rawFileName) {
		this.rawFileName = rawFileName;
	}

	public double getCollisionEnergy() {
		return trapCV;
	}

	public void setCollisionEnergy(double collisionEnergy) {
		this.trapCV = collisionEnergy;
	}

	public double getWaveHeight() {
		return waveHeight;
	}

	public void setWaveHeight(double waveHeight) {
		this.waveHeight = waveHeight;
	}

	public double getWaveVelocity() {
		return waveVelocity;
	}

	public void setWaveVelocity(double waveVelocity) {
		this.waveVelocity = waveVelocity;
	}

	public String getRangeName() {
		return rangeName;
	}

	public void setRangeName(String rangeName) {
		this.rangeName = rangeName;
	}

	public double getTrapCV() {
		return trapCV;
	}

	public void setTrapCV(double trapCV) {
		this.trapCV = trapCV;
	}

	public double getConeCV() {
		return coneCV;
	}

	public void setConeCV(double coneCV) {
		this.coneCV = coneCV;
	}

	public double getTransferCV() {
		return transferCV;
	}

	public void setTransferCV(double transferCV) {
		this.transferCV = transferCV;
	}

}
