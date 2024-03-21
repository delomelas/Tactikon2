package uk.co.eidolon.shared.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Core.IEvent;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.Packages.PackageEventList;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class ServerUploadQueue extends SQLiteOpenHelper
{
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "queue";
 
    // Contacts table name
    private static final String TABLE_EVENTS = "events";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_GAME_ID = "game_id";
    private static final String KEY_EVENT = "event";
    private static final String KEY_EVENT_TYPE = "event_type";
    
    private static Lock lock = new ReentrantLock();
    
    static GameDatabase gInstance = null;
    
    private Context mContext;
    
    boolean networking = false;
    
	public ServerUploadQueue(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
		mContext = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_EVENTS_TABLE = "CREATE TABLE " + TABLE_EVENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_EVENT + " BLOB,"
                + KEY_GAME_ID + " INTEGER,"
                + KEY_EVENT_TYPE + " VARCHAR(255)" + ")";
        db.execSQL(CREATE_EVENTS_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	}

	public void RemoveGameIdFromQueue(int gameId)
	{
		SQLiteDatabase db = null;


		try
		{
			lock.lock();
			db = getWritableDatabase();

			db.delete(TABLE_EVENTS, KEY_GAME_ID + "=?", new String[]{Integer.toString(gameId)});

		} catch (Exception e)
		{

			return;
		} finally
		{
			db.close();
			lock.unlock();
		}
	}
	
	public void AddToQueue(EventQueueItem item)
	{
		SQLiteDatabase db = null;
		
		
		try
		{
			lock.lock();
			db = getWritableDatabase();
			
			ContentValues values = new ContentValues();
			values.put(KEY_GAME_ID, item.GameID);
			values.put(KEY_EVENT_TYPE, item.event.getClass().getName());
		
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream stream = new DataOutputStream(byteStream);
			item.event.EventToBinary(stream);
			
			values.put(KEY_EVENT, byteStream.toByteArray());
			db.insert(TABLE_EVENTS, null, values);
			
		} catch (Exception e)
		{
			
			return;
		} finally
		{
			db.close();
			lock.unlock();
		}
				
		
		
	}
	
	public void ClearQueue()
	{
		SQLiteDatabase db = null;
		
		
		try
		{
			lock.lock();
			db = getWritableDatabase();
			
		
			db.delete(TABLE_EVENTS, null, null);
			
		} finally
		{
			db.close();
			lock.unlock();
		}
	}

	public boolean DoFlushQueue()
	{
		Log.i("Tact2", "Attempting to flush event queue.");
		if (networking == true)
		{
			Log.i("Tact2", "Unable to flush event queue - network action already in progress.");
			return false;
		}

		networking = true;

		// do nothing if we're not connected to a network
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		if (appWrapper.IsNetworkConnected() == false)
		{
			networking = false;
			Log.i("Tact2", "Unable to flush event queue - no network connection.");
			return false;
		}

		final long UserId = appWrapper.GetUserId();

		final PackageEventList p = new PackageEventList();


		ArrayList<EventQueueItem> eventList = new ArrayList<EventQueueItem>();

		SQLiteDatabase db = null;

		try
		{
			lock.lock();
			db = getWritableDatabase();

			/// pull all the items from the queue
			Cursor cursor = db.query(TABLE_EVENTS, new String[] {KEY_GAME_ID, KEY_EVENT, KEY_EVENT_TYPE}, null, null, null, null, "ROWID");

			while(cursor.moveToNext())
			{
				EventQueueItem item = new EventQueueItem();
				item.GameID = cursor.getInt(0);

				String eventType = cursor.getString(2);

				IEvent event = null;
				event = (IEvent)Class.forName(eventType).newInstance();

				ByteArrayInputStream byteStream = new ByteArrayInputStream(cursor.getBlob(1));
				DataInputStream stream = new DataInputStream(byteStream);
				event.BinaryToEvent(stream);

				item.event = event;
				eventList.add(item);
			}
		} catch (Exception e)
		{
			networking = false;

		} finally
		{
			db.close();
			lock.unlock();
		}


		if (eventList.size() == 0)
		{
			Log.i("Tact2", "Nothing in queue to flush.");
			networking = false;
			return true;
		}


		if (eventList.size() > 0)
		{
			p.eventList = eventList;

			PackageDelivery sender = new PackageDelivery(mContext, p, new ISendEvents() {

				@Override
				public void preExecute()
				{

				}

				@Override
				public void postExecute()
				{

					networking = false;

					int count = 0;
					TreeSet<Integer> successSet  =new TreeSet<Integer>();
					TreeSet<Integer> failSet  =new TreeSet<Integer>();
					for (EventQueueItem item : p.eventList)
					{
						if (count < p.mSuccessCount)
						{
							successSet.add(item.GameID);
						}

						if (count >= p.mSuccessCount)
						{
							failSet.add(item.GameID);
						}
						count ++;
					}

					// now remove all items from the db
					if (p.mReturnCode == PackageResponse.Success)
					{
						Log.i("Tact2", "Event Queue Flushed.");

						Iterator<Integer> itr = successSet.iterator();
						while (itr.hasNext())
						{
							int stateId = itr.next();
							Log.i("Tact2", stateId + " flushed");
							RemoveGameIdFromQueue(stateId);
						}



					} else if (p.mReturnCode == PackageResponse.ErrorStateError)
					{
						Log.i("Tact2", "State error while flushing queue");
						// communication was successful, but we'll need to reset the state or it'll remain broken
						// let's work out which was the first state to fail

						Iterator<Integer> itr = failSet.iterator();
						while (itr.hasNext())
						{
							int stateId = itr.next();

							RemoveGameIdFromQueue(stateId);
							Log.i("Tact2", stateId + " cleared");

							GameDatabase.getInstance(mContext).SetSequence(stateId, UserId, 0);

							Intent serviceIntent = new Intent();
							serviceIntent.setPackage("uk.co.eidolon.tact2)");
							IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
							serviceIntent.setAction(appWrapper.GetServerSyncIntentAction());
							serviceIntent.putExtra("forceUpdateStateId", stateId);
							mContext.startService(serviceIntent);
						}
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


		return true;

	}


	
	public void FlushQueueEx()
	{
		int backoff = 1;

		while (DoFlushQueue() == false)
		{
			try {
				Thread.sleep(backoff * 1000);
			} catch (Exception e)
			{

			}

			backoff = backoff * 2;
		}


	}

	public void FlushQueue()
	{
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				// Insert some method call here.
				FlushQueueEx();
			}
		});
		t.start();
	}
}


