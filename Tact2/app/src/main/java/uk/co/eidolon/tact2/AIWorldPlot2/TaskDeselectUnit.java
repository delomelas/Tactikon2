package uk.co.eidolon.tact2.AIWorldPlot2;

import Tactikon.State.City;
import Tactikon.State.EventFortifyUnit;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;

public class TaskDeselectUnit extends ITask
{

	protected TaskDeselectUnit(IUnit unit, TactikonState state, AIInfo info)
	{
		super(unit, state, info);
		// TODO Auto-generated constructor stub
	}

	@Override
	boolean AttemptTask()
	{
		if (mUnit.mFortified == true) return false;
		if (mUnit.mCarriedBy != -1) return false;
		for (City city : mState.cities)
		{
			if (city.x == mUnit.GetPosition().x && city.y == mUnit.GetPosition().y && city.fortifiedUnits.size() < mState.MAX_UNITS_IN_CITY)
			{
				EventFortifyUnit fortify = new EventFortifyUnit();
				fortify.cityIdToFortifyIn = city.cityId;
				fortify.unitIdToFortify = mUnit.mUnitId;
				
				mInfo.mInjector.AddEvent(fortify);
				return true;
			}
		}
		
		return false;
	}

	@Override
	boolean IsTaskOver()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub

	}

}
