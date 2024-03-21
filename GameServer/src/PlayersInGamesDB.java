
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

public class PlayersInGamesDB
{
	static PlayersInGamesDB gPlayersInGamesDB = new PlayersInGamesDB();
    
    Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
	
	String addPlayerToGameStr = "INSERT INTO playersInGames (userId, gameId, appName) VALUES (?, ?, ?)";
	String getGamesForPlayerStr = "SELECT gameId from playersInGames WHERE (userId = ? AND appName = ?)";
	
    static PlayersInGamesDB getInstance()
    {
    	return gPlayersInGamesDB;
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
    
    PlayersInGamesDB()
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
    		db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/PlayersInGames.db", config.toProperties());
    		
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
			st.execute("CREATE TABLE playersInGames (userId INTEGER, gameId INTEGER, appName VARCHAR(255), PRIMARY KEY (userId, gameId, appName) ON CONFLICT REPLACE)");
		} catch (SQLException e)
		{
			e.printStackTrace();
		} finally
		{
			writeLock.unlock();
		}
    }
    
    
    
    void AddPlayerToGame(final int UserID, final int GameID, final String appName)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement addPlayerToGame = db.prepareStatement(addPlayerToGameStr))
    	{
    		addPlayerToGame.setInt(1, UserID);
    		addPlayerToGame.setInt(2, GameID);
    		addPlayerToGame.setString(3, appName);
    		
    		addPlayerToGame.execute();
    		
    		
    	} catch (SQLException e)
    	{
    		System.out.println("ERROR: DB Error adding player to game");
    	}
    	
    	finally
    	{
    		writeLock.unlock();
    	}
		     
    }
    
    

    
    ArrayList<Integer> GetGamesForPlayer(int UserId, String appName)
    {
    	ArrayList<Integer> gamesForPlayer = new ArrayList<Integer>();
    	readLock.lock();
    	
    	 try (PreparedStatement getGamesForPlayer = db.prepareStatement(getGamesForPlayerStr))
    	 {
    		 getGamesForPlayer.setInt(1, UserId);
    		 getGamesForPlayer.setString(2,  appName);
    		 ResultSet results = getGamesForPlayer.executeQuery();
    		 
    		 while (results.next())
    		 {
    			 gamesForPlayer.add(results.getInt(1));
    		 }
    		 
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while players games list");
			 System.out.println(e.toString());
    		 return null;
		} finally
    	 {
    		 readLock.unlock();
    	 }
		return gamesForPlayer;
    }
}
