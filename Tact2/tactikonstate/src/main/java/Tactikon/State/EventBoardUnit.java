package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventBoardUnit extends IEvent
{
	public int mTransporter;
	public int mUnit;
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		// remove from a transport if it's currently in one
		
		IUnit transportUnit = tactikonState.GetUnit(mTransporter);
		if (transportUnit == null) throw new InvalidUpdateException();
		IUnit unit = tactikonState.GetUnit(mUnit);
		if (unit == null) throw new InvalidUpdateException();
		
		if (unit.GetPosition().x != transportUnit.GetPosition().x || unit.GetPosition().y != transportUnit.GetPosition().y)
			throw new InvalidUpdateException();
		
		IUnit currentTransporter = tactikonState.GetUnit(unit.mCarriedBy);
		
		if (currentTransporter != null)
		{
			int index = currentTransporter.mCarrying.indexOf(unit.mUnitId);
			currentTransporter.mCarrying.remove(index);
		}
		
		
		
		
		if (transportUnit.mUserId != tactikonState.playerToPlay) throw new InvalidUpdateException();
		
		if (transportUnit.CanCarry(unit) == false) throw new InvalidUpdateException();
		if (transportUnit.mCarrying.size() >= transportUnit.CarryCapacity()) throw new InvalidUpdateException();
		
		transportUnit.mCarrying.add(mUnit);
		unit.mCarriedBy = mTransporter;
		unit.mFortified = transportUnit.mFortified;
		
		// and remove from the city it's in
		for (City city : tactikonState.cities)
		{
			if (city.fortifiedUnits.contains(unit.mUnitId))
			{
				int index = city.fortifiedUnits.indexOf(unit.mUnitId);
				city.fortifiedUnits.remove(index);
			}
		}
		

		tactikonState.IncSequence();
				
		return tactikonState;
	}
	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mTransporter);
		stream.writeInt(mUnit);
		
	}
	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		mTransporter = stream.readInt();
		mUnit = stream.readInt();
	}
	
	

}
