package fever.stats;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


public class MultiReleaseDumpSorter
{
	// To aggregate the information over several release,
	// we dump all the info from all releases in separate files based on their change type
	// one file for all features affecting a given space, one file for all authors affecting a given space,...
	public static final void main(String[] args) throws Exception
	{
		feature_stats(args);
		System.out.println("=======");
		author_stats(args);
	}
	
	public static final void feature_stats(String[] args) throws Exception
	{
		List<String> item_list = new ArrayList<String>();  
		List<String> all_spaces_list = new ArrayList<String>();
		List<String> vm_build = new ArrayList<String>();
		List<String> source_build = new ArrayList<String>();
		List<String> source_vm = new ArrayList<String>();

		List<String> source = new ArrayList<String>();
		List<String> vm = new ArrayList<String>(); 
		List<String> build = new ArrayList<String>();
		
		LoadItemsFromFile(item_list,FilePath.o_features);
		
		LoadItemsFromFile(all_spaces_list,FilePath.o_feature_in_all);
		checkConsistency(all_spaces_list, item_list);

		//--
		LoadItemsFromFile(vm_build,FilePath.o_feature_in_vm_build);
		checkConsistency(vm_build, item_list);
		
		LoadItemsFromFile(source_build,FilePath.o_feature_in_build_source);
		checkConsistency(source_build, item_list);
		
		LoadItemsFromFile(source_vm,FilePath.o_feature_in_vm_source);
		checkConsistency(source_vm, item_list);

		//--
		LoadItemsFromFile(source,FilePath.o_feature_in_source);
		checkConsistency(source, item_list);

		LoadItemsFromFile(vm,FilePath.o_feature_in_vm);
		checkConsistency(vm, item_list);

		LoadItemsFromFile(build,FilePath.o_feature_in_build);
		checkConsistency(build, item_list);
		//--

		// now we consolidate the information bottom-up
		//	we search for features who have been affected in differences spaces over time,
		//	and we want to move them up to the proper list (cleanly if possible)
		// then we go top-bottom to clean the lists. 
		bottom_up(all_spaces_list,vm_build,source_build,source_vm, vm,build,source);

		//--
		List<List<String>> sets_to_clean = new ArrayList<List<String>>();
		sets_to_clean.add(vm_build);
		sets_to_clean.add(source_vm);
		sets_to_clean.add(source_build);
		sets_to_clean.add(vm);
		sets_to_clean.add(build);
		sets_to_clean.add(source);

		cleanListAfterConsolidations(all_spaces_list,sets_to_clean);

		System.out.println("nb timelines: " + item_list.size() );
		System.out.println("all spaces: " + all_spaces_list.size() );
		System.out.println("vm : " + vm.size() );
		System.out.println("build : " + build.size() );
		System.out.println("source: " + source.size() );
		System.out.println("vm and build: " + vm_build.size() );
		System.out.println("vm and source: " + source_vm.size() );
		System.out.println("build and source: " + source_build.size() );
	}

	private static void bottom_up(
			List<String> all_spaces_list, 
			List<String> vm_build, List<String> source_build, List<String> source_vm, 
			List<String> vm, List<String> build, List<String> source)
	{
		for(String s : all_spaces_list)
		{
			if(vm_build.contains(s)) vm_build.remove(s);
			if(source_build.contains(s)) source_build.remove(s);
			if(source_vm.contains(s)) source_vm.remove(s);
			if(vm.contains(s)) vm.remove(s);
			if(build.contains(s)) build.remove(s);
			if(source.contains(s)) source.remove(s);
		}

		for(String s : vm_build)
		{
			if(vm.contains(s)) vm.remove(s);
			if(build.contains(s)) build.remove(s);
		}
		
		for(String s : source_build)
		{
			if(source.contains(s)) source.remove(s);
			if(build.contains(s)) build.remove(s);
		}
		
		for(String s: vm_build)
		{
			if(build.contains(s)) 
				build.remove(s);			
			if(vm.contains(s)) 
				vm.remove(s);
		}
		
		List<String> to_clean = new ArrayList<String>();
		for(String s : build)
		{
			if(vm.contains(s) && source.contains(s))
			{
				all_spaces_list.add(s);
				to_clean.add(s);
			}
			if(vm.contains(s))
			{
				vm_build.add(s);
				to_clean.add(s);
			}
			if(source.contains(s))
			{
				source_build.add(s);
				to_clean.add(s);
			}
		}
	
		for(String s : to_clean)
		{
			if(source.contains(s)) source.remove(s);
			if(vm.contains(s)) vm.remove(s);
			if(build.contains(s)) build.remove(s);
		}
		to_clean.clear();
		
		for(String s: vm)
		{
			if(source.contains(s))
			{
				source_vm.add(s);
				to_clean.add(s);
			}
		}

		for(String s : to_clean)
		{
			source.remove(s);
			vm.remove(s);
		}
		
		to_clean.clear();
		
		for(String s: vm_build)
		{
			if(source_build.contains(s))
			{
				all_spaces_list.contains(s);
				to_clean.add(s);
			}
		}
		
		for(String s: source_vm)
		{
			if(source_build.contains(s))
			{
				all_spaces_list.add(s);
				to_clean.add(s);
			}
		}
		
		for(String s: vm_build )
		{
			if(source_build.contains(s))
			{
				all_spaces_list.add(s);
				to_clean.add(s);
			}
		}
		
		for(String s: to_clean)
		{
			vm_build.remove(s);
			source_build.remove(s);
			source_vm.remove(s);
		}
	}

	private static void cleanListAfterConsolidations(List<String> all_spaces_list, List<List<String>> sets_to_clean)
	{
		for(String s : all_spaces_list)
		{
			for(List<String> set : sets_to_clean)
			{
				if(set.contains(s))
				{
					set.remove(s);
				}
			}
		}
		
	}

	private static void checkConsistency(List<String> all_spaces_list, List<String> item_list)
	{
		//double check
		for(String item : all_spaces_list)
		{
			if(!item_list.contains(item))
				System.err.println("Error found, one item is not present in the complete list: item is "+item );
		}
	}
	
	private static void LoadItemsFromFile(List<String> item_list, String o_features) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(o_features));
		String line = "";
		while( null != ( line = br.readLine()) )
		{
			line = line.trim();
			if(!item_list.contains(line))
				item_list.add(line);
		}
		br.close();
	}

	public static final void author_stats(String[] args) throws Exception
	{
		List<String> item_list = new ArrayList<String>();  
		List<String> all_spaces_list = new ArrayList<String>();
		List<String> vm_build = new ArrayList<String>();
		List<String> source_build = new ArrayList<String>();
		List<String> source_vm = new ArrayList<String>();

		List<String> source = new ArrayList<String>();
		List<String> vm = new ArrayList<String>(); 
		List<String> build = new ArrayList<String>();
		
		LoadItemsFromFile(item_list,FilePath.o_authors);
		//--
		LoadItemsFromFile(source,FilePath.o_authors_in_source);
		checkConsistency(source, item_list);

		LoadItemsFromFile(vm,FilePath.o_authors_in_vm);
		checkConsistency(vm, item_list);

		LoadItemsFromFile(build,FilePath.o_authors_in_build);
		checkConsistency(build, item_list);
		//--
		
		for(String a : item_list)
		{
			if(vm.contains(a) && build.contains(a) && source.contains(a))
			{
				all_spaces_list.add(a);
				vm.remove(a);
				build.remove(a);
				source.remove(a);
			}
			else if(vm.contains(a) && build.contains(a))
			{
				vm_build.add(a);
				vm.remove(a);
				build.remove(a);
			}
			else if( build.contains(a) && source.contains(a))
			{
				source_build.add(a);
				build.remove(a);
				source.remove(a);
			}
			else if (source.contains(a) && vm.contains(a))
			{
				source_vm.add(a);
				source.remove(a);
				vm.remove(a);
			}
		}

		System.out.println("nb authors: " + item_list.size() );
		System.out.println("all spaces: " + all_spaces_list.size() );
		System.out.println("vm : " + vm.size() );
		System.out.println("build : " + build.size() );
		System.out.println("source: " + source.size() );
		System.out.println("vm and build: " + vm_build.size() );
		System.out.println("vm and source: " + source_vm.size() );
		System.out.println("build and source: " + source_build.size() );
	}
	
	
	
}
