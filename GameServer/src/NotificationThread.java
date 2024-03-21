import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class NotificationThread extends Thread
{
	public void run() 
	{
		
		boolean bOncePerHour = false;
		
		int count = 0;
		
		while(1 == 1)
		{
			try
			{
				Thread.sleep(4000);
			} catch (InterruptedException e)
			{
				return;
			}
			
			if (count == 900)
			{
				count = 0;
				bOncePerHour = true;
			} else 
			{
				bOncePerHour = false;
			}
				
			
			count ++;
			
			NotificationQueueDB db = NotificationQueueDB.getInstance();
			ArrayList<NotificationQueueItem> items = db.GetQueueItems();
			
			ArrayList<NotificationQueueItem> processedItems = new ArrayList<NotificationQueueItem>();
			
			for (NotificationQueueItem item : items)
			{
				// Canvas
				// look up the token id for the user
				// send to google
				// add to the "remove" list
				
				String RegId = DBStuff.getInstance().GetGCMRegId(item.UserID);
				if (RegId == null)
				{
					//System.out.println("Null RegID for User = " + item.UserID);
					continue;
				}
				long nowTime = System.currentTimeMillis() / 1000;
				if (nowTime > item.Time + (60*60*24*7))
				{
					System.out.println("Clearing old item: Time: " + item.Time + " User: " + item.UserID + " Game: " + item.GameID);
					processedItems.add(item);
					continue;
				}
				
				if (RegId.compareTo("") == 0)
				{
					//System.out.println("No registration, unable to send notification.");
					continue;
				}
				
				if (RegId.contains("NotRegistered-") == true)
				{
					if (bOncePerHour == true)
					{
						System.out.println("Re-trying previously unregistered notification...");
						RegId = RegId.replace("NotRegistered-", "");
					} else
					{
						//System.out.println("No registration for user: " + item.UserID + " so not attempting to send notification now.");
						continue;
					}
				}
				
				System.out.println("Sending notification for Game: " + item.GameID + " to User: " + item.UserID);
				
				
				JSONObject message = new JSONObject();
				   
				message.put("GameId", String.valueOf(item.GameID));
				message.put("UserId", String.valueOf(item.UserID));
				   

				int result = 0;
				try
				{
					result = FCMHelper.sendPushNotification(message, RegId);
				} catch (IOException e)
				{
					DBStuff.getInstance().UpdateGCMRegId(item.UserID, "");
					e.printStackTrace();
					continue;
				}
							
				/*
				if (result.getCanonicalRegistrationId() != null)
				{
					System.out.println("Updating to canonical ID for user: " + item.UserID);
					DBStuff.getInstance().UpdateGCMRegId(item.UserID, result.getCanonicalRegistrationId());
				}
				*/
				
				processedItems.add(item);
			}
			
			db.ClearFromQueue(processedItems);
			
		}
	}
	
	
}
