package uk.co.eidolon.shared.views;

import java.util.ArrayList;

import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.ChatDB.ChatMessage;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IState;
import Core.IState.GameStatus;
import Network.SyncList;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;



public class GameListView extends ListView
{
	public GameListAdapter mAdapter;
	
	public long mUserID = -1;
	
	Context mContext;
	public GameListView(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		
		mContext = c.getApplicationContext();
		
		ArrayList<IStateInfo> infoList = new ArrayList<IStateInfo>(); 
		
        mAdapter = new GameListAdapter(mContext, 0, infoList);
        
        setAdapter(mAdapter);
        
        IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
        mUserID = appWrapper.GetUserId();
        
	}
	
	public IStateInfo findItem(int GameId)
    {
    	for (IStateInfo item : mAdapter.mItems)
    	{
    		if (item.GetGameID() == GameId) return item;
    	}
    	return null;
    }
	
	void AddState(int GameID)
	{
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		
		IStateInfo holder = appWrapper.StateInfoFactory(GameID, mUserID);
		mAdapter.insert(holder, 0);
		
		new UpdateStatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	void RemoveState(int GameID)
	{
		for (int i = 0; i < mAdapter.getCount(); ++i)
		{
			IStateInfo infoHolder = mAdapter.getItem(i);
			if (infoHolder.GetGameID() == GameID)
			{
				infoHolder.Dispose();
				mAdapter.remove(infoHolder);
			}
		}
	}
	
	public void RefreshState(int GameID)
	{
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		boolean found = false;
		for (IStateInfo info : mAdapter.mItems)
		{
			if (info.GetGameID() == GameID)
			{
				found = true;
				IState state = GameDatabase.getInstance(mContext).GetGame(GameID, mUserID);

				final int position = mAdapter.getPosition(info);
				info.PopulateStateInfo(state);
				mAdapter.UpdateItem(position);
				mAdapter.notifyDataSetChanged();

			}
		}
		
		if (found == false)
		{
			IStateInfo stateInfo = appWrapper.StateInfoFactory(GameID, mUserID);
			
			IState state = GameDatabase.getInstance(mContext).GetGame(GameID, mUserID);
			if (state != null)
			{
				stateInfo.PopulateStateInfo(state);
				mAdapter.add(stateInfo);
				Log.i("Tact2", "Adding state: " + stateInfo.GetGameID());
				mAdapter.notifyDataSetChanged();
			} 
		}
	}
	
	
	public void UpdateStateList()
	{
		
		//ArrayList<Integer> fakeAdd = new ArrayList<Integer>();
		//fakeAdd.add(5219);
		//SyncListDB.getInstance(mContext).AddToSyncList(fakeAdd, "GameList", (int)mUserID);
		
		
		SyncList gamesDBList = SyncListDB.getInstance(mContext).GetList("GameList", (int)mUserID);
		ArrayList<Integer> gamesActivityList = new ArrayList<Integer>();
		
		boolean bChanged = false;
		
		for (int item = 0; item < mAdapter.getCount(); ++item)
		{
			if (mAdapter.getItem(item).GetUserID() != mUserID)
			{
				bChanged = true;
				IStateInfo info = mAdapter.getItem(item);
				mAdapter.remove(info);
				item = 0;
			}
		}
		
		for (int i = 0; i < mAdapter.getCount(); ++i)
		{
			gamesActivityList.add(mAdapter.getItem(i).GetGameID());
		}
		
		ArrayList<Integer> addedGames = new ArrayList<Integer>();
		ArrayList<Integer> deletedGames = new ArrayList<Integer>();
		
		for (int GameID : gamesDBList.GetList())
		{
			if (!gamesActivityList.contains(GameID)) addedGames.add(GameID); 
		}
		
		for (int GameID: gamesActivityList)
		{
			if (!gamesDBList.GetList().contains(GameID)) deletedGames.add(GameID);
		}
		
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		
		for (int GameID: addedGames)
		{
			bChanged = true;
			mAdapter.insert(appWrapper.StateInfoFactory(GameID, mUserID), 0);
		}
		for (int GameID : deletedGames)
		{
			for (int item = 0; item < mAdapter.getCount(); ++item)
			{
				if (mAdapter.getItem(item).GetGameID() == GameID)
				{
					bChanged = true;
					IStateInfo info = mAdapter.getItem(item);
					mAdapter.remove(info);
					item = 0;
				}
			}
		}
		
		if (bChanged == true)
		{
			mAdapter.notifyDataSetChanged();
			new UpdateStatesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
	
	
	public class GameListAdapter extends ArrayAdapter<IStateInfo>
	{
		
	    private ArrayList<IStateInfo> mItems;
	    
	    Context mContext;

	    public GameListAdapter(Context context, int textViewResourceId, ArrayList<IStateInfo> items)
	    {
	            super(context, textViewResourceId, items);
	            this.mItems = items;
	            mContext = context;
	    }
	    
	    void UpdateItem(Integer position)
	    {
	    	IStateInfo o = mItems.get(position );
	    	View v = getChildAt(position - getFirstVisiblePosition());
	    	
	    	o.PopulateView(mContext, v);
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	View view = convertView;
	    	IStateInfo o = mItems.get(position);
	    	if (view == null)
	    	{
	    		view = o.InflateView(mContext);
	    	}
	    	o.PopulateView(mContext, view);
	    	
	    	if (o.GetState() == null)
	    	{
	    		view = o.InflateView(mContext);
	    	}
	    	
	        
	        return view;
	    }
	}
	
	protected void onListItemLongClick(int position)
	{
		// launch the context menu...
	}
	
	protected void onListItemClick(int position)
	{
		
	}

	// Retrieves the state and generates thumbnails for the items in the list
	private class UpdateStatesTask extends AsyncTask<String, Integer, Integer>
	{
		UpdateStatesTask()
		{
			super();
		}
				
		@Override
		protected void onPreExecute()
		{
			
		}
		
		@Override
		protected void onPostExecute(Integer code)
		{
			
		}
		
		@Override
	    protected void onProgressUpdate(Integer... v)
		{
	        super.onProgressUpdate(v);
	        
	        if (mAdapter.getCount() > v[0])
	        {
	        	mAdapter.UpdateItem(v[0]);
	        } else
	        {
	        	Log.i("Tact2", "Not enough adapter :(");
	        }

	    }
		
		@Override
		protected Integer doInBackground(String... string)
		{
			Log.i("Tact", "Running State View Update...");
			// step through the list items and ensure we have up-to-date
			for (int i = 0; i < mAdapter.getCount(); ++i)
			{
				IStateInfo infoHolder = mAdapter.getItem(i);
				
				try
				{
					IState state = GameDatabase.getInstance(GameListView.this.mContext).GetGame(infoHolder.GetGameID(), mUserID);
					if (state == null && infoHolder.GetGameID() >= 0)
					{
						Log.i("Tact2", "No state for " + infoHolder.GetGameID() + ", fetching...");
						IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
						// we need to get the server update for this game
						Intent serviceIntent = new Intent();
						serviceIntent.setPackage("uk.co.eidolon.tact2)");
				        serviceIntent.setAction(appWrapper.GetServerSyncIntentAction());
				        serviceIntent.putExtra("updateStateId", infoHolder.GetGameID());
				        mContext.startService(serviceIntent);
					}
					
					// if we couldn't get a local game, it must have been deleted...
					if (state == null && infoHolder.GetGameID() < 0)
					{
						int GameID = infoHolder.GetGameID();
						ArrayList<Integer> removeList = new ArrayList<Integer>();
						removeList.add(GameID);
						SyncListDB.getInstance(mContext).RemoveFromSyncList(removeList, "GameList", (int)mUserID);
					}
					
					if (state != null)
					{
						// are there any unread chat messages for this game?
						infoHolder.PopulateStateInfo(state);
						publishProgress(i);
					}
				}
				catch(Exception e)
				{
					try
					{
						Thread.sleep(1000);
					} catch (InterruptedException e1)
					{
					}
				}
			}
			
			return 0;
		}
		
		
	    
	   
	}
}
