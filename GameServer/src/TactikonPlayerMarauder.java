import java.util.ArrayList;
import java.util.Date;

import Tactikon.State.EventSkipTurn;
import Tactikon.State.EventTimeout;
import Tactikon.State.EventWithdrawPlayer;
import Tactikon.State.TactikonState;

import Core.IState;
import Core.IState.GameStatus;
import Core.InvalidUpdateException;


public class TactikonPlayerMarauder extends Thread
{
	
	boolean bRunning = true;
	public void run()
	{
		
		while(bRunning == true)
		{
			// get a list of all games
			// for each game
			// - get the latest state
			// - compare the lastupdatetime with the time now
			// - if it's greater than the game timeout value, then 
			//   - enter a skip turn event
			//   - update and notify as normal
			
			// client needs to clear notification if a turn is skipped 
			
			
			
			System.out.println("Tactikon Player Marauder now working...");
			
			ArrayList<Integer> gameList = DBStuff.getInstance().GetActiveGames("uk.co.eidolon.tact2");
			
			System.out.println("[Marauder] " + gameList.size() + " active games.");
			
			for (Integer gameId : gameList)
			{
				
				IState state = DBStuff.getInstance().GetGame(gameId, 99999, "uk.co.eidolon.tact2");
				if (state == null) continue;
				TactikonState tactState = (TactikonState)state;
				
				if (tactState.GetGameState() != GameStatus.InGame) continue; 
				
				long stateLastUpdate = tactState.GetLastUpdateTime();
				long timeNow = System.currentTimeMillis() / 1000;
				
				//Date stateUpdateDate = new Date(stateLastUpdate * 1000);
				
				if (timeNow - stateLastUpdate > tactState.maxTurnSeconds)
				{
					long playerToPlay = tactState.GetPlayerToPlay();
					int playerIndex = tactState.players.indexOf((int)playerToPlay);
					
					int numPlayers = tactState.players.size();
					int turns = tactState.GetSequence();
					
					boolean withdrawwNow = (turns < numPlayers * 3);
					
					System.out.println("[Marauder] - skipped turns = " + tactState.missedTurnCount.get(playerIndex));
					
					if (tactState.missedTurnCount.get(playerIndex) < 2 && withdrawwNow == false)
					{
						EventSkipTurn skipTurnEvent = new EventSkipTurn();
						skipTurnEvent.playerIdToSkip = tactState.GetPlayerToPlay();
						
						try
						{
							TactikonState newState = (TactikonState)skipTurnEvent.updateState(tactState);
							newState.lastUpdateTime = System.currentTimeMillis() / 1000;
							if (DBStuff.getInstance().UpdateGame(gameId, newState, "uk.co.eidolon.tact2", true) == true)
							{
								System.out.println("[Marauder] - skipped a turn for gameId " + gameId + " UserId: " + skipTurnEvent.playerIdToSkip);
							} else
							{
								System.out.println("[Marauder] - error updating state while skipping turn for gameId");
							}
						} catch (InvalidUpdateException e)
						{	
							System.out.println("[Marauder] - Error applying skip turn event for " + gameId);
						}
					} else // withdraw the player
					{
						EventWithdrawPlayer withdrawPlayerEvent = new EventWithdrawPlayer();
						
						withdrawPlayerEvent.playerIdToWithdraw = tactState.GetPlayerToPlay();
						
						try
						{
							TactikonState newState = (TactikonState)withdrawPlayerEvent.updateState(tactState);
							newState.lastUpdateTime = System.currentTimeMillis() / 1000;
							if (DBStuff.getInstance().UpdateGame(gameId, newState, "uk.co.eidolon.tact2", true) == true)
							{
								System.out.println("[Marauder] - withdrawn player " + withdrawPlayerEvent.playerIdToWithdraw + "  from game " + gameId);
							} else
							{
								System.out.println("[Marauder] - error updating state while withdrawing a player for gameId");
							}
						} catch (InvalidUpdateException e)
						{	
							System.out.println("[Marauder] - Error applying withdraw player event for " + gameId);
						}
					}
					
				}

			}
			
			gameList = DBStuff.getInstance().GetWaitingGames("uk.co.eidolon.tact2");
			for (Integer gameId : gameList)
			{
				
				
				IState state = DBStuff.getInstance().GetGame(gameId, 99999, "uk.co.eidolon.tact2");
				if (state == null) continue;
				TactikonState tactState = (TactikonState)state;
				
				if (tactState.GetGameState() != GameStatus.WaitingForPlayers) continue; 
				
				long stateLastUpdate = tactState.GetLastUpdateTime();
				long timeNow = System.currentTimeMillis() / 1000;
				
				//Date stateUpdateDate = new Date(stateLastUpdate * 1000);
				
				if (timeNow - stateLastUpdate > 60*60*24*7) // one week
				{
					System.out.println("[Marauder] - Timeout for GameID " + gameId);
					EventTimeout timeOut = new EventTimeout();
					
					try
					{
						IState newState = timeOut.updateState(tactState);
						newState.lastUpdateTime = System.currentTimeMillis() / 1000;
						if (DBStuff.getInstance().UpdateGame(gameId, newState, "uk.co.eidolon.tact2", true) == true)
						{
							System.out.println("[Marauder] - Timeout applied for game " + gameId);
						} else
						{
							System.out.println("[Marauder] - error updating state while timeout being applied");
						}
					} catch (InvalidUpdateException e)
					{	
						System.out.println("[Marauder] - error applying timeout event for gameId " + gameId);
					}
				}
			}

			try
			{
				Thread.sleep(5000*60);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
	
				bRunning = false;
			}
		}
		
	}

}
