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

public class EventWithdrawPlayer extends IEvent

{
	public long playerIdToWithdraw = -1;
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		if (playerIdToWithdraw != before.GetPlayerToPlay()) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN HAS JUST ENDED
		
		ArrayList<IUnit> killUnits = new ArrayList<IUnit>();
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerIdToWithdraw) killUnits.add(unit);
		}
		
		for (IUnit unit : killUnits)
		{
			tactikonState.KillUnit(unit.mUnitId);
		}
		
		for (City city : tactikonState.cities)
		{
			if (city.playerId == playerIdToWithdraw)
			{
				city.playerId = -1;
				city.fortifiedUnits.clear();
				city.productionType = "";
				city.turnsToProduce = -1;
				city.bIsHQ = false;
			}
		}
		
		PlayerInfo info = tactikonState.GetPlayerInfo(playerIdToWithdraw);
		tactikonState.gameStateMessage = info.name + " missed too many turns.";
				
		// ------------------ WORK OUT WHICH PLAYER IS NEXT
		int playerIndex = tactikonState.players.indexOf(tactikonState.playerToPlay);
		
		// mark the player as having had their turn skipped
		int skippedTurns = tactikonState.missedTurnCount.get(playerIndex);
		skippedTurns ++;
		tactikonState.missedTurnCount.set(playerIndex, skippedTurns);
		
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
		EventHelpers eventHelpers = new EventHelpers();
		eventHelpers.DoPlayerStartTurnActions(tactikonState);
		
		
		tactikonState.CheckForWinner();
		
		tactikonState.IncSequence();
		
		return tactikonState;
	}
	
	

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		
		
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		
	}
	
}

