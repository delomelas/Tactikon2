package uk.co.eidolon.shared.activities;
import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.utils.IAppWrapper;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;

public class NewGameActivity extends Activity
{
	Fragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_new_game);
		
		boolean bNetwork = this.getIntent().getExtras().containsKey("Network");
		
		IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
		mFragment = appWrapper.NewGameFragmentFactory(this, bNetwork);
		
		    FragmentManager fragmentManager = this.getFragmentManager();
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        fragmentTransaction.replace(android.R.id.content, mFragment);
	        fragmentTransaction.commit();
	    
	    View main = (View)this.findViewById(R.id.main);
	    main.requestLayout();
	    main.invalidate();
        
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		return true;
	}
	
}
