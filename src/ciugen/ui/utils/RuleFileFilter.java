package ciugen.ui.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Filter for rule files (.rul) for rule mode file handling. Accepts only files ending in .rul. 
 * @author dpolasky
 *
 */
public class RuleFileFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.endsWith(".rul");
	}
}
