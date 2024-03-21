package uk.co.eidolon.tact2;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.Map;
import java.util.TreeMap;

import Core.IEvent;
import Core.IState;
import Core.IState.GameStatus;
import Core.PlayerInfo;
import Core.StateEngine;
import Network.UserInfo;
import Support.INewGameOptions;
import Support.Preferences;
import Tactikon.State.EventJoinGame;
import Tactikon.State.EventNewGame;
import Tactikon.State.EventSurrender;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;
import uk.co.eidolon.shared.database.ServerUploadQueue;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.utils.LogoStore;
import uk.co.eidolon.shared.views.IStateInfo;

public class AppWrapper extends Application implements IAppWrapper
{
	
	private ServerUploadQueue mUploadQueue;
	
	TextureManager mTextureManager;
	
	LogoStore mLogoStore;
	
	//public Typeface lcdFont;
	public Typeface econFont;
	public Typeface pixelFont;
	
	Map<String, IState> stateStore = new TreeMap<String, IState>();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		mUploadQueue = new ServerUploadQueue(this);
		mTextureManager = new TextureManager(this);
		mLogoStore = new LogoStore(this);
		
		//lcdFont = Typeface.createFromAsset(getAssets(), "fonts/lcd.ttf");
		econFont = Typeface.createFromAsset(getAssets(), "fonts/Signika-Regular.otf");
		pixelFont = Typeface.createFromAsset(getAssets(), "fonts/pixelfont.ttf");
		
		//Polljoy.startSession(this.getApplicationContext(), "34BD9C8B90BA8E9D9EDA");  // This is the APP ID for Tactikon 2
	}
	
	public Typeface GetEconFont()
	{
		return econFont;
	}
	
	public LogoStore GetLogoStore()
	{
		return mLogoStore;
	}
	
	public Activity GetPopupActivity()
	{
		return new PopupActivity();
	}
	
	public ServerUploadQueue GetUploadQueue()
	{
		return mUploadQueue;
	}
	
	public TextureManager GetTextureManager()
	{
		return mTextureManager;
	}
	
	public int GetUserId()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
        int id = (int)prefs.getLong(Preferences.USERID_LONG, -1);
        if (id == -1) id = 1;
        return id;
	}
	
	public String GetAccountName()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
        return prefs.getString(Preferences.ACCOUNTNAME_STRING, "");
	}
	
	public String GetLogo()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
        return prefs.getString(Preferences.LOGO_STRING, "");
	}
	
	public int GetDefaultZoomLevel()
	{
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		if (metrics.densityDpi < 240) return 1;
		if (metrics.densityDpi < 320) return 2;
		return 4;
	}
	
	public String GetAlias()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
		String name = prefs.getString(Preferences.ALIAS_STRING, "Player 1");
		if (name.length() == 0) name = "Player 1";
        return name;
	}
	
	public int GetColour()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
		int colour = prefs.getInt(Preferences.COLOUR_INT, Color.rgb(0,0,255));
		if (colour == 0) colour = Color.rgb(0,0,255);
        return colour;
	}
	
	public void SetAccountDetails(UserInfo info)
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Preferences.ACCOUNTNAME_STRING, info.accountName);
        editor.putLong(Preferences.USERID_LONG, info.userId);
        editor.putInt(Preferences.COLOUR_INT, info.colour);
        editor.putString(Preferences.ALIAS_STRING, info.alias);
        editor.putString(Preferences.LOGO_STRING, info.logo);
        editor.commit();
	}
	
	public boolean getPurchaseState()
	{
		SharedPreferences prefs = getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		boolean result = prefs.getBoolean("PURCHASE", false);
		
		return result;
	}
	
	public IState StateFactory()
	{
		return new TactikonState();
	}
	
	public IStateInfo StateInfoFactory(int GameID, long UserID)
	{
		return new TactikonStateInfo(GameID, UserID);
	}
	
	public IStateInfo StateInfoFactory(int GameID, long UserID, IState state)
	{
		TactikonStateInfo info = new TactikonStateInfo(GameID, UserID);
		info.mState = (TactikonState)state;

		return info;
	}

	
	public boolean IsNetworkConnected()
	{
		ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
            for (int i = 0; i < info.length; i++)
            {
                if (info[i].getState() == NetworkInfo.State.CONNECTED)
                {
                    return true;
                }
            }
        }

        return false;
	}
	

	@Override
	public IEvent JoinGameEventFactory(PlayerInfo info)
	{
		EventJoinGame event = new EventJoinGame();
		event.playerIdToJoin = (int) info.userId;
		
		event.name = info.name;
		event.logo = info.logo;
		
		event.r = info.r;
		event.g = info.g;
		event.b = info.b;
		
		return event;
	}
	
	
	@Override
	public
	IEvent NewGameEventFactory(INewGameOptions options)
	{
		EventNewGame event = new EventNewGame(options);
		
		return event;
	}

	@Override
	public String GetStateUpdatedIntentAction()
	{
		return "uk.co.eidolon.tact2.REFRESH_STATE_VIEW";
	}

	@Override
	public String GetServerSyncIntentAction()
	{
		return "uk.co.eidolon.tact2.UPDATE_STATE";
	}
	
	@Override
	public String GetIncomingChatIntentAction()
	{
		return "uk.co.eidolon.tact2.CHAT";
	}


	@Override
	public Fragment NewGameFragmentFactory(Context context, boolean bNetwork)
	{
		NewGameFragment fragment = new NewGameFragment();
		fragment.SetDetails(bNetwork);
		return fragment;
	}
	
	@Override
	public Fragment MainFragmentFactory(Context context)
	{
		return new TactikonGameListFragment();
	}

	@Override
	public Fragment GameFragmentFactory(StateEngine engine, int mGameId)
	{
		GameFragment fragment = new GameFragment();
		fragment.SetEngineAndGameId(engine, mGameId);
		return fragment;
	}

	@Override
	public View GameViewFactory(Context context, StateEngine engine, int gameId)
	{
		return new GameView(context, engine, gameId);
	}

	StateEngine mStateEngine = null;
	
	public StateEngine GetCurrentStateEngine()
	{
		return mStateEngine;
	}
	
	public void SetCurrentStateEngine(StateEngine stateEngine)
	{
		mStateEngine = stateEngine;
	}

	public Bitmap GetStateImage(int size, IState state, long userId)
	{
		TactikonState tState = (TactikonState)state;
		
		BitmapDrawable drawable = MapGraphics.GeneratePreview(tState, userId);
		
		return drawable.getBitmap();
		
	}
	
	@Override
	public String GetNotificationTextTitle(IState state)
	{
		TactikonState tState = (TactikonState)state;
		String notification = "";
		
		notification += "Tactikon";
		
		return notification;
	}
	
	@Override
	public String GetNotificationTextSubtext(Context context, IState state)
	{
		TactikonState tState = (TactikonState)state;
		String notification = "";
		
		if (tState.GetGameState() == GameStatus.InGame)
		{
			if (tState.playerToPlay == GetUserId())
			{
				notification += "It's your turn.";
			} else
			{
				notification += "Waiting for another player to take their turn.";
			}
			
			if (tState.winCondition == WinCondition.Annihilate)
			{
				notification += " Annihilate to win."; 
			} else if (tState.winCondition == WinCondition.CaptureAllBases)
			{
				notification += " Capture all cities to win.";
			} else if (tState.winCondition == WinCondition.CaptureHQ)
			{
				notification += " Capture HQs to win.";
			}

		} else if (tState.GetGameState() == GameStatus.GameOver)
		{
			if (tState.GetWinner() == GetUserId())
			{
				notification += "You've won the game.";
			} else
			{
				notification += "You've lost the game.";
			}
		} else if (tState.GetGameState() == GameStatus.TimeOut)
		{
			notification += "Game ended due to insufficient players.";
		}
		
	
		
		return notification;
	}
	
	@Override
	public void StoreState(IState state, String tag)
	{
		stateStore.put(tag, state);
	}
	
	@Override
	public IState RetreiveState(String tag)
	{
		if (stateStore.containsKey(tag)) return stateStore.get(tag);
		return null;
	}
	
	

	@Override
	public int GetNotificationIcon()
	{
		return R.drawable.ic_stat_notification;
	}

	@Override
	public IEvent SurrenderGameEventFactory(long playerId)
	{
		EventSurrender event = new EventSurrender();
		event.playerIdToSurrender = (int) playerId;
		
		return event;
	}

}
