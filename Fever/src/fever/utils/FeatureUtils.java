package fever.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import models.Feature;
import models.VariabilityTypes;

public class FeatureUtils
{

	public static List<String> getFeatureNames(String line)
	{
		List<String> feature_names = null;
		if (ParsingUtils.isLinux)
			feature_names = FeatureUtils.linuxFeatureNameMatcher(line);
		else if (!ParsingUtils.isLinux)
			feature_names = FeatureUtils.alternateNameMatcher(line);
		return feature_names;
	}

	static List<String> alternateNameMatcher(String line)
	{
		List<String> features = new ArrayList<String>();
		if (line.contains("$"))
		{
			features.addAll(FeatureUtils.alternateNameMatcherForMake(line));
		}
		else
		{
			features.addAll(FeatureUtils.alternateNameMatcherForIfDef(line));
		}
		return features;
	}

	static List<String> alternateNameMatcherForIfDef(String line)
	{
		List<String> features = new ArrayList<String>();
		line = line.replace("ifdef", "");
		line = line.replace("ifndef", "");
		line = line.replace("defined", "");
		line = line.replace("(", "");
		line = line.replace(")", "");
		line = line.replace("!", "");
		line = line.replace("&&", " ");
		line = line.replace("||", " ");
		String[] names = line.split("\\s");
		for (String n : names)
		{
			if (FeatureUtils.isGoodForFName(n) && !n.startsWith("_") && n.length() > 2)
				features.add(n.trim());
		}
		return features;
	}

	static boolean isGoodForFName(String s)
	{
		for (char c : s.toCharArray())
		{
			if (Character.isLetter(c) && Character.isLowerCase(c))
			{
				return false;
			}
			if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')
			{
				return false;
			}
		}
		return true;
	}

	static List<String> alternateNameMatcherForMake(String line)
	{
		List<String> features = new ArrayList<String>();
		if (!line.contains("$"))
			return features; // no variables in there, there can't be any features.
		String[] elems = line.split("\\$");
		if (elems.length > 2)
			return features; // not sure yet.
		String var_name = elems[1];
		if (var_name.indexOf(")") == -1)
			return features;
		var_name = var_name.substring(0, var_name.indexOf(")")); // taking the first variable hopefully.
		var_name = var_name.replace("(", "");
		var_name = var_name.replace(")", "");
		var_name = var_name.replace(",", "");
		var_name.trim();
		if (!ParsingUtils.isAllUpper(var_name) || var_name.length() < 2)
			return features;
		else
			features.add(var_name);
		return features;
	}

	static List<String> linuxFeatureNameMatcher(String line)
	{
		
		line=" "+line;
		Matcher matcher = ParsingUtils.pattern.matcher(line);
		List<String> feat_names = new LinkedList<String>();
		while (matcher.find())
		{
			// System.out.println(matcher.group(idx));
			String g = matcher.group(1);
			if (null != g)
			{
				g = g.trim();
				g = g.substring(g.indexOf("CONFIG_"));
				if (!feat_names.contains(g))
					feat_names.add(g);
			}
		}
		List<String> sanitized = new ArrayList<String>();
		for (String s : feat_names)
		{
			sanitized.add(s.replace("CONFIG_", ""));
		}
		return sanitized;
	}

	static public boolean isFeatureOptional(Feature f)
	{
		boolean isOpt = false;
		VariabilityTypes t = f.getType();
		if (t == VariabilityTypes.TRISTATE || t == VariabilityTypes.BOOLEAN)
		{
			isOpt = true;
		}
		else
		{
			isOpt = false;
		}
		return isOpt;
	}

	static public boolean isFeatureVisible(Feature f)
	{
		boolean visible = false;
		if (f.getPrompt() == null || "".equals(f.getPrompt()))
		{
			visible = false;
		}
		else
			visible = true;
		return visible;
	}
}
