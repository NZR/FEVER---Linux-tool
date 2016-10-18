package fever.stats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class AuthorDeDup
{
	
	public static void main(String[] args) throws Exception
	{
		
		//this program attempts to identify duplicated people name in a long list of names
		// the names may include middle name, name of company in parenthesis, maybe initials.
		// the idea here is not to identify potential dups, but more to idnetify the list of 
		// unique names in the list.
		
		
		String list = "/Users/Dante/Downloads/authors-4.4.csv";
		BufferedReader r = new BufferedReader(new FileReader(list));
		String line = "";
		
		List<String> names = new ArrayList<String>();
		
		while( ( line = r.readLine()) != null)
		{
			line = line.trim();
			
			if (line.contains("\""))
				line = line.replaceAll("\"", "");
			if (line.contains(","))
				line = line.replaceAll(",", "");
			
			if (line.contains("."))
				line = line.replaceAll(".", "");
			if (line.contains("\\?"))
				line = line.replaceAll("\\?", "");
			
			if(line.contains("("))
				line = line.replaceAll("\\(.*\\)", "");
			
			names.add(line);
		}
		
		int dups = 0;
		int unique = 0;
		
		for(int i = 0; i < names.size() ; i++)
		{
			String n = names.get(i);
			String first = "";
			String last = "";
			
			String [] elems = n.split("\\s+");
			if(elems.length == 2)
			{
				first = elems[0];
				last = elems[1];
			}
			else if (elems.length == 3)
			{
				if(elems[2].toUpperCase().equals(elems[2]))
				{
					first = elems[0];
					last = elems[1];
				}
			}
			else
			{
				continue;
			}
			
			boolean dup_found = false;
			if(!first.isEmpty() && !last.isEmpty())
			{
				for(int j = i+1 ; j < names.size(); j++)
				{
	
					String n2 = names.get(j);
				
					if (n2.toUpperCase().contains(first.toUpperCase()))
					{
						if(n2.toUpperCase().contains(last.toUpperCase()))
						{
							System.out.println("duplicated names : " + n + " -- and -- " + n2);
							dup_found = true;
						}
					}
				}
			}
			
			if(!dup_found)
				unique ++;
			else
				dups++;
		}
		System.out.println("number of unique names identified : " + unique);
		System.out.println("number of duplicated names found : " + dups);
		r.close();
	}
}
