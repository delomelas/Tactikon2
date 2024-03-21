package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;

public class EventTimeout extends IEvent
{
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.WaitingForPlayers) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN HAS JUST ENDED
		
		tactikonState.gameState = IState.GameStatus.TimeOut;
		
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

