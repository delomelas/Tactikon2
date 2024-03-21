package uk.co.eidolon.shared.network;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;

import Core.IState;
import Core.IState.GameStatus;
import Network.SyncList;
import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.activities.GameActivity;
import uk.co.eidolon.shared.activities.JoinGameActivity;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class NotificationUtils
{
	public void IncLoadingStack(Context context)
	{
		Intent intent = new Intent("uk.co.eidolon.gamelib.LOADING_STACK");
		intent.putExtra("loading", true);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	public void DecLoadingStack(Context context)
	{
		Intent intent = new Intent("uk.co.eidolon.gamelib.LOADING_STACK");
		intent.putExtra("finished", true);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	public void ClearNotification(Context context, int GameId)
	{
		NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(GameId);
	}
	
	public void DoInvite(Context context, int fromUserId, String fromAlias, String fromLogo, int fromColour, int gameId)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
		// if the user has friends-only invites enabled, don't do anything
		boolean friendsOnly = prefs.getBoolean("friendsOnlyInvites", false);
		if (friendsOnly == true)
		{
			SyncList friends = SyncListDB.getInstance(context).GetList("FriendList", (int)appWrapper.GetUserId());
			if (friends.GetList().contains(fromUserId) == false) return;
		}
		
		String title = "Tactikon Game Invite";
		String subText = fromAlias + " has invited you to join a game.";
		
		String strRingtonePreference = prefs.getString("notificationSound", "DEFAULT_SOUND");
		
		Uri alarmSound = Uri.parse(strRingtonePreference);
		
		NotificationCompat.Builder mBuilder = null;
		NotificationCompat.BigPictureStyle mPictureBuilder = null;
		
		mBuilder = new NotificationCompat.Builder(context)
		     .setSmallIcon(R.drawable.ic_stat_notification)
		     .setAutoCancel(true)
		     .setContentTitle(title)
		     .setContentText(subText)
		     .setPriority(1)
		     .setSound(alarmSound);
		
		Intent resultIntent = new Intent(context.getApplicationContext(), JoinGameActivity.class);
		resultIntent.putExtra("GameID",gameId);
		resultIntent.putExtra("fromInvite",  true);
		
		//mBuilder = mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignore", null);
		

		///mBuilder = mBuilder.addAction
		
		// Creates an explicit intent for an Activity in your app
				

				// The stack builder object will contain an artificial back stack for the
				// started Activity.
				// This ensures that navigating backward from the Activity leads out of
				// your application to the Home screen.
				TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
				// Adds the Intent that starts the Activity to the top of the stack
				stackBuilder.addParentStack(JoinGameActivity.class);
				stackBuilder.addNextIntent(resultIntent);
				
				PendingIntent resultPendingIntent =
				        stackBuilder.getPendingIntent(
				            0,
				            PendingIntent.FLAG_UPDATE_CURRENT
				        );
				resultPendingIntent.cancel();
				resultPendingIntent =
				        stackBuilder.getPendingIntent(
				            gameId,
				            PendingIntent.FLAG_UPDATE_CURRENT
				        );
				
				mBuilder.setContentIntent(resultPendingIntent);
				
				mBuilder = mBuilder.addAction(android.R.drawable.ic_menu_add, "Join Game...", resultPendingIntent);
				
				Intent blockIntent = new Intent();
				blockIntent.setAction(appWrapper.GetServerSyncIntentAction());
				blockIntent.putExtra("blockUserId",fromUserId);
				blockIntent.putExtra("GameID", gameId);
				
				PendingIntent blockPendingIntent = 
						PendingIntent.getService(context.getApplicationContext(), 0, blockIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				blockPendingIntent.cancel();
				blockPendingIntent = 
						PendingIntent.getService(context.getApplicationContext(), fromUserId, blockIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder = mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Block " + fromAlias, blockPendingIntent);
		
		boolean lights = prefs.getBoolean("blinkenlights", false);
		if (lights == true)
		{
			mBuilder = mBuilder.setLights(0xffff0000, 500, 200);
		}
		
		NotificationManager mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		
		//if (Build.VERSION.SDK_INT >= 16)
		{
			Resources res = context.getResources();
			int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			
			Drawable bitmap = appWrapper.GetLogoStore().GetLogo(fromLogo);
			
			//bitmap.setColorFilter(fromColour, Mode.MULTIPLY);
			//BitmapDrawable bmpDrawable = (BitmapDrawable)bitmap;
			Bitmap scaledBitmap = Bitmap.createBitmap(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(scaledBitmap);
			canvas.drawColor(fromColour);
			bitmap.setAlpha(255);
			
			bitmap.setBounds(0, 0, bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight());
			bitmap.draw(canvas);
			
			Bitmap newScaled = Bitmap.createScaledBitmap(scaledBitmap, width, height, true);
			
			mBuilder = mBuilder.setLargeIcon(newScaled);
		}
		
			
		{
			mNotificationManager.notify(gameId, mBuilder.build());
		}
		
		

	}

	public void DoNotification(Context context, final IState oldState, final IState info, int GameId)
	{
		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
		
		if (info.GetPlayerToPlay() != appWrapper.GetUserId())
		{
			ClearNotification(context, GameId);
			return;
		}
		
		if (info.GetGameState() != GameStatus.InGame) return;
		
		// avoid notifying if the player number hasn't actually changed
		if (oldState != null && info.GetPlayerToPlay() == oldState.GetPlayerToPlay()) return;
		
		String title = appWrapper.GetNotificationTextTitle(info);
		String subText = appWrapper.GetNotificationTextSubtext(context, info);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String strRingtonePreference = prefs.getString("notificationSound", "DEFAULT_SOUND");
		
		Uri alarmSound = Uri.parse(strRingtonePreference);
		
		
		NotificationCompat.Builder mBuilder = null;
		NotificationCompat.BigPictureStyle mPictureBuilder = null;

		int notifyID = 1;
		String CHANNEL_ID = "tactikon_channel_01";// The id of the channel.

		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			/* Create or update. */

			NotificationChannel channel = new NotificationChannel("tactikon_channel_01",
					"Tactikon2 Turn Updates",
					NotificationManager.IMPORTANCE_DEFAULT);
			mNotificationManager.createNotificationChannel(channel);
		}

		mBuilder = new NotificationCompat.Builder(context)
		     .setSmallIcon(R.drawable.ic_stat_notification)
		     .setAutoCancel(true)
		     .setContentTitle(title)
		     .setContentText(subText)
		     .setPriority(1)
			 .setChannelId(CHANNEL_ID)
		     .setSound(alarmSound);
		
		// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context.getApplicationContext(), GameActivity.class);
				resultIntent.putExtra("GameID",GameId);

				// The stack builder object will contain an artificial back stack for the
				// started Activity.
				// This ensures that navigating backward from the Activity leads out of
				// your application to the Home screen.
				TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
				// Adds the back stack for the Intent (but not the Intent itself)
				stackBuilder.addParentStack(GameActivity.class);
				// Adds the Intent that starts the Activity to the top of the stack
				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent =
				        stackBuilder.getPendingIntent(
				            0,
				            PendingIntent.FLAG_UPDATE_CURRENT
				        );
				resultPendingIntent.cancel();
				resultPendingIntent =
				        stackBuilder.getPendingIntent(
				            GameId,
				            PendingIntent.FLAG_UPDATE_CURRENT
				        );
				
				mBuilder.setContentIntent(resultPendingIntent);
		
		

		
		boolean lights = prefs.getBoolean("blinkenlights", false);
		if (lights == true)
		{
			mBuilder = mBuilder.setLights(0xffff0000, 500, 200);
		}

		// mId allows you to update the notification later on.
		
		//if (Build.VERSION.SDK_INT >= 16)
		{
			Resources res = context.getResources();
			int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			
			
			Bitmap bitmap = appWrapper.GetStateImage(80, info, appWrapper.GetUserId());
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false); 
			mBuilder = mBuilder
			      .setLargeIcon(scaledBitmap);
		}
			
		
		{
			mNotificationManager.notify(GameId, mBuilder.build());
		}
		
		

	}
}
