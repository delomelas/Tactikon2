import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.DeflaterOutputStream;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.LockingMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteOpenMode;

import Core.IState;
import Network.SyncList;
import Network.UserInfo;
import Tactikon.State.TactikonState;


public class DBStuff
{
	
	static DBStuff gDBStuff = new DBStuff();
    
	Connection db;
	
	ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	//Lock readLock = lock.readLock();
	Lock writeLock = lock.writeLock();
    
    static DBStuff getInstance()
    {
    	return gDBStuff;
    }
    
    String getUserInfoStr = "SELECT users.accountName, users.alias, users.colour, users.logo from users WHERE userId = ?";
    String getUserInfoFromNameStr = "SELECT users.userId, users.alias, users.colour, users.logo from users WHERE accountName = ?";
    String addUserStr = "INSERT INTO users (accountName, alias, colour, logo) VALUES (?, ?, ?, ?)";
    String updateUserStr = "UPDATE users SET alias = ?, colour = ?, logo = ? WHERE userId = ?";
    String updateGCMRegIdStr = "UPDATE users SET regId = ? WHERE userId = ?";
    String getGCMRegIdStr = "SELECT users.regId from users WHERE userId = ?";
    String addNewGameStr = "INSERT INTO games (sequence, status, lastUpdate, appVersion, appName, data) VALUES (?, ?, ?, ?, ?, ?)";
    String searchGamesStr = "SELECT games.gameId, games.data FROM games WHERE appVersion <= ? AND status == ? AND appName = ? LIMIT ?, ?";
    String getGameStr = "SELECT games.data FROM games WHERE gameId = ? AND appVersion <= ? AND appName = ?";
    String getSequenceNumStr = "SELECT games.sequence FROM games WHERE gameId = ?";
    String updateGameStr = "UPDATE games SET sequence = ?, data = ?, lastUpdate = ?, status = ? WHERE gameId = ?";
    String getActiveGamesStr = "SELECT games.gameId FROM games WHERE status == 1 AND appName = ?";
    String getWaitingGamesStr = "SELECT games.gameId FROM games WHERE status == 0 AND appName = ?";
    String getAllGamesStr = "SELECT games.gameId FROM games WHERE appName = ?";
    String getUserWithAliasStr = "SELECT users.alias, users.colour, users.logo, users.userId FROM users WHERE alias = ?";
    String getUserWithAliasFuzzyStr = "SELECT users.alias, users.colour, users.logo, users.userId FROM users WHERE alias = ? COLLATE NOCASE";
        
    DBStuff()
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
			db = DriverManager.getConnection("jdbc:sqlite:/home/eidolon/GameServer.db", config.toProperties());
			
			
			
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
    	
    	
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
    
    void CreateDB()
    {
    	
    	writeLock.lock();
		{
			// users table
    		//connection.exec("DROP TABLE IF EXISTS users");
			Statement st = getStatement();
			try
			{
				st.executeUpdate("CREATE TABLE users (userId INTEGER PRIMARY KEY AUTOINCREMENT, accountName VARCHAR(255) UNIQUE, state INTEGER, regId VARCVAR(255), alias VARCHAR(255) UNIQUE, logo VARCHAR(255), colour INTEGER )");
				st.executeUpdate("DROP TABLE IF EXISTS games");
				st.executeUpdate("CREATE TABLE games (gameId INTEGER PRIMARY KEY AUTOINCREMENT, sequence INTEGER, status INTEGER, lastUpdate LONG, appVersion INTEGER, appName VARCHAR(255), data BLOB )");
			} catch (SQLException e)
			{
				
			} 

		} 
		writeLock.unlock();
    }
    
    ArrayList<Integer> GetActiveGames(String appName)
    {
    	writeLock.lock();
    	
    	ArrayList<Integer> gameList = new ArrayList<Integer>();
    	
    	try (PreparedStatement getActiveGames = db.prepareStatement(getActiveGamesStr))
    	{
    		getActiveGames.setString(1, appName);
    		 ResultSet result = getActiveGames.executeQuery();
    		 while (result.next())
    		 {
    			gameList.add(result.getInt(1));
    			 
    		 }
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while retreiving active game list");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    
    	return gameList;
    }
    
    ArrayList<Integer> GetWaitingGames(String appName)
    {
    	writeLock.lock();
    	
    	ArrayList<Integer> gameList = new ArrayList<Integer>();
    	
    	try (PreparedStatement getWaitingGames = db.prepareStatement(getWaitingGamesStr))
    	{
    		getWaitingGames.setString(1, appName);
    		 ResultSet result = getWaitingGames.executeQuery();
    		 while (result.next())
    		 {
    			gameList.add(result.getInt(1));
    			 
    		 }
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while retreiving waiting game list");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    
    	return gameList;
    }
    
    ArrayList<Integer> GetAllGames(String appName)
    {
    	writeLock.lock();
    	
    	
    	ArrayList<Integer> gameList = new ArrayList<Integer>();
    	
    	try (PreparedStatement getAllGames = db.prepareStatement(getAllGamesStr))
    	{
    		getAllGames.setString(1, appName);
    		 ResultSet result = getAllGames.executeQuery();
    		 while (result.next())
    		 {
    			gameList.add(result.getInt(1));
    			 
    		 }
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while retreiving entire game list");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    
    	return gameList;
    }
    
    UserInfo GetUserInfo(final long userId)
    {
    	writeLock.lock();
    	
    	
    	
    	try (PreparedStatement getUserInfo = db.prepareStatement(getUserInfoStr))
    	{
    		 getUserInfo.setLong(1, userId);
    		 
    		 ResultSet result = getUserInfo.executeQuery();
    		 if (result.next())
    		 {
    			 UserInfo userInfo = new UserInfo();
    			 userInfo.userId = userId;
    			 userInfo.accountName = result.getString(1);
    			 userInfo.alias = result.getString(2);
    			 userInfo.colour = result.getInt(3);
    			 userInfo.logo = result.getString(4);
    			 //System.out.println("INFO: Found User [" + userId + "] in DB");
    			 return userInfo;
    		 }
    
    		System.out.println("INFO: Could not find user [" + userId + "] in DB");
    		return null;
    		 
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while searching account [" + userId+ "]");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    }
    
    UserInfo SearchAlias(final String alias)
    {
    	writeLock.lock();
    	
    	
    	try (PreparedStatement getUserWithAlias = db.prepareStatement(getUserWithAliasStr))
    	{
    		getUserWithAlias.setString(1, alias);
    		 
    		 ResultSet result = getUserWithAlias.executeQuery();
    		 if (result.next())
    		 {
    			 UserInfo userInfo = new UserInfo();
    			 
    			 userInfo.alias = result.getString(1);
    			 userInfo.colour = result.getInt(2);
    			 userInfo.logo = result.getString(3);
    			 userInfo.userId = result.getInt(4);;
    			 return userInfo;
    		 }

    		 
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while searching alias [" + alias+ "]");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    	
    	// if that failed, we'll try again with a case-insensitive search
    	
    	writeLock.lock();
    	
    	try (PreparedStatement getUserWithAliasFuzzy = db.prepareStatement(getUserWithAliasFuzzyStr))
    	{
    		getUserWithAliasFuzzy.setString(1, alias);
    		 
    		 ResultSet result = getUserWithAliasFuzzy.executeQuery();
    		 if (result.next())
    		 {
    			 UserInfo userInfo = new UserInfo();
    			 
    			 userInfo.alias = result.getString(1);
    			 userInfo.colour = result.getInt(2);
    			 userInfo.logo = result.getString(3);
    			 userInfo.userId = result.getInt(4);;
    			 return userInfo;
    		 }

    		 
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while searching alias [" + alias+ "]");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    	
    	// and return null if we found nothing :(
    	return null;
    }
    
    UserInfo GetUserInfo(final String accountName)
    {
    	writeLock.lock();
    	
    	
    	 try (PreparedStatement getUserInfoFromName = db.prepareStatement(getUserInfoFromNameStr))
    	 {
    		 getUserInfoFromName.setString(1, accountName);
    		 
    		 ResultSet result = getUserInfoFromName.executeQuery();
    		 if (result.next())
    		 {
    			 UserInfo userInfo = new UserInfo();
    			 userInfo.accountName = accountName;
    			 userInfo.userId = result.getLong(1);
    			 userInfo.alias = result.getString(2);
    			 userInfo.colour = result.getInt(3);
    			 userInfo.logo = result.getString(4);
    			 System.out.println("INFO: Found User [" + accountName + "] in DB");
    			 return userInfo;
    		 }
    
    		System.out.println("INFO: Could not find user [" + accountName + "] in DB");
    		return null;
    		 
    	 } catch (SQLException e)
    	 {
    		 System.out.println("ERROR: SQL Error while searching account [" + accountName+ "]");
    		 return null;
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    }
    
    boolean UpdateGCMRegId(final long UserId, final String RegId)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement updateGCMRegId = db.prepareStatement(updateGCMRegIdStr))
	    {
    		updateGCMRegId.setString(1,  RegId);
    		updateGCMRegId.setLong(2, UserId);
    		
    		updateGCMRegId.executeUpdate();
    		
		} catch (SQLException e)
		{		    	
			System.out.println(e.toString());
		    System.out.println("ERROR: Could not write user reg id to DB");
		    return false;
		} finally
		{
			writeLock.unlock();
		}
			
	    return true;
		
    }
    
    String GetGCMRegId(final int UserId)
    {
    	 String GCMRegId = "";
    	writeLock.lock();
     	
    	 try (PreparedStatement getGCMRegId = db.prepareStatement(getGCMRegIdStr))
    	 {
    		 getGCMRegId.setInt(1, UserId);
    		 
    		 ResultSet results = getGCMRegId.executeQuery();
    		 
    		 results.next();
    		 
    		 GCMRegId = results.getString(1);
    		 
    		 return GCMRegId;
    		  
    	 } catch (SQLException e)
    	 {
    		 System.out.println("Error: Could not find Registration ID for user: " + UserId);
    		 return "";
    	 } finally
    	 {
    		 writeLock.unlock();
    	 }
    }
    
    UserInfo AddUserInfo(final UserInfo info)
    {
    	writeLock.lock();
    	
    	
    	try (PreparedStatement addUser = db.prepareStatement(addUserStr))
    	{
    		addUser.setString(1, info.accountName);
    		addUser.setString(2, info.alias);
    		addUser.setInt(3, info.colour);
    		addUser.setString(4, info.logo);
    		addUser.executeUpdate();
    		
    		ResultSet results = addUser.getGeneratedKeys();
    		results.next();
    		
    		UserInfo userInfo = new UserInfo();
		    userInfo.userId = results.getLong(1);
		    userInfo.accountName = info.accountName;
		    userInfo.colour = info.colour;
		    userInfo.alias = info.alias;
		    return userInfo;
		}
		catch (SQLException e)
		{
			System.out.println("ERROR: SQL Error while adding account [" + info.accountName+ "]");
			return null;
		} finally
		{
			writeLock.unlock();
		}
    }
    
    boolean UpdateUserInfo(final UserInfo info)
    {
    	writeLock.lock();
    	
    	
    	try (PreparedStatement addUser = db.prepareStatement(updateUserStr))
    	{
    		addUser.setString(1, info.alias);
    		addUser.setInt(2, info.colour);
    		addUser.setString(3, info.logo);
    		addUser.setLong(4, info.userId);
    		addUser.executeUpdate();

    		return true;
		}
		catch (SQLException e)
		{
			System.out.println("ERROR: SQL Error while updating account [" + info.accountName+ "]");
			return false;
		} finally
		{
			writeLock.unlock();
		}
    }
    
    long AddNewGame(final int sequenceId, final int status, final long lastUpdate, final int appVersion, final String appName, final IState state)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement addNewGame = db.prepareStatement(addNewGameStr))
		{
    		addNewGame.setInt(1, sequenceId);
    		addNewGame.setInt(2, status);
    		addNewGame.setLong(3, lastUpdate);
    		addNewGame.setInt(4, appVersion);
    		addNewGame.setString(5, appName);
    		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
	    	DeflaterOutputStream defOutput = new DeflaterOutputStream(byteArray, true);
	    	DataOutputStream dataOutput = new DataOutputStream(defOutput);
	    	
    		state.StateToBinary(dataOutput);
    		
    		defOutput.finish();
    		
    		addNewGame.setBytes(6, byteArray.toByteArray());
    		
    		addNewGame.executeUpdate();
    		
    		ResultSet results = addNewGame.getGeneratedKeys();
    		results.next();
    		
    		long GameId = results.getLong(1);
    		
    		return GameId;    		
    		
    		 
    	 } catch (IOException e)
		 {
    		 System.out.println("ERROR: Could not write game binary to DB");
    		 return (long) -1;
		 }catch (SQLException e)
		 {
			 System.out.println("ERROR: Could not write game binary to DB");
    		 return (long) -1;
		 } finally
    	 {
			 writeLock.unlock();
    	 }
		    	 
		 
    }
    
    public class SearchGameResults
    {
    	public ArrayList<IState> resultData = new ArrayList<IState>();
    	public ArrayList<Integer> resultGames = new ArrayList<Integer>();
    	public boolean bMoreResults;
    }
    
    IState NewGameFactory(String appName)
    {
    	IState state = null;
    	if (appName.compareTo("uk.co.eidolon.nim") == 0)
    	{
    		// ARGH
    	} else if (appName.compareTo("uk.co.eidolon.tact2") == 0)
    	{
    		state = new TactikonState();
    	}
    	return state;
    }
    
    SearchGameResults SearchGames(final int appVersion, final String appName, final int offset, final int maxResults, final IState.GameStatus searchState, int searchPlayerId)
    {
    	// games db table: //gameId, sequence, status, lastUpdate, appVersion, appName, data
    	//"SELECT games.gameId, games.data FROM games WHERE appVersion <= ? AND status == ? AND appName = ? LIMIT ?, ?"
    	writeLock.lock();
    	
    	 try (PreparedStatement searchGames = db.prepareStatement(searchGamesStr))
    	 {
    		 SearchGameResults results = new SearchGameResults();
    		 
    		 searchGames.setInt(1,  appVersion);
    		 searchGames.setInt(2, searchState.ordinal());
    		 searchGames.setString(3, appName);
    		 searchGames.setInt(4, 0);
    		 searchGames.setInt(5, 256);
    		 
    		 results.bMoreResults = false;
    		 
    		 ResultSet resultSet = searchGames.executeQuery();
    		 
    		 int num = 0;
    		 
    		 while (resultSet.next())
    		 {
    			if (results.resultData.size() == maxResults) 
    			{
    				results.bMoreResults = true;
    				break;
    			}
    			
    			int gameId = resultSet.getInt(1);
    			
    			byte[] bytes = resultSet.getBytes(2);
    			
    			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
     			DataInputStream in = new DataInputStream(new InflaterInputStream(bis));
     			IState state = NewGameFactory(appName);
    			try
     			{
     				state.BinaryToState(in);
     			} catch (Exception e)
     			{
     				System.out.println("ERROR: Error deserialising state binary - skipping game " + gameId);
     				results.bMoreResults = true;
     				continue;
     			}
    			
    			// exclude non-friends from friends-only games
    			if (state.IsFriendsOnly() == true)
    			{
    				SyncList friends = SyncListDB.getInstance().GetSyncList(state.GetCreatorId(), "FriendList", appName);
    				if (friends.GetList().contains(searchPlayerId) == false && searchPlayerId != state.GetCreatorId())
    				{
    					results.bMoreResults = true;
    					System.out.println("[Search]: filtered out game " + gameId + " because " + searchPlayerId + " is not in " + state.GetCreatorId() + "'s friendslist");
    					continue;
    				}
    			}
    			SyncList blockList = SyncListDB.getInstance().GetSyncList(state.GetCreatorId(), "BlockList", appName);
    			if (blockList.GetList().contains(searchPlayerId) == true)
    			{
    				results.bMoreResults = true;
					System.out.println("[Search]: filtered out game " + gameId + " because " + searchPlayerId + " in " + state.GetCreatorId() + "'s blocklist");
					continue;
    			}
    			
    			num++;
    			
    			if (num < offset) continue;
    			
    			results.resultData.add(state);
    			results.resultGames.add(gameId);
    			
    		 }
    		 
    		 System.out.println("INFO: Sending ["+results.resultGames.size()+"] results.");
    		 return results;
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while performing game search");
			 System.out.println(e.toString());
    		 return null;
		 } finally
    	{
			 writeLock.unlock();
    	}

    }
    
    IState GetGame(final int gameId, final int appVersion, final String appName)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement getGame = db.prepareStatement(getGameStr))
    	{
    		IState state = null;
    		//st = connection.prepare("SELECT games.data FROM games WHERE gameId = ? AND appVersion <= ? AND appName = ?");
    		
    		getGame.setInt(1,  gameId);
    		getGame.setInt(2,  appVersion);
    		getGame.setString(3, appName);
    		
    		ResultSet results = getGame.executeQuery();
    		
    		if (results.next())
    		{
    			byte[] bytes = results.getBytes(1);
    			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
     			InflaterInputStream inflaterStream = new InflaterInputStream(bis);
     			DataInputStream in = new DataInputStream(inflaterStream);
     			state = NewGameFactory(appName);
     			
     			try
     			{
     				state.BinaryToState(in);
     			} catch (ZipException e)
     			{
     				state = NewGameFactory(appName);
     				System.out.println("ERROR: Failed to read zipped data. falling back to unzipped.");
     				bis = new ByteArrayInputStream(bytes);
     				in = new DataInputStream(bis);
         			
         			state = NewGameFactory(appName);
         			state.BinaryToState(in);
     			} catch (EOFException e)
     			{
     				state = NewGameFactory(appName);
     				System.out.println("ERROR: Failed to read zipped data. falling back to unzipped.");
     				bis = new ByteArrayInputStream(bytes);
     				in = new DataInputStream(bis);
         			
         			state = NewGameFactory(appName);
         			state.BinaryToState(in);
     			} catch (ArrayIndexOutOfBoundsException e)
     			{
     				state = NewGameFactory(appName);
     				System.out.println("ERROR: Failed to read zipped data. falling back to unzipped.");
     				bis = new ByteArrayInputStream(bytes);
     				in = new DataInputStream(bis);
         			
         			state = NewGameFactory(appName);
         			state.BinaryToState(in);
     			} 
    		 } else
    		 {
    			 
    			 return null;
    		 }
    		 
    		 return state;
    	 } catch (SQLException e)
		 {
			 System.out.println("ERROR: DB Error while getting game");
			 System.out.println(e.toString());
    		 return null;
		 } catch (IOException e)
		{
			System.out.println("ERROR: Error deserialising state binary");
			System.out.println(e.toString());
    		return null;
		} finally
    	{
			writeLock.unlock();
    	}
    }
    
    
    Boolean UpdateGame(final int gameId, final IState state, String appName, boolean updateTime)
    {
    	writeLock.lock();
    	
    	try (PreparedStatement getSequenceNum = db.prepareStatement(getSequenceNumStr))
    	{
    		getSequenceNum.setInt(1,  gameId);
    		
    		ResultSet results = getSequenceNum.executeQuery();
    		
    		if (results.next() == false) return false;
    		
    		int sequenceNum = results.getInt(1);
    		
    		getSequenceNum.close();
    		
    		PreparedStatement updateGame = db.prepareStatement(updateGameStr);
    		
	    	updateGame.setInt(1,  sequenceNum);
	    	ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
	    	DeflaterOutputStream defOutput = new DeflaterOutputStream(byteArray, true);
	    	DataOutputStream dataOutput = new DataOutputStream(defOutput);
	    	
    		state.StateToBinary(dataOutput);
    		
    		defOutput.finish();
    		
    		updateGame.setBytes(2, byteArray.toByteArray());
    		
    		long lastUpdate = System.currentTimeMillis() / 1000;
    		
    		if (updateTime == true)
    		{
    			updateGame.setLong(3, lastUpdate);
    		}
    		
    		updateGame.setInt(4, state.GetGameState().ordinal());
    		updateGame.setInt(5, gameId);
    		
    	    updateGame.execute();
    		
    	} catch (IOException e)
		{
    		System.out.println("ERROR: Could not write game update binary to DB");
    		return false;
		}catch (SQLException e)
		{
			System.out.println(e.toString());
    		System.out.println("ERROR: Could not write game update binary to DB");
    		return false;
		} finally
    	{
			writeLock.unlock();
    	}
		
		// notify players about this update
		ArrayList<Integer> players = state.GetPlayers();
		for (int i : players)
		{
			NotificationQueueDB.getInstance().AddToQueue(i, gameId);
		}
		
		// update the player profiles for this game
		for (int i : players)
		{
			PlayerProfileUpdateQueueDB.getInstance().AddToQueue(i, appName);
		}
    
		return true;
     }

}
