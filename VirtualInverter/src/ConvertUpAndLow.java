
public class ConvertUpAndLow {
	private int low = 0;
	private int high = 0;
	
	public ConvertUpAndLow(int num)
	{
		high = (num & 0xFFFF0000)>>16;
		low = num & 0x0000FFFF;
	}
	public int getLow()
	{
		return low;
	}
	
	public int getHigh()
	{
		return high;
	}
}
