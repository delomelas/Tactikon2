package uk.co.eidolon.shared.activities;

import java.util.ArrayList;
import java.util.Random;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.ServerUploadQueue;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.ChatListView;
import uk.co.eidolon.shared.views.FriendSelectAdapter;
import uk.co.eidolon.shared.views.GameListView;
import uk.co.eidolon.shared.views.IStateInfo;

import Core.IEvent;
import Core.IState;
import Core.IState.GameStatus;
import Core.PlayerInfo;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.SyncList;
import Network.Packages.PackageSendChat;
import Network.Packages.PackageSendInvite;
import Support.Preferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListPopupWindow;
import android.widget.Toast;

public class GameListFragment extends Fragment
{
	GameListView mGameListView;
	
	String mRefreshIntentAction;
	String mIncomingChatIntentAction;
	
	Fragment mFragment;
	
	
	public static GameListFragment getInstance()
	{
		return new GameListFragment();
        
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.mainactivity, container, false);
        
        final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		mRefreshIntentAction = appWrapper.GetStateUpdatedIntentAction();
		mIncomingChatIntentAction = appWrapper.GetIncomingChatIntentAction();
		
		Account[] accounts = AccountManager.get(this.getActivity()).getAccountsByType("com.google");
		
		if (accounts.length != 1 && appWrapper.GetUserId() == -1) // if the user has two or more accounts, force them to be set up now
		{
			Intent i = new Intent(getActivity(), QueryAccountActivity.class);
        	startActivity(i);
        }
		
        mGameListView = (GameListView)view.findViewById(R.id.gameslist);
        
        mGameListView.setClickable(true);
        mGameListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
			public void onItemClick(AdapterView<?> view, View parentView, int position, long id)
			{
				IStateInfo o = mGameListView.mAdapter.getItem(position);
				Intent gameIntent = new Intent(getActivity(), GameActivity.class);
				
				gameIntent.putExtra("GameID", o.GetGameID());
				
		    	startActivity(gameIntent);
			}
        });
        
        registerForContextMenu(mGameListView);

		if (mReceiversRegistered == false)
		{
			mReceiversRegistered = true;
			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mIntentReceiver, new IntentFilter(mRefreshIntentAction));
			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mIntentReceiver, new IntentFilter(mIncomingChatIntentAction));
			
		}
	
        return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		//setRetainInstance(true);
		
		
	    
	}
	
	@Override
	public void onActivityCreated(Bundle bundle)
	{
		super.onActivityCreated(bundle);
		FragmentManager fragmentManager = this.getFragmentManager();
		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		//if (mFragment == null)
	    {
	    	mFragment = appWrapper.MainFragmentFactory(getActivity());
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        fragmentTransaction.replace(R.id.gamelist_container, mFragment);
	        fragmentTransaction.commit();
	    }
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
	    //super.onCreateContextMenu(menu, v, menuInfo);
	    if (mGameListView == null) return;
	    
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
	    IStateInfo o = mGameListView.mAdapter.getItem((int) info.position);
	    if (o == null) return;
	    IState state = o.GetState();
	    
	    if (state == null) return;
	    
	    final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
	    
	    if (state.GetGameState() == GameStatus.GameOver || state.GetGameState() == GameStatus.TimeOut)
	    {
	    	menu.add(300, o.GetGameID(), 0, "Delete");
	    }
	    
	    if (state.GetGameState() == GameStatus.InGame && state.IsPlayerAlive(appWrapper.GetUserId()) == false)
	    {
	    	menu.add(300,o.GetGameID(),0,"Delete");
	    }
	    
	    SyncList friends = SyncListDB.getInstance(getActivity()).GetList("FriendList", (int)appWrapper.GetUserId());
	    
	    if (friends.GetList().isEmpty() == false && state.GetGameState() == GameStatus.WaitingForPlayers && (state.IsFriendsOnly() == false || state.GetCreatorId() == appWrapper.GetUserId()))
	    {
	    	ArrayList<Integer> inGame = state.GetPlayers();
    		ArrayList<Integer> remaining = friends.GetList();
    		remaining.removeAll(inGame);
    		
    		if (remaining.size() > 0)
    		{
    			menu.add(400, o.GetGameID(), 0, "Invite...");
    		}
	    }
	    	
	    
	    if (state.GetGameState() == GameStatus.InGame && state.IsPlayerAlive(appWrapper.GetUserId()) == true && o.GetGameID() >= 0)
	    {
	    	SubMenu subMenu = menu.addSubMenu("Surrender...");
	    	subMenu.add(100, o.GetGameID(), 0, "Yes, surrender");
	    	subMenu.add(100,-1,0,"No, cancel");
	    }
	    
	    if (o.GetGameID() < 0 && state.GetGameState() != GameStatus.GameOver)
	    {
	    	menu.add(300,o.GetGameID(),0,"Delete");
	    }
	    
	    menu.setHeaderTitle("Options:");
	    
	    // add "add friend" items
	    if (o.GetState() != null && appWrapper.GetUserId() != -1)
	    {
	    	final SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
	    	SyncList friendsList = syncListDB.GetList("FriendList", (int)appWrapper.GetUserId());
	    	ArrayList<PlayerInfo> players = new ArrayList<PlayerInfo>();
	    	for (Integer playerId : o.GetState().GetPlayers())
	    	{
	    		if (playerId != appWrapper.GetUserId() && friendsList.GetList().contains(playerId) == false && playerId < Integer.MAX_VALUE - 20)
	    		{
	    			players.add(o.GetState().GetPlayerInfo(playerId));
	    		}
	    	}
	    	
	    	if (players.size() > 0)
	    	{
	    		SubMenu subMenu = menu.addSubMenu("Add friend...");
	    		for (PlayerInfo playerInfo : players)
	    		{
	    			subMenu.add(200, (int) (playerInfo.userId), 0, playerInfo.name);
	    		}
	    	}
	    }
	    	    
	    menu.add(-1,-1,0,"Cancel");
	    
	}
	
	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
	    if (item.getGroupId() == 100) // surrender
	    {
	    	if (item.getItemId() != -1) // we didn't cancel the surrender
	    	{
	    		IStateInfo o = mGameListView.findItem(item.getItemId());
			    final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			    IEvent event = appWrapper.SurrenderGameEventFactory(appWrapper.GetUserId());
			    EventQueueItem queueItem = new EventQueueItem();
			    queueItem.event = event;
			    queueItem.GameID = o.GetGameID();
			    ServerUploadQueue queue = new ServerUploadQueue(getActivity());
			    queue.AddToQueue(queueItem);
			    queue.FlushQueue();
	    	}
	    	return true;
	    	
	    } else if (item.getGroupId() == 300) // delete game
	    {
	    	IStateInfo o = mGameListView.findItem(item.getItemId());
			final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
	    	final SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
	    	ArrayList<Integer> toDelete = new ArrayList<Integer>();
	    	toDelete.add(o.GetGameID());
	    	syncListDB.RemoveFromSyncList(toDelete, "GameList", (int)appWrapper.GetUserId());
	    	mGameListView.UpdateStateList();
	    	return true;
	    	
	    } else if (item.getGroupId() == 200) // add friend
	    {
	    	if (item.getItemId() > 0)
	    	{
	    		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
	    		SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
	    		ArrayList<Integer> friendToAdd = new ArrayList<Integer>();
	    		friendToAdd.add(item.getItemId());
	    		syncListDB.AddToSyncList(friendToAdd, "FriendList", (int)appWrapper.GetUserId());
	    		syncListDB.SyncWithServer("FriendList", (int)appWrapper.GetUserId(), null);
	    		UserProfileDB.getInstance(getActivity()).GetFromServer(getActivity(), item.getItemId(), null);
	    	}
	    	
	    	return true;
	    } else if (item.getGroupId() == 400) // invite
	    {
	    	final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
    		SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
    		SyncList friendList = syncListDB.GetList("FriendList", (int)appWrapper.GetUserId());
    		ArrayList<Integer> friends = friendList.GetList();
    		
    		IStateInfo o = mGameListView.findItem(item.getItemId());
    		friends.removeAll(o.GetState().GetPlayers());
    		
    		final FriendSelectAdapter selectAdapter = new FriendSelectAdapter(getActivity(), R.layout.friend_item, friends);
    		
    		final ListPopupWindow popupWindow = new ListPopupWindow(getActivity());
    		popupWindow.setAdapter(selectAdapter);
    		popupWindow.setAnchorView(getView());
    		popupWindow.setModal(true);
    		popupWindow.setHeight(getView().getHeight());
    		popupWindow.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> parent, View view,int position, long id)
				{
					InviteUserToGame(selectAdapter.getItem(position), item.getItemId());
					popupWindow.dismiss();
				}});
    		popupWindow.show();
	    }
	    
	    return false;
	}
	
	void InviteUserToGame(int userId, int gameId)
	{
		PackageSendInvite sendInvitePackage = new PackageSendInvite();
		sendInvitePackage.toUserId = userId;
		IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		sendInvitePackage.fromAlias = appWrapper.GetAlias();
		sendInvitePackage.fromUserId = (int) appWrapper.GetUserId();
		sendInvitePackage.gameId = gameId;
	
		new DoSendInvite().execute(sendInvitePackage);
	}
	
	// Retrieves the state and generates thumbnails for the items in the list
	private class DoSendInvite extends AsyncTask<PackageSendInvite, Integer, Integer>
	{
		DoSendInvite()
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
	        
	    }
		
		@Override
		protected Integer doInBackground(final PackageSendInvite... invite)
		{
				
					
			PackageDelivery sender = new PackageDelivery(getActivity(), invite[0], new ISendEvents(){

				@Override
				public void preExecute()
				{
					// TODO Auto-generated method stub
					
				}

				@Override
				public void postExecute()
				{
					// TODO Auto-generated method stub
					// and add to the adapter
					PackageSendInvite p = invite[0];
					if (p.mReturnCode == PackageResponse.Success)
					{
						if (p.bSent == true)
						{
							Toast.makeText(getActivity().getApplicationContext(), "Invite sent", Toast.LENGTH_SHORT).show();
						} else
						{
							Toast.makeText(getActivity().getApplicationContext(), "Can't send invite now",  Toast.LENGTH_SHORT).show();
						}
					} else
					{
						// network error?
						Toast.makeText(getActivity().getApplicationContext(), "Invite not sent: network error",  Toast.LENGTH_SHORT).show();
					}
				}

				@Override
				public void postExecuteBackground()
				{
						
				}
			});
			
			sender.Send();
			
			return 0;
		}
		
		
	    
	   
	}
	
	private boolean mReceiversRegistered = false;

	// Define a handler and a broadcast receiver
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
	{
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String mAction = intent.getAction();
			
			if (getActivity() == null) return;
						
		    if(mAction.equals(mRefreshIntentAction))
		    {
		    	Log.i("Tact2", "Got a refresh intent");
		    	Bundle b = intent.getExtras();
		    	long userId = -1;
		    	if (b.containsKey("UserID"))
		    	{
		    		userId = b.getLong("UserID");
		    	}
		    	if (b.containsKey("GameID"))
	    		{
	    			final int GameID = b.getInt("GameID");
	    			
	    			final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
	    			
	    			// only update the view if the correct player is playing
	    			if (appWrapper.GetUserId() == userId)
	    				RefreshStateView(GameID);
	    		}
		    	if (b.containsKey("UpdateGameList"))
		    	{
		    		UpdateGameList();
		    	}
		    }
		    
		    if (mAction.equals(mIncomingChatIntentAction))
		    {
		    	Log.i("Tact2", "Got a chat message");
		    	Bundle b = intent.getExtras();
		    	if (b.containsKey("message"))
		    	{
		    		int toGameId = b.getInt("toGameId");
		    		if (toGameId != -1)
		    		{
		    			RefreshStateView(toGameId);
		    		}
		    	}
		    }
		    
		    
		}
	};
	
	void RefreshStateView(int GameID)
	{
		// need to find the item in the mAdapter and form a new view for it
		Log.i("NimLog", "Game " + GameID + " has been updated in DB, updating view");
		mGameListView.RefreshState(GameID);
	}
	
	void UpdateGameList()
	{
		Log.i("NimLog", "Statelist has been updated, updating view");
		mGameListView.UpdateStateList();
	}
	
	public void onResume()
	{
		super.onResume();
		
		// clear the loading notification, if it's there
		//setProgressBarIndeterminateVisibility(Boolean.FALSE);
		
		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		mGameListView.mUserID = appWrapper.GetUserId();
		
		mGameListView.UpdateStateList();
		
		final SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
		
		// repair states with a sequenceId of zero
		Intent serviceIntent = new Intent();

		serviceIntent.setPackage("uk.co.eidolon.tact2");
    	serviceIntent.setAction(appWrapper.GetServerSyncIntentAction());
    	serviceIntent.putExtra("updateAllIfZero", true);
    	serviceIntent.putExtra("UserID", appWrapper.GetUserId());
    	getActivity().startService(serviceIntent);
		
		syncListDB.SyncWithServer("GameList", (int)appWrapper.GetUserId(), syncListDB.new ISyncEvents()
		{
			@Override
			public void onListUpdated()
			{
				mGameListView.UpdateStateList();
			}
		});
		
		// sync the friends list
		syncListDB.SyncWithServer("FriendList", (int)appWrapper.GetUserId(), null);
		
		// incase we've just logged in, make sure the menu is up to date
		getActivity().invalidateOptionsMenu();
		
		
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
		if(mReceiversRegistered)
		{
		 	LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mIntentReceiver);
		    mReceiversRegistered = false;
		}
	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu)
	{
		/*
        getActivity().getMenuInflater().inflate(R.menu.gamelist_menu, menu);
        
        final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		long UserID = appWrapper.GetUserId();
		
		if (UserID == -1)
		{
			menu.removeItem(R.id.action_search_games);
			menu.removeItem(R.id.action_new_game);
			menu.removeItem(R.id.action_refresh);
			
		} */
		
        super.onPrepareOptionsMenu(menu);
    }

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.gamelist_menu, menu);
		
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		final IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		
        // Handle item selection
		if (item.getItemId() == R.id.action_switch_account)
		{
        	Intent switchAccountIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.QueryAccountActivity.class);
        	startActivity(switchAccountIntent);
            return true;
		} /*else if (item.getItemId() == R.id.action_new_network_game)
		{
			if (appWrapper.GetUserId() == -1)
			{
				Intent switchAccountIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.QueryAccountActivity.class);
	        	startActivity(switchAccountIntent);
	            return true;
			}
			
        	Intent newGameIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.NewGameActivity.class);
        	newGameIntent.putExtra("Network", 1);
        	startActivity(newGameIntent);
		
            return true;
		}*/ else if (item.getItemId() == R.id.action_new_local_game)
		{
			if (appWrapper.GetUserId() == -1)
			{
				Intent switchAccountIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.QueryAccountActivity.class);
	        	startActivity(switchAccountIntent);
	            return true;
			}
			
        	Intent newGameIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.NewGameActivity.class);
        	newGameIntent.putExtra("Local", 1);
        	startActivity(newGameIntent);
		
            return true;
		} else if (item.getItemId() == R.id.action_search_games)
		{
			if (appWrapper.GetUserId() == -1)
			{
				Intent switchAccountIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.QueryAccountActivity.class);
	        	startActivity(switchAccountIntent);
	            return true;
			}
        	Intent searchGamesIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.SearchResultsActivity.class);
        	startActivity(searchGamesIntent);
            return true;
		} else if (item.getItemId() == R.id.action_refresh)
		{
			
			Intent serviceIntent = new Intent();
			serviceIntent.setPackage("uk.co.eidolon.tact2");
	    	serviceIntent.setAction(appWrapper.GetServerSyncIntentAction());
	    	serviceIntent.putExtra("updateAll", true);
	    	serviceIntent.putExtra("UserID", appWrapper.GetUserId());
	    	getActivity().startService(serviceIntent);
		}
        
        return super.onOptionsItemSelected(item);
        
    }

}
