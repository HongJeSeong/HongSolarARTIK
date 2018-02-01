public class SerialRunner {
		
	public SerialRunner()
	{
		//initial serial;
		SerialComm comm = new SerialComm();
		comm.connect("COM3", 9600);	
	}
	public void stop()
	{
	}
}
