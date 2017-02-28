package ciugen.ui.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 *  * This file is part of TWIMExtract
 *
 */
public class TextFileFilter implements FilenameFilter{

	public boolean accept(File dir, String name) {
		return name.endsWith(".txt");
	}

}