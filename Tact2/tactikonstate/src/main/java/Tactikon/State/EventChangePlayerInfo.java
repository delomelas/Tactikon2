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

public class EventChangePlayerInfo extends IEvent

{
	public long playerIdToChange = -1;
	public int r = -1;
	public int g = -1;
	public int b = -1;
	public String newAlias = "";
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		if (playerIdToChange != before.GetPlayerToPlay()) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// ----------------------------------------------------- EVENTS FOR THE PLAYER WHO'S TURN HAS JUST ENDED
		
		PlayerInfo info = tactikonState.GetPlayerInfo(playerIdToChange);
		
		if (r != -1)
		{
			info.r = r;
			info.g = g;
			info.b = b;
		}
		
		if (newAlias.length() > 1)
		{
			info.name = newAlias;
		}
				
		tactikonState.IncSequence();
		
		return tactikonState;
	}
	
	

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		// not currently serialisable - never used in network games
		
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		// not currently serialisable - never used in network games		
	}
	
}

