package uk.co.eidolon.shared.views;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IState;
import Core.IState.GameStatus;
import Network.SyncList;
import Network.UserInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FriendListView extends ListView
{
	public FriendListAdapter mAdapter;
	
	public long mUserID = -1;
	
	String mList = "";
	
	Context mContext;
	public FriendListView(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		
		mContext = c.getApplicationContext();
		
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		mUserID = appWrapper.GetUserId();
		
		mAdapter = new FriendListAdapter(mContext, 0, new ArrayList<Integer>());
        setAdapter(mAdapter);
	}
	
	public void SetDetails(String list, boolean bSearchable)
	{
		mList = list;
	}
	
	
	public void Refresh()
	{
		Log.i("Tact2", "Refreshing friends list [" + mList + "]");
		final IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		mUserID = appWrapper.GetUserId();
		final SyncListDB syncListDB = SyncListDB.getInstance(mContext);

		// if there are any items in the adapter which aren't in the friends list, remove them
		ArrayList<Integer> friends = syncListDB.GetList(mList, (int)mUserID).GetList();
		ArrayList<Integer> removeList = new ArrayList<Integer>();
		for (Integer listedFriend : mAdapter.mItems)
		{
			if (friends.contains(listedFriend) == false)
			{
				removeList.add(listedFriend);
			}
		}
		
		boolean bChanged = false;
		if (removeList.size() > 0)
		{
			mAdapter.mItems.removeAll(removeList);
			bChanged = true;
			
		}
		
		if (bChanged == true)
		{
			mAdapter.notifyDataSetChanged();
			//mAdapter.
		}
		
		syncListDB.SyncWithServer(mList, (int)appWrapper.GetUserId(), syncListDB.new ISyncEvents()
		{
			@Override
			public void onListUpdated()
			{
				new UpdateFriendsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});
		
		new UpdateFriendsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		
	}
	
	public class FriendListAdapter extends ArrayAdapter<Integer>
	{
		private ArrayList<Integer> mItems;
	    
	    Context mContext;

	    public FriendListAdapter(Context context, int textViewResourceId, ArrayList<Integer> items)
	    {
	            super(context, textViewResourceId, items);
	            this.mItems = items;
	            mContext = context;
	    }
	    
	    void PopulateView(int userId, Context context, View view)
	    {
	    	if (context == null) return;
	    	
	    	UserInfo info = UserProfileDB.getInstance(context).GetUserInfo(userId);
	    	if (info != null)
	    	{
	    		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
	    		ImageView logo = (ImageView)view.findViewById(R.id.logo);
	    		TextView alias = (TextView)view.findViewById(R.id.alias);
	    		alias.setText(info.alias);
	    		logo.setBackgroundColor(info.colour);
	    		Drawable logoImage = appWrapper.GetLogoStore().GetLogo(info.logo);
	    		if (logoImage != null)
	    		{
	    			logo.setImageDrawable(logoImage);
	    		}
	    	}
	    }
	    
	    
	    View InflateView(Context context)
	    {
	    	if (context == null) return null;
	    	
	    	LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = vi.inflate(R.layout.friend_item, null);
	        return view;
	    }
	    
	    void UpdateItem(Integer position)
	    {
	    	int o = mItems.get(position );
	    	View v = getChildAt(position - getFirstVisiblePosition());
	    	
	    	PopulateView(o, mContext, v);
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	View view = convertView;
	    	int o = mItems.get(position);
	    	if (view == null)
	    	{
	    		view = InflateView(mContext);
	    		
	    	}
	    	PopulateView(o, mContext, view);
	    	
	        return view;
	    }
	}
	
	// Retrieves the state and generates thumbnails for the items in the list
		private class UpdateFriendsTask extends AsyncTask<String, Integer, Integer>
		{
			UpdateFriendsTask()
			{
				super();
			}
					
			@Override
			protected void onPreExecute()
			{
				
			}
			
			@Override
		    protected void onProgressUpdate(Integer... v)
			{
		        super.onProgressUpdate(v);
		        
		        if (mAdapter != null && v[0] > 0) 
		        {
		        	if (mAdapter.mItems.contains(v[0]) == false)
		        	{
		        		mAdapter.addAll(v[0]);
		        	} 
		        }
		    }
			
			@Override
			protected Integer doInBackground(String... string)
			{
				Log.i("Tact", "Running Friend View Update...");
				
				SyncList friendSyncList = SyncListDB.getInstance(mContext).GetList(mList, (int)mUserID);
				
				ArrayList<Integer> friendList = new ArrayList<Integer>();
				friendList.addAll(friendSyncList.GetList());
				Set<Integer> noDups = new TreeSet<Integer>();
				noDups.addAll(friendList);
				friendList.clear();
				friendList.addAll(noDups);
				
				
				// step through the list items and ensure we have up-to-date
				for (int i = 0; i < friendList.size(); ++i)
				{
					if (mAdapter.mItems.contains(friendList.get(i)) == false)
					{
						int userId = friendList.get(i);
						
						// we'll do a refresh when viewing the individual profile, just use the cached value most of the time
						if (UserProfileDB.getInstance(mContext).GetProfile(userId) == null ||
							UserProfileDB.getInstance(mContext).GetUserInfo(userId) == null)
						{
							if (userId > 0)
							{
								UserProfileDB.getInstance(mContext).GetFromServer(mContext, userId, null);
								try
								{
									Thread.sleep(300);
								} catch (InterruptedException e)
								{
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						
						publishProgress(userId);
						
						
					}
				}
				
				return 0;
			}
			
			
		    
		   
		}

	
}
