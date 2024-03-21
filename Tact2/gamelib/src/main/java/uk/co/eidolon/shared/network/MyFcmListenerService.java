package uk.co.eidolon.shared.network;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

import Network.SyncList;
import uk.co.eidolon.shared.database.ChatDB;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class MyFcmListenerService extends FirebaseMessagingService {
	String mUpdateStateIntent;
	String mIncomingChatStateIntent;

	@Override
	public void onNewToken(String token)
	{
		GCMUtils utils = new GCMUtils(this);
		utils.sendRegistrationIdToBackend(token);
	}


	@Override
	public void onMessageReceived(RemoteMessage message)
	{
		String from = message.getFrom();
		Map data = message.getData();

		JSONObject object = new JSONObject(data);

		try {

			IAppWrapper appWrapper = (IAppWrapper) this.getApplicationContext();
			mUpdateStateIntent = appWrapper.GetStateUpdatedIntentAction();
			mIncomingChatStateIntent = appWrapper.GetIncomingChatIntentAction();

			String GameID = null;
			String UserID = null;

			if (data.containsKey("GameId") && data.containsKey("UserId"))
			{
				 GameID = object.getString("GameId");
				 UserID = object.getString("UserId");
			}



			// it's a game update message
			if (GameID != null && UserID != null) {
				int gameId = Integer.valueOf(GameID);
				int userId = Integer.valueOf(UserID);

				// only do this if the gameid is in the current synclist

				SyncList gameList = SyncListDB.getInstance(this).GetList("GameList", userId);

				Log.i("NimLog", "Server message: Update Game: " + GameID + "User: " + UserID);

				if (gameList.GetList().contains(gameId)) {
					DoGetGameUpdate(gameId, userId);
				}
			}

			// game invite
			if (data.containsKey("inviteGameId")) {
				int toUserId = Integer.valueOf(object.getString("toUserId"));
				int gameId = Integer.valueOf(object.getString("inviteGameId"));
				int fromUserId = Integer.valueOf(object.getString("fromUserId"));
				int fromColour = Integer.valueOf(object.getString("fromColour"));
				String fromAlias = object.getString("fromAlias");
				String fromLogo = object.getString("fromLogo");

				DoGameInvite(fromUserId, fromAlias, fromLogo, fromColour, gameId);
			}

			// it's an incoming chat message

			if (data.containsKey("messsage")) {
				String message2 = object.getString("messsage");
				int toUserId = Integer.valueOf(object.getString("toUserId"));
				int toGameId = Integer.valueOf(object.getString("toGameId"));
				int fromUserId = Integer.valueOf(object.getString("fromUserId"));
				int fromColour = Integer.valueOf(object.getString("fromColour"));
				String fromLogo = object.getString("fromLogo");

				DoChatMessage(fromUserId, toUserId, toGameId, fromColour, fromLogo, message2);
			}

		} catch (Exception e)
		{
			int k = 1;

		}
	               


	    }
	    
	    private void DoGameInvite(int fromUserId, String fromAlias, String fromLogo, int fromColour, int gameId)
	    {
	    	NotificationUtils notifier = new NotificationUtils();
	    	notifier.DoInvite(this, fromUserId, fromAlias, fromLogo, fromColour, gameId);
	    	
	    }
	    

		
		private void NotifyIncomingChat(int fromUserId, int toUserId, int toGameId, int fromColour, String fromLogo, long chatId, String message)
		{
			Intent intent = new Intent(mIncomingChatStateIntent);
			intent.putExtra("toGameId", toGameId);
			intent.putExtra("toUserId", toUserId);
			intent.putExtra("fromUserId", fromUserId);
			intent.putExtra("message", message);
			intent.putExtra("fromColour", fromColour);
			intent.putExtra("fromLogo", fromLogo);
			intent.putExtra("chatId", (int)chatId);
			Log.i("Tact2", "GCMIntentService sending in incoming chat broadcast");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}
		
		void DoChatMessage(int fromUserId, int toUserId, int toGameId, int fromColour, String fromLogo, String message)
		{
			
			long chatId = ChatDB.getInstance(this).AddChatMessage(fromUserId, toUserId, toGameId, fromColour, fromLogo, message);
			
			NotifyIncomingChat(fromUserId, toUserId, toGameId, fromColour, fromLogo, chatId, message);
		}

	    void DoGetGameUpdate(int GameId, int UserId)
	    {
	    	UpdateQueue queue = new UpdateQueue(this,"GetGameUpdate", UserId);
			queue.AddGameIDToQueue(GameId);

			UpdateQueuePump pump = new UpdateQueuePump(this);
			pump.PumpQueueNow(UserId);
	    }
	    
	   

}
