package uk.co.eidolon.shared.activities;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.ServerUploadQueue;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.UserProfileDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.database.UserProfileDB.IUpdateEvent;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.FriendListView;
import uk.co.eidolon.shared.views.GameListView;
import uk.co.eidolon.shared.views.IStateInfo;
import uk.co.eidolon.shared.views.FriendListView.FriendListAdapter;

import Core.IEvent;
import Core.IState;
import Core.IState.GameStatus;
import Core.PlayerInfo;
import Network.EventQueueItem;
import Network.SyncList;
import Network.UserInfo;
import Support.Preferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileFragment extends Fragment
{
	int mUserId;
	boolean destroyed = false;
	
	UserInfo userInfo;
	Map<Integer, Integer> profile;

	public static ProfileFragment getInstance(int userId)
	{
		ProfileFragment newFragment = new ProfileFragment();
		newFragment.mUserId = userId;
		
		return newFragment;
        
    }
	
	public static ProfileFragment newInstance(int userId)
	{
		ProfileFragment fragment = new ProfileFragment();
		
		fragment.mUserId = userId;
		
		return fragment;
        
    }
	
	public static int PROFILE_WINS = 0;
	public static int PROFILE_LOSSES = 1;
	public static int PROFILE_PLAYED = 2;
	public static int PROFILE_PLAYING = 3;
	public static int PROFILE_LONGEST = 4;
	
	public static int PROFILE_RANK = 5;
	public static int PROFILE_TOTAL_IN_RANK = 6;
	public static int PROFILE_POSITION_IN_RANK = 7;
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.profile_layout, container, false);
        
        IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
        
        TextView aliasText = (TextView)view.findViewById(R.id.alias_text);
        ImageView logoImage = (ImageView)view.findViewById(R.id.logo_image);
        TextView playedText = (TextView)view.findViewById(R.id.played_text);
        TextView playingText = (TextView)view.findViewById(R.id.playing_text);
        TextView winPercText = (TextView)view.findViewById(R.id.win_perc);
		TextView lossPercText = (TextView)view.findViewById(R.id.loss_perc);
		TextView winNumText = (TextView)view.findViewById(R.id.win_num);
		TextView lossNumText = (TextView)view.findViewById(R.id.loss_num);
		TextView winText = (TextView)view.findViewById(R.id.win_text);
		TextView lossText = (TextView)view.findViewById(R.id.loss_text);
        
        aliasText.setTypeface(appWrapper.GetEconFont());
        playedText.setTypeface(appWrapper.GetEconFont());
        playingText.setTypeface(appWrapper.GetEconFont());
        
        winPercText.setTypeface(appWrapper.GetEconFont());
        lossPercText.setTypeface(appWrapper.GetEconFont());
        winNumText.setTypeface(appWrapper.GetEconFont());
        lossNumText.setTypeface(appWrapper.GetEconFont());
        winText.setTypeface(appWrapper.GetEconFont());
        lossText.setTypeface(appWrapper.GetEconFont());
        
        TextView rankText = (TextView)view.findViewById(R.id.rank_title_text);
        TextView rankDescText = (TextView)view.findViewById(R.id.rank_desc_text);
        TextView rankPositionText = (TextView)view.findViewById(R.id.rank_position_text);
        rankText.setTypeface(appWrapper.GetEconFont());
        rankDescText.setTypeface(appWrapper.GetEconFont());
        rankPositionText.setTypeface(appWrapper.GetEconFont());
        
        int userId = mUserId;
        
        new UpdateProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, userId);
        
        return view;
	}
	
	public static String ordinal(int i)
	{
	    return i % 100 == 11 || i % 100 == 12 || i % 100 == 13 ? i + "th" : i + new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"}[i % 10];
	}
	
	@Override
	public void onHiddenChanged(boolean bHidden)
	{
		super.onHiddenChanged(bHidden);
		if (bHidden == false) Refresh();
	}
	
	void PopulateProfile(View view)
	{
		if (destroyed == true) return;
		if (getActivity() == null) return;
		IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();

		//final TreeMap<Integer, Integer> profile = UserProfileDB.getInstance(getActivity()).GetProfile(userId);
		
		//final UserInfo info = UserProfileDB.getInstance(getActivity()).GetUserInfo(userId);
		
		if (userInfo == null || profile == null) return;
		
		TextView aliasText = (TextView)view.findViewById(R.id.alias_text);
        ImageView logoImage = (ImageView)view.findViewById(R.id.logo_image);
        TextView playedText = (TextView)view.findViewById(R.id.played_text);
        TextView playingText = (TextView)view.findViewById(R.id.playing_text);
      
        
        if (aliasText == null) return;
        aliasText.setText(userInfo.alias);
        
        if (profile.containsKey(PROFILE_PLAYED))
        {
        	playedText.setText("Played: " + profile.get(PROFILE_PLAYED));
        } else
        {
        	playedText.setText("Played: ---");
        }
        if (profile.containsKey(PROFILE_PLAYING))
        {
        	playingText.setText("Playing: " + profile.get(PROFILE_PLAYING));
        } else
        {
        	playingText.setText("Playing: ---");
        }
        
        logoImage.setBackgroundColor(userInfo.colour);
		Drawable logoDrawable = appWrapper.GetLogoStore().GetLogo(userInfo.logo);
		if (logoDrawable != null)
		{
			logoImage.setImageDrawable(logoDrawable);
		}
		
		TextView winPercText = (TextView)view.findViewById(R.id.win_perc);
		TextView lossPercText = (TextView)view.findViewById(R.id.loss_perc);
		TextView winNumText = (TextView)view.findViewById(R.id.win_num);
		TextView lossNumText = (TextView)view.findViewById(R.id.loss_num);

		int wins = 0;
		int lost = 0;
		if (profile.containsKey(PROFILE_WINS) == true)
		{
			wins = profile.get(PROFILE_WINS);
			lost = profile.get(PROFILE_LOSSES);
		}
		
		winNumText.setText(String.valueOf(wins));
		lossNumText.setText(String.valueOf(lost));
		
		if (wins + lost == 0)
		{
			lossPercText.setText("0%");
			winPercText.setText("0%");
		} else
		{
			int winPerc = Math.round(100.0f * (float)wins/(float)(wins + lost));
			int lossPerc = Math.round(100.0f * (float)lost/(float)(wins + lost));
			lossPercText.setText(String.valueOf(lossPerc) + "%");
			winPercText.setText(String.valueOf(winPerc) + "%");
		}
			
		// draw the donut...
		
		final ImageView donutImage = (ImageView)view.findViewById(R.id.win_donut);
		if (donutImage == null) return;
		
		/*
		ViewTreeObserver viewTreeObserver = donutImage.getViewTreeObserver();
		
		viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
			@Override
			public void onGlobalLayout()
			{*/
				//int wins = 0;
				int losses = 0;
				if (profile.containsKey(PROFILE_WINS)) wins = profile.get(PROFILE_WINS);
				if (profile.containsKey(PROFILE_LOSSES)) losses = profile.get(PROFILE_LOSSES);
				
				if (userInfo != null)
					UpdateWinDonut(donutImage, wins, losses, userInfo.colour);
				
			//}});
		
		int rank = 0;
		int position = -1;
		int out_of = -1;
		
		if (profile.containsKey(PROFILE_RANK)) rank = profile.get(PROFILE_RANK);
		if (profile.containsKey(PROFILE_TOTAL_IN_RANK)) out_of = profile.get(PROFILE_TOTAL_IN_RANK);
		if (profile.containsKey(PROFILE_POSITION_IN_RANK)) position = profile.get(PROFILE_POSITION_IN_RANK);
		
		TextView rankText = (TextView)view.findViewById(R.id.rank_title_text);
        TextView rankDescText = (TextView)view.findViewById(R.id.rank_desc_text);
        TextView rankPositionText = (TextView)view.findViewById(R.id.rank_position_text);
		
		if (profile.containsKey(PROFILE_RANK) == false) // no rank information
		{
			rankPositionText.setText("");
		} 
		
		int imageResourceBackground = -1;
		int imageResourceColour = -1;
		String rankName = "";
		String desc = "";
		
		if (rank == 0)
		{
			rankName = "Private";
			desc = "Newly enlisted player";
			imageResourceBackground = R.drawable.private_background;
			imageResourceColour = R.drawable.private_colour;
		} else if (rank == 1)
		{
			rankName = "Corporal";
			desc = "Played more than 10 games";
			imageResourceBackground = R.drawable.corporal_background;
			imageResourceColour = R.drawable.corporal_colour;
		} else if (rank == 2)
		{
			rankName = "Sergeant";
			desc = "Played more than 20 games";
			imageResourceBackground = R.drawable.sergeant_background;
			imageResourceColour = R.drawable.sergeant_colour;
		} else if (rank == 3)
		{
			rankName = "Lieutenant";
			desc = "Played more than 40 games";
			imageResourceBackground = R.drawable.lieutenant_background;
			imageResourceColour = R.drawable.lieutenant_colour;
		} else if (rank == 4)
		{
			rankName = "Major";
			desc = "Played more than 80 games";
			imageResourceBackground = R.drawable.major_background;
			imageResourceColour = R.drawable.major_colour;
		} else if (rank == 5)
		{
			rankName = "Colonel";
			desc = "Played more than 160 games";
			imageResourceBackground = R.drawable.colonel_background;
			imageResourceColour = R.drawable.colonel_colour;
		} else if (rank == 6)
		{
			rankName = "Brigadier";
			desc = "Played more than 320 games";
			imageResourceBackground = R.drawable.brigadier_background;
			imageResourceColour = R.drawable.brigadier_colour;
		} else if(rank == 7)
		{
			rankName = "General";
			desc = "Played more than 640 games";
			imageResourceBackground = R.drawable.general_background;
			imageResourceColour = R.drawable.general_colour;
		} else if (rank == 8)
		{
			rankName = "Field Marshal";
			desc = "Played more than 1000 games";
			imageResourceBackground = R.drawable.field_background;
			imageResourceColour = R.drawable.field_colour;
		}
		
		ImageView rankBackgroundImage = (ImageView)view.findViewById(R.id.rank_image_background);
		ImageView rankBackgroundColour = (ImageView)view.findViewById(R.id.rank_image_colour);
		rankBackgroundImage.setImageResource(imageResourceBackground);
		rankBackgroundColour.setImageResource(imageResourceColour);
		rankBackgroundColour.setColorFilter(userInfo.colour, Mode.MULTIPLY);
		
		rankText.setText("Rank: " + rankName);
		rankDescText.setText("(" + desc + ")");
		
		if (position == -1)
		{
			rankPositionText.setText("");
		} else
		{
			String pos = "Rated " + ordinal(position) + " out of " + out_of + " players with the same rank";
			rankPositionText.setText(pos);
		}
		
	}
	
	void DrawPieSlice(int startAngle, int sweepAngle, Canvas canvas, int colour)
	{
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		RectF rectOutside = new RectF((width / 2) - (height/ 2) + 5,  5, (width / 2) + (height / 2) - 5 , height  - 5);
		RectF rectInside = new RectF((width / 2) - (height / 5), height / 2 - (height / 5) , (width / 2) + (height / 5) , height / 2 + (height / 5));
		
		Paint strokePaint = new Paint();
		strokePaint.setColor(colour);
		strokePaint.setStrokeWidth(3);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setStrokeCap(Cap.BUTT);
		strokePaint.setAntiAlias(true);
		
		Paint fillPaint = new Paint();
		fillPaint.setColor(colour);
		fillPaint.setStyle(Paint.Style.FILL);
		fillPaint.setStrokeCap(Cap.BUTT);
		fillPaint.setAntiAlias(true);
		
		Path path = new Path();
		
		path.addArc(rectOutside, startAngle, sweepAngle);
		path.arcTo(rectInside, startAngle + sweepAngle, -sweepAngle);
		path.close();
		
		canvas.drawPath(path, strokePaint);
		canvas.drawPath(path,  fillPaint);
        
	}
	
	//static Bitmap winLossDonut = null;
	
	void UpdateWinDonut(ImageView donutImage, int wins, int losses, int colour)
	{
		if (destroyed == true) return;
		int width = donutImage.getMeasuredWidth();
		int height = donutImage.getMeasuredHeight();
		if (width == 0 || height == 0) return;
		Drawable d = donutImage.getDrawable();
		Bitmap winLossDonut = null;
		if (d != null)
		{
			if (d instanceof BitmapDrawable)
			{
				winLossDonut = ((BitmapDrawable)d).getBitmap();
			}
		}
		
		if (winLossDonut == null) winLossDonut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(winLossDonut);
                
        Paint paint = new Paint();
		paint.setColor(colour);
		paint.setStrokeWidth(height / 3);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeCap(Cap.BUTT);
		paint.setAntiAlias(true);
		
		
		Path path = new Path();
		Paint blackPaint = new Paint();
		blackPaint.setColor(Color.BLACK);
		blackPaint.setStyle(Paint.Style.FILL);
		blackPaint.setAntiAlias(true);
		
        path.addCircle(width / 2, height / 2, height / 2, Direction.CW);
        canvas.drawPath(path, blackPaint);
		
        if (losses + wins == 0)
        {
        // doInBackground
        } else
        {
        	float winsAngle = ((float)wins / (float)(losses + wins)) * 360;
        	float lossesAngle = ((float)losses / (float)(losses + wins)) * 360;
        	
        	DrawPieSlice(270, Math.round(winsAngle), canvas, colour);
        	DrawPieSlice(270 + Math.round(winsAngle), Math.round(lossesAngle), canvas, Color.rgb(92,92,92));
        }
        
        donutImage.setImageBitmap(winLossDonut);
        //winLossDonut.recycle();
        
         
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		//setRetainInstance(true);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
	    super.onCreateContextMenu(menu, v, menuInfo);
	    
	}
	
	public void Refresh()
	{
		Log.i("Tact2", "Refreshing profile view");
		if (getActivity() == null) return;
		//if (mUserId == -1)
		{
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			if (mUserId == -1) 
			{
				mUserId = (int)appWrapper.GetUserId();
			}
			if (mUserId != -1)
			{
				if (mUserId == appWrapper.GetUserId())
				{
					SharedPreferences prefs = getActivity().getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);
					long millis = System.currentTimeMillis();
					long time = millis / 1000;
					long lastUpdate = prefs.getLong("LastMyProfileUpdate", 0);
					if (time > lastUpdate + 60)
					{
						new UpdateProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUserId);
						Editor editor = prefs.edit();
						editor.putLong("LastMyProfileUpdate", time);
						editor.commit();
					}
					
				} else
				{
					new UpdateProfileTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUserId);
				}
				
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
	    
	 
	    return super.onContextItemSelected(item);
	}
	
		
	public void onResume()
	{
		super.onResume();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		System.gc();
        Runtime.getRuntime().gc();
		
	}
	
	 private void unbindDrawables(View view) {
	        System.gc();
	        Runtime.getRuntime().gc();
	        if (view.getBackground() != null) {
	        view.getBackground().setCallback(null);
	        }
	        if (view instanceof ViewGroup) {
	            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
	            unbindDrawables(((ViewGroup) view).getChildAt(i));
	            }
	        ((ViewGroup) view).removeAllViews();
	        }
	    }
	
	@Override
	public void onDestroy()
	{
		destroyed = true;

		super.onDestroy();
		
		if (getView() != null) unbindDrawables(getView());
		
	}
	
	private class UpdateProfileTask extends AsyncTask<Integer, Integer, Integer>
	{
		UpdateProfileTask()
		{
			super();
		}
				
		@Override
		protected void onPreExecute()
		{
			
		}
		
		@Override
	    protected void onProgressUpdate(Integer... v)
		{
	        super.onProgressUpdate(v);
	        PopulateProfile(getView());
	    }
		
		@Override
		protected Integer doInBackground(final Integer... userId)
		{
			if (getActivity() == null) return 0;
			
			IUpdateEvent syncEvents = UserProfileDB.getInstance(getActivity()).new IUpdateEvent(){

				@Override
				public void onUpdated()
				{
					if (getView() == null) return;
					PopulateProfile(getView());
				}
				// Bitmap
				@Override
				public void onUpdatedBackground()
				{
					userInfo = UserProfileDB.getInstance(getActivity()).GetUserInfo(userId[0]);
					profile = UserProfileDB.getInstance(getActivity()).GetProfile(userId[0]);
				}};
			
			userInfo = UserProfileDB.getInstance(getActivity()).GetUserInfo(userId[0]);
			profile = UserProfileDB.getInstance(getActivity()).GetProfile(userId[0]);
			publishProgress(userId); // first update with the old data
			
			UserProfileDB.getInstance(getActivity()).GetFromServer(getActivity(), userId[0], syncEvents);
				
			return 0;
		}
		
	}
	
}
