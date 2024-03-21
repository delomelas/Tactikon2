package uk.co.eidolon.tact2;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

public class HelpActivity extends FragmentActivity
{
	HelpCollectionPagerAdapter mHelpCollectionPagerAdapter;
    ViewPager mViewPager;

	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.help_activity);
		
		// ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mHelpCollectionPagerAdapter =
                new HelpCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mHelpCollectionPagerAdapter);

	}
}
