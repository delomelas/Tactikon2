package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Map.Entry;

import uk.co.eidolon.shared.utils.ColourAdapter;
import uk.co.eidolon.shared.utils.ColourAdapter.ColourItem;
import uk.co.eidolon.shared.utils.DetailsFragment;
import uk.co.eidolon.shared.utils.IAppWrapper;
import Core.EventInjector;
import Core.IEvent;
import Core.IEventListener;
import Core.IState;
import Core.PlayerInfo;
import Core.StateEngine;
import Tactikon.State.EventChangePlayerInfo;
import Tactikon.State.EventEndTurnWithPlayerId;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.PorterDuff.Mode;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLayoutChangeListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;


public class GameFragment extends Fragment implements DetailsFragment
{

	public GameFragment()
	{
	}
	
	StateEngine mEngine;
	EventInjector mInjector;
	EventListener mListener;
	
	class EventListener extends IEventListener
	{
		@Override
		public void HandleQueuedEvent(IEvent event, IState before, final IState after)
		{
			// initiate animation or whatever the IEvent is
			final TactikonState beforeState = (TactikonState)before;
			final TactikonState afterState = (TactikonState)after;
			if (event instanceof EventEndTurnWithPlayerId)
			{
				(getActivity()).runOnUiThread(new Runnable() {
			        public void run()
			        {
			        	UpdateEndTurnButton(afterState);
						if (afterState.bLocalGame == true)
						{
							long currentPlayer = beforeState.GetPlayerToPlay();
							long newPlayer = afterState.GetPlayerToPlay();
							if (newPlayer != currentPlayer && newPlayer < TactikonState.AI_PLAYER_START)
							{
								ShowPlayerReadyScreen(newPlayer);
							} else
							{
								ClearPlayerReadyScreen();
							}
						}
			        }
			    });
			}
		}
	}
	
	boolean bTurnEnded = false;
	
	
	public void SetEngineAndGameId(StateEngine engine, int GameId)
	{
		mEngine = engine;
		
		if (getActivity() != null)
		{
			AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
			appWrapper.SetCurrentStateEngine(engine);
		}
		
		mInjector = new EventInjector(engine);
		
		mListener = new EventListener();
		mListener.bAutoPump = true;
		
		
		engine.AddListener(mListener);
		
		bTurnEnded = false;
		
		UpdateEndTurnButton((TactikonState)engine.GetState());
	}
	
	@Override
	public
	void onDetach()
	{
		super.onDetach();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		bTurnEnded = false;
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		if (getActivity() == null) return;
		
		
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		
		
	}
	
	@Override
	public void onActivityCreated(Bundle saved)
	{
		super.onActivityCreated(saved);
		//if (mEngine == null) getActivity().dis
		TactikonState state = (TactikonState)mEngine.GetState();
		UpdateEndTurnButton(state);
	}
	
	long GetCurrentUserId()
	{
		IState state = mEngine.GetState();
		if (state == null) return -1;
		TactikonState tState = (TactikonState)state;
		
		if (getActivity() == null) return -1;
		IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		
		if (tState.bLocalGame == false) return appWrapper.GetUserId();
		
		if (tState.GetPlayerToPlay() < TactikonState.AI_PLAYER_START) return tState.GetPlayerToPlay();
		
		return -1;
	}
	
	boolean bShowEndTurn = false;
	
	void UpdateEndTurnButton(TactikonState state)
	{
		if (state.bLocalGame == true)
		{
			if (state.playerToPlay < TactikonState.AI_PLAYER_START)
			{
				bShowEndTurn = true;
			} else
			{
				bShowEndTurn = false;
			}
		} else
		{
			if (state.playerToPlay == GetCurrentUserId())
			{
				bShowEndTurn = true;
			} else
			{
				bShowEndTurn = false;
			}
		}
		
		if (getActivity() != null)
			getActivity().invalidateOptionsMenu();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		// Inflate the menu; this adds items to the action bar if it is present.
		if (menu.findItem(R.id.end_turn) == null)
			inflater.inflate(R.menu.game, menu);
		
		if (bShowEndTurn == false || bReadyScreenShown)
		{
			menu.removeItem(R.id.end_turn);
		}
	}
	
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which)
	    {
	    	EndTurn();
	    }
	};
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		if (item.getItemId() == R.id.end_turn && bTurnEnded == false)
		{
			if (bReadyScreenShown == true) return true;
			
			// if show confirm dialog, do end turn, otherwise cancel
			final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			
			boolean confirmEndTurn = prefs.getBoolean("confirmEndTurn", true);
			if (confirmEndTurn == false)
			{
				EndTurn();
			} else
			{
				AlertDialog alertDialog = builder.setTitle("End turn: Are you sure?")
			        .setMultiChoiceItems(R.array.do_not_show_again_array, null, new OnMultiChoiceClickListener() {

			            @Override
			            public void onClick(DialogInterface dialog, int which, boolean isChecked)
			            {
			            	SharedPreferences.Editor editor = prefs.edit();
			        		editor.putBoolean("confirmEndTurn", !isChecked);
			        		editor.commit();
			            }
			        })
			        .setPositiveButton("End Turn", dialogClickListener)
			        .setNegativeButton("Not Yet", null).show();
			
			}
			//
            return true;
		}
		
		if (item.getItemId() == R.id.help)
		{
			Intent helpIntent = new Intent(getActivity(), uk.co.eidolon.tact2.HelpActivity.class);
        	startActivity(helpIntent);
            return true;
		}
		
		if (item.getItemId() == R.id.powergraph)
		{
			DisplayPowerGraph();
			return true;
		}
        
        return super.onOptionsItemSelected(item);
        
    }
	
	void DrawPowerGraph(ImageView graphImageView, int left, int top, int right, int bottom)
	{
		TactikonState state = (TactikonState)mEngine.GetState();
		if (state == null) return;
		
		int x = graphImageView.getMeasuredWidth();
		int y = graphImageView.getMeasuredHeight();
		
		Bitmap miniMap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
		
		BitmapDrawable drawable = new BitmapDrawable(miniMap);
		graphImageView.setImageDrawable(drawable);
		Canvas canvas = new Canvas(miniMap);
		
		
		int maxValues = 0;
		int minValue = Integer.MAX_VALUE;
		int maxValue = Integer.MIN_VALUE;
		
		for (Entry<Integer, ArrayList<Integer>> entry : state.powerGraph.entrySet())
		{
			ArrayList<Integer> values = entry.getValue();
			if (values.size() > maxValues) maxValues = values.size();
			
			for (Integer value : values)
			{
				if (value < minValue) minValue = value;
				if (value > maxValue) maxValue = value;
			}
		}
		
		if (maxValue < 20) maxValue = 20;
		if (minValue < 00) minValue = 00;
		
		
		float x_tick = (float)(x - 10) / (float)maxValues;
		float y_tick = (float)(y - 10) / (float)(maxValue - minValue);
		
		float horizDif = ((float)maxValue - (float)minValue) / 6.0f;
		
		// draw horizontal hints
		for (int i = 0; i < 6; ++i)
		{
			Paint paint = new Paint();
			paint.setColor(Color.rgb(255, 255, 255));
			paint.setStrokeWidth(5);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeJoin(Join.ROUND);
			paint.setStrokeCap(Cap.ROUND);
			
			canvas.drawLine(0, i * horizDif * y_tick + 20, x, i * horizDif * y_tick + 20, paint);
		}
		float player = 0;
		for (Entry<Integer, ArrayList<Integer>> entry : state.powerGraph.entrySet())
		{
			ArrayList<Integer> values = entry.getValue();
			
			PlayerInfo info = state.GetPlayerInfo(entry.getKey());
			
			Paint paint = new Paint();
			paint.setColor(Color.rgb(info.r, info.g,  info.b));
			paint.setStrokeWidth(6);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeJoin(Join.ROUND);
			paint.setStrokeCap(Cap.ROUND);
			paint.setAntiAlias(true);
			
			Paint paint2 = new Paint();
			paint2.setColor(Color.rgb(info.r/3, info.g/3,  info.b/3));
			paint2.setStrokeWidth(7f);
			paint2.setStyle(Paint.Style.STROKE);
			paint2.setStrokeJoin(Join.ROUND);
			paint2.setStrokeCap(Cap.ROUND);
			paint2.setAntiAlias(true);
			//paint.set
			
			if (values.size() == 0) continue;
			
			int prevValue = values.get(0);
			int x_axis = 1;
			Path path = new Path();
			for (Integer value : values)
			{
				canvas.drawPoint((x_axis) * x_tick + 5, (y-10)- ((value - minValue)) * y_tick + player, paint);
				// draw a line from (x-1, prevValue) to (x, value)
				path.moveTo((x_axis-1) * x_tick + 5, (y-10)- ((prevValue - minValue)) * y_tick + player);
				path.lineTo((x_axis*x_tick) + 5, (y-10)- (value - minValue) * y_tick + player);
				
				
				prevValue = value;
				x_axis = x_axis + 1;
			}
			player = player + 0.5f;
			canvas.drawPath(path, paint2);
			canvas.drawPath(path, paint);
		}
	}
	
	void DisplayPowerGraph()
	{
		TactikonState state = (TactikonState)mEngine.GetState();
		if (state == null) return;
		
		
		AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
		
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout hudLayer = (RelativeLayout)getActivity().findViewById(R.id.hud_layer);
		
		// remove the existing powergraph if there's already one there
		LinearLayout oldGraphLayout = (LinearLayout)getActivity().findViewById(R.id.graph_root);
		if (oldGraphLayout != null)
		{
			hudLayer.removeView(oldGraphLayout);
		}
		
		LinearLayout powerGraphLayout = (LinearLayout)inflater.inflate(R.layout.powergraph, null);
		
		LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		//params.addRule(RelativeLayout.)
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		powerGraphLayout.setLayoutParams(params);
		
		TextView winCondition = (TextView)powerGraphLayout.findViewById(R.id.win_condition);
		winCondition.setTypeface(appWrapper.econFont);
		if (state.winCondition == WinCondition.Annihilate)
		{
			winCondition.setText("Annihilate to win");
		} else if (state.winCondition == WinCondition.CaptureAllBases)
		{
			winCondition.setText("Capture all cities");
		} else if (state.winCondition == WinCondition.CaptureHQ)
		{
			winCondition.setText("Capture the HQs");
		} else
		{
			winCondition.setText("");
		}
		
		TextView turnCount = (TextView)powerGraphLayout.findViewById(R.id.turn_count);
		turnCount.setTypeface(appWrapper.econFont);
		// find the number of turns the max player has done
		int max = 0;
		for (Entry<Integer, ArrayList<Integer>> entry : state.powerGraph.entrySet())
		{
			if (entry.getValue().size() > max) max = entry.getValue().size();
		}
		if (max == 0)
		{
			turnCount.setText("      (v" + state.mGameVersion + ")");
		} else
		{
			turnCount.setText("Turns: " + Integer.toString(max) + "      (v" + state.mGameVersion + ")");
		}
		
		for (int i = 0; i < state.GetPlayers().size(); ++i)
		{
			int playerId = state.GetPlayers().get(i);
			PlayerInfo info = state.GetPlayerInfo(playerId);
			LinearLayout playerInfoView = (LinearLayout)inflater.inflate(R.layout.graph_player_name, null);
			
			FrameLayout logoImageViewHolder = (FrameLayout)playerInfoView.findViewById(R.id.name_logo_holder);
			Drawable drawable = appWrapper.GetLogoStore().GetLogo(info.logo);
			ImageView imageView = new ImageView(getActivity().getApplicationContext());
			if (drawable != null)
			{
				imageView.setAdjustViewBounds(false);
				imageView.setImageDrawable(drawable);
				imageView.setBackgroundColor(Color.rgb(info.r, info.g, info.b));
			}
			
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			logoImageViewHolder.addView(imageView);
			
			if (state.IsPlayerAlive(playerId) == false)
			{
				ImageView cross = new ImageView(getActivity().getApplicationContext());
				cross.setImageResource(R.drawable.cross);
				cross.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				logoImageViewHolder.addView(cross);
				
			}
			
			
			TextView playerName = (TextView)playerInfoView.findViewById(R.id.graph_name);
			playerName.setText(info.name);
			playerName.setTypeface(appWrapper.econFont);
			
			if (state.GetPlayerToPlay() == playerId)
			{
				playerInfoView.setBackgroundColor(Color.rgb(255, 255, 255));
			}
						
			LinearLayout playerArea = (LinearLayout)powerGraphLayout.findViewById(R.id.names_row1);
			if (i > 2)
			{
				playerArea = (LinearLayout)powerGraphLayout.findViewById(R.id.names_row2);
			}
			
			if (i > 5)
			{
				playerArea = (LinearLayout)powerGraphLayout.findViewById(R.id.names_row3);
			}
			
			playerArea.addView(playerInfoView);
			
		}
		
		ImageView graphImageView = (ImageView)powerGraphLayout.findViewById(R.id.graph_area);
		
		TextView title = (TextView)powerGraphLayout.findViewById(R.id.graph_title);
		title.setTypeface(appWrapper.econFont);
		
		hudLayer.addView(powerGraphLayout);
		graphImageView.addOnLayoutChangeListener(new OnLayoutChangeListener(){

			@Override
			public void onLayoutChange(View arg0, int arg1, int arg2, int arg3,
					int arg4, int arg5, int arg6, int arg7, int arg8)
			{
				// redraw the graph
				DrawPowerGraph((ImageView)arg0, arg1, arg2, arg3, arg4);
				
			}});
		
		powerGraphLayout.setClickable(true);
		powerGraphLayout.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0)
			{
				hudLayer.removeView(arg0);
				
			}});
		
	}
	
	 @Override
	  public void onResume()
	 {
		 
	    super.onResume();
		 /*
	    if (mAdView != null) {
	    	mAdView.resume();
	    } else
	    {
	    	AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
			
			//SharedPreferences prefs = getActivity().getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
			boolean bTutorialMode = false;
			if (mEngine != null && mEngine.GetState() != null)
			{
				TactikonState tState = (TactikonState)mEngine.GetState();
				if (tState.bTutorial == true) bTutorialMode = true;
				
			}

			/*
			if (appWrapper.getPurchaseState() == false && bTutorialMode == false) // don't do any advertising if purchased
			{
				RelativeLayout hudLayer = (RelativeLayout)getActivity().findViewById(R.id.ad_layer);
				if (hudLayer == null) return;
				
				LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				//params.addRule(RelativeLayout.ALIGN_BOTTOM);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				mAdView = new AdView(getActivity());
				mAdView.setAdSize(AdSize.BANNER);
				//04-14 17:35:37.934: I/Ads(5280): Use AdRequest.Builder.addTestDevice("9FDB214EFC6BB5734A2ED89BC5AC4A16") to get test ads on this device.
				//mAdView.("9FDB214EFC6BB5734A2ED89BC5AC4A16");
				mAdView.setAdUnitId("ca-app-pub-6972320584437865/6013146232");
				mAdView.setLayoutParams(params);
				hudLayer.addView(mAdView);
		
			    // Create an ad request. Check logcat output for the hashed device ID to
			    // get test ads on a physical device.
			    AdRequest adRequest = new AdRequest.Builder()
			        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			        .addTestDevice("9FDB214EFC6BB5734A2ED89BC5AC4A16")
			        .build();
		
			    // Start loading the ad in the background.
			    mAdView.loadAd(adRequest);
			}

	    }*/
	    
	    RelativeLayout hudLayer = (RelativeLayout)getActivity().findViewById(R.id.hud_layer);
	    hudLayer.invalidate();
	    hudLayer.requestLayout();
	    hudLayer.setClipChildren(false);
	    
	    RelativeLayout adLayer = (RelativeLayout)getActivity().findViewById(R.id.ad_layer);
	    adLayer.invalidate();
	    adLayer.requestLayout();
	    adLayer.setClipChildren(false);
	    
	  }

	  @Override
	  public void onPause() {
		  /*
	    if (mAdView != null) {
	    	mAdView.pause();
	    }*/
	    
	    if (mEngine != null)
	    {
	    	mEngine.RemoveListener(mListener);
	    }
	    
	    super.onPause();
	    
	  }

	  /** Called before the activity is destroyed. */
	  @Override
	  public void onDestroy() {
	    // Destroy the AdView.
	    /*if (mAdView != null) {
	    	mAdView.destroy();
	    	mAdView = null;
	    }*/
	    super.onDestroy();
	  }
	  
	  boolean bReadyScreenShown = false;
	  
	  int mSelectedColour;
	  
	  void SetupColourAdapter(IState state, View view, int myPlayerId)
	  {
		  ImageView colourBlob = (ImageView)view.findViewById(R.id.logo);
		  colourBlob.setVisibility(View.GONE);
		  final ViewPager colourPager = (ViewPager) view.findViewById(R.id.colour_gallery);
			ColourAdapter adapter = new ColourAdapter(this.getActivity());
			colourPager.setAdapter(adapter);
			//Necessary or the pager will only have one extra page to show
			// make this at least however many pages you can see
			colourPager.setOffscreenPageLimit(9);
			//A little space between pages
			colourPager.setPageMargin(15);
			 
			//If hardware acceleration is enabled, you should also remove
			// clipping on the pager for its children.
			colourPager.setClipChildren(false);

			// remove the other players colours from the adapter
			for (Integer player : state.GetPlayers())
			{
				//if (player == myPlayerId) continue;
				if (player != -1)
				{
					PlayerInfo info = state.GetPlayerInfo(player);
					int index = -1;
					for (int i = 0; i < adapter.mColours.size(); ++i)
					{
						ColourItem colour = adapter.mColours.get(i);
						if (colour.r > info.r - 40 && colour.r < info.r + 40 &&
								colour.g > info.g - 40 && colour.g < info.g + 40 &&
								colour.b > info.b - 40 && colour.b < info.b + 40)
						{
							index = i;
						}
					}
					if (index != -1)
					{
						adapter.mColours.remove(index);
						adapter.notifyDataSetChanged();
					}
					
				}
			}
			
			PlayerInfo info = state.GetPlayerInfo(myPlayerId);
			ColourItem bob = adapter.new ColourItem(info.r, info.g, info.b);
			adapter.mColours.add(bob);
			
			colourPager.setAdapter(adapter);
			
			colourPager.setOnPageChangeListener(new OnPageChangeListener() {
			    public void onPageScrollStateChanged(int state)
			    {
			    	
			    }
			    
			    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
			    {
			    	
			    }

			    public void onPageSelected(int position)
			    {
			    	ColourAdapter adapter = (ColourAdapter)colourPager.getAdapter();
			    	ColourItem item = adapter.mColours.get(position);
			    	if (item == null) return;
			    	int col = Color.rgb(item.r, item.g, item.b);
			    	mSelectedColour = col;
			    	
			    	
			    }
			});
			colourPager.setCurrentItem(adapter.getCount() - 1);
	  }
	  
	  void ShowPlayerReadyScreen(final long playerId)
	  {
		  
		bReadyScreenShown = true;
		  
		IState state = mEngine.GetState();
		if (state == null) return;
		final TactikonState tState = (TactikonState)state;
		
		int numHumans = 0;
		for (Integer testId : tState.GetPlayers())
		{
			if (testId < TactikonState.AI_PLAYER_START)
			{
				numHumans ++;
			}
		}
		
		if (numHumans <= 1)
		{
			bReadyScreenShown = false;
			return;
		}
		
		final PlayerInfo info = tState.GetPlayerInfo(playerId);
		  
		AppWrapper appWrapper = (AppWrapper)getActivity().getApplicationContext();
			
			LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final RelativeLayout hudLayer = (RelativeLayout)getActivity().findViewById(R.id.hud_layer);
			
			// remove the existing powergraph if there's already one there
			LinearLayout oldReadyLayout = (LinearLayout)getActivity().findViewById(R.id.user_start_root);
			if (oldReadyLayout != null)
			{
				hudLayer.removeView(oldReadyLayout);
			}
			
			final LinearLayout userStartLayout = (LinearLayout)inflater.inflate(R.layout.user_start, null);
			
			ImageView logo = (ImageView)userStartLayout.findViewById(R.id.logo);
			final TextView alias = (TextView)userStartLayout.findViewById(R.id.alias);
			alias.setText(info.name);
			logo.setImageDrawable(appWrapper.GetLogoStore().GetLogo(info.logo));
			logo.setBackgroundColor(Color.rgb(info.r, info.g, info.b));
			logo.setVisibility(View.VISIBLE);
			alias.setTypeface(appWrapper.GetEconFont());
			
			LinearLayout colourLayout = (LinearLayout)userStartLayout.findViewById(R.id.colour_area);
			colourLayout.setVisibility(View.GONE);
			final EditText aliasEdit = (EditText)userStartLayout.findViewById(R.id.alias_edit);
			aliasEdit.setVisibility(View.GONE);
			aliasEdit.setTypeface(appWrapper.GetEconFont());
			final Button readyButton = (Button)userStartLayout.findViewById(R.id.ready_button);
			
			//aliasEdit.setOn
			InputFilter alphaNumericFilter = new InputFilter() {   
		        @Override  
		        public CharSequence filter(CharSequence arg0, int arg1, int arg2, Spanned arg3, int arg4, int arg5)  
		        {  
		           for (int k = arg1; k < arg2; k++)
		           {   
		        	   if (!Character.isLetterOrDigit(arg0.charAt(k)))
		        	   {   
		        		   return "";   
		        	   }   
		           }   
		           return null;   
		        }   
		      };   

				aliasEdit.setFilters(new InputFilter[] { new InputFilter.LengthFilter(24), alphaNumericFilter });
				
			aliasEdit.addTextChangedListener(new TextWatcher(){

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after)
				{
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count)
				{
					// TODO Auto-generated method stub
					
				}

				@Override
				public void afterTextChanged(Editable s)
				{
					// TODO Auto-generated method stub
					if (s.length() < 6)
	                {
	                	aliasEdit.setError("Alias must be 6 or more characters");
	                	readyButton.setEnabled(false);
	                } else
	                {
	                	aliasEdit.setError(null);
	                	readyButton.setEnabled(true);
	                }
					
				}});

			LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
			userStartLayout.setLayoutParams(params);
			
			readyButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					ClearPlayerReadyScreen();
					
					LinearLayout colourLayout = (LinearLayout)userStartLayout.findViewById(R.id.colour_area);
					EventChangePlayerInfo changeColourEvent = new EventChangePlayerInfo();
					boolean bChangedInfo = false;
					if (colourLayout.getVisibility() == View.VISIBLE)
					{
						bChangedInfo = true;
						changeColourEvent.playerIdToChange = playerId;
						changeColourEvent.r = Color.red(mSelectedColour);
						changeColourEvent.g = Color.green(mSelectedColour);
						changeColourEvent.b = Color.blue(mSelectedColour);
						
						
					}
					EditText aliasEdit = (EditText)userStartLayout.findViewById(R.id.alias_edit);
					if (aliasEdit.getVisibility() == View.VISIBLE)
					{
						bChangedInfo = true;
						changeColourEvent.playerIdToChange = playerId;
						changeColourEvent.newAlias = aliasEdit.getText().toString();
					}
					if (bChangedInfo == true)
					{
						mInjector.AddEvent(changeColourEvent);
					}
				}});
			
			logo.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					LinearLayout colourLayout = (LinearLayout)userStartLayout.findViewById(R.id.colour_area);
					colourLayout.setVisibility(View.VISIBLE);
					SetupColourAdapter(tState, userStartLayout, (int)playerId);
				}});
			
			alias.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v)
				{
					EditText aliasEdit = (EditText)userStartLayout.findViewById(R.id.alias_edit);
					aliasEdit.setVisibility(View.VISIBLE);
					aliasEdit.setText(info.name);
					
					alias.setVisibility(View.GONE);
					
				}});
			
			hudLayer.addView(userStartLayout);
			
			getActivity().invalidateOptionsMenu();
		  
	  }
	  
	  void ClearPlayerReadyScreen()
	  {
		  bReadyScreenShown = false;
		  final RelativeLayout hudLayer = (RelativeLayout)getActivity().findViewById(R.id.hud_layer);
			
			// remove the existing powergraph if there's already one there
			LinearLayout oldReadyLayout = (LinearLayout)getActivity().findViewById(R.id.user_start_root);
			if (oldReadyLayout != null)
			{
				hudLayer.removeView(oldReadyLayout);
			}
			
			getActivity().invalidateOptionsMenu();
	  }

	void EndTurn()
	{
		if (mEngine == null) return;
		IState state = mEngine.GetState();
		if (state == null) return;
		
		IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
		TactikonState tState = (TactikonState)state;
		if (tState.playerToPlay != GetCurrentUserId()) return;
		
		EventEndTurnWithPlayerId endTurnEvent = new EventEndTurnWithPlayerId();
		endTurnEvent.playerId =  GetCurrentUserId();
		
		mInjector.AddEvent(endTurnEvent);
		
		if (tState.bLocalGame == false)
			getActivity().finish();
	}

	
}
