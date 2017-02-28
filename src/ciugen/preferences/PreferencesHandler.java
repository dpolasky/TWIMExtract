package ciugen.preferences;

/**
 * This file is part of TWIMExtract
 *  
 */
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class PreferencesHandler
  extends DefaultHandler
{
  private Preferences preferences;
  
  public PreferencesHandler()
  {
    this.preferences = Preferences.getInstance();
  }
  
  public final void startElement(String strUri, String strLocalName, String strQualifiedName, Attributes attributes)
  {
    if (strQualifiedName.equalsIgnoreCase("BUILD"))
    {
      String version = attributes.getValue("VERSION");
      String build = attributes.getValue("NUMBER");
      this.preferences.setStrVersion(version);
      this.preferences.setnBuildNumber(Integer.parseInt(build));
    }
    else
    {
      String visible;
      if (strQualifiedName.equalsIgnoreCase("DISPLAY"))
      {
        String name = attributes.getValue("NAME");
        visible = attributes.getValue("VISIBLE");
      }
      else if (strQualifiedName.equalsIgnoreCase("PROPERTY"))
      {
        String key = attributes.getValue("KEY");
        String value = attributes.getValue("VALUE");
        if (key.equals("max_dt_bins")) {
          this.preferences.setnMaxDTBins(Integer.parseInt(value));
        } else if (key.equals("working_dir_path")) {
          this.preferences.setM_strWorkingDir(value);
        }
      }
    }
  }
  
  public final void characters(char[] ac, int i, int j) {}
  
  public final void endElement(String strUri, String strLocalName, String strQualifiedName) {}
}
