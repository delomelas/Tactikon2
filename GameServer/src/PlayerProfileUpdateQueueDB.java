
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

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class PlayerProfileUpdateQueueDB
{
	static PlayerProfileUpdateQueueDB gQueueDB = new PlayerProfileUpdateQueueDB();
    
    Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
	
	String addToQueueStr = "INSERT INTO requiresUpdate (userId, appName) VALUES (?, ?)";
	String deleteFromQueueStr = "DELETE FROM requiresUpdate WHERE (userId = ? AND appName = ?)";
	String getQueueItemsStr = "SELECT userId FROM requiresUpdate WHERE (appName = ?)";
	
    static PlayerProfileUpdateQueueDB getInstance()
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
    
    PlayerProfileUpdateQueueDB()
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
    		SQLiteConfig config = new SQLiteConfig();
    		config.setOpenMode(SQLiteOpenMode.FULLMUTEX);
    		db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/PlayerProfileUpdateQueue.db", config.toProperties());
    		
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
			st.execute("CREATE TABLE requiresUpdate (userId INTEGER, appName VARCHAR(255))");
		} catch (SQLException e)
		{
			e.printStackTrace();
		} finally
		{
			writeLock.unlock();
		}
    }
    
    
    
    void AddToQueue(final int UserID, String appName)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement addToQueue = db.prepareStatement(addToQueueStr))
    	{
    		addToQueue.setInt(1, UserID);
    		addToQueue.setString(2, appName);
    		addToQueue.execute();
    	}
    	catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error adding to playerProfile queue");
    	} finally
    	{
    		writeLock.unlock();
    	}
		     
    }
    
    
    void ClearFromQueue(int UserId, String appName)
    {
    	writeLock.lock();
    	
    	 try (PreparedStatement deleteFromQueue = db.prepareStatement(deleteFromQueueStr))
    	 {
    		deleteFromQueue.setInt(1, UserId);
    		deleteFromQueue.setString(2, appName);
    		deleteFromQueue.execute();
    		
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while deleting profile queue items");
			 System.out.println(e.toString());

		} finally
    	{
			writeLock.unlock();
    	}
    	
    }
    
    ArrayList<Integer> GetQueueItems(String appName)
    {
    	final ArrayList<Integer> queueItems = new ArrayList<Integer>();
    	readLock.lock();
    	
    	 try (PreparedStatement getQueueItems = db.prepareStatement(getQueueItemsStr))
    	 {
    		getQueueItems.setString(1, appName);
			ResultSet results = getQueueItems.executeQuery();
			
			while (results.next())
			{
				int userId = results.getInt(1);
				queueItems.add(userId);
			}
    		
    	 } catch (SQLException e)
		 {
			System.out.println("ERROR: DB Error while getting player profile queue");
			System.out.println(e.toString());
    		return null;
		} finally
    	{
			readLock.unlock();
    	}
		return queueItems;
    }
}
