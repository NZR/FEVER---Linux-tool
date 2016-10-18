package fever.utils;

import models.CodeEdit;
import models.ConditionalBlock;

public class CodeUtils
{

	///
	public static boolean codeInRemovedBlockFullyRemovedByEdit(ConditionalBlock cb, CodeEdit ce)
	{
		int start_rem = ce.getRem_idx();
		int stop_rem = start_rem + ce.getRem_size();
		if (stop_rem < cb.getStart() || start_rem > cb.getEnd())
			return false; // edit ends before the blocks starts, or start after the block is finished
		if (start_rem <= (cb.getStart() + 1) && stop_rem >= (cb.getEnd() - 1))
		{ // the edit starts before the first line of code in the blocks and ends after the last line of code of the block
			return true;
		}
		return false;
	}

	public static boolean codeInRemovedBlockEditedByEdit(ConditionalBlock cb, CodeEdit ce)
	{
		int start_rem = ce.getRem_idx();
		// DEBUG int stop_rem = ce.getRem_idx() + ce.getAdd_size() -1;
		int stop_rem = ce.getRem_idx() + ce.getRem_size() - 1;
		if (ce.getRem_size() != 0)
		{
			// Added fragments
			if (start_rem > (cb.getStart() + 1) && stop_rem < (cb.getEnd() - 1))
			{ // edit fully contained in block
				return true;
			}
			if (start_rem < (cb.getStart() + 1) && stop_rem >= (cb.getStart() + 1))
			{ // starts before the block and ends after the start.
				return true;
			}
			if (start_rem > (cb.getStart() + 1) && start_rem < (cb.getEnd() - 1))
			{ // starts in the block.
				return true;
			}
		}
		return false;
	}

	public static boolean codeInNewBlockFullyAddedByEdit(ConditionalBlock cb, CodeEdit ce)
	{
		int start_add = ce.getAdd_idx();
		int stop_add = start_add + ce.getAdd_size();
		if (stop_add < cb.getStart() || start_add > cb.getEnd())
			return false; // edit ends before the blocks starts, or start after the block is finished
		if (start_add <= (cb.getStart() + 1) && stop_add >= (cb.getEnd() - 1))
		{ // the edit starts before the first line of code in the blocks and ends after the last line of code of the block
			return true;
		}
		return false;
	}

	public static boolean EditInsideBlock(ConditionalBlock cb, CodeEdit ce)
	{
		// this is where i make it happen.
		if (cb.getStart() < ce.getAdd_idx() && cb.getEnd() > ce.getAdd_idx() || cb.getStart() < ce.getRem_idx() && cb.getEnd() > ce.getRem_idx())
			return true;
		else
			return false;
	}

	public static boolean codeInNewBlockEditedByEdit(ConditionalBlock cb, CodeEdit ce)
	{
		int start_add = ce.getAdd_idx();
		int stop_add = ce.getAdd_idx() + ce.getAdd_size() - 1;
		if (ce.getAdd_size() != 0)
		{
			// Added fragments
			if (start_add > (cb.getStart() + 1) && stop_add < (cb.getEnd() - 1))
			{ // edit fully contained in block
				return true;
			}
			if (start_add < (cb.getStart() + 1) && stop_add >= (cb.getStart() + 1))
			{ // starts before the block and ends after the start.
				return true;
			}
			if (start_add > (cb.getStart() + 1) && start_add < (cb.getEnd() - 1))
			{ // starts in the block.
				return true;
			}
		}
		int start_rem = ce.getAdd_idx(); // we are actually dealing with new blocks. The indices we get are in the new file format.
		int end_rem = start_rem + ce.getRem_size();
		if (ce.getRem_size() != 0)
		{
			// Removed fragments
			if (start_rem > (cb.getStart() + 1) && end_rem < (cb.getEnd() - 1))
			{ // edit fully contained in block
				return true;
			}
			if (start_rem < (cb.getStart() + 1) && end_rem > (cb.getStart() + 1))
			{ // starts before the block and ends after the start.
				return true;
			}
			if (start_rem > (cb.getStart() + 1) && start_rem < (cb.getEnd() - 1))
			{ // starts in the block.
				return true;
			}
		}
		return false;
	}

	public static boolean touchesIfDefs(String diff)
	{
		if (diff.contains("#if"))
			return true;
		else
			return false;
	}
}
