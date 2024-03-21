package uk.co.eidolon.tact2.AIWorldPlot;

import java.util.ArrayList;
import java.util.Map.Entry;

import Tactikon.State.City;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class TaskCaptureCityThisTurn extends ITask
{

	int targetCityId;
	
	TaskCaptureCityThisTurn(IUnit unit, TactikonState state, AIInfo info)
	{
		super(unit, state, info);
	}
	
		
	@Override
	boolean IsTaskOver()
	{
		if (mUnit.mFortified == true) return true;
		return false;
	}

	@Override
	boolean AttemptTask()
	{
		// generate a worldplot to uncaptured cities
		// get the best cost
		ArrayList<Position> unownedCities = new ArrayList<Position>();
		
		// if we can move from where we are now to capture a city, do it immediately
		ArrayList<Position> moves = mState.GetPossibleMoves(mUnit.mUnitId);
		for (Position move : moves)
		{
			for (City city : mState.cities)
			{
				if (move.x == city.x && move.y == city.y && city.playerId != mUnit.mUserId && city.fortifiedUnits.size() == 0)
				{
					EventMoveUnit event = new EventMoveUnit();
					event.mUnitId = mUnit.mUnitId;
					event.mFrom = mUnit.GetPosition();
					event.mTo = move;
					mInfo.mInjector.AddEvent(event);
					return true;
				}
			}
		}
		
		return false;

	}
	
}
