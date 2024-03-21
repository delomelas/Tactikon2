package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventLeaveTransport extends IEvent
{
	public int mTransporter;
	public int mUnit;
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		IUnit transportUnit = tactikonState.GetUnit(mTransporter);
		IUnit unit = tactikonState.GetUnit(mUnit);
		
		if (transportUnit == null) throw new InvalidUpdateException();
		if (unit == null) throw new InvalidUpdateException();
		
		if (!transportUnit.mCarrying.contains(mUnit)) throw new InvalidUpdateException();
		// find the city we're in
		
		// and remove from the city it's in
		City inCity = null;
		for (City city : tactikonState.cities)
		{
			if (city.x == unit.GetPosition().x && city.y == unit.GetPosition().y)
			{
				inCity = city;
			}
		}
		if (inCity == null) throw new InvalidUpdateException();
		
		if (inCity.fortifiedUnits.size() >= tactikonState.MAX_UNITS_IN_CITY) throw new InvalidUpdateException();
		
		inCity.fortifiedUnits.add(mUnit);
		int index = transportUnit.mCarrying.indexOf(mUnit);
		transportUnit.mCarrying.remove(index);
		unit.mCarriedBy = -1;
		unit.mFortified = true;

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
