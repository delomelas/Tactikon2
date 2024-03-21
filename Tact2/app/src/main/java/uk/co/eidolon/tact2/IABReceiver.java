package uk.co.eidolon.tact2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class IABReceiver extends BroadcastReceiver
{
	
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Start the service, keeping the device awake while it is launching.
        final Bundle b = intent.getExtras();
        //Log.i("Tact2", "Got aibresponse");
		if (b != null)
		{
			if (b.getBoolean("iabresponse") == true)
			{
				SharedPreferences prefs = context.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		        SharedPreferences.Editor editor = prefs.edit();
		        editor.putBoolean("PURCHASE", true);
		        editor.commit();
		        
		  //      Log.i("Tact2", "Setting purchased = true");
			} else
			{
			//	Log.i("Tact2", "Notpurchased");
			}
		}
        //setResultCode(Activity.RESULT_OK);
    }
}
