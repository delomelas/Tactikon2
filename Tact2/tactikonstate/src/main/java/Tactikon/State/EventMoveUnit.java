package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import Support.INewGameOptions;

import Core.IEvent;
import Core.IState.GameStatus;
import Core.InvalidUpdateException;
import Core.IState;

public class EventMoveUnit extends IEvent
{
	public Position mFrom, mTo;
	public int mUnitId;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	public EventMoveUnit()
	{
	}
	
	public EventMoveUnit(int unitId, Position from, Position to)
	{
		mFrom = from;
		mTo = to;
		mUnitId = unitId;
	}
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		
		IState newState = before.CopyState();
		
		// check that it's a valid move for that unit
		TactikonState state = (TactikonState)newState;
		
		IUnit unit = state.GetUnit(mUnitId);
		
		if (unit == null) throw new InvalidUpdateException();
		
		System.out.println("Unit: " + unit.getClass() + "(" + unit.mUnitId + ") From: " + mFrom.x + "," + mFrom.y +" To: " + mTo.x + "," + mTo.y);
		
		// is it the right player?
		if (unit.mUserId != state.playerToPlay)
		{
			//System.out.println("Unit: " + unit.getClass() + "(" + unit.mUnitId + ") From: " + mFrom.x + "," + mFrom.y +" To: " + mTo.x + "," + mTo.y);
			System.out.println("User: " + unit.mUserId + " PlayerToPlay: " + state.playerToPlay);
			throw new InvalidUpdateException();
		}

		// is the game in progress?
		if (state.GetGameState() != GameStatus.InGame) throw new InvalidUpdateException();
		
		// is this really a possible move for this unit?
		ArrayList<Position> possibleMoves = state.GetPossibleMoves(mUnitId);
		state.possibleMoveCache.clear();
		boolean bFound = false;
		for (Position pos : possibleMoves)
		{
			if (pos.x == mTo.x && pos.y == mTo.y)
			{
				bFound = true;
				break;
			}
		}
		if (bFound == false)
		{
			//System.out.println("Unit: " + unit.getClass() + "(" + unit.mUnitId + ") From: " + mFrom.x + "," + mFrom.y +" To: " + mTo.x + "," + mTo.y);
			System.out.println("Valid moves: " + possibleMoves.size());
			throw new InvalidUpdateException();
		}
		
		// did the unit really start from there?
		if (unit.GetPosition().x != mFrom.x || unit.GetPosition().y != mFrom.y)
			throw new InvalidUpdateException();
		
		if (unit.bMoved == true) throw new InvalidUpdateException();
		
		// okay then, we'll move the unit to the new position
		
		TactikonState tactikonNewState = (TactikonState)newState;
		
		ArrayList<Position> route = tactikonNewState.GetRoute(mUnitId, mTo);
		
		IUnit movedUnit = tactikonNewState.GetUnit(mUnitId);
		movedUnit.SetPosition(mTo.x, mTo.y);
		
		boolean bBoarded = false;
		
		// if we're moving out of a fortified city, update the city fortification data
		if (movedUnit.mFortified == true)
		{
			City fortifiedCity = null;
			for (City city : tactikonNewState.cities)
			{
				if (mFrom.x == city.x && mFrom.y == city.y)
				{
					fortifiedCity = city;
				}
			}
			if (fortifiedCity != null)
			{
				int index = fortifiedCity.fortifiedUnits.indexOf(movedUnit.mUnitId);
				if (index != -1)
				{
					fortifiedCity.fortifiedUnits.remove(index);
				}
			}
			
			movedUnit.mFortified = false;
			for (Integer carriedUnitId : movedUnit.mCarrying)
			{
				IUnit carriedUnit = tactikonNewState.GetUnit(carriedUnitId);
				carriedUnit.mFortified = false;
				
				for (Integer carriedCarriedUnitId : carriedUnit.mCarrying)
				{
					IUnit carriedCarriedUnit = tactikonNewState.GetUnit(carriedCarriedUnitId);
					carriedCarriedUnit.mFortified = false;
				}
			}
		}

		// we might have boarded another unit
		for (Entry <Integer, IUnit> entry : tactikonNewState.units.entrySet())
		{
			IUnit transporterUnit = entry.getValue();
			if (transporterUnit.GetPosition().x == movedUnit.GetPosition().x &&
				transporterUnit.GetPosition().y == movedUnit.GetPosition().y &&
				transporterUnit.CanCarry(movedUnit) &&
				transporterUnit.mFortified == false &&
				transporterUnit.mCarrying.size() < transporterUnit.CarryCapacity())
				{
					transporterUnit.mCarrying.add(movedUnit.mUnitId);
					bBoarded = true;
					
					if (movedUnit.mCarriedBy != -1) // handle the case where we're exiting one unit to board another
					{
						IUnit currentCarry = tactikonNewState.GetUnit(movedUnit.mCarriedBy);
						int index = currentCarry.mCarrying.indexOf(movedUnit.mUnitId);
						currentCarry.mCarrying.remove(index);
					}
					
					movedUnit.mCarriedBy = transporterUnit.mUnitId;
					movedUnit.mFortified = transporterUnit.mFortified;
					break;
				}
		}
		
		// we might have moved into a city
		if (bBoarded == false)
		{
			for (City city : tactikonNewState.cities)
			{
				if (city.x == mTo.x && city.y == mTo.y)
				{
					if (city.playerId == -1)
					{
						// capture if we can
						if (movedUnit.CanCaptureCity() == true)
						{
							city.playerId = movedUnit.mUserId;
							if (city.turnsToProduce == -1)
							{
								IUnit tank = new UnitTank();
								city.productionType = tank.getClass().getSimpleName();
								city.turnsToProduce = tank.GetProductionTime(city, tactikonNewState);
							}
						}
					}
					
					if (city.playerId == movedUnit.mUserId)
					{
						if (city.fortifiedUnits.size() >= tactikonNewState.MAX_UNITS_IN_CITY) throw new InvalidUpdateException();
						city.fortifiedUnits.add(movedUnit.mUnitId);
						movedUnit.mFortified = true;
						for (Integer carriedUnitId : movedUnit.mCarrying)
						{
							IUnit carriedUnit = tactikonNewState.GetUnit(carriedUnitId);
							carriedUnit.mFortified = true;
							
							for (Integer carriedCarriedUnitId : carriedUnit.mCarrying)
							{
								IUnit carriedCarriedUnit = tactikonNewState.GetUnit(carriedCarriedUnitId);
								carriedCarriedUnit.mFortified = true;
							}
						}
					}
				}
			}
		}

		
		// we might have exited another unit
		if (movedUnit.mCarriedBy != -1)
		{
			IUnit transporterUnit = tactikonNewState.GetUnit(movedUnit.mCarriedBy);
			if (mTo.x != transporterUnit.GetPosition().x || // check this isn't actually the unit we just boarded
				mTo.y != transporterUnit.GetPosition().y)
				{
					int index = transporterUnit.mCarrying.indexOf(movedUnit.mUnitId);
					transporterUnit.mCarrying.remove(index);
					movedUnit.mCarriedBy = -1;
				}
		}
		
		
		// we need to move other units with is if we're carrying things
		for (Integer unitId : movedUnit.mCarrying)
		{
			IUnit transportedUnit = tactikonNewState.GetUnit(unitId);
			transportedUnit.SetPosition(movedUnit.GetPosition().x, movedUnit.GetPosition().y);
			
			// the transportedUnit might be carrying as well
			for (Integer deeperUnitId : transportedUnit.mCarrying)
			{
				IUnit deeperTransportedUnit = tactikonNewState.GetUnit(deeperUnitId);
				deeperTransportedUnit.SetPosition(movedUnit.GetPosition().x, movedUnit.GetPosition().y);
			}
		}
		
		// if fog of war is switched on, update the fogmaps
		if (tactikonNewState.bFogOfWar == true)
		{
			tactikonNewState.UpdateFogMap(mUnitId);
		}
		
		// we might have captured a city
		for (City city : tactikonNewState.cities)
		{
			if (movedUnit.CanCaptureCity() == false) continue;
			if (city.x == movedUnit.GetPosition().x && city.y == movedUnit.GetPosition().y)
			{
				if (city.playerId != movedUnit.mUserId)
				{
					BubbleInfoCityLost bubbleInfo = new BubbleInfoCityLost(city.playerId, city.cityId);
					tactikonNewState.bubbles.add(bubbleInfo);
					BubbleInfoCityLost2 bubbleInfo2 = new BubbleInfoCityLost2(city.playerId, city.cityId);
					tactikonNewState.bubbles2.add(bubbleInfo2);

					city.playerId = movedUnit.mUserId;
					break;
				}
			}
		}
		
		// we'll burn some fuel
		if (movedUnit.GetMaxFuel() != -1)
		{
			if (route == null)
			{
				movedUnit.fuelRemaining --;
				if (movedUnit.fuelRemaining < 0) movedUnit.fuelRemaining = 0;
			} else
			{
				movedUnit.fuelRemaining -= (route.size()-1);
				if (movedUnit.fuelRemaining < 0) movedUnit.fuelRemaining = 0;
			}
		}
		
		if (movedUnit instanceof UnitSub == false || movedUnit.mConfig == 0)
		{
			
			// 	update the tracks for this unit
			Tracks tracks = new Tracks();
			tracks.numPoints = route.size();
			tracks.renderPoints = route.size();
			tracks.x = new byte[tracks.numPoints];
			tracks.y = new byte[tracks.numPoints];
			tracks.playerId = movedUnit.mUserId;
			for (int i = 0; i < tracks.numPoints; ++i)
			{
				tracks.x[i] = (byte)route.get(i).x;
				tracks.y[i] = (byte)route.get(i).y;
			}
			tactikonNewState.tracks.put(Long.valueOf(movedUnit.mUnitId), tracks);
			
			if (movedUnit instanceof UnitSub) tracks.type = 1;
			if (movedUnit instanceof UnitCarrier) tracks.type = 1;
			if (movedUnit instanceof UnitBattleship) tracks.type = 1;
			if (movedUnit instanceof UnitBoatTransport) tracks.type = 1;
			if (movedUnit instanceof UnitInfantry) tracks.type = 2;
			if (movedUnit instanceof UnitTank) tracks.type = 3;
		}
		
		movedUnit.bMoved = true;
		
		tactikonNewState.CheckForWinner();
		
		// TODO: and mark the unit as no longer able to move
		tactikonNewState.IncSequence();
		
		return tactikonNewState;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(mUnitId);
		stream.writeInt(mFrom.x);
		stream.writeInt(mFrom.y);
		stream.writeInt(mTo.x);
		stream.writeInt(mTo.y);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		if (version >= VERSION_FIRST)
		{
			mUnitId = stream.readInt();
			int x1 = stream.readInt();
			int y1 = stream.readInt();
			int x2 = stream.readInt();
			int y2 = stream.readInt();
			
			mFrom = new Position(x1, y1);
			mTo = new Position(x2, y2);
		}
		
	}
}
	

