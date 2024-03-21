package uk.co.eidolon.shared.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.View;

import Core.IEvent;
import Core.IState;
import Core.PlayerInfo;
import Core.StateEngine;
import Network.UserInfo;
import Support.INewGameOptions;
import uk.co.eidolon.shared.database.ServerUploadQueue;
import uk.co.eidolon.shared.views.IStateInfo;

public interface  IAppWrapper
{
	public int GetDefaultZoomLevel();
	public ServerUploadQueue GetUploadQueue();
	public LogoStore GetLogoStore();
	
	public int GetUserId();
	public String GetAccountName();
	public String GetLogo();
	public String GetAlias();
	public int GetColour();
	
	Bitmap GetStateImage(int size, IState state, long userId);
	
	public Typeface GetEconFont();
	
	public void SetAccountDetails(UserInfo info);
	
	public IState StateFactory();
	public IStateInfo StateInfoFactory(int GameID, long UserID);
	public IStateInfo StateInfoFactory(int GameID, long UserID, IState state);
	
	public boolean IsNetworkConnected();
	
	public String GetStateUpdatedIntentAction();
	public String GetIncomingChatIntentAction();
	
	public String GetServerSyncIntentAction();
	
	public IEvent JoinGameEventFactory(PlayerInfo info);
	
	public IEvent SurrenderGameEventFactory(long playerId);

	public IEvent NewGameEventFactory(INewGameOptions options);
	
	public Activity GetPopupActivity();
	
	public View GameViewFactory(Context context, StateEngine engine, int gameId);
	public Fragment NewGameFragmentFactory(Context context, boolean bNetwork);
	public Fragment GameFragmentFactory(StateEngine engine, int gameId);
	public Fragment MainFragmentFactory(Context context);

	public String GetNotificationTextTitle(IState state);
	public String GetNotificationTextSubtext(Context context, IState state);
	public int GetNotificationIcon();
	
	public void StoreState(IState state, String tag);
	public IState RetreiveState(String tag);

}
