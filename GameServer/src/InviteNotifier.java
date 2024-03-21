import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import Core.IState;
import Core.PlayerInfo;
import Network.SyncList;
import Network.UserInfo;


public class InviteNotifier
{
	public class InviteResult
	{
		boolean bSent = false;
	}
	
	public InviteResult SendInvite(int toUserId, int toGameId, int fromUserId, String appName)
	{
		InviteResult result = new InviteResult();
		int fromColour = 0;
		String fromLogo = "";
		String fromAlias = "";
		
		UserInfo senderInfo = DBStuff.getInstance().GetUserInfo(fromUserId);
		
		if (senderInfo == null)
		{
			System.out.println("[Invite]: Error getitng getting info");
			return result;
		}
		fromColour = senderInfo.colour;
		fromLogo = senderInfo.logo;
		fromAlias = senderInfo.alias;
		
		// now build a GCM message
		JSONObject gcmMessage = new JSONObject();

		gcmMessage.put("toUserId", String.valueOf(toUserId));
		gcmMessage.put("inviteGameId", String.valueOf(toGameId));
		gcmMessage.put("fromUserId", String.valueOf(fromUserId));
		gcmMessage.put("fromAlias", fromAlias);
		gcmMessage.put("fromLogo", fromLogo);
		gcmMessage.put("fromColour", String.valueOf(fromColour));

		
		// collect the GCM reg ids of the users we're sending to...
		String RegId = DBStuff.getInstance().GetGCMRegId(toUserId);
		ArrayList<String> gcmIDs = new ArrayList<String>();
		if (RegId.length() > 0 && !RegId.contains("NotRegistered-"))
		{
			gcmIDs.add(RegId);
		}
		
		if (gcmIDs.size() == 0)
		{
			System.out.println("[Invite]: No GCM ids to send to for user " + toUserId);
			result.bSent = false;
			return result;
		}
		
		int fails = 0;
		
		try {
			fails = FCMHelper.sendMultipleDataMessages(gcmMessage, gcmIDs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (fails == 0)
		{
			result.bSent = true;
			System.out.println("[Invite] Invite sent successfully");
		} else
		{
			System.out.println("[Invite] Error sending invite to game " + toGameId);
			
		}
				
		return result;
	}
	
	
}
