package ciugen.ui;

public class Options {
/**
 * Options class for holding information from command line arguments
 */
	public String input;
	public String output;
	public int function;
	public String range = "FULL";
	public int mode;
	public boolean ruleMode = false;
	public boolean combineMode = false;
	public boolean extract_in_ms = true;
	
	public Options(String inputpath, String outputpath, String rangepath, int extmode, int parsed_func,
			boolean rule, boolean combine, boolean ext_unit){
		this.input = inputpath;
		this.output = outputpath;
		this.mode = extmode;
		this.function = parsed_func;	// parsed_func will always be supplied - if not passed by user, value will be -1
		
		// allow null values for optional booleans
		try{ this.range = rangepath; } catch (NullPointerException ex){
			// no parameter supplied, use default value
		}
		try{ this.ruleMode = rule;} catch (NullPointerException ex){}
		try{ this.combineMode = combine; } catch (NullPointerException ex){};
		try{ this.extract_in_ms = ext_unit; } catch (NullPointerException ex) {};
	}
	
	public void print_options(){
		System.out.println("input: " + this.input);
		System.out.println("output: " + this.output);
		System.out.println("mode: " + this.mode);
		System.out.println("function: " + this.function);
		System.out.println("range: " + this.range);
		System.out.println("ruleMode: " + this.ruleMode);
		System.out.println("combineMode: " + this.combineMode);
		String ext_unit = "";
		if (this.extract_in_ms){
			ext_unit = "milliseconds";
		} else {
			ext_unit = "bins";
		}
		System.out.println("extraction unit = " + ext_unit);

	}
	
}
