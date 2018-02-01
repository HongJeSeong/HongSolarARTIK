import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBInserter {
	private Connection conn;
	
	public Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC").newInstance();
			conn = DriverManager.getConnection("jdbc:sqlite:/hongsolar/hsEnergyData.db");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return conn;
	}
	
	public void setDB() {
		this.conn = getConnection();
	}
	
	public Connection getDB() {
		return this.conn;
	}
	
	public void closeDB() throws SQLException {
		if (this.conn != null) {
			this.conn.close();
		}
	}
	
	public void makeDBTable() {
		try {
			Statement stat = conn.createStatement();

			stat.executeUpdate("drop table if exists Virtual_Inverter;");
			stat.executeUpdate("create table Virtual_Inverter ("
					+ "HANJEON INTEGER,"
					+ "MONTH_GEN INTEGER,"
					+ "CO_DEC INTEGER,"
					+ "NOW_GEN INTEGER,"
					+ "ACCUMULATED_GEN INTEGER,"
					+ "TODAY_GEN INTEGER,"
					+ "TIME TEXT"
					+ ");");
			stat.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void insertDB(ArrayList<String> arrData) throws SQLException {
//		String sql = "";
//		String workingMessage = "";
//		getDB().setAutoCommit(false);
//		
//		sql = "insert into Virtual_Inverter values (?, ?, ?, ?, ?, ?)";
//		workingMessage = "insert Virtual_Inverter<" + arrData.size() + ">";
//		
//		PreparedStatement preStat = getDB().prepareStatement(sql);
//		System.out.println(workingMessage);
//		
//		int i = 0;
//		for (int data : arrData) {
//			preStat.setInt(i + 1, data);
//		}
//		preStat.addBatch();
//		preStat.executeBatch();
//		getDB().commit();
//		preStat.clearBatch();
//
//		preStat.close();
//		getDB().setAutoCommit(true);
		
		String sql = "insert into Virtual_Inverter values ('" + Integer.parseInt(arrData.get(0))
				+ "', '" + Integer.parseInt(arrData.get(1))
				+ "', '" + Integer.parseInt(arrData.get(2))
				+ "', '" + Integer.parseInt(arrData.get(3))
				+ "', '" + Integer.parseInt(arrData.get(4))
				+ "', '" + Integer.parseInt(arrData.get(5))
				+ "', '" + arrData.get(6)
				+ "');";
		Statement stat = getDB().createStatement();
		stat.executeUpdate(sql);
		
		System.out.println("..complete.");
	}
}
