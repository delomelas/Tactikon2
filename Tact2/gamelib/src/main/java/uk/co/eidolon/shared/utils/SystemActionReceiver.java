package uk.co.eidolon.shared.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import uk.co.eidolon.shared.network.GCMUtils;

public class SystemActionReceiver extends BroadcastReceiver
{
	String mUpdateReceiverIntent;
	Handler handler = new Handler();
	@Override
	public void onReceive(final Context arg0, Intent arg1)
	{
		
		IAppWrapper appWrapper = (IAppWrapper)arg0.getApplicationContext();
		mUpdateReceiverIntent = appWrapper.GetServerSyncIntentAction();
        
		if (arg1.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))
		{
			GCMUtils gcmUtils = new GCMUtils(arg0);
			gcmUtils.Register(arg0);
		}
		
		if (arg1.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Intent serviceIntent = new Intent();
			serviceIntent.setPackage("uk.co.eidolon.tact2)");
        	serviceIntent.setAction(mUpdateReceiverIntent);
        	serviceIntent.putExtra("updateAll", true);
        	serviceIntent.putExtra("UserID", appWrapper.GetUserId());
        	arg0.startService(serviceIntent);
		}
		
		if (arg1.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			NetworkInfo networkInfo = arg1.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if(networkInfo.isConnected())
			{
				Intent serviceIntent = new Intent();
				serviceIntent.setPackage("uk.co.eidolon.tact2)");
		    	serviceIntent.setAction(mUpdateReceiverIntent);
		    	serviceIntent.putExtra("updateAll", true);
		    	serviceIntent.putExtra("UserID", appWrapper.GetUserId());
		    	arg0.startService(serviceIntent);
		    	
		    	GCMUtils gcmUtils = new GCMUtils(arg0);
				gcmUtils.Register(arg0);
			}
		}
	}
}
