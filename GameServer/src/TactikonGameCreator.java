import java.util.ArrayList;
import java.util.Random;

import Core.InvalidUpdateException;
import Tactikon.State.EventNewGame;
import Tactikon.State.TactikonNewGameOptions;
import Tactikon.State.TactikonNewGameOptions.MirrorType;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;


public class TactikonGameCreator extends Thread
{
	boolean bRunning = true;
	
	public void run()
	{
		while(bRunning == true)
		{
			try
			{
				Thread.sleep(60 * 1000 * 30); // create a new game every thirty mins at most
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// count how many games are waiting for players
			
			ArrayList<Integer> gameIdList = DBStuff.getInstance().GetActiveGames("uk.co.eidolon.tact2");
			
			// if it's anything other than zero, loop again
			if (gameIdList.size() > 0) continue;
			
			System.out.println("[TactikonNewGameCreator] - Creating a new game as there aren't any at the moment.");
			TactikonNewGameOptions options = new TactikonNewGameOptions();
			Random rand = new Random();
			options.bForest = rand.nextBoolean();
			options.bLocalGame = false;
			options.bMountains = rand.nextBoolean();
			options.cities = rand.nextInt(3);
			options.fogOfWar = rand.nextBoolean();
			options.landMassRatio = (rand.nextInt() / 2.0f) + 0.25f;
			options.mapSeed = rand.nextInt();
			options.mapSize = 32;
			options.mirrorType = MirrorType.Both;
			int pl = rand.nextInt(3);
			options.numHumanPlayers = 2;
			if (pl == 0) options.numHumanPlayers = 4;
			
			options.scale = rand.nextInt(9) + 3;
			options.turnTimeOut = 24;
			options.winCondition = WinCondition.CaptureAllBases;
			
			TactikonState state = null;
			
			EventNewGame event = new EventNewGame(options);
			try
			{
				state = (TactikonState) event.updateState(state);
				state.lastUpdateTime = System.currentTimeMillis() / 1000;
			} catch (InvalidUpdateException ex)
			{
				System.out.println("[TactikonGameCreator] - Error creating new game.");
				continue;
			}
			
			long epoch = System.currentTimeMillis()/1000;
			long gameId = DBStuff.getInstance().AddNewGame(0, 0, epoch, 0, "uk.co.eidolon.tact2", state);
			
			System.out.println("[TactikonGameCreator]: Added game with gameId=["+gameId+"]");
					

		
		}
	}
}


