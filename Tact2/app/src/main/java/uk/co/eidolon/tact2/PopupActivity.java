package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Random;

import uk.co.eidolon.shared.activities.GameActivity;
import uk.co.eidolon.shared.activities.JoinGameActivity;
import uk.co.eidolon.shared.activities.QueryAccountActivity;

import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.ColourAdapter;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.utils.ColourAdapter.ColourItem;
import Core.IState;
import Core.InvalidUpdateException;
import Network.PackageResponse;
import Network.UserInfo;
import Network.Packages.PackageLogin;
import Network.Packages.PackageUpdateAccount;
import Network.Packages.PackageUpdateGCM;
import Tactikon.State.EventJoinGame;
import Tactikon.State.EventNewGame;
import Tactikon.State.TactikonNewGameOptions;
import Tactikon.State.TactikonState;
import Tactikon.State.TactikonNewGameOptions.MirrorType;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PopupActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	    getActionBar().hide();
		setContentView(R.layout.activity_popup);
		
		Button tutorialButton = (Button)findViewById(R.id.tutorial_button);
		
		tutorialButton.setEnabled(false);
		
		
		Button singleButton = (Button)findViewById(R.id.single_button);
		Button joinButton = (Button)findViewById(R.id.join_button);
		
		joinButton.setEnabled(false);
		
		Button setupAccountButton = (Button)findViewById(R.id.account_button);
		
		tutorialButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				DoSkirmish();
			}});
		
		Button closeButton = (Button)findViewById(R.id.close_button);
		
		closeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				PopupActivity.this.finish();
			}});
		
		setupAccountButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent i = new Intent(PopupActivity.this, QueryAccountActivity.class);
	        	startActivity(i);
			}});
		
		joinButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent searchGamesIntent = new Intent(PopupActivity.this, uk.co.eidolon.shared.activities.SearchResultsActivity.class);
	        	startActivity(searchGamesIntent);
			}});
		
		singleButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent newGameIntent = new Intent(PopupActivity.this, uk.co.eidolon.shared.activities.NewGameActivity.class);
	        	newGameIntent.putExtra("Local", 1);
	        	startActivity(newGameIntent);
			}});
		
	}
	
	void DoSkirmish()
	{
		TactikonNewGameOptions options = new TactikonNewGameOptions();
		options.bForest = false;
		options.bLocalGame = true;
		options.bMountains = true;
		options.cities = 2;
		options.fogOfWar = false;
		options.landMassRatio = 0.6f;
		options.mapSeed = 80;
		options.mapSize = 16;
		options.mirrorType = MirrorType.Vertical;
		options.numHumanPlayers = 2;
		options.scale = 6;
		options.turnTimeOut = 24;
		options.winCondition = WinCondition.CaptureAllBases;
		options.bTutorial = true;
		
		IState state = null;
		EventNewGame newGameEvent = new EventNewGame(options);
		try
		{
			state = newGameEvent.updateState(state);
		} catch (InvalidUpdateException e)
		{
			// swallow error!
			return;
		}
		
		// join human player
		IAppWrapper appWrapper = (IAppWrapper)this.getApplicationContext();
		EventJoinGame joinEvent = new EventJoinGame();
		joinEvent.playerIdToJoin = (int)appWrapper.GetUserId();
		joinEvent.r = Color.red(appWrapper.GetColour());
		joinEvent.g = Color.green(appWrapper.GetColour());
		joinEvent.b = Color.blue(appWrapper.GetColour());
		
		joinEvent.logo = appWrapper.GetLogo();
		joinEvent.name = appWrapper.GetAlias();
		
		try
		{
			state = joinEvent.updateState(state);
		} catch (InvalidUpdateException e)
		{
			return;
		}
		
		joinEvent.playerIdToJoin = TactikonState.AI_PLAYER_START;
		int r = joinEvent.r;
		int g = joinEvent.g;
		int b = joinEvent.b;
		joinEvent.r = g;
		joinEvent.g = b;
		joinEvent.b = r;
		Random rand = new Random();
		int logoNum = rand.nextInt(appWrapper.GetLogoStore().GetLogoList().length);
		joinEvent.logo = appWrapper.GetLogoStore().GetLogoList()[logoNum];
		joinEvent.name = "AI Player";
		try
		{
			state = joinEvent.updateState(state);
		} catch (InvalidUpdateException e)
		{
			return;
		}
		
		// now add the game to the gamesDb
		// we can use -1 as the ID because we know there are no games currently in the list
		GameDatabase.getInstance(this).AddGame(-1, appWrapper.GetUserId());
		GameDatabase.getInstance(this).UpdateGame(-1, appWrapper.GetUserId(), state);
		GameDatabase.getInstance(this).SetSequence(-1, appWrapper.GetUserId(), state.GetSequence());
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(-1);
		SyncListDB.getInstance(this).AddToSyncList(list, "GameList", (int)appWrapper.GetUserId());
		
		// and launch the gameActiviy to view this game
		Intent gameIntent = new Intent(this, GameActivity.class);
		
		gameIntent.putExtra("GameID", -1);
		
		SharedPreferences prefs = this.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        //editor.putBoolean("TUTORIAL_MODE", true);
        
        editor.putFloat(Integer.toString(-1) + "_" + Long.toString(appWrapper.GetUserId()) + "_CameraX", 13);
		editor.putFloat(Integer.toString(-1) + "_" + Long.toString(appWrapper.GetUserId()) + "_CameraY", 2);
		editor.putFloat(Integer.toString(-1) + "_" + Long.toString(appWrapper.GetUserId()) + "_ScaleFactor", appWrapper.GetDefaultZoomLevel());
		editor.commit();
		
    	startActivity(gameIntent);
		
	}
	
	
	@Override
	public void onResume()
	{
		Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		
		if (accounts.length == 1)
		{
			// use the only account without letting the user know what we're doing...
			String accountName = accounts[0].name;
			DoLogin(accountName);
		}
		
		IAppWrapper appWrapper = (IAppWrapper)PopupActivity.this.getApplication();
		if (appWrapper.GetUserId() != -1)
		{
			Button tutorialButton = (Button)findViewById(R.id.tutorial_button);
			Button joinButton = (Button)findViewById(R.id.join_button);
			tutorialButton.setEnabled(true);
			joinButton.setEnabled(true);
		}
		
		super.onResume();
	}
	
	public void DoLogin(final String accountName)
	{
		final PackageLogin p = new PackageLogin();
		p.accountName = accountName;
		
		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
				
			}

			@Override
			public void postExecute()
			{
				if (p.mReturnCode != PackageResponse.Success)
				{
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
					    DoLogin(accountName);
					  }
					}, 1000);
				} else if (p.mReturnCode == PackageResponse.Success)
				{
					IAppWrapper appWrapper = (IAppWrapper)PopupActivity.this.getApplication();
					UserInfo info = new UserInfo();
					info.userId = p.userId;
					info.accountName = p.accountName;
					info.colour = p.colour;
					if (p.colour == 0)
					{
						info.colour = Color.rgb(255,  0,  0);
					}
					info.logo = p.logo;
					String[] logoList = appWrapper.GetLogoStore().GetLogoList();
					Random rand = new Random();
					info.logo = logoList[rand.nextInt(logoList.length)];
					info.alias = p.alias;
					appWrapper.SetAccountDetails(info);
					
					Button tutorialButton = (Button)findViewById(R.id.tutorial_button);
					tutorialButton.setEnabled(true);
					
					Button joinButton = (Button)findViewById(R.id.join_button);
					joinButton.setEnabled(true);
					
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
	
		
	@Override
	public void onDestroy()
	{
	   super.onDestroy();
	}
}
