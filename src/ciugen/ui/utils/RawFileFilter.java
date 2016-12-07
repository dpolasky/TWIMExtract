package ciugen.ui.utils;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class RawFileFilter
  extends FileFilter implements FilenameFilter
{
  class CDTFilter
    implements FilenameFilter
  {
    CDTFilter() {}
    
    public boolean accept(File dir, String name)
    {
      return name.toLowerCase().endsWith(".cdt");
    }
  }
  
  public boolean accept(File file)
  {
    String filename = file.getName();
    boolean bAccept = false;
    if ((file.isDirectory()) && (filename.toLowerCase().endsWith(".raw"))) {
      bAccept = file.list(new CDTFilter()).length > 0;
    } else if (file.isDirectory()) {
      bAccept = true;
    }
    return bAccept;
  }
  
  // Determine if the file is a 1dDT file and accept it if so
  public static boolean acceptdDT(File file){
	  String filename = file.getName().toLowerCase();
	  boolean bAccept = false;  
	  try{
		  if (filename.substring(filename.lastIndexOf(".")).equals(".1ddt")){
			  bAccept = true;
		  } 
      // Catch a bad string location since the filename test is pretty rough cut. If an exception
      // happens, it's definitely not a .1dDT file. 
	  } catch (StringIndexOutOfBoundsException ex){
		  bAccept = false;
		  return bAccept;
	  }
	  return bAccept;
  }
  
  //Determine if the file is a 1dMZ file and accept it if so
  public static boolean acceptMZ(File file){
	  String filename = file.getName().toLowerCase();
	  boolean bAccept = false;  
	  try{
		  if (filename.substring(filename.lastIndexOf(".")).equals(".1dmz")){
			  bAccept = true;
		  } 
		  // Catch a bad string location since the filename test is pretty rough cut. If an exception
		  // happens, it's definitely not a .1dDT file. 
	  } catch (StringIndexOutOfBoundsException ex){
		  bAccept = false;
		  return bAccept;
	  }
	  return bAccept;
  }
  
  //Determine if the file is a 1dRT file and accept it if so
  public static boolean acceptRT(File file){
	  String filename = file.getName().toLowerCase();
	  boolean bAccept = false;  
	  try{
		  if (filename.substring(filename.lastIndexOf(".")).equals(".1drt")){
			  bAccept = true;
		  } 
		  // Catch a bad string location since the filename test is pretty rough cut. If an exception
		  // happens, it's definitely not a .1dRT file. 
	  } catch (StringIndexOutOfBoundsException ex){
		  bAccept = false;
		  return bAccept;
	  }
	  return bAccept;
  }
  
  public String getDescription()
  {
    return "RAW files";
  }

  public boolean accept(File file, String arg)
  {
    String filename = file.getName();
    boolean bAccept = false;
    if ((file.isDirectory()) && (filename.toLowerCase().endsWith(".raw"))) {
      bAccept = file.list(new CDTFilter()).length > 0;
    } else if (file.isDirectory()) {
      bAccept = true;
    }
    return bAccept;
  }
}
