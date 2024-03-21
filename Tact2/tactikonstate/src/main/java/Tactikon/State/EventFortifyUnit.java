package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventFortifyUnit extends IEvent
{
	public int unitIdToFortify;
	public int cityIdToFortifyIn;

	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		
		// check that it's a valid move for that unit
		TactikonState state = (TactikonState)before.CopyState();
		IUnit unit = state.GetUnit(unitIdToFortify);
		if (unit == null)
		{
			throw new InvalidUpdateException();
		}

		City city = state.GetCity(cityIdToFortifyIn);
		if (city == null)
		{
			throw new InvalidUpdateException();
		}
		if (city.playerId != unit.mUserId)
		{
			throw new InvalidUpdateException();
		}
		
		if (unit.mCarriedBy != -1)
		{
			throw new InvalidUpdateException();
		}
		
		if (unit.GetPosition().x != city.x ||
			unit.GetPosition().y != city.y)
		{
			throw new InvalidUpdateException();
		}
		
		if (city.fortifiedUnits.size() >= state.MAX_UNITS_IN_CITY)
		{
			throw new InvalidUpdateException();
		}
		
		unit.mFortified = true;
		city.fortifiedUnits.add(unit.mUnitId);
		
		for (Integer carriedId : unit.mCarrying)
		{
			IUnit carried = state.GetUnit(carriedId);
			carried.mFortified = true;
		}
		
		state.IncSequence();
		
		return state;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(unitIdToFortify);
		stream.writeInt(cityIdToFortifyIn);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		unitIdToFortify = stream.readInt();
		cityIdToFortifyIn = stream.readInt();
		
	}

}
