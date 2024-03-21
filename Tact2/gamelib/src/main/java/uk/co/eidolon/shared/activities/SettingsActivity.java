package uk.co.eidolon.shared.activities;

import uk.co.eidolon.gamelib.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class SettingsActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{	
		super.onCreate(savedInstanceState);
		
		//setContentView(R.layout.activity_search_results);
		
        getFragmentManager().beginTransaction()
        	.replace(android.R.id.content, new SettingsFragment())
        	.commit();

	}
		
}
