package uk.co.eidolon.shared.activities;

import java.util.ArrayList;
import java.util.Random;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.ServerUploadQueue;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.database.UserProfileDB.IUpdateEvent;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.FriendListView;
import uk.co.eidolon.shared.views.FriendListView.FriendListAdapter;

import Core.IEvent;
import Core.IState;
import Core.IState.GameStatus;
import Core.PlayerInfo;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.SyncList;
import Network.UserInfo;
import Network.Packages.PackageLogin;
import Network.Packages.PackageSearchAlias;
import Network.Packages.PackageUpdateGCM;
import Support.Preferences;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FriendListFragment extends Fragment
{
	FriendListView mFreindListView;
	
	int mSearchId = -1;
	
	boolean bSearchable = false;
	String mList = "";
	
	public static FriendListFragment getInstance(String list, boolean bSearchable)
	{
		FriendListFragment newFragment = new FriendListFragment();
		Bundle bundle = new Bundle();
		bundle.putBoolean("canSearch", bSearchable);
		bundle.putString("syncList", list);
		newFragment.setArguments(bundle);
		
		return newFragment;
        
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.friend_list_layout, container, false);
        
        mFreindListView = (FriendListView)view.findViewById(R.id.friendslist);
        
        mFreindListView.setClickable(true);
        mFreindListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
			public void onItemClick(AdapterView<?> view, View parentView, int position, long id)
			{
				int userId = mFreindListView.mAdapter.getItem(position);
				Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
				profileIntent.putExtra("UserID", userId);
		    	startActivity(profileIntent);
			}
        });
        
        if (savedInstanceState != null)
        {
        	bSearchable = savedInstanceState.getBoolean("canSearch", false);
        	mList = savedInstanceState.getString("syncList", "");
        }
        if (getArguments() != null)
        {
        	if (getArguments().containsKey("syncList"))
        	{
        		mList = getArguments().getString("syncList","");
        		bSearchable = getArguments().getBoolean("canSearch", false);
        	}
        }
        
        mFreindListView.SetDetails(mList, bSearchable);
        Log.i("Tact2", "Setting details: " + mList);
        
        LinearLayout searchResultLayout = (LinearLayout)view.findViewById(R.id.searchresult);
        LinearLayout searchFriendLayout = (LinearLayout)view.findViewById(R.id.searchfriend);
        searchResultLayout.setVisibility(View.GONE);
        searchFriendLayout.setVisibility(View.GONE);
        
        registerForContextMenu(mFreindListView);
        
        return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
	    
	    int friendId = mFreindListView.mAdapter.getItem(info.position);
	    
	    menu.setHeaderTitle("Options:");
	    menu.add(mList.hashCode(), friendId,0,"Remove");
	    menu.add(mList.hashCode() + 1,-1,0,"Cancel");
	    
	   
	    
	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
    }

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.friendlist_menu, menu);
		
		if (bSearchable == false)
		{
			menu.removeItem(R.id.action_search_friend);
		}
		
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		if (item.getItemId() == R.id.action_search_friend)
		{
			LinearLayout searchFriendLayout = (LinearLayout)getView().findViewById(R.id.searchfriend);
	        searchFriendLayout.setVisibility(View.VISIBLE);
	        
	        EditText searchEditText = (EditText)getView().findViewById(R.id.search_text);
	        searchEditText.setHint("search alias to add");
	        searchEditText.setText("");
	        searchEditText.setEnabled(true);
	        
	        InputFilter alphaNumericFilter = new InputFilter() {   
	            @Override  
	            public CharSequence filter(CharSequence arg0, int arg1, int arg2, Spanned arg3, int arg4, int arg5)  
	            {  
	               for (int k = arg1; k < arg2; k++)
	               {   
	            	   if (!Character.isLetterOrDigit(arg0.charAt(k)))
	            	   {   
	            		   return "";   
	            	   }   
	               }   
	               return null;   
	            }   
	          };   
	        
	        searchEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(24), alphaNumericFilter });
	        
	        Button searchButton = (Button)getView().findViewById(R.id.search_button);
	        searchButton.setEnabled(true);
	        searchButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0)
				{
					ClickedSearchFriend();
				}});
	        
	        LinearLayout searchResultLayout = (LinearLayout)getView().findViewById(R.id.searchresult);
	        searchResultLayout.setVisibility(View.GONE);
	        
	        searchEditText.setImeActionLabel("Search", KeyEvent.KEYCODE_ENTER);
		     
	        searchEditText.setOnEditorActionListener(new OnEditorActionListener(){
		
					@Override
					public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event)
					{
						if (event != null && event.getAction() != KeyEvent.ACTION_DOWN)
						{
					        return false;
					    } else if (actionId == EditorInfo.IME_ACTION_SEND
					        || event == null
					        || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
					    {
					    	ClickedSearchFriend();
							return true;
					    }
		
						return false;
					}});
	        
	        
            return true;
		} 
        
        return super.onOptionsItemSelected(item);
        
    }
	
	void ClickedSearchFriend()
	{
		LinearLayout searchResultLayout = (LinearLayout)getView().findViewById(R.id.searchresult);
        searchResultLayout.setVisibility(View.VISIBLE);
        
        Button cancelButton = (Button)getView().findViewById(R.id.cancel_button);
        cancelButton.setEnabled(true);
        cancelButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0)
			{
				CancelSearch();
			}});
        
        Button searchButton = (Button)getView().findViewById(R.id.search_button);
        searchButton.setEnabled(false);
        
        Button addButton = (Button)getView().findViewById(R.id.add_button);
        addButton.setEnabled(false);
        
        ImageView image = (ImageView)getView().findViewById(R.id.searchresult_logo);
        image.setVisibility(View.GONE);
        
        TextView searchResultText = (TextView)getView().findViewById(R.id.searchresult_name);
        searchResultText.setText("Searching...");
        searchResultText.setTextColor(Color.rgb(128,  128,  128));
        
        ProgressBar progress = (ProgressBar)getView().findViewById(R.id.search_spinner);
        progress.setVisibility(View.VISIBLE);
        
        EditText searchEditText = (EditText)getView().findViewById(R.id.search_text);
        searchEditText.setEnabled(false);
        
        
        
        Random rand = new Random();
        mSearchId = rand.nextInt();
        
        DoSearch(searchEditText.getText().toString(), mSearchId);
	}
	
	void CancelSearch()
	{
		LinearLayout searchResultLayout = (LinearLayout)getView().findViewById(R.id.searchresult);
        searchResultLayout.setVisibility(View.GONE);
        
        LinearLayout searchFriendLayout = (LinearLayout)getView().findViewById(R.id.searchfriend);
        searchFriendLayout.setVisibility(View.VISIBLE);
        
        EditText searchEditText = (EditText)getView().findViewById(R.id.search_text);
        searchEditText.setText("");
        searchEditText.setEnabled(true);
        
        Button searchButton = (Button)getView().findViewById(R.id.search_button);
        searchButton.setEnabled(true);
        
        mSearchId = -1;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		if (item.getGroupId() == mList.hashCode()) // remove
		{
			int friendId = item.getItemId();
			final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			int myUserId = (int)appWrapper.GetUserId();
			ArrayList<Integer> friends = new ArrayList<Integer>();
			friends.add(friendId);
			SyncListDB.getInstance(getActivity()).RemoveFromSyncList(friends, mList, myUserId);
				
			mFreindListView.mAdapter.remove(friendId);
			mFreindListView.mAdapter.notifyDataSetChanged();
			
			SyncListDB.getInstance(getActivity()).SyncWithServer(mList, myUserId, null);
			return true;
		} else if (item.getGroupId() == mList.hashCode() + 1) // cancel
		{
			return true;
		}
		
	    	
		return super.onContextItemSelected(item); 
	}
	
	public void Refresh()
	{
		if (mFreindListView == null) return;
		mFreindListView.Refresh();
	}
	
	@Override
	public void onHiddenChanged(boolean bHidden)
	{
		super.onHiddenChanged(bHidden);
		if (bHidden == false) Refresh();
	}
		
	public void onResume()
	{
		super.onResume();
		
		// clear the loading notification, if it's there
		//setProgressBarIndeterminateVisibility(Boolean.FALSE);
		
		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		
		//mFreindListView.UpdateFriendList();
		
		final SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
		
		syncListDB.SyncWithServer(mList, (int)appWrapper.GetUserId(), syncListDB.new ISyncEvents()
		{
			@Override
			public void onListUpdated()
			{
				mFreindListView.Refresh();
			}
		});
		
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		
		
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
	}
	
	void AddFriend(final int friendIdToAdd)
	{
		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
		ArrayList<Integer> friendToAdd = new ArrayList<Integer>();
		friendToAdd.add(friendIdToAdd);
		syncListDB.AddToSyncList(friendToAdd, mList, (int)appWrapper.GetUserId());
		syncListDB.SyncWithServer(mList, (int)appWrapper.GetUserId(), null);
		
		UserProfileDB userProfileDB = UserProfileDB.getInstance(getActivity());
		
		Button addButton = (Button)getView().findViewById(R.id.add_button);
        addButton.setEnabled(false);
		
		userProfileDB.GetFromServer(getActivity(), friendIdToAdd, userProfileDB.new IUpdateEvent(){

			@Override
			public void onUpdated()
			{
				mFreindListView.mAdapter.remove(friendIdToAdd);
				mFreindListView.mAdapter.add(friendIdToAdd);
				mFreindListView.mAdapter.notifyDataSetChanged();
			}

			@Override
			public void onUpdatedBackground()
			{
				// TODO Auto-generated method stub
				
			}});
		
		
	}
	
	public void DoSearch(final String alias, int id)
	{
		final PackageSearchAlias p = new PackageSearchAlias();
		p.searchAlias = alias;
		p.searchId = id;
		
		PackageDelivery sender = new PackageDelivery(getActivity(), p, new ISendEvents(){

			@Override
			public void preExecute()
			{
 
			}

			@Override
			public void postExecute()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.searchId != mSearchId) return;
					
					ImageView playerLogo = (ImageView)getView().findViewById(R.id.searchresult_logo);
					TextView playerAlias = (TextView)getView().findViewById(R.id.searchresult_name);
					playerAlias.setText(p.alias);
					playerAlias.setTextColor(Color.rgb(255, 255, 255));
					ProgressBar progress = (ProgressBar)getView().findViewById(R.id.search_spinner);
			        progress.setVisibility(View.GONE);
			        
			        IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		    		playerLogo.setBackgroundColor(p.colour);
		    		playerLogo.setVisibility(View.VISIBLE);
		    		Drawable logoImage = appWrapper.GetLogoStore().GetLogo(p.logo);
		    		if (logoImage != null)
		    		{
		    			playerLogo.setImageDrawable(logoImage);
		    		}
		    		
		    		Button cancelButton = (Button)getView().findViewById(R.id.cancel_button);
		            cancelButton.setEnabled(true);
		            		            
		            Button addButton = (Button)getView().findViewById(R.id.add_button);
		            addButton.setEnabled(true);
		            
		            final int addId = (int) p.userId;
		            
		            addButton.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View arg0)
						{
							AddFriend(addId);
							
						}});
					
					
				} else if (p.mReturnCode == PackageResponse.ErrorNoSearch)
				{
					ProgressBar progress = (ProgressBar)getView().findViewById(R.id.search_spinner);
			        progress.setVisibility(View.INVISIBLE);
			        TextView searchResultText = (TextView)getView().findViewById(R.id.searchresult_name);
			        searchResultText.setText("Alias not found.");
			        searchResultText.setTextColor(Color.rgb(128,  128,  128));
			        
			        Button cancelButton = (Button)getView().findViewById(R.id.cancel_button);
		            cancelButton.setEnabled(false);
		            
		            EditText searchEditText = (EditText)getView().findViewById(R.id.search_text);
		            searchEditText.setEnabled(true);
		            
		            Button searchButton = (Button)getView().findViewById(R.id.search_button);
		            searchButton.setEnabled(true);
			        
				} else
				{
					ProgressBar progress = (ProgressBar)getView().findViewById(R.id.search_spinner);
			        progress.setVisibility(View.INVISIBLE);
			        TextView searchResultText = (TextView)getView().findViewById(R.id.searchresult_name);
			        searchResultText.setText("Network error - try again...");
			        searchResultText.setTextColor(Color.rgb(255,  32,  32));
			        
			        Button cancelButton = (Button)getView().findViewById(R.id.cancel_button);
		            cancelButton.setEnabled(false);
		            
		            EditText searchEditText = (EditText)getView().findViewById(R.id.search_text);
		            searchEditText.setEnabled(true);
		            
		            Button searchButton = (Button)getView().findViewById(R.id.search_button);
		            searchButton.setEnabled(true);
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
