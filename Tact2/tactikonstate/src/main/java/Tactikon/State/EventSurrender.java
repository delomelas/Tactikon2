package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;
import Core.PlayerInfo;

public class EventSurrender extends IEvent
{
	public int playerIdToSurrender = -1;
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException("Null state");
		if (before.GetGameState() != IState.GameStatus.InGame)
		{
			throw new InvalidUpdateException("Not in game, can't surrender");
		}
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		

		
		// check player is in this game
		if (tactikonState.players.contains(playerIdToSurrender) == false)
		{
			throw new InvalidUpdateException();
		}
		
		// remove all the players units
		ArrayList<IUnit> killUnits = new ArrayList<IUnit>();
		
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerIdToSurrender)
			{
				killUnits.add(unit);
			}
		}
		
		for (IUnit unit : killUnits)
		{
			tactikonState.KillUnit(unit.mUnitId);
		}
		
		// release any owned cities
		for (City city : tactikonState.cities)
		{
			if (city.playerId == playerIdToSurrender)
			{
				city.playerId = -1;
				city.bIsHQ = false;
				city.fortifiedUnits.clear();
				city.turnsToProduce = -1;
				city.productionType = "";
			}
		}
		
		// move on to the next player if the surrendering player was next to play
		if (tactikonState.playerToPlay == playerIdToSurrender)
		{
			int playerIndex = tactikonState.players.indexOf(tactikonState.playerToPlay);
		
			// move past the current player
			playerIndex ++;
			if (playerIndex == tactikonState.GetNumJoinedPlayers()) playerIndex = 0;
			
			// find the next alive player
			while (tactikonState.PlayerIsAlive(tactikonState.players.get(playerIndex)) == false)
			{
				playerIndex ++;
				if (playerIndex == tactikonState.GetNumJoinedPlayers()) playerIndex = 0;
			}
			
			// and set them as the next player to play
			tactikonState.playerToPlay = tactikonState.players.get(playerIndex);
		}
		
		PlayerInfo info = tactikonState.GetPlayerInfo(playerIdToSurrender);
		tactikonState.gameStateMessage = info.name + " surrendered.";
		
		tactikonState.CheckForWinner();
		
		tactikonState.IncSequence();
		
		return tactikonState;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerIdToSurrender);
		
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		playerIdToSurrender = stream.readInt();
		
	}
	
}
