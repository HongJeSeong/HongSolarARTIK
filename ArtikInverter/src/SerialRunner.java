public class SerialRunner {
	private int SECOND = 1000;
	public SerialRunner(String comname, DBInserter db)
	{
		//initial serial;
		SerialComm comm = new SerialComm();
		comm.connect(comname, 9600, db);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true)
				{
					comm.write();
					try {
						Thread.sleep(600*SECOND);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						System.out.println(e);
					}
				}
			}
		}).run();
	}
	
	public void stop()
	{
	}
}
