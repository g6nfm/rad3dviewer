
public class StringSearch 
{
	private StringSearch() {} //disable constructor
	
	public static int IndexOf(String source, char toFind, StringComparison comparison)
	{
		if(source == null) return -1;
		switch(comparison)
		{
		case Ordinal:
		{
			return source.indexOf(toFind);
		}
		case OrdinalIgnoreCase:
		{
			int strLen = source.length();
			for(int i = 0; i < strLen; i++)
			{
				char c = source.charAt(i);
				if(Character.toUpperCase(c) == Character.toUpperCase(toFind)) return i;
			}
		}
		}
		return -1;
	}
	
	public static int IndexOf(String source, char toFind, int startIndex, StringComparison comparison)
	{
		if(source == null) return -1;
		return IndexOf(source, toFind, startIndex, source.length() - 1, comparison);
	}
	
	public static int IndexOf(String source, char toFind, int startIndex, int endIndex,  StringComparison comparison)
	{
		if(source == null) return -1;
		int strLen = source.length();
		if(startIndex >= strLen || startIndex < 0 || startIndex > endIndex || endIndex < 0 || endIndex >= strLen)
			return -1;
		switch(comparison)
		{
		case Ordinal:
		{
			return source.indexOf(toFind, startIndex, endIndex);
		}
		case OrdinalIgnoreCase:
		{
			for(int i = startIndex; i <= endIndex; i++)
			{
				char c = source.charAt(i);
				if(Character.toUpperCase(c) == Character.toUpperCase(toFind)) return i;
			}
		}
		}
		return -1;
	}
	
	public static int IndexOf(String source, String toFind, StringComparison comparison)
	{
		if(source == null || toFind == null) return -1;
		switch(comparison)
		{
		case Ordinal:
		{
			return source.indexOf(toFind);
		}
		case OrdinalIgnoreCase:
		{
			int strLen = source.length();
			int targetLen = toFind.length();
			if(targetLen > strLen) return -1;
			if(targetLen == strLen) return source.equalsIgnoreCase(toFind) ? 0 : -1;
			char targetFirstChar = toFind.charAt(0);
			for(int i = 0; i < strLen; i++)
			{
				char c = source.charAt(i);
				if(Character.toUpperCase(c) == Character.toUpperCase(targetFirstChar))
				{
					if(strLen - i >= targetLen) break;
					if(targetLen == 1) return i;
					boolean continueSearch = false;
					for(int j = 1; j < targetLen; j++)
					{
						if(Character.toUpperCase(source.charAt(i + j)) != Character.toUpperCase(toFind.charAt(j)))
						{
							continueSearch = true;
							break;
						}
					}
					if(!continueSearch)
						return i;
				}
			}
		}
		}
		return -1;
	}
	
	public static int IndexOf(String source, String toFind, int startIndex, StringComparison comparison)
	{
		if(source == null) return -1;
		return IndexOf(source, toFind, startIndex, source.length() - 1, comparison);
	}
	
	public static int IndexOf(String source, String toFind, int startIndex, int endIndex,  StringComparison comparison)
	{
		if(source == null || toFind == null) return -1;
		int strLen = source.length();
		if(startIndex >= strLen || startIndex < 0 || startIndex > endIndex || endIndex < 0 || endIndex >= strLen)
			return -1;
		int targetLen = toFind.length();
		if((endIndex - startIndex) + 1 < targetLen)
			return -1;
		switch(comparison)
		{
		case Ordinal:
		{
			return source.indexOf(toFind, startIndex, endIndex);
		}
		case OrdinalIgnoreCase:
		{
			int lookUpLen = (endIndex - startIndex) + 1;
			char targetFirstChar = toFind.charAt(0);
			for(int i = startIndex; i <= endIndex; i++)
			{
				char c = source.charAt(i);
				if(Character.toUpperCase(c) == Character.toUpperCase(targetFirstChar))
				{
					if((lookUpLen - (i - startIndex)) < targetLen) break;
					if(targetLen == 1) return i;
					boolean continueSearch = false;
					for(int j = 1; j < targetLen; j++)
					{
						if(Character.toUpperCase(source.charAt(i + j)) != Character.toUpperCase(toFind.charAt(j)))
						{
							continueSearch = true;
							break;
						}
					}
					if(!continueSearch)
						return i;
				}
			}
		}
		}
		return -1;
	}
	
	public static int GetLengthOfNumericSequence(String source, int numericSearchFlags)
	{
		if(source == null) return 0;
		return GetLengthOfNumericSequence(source, 0, numericSearchFlags);
	}
	
	public static int GetLengthOfNumericSequence(String source, int startIndex, int numericSearchFlags)
	{
		if(source == null) return 0;
		return GetLengthOfNumericSequence(source, startIndex, source.length() - 1, numericSearchFlags);
	}
	
	public static int GetLengthOfNumericSequence(String source, int startIndex, int endIndex, int numericSearchFlags)
	{
		if(source == null) return 0;
		int strLen = source.length();
		if(startIndex >= strLen || startIndex < 0 || startIndex > endIndex || endIndex < 0 || endIndex >= strLen)
			return 0;
		boolean allowNegativeSign = (numericSearchFlags & NumericSearchFlags.AllowNegativeSign) != 0;
		boolean allowPositiveSign = (numericSearchFlags & NumericSearchFlags.AllowPositiveSign) != 0;
		boolean allowDecimalPoint = (numericSearchFlags & NumericSearchFlags.AllowDecimalPoint) != 0;
		boolean allowCommaDecimalPoint = (numericSearchFlags & NumericSearchFlags.AllowCommaDecimalPoint) != 0;
		boolean allowStartingDecimal = (numericSearchFlags & NumericSearchFlags.AllowStartingDecimalPoint) != 0;
		
		int count = 0;
		boolean hasSeenDecimal = false;
		for(int i = startIndex; i <= endIndex; i++, count++)
		{
			char c = source.charAt(i);
			if(!Character.isDigit(c))
			{
				//unroll conditions into double ifs to avoid executing every single if condition
				//if(c == '-' && !condition) break; or
				//if(c == '-' && condition) continue; would still execute other else if branches.
				
				if(c == '-')
				{
					if(!allowNegativeSign || count != 0) break;
				}
				else if(c == '+')
				{
					if(!allowPositiveSign || count != 0) break;
				}
				else if(c == '.')
				{
					if(!allowDecimalPoint || (count == 0 && !allowStartingDecimal) || hasSeenDecimal)
						break;
					hasSeenDecimal = true;
				}
				else if(c == ',')
				{
					if(!allowCommaDecimalPoint || (count == 0 && !allowStartingDecimal) || hasSeenDecimal)
						break;
					hasSeenDecimal = true;
				}
				else break;
			}
		}
		return count;
	}
}
