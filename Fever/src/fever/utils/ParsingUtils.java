package fever.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Pattern;

import fever.PropReader;
import fever.change.FeatureOrientedChange;
import fever.change.FeatureOrientedChange.Optionality;
import models.ChangeType;
import models.CompilationTargetType;
import models.DefaultValue;

public class ParsingUtils
{
	static Pattern pattern = Pattern.compile("(([^a-zA-Z_]CONFIG_[0-9a-zA-Z_\\-]+))", Pattern.MULTILINE);
	
	public static boolean isLinux = false;
	public static boolean isAxtls = false;
	
	static
	{
		PropReader r;
		try
		{
			r = new PropReader();
			String p = r.getProperty("repo.path");
			if (p.contains("linux"))
				isLinux = true;
			else if (p.contains("axtls"))
				isAxtls = true;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static Vector<Integer> toVector(int[] array)
	{
		Vector<Integer> v = new Vector<Integer>();
		for (int i : array)
			v.add(i);
		return v;
	}
	
	static boolean isAllUpper(String s)
	{
		for (char c : s.toCharArray())
			if (Character.isLetter(c) && Character.isLowerCase(c))
				return false;
		
		return true;
	}
	
	///FILE LEVEL INFORMATION
	
	static public boolean isBinaryFile(String fileName)
	{
		if(hasBinaryExtension(fileName))
		{
			return true;
		}
		else
			return false;
	}
	
	static public boolean isDataFile(String fileName)
	{
		if(hasDataExtension(fileName))
		{
			return true;
		}
		else
			return false;
	}
	
	static public boolean isSourceFile(String fileName)
	{
		if (hasSourceFileExtension(fileName))
			return true;
		else
			return false;
	}
	
	static public boolean isBuildFile(String fileName)
	{
		if (fileName.indexOf("Makefile") != -1 || fileName.indexOf("Kbuild") != -1 || fileName.indexOf("Platform") != -1)
		{
			return true;
		}
		else
			return false;
	}
	
	static public boolean isVariabilityFile(String fileName)
	{
		String key = null;
		try
		{
			PropReader p = new PropReader();
			key = p.getProperty("parsing.conf_file");
		}
		catch (Exception e)
		{
		}
		
		if (key == null)
			key = "Kconfig"; // falling back to Linux case in case something goes wrong.
		if (fileName.contains(key))
			return true;
		else
			return false;
	}
	
	static public boolean fileMatchCompilationUnit(String file_name, String cu_name)
	{
		try
		{
			String f = file_name.substring(file_name.lastIndexOf("/") + 1, file_name.lastIndexOf(".") - 1);
			String c = cu_name.substring(0, cu_name.lastIndexOf(".") - 1);
			if (f.equalsIgnoreCase(c))
				return true;
			else
			{
				if (f.endsWith(c))
					return true;
				else if (c.endsWith(f))
					return true;
				else
					return false;
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	
	public static boolean isAdd(ChangeType op)
	{
		if (op.equals(ChangeType.ADDED) || op.equals(ChangeType.ADDED_VALUE))
			return true;
		else
			return false;
	}
	
	public static boolean isRemove(ChangeType op)
	{
		if (op.equals(ChangeType.REMOVED) || op.equals(ChangeType.REMOVED_VALUE))
			return true;
		else
			return false;
	}
	
	public static boolean isModified(ChangeType op)
	{
		if (op.equals(ChangeType.MODIFIED) || op.equals(ChangeType.MODIFIED_VALUE) || op.equals(ChangeType.MOVED))
			return true;
		else
			return false;
	}
	
	///MAPPING TARGET INFO
	public static boolean isCompilationFlag(String l)
	{
		if (l.indexOf("-l") != -1)
			return true;
		if (l.indexOf("-I") != -1)
			return true;
		if (l.indexOf("-W") != -1)
			return true;
		if (l.indexOf("-D") != -1)
			return true;
		if (l.indexOf("cc-option") != -1)
			return true;
		if (l.indexOf("cc-flag") != -1)
			return true;
		if (l.startsWith("-f"))
			return true;
		if (l.startsWith("-pg"))
			return true;
		if (l.contains("$") || l.contains("("))
			return false;
		if (!l.startsWith("-"))
			return false;
		
		if (!isSourceFile(l) && !isBuildFile(l))
			return true; // serious hack... that's not very good.
		return false;
	}
	
	
	public static boolean isFolderName(String t)
	{
		PropReader r = null;
		try
		{
			r = new PropReader();
		}
		catch (Exception e)
		{
			throw new RuntimeException("unable to load property file! Aborting.");
		}
		String _guard_suffix = r.getProperty("guard_suffix");
		if (!t.startsWith("-") && (t.endsWith("/") || t.endsWith(_guard_suffix)))
			return true;
		else
			return false;
		
//		if (ParsingUtils.isLinux)
//		{
//			if (t.length() < 2)
//				return false;
//			if  (t.contains("0x"))
//				return false;
//			if (t.endsWith("dtbs")) // ok - true hack here. This specific string is NOT a folder. And it's annoying me.
//				return false;
//			if (!t.contains(".") && !t.contains("-") && !t.contains("_"))
//				return true;
//		}
//		return false;
	}
	
	public static boolean hasSourceFileExtension(String s)
	{
		s= s.trim();
		
		if (!s.contains("."))
			return false;
		
		if (s.endsWith(".o") || s.endsWith(".c") || s.endsWith(".h") || s.endsWith(".S") ||s.endsWith(".s"))
			return true;
		else if (s.endsWith(".vb") || s.endsWith(".pl") || s.endsWith(".cs") || s.endsWith(".java") || s.endsWith(".py") || s.endsWith(".js"))
			return true;
		else
			return false;
	}
	
	public static boolean hasBinaryExtension(String s)
	{
		if (s.endsWith(".dll") || s.endsWith(".so") || s.endsWith(".a") || s.endsWith(".lib"))
			return true;
		return false;
	}
	
	public static boolean hasDocumentationExtension(String s)
	{
		if (s.endsWith(".txt"))
			return true;
		return false;
	}
	
	public static boolean hasDataExtension(String s)
	{
		if (s.endsWith(".json") || s.endsWith(".fbp") || s.endsWith(".dts") || s.endsWith(".dtb") )
		{
			return true;
		}
		
		return false;
	}
}
