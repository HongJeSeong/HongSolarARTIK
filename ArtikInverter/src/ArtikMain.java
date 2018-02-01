
public class ArtikMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Start Artik Main!!");
		
		DBInserter db = new DBInserter();
		db.setDB();
		db.makeDBTable();
		
		if(args.length == 1)
			new SerialRunner(args[0], db);
		else
			System.out.println("fail: you must have one argument");
	}
}
