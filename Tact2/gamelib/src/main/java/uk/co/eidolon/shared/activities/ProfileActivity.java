package uk.co.eidolon.shared.activities;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.network.GameNetworkListener;
import uk.co.eidolon.shared.network.NotificationUtils;
import uk.co.eidolon.shared.utils.IAppWrapper;
import Core.EventInjector;
import Core.IState;
import Core.StateEngine;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ProfileActivity extends Activity
{
	int mUserId;
	
	Fragment mFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.profile_activity);
		
		IAppWrapper appWrapper = (IAppWrapper) getApplicationContext();
		
		mUserId =(int)appWrapper.GetUserId();
		
		Bundle b = this.getIntent().getExtras();
        if (b != null)
        {
        	mUserId = b.getInt("UserID");
        }
		
	    FragmentManager fragmentManager = this.getFragmentManager();
	    if (mFragment == null)
	    {
	    	mFragment = fragmentManager.findFragmentByTag("ProfileFragmemt");
	    	if (mFragment == null)
	    	{
	    		mFragment = ProfileFragment.newInstance(mUserId);
	    		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	    		fragmentTransaction.add(android.R.id.content, mFragment, "ProfileFragment");
	    		fragmentTransaction.commit();
	    	}
	    }
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		
	}
	
	@Override
	protected void onResume()
	{		
		super.onResume();
		
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		
		
	}
	
}
