import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import Core.IState;
import Core.IState.GameStatus;
import Core.InvalidUpdateException;
import Tactikon.State.EventNewGame;
import Tactikon.State.TactikonNewGameOptions;
import Tactikon.State.TactikonNewGameOptions.MirrorType;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;


public class PlayerProfileThread extends Thread
{
	boolean bRunning = true;
	String mAppName;
	
	PlayerProfileThread(String appName)
	{
		mAppName = appName;
	}
	
	
	
	public void run()
	{
		// first, ensure the PlayersInGamesDB is up to date
		ArrayList<Integer> gameIdList = DBStuff.getInstance().GetAllGames(mAppName);
		
		for (Integer gameId : gameIdList)
		{
			IState state = DBStuff.getInstance().GetGame(gameId, 99999, mAppName);
			
			for (Integer playerId : state.GetPlayers())
			{
				if (playerId >= 0)
				{
					PlayersInGamesDB.getInstance().AddPlayerToGame(playerId, gameId, mAppName);
				}
			}
		}
		
		while(bRunning == true)
		{
			ArrayList<Integer> playerIdList = PlayerProfileUpdateQueueDB.getInstance().GetQueueItems(mAppName);
			// read the profile update queue
			// for each profile that should be updated
			for (Integer playerId : playerIdList)
			{
				
				
				System.out.println("[PlayerProfile]: Updating for UserId: " + playerId);
				// remove from the queue as it's been processed
				PlayerProfileUpdateQueueDB.getInstance().ClearFromQueue(playerId, mAppName);
				
				// get the games the player is in
				ArrayList<Integer> playerGameList = PlayersInGamesDB.getInstance().GetGamesForPlayer(playerId, mAppName);
				
				// calculate the stats
				// write to the player profile db
				int playing = 0;
				int played = 0;
				int won = 0;
				int lost = 0;
				int longestGame = 0;
				Set<Integer> opponents = new TreeSet<Integer>();
				
				TreeMap<Integer, Integer> myProfile = ProfileDB.getInstance().GetProfile(playerId, mAppName);
				
				for (Integer gameId : playerGameList)
				{
					IState state = DBStuff.getInstance().GetGame(gameId, 99999, mAppName);
					if (state.GetGameState() == GameStatus.GameOver)
					{
						played ++;
						if (state.IsPlayerAlive(playerId))
						{
							won++;
						} else
						{
							lost++;
						}
						
						opponents.addAll(state.GetPlayers());
					} else if (state.GetGameState() == GameStatus.InGame)
					{
						if (state.IsPlayerAlive(playerId))
						{
							playing ++;
						} else
						{
							played ++;
							lost ++;
						}
					}
					
					if (mAppName.compareTo("uk.co.eidolon.tact2") == 0)
					{
						TactikonState tactState = (TactikonState)state;
						if (tactState.powerGraph.containsKey(playerId))
						{
							if (tactState.powerGraph.get(playerId).size() > longestGame)
							{
								longestGame = tactState.powerGraph.get(playerId).size();
							}
						}
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
					Thread.yield();
				}
				System.out.println("Done fetching games.");
				
				// compute rank
				int rank =  0;											// private
				if (played > 10 && opponents.size() > 2) rank = 1;		// corporal
				if (played > 20 && opponents.size() > 4) rank = 2;		// sergeant
				if (played > 40 && opponents.size() > 8) rank = 3;     // Lieutenant 	
				if (played > 80 && opponents.size() > 16) rank = 4;     // Major
				if (played > 160 && opponents.size() > 32) rank = 5;    // Colonel
				if (played > 320 && opponents.size() > 48) rank = 6;    // Brigadier
				if (played > 640 && opponents.size() > 64) rank = 7;    // General
				if (played > 1000 && opponents.size() > 80) rank = 8;  
				
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_WINS, won);
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_PLAYING, playing);
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_LOSSES, lost);
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_LONGEST, longestGame);
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_PLAYED, played);
				ProfileDB.getInstance().AddToProfile(playerId, mAppName, ProfileDB.PROFILE_RANK, rank);
				
				if (myProfile != null)
				{
					if (myProfile.containsKey(ProfileDB.PROFILE_WINS) && myProfile.containsKey(ProfileDB.PROFILE_LOSSES) 
							&&  myProfile.get(ProfileDB.PROFILE_WINS) == won &&
						myProfile.get(ProfileDB.PROFILE_LOSSES) == lost)
						{
						// 	nothing else to do with this player as the wins/losses are still the same
							System.out.println("Rank unchanged, moving on.");
							continue;
						}
				}
				
				// now compute the rank order for all the players in this rank
				// get the other players with the same rank
				Set<Integer> playersInRank =  ProfileDB.getInstance().GetPlayersWithTag(ProfileDB.PROFILE_RANK, rank, mAppName);
				Map<Float, ArrayList<Integer>> playersInRankOrder = new TreeMap<Float, ArrayList<Integer>>();
				for (Integer rankPlayer : playersInRank)
				{
					TreeMap<Integer, Integer> profile = ProfileDB.getInstance().GetProfile(rankPlayer, mAppName);
					float rankWins = 0;
					float rankPlayed = 0;

					if (profile.containsKey(ProfileDB.PROFILE_WINS)) rankWins = profile.get(ProfileDB.PROFILE_WINS);
					if (profile.containsKey(ProfileDB.PROFILE_PLAYED)) rankPlayed = profile.get(ProfileDB.PROFILE_PLAYED);
					float score = 0;
					if (rankPlayed > 0) score = (rankWins / rankPlayed) * 10000.0f;
					
					if (playersInRankOrder.containsKey(score))
					{
						playersInRankOrder.get(score).add(rankPlayer);
					} else
					{
						ArrayList<Integer> newList = new ArrayList<Integer>();
						newList.add(rankPlayer);
						playersInRankOrder.put(score, newList);
					}
					
				}
				
				
				
				int positionInRank = playersInRank.size();
				int totalInRank = playersInRank.size();
				for (Entry<Float, ArrayList<Integer>> entry : playersInRankOrder.entrySet())
				{
					ArrayList<Integer> rankPlayers = entry.getValue();
					
					for (Integer rp : rankPlayers)
					{
						ProfileDB.getInstance().AddToProfile(rp, mAppName, ProfileDB.PROFILE_TOTAL_IN_RANK, totalInRank);
						ProfileDB.getInstance().AddToProfile(rp, mAppName, ProfileDB.PROFILE_POSITION_IN_RANK, positionInRank);
					
						positionInRank --;
						
						Thread.yield();
					}
				}
								
			}
			
			try
			{
				Thread.sleep(1000 * 60); // run once every sixty seconds
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
}


