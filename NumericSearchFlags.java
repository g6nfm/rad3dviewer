
public class NumericSearchFlags 
{
	private NumericSearchFlags() {}
	
	public static final int AllowNegativeSign = 1;
	public static final int AllowPositiveSign = 2;
	public static final int AllowDecimalPoint = 4;
	public static final int AllowCommaDecimalPoint = 8;
	public static final int AllowStartingDecimalPoint = 16;
	
	public static final int Integer = AllowNegativeSign | AllowPositiveSign;
	public static final int FloatingPoint = AllowNegativeSign | AllowPositiveSign | AllowDecimalPoint | AllowStartingDecimalPoint;
	public static final int All = AllowNegativeSign | AllowPositiveSign | AllowDecimalPoint | AllowCommaDecimalPoint | AllowStartingDecimalPoint; 
}
