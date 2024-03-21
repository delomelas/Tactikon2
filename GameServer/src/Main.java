

public class Main
{

	
	public static void main(String [ ] args)
	{
		DBStuff.getInstance().CreateDB();
		NotificationQueueDB.getInstance().CreateDB();
		SyncListDB.getInstance().CreateDB();
		PlayersInGamesDB.getInstance().CreateDB();
		PlayerProfileUpdateQueueDB.getInstance().CreateDB();
		ProfileDB.getInstance().CreateDB();
		
		//ThreadPooledServer server = new ThreadPooledServer(12869);
		//new Thread(server).start();
		
		SSLThreadPooledServer sslServer = new SSLThreadPooledServer(8443);
		new Thread(sslServer).start();
		
		
		NotificationThread notificationThread = new NotificationThread();
		notificationThread.start();
		
		TactikonPlayerMarauder tactPlayerMarauder = new TactikonPlayerMarauder();
		tactPlayerMarauder.start();
		
		//TactikonGameCreator tactGameCreator = new TactikonGameCreator();
		//tactGameCreator.start();
		
		PlayerProfileThread playerProfileThread = new PlayerProfileThread("uk.co.eidolon.tact2");
		playerProfileThread.start();
		
		
		boolean stopped = false;
		
		while(stopped == false)
		{
			try
			{
				Thread.sleep(5000);
			} catch (InterruptedException e)
			{
			}
		}
	
		System.out.println("Stopping Server");
		//server.stop();
		sslServer.stop();
	}

}
