package uk.co.eidolon.shared.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IState;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class GameDatabase extends SQLiteOpenHelper
{
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "games";
 
    // Contacts table name
    private static final String TABLE_GAME_CACHE = "game_cache";
 
    // Contacts Table Columns names
    private static final String KEY_GAMEID = "gameId";
    private static final String KEY_SEQUENCE = "sequence";
    private static final String KEY_DATA = "data";
    private static final String KEY_ACCOUNT = "userId";
    
    private static Lock lock = new ReentrantLock();
    
    static GameDatabase gInstance = null;
    
    static Context mContext;
    
	public GameDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
		mContext = context;

	}
	
	public static GameDatabase getInstance(Context context)
	{
		if (gInstance == null) gInstance = new GameDatabase(context);
		return gInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_GAMES_TABLE = "CREATE TABLE " + TABLE_GAME_CACHE + "("
                + KEY_GAMEID + " INTEGER," + KEY_DATA + " BLOB,"
                + KEY_ACCOUNT + " INTEGER,"
                + KEY_SEQUENCE + " INTEGER" + ")";
        db.execSQL(CREATE_GAMES_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	}
	
	public boolean AddGame(int GameId, long UserId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
			ContentValues values = new ContentValues();
			values.put(KEY_GAMEID, GameId);
			
			values.put(KEY_ACCOUNT, UserId);
			
			db.insert(TABLE_GAME_CACHE, null, values);
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		return true;
	}
	
	public int GetSequence(int GameId, long UserId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
			Cursor cursor = db.query(TABLE_GAME_CACHE, new String[] {KEY_SEQUENCE}, KEY_GAMEID + " = ? AND " + KEY_ACCOUNT + " = ?", new String[]{String.valueOf(GameId), String.valueOf(UserId)}, null, null, null);
			int sequence = -1;
			while(cursor.moveToNext())
			{
				sequence = cursor.getInt(0);
			}
			return sequence;
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		return -1;
	}
	
	public void SetSequence(int GameId, long UserId, int sequenceId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_SEQUENCE, sequenceId);
			
			if (db.update(TABLE_GAME_CACHE, values, KEY_GAMEID + " = ? AND " + KEY_ACCOUNT + " = ?", new String[]{String.valueOf(GameId), String.valueOf(UserId)}) == 0)
			{
				// ignore if there's an error
			} else
			{
			// success
			}
			
		} catch (Exception e)
		{
		} finally
		{
			db.close();
			lock.unlock();
		}
	}
	
	public boolean UpdateGame(int GameId, long UserId, IState state)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(KEY_SEQUENCE, state.GetSequence());
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(byteStream);
			state.StateToBinary(stream);
			values.put(KEY_DATA, byteStream.toByteArray());
			
			if (db.update(TABLE_GAME_CACHE, values, KEY_GAMEID + " = ? AND " + KEY_ACCOUNT + " = ?", new String[]{String.valueOf(GameId), String.valueOf(UserId)}) == 0)
			{
				// not updated, so insert instead
				values.put(KEY_GAMEID, GameId);
				values.put(KEY_ACCOUNT, UserId);
				db.insert(TABLE_GAME_CACHE, null, values);
			} else
			{
			// success
			}
			
			return true;
		} catch (Exception e)
		{
		} finally
		{
			db.close();
			lock.unlock();
		}
		return false;
	}
	
	public IState GetGame(int GameId, long UserId)
	{
		SQLiteDatabase db = null;
		boolean bRepair = false;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
			Cursor cursor = db.query(TABLE_GAME_CACHE, new String[] {KEY_DATA}, KEY_GAMEID + " = ? AND " + KEY_ACCOUNT + " = ?", new String[]{String.valueOf(GameId), String.valueOf(UserId)}, null, null, null);
			
			IState state = null;
			
			IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
			
			while(cursor.moveToNext())
			{
				if (cursor.getBlob(0) == null) continue;
				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(0));
				DataInputStream stream = new DataInputStream(byteStream);
				
				state = appWrapper.StateFactory();
				
				state.BinaryToState(stream);
				
				return state;
			}
		} catch (EOFException e)
		{
			// db copy is damaged - mark it for a refresh from server
			bRepair = true;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally
		{
			db.close();
			lock.unlock();
		}
			
		if (bRepair == true)
		{
			// set the sequence num to 0 so that this game will get repaired next time round
			SetSequence(GameId, UserId, 0);
		}
			
		return null;
	}
	
}
