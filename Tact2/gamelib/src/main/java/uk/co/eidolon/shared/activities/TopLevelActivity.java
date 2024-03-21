package uk.co.eidolon.shared.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.util.Map;
import java.util.TreeMap;

import Support.Preferences;
import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class TopLevelActivity extends Activity
{
	FragmentPagerAdapter mTopLevelCollectionPagerAdapter;
    ViewPager mViewPager;

    static int loadingStack = 0;
    
    GCMUtils gcmUtils;
    
    final static int FRAGMENT_GAMESLIST = 0;
    final static int FRAGMENT_PROFILE = 1;
    final static int FRAGMENT_FRIENDSLIST = 2;
    final static int FRAGMENT_BLOCKLIST = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.top_level_activity);
		
		// ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
		mTopLevelCollectionPagerAdapter = new MyPagerAdapter(getFragmentManager());
		
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTopLevelCollectionPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);
        //mViewPager.set
        
        mViewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageSelected(int arg0)
			{
				if (arg0 == FRAGMENT_FRIENDSLIST)
				{
					FriendListFragment fragment = (FriendListFragment)mFragments.get(FRAGMENT_FRIENDSLIST);
					if (fragment != null) fragment.Refresh();
				} else if(arg0 == FRAGMENT_PROFILE)
				{
					ProfileFragment fragment = (ProfileFragment)mFragments.get(FRAGMENT_PROFILE);
					if (fragment != null) fragment.Refresh();
				}else if(arg0 == FRAGMENT_BLOCKLIST)
				{
					FriendListFragment fragment = (FriendListFragment)mFragments.get(FRAGMENT_BLOCKLIST);
					if (fragment != null) fragment.Refresh();
				}
			}});
        
        gcmUtils = new GCMUtils(this);
        
        if (mReceiversRegistered == false)
		{
			mReceiversRegistered = true;
			LocalBroadcastManager.getInstance(this).registerReceiver(mIntentReceiver, new IntentFilter("uk.co.eidolon.gamelib.LOADING_STACK"));
		}
        
        
	}
	
	
	private boolean mReceiversRegistered = false;

	// Define a handler and a broadcast receiver
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
	{
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String mAction = intent.getAction();
			

		    if (mAction.equals("uk.co.eidolon.gamelib.LOADING_STACK"))
		    {
		    	Bundle b = intent.getExtras();
		    	if (b.containsKey("loading"))
		    	{
		    		loadingStack ++;
		    		
		    		if (loadingStack > 0) setProgressBarIndeterminateVisibility(Boolean.TRUE);
		    		invalidateOptionsMenu();
		    	}
		    	if (b.containsKey("finished"))
		    	{
		    		loadingStack --;
		    		if (loadingStack < 0) loadingStack = 0;

		    		if (loadingStack == 0)
		    			setProgressBarIndeterminateVisibility(Boolean.FALSE);
		    		invalidateOptionsMenu();
		    	}
		    }
		}
	};
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu)
	{
        //getMenuInflater().inflate(R.menu.toplevel_menu, menu);
        
        return super.onPrepareOptionsMenu(menu);
    }

	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.toplevel_menu, menu);
		
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		if (item.getItemId() == R.id.action_switch_account)
		{
        	Intent switchAccountIntent = new Intent(this, uk.co.eidolon.shared.activities.QueryAccountActivity.class);
        	startActivity(switchAccountIntent);
            return true;
		} else if (item.getItemId() == R.id.action_settings)
		{
			Intent settingsIntent = new Intent(this, uk.co.eidolon.shared.activities.SettingsActivity.class);
        	startActivity(settingsIntent);
        	return true;
		}
        
        return super.onOptionsItemSelected(item);
        
    }
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		// clear the loading notification, if it's there
		setProgressBarIndeterminateVisibility(Boolean.FALSE);
		loadingStack = 0;

		//Toast.makeText(this, "After over 6 years, Tactikon 2 multiplayer is ending now. Thanks to everyone for playing!",Toast.LENGTH_LONG).show();


		if (1 == 1)
		{
			SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);
			long millis = System.currentTimeMillis();
			long time = millis / 1000;
			long lastUpdate = prefs.getLong(Preferences.LASTGCMUPDATE, 0);
			
			if (time - lastUpdate > 60*30) // only do this once per hour at most
			{

				String tok = gcmUtils.getToken();

				gcmUtils.sendRegistrationIdToBackend(tok);
			}
		}
		
		invalidateOptionsMenu();
		
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);
		
		boolean displayedPopup = prefs.getBoolean("DisplayedPopup", false);
		if (displayedPopup == false)
		{
			Editor ed = prefs.edit();
			ed.putBoolean("DisplayedPopup", true);
			ed.commit();
			
			IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
			Activity popup = appWrapper.GetPopupActivity();
			Intent settingsIntent = new Intent(this, popup.getClass());
        	startActivity(settingsIntent);
		}
		
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if(mReceiversRegistered)
		{
		 	LocalBroadcastManager.getInstance(this).unregisterReceiver(mIntentReceiver);
		    mReceiversRegistered = false;
		}
	}
	
	Map<Integer, Fragment> mFragments = new TreeMap<Integer, Fragment>();
	
	public class MyPagerAdapter extends FragmentPagerAdapter
	{
	    private int NUM_ITEMS = 1;

	        public MyPagerAdapter(FragmentManager fragmentManager)
	        {
	            super(fragmentManager);
	        }

	        // Returns total number of pages
	        @Override
	        public int getCount()
	        {
	            return NUM_ITEMS;
	        }

	        // Returns the fragment to display for that page
	        @Override
	        public Fragment getItem(int position)
	        {
	        	IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
	            switch (position) {
	            case FRAGMENT_GAMESLIST: 
	                Fragment fragment = GameListFragment.getInstance();
	                mFragments.put(FRAGMENT_GAMESLIST, fragment);
	                return fragment;
	            default:
	                return null;
	            }
	        }

	        // Returns the page title for the top indicator
	        @Override
	        public CharSequence getPageTitle(int position)
	        {
	            if (position == FRAGMENT_GAMESLIST)
	            {
	            	return "GAMES";
	            }
	            return "";
	        }

	    }
}
