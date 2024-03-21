package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;

public class EventEndTurnWithPlayerId extends IEvent
{
	
	public long playerId;

	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		if (before.GetPlayerToPlay() != playerId) throw new InvalidUpdateException();
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN HAS JUST ENDED
		
		EventHelpers eventHelpers = new EventHelpers();
		
		eventHelpers.DoPlayerEndTurnActions(tactikonState);
		
		// mark the player as having taken their turn
		tactikonState.missedTurnCount.set(tactikonState.players.indexOf(tactikonState.playerToPlay), 0);
				
		// ------------------ WORK OUT WHICH PLAYER IS NEXT
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
				
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN IS JUST STARTING
		eventHelpers.DoPlayerStartTurnActions(tactikonState);

		// hack to remove fortified units that aren't actually in the city
		for (City city : tactikonState.cities)
		{
			for (Integer unitId : city.fortifiedUnits)
			{
				IUnit unit = tactikonState.GetUnit(unitId);
				if (unit == null)
				{
					int index = city.fortifiedUnits.indexOf(unitId);
					city.fortifiedUnits.remove(index);
					break;
				}
				
				if (unit.GetPosition().x != city.x || unit.GetPosition().y != city.y)
				{
					int index = city.fortifiedUnits.indexOf(unitId);
					city.fortifiedUnits.remove(index);
					break;
				}
			}
		}
		
		tactikonState.gameStateMessage = "";
		
		tactikonState.CheckForWinner();
		
		tactikonState.IncSequence();
		
		return tactikonState;
	}
	
	

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeLong(playerId);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		playerId = stream.readLong();
	}
	
}

