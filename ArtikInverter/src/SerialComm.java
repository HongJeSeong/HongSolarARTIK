import java.sql.SQLException;
import java.util.ArrayList;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialComm
{
	/*public final static String inverterPortName = "COM4";
	public final static String sensorPortName = "COM5";
	public final static String stringPortName = "COM6";
	
	public final static boolean INVERTER_RUN = true;
	public final static boolean SENSOR_RUN	= true;
	public final static boolean STRING_RUN = false;*/
	
	private String logText = "";
	private SerialPort serialPort = null;
	private boolean bConnected = false;
    private Thread writer_thread = null;
    private String portName = "";
    private DBInserter db;
    
    public SerialComm()
    {
        super();
    }
    
    public void connect ( String portName, int speed, DBInserter db)
    {
    	this.portName = portName;
    	this.db = db;
    	try
    	{
    		serialPort = new SerialPort(portName);
    		boolean isOpend = serialPort.openPort();
	        if ( !isOpend )
	        {
	            System.out.println("Error: Port is currently in use");
	        }
	        else
	        {
                serialPort.setParams(speed,
                		SerialPort.DATABITS_8,
                		SerialPort.STOPBITS_1,
                		SerialPort.PARITY_NONE);
                
                setConnected(true);
                serialPort.setRTS(false); 
                serialPort.setDTR(false);
                

                int mask = SerialPort.MASK_RXCHAR; //Prepare mask
                serialPort.setEventsMask(mask);//Set mask
                serialPort.addEventListener(new SerialReader(serialPort, portName));
	        }
    	}
        catch (SerialPortException e)
        {
            logText = "Failed to open " + portName ; //+ "(" + e.toString() + ")";
            System.out.println(logText);
            disconnect();
        }
    }
        
    public boolean getConnected()
    {
    	return bConnected;
    }
    
    @SuppressWarnings("deprecation")
	public void disconnect()
    {
    	if(writer_thread != null)
    		writer_thread.stop();
    	
        //close the serial port
        try
        {
        	if(serialPort != null)
        	{
        		//serialPort.notifyOnBreakInterrupt(true);
		        serialPort.removeEventListener();
		        serialPort.closePort();
		        serialPort = null;
        	}
        	
            setConnected(false);

            logText = "Disconnected.";
            System.out.println(logText + " "+ portName);
        }
        catch (Exception e)
        {
            logText = "Failed to close " + portName
                              + "(" + e.toString() + ")";
            System.out.println(logText);
        }
    }
    
    private void setConnected(boolean val)
    {
    	bConnected = val;
    }
    
    /**
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example. 
     */
    public class SerialReader implements SerialPortEventListener 
    {
//        private byte[] buffer = new byte[1024];
    	private SerialPort	sPort = null;
        private String portName = "";
        
        private ArrayList<Byte> buffer = new ArrayList<>();


        public SerialReader ( SerialPort port, String portName)
        {
        	this.sPort = port;
            this.portName = portName;
        }
        
        private void addBuffer(byte tempBuffer[])
        {
        	if(tempBuffer != null)
        	{
            	for(int i=0; i < tempBuffer.length; i++)
            	{
            		buffer.add(tempBuffer[i]);
            	}
        	}
        }
        private byte[] copyToByteFromBuffer()
        {
        	byte[] completeBuffer = null;
        	if(buffer.size() > 0 )
            {
         	   completeBuffer = new byte[buffer.size()];
         	   for(int j = 0; j < buffer.size(); j++)
         	   {
         		   completeBuffer[j] = buffer.get(j);
         	   }
            }
        	return completeBuffer;
        }
        
		@Override
		public void serialEvent(SerialPortEvent event) {
			byte[] completeBuffer = null;
			if(sPort == null)
				return;
				
			if(event.isRXCHAR()){//If data is available
                try {
                    byte tempBuffer[] = serialPort.readBytes();
                    //System.out.println("RECV("+portName+"): count: "+serialPort.getInputBufferBytesCount()+
                    //		", "+serialPort.getOutputBufferBytesCount());
                    try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
                    
                    //serialPort.getInputBufferBytesCount();
                    if(serialPort.getInputBufferBytesCount() == 0) //complte
                    {
                    	addBuffer(tempBuffer);
                    	completeBuffer = copyToByteFromBuffer();
                    	buffer.clear();
                    }
                    else
                    {
                    	addBuffer(tempBuffer);
                    	return; //not data complete
                    }
                    /*System.out.println(buffer+"/"+serialPort.getInputBufferBytesCount());
                    if(buffer != null)
                    	System.out.println("RECV("+portName+"): "+ event.getEventType() +" : "+ 
                    			event.getEventValue() +" : "+ buffer.length +" : "+new String(buffer));*/
                
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
			}
               
           if(completeBuffer == null)
        	   return;
           
            System.out.println("RECV("+portName+"):"+new String(completeBuffer));
            System.out.println("RECV size :"+completeBuffer.length);
            
            try {
				new WillingsProtocolParser().readParser(completeBuffer, db);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }
    }
    
    public void write()
    {
    	writer_thread = (new Thread(new SerialWriter()));
    	if(writer_thread != null)
    		writer_thread.start();
    }
    protected synchronized void write(byte[] datas)
	{
		try {
			if(serialPort != null)
			{
				serialPort.writeBytes(datas);
			}
		} catch (SerialPortException e) {
			System.out.println(e);
		}
	}
    public class SerialWriter implements Runnable 
    {
        
        public SerialWriter ()
        {
        }
        
        public void run ()
        {
        	
    	   try {    			   
    		   writeProcess();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//M_PVMSClient.serial_mode = false;
				System.out.println(e);
			}
        	
        }
    }
    	
	private final static int WRITE_DELAY_TIME = 100;

	private void writeProcess() throws InterruptedException {
		// TODO Auto-generated method stub
		int i=1;
		byte[] datas = WillingsProtocolParser.writeParser("000003",
				WillingsProtocolParser.SYSTEM_STATUS, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("003203",
				WillingsProtocolParser.PV_POWER, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("003502",
				WillingsProtocolParser.PV_ACC_POWER, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("006403",
				WillingsProtocolParser.INV_VOLT, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("006703",
				WillingsProtocolParser.INV_CURRENT, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("007003",
				WillingsProtocolParser.AC_FREQ, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("007302",
				WillingsProtocolParser.ACC_POWER, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
		
		datas = WillingsProtocolParser.writeParser("007702",
				WillingsProtocolParser.DAY_POWER, i);
		write(datas);
		printWriteData(datas);
		Thread.sleep(WRITE_DELAY_TIME);
	}
	protected void printWriteData(byte[] datas)
	{
		System.out.println("SEND("+ portName +"):"+ new String(datas)  );
	}

}