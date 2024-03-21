package uk.co.eidolon.shared.activities;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.network.GameNetworkListener;
import uk.co.eidolon.shared.network.NotificationUtils;
import uk.co.eidolon.shared.utils.DetailsFragment;
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
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class GameActivity extends Activity
{
	public StateEngine mStateEngine;
	int mGameID = -1;
	long mUserID = -1;
	
	GameNetworkListener mNetworkListener;
	EventInjector mEventInjector;
	
	Fragment mFragment;
	
	ChatFragment mChatFragment;
	
	View mGameView;
	
	@Override
	protected void onStart()
	{
		super.onStart();
	
		IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
		
		FragmentManager fragmentManager = this.getFragmentManager();
	    if (fragmentManager.findFragmentByTag("GameFragment") == null)
	    {
	    	mFragment = (Fragment)appWrapper.GameFragmentFactory(mStateEngine, mGameID);
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        fragmentTransaction.add(android.R.id.content, mFragment, "GameFragment");
	        fragmentTransaction.commit();
	    } else
	    {
	    	DetailsFragment fragment = (DetailsFragment)fragmentManager.findFragmentByTag("GameFragment");
	    	fragment.SetEngineAndGameId(mStateEngine, mGameID);
	    }
	    
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.game_activity);
		
		Bundle b = this.getIntent().getExtras();
        if (b != null)
        {
        	mGameID = b.getInt("GameID");
        }
        
        IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
		
		mUserID = appWrapper.GetUserId();
        
		IState state = GameDatabase.getInstance(this).GetGame(mGameID, mUserID);
		if (state == null)
		{
			finish();
			return;
		}
		
		new NotificationUtils().ClearNotification(this, mGameID);
		
		mStateEngine = new StateEngine(state);
		
		mGameView = appWrapper.GameViewFactory(this, mStateEngine, mGameID);
        
        final FrameLayout frameLayout = (FrameLayout)findViewById(R.id.gameplayarea);
        final RelativeLayout.LayoutParams gridLayoutParams = new RelativeLayout.LayoutParams
        	(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT );
        frameLayout.removeAllViews();
        frameLayout.addView(mGameView, gridLayoutParams);
        
		mNetworkListener = new GameNetworkListener(this, mGameID, mUserID);
		mStateEngine.AddListener(mNetworkListener);
		
		mEventInjector = new EventInjector(mStateEngine);
		
		FragmentManager fragmentManager = this.getFragmentManager();
	    if (fragmentManager.findFragmentByTag("GameFragment") == null)
	    {
	    	mFragment = (Fragment)appWrapper.GameFragmentFactory(mStateEngine, mGameID);
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        fragmentTransaction.add(android.R.id.content, mFragment, "GameFragment");
	        fragmentTransaction.commit();
	    } else
	    {
	    	DetailsFragment fragment = (DetailsFragment)fragmentManager.findFragmentByTag("GameFragment");
	    	fragment.SetEngineAndGameId(mStateEngine, mGameID);
	    }
	    
	    
	    if (mGameID > 0)
	    {
	    	if (fragmentManager.findFragmentByTag("ChatFragment") == null)
	    	{
	    		mChatFragment = ChatFragment.getInstance();
	    		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	    		mChatFragment.SetGameId(mGameID);
	    		fragmentTransaction.add(android.R.id.content, mChatFragment, "ChatFragment");
	    		fragmentTransaction.commit();
	    	}
	    }

	    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
	    super.onSaveInstanceState(outState);

	    //Save the fragment's instanceces
	    if (mChatFragment != null)
	    {
	    	getFragmentManager().putFragment(outState, "ChatFragment", mChatFragment);
	    }
	}
	
	@Override 
	public void onRestoreInstanceState(Bundle inState)
	{
		mChatFragment = (ChatFragment) getFragmentManager().getFragment(inState,"myfragment");
	}
	
	@Override
	protected void onResume()
	{		
		super.onResume();
		
		final GLSurfaceView view = (GLSurfaceView)mGameView;
		if (view != null) view.onResume();
		
		
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		GLSurfaceView view = (GLSurfaceView)mGameView;
		if (view != null) view.onPause();
		
		while (mNetworkListener.EventWaiting())
		{
			mNetworkListener.PumpQueue();
		}
		
		IAppWrapper wrapper = (IAppWrapper)this.getApplication();
		Log.i("Tact2", "Flushing event queue...");
		
		wrapper.GetUploadQueue().FlushQueue();
		
		final IState state = mStateEngine.GetState();
		
		GameDatabase.getInstance(GameActivity.this).UpdateGame(mGameID, mUserID, state);
		
		
		IAppWrapper appWrapper = (IAppWrapper)this.getApplicationContext();
		String updateStatesIntent = appWrapper.GetStateUpdatedIntentAction();
		Intent intent = new Intent(updateStatesIntent);
		intent.putExtra("UserID", mUserID);
		intent.putExtra("GameID", mGameID);
		Log.i("Tact2", "GameActivity refreshing GameID" + mGameID);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		
		//thread.start();
		
	}
	
}
