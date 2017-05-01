# TWIMExtract

**If you use TWIMExtract, please cite:**

Haynes, S.E., Polasky D. A., Dixit, S. M., Majmudar, J. D., Neeson, K., Ruotolo, B. T., 
Martin, B. R. "Variable-velocity traveling-wave ion mobility separation enhances peak capacity for
data-independent acquisition proteomics". Manuscript in review

TWIMExtract is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

TWIMExtract is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with TWIMExtract.  If not, see <http://www.gnu.org/licenses/>.

Authors : Daniel Polasky (dpolasky@umich.edu) and Kieran Neeson (Waters)

********************************************************************************
TWIMExtract User Guide
********************************************************************************

Setup:

	1) Download TWIMExtract_Setup.exe from http://sites.lsa.umich.edu/ruotolo/software/twim-extract/
	2) Run TWIMExtract_Setup.exe. Setup will create a shortcut (Run_TWIMExtract.bat) that should be used 
		to run the program. 
	3) Double click Run_TWIMExtract.bat to run TWIMExtract

General Info/Purpose:

	- TWIMExtract will pull data from Waters .raw files using user-defined 'range files'. 
	- Extracted data will be collapsed to one dimension, saving either chromatographic retention time (RT), 
		ion mobility drift time (DT), or the mass spectrum (MZ) to one axis and intensity to the other. 
	- Extracted data are saved to a comma separated text file (.csv) without further processing

Basic Use:

	1) To use TWIMExtract, start the program by double clicking the Run_TWIMExtract.bat shortcut

	2) Prepare range/rule files:
		- To use TWIMExtract, the range(s) of data you want to extract need to be defined
		- This can be done with range files or rule files:
			- Range files are simple text files with 6 fields denoting the starting and ending
				retention time, drift time, and m/z to define a cube in the 3D RT-DT-MZ dataset.
				TWIMExtract will collapse all data in the cube onto the desired axis and output a
				text file (.csv) with the information. 
				**See example range file in C:\TWIMExtract\_EXAMPLES for more information
			- Rule files are created using Driftscope (from Waters). In Driftscope, regions of DT-MZ
				space can be selected. To create a rule file, save the selected region using 
				File\Export Selection Rule. This will create a .rul file that can be selected for
				use in TWIMExtract. See 'Making Selection Rules' in the Examples folder 
				(C:\TWIMExtract\_EXAMPLES) for more info.

	3) Select the raw data to extract from using the "Browse Data" button in TWIMExtract
		***NOTE: for convenience, the default raw, range, and rule file directories can be set using
		the options under the file menu. Then the buttons will open to the chosen directories when pressed. ***

	4) Choose your extraction settings. These can be adjusted using the options menu (top) and the check boxes (bottom)
	   of the extractor interface. The primary options are:
		- Range or Rule file mode: whether to use range (.txt) or rule (.rul) files for the extraction
		- Combine Outputs: whether to combine the output of multiple ranges (extractions) on the SAME raw file into
			a single output file, or to leave them separate and generate multiple output files. Different
			raw files will always generate a new output file. 
		- Save info: Whether to save any information about the file (collision voltages or IM settings) to the
			output file as a header. 

	5) Select the type of extraction (RT, DT, or MZ) using the appropriate button. This will open a filechooser
		to select your desired range or rule file(s). Once the files are selected, extraction will begin. 
		NOTE: extraction may take some time. Typically a few seconds per range file, but can be longer for large raw files. 

Advanced/Other modes:

	- Batch mode: To run multiple extractions in series, a batch can be generated. Batches consist of a single .csv
		file that contains the location of a FOLDER containing raw files in one column and a FOLDER containing
		range or rule files in the second column. Each line in the batch .csv represents a single extraction - that is, 
		all the raw files in the folder from column 1 will be extracted using all the range files from the folder in column 2. 
		A one line batch .csv file is the same as a single regular extraction using the TWIMExtract interface, but many lines
		can be added to create batches of arbitrary size. 
		- To use batch mode, click the Batch menu at the top of the interface, then 'select batch csv and run batch'. This
		will open a file chooser to select the .csv file with batch information. The batch will begin running as soon as
		the .csv file is selected. 
		- For more information, see the batch example in the examples folder.

	- Legacy range file mode: If you used the beta version of TWIMExtract and have old format range files (containing 9 fields
		instead of 6), those files can still be used by selected 'Legacy range file mode' in the Advanced menu. 

	- Careful mode: **most users should not have to worry about this at all**
		- If a raw file contains multiple functions to be extracted, the default behavior of TWIMExtract is to use the
		same number of bins for each function, since getting bin sizes takes as long as actually extracting the data. 
		This should not be a problem unless two functions within the same raw file have mass ranges that differ by an
		order of magnitude or more in size (e.g. function 1 is from 500-1000 m/z and function 2 is from 100-100000 m/z). 
		In that example, function 2 would have fewer m/z bins than it should, possibly resulting in artifical loss of 
		resolution. 
		- Careful mode gets the number of bins individually for each function to avoid resolution losses, but is slower
		than normal mode as a result.  

	- Command line arguments: can be used for scripting with TWIMExtract. 
		General use: java -jar TWIMExtract.jar [ARGS]
		Help: java -jar TWIMExtract.jar -h
		
		NOTE: directories must be in quotes ("") if they contain spaces or other special characters
		Arguments:
			Required:
			-i "[input directory]" : The full system path to the .raw file from which to extract
			-o "[output directory]" : The full system path to the folder in which to save output
			-m [mode] : the extraction mode (the dimension of data to save). 0 = RT, 1 = DT, 2 = MZ
			Optional:
			-f [func] : the individual function to extract. If not provided, extracts all functions
			-r "[Range path]" : The full system path to a range (.txt) or rule (.rul) file to use
				for extraction. If not provided, extracts the full ranges in all dimensions
			-rulemode [true or false] : Whether to use range or rule file. 
			-combinemode [true or false] : Whether to combine all outputs from a single raw file
				(e.g. multiple functions) into a single output. 

		Example: The command below would extract DT information from all functions from the 
		"My_data.raw" file using the "my_range.txt" range file, combine the output, and place it 
		in C:\Extracted Data:

		java -jar TWIMExtract.jar -i "C:\Data\My_data.raw" -o "C:\Extracted Data" -m 1 
			-r "C:\Ranges\my_range.txt" -rulemode false -combinemode true

