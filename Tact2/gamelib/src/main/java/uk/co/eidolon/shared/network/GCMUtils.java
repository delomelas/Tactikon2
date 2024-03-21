package uk.co.eidolon.shared.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.iid.FirebaseInstanceId;

import Network.PackageResponse;
import Network.Packages.PackageUpdateGCM;
import Support.Preferences;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class GCMUtils
{

	Context mContext;
	
	String regid;
	
	String SENDER_ID = "240307466011";


	public GCMUtils(Context context)
	{
		mContext = context;
	}


    public boolean Register(Context context)
	{
		return true;
	}
	
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	
	private boolean checkPlayServices()
	{
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
	    if (resultCode != ConnectionResult.SUCCESS)
	    {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
	        {
	        	if (mContext instanceof Activity)
	        	{
	        		GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity) mContext,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        	}
	        } else
	        {
	            Log.i("NimLog", "This device is not supported.");
	        }
	        return false;
	    }
	    return true;
	}

	public String getToken()
	{
		String token = getRegistrationId(mContext);
		return token;
	}
	
	/**
	 * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
	 * or CCS to send messages to your app. Not needed for this demo since the
	 * device sends upstream messages to a server that echoes back the message
	 * using the 'from' address in the message.
	 */
	public void sendRegistrationIdToBackend(String token)
	{
		return;
		/*
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		long userId = appWrapper.GetUserId();
		if (userId == -1) return; // bail out if we're not a registered user
		
		final PackageUpdateGCM gcmPackage = new PackageUpdateGCM();
		
		gcmPackage.gcmRegId = token;
		if (gcmPackage.gcmRegId.length() == 0) return;
		
		gcmPackage.UserId = userId;
		PackageDelivery gcmSender = new PackageDelivery(mContext, gcmPackage, new ISendEvents(){

			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (gcmPackage.mReturnCode == PackageResponse.Success)
				{
					SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);
					SharedPreferences.Editor editor = prefs.edit();
					long millis = System.currentTimeMillis();
					long time = millis / 1000;
					editor.putLong(Preferences.LASTGCMUPDATE, time);
					editor.commit();
				}
			}

			@Override
			public void postExecuteBackground()
			{
				// TODO Auto-generated method stub
				
			}
		
		});
		gcmSender.Send();
		*/
	}
	
	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	public String getRegistrationId(Context context)
	{
		String refreshedToken = FirebaseInstanceId.getInstance().getToken();

		return refreshedToken;
	}

}
