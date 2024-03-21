package uk.co.eidolon.shared.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.ChatDB.ChatMessage;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.ChatListView;
import uk.co.eidolon.shared.views.GameListView;
import uk.co.eidolon.shared.views.IStateInfo;
import Core.IState;
import Network.PackageResponse;
import Network.SyncList;
import Network.Packages.PackageSendChat;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager.LayoutParams;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class ChatFragment extends Fragment implements android.view.GestureDetector.OnGestureListener
{
	private GestureDetector gestureDetector;
	boolean mReceiversRegistered = false;
	String mIncomingChatIntentAction;
	
	public static ChatFragment getInstance()
	{
		return new ChatFragment();
        
    }
	
	int mGameId = -1;
	int mUserId = -1;
	int topMarginClosed;
	
	int maxChatExpansion = 400;
	
	Map<String, Integer> mUsers = new TreeMap<String, Integer>();
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
		
		final View view = populateViewForOrientation(inflater, container);
		
		final Spinner toSpinner = (Spinner)view.findViewById(R.id.to_spinner);
		
		IState state = GameDatabase.getInstance(getActivity()).GetGame(mGameId, mUserId);
		
		if (state == null) return view;
		
		List<String> list = new ArrayList<String>();
		list.add("All");
		mUsers.put("All", -1);
		for (Integer userId : state.GetPlayers())
		{
			if (userId != mUserId)
			{
				list.add(state.GetPlayerInfo(userId).name);
				mUsers.put(state.GetPlayerInfo(userId).name, userId);
				
			}
		}
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.chat_spinner_item, list);
		dataAdapter.setDropDownViewResource(R.layout.chat_spinner_item);
		toSpinner.setAdapter(dataAdapter);
		
		toSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	EditText messageEdit = (EditText)view.findViewById(R.id.message_entry);
		    	
		    	String item = (String) toSpinner.getItemAtPosition(position);
		    	
		    	if (position == 0)
		    	{
		    		messageEdit.setHint("Send message to all players");
		    	} else
		    	{
		    		messageEdit.setHint("Send message to " + item);
		    	}
		    	
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		return view;
	}
	
	void HideError()
	{
		TextView errorText = (TextView)getView().findViewById(R.id.error_text);
		errorText.setVisibility(View.GONE);
	}
	
	void ShowError(String text)
	{
		if (getView() == null) return;
		
		TextView errorText = (TextView)getView().findViewById(R.id.error_text);
		errorText.setVisibility(View.VISIBLE);
		errorText.setText(text);
	}
	
	void ShowNewMessageStar()
	{
		if (getView() == null) return;
		ImageView messageStar = (ImageView)getView().findViewById(R.id.new_message_star);
		messageStar.setVisibility(View.VISIBLE);
	}
	
		
	void HideNewMessageStar()
	{
		if (getView() == null) return;
		ImageView messageStar = (ImageView)getView().findViewById(R.id.new_message_star);
		messageStar.setVisibility(View.GONE);
	}
	
	// Define a handler and a broadcast receiver
		private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
		{
			
			@Override
			public void onReceive(Context context, Intent intent)
			{
				if (getView() == null) return;
				
				String mAction = intent.getAction();
				
			    if(mAction.equals(mIncomingChatIntentAction))
			    {
			    	Log.i("Tact2", "Got a refresh intent");
			    	Bundle b = intent.getExtras();
			    	
			    	if (b.containsKey("message"))
			    	{
			    		int toUserId = b.getInt("toUserId");
			    		int toGameId = b.getInt("toGameId");
			    		int fromUserId = b.getInt("fromUserId");
			    		String messageStr = b.getString("message");
			    		String fromLogo = b.getString("fromLogo");
			    		int fromColour = b.getInt("fromColour");
			    		int chatId = b.getInt("chatId");
			    		
			    		if ((toUserId == -1 || toUserId == mUserId) && toGameId == mGameId)
			    		{
			    			ChatListView chatView = (ChatListView)getView().findViewById(R.id.chat_area);
			    			long millis = System.currentTimeMillis();
			    			int time = (int)(millis / 1000);
			    			chatView.AddMessage(fromUserId, toUserId, toGameId, fromColour, fromLogo, chatId, time, messageStr);
			    			
			    			if (bOpen == false) 
			    			{
			    				ShowNewMessageStar();
			    			} else
			    			{
			    				ChatDB.getInstance(getActivity()).MarkChatRead(chatId);
			    			}
			    		}
			    	}
			    	
			    }
			    
			    
			}
		};
	
	public void SetGameId(int gameId)
	{
		mGameId = gameId;
	}
	
	boolean bSending = false;
	
	String GetToken()
	{
		// TODO Auto-generated method stub
		try
		{
			if (getActivity() == null) return "";
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			String result = GoogleAuthUtil.getTokenWithNotification(getActivity(), appWrapper.GetAccountName() , "audience:server:client_id:240307466011.apps.googleusercontent.com", null);
			return result;
		} catch (UserRecoverableAuthException e)
		{
			return "";
		} catch (IOException e)
		{
			return "";
		} catch (GoogleAuthException e)
		{
			return "";
		}
	}
	
	String lastMessage = "";
	
	void SendMessage()
	{
		if (getActivity() == null) return;
		final EditText messageSendEdit = (EditText)getView().findViewById(R.id.message_entry);
		
		String message = messageSendEdit.getText().toString();
		if (message.compareTo(lastMessage) == 0) return;
		lastMessage = message;
		if (message.length() < 1) return;
		if (bSending == true) return;
		
		bSending = true;
		
		new GetTokenAndSendChat().execute(message);
		
		
	}
	
	// Retrieves the state and generates thumbnails for the items in the list
		private class GetTokenAndSendChat extends AsyncTask<String, Integer, Integer>
		{
			GetTokenAndSendChat()
			{
				super();
			}
					
			@Override
			protected void onPreExecute()
			{
				ShowError("Sending...");
			}
			
			@Override
			protected void onPostExecute(Integer code)
			{
				if (getActivity() == null) return;
				if (code == -1)
				{
					ShowError("Please accept identity confirmation to chat.");
				}
				
				if (code == -2)
				{
					ShowError("Unable to send now.");
				}
				
				bSending = false;
			}
			
			@Override
		    protected void onProgressUpdate(Integer... v)
			{
		        super.onProgressUpdate(v);
		    }
			
			@Override
			protected Integer doInBackground(String... string)
			{
				String token = GetToken();
				//String token = "FakeToken";
				
				if (token == "")
				{
					return -1;
				}
				
				final PackageSendChat p = new PackageSendChat();
				if (getActivity() == null) return -2;
				IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
				p.fromUserId = (int)appWrapper.GetUserId();
				
				final Spinner toSpinner = (Spinner)getView().findViewById(R.id.to_spinner);
				int position = toSpinner.getSelectedItemPosition();
				String toUserStr = (String)toSpinner.getItemAtPosition(position);
				int toUserId = mUsers.get(toUserStr);
				
				p.toUserId = toUserId;
				p.toGameId = mGameId;
				
				p.messageStr = string[0];
				Random rand = new Random();
				p.chatOTPKey = rand.nextInt();
				p.idToken = token;
				
				//Log.i("GameLib", "Token: " + token);
			
				PackageDelivery sender = new PackageDelivery(getActivity(), p, new ISendEvents(){

					@Override
					public void preExecute()
					{
						// TODO Auto-generated method stub
						
					}

					@Override
					public void postExecute()
					{
						if (getView() == null || getActivity() == null) return;
						// TODO Auto-generated method stub
						// and add to the adapter
						if (p.mReturnCode == PackageResponse.Success)
						{
							if (p.bSent == true)
							{
								ChatListView chatView = (ChatListView)getView().findViewById(R.id.chat_area);
								IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
								long millis = System.currentTimeMillis();
								int time = (int)(millis / 1000);
								chatView.AddMessage(p.fromUserId, p.toUserId, p.toGameId, appWrapper.GetColour(), appWrapper.GetLogo(), -1, time, p.messageStr);
								EditText messageSendEdit = (EditText)getView().findViewById(R.id.message_entry);
								messageSendEdit.setText("");
								HideError();
							} else
							{
								ShowError("Not sent - unable to send now.");
							}
						} else
						{
							ShowError("Not sent - network error.");
						}
					}

					@Override
					public void postExecuteBackground()
					{
						if (p.mReturnCode == PackageResponse.Success && p.bSent == true)
						{
						// 	TODO Auto-generated method stub
						// 	add it to the database
							if (getActivity() == null) return;
							IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
							long rowId = ChatDB.getInstance(getActivity()).AddChatMessage(p.fromUserId, p.toUserId, p.toGameId, appWrapper.GetColour(), appWrapper.GetLogo(), p.messageStr);
							
							if (rowId != -1)
							{
								ChatDB.getInstance(getActivity()).MarkChatRead((int) rowId);
							}
						}
						
					}
				});
				
				sender.Send();
				
				return 0;
			}
			
			
		    
		   
		}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//setRetainInstance(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
	    super.onSaveInstanceState(outState);

	    outState.putInt("GameID",  mGameId);
	    
	
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			mGameId = savedInstanceState.getInt("GameID");
		}
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		SetDimensions(PixtoDP(width), PixtoDP(height));
	}
	
	void SetDimensions(int width, int height)
	{
		View view = getView();
		
		if (varl != null)
			varl.end();
		 
		 final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
		 
		 //int width = newConfig.screenWidthDp;
		 //int height = newConfig.screenHeightDp;
		 
		 int chatHeight = Math.min(height - 50, 400);
		 int chatWidth = Math.min(width - 50, 500);
		 
		 params.width = DPtoPix(chatWidth);
		 params.height = DPtoPix(chatHeight);
		 int topMargin = 0;
		 topMarginClosed = (DPtoPix(40 - chatHeight));
		 if (bOpen == false)
		 {
			 topMargin = topMarginClosed;
			 
		 } else
		 {
			 topMargin = 0;
		 }
		 params.topMargin = topMargin;
		 
		 view.setLayoutParams(params);
		 
		 
		 
		 view.requestLayout();
	}
	
	int DPtoPix(int val)
	{
		Resources r = getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, r.getDisplayMetrics());
		return Math.round(px);
	}
	
	int PixtoDP(float px)
	{
	    Resources resources = getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return Math.round(dp);
	}
	
	 @Override
	 public void onConfigurationChanged(Configuration newConfig)
	 {
		 super.onConfigurationChanged(newConfig);
		 
		 SetDimensions(newConfig.screenWidthDp, newConfig.screenHeightDp);
	 }
	  
	 private View populateViewForOrientation(LayoutInflater inflater, ViewGroup viewGroup)
	 {
		
		View view = inflater.inflate(R.layout.game_chat_layout, viewGroup, false);
		  
		 
	     RelativeLayout expanderLayout = (RelativeLayout)view.findViewById(R.id.expander_tab);
	     
	     gestureDetector = new GestureDetector(getActivity(), this);
	
	     expanderLayout.setOnTouchListener(new OnTouchListener() {
	         @Override
	         public boolean onTouch(View v, MotionEvent event) {
	         	gestureDetector.onTouchEvent(event);
	             return true;
	         }
	     });
	     
	     EditText messageSendEdit = (EditText)view.findViewById(R.id.message_entry);
	     messageSendEdit.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);
	     
	     messageSendEdit.setOnEditorActionListener(new OnEditorActionListener(){
	
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
				    	SendMessage();
						return true;
				    }
	
					return false;
				}});
	     
	     getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	     
	     IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
	     mIncomingChatIntentAction = appWrapper.GetIncomingChatIntentAction();
	     mUserId = (int)appWrapper.GetUserId();
	     
	     //ChatListView chatView = (ChatListView)view.findViewById(R.id.chat_area);
	     //chatView.Populate(mGameId);
	     
	     if (mReceiversRegistered == false)
			{
				mReceiversRegistered = true;
				LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mIntentReceiver, new IntentFilter(mIncomingChatIntentAction));
			}
	     
	     ImageView messageStar = (ImageView)view.findViewById(R.id.new_message_star);
		messageStar.setVisibility(View.GONE);
			
			return view;
	 }
	
		
	public void onResume()
	{
		super.onResume();
		
		HideError();
		
		if (getView() != null)
		{
			ChatListView chatView = (ChatListView)getView().findViewById(R.id.chat_area);
			
			chatView.Update(mGameId);
			
			boolean bUnread = chatView.HasUnreadMessages();
			if (bUnread == true && bOpen == false)
			{
				ShowNewMessageStar();
			}
			
			registerForContextMenu(chatView);
			
		}
		
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		ChatListView chatView = (ChatListView)getView().findViewById(R.id.chat_area);
	    if (chatView == null) return;
	    
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
	    ChatMessage message = chatView.mAdapter.getItem((int) info.position);
	    if (message == null) return;
	    
	    SyncList friends = SyncListDB.getInstance(getActivity()).GetList("FriendList", mUserId);
	    SyncList blocked = SyncListDB.getInstance(getActivity()).GetList("BlockList", mUserId);
	    
	    if (message.fromUserId == mUserId)
	    {
	    	
	    } else
	    {
	    	int fromUser = message.fromUserId;
	    	
	    	if (friends.GetList().contains(fromUser) == false)
	    		menu.add(100, fromUser, 0, "Add Friend");
	    	
	    	if (blocked.GetList().contains(fromUser) == false)
	    		menu.add(200, fromUser, 0, "Block User");
	    }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
	    if (item.getGroupId() == 100) // add friend
	    {
	    	int friendToAdd = item.getItemId();
	    	
	    	SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
    		ArrayList<Integer> friendListToAdd = new ArrayList<Integer>();
    		friendListToAdd.add(friendToAdd);
    		syncListDB.AddToSyncList(friendListToAdd, "FriendList", mUserId);
    		syncListDB.SyncWithServer("FriendList", mUserId, null);
    		
	    	return true;
	    }
	    
	    if (item.getGroupId() == 200) // block user
	    {
	    	int userToBlock = item.getItemId();
	    	
	    	SyncListDB syncListDB = SyncListDB.getInstance(getActivity());
    		ArrayList<Integer> friendListToAdd = new ArrayList<Integer>();
    		friendListToAdd.add(userToBlock);
    		syncListDB.AddToSyncList(friendListToAdd, "BlockList", mUserId);
    		syncListDB.SyncWithServer("BlockList", mUserId, null);
	    	
	    	return true;
	    }
	    
	    
	    return false;
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



	@Override
	public boolean onDown(MotionEvent arg0)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	boolean bOpen = false;
	
	ValueAnimator varl = null;
	
	void OpenMessages()
	{
		bOpen = true;
		
		HideNewMessageStar();
		
        //From Top to Bottom
        final LinearLayout chatArea = (LinearLayout)getView().findViewById(R.id.game_chat_container);
        
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) chatArea.getLayoutParams();
        if (varl != null) varl.end();
        varl = ValueAnimator.ofInt(params.topMargin, 0);
        
        varl.setDuration(1000);
        
        varl.addUpdateListener(new AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
            	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) chatArea.getLayoutParams();
                lp.setMargins(0, (Integer) animation.getAnimatedValue(), 0, 0);
                chatArea.setLayoutParams(lp);      
            }
        });
        varl.start();
        
        
        ChatListView chatView = (ChatListView)getView().findViewById(R.id.chat_area);
        chatView.MarkAllRead();
		
	}
	
	void CloseMessages()
	{
		bOpen = false;
		
		final LinearLayout chatArea = (LinearLayout)getView().findViewById(R.id.game_chat_container);
        
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) chatArea.getLayoutParams();
        
        if (varl != null) varl.end();
        varl = ValueAnimator.ofInt(params.topMargin, topMarginClosed);
        
        varl.setDuration(1000);
        
        varl.addUpdateListener(new AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
            	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) chatArea.getLayoutParams();
                lp.setMargins(0, (Integer) animation.getAnimatedValue(), 0, 0);
                chatArea.setLayoutParams(lp);      
            }
        });
        varl.start();
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY)
	{
		int SWIPE_MIN_DISTANCE = 20; //120;
        int SWIPE_THRESHOLD_VELOCITY = 10; //200;

        if(e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY)
        {
            CloseMessages();
            
            return true;
        }  else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY)
        {
        	OpenMessages();
            return true;
        }
        return false;
	}



	@Override
	public void onLongPress(MotionEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}



	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3)
	{
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void onShowPress(MotionEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}



	@Override
	public boolean onSingleTapUp(MotionEvent arg0)
	{
		// TODO Auto-generated method stub
		return false;
	}

	

}
