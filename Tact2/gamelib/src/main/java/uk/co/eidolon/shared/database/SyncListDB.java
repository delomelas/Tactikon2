package uk.co.eidolon.shared.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IEvent;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.SyncList;
import Network.Packages.PackageEventList;
import Network.Packages.PackageSyncList;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;

public class SyncListDB extends SQLiteOpenHelper
{
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "synclist";
 
    // Contacts table name
    private static final String TABLE_LISTS = "synclists";
 
    // Contacts Table Columns names
    private static final String KEY_TAG = "tag";
    private static final String KEY_APP = "app";
    private static final String KEY_DATA = "data";
    private static final String KEY_USERID = "userId";
    
    private static Lock lock = new ReentrantLock();
    
    static SyncListDB gInstance = null;
    
    private Context mContext;
    
    private String mAppName; 
    
	public SyncListDB(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
		
		mAppName = "GameLib";
	}
	
	public static SyncListDB getInstance(Context context)
	{
		if (gInstance == null)
		{
			gInstance = new SyncListDB(context);
		}
		return gInstance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_SYNCLIST_TABLE = "CREATE TABLE " + TABLE_LISTS + "("
                + KEY_TAG + " VARCHAR(255)," + KEY_USERID + " INTEGER," + KEY_APP + " VARCHAR(255) ,"
                + KEY_DATA + " BLOB" + ")";
        db.execSQL(CREATE_SYNCLIST_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	}
	
	public void AddToSyncList(ArrayList<Integer> items, String tag, int UserID)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
		// 	need to fetch the existing synclist for that tag (or make a new one)
			
			Cursor cursor = db.query(TABLE_LISTS, new String[] {KEY_DATA}, KEY_TAG + " = ? AND " + KEY_APP + " = ? AND " + KEY_USERID + " = ?", new String[]{tag, mAppName, String.valueOf(UserID)}, null, null, null);
			
			ArrayList<Integer> list = new ArrayList<Integer>();
			SyncList dbList = new SyncList(tag);
			
			while(cursor.moveToNext())
	        {
				if (cursor.getBlob(0) == null) continue;
				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(0));
				DataInputStream stream = new DataInputStream(byteStream);
				
				dbList.BinaryToList(stream);
	        }
			
			// 	then add to the list
			dbList.AddToList(items);
			
			// if the item is already pending to be removed, remove from the pending removal list
			dbList.removeList.removeAll(items);
			
			ContentValues values = new ContentValues();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(byteStream);
			dbList.ListToBinary(stream);
			values.put(KEY_DATA, byteStream.toByteArray());
			
			if (db.update(TABLE_LISTS, values, KEY_APP + " = ? AND " + KEY_TAG + " = ? AND " + KEY_USERID + " = ?", new String[]{mAppName, tag, String.valueOf(UserID)}) == 0)
			{
				values.put(KEY_APP, mAppName);
				values.put(KEY_TAG, tag);
				values.put(KEY_USERID, UserID);
				db.insert(TABLE_LISTS,null, values);
			} else
			{
				// success
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
	public boolean CollapseList(ArrayList<Integer> list, final String tag, final int UserId)
	{
		boolean bChanged = false;
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
		// 	fetch the existing synclist for that tag (or make a new one)
			
			Cursor cursor = db.query(TABLE_LISTS, new String[] {KEY_DATA}, KEY_TAG + " = ? AND " + KEY_APP + " = ? AND " + KEY_USERID + " = ?", new String[]{tag, mAppName, String.valueOf(UserId)}, null, null, null);
			
			SyncList dbList = new SyncList(tag);
			
			while(cursor.moveToNext())
	        {
				if (cursor.getBlob(0) == null) continue;
				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(0));
				DataInputStream stream = new DataInputStream(byteStream);
				
				dbList.BinaryToList(stream);
	        }
			
			// 	then add to the list
			dbList.list.removeAll(dbList.removeList);
			dbList.list.addAll(dbList.addList);
			
			if (dbList.list.size() != list.size() || !dbList.list.containsAll(list) || !list.containsAll(dbList.list))
			{
				bChanged = true;
			}
			
			dbList.list.removeAll(list);
			dbList.list.addAll(list);
			
			dbList.removeList.retainAll(list);
			dbList.addList.removeAll(list);
			
			ContentValues values = new ContentValues();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(byteStream);
			dbList.ListToBinary(stream);
			values.put(KEY_DATA, byteStream.toByteArray());
			
			if (db.update(TABLE_LISTS, values, KEY_APP + " = ? AND " + KEY_TAG + " = ? AND " + KEY_USERID + " = ?", new String[]{mAppName, tag, String.valueOf(UserId)}) == 0)
			{
				values.put(KEY_APP, mAppName);
				values.put(KEY_TAG, tag);
				values.put(KEY_USERID, UserId);
				db.insert(TABLE_LISTS,null, values);
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
		
		return bChanged;
	}
	
	public void RemoveFromSyncList(ArrayList<Integer> items, String tag, int UserID)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
		
		// 	fetch the existing synclist for that tag (or make a new one)
			
			Cursor cursor = db.query(TABLE_LISTS, new String[] {KEY_DATA}, KEY_TAG + " = ? AND " + KEY_APP + " = ? AND " + KEY_USERID + " = ?", new String[]{tag, mAppName, String.valueOf(UserID)}, null, null, null);
			
			SyncList dbList = new SyncList(tag);
			
			while(cursor.moveToNext())
	        {
				if (cursor.getBlob(0) == null) continue;
				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(0));
				DataInputStream stream = new DataInputStream(byteStream);
				
				dbList.BinaryToList(stream);
	        }

			// 	then add to the list
			dbList.RemoveFromList(items);
			
			ContentValues values = new ContentValues();
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(byteStream);
			dbList.ListToBinary(stream);
			values.put(KEY_DATA, byteStream.toByteArray());
			
			if (db.update(TABLE_LISTS, values, KEY_APP + " = ? AND " + KEY_TAG + " = ? AND " + KEY_USERID + " = ?", new String[]{mAppName, tag, String.valueOf(UserID)}) == 0)
			{
				values.put(KEY_APP, mAppName);
				values.put(KEY_TAG, tag);
				values.put(KEY_USERID, UserID);
				db.insert(TABLE_LISTS,null, values);
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
	
	
	public SyncList GetList(String tag, int UserID)
	{
		SQLiteDatabase db = null;
		try
		{
			lock.lock();
			
			db = getWritableDatabase();
			
			Cursor cursor = db.query(TABLE_LISTS, new String[] {KEY_DATA}, KEY_TAG + " = ? AND " + KEY_APP + " = ? AND " + KEY_USERID + " = ?", new String[]{tag, mAppName, String.valueOf(UserID)}, null, null, null);
			
			SyncList list = new SyncList(tag);
			
			while(cursor.moveToNext())
	        {
				if (cursor.getBlob(0) == null) continue;
				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(0));
				DataInputStream stream = new DataInputStream(byteStream);
				
				list.BinaryToList(stream);
				
	        }
			
			return list;
		} catch (Exception e)
		{
			
		} finally
		{
			db.close();
			lock.unlock();
		}
		

		return new SyncList(tag);
	}
	
	abstract public class ISyncEvents
	{
		abstract public void onListUpdated();
	}
	
	public void SyncWithServer(final String tag, final int userId, final ISyncEvents syncEvent)
	{
		// construct and send package to server
		// collapse the add and delete lists according to what the server says
		
		SyncList syncList = GetList(tag, userId);
		
		if (syncList.addList.isEmpty() && syncList.removeList.isEmpty() && !syncList.list.isEmpty()) 
		{
			if (syncEvent != null) syncEvent.onListUpdated();
			return;
		}
		
		final PackageSyncList syncPackage = new PackageSyncList();
		syncPackage.addList.addAll(syncList.addList);
		syncPackage.deleteList.addAll(syncList.removeList);
		syncPackage.syncListName = syncList.mName;
		
		PackageDelivery sender = new PackageDelivery(mContext, syncPackage, new ISendEvents()
		{
			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (syncPackage.mReturnCode == PackageResponse.Success)
				{
					CollapseList(syncPackage.syncList, tag, userId);
					if (syncEvent != null) syncEvent.onListUpdated();
				}
			}

			@Override
			public void postExecuteBackground()
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		sender.Send();
	}
}


