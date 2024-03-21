package uk.co.eidolon.shared.views;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.ChatDB.ChatMessage;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.utils.IAppWrapper;

import Core.IState;
import Core.PlayerInfo;
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

public class ChatListView extends ListView
{
	public ChatListAdapter mAdapter;
	
	public long mUserID = -1;
	
	Context mContext;
	public ChatListView(Context c, AttributeSet attrs)
	{
		super(c, attrs);
		
		mContext = c.getApplicationContext();
		
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		mUserID = appWrapper.GetUserId();
		
		setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		
		mAdapter = new ChatListAdapter(mContext, 0, new ArrayList<ChatMessage>());
        setAdapter(mAdapter);
	}
	
	
	public void Populate(int gameId)
	{
		ArrayList<ChatMessage> messages = ChatDB.getInstance(mContext).GetMessagesForGameID(gameId, 0);
		mAdapter.clear();
		mAdapter.addAll(messages);
		mAdapter.notifyDataSetChanged();
	}
	
	public boolean HasUnreadMessages()
	{
		for (ChatMessage message : mAdapter.mItems)
		{
			if (message.bRead == false) return true;
		}
		
		return false;
	}
	
	public void MarkAllRead()
	{
		for (ChatMessage message : mAdapter.mItems)
		{
			if (message.bRead == false)
			{
				if (message.id != -1)
				{
					message.bRead = true;
					ChatDB.getInstance(mContext).MarkChatRead(message.id);
				}
			}
		}
	}
	
	
	
	public void Update(int gameId)
	{
		int recent = 0;
		// work out when the last received message was
		for (ChatMessage message : mAdapter.mItems)
		{
			if (message.time > recent) recent = message.time;
		}
		
		// and fetch the whole lot back from the db
		ArrayList<ChatMessage> messages = ChatDB.getInstance(mContext).GetMessagesForGameID(gameId, recent + 1);
		while (messages.size() > 50)
		{
			messages.remove(0);
		}
		for (ChatMessage msg : messages)
		{
			AddMessage(msg);
		}
		
		mAdapter.notifyDataSetChanged();
	}
	
	Map<Integer, IState> gameCache = new TreeMap<Integer, IState>();
	
	public void AddMessage(ChatMessage msg)
	{
		IState state = null;
    	if (gameCache.containsKey(msg.toGameId))
    	{
    		state = gameCache.get(msg.toGameId);
    	} else
    	{
    		state =  GameDatabase.getInstance(mContext).GetGame(msg.toGameId, mUserID);
    		if (state != null)
    		{
    			gameCache.put(msg.toGameId, state);
    		}
    	}
    	if (state != null)
    	{
    		PlayerInfo info = state.GetPlayerInfo(msg.fromUserId);
    		if (info != null)
    		{
    			msg.fromAlias = info.name;
    		}
    		
    		if (msg.toUserId > 0)
    		{
    			PlayerInfo info2 = state.GetPlayerInfo(msg.toUserId);
    			msg.toAlias = info2.name;
    		}
    	}
    	
		
		mAdapter.add(msg);
		if (mAdapter.mItems.size() > 50)
    	{
    		mAdapter.mItems.remove(0);
    	}
	}
	
	public void AddMessage(int fromUserId, int toUserId, int toGameId, int fromColour, String fromLogo, int chatId, int time, String messageStr)
    {
    	ChatMessage msg = ChatDB.getInstance(mContext).new ChatMessage();
    	msg.fromUserId = fromUserId;
    	msg.toUserId = toUserId;
    	msg.bRead = true;
    	msg.toGameId = toGameId;
    	msg.message = messageStr;
    	msg.fromColour = fromColour;
    	msg.fromLogo = fromLogo;
    	msg.bRead = false;
    	msg.time = time;
    	
    	
    	if (chatId == -1) msg.bRead = true;
    	msg.id = chatId;
    	
    	AddMessage(msg);
    }
	
	public class ChatListAdapter extends ArrayAdapter<ChatMessage>
	{
		
		
		private ArrayList<ChatMessage> mItems;
	    
	    Context mContext;
	    
	    Map<Integer, Drawable> logoCache = new TreeMap<Integer, Drawable>();

	    public ChatListAdapter(Context context, int textViewResourceId, ArrayList<ChatMessage> items)
	    {
	            super(context, textViewResourceId, items);
	            this.mItems = items;
	            mContext = context;
	    }
	    
	    void PopulateView(ChatMessage message, Context context, View view)
	    {
	    	if (context == null) return;
	    	
	    	IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
	    	
	    	//TextView messageAlias = (TextView)view.findViewById(R.id.chat_alias);
	    	//messageAlias.setText(message.fromAlias);
	    	
	    	TextView messageText = (TextView)view.findViewById(R.id.chat_message);
	    	messageText.setText(message.message);
	    	messageText.setTextColor(Color.rgb(255, 255,  255));
	    	ImageView messageLogo = (ImageView)view.findViewById(R.id.chat_logo);
	    	
	    	if (logoCache.containsKey(message.fromUserId))
	    	{
	    		messageLogo.setImageDrawable(logoCache.get(message.fromUserId));
	    		messageLogo.setBackgroundColor(message.fromColour);
	    	} else
	    	{
	    		Drawable logo = appWrapper.GetLogoStore().GetLogo(message.fromLogo);
	    		messageLogo.setImageDrawable(logo);
	    		messageLogo.setBackgroundColor(message.fromColour);
	    		logoCache.put(message.fromUserId, logo);
	    	}
	    	
	    	if (message.fromAlias.length() > 0)
	    	{
	    		if (message.toUserId != -1)
	    		{
	    			TextView aliasText = (TextView)view.findViewById(R.id.chat_fromalias);
	    			if (mUserID == message.fromUserId)
	    			{
	    				aliasText.setText(message.fromAlias + " (whispering to " + message.toAlias + ")");
	    			} else
	    			{
	    				aliasText.setText(message.fromAlias + " (to you only)");
	    			}
	    			messageText.setTextColor(Color.rgb(220, 220,  0));
	    		} else
	    		{
	    			TextView aliasText = (TextView)view.findViewById(R.id.chat_fromalias);
	    			aliasText.setText(message.fromAlias);
	    		}
	    	} else
	    	{
	    		TextView aliasText = (TextView)view.findViewById(R.id.chat_fromalias);
	    		aliasText.setText("");
	    	}
	    	
	    }
	    
	    
	    
	    
	    View InflateView(Context context)
	    {
	    	if (context == null) return null;
	    	
	    	LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = vi.inflate(R.layout.chat_item, null);
	        return view;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	View view = convertView;
	    	ChatMessage message = mItems.get(position);
	    	if (view == null)
	    	{
	    		view = InflateView(mContext);
	    		
	    	}
	    	PopulateView(message, mContext, view);
	    	
	        return view;
	    }
	}
	
	
}
