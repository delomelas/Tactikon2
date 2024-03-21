import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import Core.IState;
import Core.PlayerInfo;
import Network.SyncList;


public class ChatNotifier
{
	class ChatResult
	{
		boolean bSent = false;
		boolean bGCMUpdateRequired = false;;
	}
	
	public ChatResult SendChatMessage(String message, int toUserId, int toGameId, int fromUserId, String appName)
	{
		ArrayList<Integer> userIdsToSend = new ArrayList<Integer>();
		ChatResult result = new ChatResult();
		int fromColour = 0;
		String fromLogo = "";
		IState state = DBStuff.getInstance().GetGame(toGameId, 999999, appName);
		if (toUserId == -1)
		{
			// get users from the game
			
			if (state == null)
			{
				return result;
			}
			
			userIdsToSend.addAll(state.GetPlayers());
						
		} else
		{
			userIdsToSend.add(toUserId);
		}
		
		// for each player in the send list, check the fromUser isn't in their blocklist
		ArrayList<Integer> removeSend = new ArrayList<Integer>();
		for (Integer sendingTo : userIdsToSend)
		{
			SyncList blockList = SyncListDB.getInstance().GetSyncList(sendingTo, "BlockList", appName);
			if (blockList.GetList().contains(fromUserId))
			{
				removeSend.add(sendingTo);
			}
		}
		userIdsToSend.removeAll(removeSend);
		
		PlayerInfo info = state.GetPlayerInfo(fromUserId);
		if (info == null) return result;
		
		fromColour = 255; // alpha
		fromColour = fromColour * 256 + info.r;
		fromColour = fromColour * 256 + info.g;
		fromColour = fromColour * 256 + info.b;
		
		fromLogo = info.logo;
		
		// don't send to the originating user
		// userIdsToSend.(fromUserId);
		int index = userIdsToSend.indexOf(fromUserId);
		if (index != -1)
		{
			userIdsToSend.remove(index);
		}
		
		// now build a GCM message
		
		JSONObject gcmMessage = new JSONObject();
		   
		   gcmMessage.put("toUserId", String.valueOf(toUserId));
		   gcmMessage.put("toGameId", String.valueOf(toGameId));
		   gcmMessage.put("fromUserId", String.valueOf(fromUserId));
		   gcmMessage.put("messsage", message);
		   gcmMessage.put("fromLogo", fromLogo);
		   gcmMessage.put("fromColour", String.valueOf(fromColour));
		   
		
		// collect the GCM reg ids of the users we're sending to...
		ArrayList<String> gcmIDs = new ArrayList<String>();
		for (Integer userId : userIdsToSend)
		{
			String RegId = DBStuff.getInstance().GetGCMRegId(userId);
			if (RegId.length() > 0 && !RegId.contains("NotRegistered-"))
			{
				gcmIDs.add(RegId);
			}
		}
		
		if (gcmIDs.size() == 0)
		{
			result.bSent = false;
			return result;
		}
		
		int fails = 0;
		try {
			fails = FCMHelper.sendMultipleDataMessages(gcmMessage,  gcmIDs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fails = 1;
			e.printStackTrace();
		}
		
		if (fails == 0)
		{
			result.bSent = true;
			System.out.println("[Chat] Chat message sent successfully");
		} else
		{
			System.out.println("[Chat] Error sending chat to game " + toGameId);
		}
				
		return result;
	}
	
	
}
