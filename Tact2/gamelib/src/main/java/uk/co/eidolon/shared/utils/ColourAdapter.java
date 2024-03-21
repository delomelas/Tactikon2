package uk.co.eidolon.shared.utils;

import java.util.ArrayList;

import uk.co.eidolon.gamelib.R;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ColourAdapter extends PagerAdapter
{
	
	public class ColourItem
	{
		public int colour;
		public int r;
		public int g;
		public int b;
		public ColourItem(int r, int g, int b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
			
			this.colour = Color.rgb(r, g, b);
		}
	}
	
	 Context mContext;
	 
	 public ArrayList<ColourItem> mColours = new ArrayList<ColourItem>();
	 
	 public int FindColour(int colour)
	 {
		 for (int i = 0; i < mColours.size(); ++i)
		 {
			 if (mColours.get(i) == null) continue;
			 if (colour == mColours.get(i).colour)
			 {
				 return i;
			 }
		 }
		 return -1;
	 }
	 
	 public ColourAdapter(Context context)
	 {
		 mContext = context;
		 
		mColours.add(new ColourItem(192, 192, 192));
		 
		mColours.add(new ColourItem(255, 0, 0));
        mColours.add(new ColourItem(255, 128, 0));
        mColours.add(new ColourItem(255, 255, 0));
        //mColours.add(new ColourItem(170, 255, 0));
        mColours.add(new ColourItem(0, 255, 0));
        mColours.add(new ColourItem(0, 255, 128));
        mColours.add(new ColourItem(0, 255, 255));
        mColours.add(new ColourItem(0, 128, 255));
        mColours.add(new ColourItem(0, 0, 255));
        mColours.add(new ColourItem(128, 0, 255));
        mColours.add(new ColourItem(255, 0, 255));
        mColours.add(new ColourItem(255, 128, 128));
        mColours.add(new ColourItem(255, 0, 128));
        
	 }
	 
	 @Override
	 public Object instantiateItem(ViewGroup container, int position)
	 {
		 LayoutInflater inflater = (LayoutInflater) mContext
	                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		 
		 View view = inflater.inflate(R.layout.colour_item, null);
		 ColourItem item = mColours.get(position);
		 if (item == null) return view;
		 view.setBackgroundColor(Color.rgb(item.r, item.g, item.b));
		 
		 container.addView(view);
		 return view;
	 }
	  
	 @Override
	 public void destroyItem(ViewGroup container, int position, Object object)
	 {
		 container.removeView((View)object);
	 }
	  
	 @Override
	 public int getCount()
	 {
		 return mColours.size();
	 }
	  
	 @Override
	 public boolean isViewFromObject(View view, Object object)
	 {
		 return (view == object);
	 }
	 
	 


	 }
