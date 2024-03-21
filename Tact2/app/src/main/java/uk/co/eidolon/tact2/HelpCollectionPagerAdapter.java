package uk.co.eidolon.tact2;


import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


//Since this is an object collection, use a FragmentStatePagerAdapter,
//and NOT a FragmentPagerAdapter.
public class HelpCollectionPagerAdapter extends FragmentStatePagerAdapter
{
	static ArrayList<Integer> pages = new ArrayList<Integer>();
	ArrayList<String> pageTitles = new ArrayList<String>();
 public HelpCollectionPagerAdapter(FragmentManager fm)
 {
     super(fm);
     pages.clear();
     pageTitles.clear();
     pages.add(R.layout.help_introduction); pageTitles.add("INTRODUCTION");
     pages.add(R.layout.help_units); pageTitles.add("UNITS");
     pages.add(R.layout.help_cities); pageTitles.add("CITIES");
     pages.add(R.layout.help_combat); pageTitles.add("COMBAT");
     pages.add(R.layout.help_terrain); pageTitles.add("TERRAIN");
     pages.add(R.layout.help_multiplayer); pageTitles.add("MULTIPLAYER");
     pages.add(R.layout.help_fog_of_war); pageTitles.add("FOG OF WAR");
     pages.add(R.layout.help_highlights); pageTitles.add("GAME ARENA");
     pages.add(R.layout.help_aircraft); pageTitles.add("AIRCRAFT AND FUEL");
     pages.add(R.layout.help_friends); pageTitles.add("FRIENDS");
     pages.add(R.layout.help_chat); pageTitles.add("CHAT");
 }

 @Override
 public Fragment getItem(int i) {
     Fragment fragment = new DemoObjectFragment();
     Bundle args = new Bundle();
     // Our object is just an integer :-P
     args.putInt(DemoObjectFragment.ARG_OBJECT, i );
     fragment.setArguments(args);
     return fragment;
 }

 @Override
 public int getCount() {
     return pages.size();
 }

 @Override
 public CharSequence getPageTitle(int position)
 {
	 return pageTitles.get(position);
 }
 
//Instances of this class are fragments representing a single
//object in our collection.
public static class DemoObjectFragment extends Fragment {
public static final String ARG_OBJECT = "object";

@Override
public View onCreateView(LayoutInflater inflater,
        ViewGroup container, Bundle savedInstanceState) {
    // The last two arguments ensure LayoutParams are inflated
    // properly.
    
    Bundle args = getArguments();
    //((TextView) rootView.findViewById(android.R.id.text1)).setText(
      //      Integer.toString(args.getInt(ARG_OBJECT)));
    int pageNum = args.getInt(ARG_OBJECT);
    View rootView = inflater.inflate(
            pages.get(pageNum), container, false);
    return rootView;
}
}
}

