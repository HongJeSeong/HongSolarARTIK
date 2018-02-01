
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialComm {
	static int num = 1;
	/*
	 * public final static String inverterPortName = "COM4"; public final static
	 * String sensorPortName = "COM5"; public final static String stringPortName
	 * = "COM6";
	 * 
	 * public final static boolean INVERTER_RUN = true; public final static
	 * boolean SENSOR_RUN = true; public final static boolean STRING_RUN =
	 * false;
	 */

	private String logText = "";
	private SerialPort serialPort = null;
	private boolean bConnected = false;
	private Thread writer_thread = null;
	private String portName = "";

	public SerialComm() {
		super();
	}

	public void connect(String portName, int speed) {
		this.portName = portName;
		try {
			serialPort = new SerialPort(portName);
			boolean isOpend = serialPort.openPort();
			if (!isOpend) {
				System.out.println("Error: Port is currently in use");
			} else {
				serialPort.setParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				setConnected(true);
				serialPort.setRTS(false);
				serialPort.setDTR(false);

				int mask = SerialPort.MASK_RXCHAR; // Prepare mask
				serialPort.setEventsMask(mask);// Set mask
				serialPort.addEventListener(new SerialReader(serialPort, portName));
			}
		} catch (SerialPortException e) {
			logText = "Failed to open " + portName; // + "(" + e.toString() +
													// ")";
			System.out.println(logText);
			disconnect();
		}
	}

	public boolean getConnected() {
		return bConnected;
	}

	@SuppressWarnings("deprecation")
	public void disconnect() {
		if (writer_thread != null)
			writer_thread.stop();

		// close the serial port
		try {
			if (serialPort != null) {
				// serialPort.notifyOnBreakInterrupt(true);
				serialPort.removeEventListener();
				serialPort.closePort();
				serialPort = null;
			}

			setConnected(false);

			logText = "Disconnected.";
			System.out.println(logText + " " + portName);
		} catch (Exception e) {
			logText = "Failed to close " + portName + "(" + e.toString() + ")";
			System.out.println(logText);
		}
	}

	private void setConnected(boolean val) {
		bConnected = val;
	}

	/**
	 * Handles the input coming from the serial port. A new line character is
	 * treated as the end of a block in this example.
	 */
	public class SerialReader implements SerialPortEventListener {
		// private byte[] buffer = new byte[1024];
		private SerialPort sPort = null;
		private String portName = "";

		private ArrayList<Byte> buffer = new ArrayList<>();

		public SerialReader(SerialPort port, String portName) {
			this.sPort = port;
			this.portName = portName;
		}

		private void addBuffer(byte tempBuffer[]) {
			if (tempBuffer != null) {
				for (int i = 0; i < tempBuffer.length; i++) {
					buffer.add(tempBuffer[i]);
				}
			}
		}

		private byte[] copyToByteFromBuffer() {
			byte[] completeBuffer = null;
			if (buffer.size() > 0) {
				completeBuffer = new byte[buffer.size()];
				for (int j = 0; j < buffer.size(); j++) {
					completeBuffer[j] = buffer.get(j);
				}
			}
			return completeBuffer;
		}

		public Connection getConnection() {
			Connection conn = null;
			try {
				Class.forName("org.sqlite.JDBC").newInstance();
				conn = DriverManager.getConnection("jdbc:sqlite:/C:/Users/Jihoon/Downloads/hsEnergyData.db");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return conn;
		}

		@Override
		public void serialEvent(SerialPortEvent event) {
			byte[] completeBuffer = null;
			if (sPort == null)
				return;

			if (event.isRXCHAR()) {// If data is available
				try {
					byte tempBuffer[] = serialPort.readBytes();
					// System.out.println("RECV("+portName+"): count:
					// "+serialPort.getInputBufferBytesCount()+
					// ", "+serialPort.getOutputBufferBytesCount());
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}

					// serialPort.getInputBufferBytesCount();
					if (serialPort.getInputBufferBytesCount() == 0) // complte
					{
						addBuffer(tempBuffer);
						completeBuffer = copyToByteFromBuffer();
						buffer.clear();
					} else {
						addBuffer(tempBuffer);
						return; // not data complete
					}
					/*
					 * System.out.println(buffer+"/"+serialPort.
					 * getInputBufferBytesCount()); if(buffer != null)
					 * System.out.println("RECV("+portName+"): "+
					 * event.getEventType() +" : "+ event.getEventValue()
					 * +" : "+ buffer.length +" : "+new String(buffer));
					 */

				} catch (SerialPortException ex) {
					System.out.println(ex);
				}
			}

			if (completeBuffer == null)
				return;
			
			Connection conn = getConnection();
			try {
				long time = System.currentTimeMillis();
				SimpleDateFormat dayTime = new SimpleDateFormat("YYYY-MM-dd HH:mm");
				String str = dayTime.format(new Date(time));
				String date = str.substring(11, str.length() - 1);
				
				Statement stat = conn.createStatement();
				ResultSet rs = stat.executeQuery("select * from hsPlantDisplay where time LIKE '%" + date + "%'");
				float month_gen = rs.getFloat("month_power");
				
				rs = stat.executeQuery("select * from hsSensors where time LIKE '%" + date + "%'");
				int co_dec = rs.getInt("co2") * 10;
				
				stat.close();
				rs.close();
				
				Statement stat_inverter = conn.createStatement();
				ResultSet rs_inverter = stat.executeQuery("select * from hsInverters where time LIKE '%" + date + "%' and inverter_num='1'");
				float current_power = rs_inverter.getFloat("current_power");
				float day_power = rs_inverter.getFloat("day_power");
				float total_power = rs_inverter.getFloat("total_power");
				float output_current = rs_inverter.getFloat("output_current");
				float output_voltage = rs_inverter.getFloat("output_voltage");
				float input_power = rs_inverter.getFloat("input_power");
				float input_current = rs_inverter.getFloat("input_current");
				float input_voltage = rs_inverter.getFloat("input_voltage");
				float frequency = rs_inverter.getFloat("frequency");
				
				rs_inverter = stat.executeQuery("select * from hsSensors where time LIKE '%" + date + "%'");
				
				stat_inverter.close();
				rs_inverter.close();
				
				System.out.println("today_gen: " + day_power + ", month_gen: " + month_gen + ", acc_gen: " + total_power + ", now_gen: " + current_power + ", co_dec: " + co_dec + "\n");
				
				System.out.println("RECV(" + portName + "):" + new String(completeBuffer));
				int packetSize = 20;
				System.out.println("RECV size :" + completeBuffer.length);
				for (int i = 0; i < completeBuffer.length; i++) {
					if (completeBuffer[i] == ENQ) {
						if (i > completeBuffer.length - packetSize)
							break;
						// System.out.println("position: "+i);
						byte[] bytes = new byte[packetSize];
						System.arraycopy(completeBuffer, i, bytes, 0, packetSize);

						String readLine = new String(bytes);
						// System.out.println(readLine);
						try {
							if (readLine.contains("MW000003")) {
								// 1: 태양전지 상태표시, 2: 인버터 상태표시, 3: 계통 상태표시
								sPort.writeBytes(makeBytes12(makeError(), makeError(), makeError()));
							} else if (readLine.contains("MW003203")) {
								// 1:태양전지 전압, 2: 태양전지 전류, 3:태양전지 전력(순시)
								sPort.writeBytes(makeBytes12((int)input_voltage,(int)(input_current*10), (int)(input_power*10)));
							} else if (readLine.contains("MW003502")) {
								// 1:태양전지 전력량(하위메모리), 2:태양전지 전력량(상위메모리)
								ConvertUpAndLow convert = new ConvertUpAndLow( (int)(month_gen*0.1) );
								sPort.writeBytes(makeBytes4(convert.getLow(), convert.getHigh()));
							} else if (readLine.contains("MW006403")) {
								// 1:계통 R상 전압(RMS), 2:계통 S상 전압(RMS), 3:계통 T상 전압(RMS)
								sPort.writeBytes(makeBytes12((int)output_voltage, (int)output_voltage, (int)output_voltage));
							} else if (readLine.contains("MW006703")) {
								// 1:계통 R상 전류(RMS), 2:계통 S상 전류(RMS), 3:계통 T상 전류(RMS)
								sPort.writeBytes(makeBytes12((int)(output_current*10), (int)(output_current*10), (int)(output_current*10)));
							} else if (readLine.contains("MW007003")) {
								// 1:계통 주파수, 2: 역률, 3:계통 전력(순시)
								sPort.writeBytes(makeBytes12((int)(frequency*10), 995, (int)(current_power*10)));
							} else if (readLine.contains("MW007302")) {
								// 1:계통 출력 전력량(하위), 2:계통 출력 전력량(상위) 
								ConvertUpAndLow convert = new ConvertUpAndLow((int)(total_power*0.1));
								sPort.writeBytes(makeBytes4(convert.getLow(), convert.getHigh()));
							} else if (readLine.contains("MW007702")) {
								// 1: 금일 전력량(하위), 2: 금일 전력량(상위) 
								ConvertUpAndLow convert = new ConvertUpAndLow((int)(day_power*0.01));
								sPort.writeBytes(makeBytes4(convert.getLow(), convert.getHigh()));
							}
						} catch (SerialPortException e) {
							System.out.println(e);
						}
						// i += packetSize;
					}
				}
				num += 2;
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			/*
			 * addByteArray(datas, ENQ); addByteArray(datas, "0");
			 * addByteArray(datas, String.valueOf(invertNum));
			 * addByteArray(datas, "rSB07%MW"); addByteArray(datas, inputData);
			 * addByteArray(datas, EOT);
			 * 
			 * 01rSB07%MW000003C4
			 */
			// System.out.println("T/F:"+pParser.isRead());
			// selectParser(portName, buffer);

		}
	}

	private final static byte ENQ = 0x05;
	// private final static byte EOT = 0x04;
	private final static byte ACK = 0x06;
	private final static byte ETX = 0x03;

	private byte computationBCC(ArrayList<Byte> bytes) {
		byte sum = 0;
		for (byte b : bytes) {
			sum += b;
		}

		return sum;
	}

	private String byteToHexString(byte b) {
		String out = "";
		if (b <= 0xF) {
			out += "0";
		}

		out += Integer.toHexString(b);

		out = out.substring(out.length() - 2, out.length());
		out = out.toUpperCase();
		return out;
	}

	private void addBytes(ArrayList<Byte> list, byte[] bytes) {
		if (bytes != null) {
			for (int i = 0; i < bytes.length; i++)
				list.add(bytes[i]);
		}
	}

	private byte[] toBytes(ArrayList<Byte> list) {
		byte[] bytes = new byte[list.size()];
		for (int i = 0; i < list.size(); i++) {
			bytes[i] = list.get(i);
		}
		return bytes;
	}

	private String toByteString(int num) {
		return String.format("%02X", num);
	}

	private String toIntString(int num) {
		return String.format("%04X", num);
	}

	private int makeError() {
		Random random = new Random();
		int[] value = { 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024 };
		return value[random.nextInt(12)];
	}

	private int makeRandom() {
		Random random = new Random();
		return random.nextInt(10000);
	}

	private byte[] makeBytes12(int v1, int v2, int v3) {
		ArrayList<Byte> list = new ArrayList<Byte>();
		list.add(ACK);
		String rno = toByteString(6);

		addBytes(list, ("01rSB01" + rno).getBytes());
		addBytes(list, toIntString(v1).getBytes());
		addBytes(list, toIntString(v2).getBytes());
		addBytes(list, toIntString(v3).getBytes());
		list.add(ETX);

		byte bcc = computationBCC(list);
		addBytes(list, byteToHexString(bcc).getBytes());

		System.out.println("write: " + new String(toBytes(list)));

		return toBytes(list);
	}

	private byte[] makeBytes4(int v1, int v2) {
		ArrayList<Byte> list = new ArrayList<Byte>();
		list.add(ACK);
		String rno = toByteString(4);

		addBytes(list, ("01rSB01" + rno).getBytes());
		addBytes(list, toIntString(v1).getBytes());
		addBytes(list, toIntString(v2).getBytes());
		list.add(ETX);

		byte bcc = computationBCC(list);
		addBytes(list, byteToHexString(bcc).getBytes());

		System.out.println("write: " + new String(toBytes(list)));

		return toBytes(list);
	}

}