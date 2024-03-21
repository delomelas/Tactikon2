package uk.co.eidolon.shared.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IEvent;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.SyncList;
import Network.UserInfo;
import Network.Packages.PackageEventList;
import Network.Packages.PackageGetProfile;
import Network.Packages.PackageSyncList;
import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

public class ChatDB extends SQLiteOpenHelper
{
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "chat";
 
    private static final String TABLE_CHAT = "chatMessages";
 
    // Chat Column names
    private static final String KEY_FROMUSERID = "fromUserId";
    private static final String KEY_TOUSERID = "toUserId";
    private static final String KEY_TOGAMEID = "toGameId";
    private static final String KEY_ID = "chatID";
    private static final String KEY_TIME = "chatTime";
    private static final String KEY_READ = "chatRead";
    private static final String KEY_MESSAGE = "chatMessage";
    private static final String KEY_LOGO = "fromLogo";
    private static final String KEY_COLOUR = "fromColour";
    
    private static Lock lock = new ReentrantLock();
    
    static ChatDB gInstance = null;
    
    Context mContext;
    
    public class ChatMessage
    {
    	public int fromUserId;
    	public int toUserId;
    	public int toGameId;
    	public String message;
    	public boolean bRead;
    	public int time;
    	public int id;
    	public int fromColour;
    	public String fromLogo;
    	
    	// not stored in teh db
    	public String fromAlias = "";
    	public String toAlias = "";
    }
    
	public ChatDB(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}
	
	public static ChatDB getInstance(Context context)
	{
		if (gInstance == null)
		{
			gInstance = new ChatDB(context);
		}
		return gInstance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_USERINFO_TABLE = "CREATE TABLE " + TABLE_CHAT + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FROMUSERID + " INTEGER,"
				+ KEY_TOUSERID + " INTEGER ,"
                + KEY_TOGAMEID + " INTEGER, "
                + KEY_TIME + " INTEGER, "
                + KEY_READ + " INTEGER, "
                + KEY_COLOUR + " INTEGER, "
                + KEY_LOGO + " VARCHAR(64), "
                + KEY_MESSAGE + " VARCHAR(255) )";
        db.execSQL(CREATE_USERINFO_TABLE);
        
      
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	}
	
	public ArrayList<ChatMessage> GetUnreadMessagesForGameID(int GameId, int minTime)
	{
		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_CHAT, new String[] {KEY_MESSAGE, KEY_TIME, KEY_READ, KEY_FROMUSERID, KEY_TOUSERID, KEY_TOGAMEID, KEY_ID, KEY_COLOUR, KEY_LOGO}, KEY_TOGAMEID + " = ? AND " + KEY_TIME + " > ? AND " + KEY_READ + " = 0", new String[]{String.valueOf(GameId), String.valueOf(minTime)}, null, null, null);
			
			while(cursor.moveToNext())
	        {
				ChatMessage message = new ChatMessage();
				message.message = cursor.getString(0);
				message.time = cursor.getInt(1);
				int read = cursor.getInt(2);
				if (read == 0) message.bRead = false;
				if (read == 1) message.bRead = true;
				message.fromUserId = cursor.getInt(3);
				message.toUserId = cursor.getInt(4);
				message.toGameId = cursor.getInt(5);
				message.id = cursor.getInt(6);
				message.fromColour = cursor.getInt(7);
				message.fromLogo = cursor.getString(8);
				
				chatMessages.add(message);
	        }
			
			return chatMessages;
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		
		return chatMessages;
	}
	
	public ArrayList<ChatMessage> GetMessagesForGameID(int GameId, int minTime)
	{
		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_CHAT, new String[] {KEY_MESSAGE, KEY_TIME, KEY_READ, KEY_FROMUSERID, KEY_TOUSERID, KEY_TOGAMEID, KEY_ID, KEY_COLOUR, KEY_LOGO}, KEY_TOGAMEID + " = ? AND " + KEY_TIME + " > ?", new String[]{String.valueOf(GameId), String.valueOf(minTime)}, null, null, null);
			
			while(cursor.moveToNext())
	        {
				ChatMessage message = new ChatMessage();
				message.message = cursor.getString(0);
				message.time = cursor.getInt(1);
				int read = cursor.getInt(2);
				if (read == 0) message.bRead = false;
				if (read == 1) message.bRead = true;
				message.fromUserId = cursor.getInt(3);
				message.toUserId = cursor.getInt(4);
				message.toGameId = cursor.getInt(5);
				message.id = cursor.getInt(6);
				message.fromColour = cursor.getInt(7);
				message.fromLogo = cursor.getString(8);
				
				chatMessages.add(message);
	        }
			
			return chatMessages;
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		return chatMessages;
	}
	
	public ArrayList<ChatMessage> GetMessagesForUserID(int fromUserId, int toUserId, int minTime)
	{
		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_CHAT, new String[] {KEY_MESSAGE, KEY_TIME, KEY_READ, KEY_FROMUSERID, KEY_TOUSERID, KEY_TOGAMEID, KEY_ID, KEY_COLOUR, KEY_LOGO}, KEY_FROMUSERID + " = ? AND " + KEY_TOUSERID + " = ? AND " + KEY_TIME + " > ?", new String[]{String.valueOf(fromUserId), String.valueOf(toUserId), String.valueOf(minTime)}, null, null, null);
			
			while(cursor.moveToNext())
	        {
				ChatMessage message = new ChatMessage();
				message.message = cursor.getString(0);
				message.time = cursor.getInt(1);
				int read = cursor.getInt(2);
				if (read == 0) message.bRead = false;
				if (read == 1) message.bRead = true;
				message.fromUserId = cursor.getInt(3);
				message.toUserId = cursor.getInt(4);
				message.toGameId = cursor.getInt(5);
				message.id = cursor.getInt(6);
				message.fromColour = cursor.getInt(7);
				message.fromLogo = cursor.getString(8);
				
				chatMessages.add(message);
	        }
			
			return chatMessages;
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		
		}
		return chatMessages;
	}
	
	public void MarkChatRead(int chatId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_READ, 1);
			
			db.update(TABLE_CHAT, values, KEY_ID + " = ?", new String[] {String.valueOf(chatId)});
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
	}
	
	
	public long AddChatMessage(int fromUserId, int toUserId, int toGameId, int fromColour, String fromLogo, String message)
	{
		long rowId = -1;
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
			ContentValues values = new ContentValues();
			values.put(KEY_TOUSERID, toUserId);
			values.put(KEY_FROMUSERID, fromUserId);
			values.put(KEY_TOGAMEID, toGameId);
			values.put(KEY_COLOUR, fromColour);
			values.put(KEY_LOGO, fromLogo);
			
			if (message.length() > 255) message = message.substring(0, 255);
			values.put(KEY_MESSAGE, message);
			
			int time = (int)(System.currentTimeMillis() / 1000);
			values.put(KEY_TIME, time);
			values.put(KEY_READ, 0);
			
			rowId = db.insert(TABLE_CHAT, null, values);
			
		}
		catch (Exception e)
		{

		} finally
		{
			db.close();
			lock.unlock();
		}
		
		return rowId;
	}

	
}


