
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class ProfileDB
{
	static ProfileDB gProfileDB = new ProfileDB();
    
    Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
	
	String addToProfileStr = "INSERT INTO profile (userId, appName, profileTag, profileValue) VALUES (?, ?, ?, ?)";
	String getProfileStr = "SELECT profileTag, profileValue FROM profile WHERE (userId = ? AND appName = ?)";
	String getPlayersWithTagStr = "SELECT userId FROM profile WHERE (appName = ? AND profileTag = ? AND profileValue = ?)";
	
	public static int PROFILE_WINS = 0;
	public static int PROFILE_LOSSES = 1;
	public static int PROFILE_PLAYED = 2;
	public static int PROFILE_PLAYING = 3;
	public static int PROFILE_LONGEST = 4;
	public static int PROFILE_RANK = 5;
	
	public static int PROFILE_TOTAL_IN_RANK = 6;
	public static int PROFILE_POSITION_IN_RANK = 7;
	
    static ProfileDB getInstance()
    {
    	return gProfileDB;
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
    
    ProfileDB()
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
    		db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/Profiles.db", config.toProperties());
    		
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
			st.execute("CREATE TABLE profile (userId INTEGER, appName VARCHAR(255), profileTag INTEGER, profileValue INTEGER, PRIMARY KEY (userId, appName, profileTag) ON CONFLICT REPLACE)");
		} catch (SQLException e)
		{
			e.printStackTrace();
		} finally
		{
			writeLock.unlock();
		}
    }
    
    Set<Integer> GetPlayersWithTag(int profileTag, int profileValue, String appName)
    {
    	Set<Integer> players = new TreeSet<Integer>();
    	readLock.lock();
    	
    	try (PreparedStatement getPlayersWithTag = db.prepareStatement(getPlayersWithTagStr))
    	{
    		getPlayersWithTag.setString(1, appName);
    		getPlayersWithTag.setInt(2, profileTag);
    		getPlayersWithTag.setInt(3, profileValue);
    		ResultSet results = getPlayersWithTag.executeQuery();
    		
    		while (results.next())
    		{
    			int userId = results.getInt(1);
    			
    			players.add(userId);
    		}
    	} catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error getting tag profile data");
    	}
    	finally
    	{
    		readLock.unlock();
    	}
    	
    	return players;
    }
    
    void AddToProfile(int userId, String appName, int profileTag, int profileValue)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement addToProfile = db.prepareStatement(addToProfileStr))
    	{
    		addToProfile.setInt(1, userId);
    		addToProfile.setString(2, appName);
    		addToProfile.setInt(3, profileTag);
    		addToProfile.setInt(4, profileValue);
    		
    		addToProfile.execute();
    	} catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error adding profile data");
    	}
    	finally
    	{
    		writeLock.unlock();
    	}
		     
    }
    
    
    TreeMap<Integer, Integer> GetProfile(int userId, String appName)
    {
    	TreeMap<Integer, Integer> profile = new TreeMap<Integer, Integer>();
    	readLock.lock();
    	
    	try (PreparedStatement getProfile = db.prepareStatement(getProfileStr))
    	{
    		getProfile.setInt(1, userId);
    		getProfile.setString(2, appName);
    		ResultSet results = getProfile.executeQuery();
    		
    		while (results.next())
    		{
    			int profileTag = results.getInt(1);
    			int profileValue = results.getInt(2);
    			profile.put(profileTag, profileValue);
    		}
    		
    	} catch (SQLException e)
		{
			System.out.println("ERROR: DB Error while getting profile data");
			System.out.println(e.toString());
    		return null;
		} finally
    	{
    		readLock.unlock();
    	}
		return profile;
    }
}
