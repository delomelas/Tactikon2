package uk.co.eidolon.tact2;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class TactikonGameListFragment extends Fragment
{
	public TactikonGameListFragment()
	{
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		/*
		SharedPreferences prefs = getActivity().getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
        boolean purchase = prefs.getBoolean("PURCHASE", false);
        
		Polljoy.getPoll(null,
		           0,
		           Polljoy.getSession(),
		           0,
		           purchase ? PJUserType.PJPayUser : PJUserType.PJNonPayUser,
		           null,
		           null);
		           */
	}
	
	@Override
	public void onResume()
	{
		super.onResume();

		DoneT2BillingSetup();
	}
	
	
	void DoJoinGame()
	{
		Intent searchGamesIntent = new Intent(getActivity(), uk.co.eidolon.shared.activities.SearchResultsActivity.class);
    	startActivity(searchGamesIntent);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.extra_main, menu);

		AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
		boolean bPurchased = appWrapper.getPurchaseState();

		/*
		boolean bUpgradeItemPresent = menu.findItem(R.id.pro_upgrade) != null;
		
		if (bPurchased && bUpgradeItemPresent)
		{
			menu.removeItem(R.id.pro_upgrade);
		}*/

		
	}
	


		
	void DoneT2BillingSetup()
	{

					SharedPreferences prefs = getActivity().getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
			        SharedPreferences.Editor editor = prefs.edit();
			        editor.putBoolean("PURCHASE", true);
			        editor.commit();

	}
	
	@Override
	public void onDestroy() {
	   super.onDestroy();
	   

	}
	
	@Override
    public void onPrepareOptionsMenu(Menu menu)
	{
        
		AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
		boolean bPurchased = appWrapper.getPurchaseState();

		/*
		boolean bUpgradeItemPresent = menu.findItem(R.id.pro_upgrade) != null;
		
		if (bPurchased && bUpgradeItemPresent)
		{
			menu.removeItem(R.id.pro_upgrade);
		}
		*/
		
        super.onPrepareOptionsMenu(menu);
    }
	
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		/*
		if (item.getItemId() == R.id.pro_upgrade)
		{
			Intent upgradeIntent = new Intent(getActivity(), uk.co.eidolon.tact2.UpgradeActivity.class);
        	startActivity(upgradeIntent);
            return true;
		}
		*/
		
		if (item.getItemId() == R.id.about)
		{
			Intent upgradeIntent = new Intent(getActivity(), uk.co.eidolon.tact2.AboutActivity.class);
        	startActivity(upgradeIntent);
            return true;
		}
		
		if (item.getItemId() == R.id.help)
		{
			Intent helpIntent = new Intent(getActivity(), uk.co.eidolon.tact2.HelpActivity.class);
        	startActivity(helpIntent);
            return true;
		}
        
        return super.onOptionsItemSelected(item);
        
    }

}
