package uk.co.eidolon.shared.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import Core.IState;
import Core.PlayerInfo;
import Network.IPackage;
import Network.PackageResponse;
import Network.Packages.PackageGetGame;
import Network.Packages.PackageJoinGame;
import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.NotificationUtils;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.ColourAdapter;
import uk.co.eidolon.shared.utils.ColourAdapter.ColourItem;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.IStateInfo;

public class JoinGameActivity extends Activity
{
	public int mGameID = -1;
	
	public long mUserID = -1;
	
	
	
	boolean bChooseColour = false;
	int mSelectedColour;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_join_game);
		
		Bundle b = this.getIntent().getExtras();
        if (b != null)
        {
        	mGameID = b.getInt("GameID");
        }
        
        IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
        mUserID = appWrapper.GetUserId();
        
        Button joinGameButton = (Button)findViewById(R.id.join_game_button);
        joinGameButton.setEnabled(false);
		joinGameButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				JoinGame();
			}
        });
		
		
		IState stateToJoin = null;
		if (b.containsKey("fromInvite") == false)
		{
			
			stateToJoin = appWrapper.RetreiveState("GameToJoin");
			SetupJoinArea(stateToJoin);
		} else
		{
			// need to fetch the state from the server
			new NotificationUtils().ClearNotification(this, mGameID);
			final PackageGetGame p = new PackageGetGame();
			p.currentSequenceId = 0;
			p.gameId = mGameID;
			
			PackageDelivery sender = new PackageDelivery((Context)this, (IPackage)p, new ISendEvents() {

				@Override
				public void preExecute()
				{
					// TODO Auto-generated method stub
					
				}

				@Override
				public void postExecute()
				{
					// TODO Auto-generated method stub
					if (p.mReturnCode == PackageResponse.Success)
					{
						SetupJoinArea(p.state);
					} else
					{
						LinearLayout progressAreaLayout = (LinearLayout)findViewById(R.id.progress_area);
						progressAreaLayout.setVisibility(View.VISIBLE);
						TextView progressText = (TextView)findViewById(R.id.text_progress);
						progressText.setText("Error getting game details. Please try again later.");
					}
				}

				@Override
				public void postExecuteBackground()
				{
					// TODO Auto-generated method stub
				}
			});
			sender.Send();
			
		}
		
		
		
	}
	
	void SetupJoinArea(IState stateToJoin)
	{
		IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
		LinearLayout gameAreaLayout = (LinearLayout)findViewById(R.id.game_area);
		gameAreaLayout.removeAllViews();
		IStateInfo stateInfo = appWrapper.StateInfoFactory(mGameID, appWrapper.GetUserId(), stateToJoin);
		View view = stateInfo.InflateView(this);
		stateInfo.PopulateView(this, view);
		gameAreaLayout.addView(view);
		
		Button joinGameButton = (Button)findViewById(R.id.join_game_button);
		joinGameButton.setEnabled(true);

		int playerColourInt = appWrapper.GetColour();
		
		if (playerColourInt == 0) bChooseColour = true;
		
		for (Integer player : stateToJoin.GetPlayers())
		{
			if (player != -1)
			{
				PlayerInfo info = stateToJoin.GetPlayerInfo(player);
				if (info.r > Color.red(playerColourInt) - 15 && info.r < Color.red(playerColourInt) + 15 &&
					info.g > Color.green(playerColourInt) - 15 && info.g < Color.green(playerColourInt) + 15 &&
					info.b > Color.blue(playerColourInt) - 15 && info.b < Color.blue(playerColourInt) + 15)
				{
					bChooseColour = true;
				}
			}
		}
		
		if (bChooseColour == true)
		{
			LinearLayout colourArea = (LinearLayout)findViewById(R.id.colour_area);
			colourArea.setVisibility(View.VISIBLE);
			
			final ViewPager colourPager = (ViewPager) findViewById(R.id.colour_gallery);
			ColourAdapter adapter = new ColourAdapter(this);
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
			for (Integer player : stateToJoin.GetPlayers())
			{
				if (player != -1)
				{
					PlayerInfo info = stateToJoin.GetPlayerInfo(player);
					int index = -1;
					for (int i = 0; i < adapter.mColours.size(); ++i)
					{
						ColourItem colour = adapter.mColours.get(i);
						if (colour.r > info.r - 30 && colour.r < info.r + 30 && colour.g > info.g - 30 && colour.g < info.g + 30 && colour.b > info.b - 15 && colour.b < info.b + 15)
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
			
			colourPager.setAdapter(adapter);
			
			// and set to a random colour
			Random rand = new Random();
			int colourIndex = rand.nextInt(adapter.getCount());
			colourPager.setCurrentItem(colourIndex);
			mSelectedColour = adapter.mColours.get(colourIndex).colour;
			
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
		}
	}

	
	void JoinGame()
	{
		IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
		
		final PackageJoinGame p = new PackageJoinGame();
		
		PlayerInfo info = new PlayerInfo();
		int colour = appWrapper.GetColour();
		info.r = Color.red(colour);
		info.g = Color.green(colour);
		info.b = Color.blue(colour);
		info.name = appWrapper.GetAlias();
		info.logo = appWrapper.GetLogo();
		info.userId = appWrapper.GetUserId();
		
		if (bChooseColour == true) // we've used the colour picker, so override the colour selection
		{
			info.r = Color.red(mSelectedColour);
			info.g = Color.green(mSelectedColour);
			info.b = Color.blue(mSelectedColour);
		}
		
		p.gameId = mGameID;
		p.joinGameEvent = appWrapper.JoinGameEventFactory(info);
		
		final Button joinGameButton = (Button)findViewById(R.id.join_game_button);
		joinGameButton.setEnabled(false);
		
		PackageDelivery sender = new PackageDelivery((Context)this, (IPackage)p, new ISendEvents() {
			@Override
			public void preExecute()
			{
				
				JoinGameActivity.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
			}

			@Override
			public void postExecute()
			{
				JoinGameActivity.this.setProgressBarIndeterminateVisibility(Boolean.FALSE);
				if (p.mReturnCode == PackageResponse.Success)
				{
					// add this gameId to the local game database
					GameDatabase.getInstance(JoinGameActivity.this).AddGame(p.gameId, mUserID);
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(p.gameId);
					SyncListDB.getInstance(JoinGameActivity.this).AddToSyncList(list, "GameList", (int)mUserID);
					
					// retreive the game from the server
					Intent serviceIntent = new Intent();

					serviceIntent.setPackage("uk.co.eidolon.tact2");
					IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
			        serviceIntent.setAction(appWrapper.GetServerSyncIntentAction());
			        serviceIntent.putExtra("updateStateId", p.gameId);
			        serviceIntent.putExtra("UserID", mUserID);
			        startService(serviceIntent);
			        
			        // update the state list to include the new game
				    String updateStatesIntent = appWrapper.GetStateUpdatedIntentAction();
					Intent intent = new Intent(updateStatesIntent);
					intent.putExtra("GameListChanged", "true");
					LocalBroadcastManager.getInstance(JoinGameActivity.this).sendBroadcast(intent);
					
					finish();
				} else
				{
					final Button joinGameButton = (Button)findViewById(R.id.join_game_button);
					joinGameButton.setEnabled(true);
					
					LinearLayout progressAreaLayout = (LinearLayout)findViewById(R.id.progress_area);
					progressAreaLayout.setVisibility(View.VISIBLE);
					TextView progressText = (TextView)findViewById(R.id.text_progress);
					if (p.mReturnCode == PackageResponse.ErrorStateError)
					{
						progressText.setText("Sorry, you can't join this game.");
					} else 
					{
						progressText.setText("Sorry, there was a network error while joining this game.");
					}
				}
			}

			@Override
			public void postExecuteBackground()
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		sender.Send();
	}
	
	

}
