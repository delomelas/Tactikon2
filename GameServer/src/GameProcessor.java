import java.awt.Color;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import Core.IState;
import Network.EventQueueItem;
import Network.PackageResponse;
import Network.SyncList;
import Network.UserInfo;
import Core.InvalidUpdateException;
import Network.Packages.PackageEventList;
import Network.Packages.PackageGetGame;
import Network.Packages.PackageGetProfile;
import Network.Packages.PackageNewGame;
import Network.Packages.PackageSearchAlias;
import Network.Packages.PackageSearchGames;
import Network.Packages.PackageJoinGame;
import Network.Packages.PackageSendChat;
import Network.Packages.PackageSendInvite;
import Network.Packages.PackageSyncList;
import Network.Packages.PackageUpdateAccount;
import Tactikon.State.EventEndTurn;
import Tactikon.State.EventEndTurnWithPlayerId;
import Tactikon.State.EventJoinGame;
import Tactikon.State.EventSkipTurn;
import Tactikon.State.EventSurrender;
import Tactikon.State.EventTimeout;
import Tactikon.State.EventWithdrawPlayer;

public class GameProcessor
{
	void DoPackageNewGame(UserInfo userInfo, PackageNewGame p, int appVersion, String appName)
	{
		p.mReturnCode = PackageResponse.ErrorIOError;
		return;
		/*
		// we've already verified the userId, userInfo should be correct
		if (userInfo.userId < 0)
		{
			System.out.println("No valid user, can't create new game.");
			return;
		}
		
		IState gameState = null;
		
		try
		{
			gameState = p.newGameEvent.updateState(null);
			gameState.lastUpdateTime = System.currentTimeMillis() / 1000;
		} catch (InvalidUpdateException e)
		{
			// if the update wasn't valid, need to respond with a -1
			p.mReturnCode = PackageResponse.ErrorIOError;
			return;
		}
		
		// add "gameState" to the database
		long epoch = System.currentTimeMillis()/1000;
		long gameId = DBStuff.getInstance().AddNewGame(0, 0, epoch, appVersion, appName, gameState);
		//gameId, sequence, status, lastUpdate, appVersion, appName, blob
		
		System.out.println("INFO: Added game with gameId=["+gameId+"]");
		
		p.mGameID = (int)gameId;
		p.mReturnCode = PackageResponse.Success;*/
	}
	
	void DoPackageGetProfile(UserInfo userInfo, PackageGetProfile p, int appVersion, String appName)
	{
		if (userInfo.userId < 0)
		{
			System.out.println("No valid user, can't get user profile.");
			return;
		}
		
		TreeMap<Integer, Integer> profile = ProfileDB.getInstance().GetProfile(p.userId, appName);
		UserInfo info = DBStuff.getInstance().GetUserInfo(p.userId);
		
		if (info == null)
		{
			p.mReturnCode = PackageResponse.ErrorNoUser;
			return;
		}
		p.alias = info.alias;
		if (p.alias.contains("@"))
		{
			int at = p.alias.indexOf("@");
			if (at > 2)
				p.alias = p.alias.substring(0, at - 1);
		}
		p.colour = info.colour;
		p.logo = info.logo;
		p.profile = profile;
		p.mReturnCode = PackageResponse.Success;
		
	}
	
	void DoPackageSearchGames(UserInfo userInfo, PackageSearchGames p, int appVersion, String appName)
	{
		// we've already verified the userId, userInfo should be correct
		if (userInfo.userId < 0)
		{
			System.out.println("No valid user, can't search games.");
			return;
		}
		
		// prepare a DB query to search as closely as requested (can we do the offset as part of the db request?)
		// filter by users in-play, if required
		DBStuff.SearchGameResults results = DBStuff.getInstance().SearchGames(appVersion, appName, p.startOffset, p.maxResults, p.searchState, (int)userInfo.userId);
		
		p.numReturned = results.resultData.size();
		p.resultData = results.resultData;
		p.resultGameIDs = results.resultGames;
		p.bMoreResults = results.bMoreResults;
		p.mReturnCode = PackageResponse.Success;
	}
	
	
	void DoPackageJoinGame(UserInfo userInfo, PackageJoinGame p, int appVersion, String appName)
	{
		// we've already verified the userId, userInfo should be correct
		if (userInfo.userId < 0)
		{
			System.out.println("No valid user, can't join game.");
			p.mReturnCode = PackageResponse.ErrorStateError;
			return;
		}
		
		// retrieve the game from the database
		IState state = DBStuff.getInstance().GetGame(p.gameId, appVersion, appName);
		
		if (state == null)
		{
			System.out.println("Failed to join game [" + p.gameId +"] - not in DB");
			p.mReturnCode = PackageResponse.ErrorIOError;
			return;
		}
		
		// attempt to apply the join game event
		IState newState = null;
		try
		{
			newState = p.joinGameEvent.updateState(state);
			newState.lastUpdateTime = System.currentTimeMillis() / 1000;
		} catch (InvalidUpdateException e)
		{
			System.out.println("Failed to join game [" + p.gameId +"] - InvalidUpdate (" + e.getMessage() + ")");

			p.mReturnCode = PackageResponse.ErrorStateError;
			return;
		}
		
		if (newState == null)
		{
			System.out.println("Failed to join game [" + p.gameId +"] - null state");
			p.mReturnCode = PackageResponse.ErrorIOError;
			return;
		}
		
		// write the state back to the database
		if (DBStuff.getInstance().UpdateGame(p.gameId, newState, appName, true) == true)
		{
			System.out.println("Player: " + userInfo.userId + " has joined game: " + p.gameId);
			p.mReturnCode = PackageResponse.Success;
		} else
		{
			// didn't manage to join the game
			p.mReturnCode = PackageResponse.ErrorStateError;
		}
		
		// add the player to the PlayersInGamesDB
		PlayersInGamesDB.getInstance().AddPlayerToGame((int) userInfo.userId, p.gameId, appName);
		
	}
	
	void DoPackageGetGame(UserInfo userInfo, PackageGetGame p, int appVersion, String appName)
	{
		IState state = DBStuff.getInstance().GetGame(p.gameId, appVersion, appName);
		if (state == null)
		{
			System.out.println("Failed to get Game [" + p.gameId +"]");
			p.mReturnCode = PackageResponse.ErrorIOError;
			return;
		}
		
		p.mReturnCode = PackageResponse.Success;
		p.state = state;

		if (p.state.GetSequence() == p.currentSequenceId)
		{
			System.out.println("GameID: " + p.gameId + " (Seq: " + p.state.GetSequence() + ") already up to date for PlayerId: " + userInfo.userId);
			p.state = null;
		} else
		{
			System.out.println("GameID: " + p.gameId + " (Seq: " + p.state.GetSequence() + ") sent to PlayerId: " + userInfo.userId);
		}
	}
	
	void DoPackageEventList(UserInfo userInfo, PackageEventList p, int appVersion, String appName)
	{
		TreeMap<Integer, IState> stateMap = new TreeMap<Integer, IState>();
		TreeMap<Integer,Boolean>  updateMap = new TreeMap<Integer, Boolean>();
		int count = 0;
		for (EventQueueItem item : p.eventList)
		{
			IState state = null;
			if (stateMap.containsKey(item.GameID)) state = stateMap.get(item.GameID);
			if (state == null)
			   state = DBStuff.getInstance().GetGame(item.GameID, appVersion, appName);
			if (updateMap.containsKey(item.GameID) == false)
				updateMap.put(item.GameID, false);
			System.out.print("Event: " + item.event.getClass().getName() + " Game: " + item.GameID);
			
			if ((item.event.getClass().equals(EventEndTurnWithPlayerId.class)) ||
					(item.event.getClass().equals(EventSurrender.class)) ||
					(item.event.getClass().equals(EventSkipTurn.class)) ||
					(item.event.getClass().equals(EventWithdrawPlayer.class)) ||
					(item.event.getClass().equals(EventEndTurn.class)) ||
					(item.event.getClass().equals(EventJoinGame.class)) ||
					(item.event.getClass().equals(EventTimeout.class)))
				updateMap.put(item.GameID, true);
			
			
			try
			{
				IState newState = item.event.updateState(state);	
				if (updateMap.get(item.GameID) == true)
				{
					newState.lastUpdateTime = System.currentTimeMillis() / 1000;
				}
				stateMap.put(item.GameID,  newState);
				
			} catch (InvalidUpdateException e)
			{
				// TODO Auto-generated catch block
				System.out.println("ERROR: Invalid Event while processing EventList ("+item.event.getClass().getName() + ") from User: " + userInfo.userId + " Game: " + item.GameID);
				e.printStackTrace();
				System.out.println(e.toString());
				p.mReturnCode = PackageResponse.ErrorStateError;
				p.mSuccessCount = count;
				
				// write out the gamestates
				for (Entry<Integer, IState> entry : stateMap.entrySet())
				{
					DBStuff.getInstance().UpdateGame(entry.getKey(), entry.getValue(), appName, true);
				}
				
				return;
			}
			count ++;
		}
		
		
		
		// write out the gamestates
		for (Entry<Integer, IState> entry : stateMap.entrySet())
		{
			DBStuff.getInstance().UpdateGame(entry.getKey(), entry.getValue(), appName, updateMap.get(entry.getKey()));
		}
		
		System.out.println("INFO: Applied " + p.eventList.size() + " event(s) from Player: " + userInfo.userId);
		
		p.mSuccessCount = count;
		p.mReturnCode = PackageResponse.Success;
	}
	
	void DoPackageUpdateAccount(UserInfo userInfo, PackageUpdateAccount p, int appVersion, String appName)
	{
		UserInfo info = new UserInfo();
		info.userId = userInfo.userId;
		info.colour = p.colour;
		info.alias = p.alias;
		info.logo = p.logo;
		
		if (info.alias.length() > 32) info.alias = info.alias.substring(0, 31);
		
		boolean bResult = DBStuff.getInstance().UpdateUserInfo(info);
		
		if (bResult == true)
		{
			p.bAliasAlreadyInUse = false;
			System.out.println("INFO: Updated account info for " + info.userId);
		} else
		{
			p.bAliasAlreadyInUse = true;
			p.oldAlias = userInfo.alias;
			System.out.println("INFO: Failed to update account for user " + info.userId);
		}
		
		p.mReturnCode = PackageResponse.Success;
	}
	
	void DoPackageSendChat(UserInfo userInfo, PackageSendChat p, int appVersion, String appName)
	{
		System.out.println("[Chat Message] " + p.messageStr);
		
		ChatNotifier chatNotifier = new ChatNotifier();
		
		ChatNotifier.ChatResult result =		
				chatNotifier.SendChatMessage(p.messageStr, p.toUserId, p.toGameId, p.fromUserId, appName);
		
		p.bSent = result.bSent;
		p.bGCMUpdateRequired = false;
		
		p.mReturnCode = PackageResponse.Success;
	}
	
	void DoPackageSendInvite(UserInfo userInfo, PackageSendInvite p, int appVersion, String appName)
	{
		// check that the sending player isn't blocked 
		SyncList targetBlockList = SyncListDB.getInstance().GetSyncList(p.toUserId, "BlockList", appName);
		if (targetBlockList.GetList().contains(p.fromUserId))
		{
			System.out.println("[Invite] Blocked by " + p.toUserId + " from " + p.fromUserId);
			p.bSent = false;
			p.mReturnCode = PackageResponse.Success;
			return;
		}
		
		InviteNotifier notifier = new InviteNotifier();
		InviteNotifier.InviteResult result = notifier.SendInvite(p.toUserId, p.gameId, (int)userInfo.userId, appName);
		
		p.bSent = result.bSent;
		p.mReturnCode = PackageResponse.Success;
		
	}
	
	void DoPackageSyncList(UserInfo userInfo, PackageSyncList p, int appVersion, String appName)
	{
		// get the pre-existing sync-list (if it exists)
		SyncList list = SyncListDB.getInstance().GetSyncList((int)userInfo.userId, p.syncListName, appName);
		
		list.list.addAll(p.addList);
		list.list.removeAll(p.deleteList);
		
		//System.out.println("INFO: SyncList: " + userInfo.userId + " Tag: " + p.syncListName + " Items: " + list.list.toString());
		
		SyncListDB.getInstance().SetSyncList((int)userInfo.userId, p.syncListName, appName, list);
		
		p.syncList = list.list;
		
		p.mReturnCode = PackageResponse.Success;
	}
	
	void DoPackageSearchAlias(UserInfo userInfo, PackageSearchAlias p, int appVersion, String appName)
	{
		UserInfo searchResult = DBStuff.getInstance().SearchAlias(p.searchAlias);
		
		if (searchResult == null)
		{
			p.userId = -1;
			p.mReturnCode = PackageResponse.ErrorNoSearch;
			return;
		}
		
		p.userId = searchResult.userId;
		p.alias = searchResult.alias;
		p.colour = searchResult.colour;
		p.logo = searchResult.logo;
		
		p.mReturnCode = PackageResponse.Success;
	}
}
