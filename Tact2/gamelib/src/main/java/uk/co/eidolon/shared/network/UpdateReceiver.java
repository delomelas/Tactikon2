package uk.co.eidolon.shared.network;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;

import Core.IState;
import Network.PackageResponse;
import Network.Packages.PackageGetGame;
import Network.SyncList;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.utils.IAppWrapper;


public class UpdateReceiver extends IntentService
{

	String mUpdateStateIntent;
	
	public UpdateReceiver()
	{
		super("UpdateReceiver");
		
		
	}
	
	long mUserID = -1;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		IAppWrapper appWrapper = (IAppWrapper)this.getApplicationContext();
        mUpdateStateIntent = appWrapper.GetStateUpdatedIntentAction();
	}

	
	@Override
	public void onHandleIntent(Intent intent)
	{
		
		IAppWrapper appWrapper = (IAppWrapper)this.getApplicationContext();
		
		mUserID = appWrapper.GetUserId();
		
		Log.i("Tact2", "Intent received: " + intent.getAction());
		
		final Bundle bundle = intent.getExtras();
		if (bundle != null)
		{
			if (bundle.containsKey("blockUserId"))
			{
				int userId = bundle.getInt("blockUserId");
				int GameID = bundle.getInt("GameID");
				
				SyncListDB syncListDB = SyncListDB.getInstance(this);
	    		ArrayList<Integer> userToBlock = new ArrayList<Integer>();
	    		userToBlock.add(userId);
	    		syncListDB.AddToSyncList(userToBlock, "BlockList", (int)appWrapper.GetUserId());
	    		syncListDB.SyncWithServer("BlockList", (int)appWrapper.GetUserId(), null);
	    		new NotificationUtils().ClearNotification(this, GameID);
	    		//Toast.makeText(this.getApplicationContext(), "User blocked", Toast.LENGTH_SHORT).show();
			}
			if (bundle.containsKey("updateAll") && bundle.getBoolean("updateAll") == true)
			{
				appWrapper.GetUploadQueue().FlushQueue();
				UpdateAllGamesFromServer(mUserID);
				appWrapper.GetUploadQueue().FlushQueue();
			}
			
			
			if (bundle.containsKey("updateStateId"))
			{
				GetLatestFromServer(bundle.getInt("updateStateId"), mUserID);
			}
			
			if (bundle.containsKey("forceUpdateStateId"))
			{
				Log.i("GameLib", "Forcing update to repair broken state...");
				ForceGetLatestFromServer(bundle.getInt("forceUpdateStateId"), mUserID);
			}
			
			if (bundle.containsKey("updateAllIfZero"))
			{
				appWrapper.GetUploadQueue().FlushQueue();
				UpdateAllGamesIfZero(mUserID);
			}
		}
		
		
	}
	
	
	
	private void NotifyStateUpdated(int GameID, long UserId)
	{
		/*
		final Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(mUpdateStateIntent);
		broadcastIntent.putExtra("GameID", GameID);
		this.sendBroadcast(broadcastIntent);
		*/
		
		Intent intent = new Intent(mUpdateStateIntent);
		intent.putExtra("GameID", GameID);
		intent.putExtra("UserID", UserId);
		Log.i("Tact2", "UpdateReceiver refreshing GameID" + GameID);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	protected void ForceGetLatestFromServer(final int GameId, final long UserId)
	{
		final PackageGetGame p = new PackageGetGame();
		p.gameId = GameId;
		
		// use sequenceId 0 so that this game will definately be updated
		p.currentSequenceId = 0;
		
		if (GameId < 0) return; // do nothing for local games
		
		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.state != null)
					{
						NotifyStateUpdated(GameId, UserId);
						new NotificationUtils().DoNotification(UpdateReceiver.this, oldState, p.state, GameId);
					}
				} 
			}

			IState oldState = null;
			
			@Override
			public void postExecuteBackground()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.state != null)
					{
						oldState = GameDatabase.getInstance(UpdateReceiver.this).GetGame(GameId, UserId);
						GameDatabase.getInstance(UpdateReceiver.this).UpdateGame(GameId, UserId, p.state);

						if (oldState != null && oldState.GetSequence() != p.state.GetSequence())
						{
							IAppWrapper appWrapper = (IAppWrapper)UpdateReceiver.this.getApplicationContext();
							appWrapper.GetUploadQueue().RemoveGameIdFromQueue(GameId);
						}
					}
				} else  
				{
					// schedule another update for x time in the future
					GameDatabase.getInstance(UpdateReceiver.this).SetSequence(GameId, UserId, 0);
				}
			}
		});
		
		sender.Send();
	}
	
	protected void GetLatestFromServer(final int GameId, final long UserId)
	{
		final PackageGetGame p = new PackageGetGame();
		p.gameId = GameId;
		p.currentSequenceId = GameDatabase.getInstance(this).GetSequence(GameId, UserId);
		
		
		if (GameId < 0) return; // do nothing for local games
		
		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.state != null)
					{
						NotifyStateUpdated(GameId, UserId);
						new NotificationUtils().DoNotification(UpdateReceiver.this, oldState, p.state, GameId);

						// remove if it's already on the queue to be updated
						UpdateQueue queue = new UpdateQueue(UpdateReceiver.this,"GetGameUpdate", UserId);
						queue.RemoveGameIDFromQueue(GameId);
					}
				} 
			}

			IState oldState = null;
			
			@Override
			public void postExecuteBackground()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.state != null)
					{
						oldState = GameDatabase.getInstance(UpdateReceiver.this).GetGame(GameId, UserId);
						GameDatabase.getInstance(UpdateReceiver.this).UpdateGame(GameId, UserId, p.state);

						if (oldState != null && oldState.GetSequence() != p.state.GetSequence())
						{
							IAppWrapper appWrapper = (IAppWrapper)UpdateReceiver.this.getApplicationContext();
							appWrapper.GetUploadQueue().RemoveGameIdFromQueue(GameId);
						}
					}
				} else  
				{
					// schedule another update for x time in the future
					GameDatabase.getInstance(UpdateReceiver.this).SetSequence(GameId, UserId, 0);
				}
			}
		});
		
		sender.Send();
	}
	
	private void UpdateAllGamesFromServer(long UserID)
	{
		SyncList gameIDList = SyncListDB.getInstance(this).GetList("GameList", (int)mUserID);
		
		for (Integer i : gameIDList.GetList())
		{
			GetLatestFromServer(i, UserID);
			try
			{
				Thread.sleep(200);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void UpdateAllGamesIfZero(long UserID)
	{
		SyncList gameIDList = SyncListDB.getInstance(this).GetList("GameList", (int)mUserID);
		
		for (Integer i : gameIDList.GetList())
		{
			int sequence = GameDatabase.getInstance(UpdateReceiver.this).GetSequence(i, UserID);
			if (sequence == 0)
			{
				Log.i("GameLib", "Forcing update of game " + i + " to update state.");
				GetLatestFromServer(i, UserID);		
			}
		}
	}
		
	

	
}
