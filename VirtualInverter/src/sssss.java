import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class sssss extends Thread {
	
	public Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:/C:/Users/Jihoon/Downloads/hsEnergyData.db");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return conn;
	}
	
	public void start() {
		// TODO Auto-generated method stub
		Connection conn = getConnection();
		try {
			long time = System.currentTimeMillis();
			SimpleDateFormat dayTime = new SimpleDateFormat("YYYY-MM-dd HH:mm");
			String str = dayTime.format(new Date(time));
			String date = str.substring(11, str.length() - 1);
			
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("select * from hsPlantDisplay where time LIKE '%" + date + "%'");
			int today_gen = rs.getInt("day_power");
			int month_gen = rs.getInt("month_power");
			int accumulated_gen = rs.getInt("total_power");
			int now_gen = rs.getInt("current_power");
			
			rs = stat.executeQuery("select * from hsSensors where time LIKE '%" + date + "%'");
			int co_dec = rs.getInt("co2");
			
			System.out.println("today_gen: " + today_gen + ", month_gen: " + month_gen + ", acc_gen: " + accumulated_gen + ", now_gen: " + now_gen + ", co_dec: " + co_dec + "\n");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		sssss s = new sssss();
		s.start();
	}

}
