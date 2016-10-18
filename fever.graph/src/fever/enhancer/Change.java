package fever.enhancer;

import java.util.ArrayList;
import java.util.List;

public class Change
{
	String feature = "";
	
	public boolean Bool = false;
	public boolean Tristate = false;
	public boolean String_var = false;
	public boolean Visible = false;
	
	
	public boolean mapped_to_file = false;
	public boolean mapped_to_folder = false;
	public boolean mapped_to_cf = false; 
	
	
	
	List<String> added_features = new ArrayList<String>(); 
	List<String> removed_features = new ArrayList<String>(); 
	
	List<String> added_includes = new ArrayList<String>(); 
	List<String> removed_includes = new ArrayList<String>(); 
	
}
