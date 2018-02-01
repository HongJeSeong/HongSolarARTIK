import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WillingsProtocolParser {

	public WillingsProtocolParser() {
		initParser();
	}

	private final static byte ENQ = 0x05;
	private final static byte EOT = 0x04;
	private final static byte ACK = 0x06;
	private final static byte ETX = 0x03;
	
	public final static int SYSTEM_STATUS = 1;
	public final static int PV_POWER = 2;
	public final static int PV_ACC_POWER = 3;
	public final static int INV_VOLT = 4;
	public final static int INV_CURRENT = 5;
	public final static int AC_FREQ = 6;
	public final static int ACC_POWER = 7;
	public final static int DAY_POWER = 8;
	
	private int protocolNum = 0;
	//index procees
	public void readParser(byte[] readData, DBInserter db) throws SQLException
	{
		//protocolNum = 0;
		if(protocolNum % DAY_POWER == 0)
			protocolNum = 0;

		ArrayList<String> arrData = new ArrayList<>();
		
		for(int i = 0; i < readData.length; i++)
		{
			if(readData[i] == ACK)
			{
				protocolNum++; //first number is 1;
				ArrayList<Byte> list = new ArrayList<Byte>();
				list.add(readData[i]);
				
				byte[] ADDR = new byte[2];
				i++;
				ADDR[0] = readData[i];
				i++;
				ADDR[1] = readData[i];
				addByteArray(list, ADDR);
				String strAddr = byteToString(ADDR);
				//System.out.println(strAddr); //InvererNum
				int inverterNum = Integer.parseInt(strAddr, 16);
				
				for(int j=0; j < 5; j++)
				{
					i++;
					list.add(readData[i]);
				}
				
				/* block length */
				byte[] RNO = new byte[2];
				i++;
				RNO[0] = readData[i];
				i++;
				RNO[1] = readData[i];
				
				addByteArray(list, RNO);
				String strLen = byteToString(RNO);
				
				int len = Integer.parseInt(strLen, 16);
				//System.out.println("LEN: "+ strLen);
				
				/* block length */
				byte[] datas = new byte[len*2];
				for(int j=0; j < len*2; j++)
				{
					i++;
					list.add(readData[i]);
					datas[j] = readData[i];
				}
				
				//ETX
				i++;
				list.add(readData[i]);
				if(readData[i] != ETX)
				{
					continue;
				}
				
				byte[] BCC = new byte[2];
				i++;
				BCC[0] = readData[i];
				i++;
				BCC[1] = readData[i];
				
				String computationBCC = computationBCC(list);
				//System.out.println("read:"+byteToString(BCC)+", com: "+computationBCC);
				//error check
				if(!computationBCC.equals(byteToString(BCC)))
				{
					continue;
				}

				//System.out.println("device index:"+index+", start number:"+ getStartNumber());
				//System.out.println("num:"+inverterNum+",max:"+M_PVMSClient.pHelper.getInverterMax(index));
				//System.out.println("ParsedData: "+parsedData);
				//System.out.println("data index: "+ (inverterNum-1 + getStartNumber()) );
				
				if(inverterNum > 0 && inverterNum <= 1) {
					String data = Integer.toString(dataParser(protocolNum, byteToString(datas), inverterNum));
					System.out.println("Input: " + data);
					if (Integer.parseInt(data) != -1) {
						arrData.add(data);
					}
				}
				//System.out.println(byteToString(datas));
			}
			
			if(protocolNum % DAY_POWER == 0)
				protocolNum = 0;
		}
		
		long time = System.currentTimeMillis();
		SimpleDateFormat dayTime = new SimpleDateFormat("YYYY-MM-dd HH:mm");
		String str = dayTime.format(new Date(time));
		arrData.add(str);
		
		db.insertDB(arrData);
		Connection conn = db.getConnection();
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery("select * from virtual_inverter");
		System.out.println("show db start");
		while (rs.next()) {
			for (int i = 1; i < 8; i++) {
				System.out.println(rs.getInt(i));
			}
		}
	}
	
	private int dataParser(int index, String data, int num)
	{
		int arrData = -1;
		System.out.println("data parse");
		System.out.println("inverter number: "+num);
		
		switch(index)
		{
		case SYSTEM_STATUS:
			//System.out.println("data length: "+data.length());
			if(data.length() != 12)
				break;
			
			int pv_num = hexStringToInteger(0, data);
			int in_num = hexStringToInteger(4, data);
			int ph_num = hexStringToInteger(8, data);
			
			ErrorPrint(ErrorCheck("pv", pv_num));
			ErrorPrint(ErrorCheck("inverter", in_num));
			ErrorPrint(ErrorCheck("phase", ph_num));
			
			break;
		case PV_POWER:
			if(data.length() != 12)
				break;
			
			int pv_volt_num = hexStringToInteger(0, data);
			int pv_current_num = hexStringToInteger(4, data);
			int pv_power_num = hexStringToInteger(8, data);
			
			System.out.println( "PV Volt: "+pv_volt_num );
			System.out.println( "PV Current: "+pv_current_num*0.1 );
			System.out.println( "PV Power: "+pv_power_num*0.1 );
			
			arrData = (int)(pv_volt_num*0.1);
			
			break;
		case PV_ACC_POWER:
			int acc_num = hexStringToInteger(data);
			System.out.println("PV ACC POWER: "+ acc_num*10);
			
			arrData = (int)(acc_num * 10);
			
			break;
		case INV_VOLT:
			if(data.length() != 12)
				break;
			
			int ph_volt_R = hexStringToInteger(0, data);
			int ph_volt_S = hexStringToInteger(4, data);
			int ph_volt_T = hexStringToInteger(8, data);
			
			float ph_volt = (ph_volt_R + ph_volt_S + ph_volt_T)/3.f;
		
			System.out.println( "PH Volt R: "+ph_volt_R );
			System.out.println( "PH Volt S: "+ph_volt_S );
			System.out.println( "PH Volt T: "+ph_volt_T );
			System.out.println( "PH Volt: "+ph_volt );
			
			arrData = (int)ph_volt;
			
			break;
		case INV_CURRENT:
			if(data.length() != 12)
				break;
			
			int ph_current_R = hexStringToInteger(0, data);
			int ph_current_S = hexStringToInteger(4, data);
			int ph_current_T = hexStringToInteger(8, data);
			
			float ph_current = (ph_current_R + ph_current_S + ph_current_T)/3.f;
			
			System.out.println( "PH Current R: "+ph_current_R*0.1 );
			System.out.println( "PH Current S: "+ph_current_S*0.1 );
			System.out.println( "PH Current T: "+ph_current_T*0.1 );
			System.out.println( "PH Current: "+ph_current*0.1 );
			
			break;
		case AC_FREQ:
			if(data.length() != 12)
				break;
			
			int AC_freq_num = hexStringToInteger(0, data);
			int AC_pf_num = hexStringToInteger(4, data);
			int AC_power_num = hexStringToInteger(8, data);
		
			System.out.println( "AC Freq: "+AC_freq_num*0.1 );
			System.out.println( "AC PF: "+AC_pf_num*0.1 );
			System.out.println( "AC Power: "+AC_power_num*0.1 );
			
			arrData = (int)(AC_power_num*0.1);
			
			break;
		case ACC_POWER:
			int acc_power_num = hexStringToInteger(data);
			
			System.out.println("ACC Power: "+ acc_power_num*10);
			
			arrData = (int)(acc_power_num * 10);
			
			break;
		case DAY_POWER:
			int day_power_num = hexStringToInteger(data);
			System.out.println("Day Power: "+ day_power_num*10);
			
			arrData = (int)(day_power_num * 10);
			
			break;
		}
		
		return arrData;
	}
	
	public static byte[] writeParser(String inputData, int num, int invertNum)
	{
		//protocolNum = num;
		ArrayList<Byte> datas = new ArrayList<Byte>();
		addByteArray(datas, ENQ);
		addByteArray(datas, "0");
		addByteArray(datas, String.valueOf(invertNum));
		addByteArray(datas, "rSB07%MW");
		addByteArray(datas, inputData);
		addByteArray(datas, EOT);
		//System.out.println(computationBCC(datas));
		addByteArray(datas, computationBCC(datas));
		//System.out.println(datas.size());
		byte[] out = getBytes(datas);
		//System.out.println(new String(out));
		
		return out;
	}
	
	private static byte[] getBytes(ArrayList<Byte> byteList)
	{
		byte[] byteArray = new byte[byteList.size()];
		int index = 0;
		for (byte b : byteList) {
		    byteArray[index++] = b;
		}
		return byteArray;
	}
	
	private void initParser() {
		// TODO Auto-generated method stub
		protocolNum = 0;
	}
	
	private static void addByteArray(ArrayList<Byte> bytes, byte input )
	{
		bytes.add(new Byte(input));
	}
	
	private static void addByteArray(ArrayList<Byte> bytes, String input)
	{
		for(byte b : input.getBytes())
		{
			bytes.add(new Byte(b));
		}
	}
	
	
	private static void addByteArray(ArrayList<Byte> bytes, byte[] inputs )
	{
		for(byte b : inputs)
			bytes.add(new Byte(b));
	}
	
	
	private static String computationBCC(ArrayList<Byte> bytes)
	{
		byte sum = 0;
		for(byte b : bytes)
		{
			sum += b;
		}
		
		return byteToHexString(sum);
	}
	
	private static String byteToHexString(byte b)
	{
		String out = "";
		if(b <= 0xF)
		{
			out += "0";
		}
		
		out += Integer.toHexString(b);
		
		out = out.substring(out.length() - 2, out.length());
		out = out.toUpperCase();
		return out;
	}
	
	private String byteToString(byte[] bytes)
	{
		String retString = "";
		for(byte b : bytes)
			retString += (char)b;
		
		return retString;
	}
	
	private int hexStringToInteger(int index, String data)
	{
		//System.out.println(data);
		
		String subData = data.substring(index, index+4);
		if(!hexDataCheck(subData))
			return 0;
		
		return Integer.parseInt(subData, 16);
	}
	
	private int hexStringToInteger(String data)
	{
		if(data.length() != 8)
			return -1;
		
		String low = data.substring(0, 4);
		String high = data.substring(4, 8);
		String high_low = high + low;
		int num = Integer.parseInt(high_low, 16);
		return num;
	}
	
	private boolean hexDataCheck(String data)
	{
		for(int i=0; i < data.length(); i++)
		{
			String character = data.substring(i, i+1);
			if( !character.equals("0") && !character.equals("1") && !character.equals("2") &&
				!character.equals("3") && !character.equals("4") && !character.equals("5") &&
				!character.equals("6") && !character.equals("7") && !character.equals("8") &&
				!character.equals("9") && !character.equals("A") && !character.equals("B") &&
				!character.equals("C") && !character.equals("D") && !character.equals("E") &&
				!character.equals("F"))
				return false;
		}
		return true;
	}
	
	private ArrayList<String> ErrorCheck(String type, int num)
	{
		return ErrorCheckMode(type, num, 0);
	}
	private final static int MAX_ERROR = 15;
	private ArrayList<String> ErrorCheckMode(String type, int num, int start)
	{
		ArrayList<String> list = new ArrayList<String>();
		for(int i=start; i < MAX_ERROR; i++)
		{
			int errorNum = (int) Math.pow(2, i);
			if( (num & errorNum) == errorNum )
			{
				list.add(type + "_" + String.valueOf(i));
			}
		}
		return list;
	}
	
	private void ErrorPrint(ArrayList<String> list)
	{
		for(String n : list)
		{
			System.out.println( n );
		}
	}
}
