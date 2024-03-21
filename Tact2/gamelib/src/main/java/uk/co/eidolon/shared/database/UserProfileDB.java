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

public class UserProfileDB extends SQLiteOpenHelper
{
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "userprofiles";
 
    private static final String TABLE_USERINFO = "userinfo";
    private static final String TABLE_PROFILES = "profiles";
 
    // UserInfo Column names
    private static final String KEY_USERID = "userId";
    private static final String KEY_COLOUR = "colour";
    private static final String KEY_ALIAS = "alias";
    private static final String KEY_LOGO = "logo";
    
    // PROFILE column names
    private static final String KEY_PROFILETAG = "profileTag";
    private static final String KEY_PROFILEVALUE = "profileValue";
    
    
    
    private static Lock lock = new ReentrantLock();
    
    static UserProfileDB gInstance = null;
    
    private Context mContext;
    
	public UserProfileDB(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}
	
	public static UserProfileDB getInstance(Context context)
	{
		if (gInstance == null)
		{
			gInstance = new UserProfileDB(context);
		}
		return gInstance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_USERINFO_TABLE = "CREATE TABLE " + TABLE_USERINFO + "("
                + KEY_USERID + " INTEGER,"
				+ KEY_COLOUR + " INTEGER ,"
                + KEY_LOGO + " VARCHAR(255), "
                + KEY_ALIAS + " VARCHAR(255), PRIMARY KEY (" + KEY_USERID + ") ON CONFLICT REPLACE)";
        db.execSQL(CREATE_USERINFO_TABLE);
        
        String CREATE_PROFILES_TABLE = "CREATE TABLE " + TABLE_PROFILES + "("
                + KEY_USERID + " INTEGER,"
				+ KEY_PROFILETAG + " INTEGER,"
                + KEY_PROFILEVALUE + " INTEGER,"
                + " PRIMARY KEY (" + KEY_USERID + ", "+ KEY_PROFILETAG +") ON CONFLICT REPLACE)";
        db.execSQL(CREATE_PROFILES_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	}
	
	public void AddUserInfo(int UserId, String alias, int colour, String logo)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
		// 	need to fetch the existing synclist for that tag (or make a new one)
			
			ContentValues values = new ContentValues();
			values.put(KEY_ALIAS, alias);
			values.put(KEY_COLOUR, colour);
			values.put(KEY_LOGO, logo);
			values.put(KEY_USERID, UserId);
			
			long rowId = db.insert(TABLE_USERINFO, null, values);
			
		}
		catch (Exception e)
		{

		} finally
		{
			db.close();
			lock.unlock();
		}
	}
	
	// returns true if the list changes
	public UserInfo GetUserInfo(int userId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_USERINFO, new String[]{KEY_ALIAS, KEY_LOGO, KEY_COLOUR}, KEY_USERID + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
			
			UserInfo userInfo = new UserInfo();
			while(cursor.moveToNext())
	        {
				userInfo.alias = cursor.getString(0);
				userInfo.logo = cursor.getString(1);
				
				userInfo.userId = userId;
				userInfo.colour = cursor.getInt(2);
				
				return userInfo;
	        }
			
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		
		return null;
	}
	
	public void AddProfile(int UserId, TreeMap<Integer, Integer> profile)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
		// 	need to fetch the existing synclist for that tag (or make a new one)
			
			for (Entry<Integer, Integer> entry : profile.entrySet())
			{
				ContentValues values = new ContentValues();
				values.put(KEY_USERID, UserId);
				values.put(KEY_PROFILETAG, entry.getKey());
				values.put(KEY_PROFILEVALUE, entry.getValue());
			
				db.insert(TABLE_PROFILES, null, values);
			}
			
		}
		catch (Exception e)
		{

		} finally
		{
			db.close();
			lock.unlock();
		}
	}
	
	// returns true if the list changes
	public TreeMap<Integer, Integer> GetProfile(int userId)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_PROFILES, new String[]{KEY_PROFILETAG, KEY_PROFILEVALUE}, KEY_USERID + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
			
			TreeMap<Integer, Integer> profile = new TreeMap<Integer, Integer>();
			while(cursor.moveToNext())
	        {
				int tag =  cursor.getInt(0);
				int value =  cursor.getInt(1);
				
				profile.put(tag, value);
	        }
			if (profile.size() == 0) return null;
			return profile;
			
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		
		return null;
	}
	
	abstract public class IUpdateEvent
	{
		abstract public void onUpdated();
		abstract public void onUpdatedBackground();
	}
	
	public void GetFromServer(final Context context, final int UserId, final IUpdateEvent updateEvent)
	{
		final PackageGetProfile getProfilePackage = new PackageGetProfile();
		getProfilePackage.userId = UserId;
		if (UserId == -1 || UserId == 0) return;
		PackageDelivery sender = new PackageDelivery(mContext, getProfilePackage, new ISendEvents()
		{
			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (getProfilePackage.mReturnCode == PackageResponse.Success)
				{
					if (updateEvent != null)
					{
						updateEvent.onUpdated();
					}
				}
			}

			@Override
			public void postExecuteBackground()
			{
				if (getProfilePackage.mReturnCode == PackageResponse.Success)
				{
					UserProfileDB.getInstance(context).AddUserInfo(UserId, getProfilePackage.alias, getProfilePackage.colour, getProfilePackage.logo);
					UserProfileDB.getInstance(context).AddProfile(UserId, getProfilePackage.profile);
					if (updateEvent != null)
					{
						updateEvent.onUpdatedBackground();
					}
				}
			}
		});
		
		sender.Send();
	}
}


