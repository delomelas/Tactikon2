
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NotificationQueueDB
{
	static NotificationQueueDB gQueueDB = new NotificationQueueDB();
    
    Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
	
	String addToQueueStr = "INSERT INTO notifications (userId, insertTime, gameId) VALUES (?, ?, ?)";
	String findInQueueStr = "SELECT InsertTime from notifications WHERE (userId = ? AND GameId = ?)";
	String deleteFromQueueStr = "DELETE FROM notifications WHERE (userId = ? AND GameId = ? AND InsertTime = ?)";
	String getQueueItemsStr = "SELECT userId, GameId, InsertTime FROM notifications";
	
    static NotificationQueueDB getInstance()
    {
    	return gQueueDB;
    }
    
    Statement getStatement()
    {
    	Statement statement = null;
		try
		{
			statement = db.createStatement();
			statement.setQueryTimeout(10);
		} catch (SQLException e)
		{
		}
		
    	return statement;
    }
    
    NotificationQueueDB()
    {
    	try
		{
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	try
    	{
    		db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/NotificationQueue.db");
    		
    	} catch (SQLException e)
    	{
    		
    	}
    }
    
    void CreateDB()
    {
    	Statement st = getStatement();
    	try
		{
    		writeLock.lock();
			//st.execute("DROP TABLE IF EXISTS notifications");
			st.execute("CREATE TABLE notifications (userId INTEGER, insertTime INTEGER, gameId INTEGER)");
		} catch (SQLException e)
		{
			e.printStackTrace();
		} finally
		{
			writeLock.unlock();
		}
    }
    
    
    
    void AddToQueue(final int UserID, final int GameID)
    {
    	writeLock.lock();
    	
    	// first search for any matching entries for this userId and gameId
    	final ArrayList<NotificationQueueItem> queueItems = new ArrayList<NotificationQueueItem>();
    	try (PreparedStatement findInQueue = db.prepareStatement(findInQueueStr))
    	{
    		findInQueue.setInt(1, UserID);
    		findInQueue.setInt(2, GameID);
    		
    		ResultSet results = findInQueue.executeQuery();
   		 
    		while (results.next())
   		 	{
   			 	NotificationQueueItem item = new NotificationQueueItem();
   			 	item.Time = results.getInt(1);
   			 	item.UserID = UserID;
   			 	item.GameID = GameID;
   			 	queueItems.add(item);
   		 	}
    		
    	} catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error searching notifcation queue");
    	}
    	
    	if (queueItems.size() > 0) // if there are any pre-existing items, delete them
    	{
    		try (PreparedStatement deleteFromQueue = db.prepareStatement(deleteFromQueueStr))
       	 	{
       		 for (NotificationQueueItem item : queueItems)
       		 {
       			 deleteFromQueue.setInt(1, item.UserID);
       			 deleteFromQueue.setInt(2, item.GameID);
       			 deleteFromQueue.setLong(3, item.Time);
       			
       			 deleteFromQueue.execute();
       		 }
       		  
       	 } catch (SQLException e)
   		 {
   			 System.out.println("ERROR: DB Error while deleting queue items in preparation for new addition");
   		}
    		
    	}
    	
    	// and finally add the new item
    	try (PreparedStatement addToQueue = db.prepareStatement(addToQueueStr))
    	{
    		long Time = (System.currentTimeMillis() / 1000);
    		addToQueue.setInt(1, UserID);
    		addToQueue.setLong(2, Time);
    		addToQueue.setInt(3, GameID);

    		addToQueue.execute();
    	}
    	catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error adding to notifcation queue");
    	} finally
    	{
    		writeLock.unlock();
    	}
		     
    }
    
    
    void ClearFromQueue(final ArrayList<NotificationQueueItem> items)
    {
    	if (items.size() == 0) return;
    	 writeLock.lock();
    	
    	 try (PreparedStatement deleteFromQueue = db.prepareStatement(deleteFromQueueStr))
    	 {
    		 for (NotificationQueueItem item : items)
    		 {
    			 deleteFromQueue.setInt(1, item.UserID);
    			 deleteFromQueue.setInt(2, item.GameID);
    			 deleteFromQueue.setLong(3, item.Time);
    			
    			 deleteFromQueue.execute();
    			 
    		 }
    		  
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while deleting queue items");
			 System.out.println(e.toString());

		} finally
    	{
			writeLock.unlock();
    	}
		
    	
    }
    
    ArrayList<NotificationQueueItem> GetQueueItems()
    {
    	final ArrayList<NotificationQueueItem> queueItems = new ArrayList<NotificationQueueItem>();
    	readLock.lock();
    	
    	 try (PreparedStatement getQueueItems = db.prepareStatement(getQueueItemsStr))
    	 {
    		 ResultSet results = getQueueItems.executeQuery();
    		 
    		 while (results.next())
    		 {
    			 NotificationQueueItem item = new NotificationQueueItem();
    			 item.UserID = results.getInt(1);
    			 item.GameID = results.getInt(2);
    			 item.Time = results.getInt(3);
    			 queueItems.add(item);
    		 }
    		 
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while getting notification queue");
			 System.out.println(e.toString());
    		 return null;
		} finally
    	 {
    		 readLock.unlock();
    	 }
		return queueItems;
    }
}
