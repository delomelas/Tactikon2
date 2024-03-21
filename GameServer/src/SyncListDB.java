
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

import Core.IState;
import Network.SyncList;

public class SyncListDB
{
	static SyncListDB gSyncListDB = new SyncListDB();
    
    Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
	
	String setSyncListStr = "INSERT OR REPLACE INTO syncLists (list, appName, userId, listTag) VALUES (?, ?, ?, ?)";
	String getSyncListStr = "SELECT (list) FROM syncLists WHERE (listTag = ? AND appName = ? AND userId = ?)";
    
    static SyncListDB getInstance()
    {
    	return gSyncListDB;
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
    
    SyncListDB()
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
    		db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/SyncList.db");
    		
    	} catch (SQLException e)
    	{
    		e.printStackTrace();
    	}
    }
    
    void CreateDB()
    {
    	Statement st = getStatement();
    	try
		{
    		writeLock.lock();
			//st.execute("DROP TABLE IF EXISTS syncLists");
			st.execute("CREATE TABLE syncLists (listTag VARCHAR(32), appName VARCHAR(256), userId INTEGER, list BLOB, PRIMARY KEY ( listTag, appName, userId))");
		} catch (SQLException e)
		{
			e.printStackTrace();
		} finally
		{
			writeLock.unlock();
		}
    }
    
    void SetSyncList(final int UserID, final String tag, final String appName, SyncList list)
    {
    	writeLock.lock();
    	try (PreparedStatement setSyncList = db.prepareStatement(setSyncListStr))
    	{
    		setSyncList.setString(2, appName);
    		setSyncList.setInt(3, UserID);
    		setSyncList.setString(4, tag);
    		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    		list.ListToBinary(new DataOutputStream(byteArray));
    		setSyncList.setBytes(1, byteArray.toByteArray());
    		setSyncList.execute();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		System.out.println("ERROR: Unable to set synclist");
    	} finally
    	{
    		writeLock.unlock();
    	}
    }
    
    SyncList GetSyncList(final int UserID, final String tag, final String appName)
    {
    	readLock.lock();
    	try (PreparedStatement getSyncList = db.prepareStatement(getSyncListStr))
    	{
    		getSyncList.setString(1, tag);
    		getSyncList.setString(2, appName);
    		getSyncList.setInt(3, UserID);
    		
    	    ResultSet results = getSyncList.executeQuery();
    	    
    	    if (results.next())
    	    {
    	    	byte[] bytes = results.getBytes(1);
    	    	ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    			DataInputStream in = new DataInputStream(bis);
    			SyncList list = new SyncList(tag);
    			list.BinaryToList(in);
    			return list;
    	    } 
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		System.out.println("ERROR: Unable to retreive synclist");
    	} finally
    	{
    		readLock.unlock();
    	}
    	
    	return new SyncList(tag);
		     
    }
    
}
