package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventChangeUnitConfig extends IEvent
{
	public int mUnit;
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		IUnit unit = tactikonState.GetUnit(mUnit);
		
		if (unit == null) throw new InvalidUpdateException();
		
		if (unit.mUserId != tactikonState.GetPlayerToPlay()) throw new InvalidUpdateException();
		
		if (unit.bChangedConfig == true) throw new InvalidUpdateException();
		
		if (unit.CanChangeConfig(tactikonState) == false) throw new InvalidUpdateException();
		
		if (unit.mConfig == 0)
		{
			unit.mConfig = 1;
		} else
		{
			unit.mConfig = 0;
		}
		
		unit.bChangedConfig = true;
		
		
		tactikonState.IncSequence();
				
		return tactikonState;
	}
	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{

		stream.writeInt(mUnit);
		
	}
	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{

		mUnit = stream.readInt();
	}
	
	

}
