package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;

public class EventSkipTurn extends IEvent
{
	public long playerIdToSkip = -1;
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		if (playerIdToSkip != before.GetPlayerToPlay()) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN HAS JUST ENDED
		
		EventHelpers eventHelpers = new EventHelpers();
		
		eventHelpers.DoPlayerEndTurnActions(tactikonState);
				
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

