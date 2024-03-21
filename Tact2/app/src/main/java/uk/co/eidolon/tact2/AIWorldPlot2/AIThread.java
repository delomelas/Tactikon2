package uk.co.eidolon.tact2.AIWorldPlot2;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeSet;



//import android.util.Log;

import uk.co.eidolon.tact2.AIHelpers.IAIThread;
import uk.co.eidolon.tact2.AIWorldPlot2.AIInfo.Task;
import uk.co.eidolon.tact2.AIWorldPlot2.AIInfo.TaskType;

import Tactikon.State.Battle;
import Tactikon.State.Battle.Outcome;
import Tactikon.State.EventBoardUnit;
import Tactikon.State.City;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.EventChangeProduction;
import Tactikon.State.EventEndTurnWithPlayerId;
import Tactikon.State.EventLeaveTransport;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBattleship;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitBomber;
import Tactikon.State.UnitFighter;
import Tactikon.State.UnitHelicopter;
import Tactikon.State.UnitInfantry;
import Tactikon.State.UnitSub;
import Tactikon.State.UnitTank;
import Core.EventInjector;
import Core.IEvent;
import Core.IEventListener;
import Core.IState;
import Core.IState.GameStatus;
import Core.StateEngine;

public class AIThread extends IAIThread
{
	volatile private TactikonState mState;
	
	public float varAttackProbRequired = 0.1f;
	
	public float varMoveAttackProbRequired = 0.5f;
	
	class EventListener extends IEventListener
	{
		@Override
		public void HandleQueuedEvent(IEvent event, IState before, final IState after)
		{
			final TactikonState state = (TactikonState)after;
		
			if (event instanceof EventEndTurnWithPlayerId && state.playerToPlay == myPlayerId)
			{
				onStartTurn(state);
			}
			
			mState = state;
		}
	}
	
	EventInjector mInjector;
	EventListener mListener;
	
	int myPlayerId;
	
	ArrayList<IUnit> mUnitList = new ArrayList<IUnit>();
	
	AIInfo info = null;
	
	StateEngine mEngine;
	
	public AIThread(StateEngine stateEngine, int playerId)
	{
		mListener = new EventListener();
		stateEngine.AddListener(mListener);
		mEngine = stateEngine;
		mInjector = new EventInjector(stateEngine);
		
		myPlayerId = playerId;
		
		mState = (TactikonState)stateEngine.GetState();
		
		info = new AIInfo(mInjector);
		
		onStartTurn(mState);
		
	}
	
	void onStartTurn(TactikonState state)
	{
		info.cityUnitCaptureMap.clear();
		info.unitTaskAssignedMap.clear();
		
		info.GenerateZoneMap(state,  myPlayerId);
		info.GenerateBlockMap(state, myPlayerId);
		info.GenerateDangermap(state, myPlayerId);
		info.GenerateLandmassMap(state, myPlayerId);
		info.GenerateMyUnitsMap(state, myPlayerId);
		
	}
	
	void onEndTurn(TactikonState state)
	{
		
	}
	
	
	@Override
	public void run()
	{
		
		while (stop == false)
		{
			while (mState.playerToPlay != myPlayerId || mState.GetGameState() != GameStatus.InGame && stop == false)
			{
				if (stop == true) break;
				try
				{
					Thread.sleep(2);
				} catch (InterruptedException e)
				{
				}
				if (stop == false) mListener.PumpQueue();
				
			}
			
			if (mState.GetGameState() != GameStatus.InGame) break;
			
			while(mState.playerToPlay == myPlayerId && mState.GetGameState() == GameStatus.InGame && stop == false)
			{
				if (stop == false) TickAI(mState);
				
				while (mListener.EventWaiting() == true && stop == false)
				{
 					mListener.PumpQueue();
				}
			}
			if (stop == false) onEndTurn(mState);
		}
		mEngine.RemoveListener(mListener);
		stopped = true;
	}
	
	boolean NearbyCitiesToConquer(City myCity, TactikonState state)
	{
		// if the nearest non-player city is unowned
		City nearestNonPlayerCity = null;
		int bestDist = 999;
		for (City city : state.cities)
		{
			if (city.playerId != myPlayerId)
			{
				int dist = Math.abs(city.x - myCity.x) + Math.abs(city.y - myCity.y);
				if (dist < bestDist)
				{
					nearestNonPlayerCity = city;
					bestDist = dist;
				}
			}
		}
		if (nearestNonPlayerCity == null) return false;
		if (nearestNonPlayerCity.playerId == -1) return true;
		
		return false;
	}
	
	boolean CityInSafeArea(City myCity, TactikonState state)
	{
		// if the two nearest cities are player-owned
		City nearestCity = null;
		int bestDist = 999;
		for (City city : state.cities)
		{
			if (city == myCity) continue;
			int dist = Math.abs(city.x - myCity.x) + Math.abs(city.y - myCity.y);
			if (dist < bestDist)
			{
				nearestCity = city;
				bestDist = dist;
			}
		}
		if (nearestCity.playerId != myPlayerId && nearestCity.playerId != -1) return false;
		
		int safeDist = bestDist * 3;
		for (City city : state.cities)
		{
			if (city == myCity) continue;
			if (city.playerId == -1 || city.playerId == myPlayerId) continue;
			int dist = Math.abs(city.x - myCity.x) + Math.abs(city.y - myCity.y);
			if (dist < safeDist) return false;
			
		}
		
		return true;
	}
	
	boolean EnemyUnitWithin(City city, TactikonState state, int maxDist)
	{
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == myPlayerId) continue;
			
			int dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			if (dist < maxDist) return false;
		}
		
		return true;
	}
	
	boolean EnemyHasUnitWithin(City city, TactikonState state, Class unitType, int maxDist)
	{
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == myPlayerId) continue;
			
			if (unit.getClass() != unitType) continue;
			
			
			int dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			if (dist < maxDist) return false;
		}
		
		return true;
	}
	
	boolean NoUnitWithin(City city, TactikonState state, Class unitType, int maxDist)
	{
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != myPlayerId) continue;
			
			if (unit.getClass() != unitType) continue;
			
			int dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			if (dist < maxDist) return false;
		}
		
		return true;
	}
	
	void SetProductionEvent(City city, TactikonState state, Class unitType)
	{
		EventChangeProduction event = new EventChangeProduction();
		event.cityId = city.cityId;
		event.unitClass = unitType.getSimpleName();
		mInjector.AddEvent(event);
		
		//System.out.println("Producing: " + unitType.getSimpleName());
	}
	
	boolean UnitInCity(Class unitType, City city, TactikonState state)
	{
		for (Integer fortUnitId : city.fortifiedUnits)
		{
			IUnit unit = state.GetUnit(fortUnitId);
			if (unit.getClass() == unitType) return true;
		}
		return false;
	}
	
	ArrayList<Integer> GetCitiesOnAdjacentLandmasses(City city, TactikonState state)
	{
		ArrayList<Integer> cityList = new ArrayList<Integer>();
		if (city.isPort == false) return cityList;
	
		
		
		Set<Integer> seaMassList = new TreeSet<Integer>();
		if (city.x > 0 && state.map[city.x-1][city.y] == TactikonState.TileType_Water) seaMassList.add(info.massMap[city.x-1][city.y]);
		if (city.y > 0 && state.map[city.x][city.y-1] == TactikonState.TileType_Water) seaMassList.add(info.massMap[city.x][city.y-1]);
		if (city.x < state.mapSize - 1 && state.map[city.x+1][city.y] == TactikonState.TileType_Water) seaMassList.add(info.massMap[city.x+1][city.y]);
		if (city.y < state.mapSize - 1 && state.map[city.x][city.y+1] == TactikonState.TileType_Water) seaMassList.add(info.massMap[city.x][city.y+1]);
		
		// now find their neighbours
		Set<Integer> neighbourLandmasses = new TreeSet<Integer>();
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (seaMassList.contains(info.massMap[x][y]))
				{
					// found some of the sea that ajoins the port
					if (x > 0 && info.IsLand(x - 1, y, state.map))neighbourLandmasses.add(info.massMap[x-1][y]); 
					if (y > 0 && info.IsLand(x, y - 1, state.map))neighbourLandmasses.add(info.massMap[x][y-1]);
					if (x < state.mapSize - 1 && info.IsLand(x + 1, y, state.map))neighbourLandmasses.add(info.massMap[x+1][y]);
					if (y < state.mapSize - 1 && info.IsLand(x, y + 1, state.map))neighbourLandmasses.add(info.massMap[x][y+1]);
				}
			}
		}
		neighbourLandmasses.remove(info.massMap[city.x][city.y]);
		
		for (City otherCity : state.cities)
		{
			if (neighbourLandmasses.contains(info.massMap[otherCity.x][otherCity.y]))
			{
				cityList.add(otherCity.cityId);
			}
		}
		
		
		return cityList;
	}
	
	boolean CanReachEnemyCities(City city, TactikonState state)
	{
		ArrayList<Integer> cities = GetCitiesOnAdjacentLandmasses(city, state);
		
		for (Integer cityId : cities)
		{
			City otherCity = state.GetCity(cityId);
			if (otherCity.playerId == myPlayerId || otherCity.playerId != -1) continue;
			
			if (otherCity.isPort == true) return true;
		}
		
		return false;
	}
	
	boolean UnownedCitiesOnAdjacentLandmass(City city, TactikonState state)
	{
		ArrayList<Integer> accessibleCityes = GetCitiesOnAdjacentLandmasses(city, state);
		for (Integer otherCityId : accessibleCityes)
		{
			City otherCity = state.GetCity(otherCityId);
			if (otherCity.playerId == -1) return true;
		}
		
		return false;
	}
	
	int CountLandUnitsInCity(City city, TactikonState state)
	{
		int num = 0;
		for (Integer unitId : city.fortifiedUnits)
		{
			IUnit unit = state.GetUnit(unitId);
			if (unit instanceof UnitInfantry) num++;
			if (unit instanceof UnitTank) num++;
		}
		return num;
	}
	
	void SetProductionForCity(TactikonState state, City city)
	{
		// if we're in the expansion zone, just produce capturing facilities
		if (info.zoneMap[city.x][city.y] == AIInfo.ZONE_EXPANSION)
		{
			if (UnitInCity(UnitHelicopter.class, city, state))
			{ 
				SetProductionEvent(city, state, UnitInfantry.class);
				return;
			}
			
			if (UnitInCity(UnitBoatTransport.class, city, state))
			{
				SetProductionEvent(city, state, UnitTank.class);
				return;
			}
			
			
			if (NoUnitWithin(city, state, UnitHelicopter.class, 10))
			{
				// then build a helicopter
				SetProductionEvent(city, state, UnitHelicopter.class);
				return;
			}
			
			// if it's worthwhile building boats, build one
			// some unowned cities need to exist adjactent to my cities landmass
			if (UnownedCitiesOnAdjacentLandmass(city, state) && NoUnitWithin(city, state, UnitBoatTransport.class, 10))
			{
				SetProductionEvent(city, state, UnitBoatTransport.class);
				//System.out.println("Setting production to Boat");
				return;
			}
			
			
			
			SetProductionEvent(city, state, UnitTank.class);
			return;
		}
		
		// if there's no infantry or tank nearby, produce them
		if (NoUnitWithin(city, state, UnitInfantry.class, 8))
		{
			SetProductionEvent(city, state, UnitInfantry.class);
			return;
		}
		if (NoUnitWithin(city, state, UnitTank.class, 12))
		{
			SetProductionEvent(city, state, UnitTank.class);
			return;
		}
		
		// if there's a tank in the city, and it's useful to produce boats, produce a boat
		if (city.isPort == true && UnitInCity(UnitTank.class, city, state) && CanReachEnemyCities(city, state) && NoUnitWithin(city, state, UnitBoatTransport.class, 6))
		{
			SetProductionEvent(city, state, UnitBoatTransport.class);
			return;
		}
		
		// if there's infantry in the city, produce a chopper
		if (UnitInCity(UnitInfantry.class, city, state) && NoUnitWithin(city, state, UnitHelicopter.class, 6))
		{
			SetProductionEvent(city, state, UnitHelicopter.class);
			return;
		}

		if (info.zoneMap[city.x][city.y] == AIInfo.ZONE_ENEMY)
		{
			SetProductionEvent(city, state, UnitTank.class);
			return;
		}
		
		if (info.zoneMap[city.x][city.y] == AIInfo.ZONE_SAFE)
		{
			if (city.isPort == true && NoUnitWithin(city, state, UnitBattleship.class, 32) && CanReachEnemyCities(city, state))
			{
				SetProductionEvent(city, state, UnitBattleship.class);
				return;
			}
			
			if (EnemyHasUnitWithin(city, state, UnitBattleship.class, 24) && NoUnitWithin(city, state, UnitBomber.class, 16))
			{
				SetProductionEvent(city, state, UnitBomber.class);
				return;
			}
	
			
			if (EnemyHasUnitWithin(city, state, UnitBomber.class, 24) && NoUnitWithin(city, state, UnitFighter.class, 16))
			{
				SetProductionEvent(city, state, UnitFighter.class);
				return;
			}
			
			if (EnemyHasUnitWithin(city, state, UnitHelicopter.class, 24) && NoUnitWithin(city, state, UnitFighter.class, 16))
			{
				SetProductionEvent(city, state, UnitFighter.class);
				return;
			}
			
			if (EnemyHasUnitWithin(city, state, UnitFighter.class, 24) && NoUnitWithin(city, state, UnitFighter.class, 16))
			{
				SetProductionEvent(city, state, UnitFighter.class);
				return;
			}
			
			if (city.isPort && CanReachEnemyCities(city, state) && EnemyHasUnitWithin(city, state, UnitBoatTransport.class, 16) && NoUnitWithin(city, state, UnitSub.class, 14))
			{
				SetProductionEvent(city, state, UnitSub.class);
				return;
			}
			
		}
		// if we've got to here, set production to something random, just to mix things up a bit
		
		Random rand = new Random();
		int num = rand.nextInt(4);
		if (city.isPort && CanReachEnemyCities(city, state))
		{
			num = rand.nextInt(6);
		}
		if (num == 0) 
		{
			SetProductionEvent(city, state, UnitBomber.class);
		} else if (num == 1)
		{
			SetProductionEvent(city, state, UnitTank.class);
		} else if (num == 2)
		{
			SetProductionEvent(city, state, UnitFighter.class);
		} else if (num == 3)
		{
			SetProductionEvent(city, state, UnitInfantry.class);
		} else if (num == 4)
		{
			SetProductionEvent(city, state, UnitBattleship.class);
		} else if (num == 5)
		{
			SetProductionEvent(city, state, UnitSub.class);
		}
		
	}
	
	void SetProduction(int playerId, TactikonState state)
	{
		for (City city : state.cities)
		{
			if (city.playerId == playerId)
			{
				if (city.turnsToProduce == -1 || city.bHasProduced == true)
				{
					SetProductionForCity(state, city);
					continue;
				}
			}
		}
	}
	
	public void EndTurnEvent(int playerId, TactikonState state)
	{
		SetProduction(playerId, state);
		
		EventEndTurnWithPlayerId event = new EventEndTurnWithPlayerId();
		event.playerId = playerId;
		mInjector.AddEvent(event);
	}
	
	
	// do everything that we can quickly and easily decide to do right now
	// - should include high-chance attacks, captureing we can do this turn, etc
	boolean DoObviousMoves(IUnit unit, TactikonState state)
	{
		ArrayList<ITask> taskList = new ArrayList<ITask>();
		if (unit.bMoved == false && unit.CanCaptureCity() == true)
		{
			TaskCaptureCityThisTurn task = new TaskCaptureCityThisTurn(unit, state, info);
			taskList.add(task);
		}
		
		TaskDeselectUnit tastDeselectUnit = new TaskDeselectUnit(unit, state, info);
		taskList.add(tastDeselectUnit);
		
		for (ITask task : taskList)
		{
			boolean bResult = task.AttemptTask();
			if (bResult == true)
			{
				return true;
			}
		}
		return false;
		
	}
	
	
	class CityCaptureInfo
	{
		int cityId;
		int turns;
		int unitId;
		
		int transportId = -1;
		WorldPlot transportWorldPlot = null;
		Position transTarget = null;
	}
	
	City GetCityAt(Position pos, TactikonState state)
	{
		for (City city : state.cities)
		{
			if (city.x == pos.x && city.y == pos.y) return city;
		}
		return null;
	}
	
	CityCaptureInfo GetWalkingCaptureInfo(IUnit unit, WorldPlot plot, TactikonState state)
	{
		// now see how long it'll take to walk to the closest city
		if (unit.bMoved == true) return null;
		
		Position destination = plot.FindDestination(state, unit.GetPosition());
		
		if (destination != null) 
		{
			int turns = plot.mTurnsMap[unit.GetPosition().x][unit.GetPosition().y];
			
			// now work out which city this correlates to
			City city = GetCityAt(destination, state);
			
			CityCaptureInfo newCaptureInfo = new CityCaptureInfo();
			newCaptureInfo.cityId = city.cityId;
			newCaptureInfo.turns = turns;
			newCaptureInfo.unitId = unit.mUnitId;
			return newCaptureInfo;
		}
		return null;
	}
	
	CityCaptureInfo GetTransportCaptureInfo(IUnit unit, WorldPlot plot, TactikonState state, TaskType transportTask, int maxTurns)
	{
		// are we already on board a transport
		IUnit transport = null;
		if (unit.mCarriedBy != -1)
		{
			transport = state.GetUnit(unit.mCarriedBy);
		}
		
		if (transport == null) // is there a transport nearby we can board?
		{
			ArrayList<IUnit> possibleTransports = new ArrayList<IUnit>();
			ArrayList<IUnit> availableUnits = info.GetUnitsAvailable(state,  myPlayerId);
			for (IUnit testTrans : availableUnits)
			{
				if (!testTrans.CanCarry(unit)) continue;
				if (testTrans.mUserId != unit.mUserId) continue;
				if (testTrans.mCarrying.size() >= testTrans.CarryCapacity()) continue;
				
				if (info.unitTaskAssignedMap.containsKey(testTrans.mUnitId))
				{
					if (info.unitTaskAssignedMap.get(testTrans.mUnitId).taskType != transportTask) continue;
				}
				
				if (testTrans.GetPosition().x == unit.GetPosition().x &&
					testTrans.GetPosition().y == unit.GetPosition().y)
				{
					transport = testTrans;
					break;
				}
				
				possibleTransports.add(testTrans);
			}
			
			if (transport == null) // maybe we can get to a transport by walking for a turn
			{
				ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
				for (IUnit possibleTransport : possibleTransports)
				{
					for (Position move : moves)
					{
						if (move.x == possibleTransport.GetPosition().x &&
							move.y == possibleTransport.GetPosition().y)
						{
							transport = possibleTransport;
							break;
						}
					}
					if (transport != null) break;
				}
			}
		}
		
		if (transport == null) return null; // there wasn't a transport nearby for this unit
		
		// so, which city can we get to with this transport?
		ArrayList<Position> carrierStart = new ArrayList<Position>();
		carrierStart.add(transport.GetPosition());
		
		if (transport.GetMaxFuel() != -1)
		{
			if (transport.fuelRemaining / transport.GetMovementDistance() > maxTurns)
			{
				maxTurns = transport.fuelRemaining / transport.GetMovementDistance();
			}
		}
		WorldPlot carrierPlot = new WorldPlot(state, carrierStart, transport, info, maxTurns);
		carrierPlot.ComputePlot();
		
		// now check each plot, finding the point where the two plots have adjacent turns, with the lowest total
		Position unitTarget = null;
		Position carrierTarget = null;
		int bestTurns = 999;
		int bestTargetDist = 999;
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				int carrierDist = carrierPlot.mDistanceMap[x][y];
				if (carrierDist == Byte.MAX_VALUE) continue;
				if (info.blockMap[x][y] != 0) continue;
				if (info.myUnits[x][y] != 0) continue;
				
				if (transport.GetMaxFuel() != -1)
				{
					// don't go further than we have fuel for
					if (transport.fuelRemaining <= carrierDist + 1)
					{
						continue;
					}
				}

				int carrierTurns = carrierPlot.mTurnsMap[x][y];
				
				for (int xx = -1; xx <= 1; ++xx)
				{
					for (int yy = -1; yy <= 1; ++yy)
					{
						if (Math.abs(xx) + Math.abs(yy) != 1) continue;
						int tx = x + xx;
						int ty = y + yy;
						if (tx < 0 || ty < 0 || tx >= state.mapSize || ty >= state.mapSize) continue;

						if (info.blockMap[tx][ty] != 0) continue;
						int targetDistance = plot.mDistanceMap[tx][ty];
						
						if (targetDistance == Byte.MAX_VALUE) continue;
						
						targetDistance += unit.GetMoveCost(new Position(x, y), new Position(tx,ty), state);
						
						if (targetDistance == 0) continue;
						
						if (transport.GetMaxFuel() != -1 && targetDistance + carrierDist > transport.GetMaxFuel()) continue;
						
						int targetTurns = plot.mTurnsMap[tx][ty];
						
						int totalTurns = (carrierTurns + targetTurns) - 1;
						
						if (totalTurns < bestTurns || (totalTurns == bestTurns && targetDistance < bestTargetDist))
						{
							unitTarget = new Position(tx, ty);
							carrierTarget = new Position(x, y);
							bestTurns = totalTurns;
							bestTargetDist = targetDistance;
						}
						
					}
				}
			}
		}
		
		
		if (unitTarget != null)
		{
			CityCaptureInfo info = new CityCaptureInfo();

			info.turns = bestTurns;
			Position cityDest = plot.FindDestination(state, unitTarget);
			City city = GetCityAt(cityDest, state);
			info.unitId = unit.mUnitId;
			if (city != null) info.cityId = city.cityId;
			info.transportId = transport.mUnitId;
			info.transportWorldPlot = carrierPlot;
			info.transTarget = carrierTarget;
			return info;
		}
		
		return null;
		
	}
	

	boolean DoCapturingTasks(TactikonState state, int maxTurns)
	{
		
		// for each unowned city, work out
		// - how long it'll take to reach by foot (if possible)
		// - how long it'll take to reac by transport (assuming we can reach a transport)
		ArrayList<Position> cities = new ArrayList<Position>();
		for (City city : state.cities)
		{
			// any city with no fortification
			if (city.playerId == myPlayerId || city.fortifiedUnits.size() > 0) continue;
			
			// don't consider cities we're already attempting to capture
			if (info.cityUnitCaptureMap.containsKey(city.cityId)) 
			{
				//int unitId = info.cityUnitCaptureMap.get(city.cityId);
				//IUnit u = state.GetUnit(unitId);
				//System.out.println("Not attempting capture of city at " + city.x + ", " + city.y + " as unit at " + u.GetPosition().x + ", " + u.GetPosition().y + " is already doing so.");
				continue;
			}
			
			cities.add(new Position(city.x, city.y));
		}
		
		if (cities.size() == 0) return false;
		
		IUnit infantry = new UnitInfantry();
		infantry.mUserId = myPlayerId;
		IUnit tank = new UnitTank();
		tank.mUserId = myPlayerId;
		
		WorldPlot infantryWorldPlot = new WorldPlot(state, cities, infantry, info, maxTurns);
		infantryWorldPlot.ComputePlot();
		WorldPlot tankWorldPlot = new WorldPlot(state, cities, tank, info, maxTurns);
		tankWorldPlot.ComputePlot();
		
		// TODO: We should prioritise not capturing cities the enemy will capture first 

		int bestTurns = maxTurns;
		CityCaptureInfo bestCapInfo = null;
		
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state,  myPlayerId);
		for (IUnit unit : units)
		{
			if (unit.CanCaptureCity() == false) continue;
			
			WorldPlot plot = null;
			if (unit instanceof UnitInfantry) plot = infantryWorldPlot;
			if (unit instanceof UnitTank) plot = tankWorldPlot;
			
			CityCaptureInfo walkCaptureInfo = GetWalkingCaptureInfo(unit, plot, state);
			CityCaptureInfo transportCaptureInfo = GetTransportCaptureInfo(unit, plot, state, TaskType.DeliveringUnit, bestTurns);
			
			if (walkCaptureInfo != null)
			{
				City city = state.GetCity(walkCaptureInfo.cityId);
				//System.out.println("Walk from: " + unit.GetPosition().x + ", " + unit.GetPosition().y + " to " + city.x + ", " + city.y + " in " + walkCaptureInfo.turns);
					if (walkCaptureInfo.turns < bestTurns)
					{
						bestCapInfo = walkCaptureInfo;
						bestTurns = walkCaptureInfo.turns;
					}
			}
				
			
			if (transportCaptureInfo != null)
			{
				IUnit transport = state.GetUnit(transportCaptureInfo.transportId);
				if (transportCaptureInfo.transTarget.x == transport.GetPosition().x && transportCaptureInfo.transTarget.y == transport.GetPosition().y)
					continue; // don't consider this if the transport is already at the target
				
				if (transportCaptureInfo.turns < bestTurns)
				{
					bestTurns = transportCaptureInfo.turns;
					bestCapInfo = transportCaptureInfo;
				}
			}
			
		}
		
		// now we have a map of citycaptureinfos...
		// we should do the one with the smallest turns
	
		
		if (bestCapInfo != null) // we have the best thing to attempt capturing... do it!
		{
			boolean bInjected = false;
			if (bestCapInfo.transportId == -1)
			{
				// now actually do a move if we got this far...
				IUnit unit = state.GetUnit(bestCapInfo.unitId);
				WorldPlot plot = null;
				if (unit instanceof UnitInfantry) plot = infantryWorldPlot;
				if (unit instanceof UnitTank) plot = tankWorldPlot;
				
				Position bestMove = plot.FindClosestMove(unit, state, info);
				if (bestMove != null)
				{
					EventMoveUnit event = new EventMoveUnit();
					event.mUnitId = unit.mUnitId;
					event.mFrom = unit.GetPosition();
					event.mTo = bestMove;
					mInjector.AddEvent(event);
					
					info.cityUnitCaptureMap.put(bestCapInfo.cityId, bestCapInfo.unitId);
					bInjected = true;
					info.AssignTask(unit, TaskType.CapturingCity);
					return true;
				}
			} else
			{
				// we're doing a transport plot...
				// if we're already on a transport, more the transport closer to the destination
				IUnit unit = state.GetUnit(bestCapInfo.unitId);
				IUnit transport = state.GetUnit(bestCapInfo.transportId);
				
				// if we're not on the transport, board it
				if (unit.mCarriedBy != transport.mUnitId)
				{
					if (unit.GetPosition().x != transport.GetPosition().x || unit.GetPosition().y != transport.GetPosition().y)
					{
						// need to move there
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = unit.mUnitId;
						event.mFrom = unit.GetPosition();
						event.mTo = transport.GetPosition();
						mInjector.AddEvent(event);
						info.AssignTask(unit, TaskType.CapturingCity);
						info.AssignTask(transport,  TaskType.DeliveringUnit);
						bInjected = true;
						
						// and if we're in a city, we'll need to board it specifically also
						if (transport.mFortified == true)
						{
							EventBoardUnit event2 = new EventBoardUnit();
							event2.mTransporter = transport.mUnitId;
							event2.mUnit = unit.mUnitId;
							mInjector.AddEvent(event2);
							
						}
					} else
					{
						// must be in a city, board right away
						EventBoardUnit event = new EventBoardUnit();
						event.mTransporter = transport.mUnitId;
						event.mUnit = unit.mUnitId;
						mInjector.AddEvent(event);
						
						info.AssignTask(unit,  TaskType.CapturingCity);
						info.AssignTask(transport,  TaskType.DeliveringUnit);
						bInjected = true;
					}
				}
				
				// and then move the transport where we need to
				{
					ArrayList<Position> targetList = new ArrayList<Position>();
					targetList.add(bestCapInfo.transTarget);
					WorldPlot plot = new WorldPlot(state, targetList, transport, info, maxTurns);
					plot.ComputePlot();
					Position bestMove = plot.FindClosestMove(transport, state, info);
					if (bestMove != null)
					{
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = transport.mUnitId;
						event.mFrom = transport.GetPosition();
						event.mTo = bestMove;
						mInjector.AddEvent(event);
						
						if (bestMove.x == bestCapInfo.transTarget.x && bestMove.y == bestCapInfo.transTarget.y)
						{
							info.cityUnitCaptureMap.remove(bestCapInfo.cityId);
						} else
						{
							info.cityUnitCaptureMap.put(bestCapInfo.cityId, bestCapInfo.unitId);
							info.AssignTask(unit, TaskType.CapturingCity);
							info.AssignTask(transport, TaskType.DeliveringUnit);
						}
					
						bInjected = true;
					} else
					{
						//System.out.println("No best move :(");
						//System.out.println("Unit: " + transport.GetPosition().x + ", " + transport.GetPosition().y + "Moved: " + transport.bMoved);
					}
				}
				
			}
			// if we've got here, something has gone wrong :(
			//System.out.println("Something has gone wrong :(");
			if (bInjected == true) return true;
		}
		
		return false;
			
	}
	
	boolean DoPickupUnits(TactikonState state)
	{
		// for units which are currently unassigned, find transports to pick them up

		
		// chopperworldplot - infantry destinations, fake chopper as source
		// boatworldplot - infantry and tanks as destinations, fake boat as source
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state, myPlayerId);
		
		ArrayList<IUnit> tanks = new ArrayList<IUnit>();
		ArrayList<IUnit> infantry = new ArrayList<IUnit>();
		ArrayList<IUnit> tanksAndInfantry = new ArrayList<IUnit>();
		ArrayList<IUnit> boats = new ArrayList<IUnit>();
		ArrayList<IUnit> choppers = new ArrayList<IUnit>();
		ArrayList<IUnit> choppersAndBoats = new ArrayList<IUnit>();
		
		for (IUnit unit : units)
		{
		
			// don't pickup units on the front line, or fortified units with a defence shortfall in their city
			// fortified defenders won't leave anyway (as they'll have a task assigned)
			boolean bNoPickup = false;
			
			if (info.zoneMap[unit.GetPosition().x][unit.GetPosition().y] == AIInfo.ZONE_FRONTLINE) bNoPickup = true;
			if (info.zoneMap[unit.GetPosition().x][unit.GetPosition().y] == AIInfo.ZONE_ENEMY) bNoPickup = true;
			
			if (unit instanceof UnitTank)
			{
				if (bNoPickup == false)
				{
					tanks.add(unit);
					tanksAndInfantry.add(unit);
				}
			} else if (unit instanceof UnitInfantry)
			{
				if (bNoPickup == false)
				{
					infantry.add(unit);
					tanksAndInfantry.add(unit);
				}
			} else if (unit instanceof UnitBoatTransport)
			{
				if (unit.mCarrying.size() == 0)
				{
					boats.add(unit);
					choppersAndBoats.add(unit);
				}
			} else if (unit instanceof UnitHelicopter)
			{
				if (unit.mCarrying.size() == 0)
				{
					choppers.add(unit);
					choppersAndBoats.add(unit);
				}
			}
		}
		
		IUnit fakeChopper = new UnitHelicopter();
		fakeChopper.mUserId = myPlayerId;
		IUnit fakeBoat = new UnitBoatTransport();
		fakeBoat.mUserId = myPlayerId;
		
		
		WorldPlot boatWorldPlot = new WorldPlot(state, fakeBoat, tanksAndInfantry, info, 999);
		boatWorldPlot.ComputePlot();
		WorldPlot chopperWorldPlot = new WorldPlot(state, fakeChopper, infantry, info, 999);
		chopperWorldPlot.ComputePlot();
		
		// now for each each concrete chopper and boat, work out where the nearest unit we can pick up is
		// once we have the big list of pickups, choose the best one and do it...
		int bestTurns = 999;
		IUnit pickupTransport = null;
		IUnit pickupUnit = null;
		for (IUnit transport : choppersAndBoats)
		{
			WorldPlot plot = null;
			if (transport instanceof UnitHelicopter) plot = chopperWorldPlot;
			if (transport instanceof UnitBoatTransport) plot = boatWorldPlot;
			
			Position targetUnitPos = plot.FindDestination(state, transport.GetPosition());
			if (targetUnitPos != null)
			{
				IUnit targetUnit = null;
				// work out which unit it was
				for (IUnit findUnit : tanksAndInfantry)
				{
					if (findUnit.GetPosition().x == targetUnitPos.x && findUnit.GetPosition().y == targetUnitPos.y)
					{
						targetUnit = findUnit;
						break;
					}
				}
				
				if (targetUnit == null) continue; // didn't manage to find which unit we can pick up
				
				// otherwise, see if this is the best option
				int distance = plot.mDistanceMap[transport.GetPosition().x][transport.GetPosition().y];
				if (distance == 0) continue; // don't try to pick up units on the same square, it'll just confuse things
				if (transport.GetMaxFuel() != -1 && transport.fuelRemaining < distance) continue; // don't go out of fuel range
				if (distance < bestTurns)
				{
					bestTurns = distance;
					pickupTransport = transport;
					pickupUnit = targetUnit;
				}
			}
		}
		
		if (pickupTransport != null && pickupUnit != null)
		{
			// make the pickup transport move as requested
			WorldPlot plot = null;
			if (pickupTransport instanceof UnitHelicopter) plot = chopperWorldPlot;
			if (pickupTransport instanceof UnitBoatTransport) plot = boatWorldPlot;
			
			Position bestMove = plot.FindClosestMove(pickupTransport, state, info);
			if (bestMove != null)
			{
				EventMoveUnit event = new EventMoveUnit();
				event.mUnitId = pickupTransport.mUnitId;
				event.mFrom = pickupTransport.GetPosition();
				event.mTo = bestMove;
				mInjector.AddEvent(event);
				
				info.AssignTask(pickupUnit, TaskType.WaitingForPickup);
				info.AssignTask(pickupTransport, TaskType.PickingUp);
				
				//System.out.println("Picking up unit");
				
				return true;
			
			}
		}
		
		return false;
	}
	
	int GetCityDefenceAssigned(City city, TactikonState state)
	{
		int cityDefAssigned = 0;
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			if (info.unitTaskAssignedMap.containsKey(entry.getKey()))
			{
				IUnit unit = entry.getValue();
				if (unit.GetPosition().x != city.x || unit.GetPosition().y != city.y) continue;
				if (unit.mUserId != myPlayerId) continue;
				Task task = info.unitTaskAssignedMap.get(entry.getKey());
				if (task.taskType == TaskType.DefendingCity)
				{
					if (unit instanceof UnitTank) cityDefAssigned += 2;
					if (unit instanceof UnitInfantry) cityDefAssigned++;
				}
			}
		}
		return cityDefAssigned;
	
	}
	
			
	boolean DeliverToDefendCity(TactikonState state, int maxTurns)
	{
		//for each city, check the defence requirements
		// assign as many fortified units as required to defending
		
		ArrayList<Position> cities = new ArrayList<Position>();
		for (City city : state.cities)
		{
			if (city.playerId != myPlayerId) continue;
			
			int cityDefNeeded = info.GetCityDefenceRequirement(city, state);
			if (cityDefNeeded == 0) continue;
			int cityDefAssigned = GetCityDefenceAssigned(city, state);
			
			if (cityDefAssigned < cityDefNeeded)
			{
				cities.add(new Position(city.x, city.y));
			}
		}
		
		IUnit infantry = new UnitInfantry();
		infantry.mUserId = myPlayerId;
		IUnit tank = new UnitTank();
		tank.mUserId = myPlayerId;
		
		WorldPlot infantryWorldPlot = new WorldPlot(state, cities, infantry, info, maxTurns);
		infantryWorldPlot.ComputePlot();
		WorldPlot tankWorldPlot = new WorldPlot(state, cities, tank, info, maxTurns);
		tankWorldPlot.ComputePlot();
		
		// TODO: We should prioritise not capturing cities the enemy will capture first 

		int bestTurns = maxTurns;
		CityCaptureInfo bestCapInfo = null;
		
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state,  myPlayerId);
		for (IUnit unit : units)
		{
			if (unit.CanCaptureCity() == false) continue;
			
			WorldPlot plot = null;
			if (unit instanceof UnitInfantry) plot = infantryWorldPlot;
			if (unit instanceof UnitTank) plot = tankWorldPlot;
			
			CityCaptureInfo walkCaptureInfo = GetWalkingCaptureInfo(unit, plot, state);
			CityCaptureInfo transportCaptureInfo = GetTransportCaptureInfo(unit, plot, state, TaskType.DeliveringUnit, bestTurns);
			
			if (walkCaptureInfo != null)
			{
					if (walkCaptureInfo.turns < bestTurns)
					{
						bestCapInfo = walkCaptureInfo;
						bestTurns = walkCaptureInfo.turns;
					}
			}
				
			
			if (transportCaptureInfo != null)
			{
				IUnit transport = state.GetUnit(transportCaptureInfo.transportId);
				if (transportCaptureInfo.transTarget.x == transport.GetPosition().x && transportCaptureInfo.transTarget.y == transport.GetPosition().y)
					continue; // don't consider this if the transport is already at the target
				
				if (transportCaptureInfo.turns < bestTurns)
				{
					bestTurns = transportCaptureInfo.turns;
					bestCapInfo = transportCaptureInfo;
				}
			}
			
		}
		
		// now we have a map of citycaptureinfos...
		// we should do the one with the smallest turns
	
		
		if (bestCapInfo != null) // we have the best thing to attempt capturing... do it!
		{
			boolean bInjected = false;
			if (bestCapInfo.transportId == -1)
			{
				// now actually do a move if we got this far...
				IUnit unit = state.GetUnit(bestCapInfo.unitId);
				WorldPlot plot = null;
				if (unit instanceof UnitInfantry) plot = infantryWorldPlot;
				if (unit instanceof UnitTank) plot = tankWorldPlot;
				
				
				Position bestMove = plot.FindClosestMove(unit, state, info);
				if (bestMove != null)
				{
					EventMoveUnit event = new EventMoveUnit();
					event.mUnitId = unit.mUnitId;
					event.mFrom = unit.GetPosition();
					event.mTo = bestMove;
					mInjector.AddEvent(event);
					
					bInjected = true;
					return true;
				}
			} else
			{
				// we're doing a transport plot...
				// if we're already on a transport, more the transport closer to the destination
				IUnit unit = state.GetUnit(bestCapInfo.unitId);
				IUnit transport = state.GetUnit(bestCapInfo.transportId);
				
				// if we're not on the transport, board it
				if (unit.mCarriedBy != transport.mUnitId)
				{
					if (unit.GetPosition().x != transport.GetPosition().x || unit.GetPosition().y != transport.GetPosition().y)
					{
						// need to move there
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = unit.mUnitId;
						event.mFrom = unit.GetPosition();
						event.mTo = transport.GetPosition();
						mInjector.AddEvent(event);
						bInjected = true;
					} else
					{
						// must be in a city, board right away
						EventBoardUnit event = new EventBoardUnit();
						event.mTransporter = transport.mUnitId;
						event.mUnit = unit.mUnitId;
						mInjector.AddEvent(event);
						
						bInjected = true;
					}
				}
				
				// and then move the transport where we need to
				{
					ArrayList<Position> targetList = new ArrayList<Position>();
					targetList.add(bestCapInfo.transTarget);
					WorldPlot plot = new WorldPlot(state, targetList, transport, info, maxTurns);
					plot.ComputePlot();
					Position bestMove = plot.FindClosestMove(transport, state, info);
					if (bestMove != null)
					{
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = transport.mUnitId;
						event.mFrom = transport.GetPosition();
						event.mTo = bestMove;
						mInjector.AddEvent(event);
						
							bInjected = true;
					} else
					{
						//System.out.println("No best move :(");
						//System.out.println("Unit: " + transport.GetPosition().x + ", " + transport.GetPosition().y + "Moved: " + transport.bMoved);
					}
				}
				
			}
			// if we've got here, something has gone wrong :(
			//System.out.println("Something has gone wrong :(");
			if (bInjected == true) return true;
		}
		
		return false;
	}

	boolean AssignUnitDefence(TactikonState state)
	{
		// for each city, check the defence requirements
		// assign as many fortified units as required to defending
		
		for (City city : state.cities)
		{
			if (city.playerId != myPlayerId) continue;
			
			int cityDefNeeded = info.GetCityDefenceRequirement(city, state);
			if (cityDefNeeded == 0) continue;
			int cityDefAssigned = GetCityDefenceAssigned(city, state);
			
			if (cityDefAssigned < cityDefNeeded)
			{
				// then find a unit in the city to beef up defence
				ArrayList<IUnit> units = info.GetUnitsAvailable(state, myPlayerId);
				IUnit bestUnit = null;
				int bestScore = 0;
				for (IUnit unit : units)
				{
					if (unit.mCarriedBy != -1) continue;
					int score = 0;
					// just find the units already in the city to begin with
					if (unit.GetPosition().x == city.x && unit.GetPosition().y == city.y)
					{
						score = score + 3;
					} else
					{
						ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
						for (Position move : moves)
						{
							if (move.x == city.x && move.y == city.y)
							{
								score = score + 1;
							}
						}
					}
					
					if (unit instanceof UnitTank) score = score * 4;
					if (unit instanceof UnitInfantry) score = score * 3;
					
					if (score > bestScore)
					{
						bestUnit = unit;
						bestScore = score;
					}
				}
				if (bestUnit != null)
				{
					if (bestUnit.GetPosition().x == city.x && bestUnit.GetPosition().y == city.y &&
						bestUnit.mCarrying.size() > 0 && city.fortifiedUnits.size() < state.MAX_UNITS_IN_CITY)
					{
						EventLeaveTransport event = new EventLeaveTransport();
						event.mTransporter = bestUnit.mUnitId;
						event.mUnit = bestUnit.mCarrying.get(0);
						mInjector.AddEvent(event);
					}
					// then assign this unit
					info.AssignTask(bestUnit, TaskType.DefendingCity);
					if (bestUnit.GetPosition().x != city.x || bestUnit.GetPosition().y != city.y)
					{
						// and if we need to, move it there as well...
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = bestUnit.mUnitId;
						event.mFrom = bestUnit.GetPosition();
						event.mTo = new Position(city.x, city.y);
						mInjector.AddEvent(event);
				
					}
					return true;
				}
			}
		}
		
		return false;
	}
		
	Position FindBestLandingSpotNear(City city, TactikonState state)
	{
		Position bestLanding = null;
		int leastDanger = 999;
		for (int x = city.x - 3; x <= city.x + 3; x++)
		{
			if (x < 0) continue;
			if (x >= state.mapSize) continue;
			for (int y = city.y - 3; y <= city.y + 3; ++y)
			{
				if (y < 0) continue;
				if (y >= state.mapSize) continue;
			
				if (info.blockMap[x][y] != 0) continue;
				if (info.myUnits[x][y] != 0) continue;
				if (info.massMap[city.x][city.y] != info.massMap[x][y]) continue;
				
				int danger = info.landDangerMap[x][y];
				if (danger < leastDanger)
				{
					bestLanding = new Position(x, y);
					leastDanger = danger;
				}
			}
		}
		return bestLanding;
	}
	
	boolean DeliverToZoneAssignments(TactikonState state, int maxTurns, byte zone)
	{
		// for each enemy city on the front lines
		ArrayList<Position> cities = new ArrayList<Position>();
		for (City city : state.cities)
		{
			// only consider unowned cities for now
			if (city.playerId == -1) continue;
			if (city.playerId == myPlayerId) continue;

			if (info.zoneMap[city.x][city.y] != zone) continue;
			

			// choose the area around the city where there aren't any enemy units
			// find the least dangerous place to land
			Position pos = FindBestLandingSpotNear(city,state);
			if (pos != null)
				cities.add(pos);
		}
		
		if (cities.size() == 0) return false;
		
		
		IUnit infantry = new UnitInfantry();
		infantry.mUserId = myPlayerId;
		IUnit tank = new UnitTank();
		tank.mUserId = myPlayerId;
		
		WorldPlot infantryWorldPlot = new WorldPlot(state, cities, infantry, info, maxTurns);
		infantryWorldPlot.ComputePlot();
		WorldPlot tankWorldPlot = new WorldPlot(state, cities, tank, info, maxTurns);
		tankWorldPlot.ComputePlot();
		
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state,  myPlayerId);
		
		int bestTurns = 999;
		CityCaptureInfo bestCapInfo = null;
		
		for (IUnit unit : units)
		{
			if (unit.CanCaptureCity() == false) continue;
			
			if (unit.mCarriedBy == -1)
			{
				// don't board a transport for delivery if we're already in the danger zone
				if (info.zoneMap[unit.GetPosition().x][unit.GetPosition().y] == AIInfo.ZONE_ENEMY ||
					info.zoneMap[unit.GetPosition().x][unit.GetPosition().y] == AIInfo.ZONE_FRONTLINE) continue;
					
			}
			
			WorldPlot plot = null;
			if (unit instanceof UnitInfantry) plot = infantryWorldPlot;
			if (unit instanceof UnitTank) plot = tankWorldPlot;
			
			CityCaptureInfo transportCaptureInfo = GetTransportCaptureInfo(unit, plot, state, TaskType.DeliveringToAttack, bestTurns);
			
			if (transportCaptureInfo != null)
			{
				IUnit transport = state.GetUnit(transportCaptureInfo.transportId);
				if (transportCaptureInfo.transTarget.x == transport.GetPosition().x && transportCaptureInfo.transTarget.y == transport.GetPosition().y)
					continue; // don't consider this if the transport is already at the target

				if (transportCaptureInfo.turns < bestTurns)
				{
					bestTurns = transportCaptureInfo.turns;
					bestCapInfo = transportCaptureInfo;
					
				}
			}
		}
		
		// now we have a map of citycaptureinfos...
		// we should do the one with the smallest turns
	
		if (bestCapInfo != null) // we have the best thing to attempt capturing... do it!
		{
			{
				// we're doing a transport plot...
				// if we're already on a transport, more the transport closer to the destination
				IUnit unit = state.GetUnit(bestCapInfo.unitId);
				IUnit transport = state.GetUnit(bestCapInfo.transportId);
				
				// if we're not on the transport, board it
				if (unit.mCarriedBy != transport.mUnitId)
				{
					if (unit.GetPosition().x != transport.GetPosition().x || unit.GetPosition().y != transport.GetPosition().y)
					{
						// need to move there
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = unit.mUnitId;
						event.mFrom = unit.GetPosition();
						event.mTo = transport.GetPosition();
						mInjector.AddEvent(event);
						info.AssignTask(unit, TaskType.AttackingCity);
						info.AssignTask(transport,  TaskType.DeliveringToAttack);
					} else
					{
						// must be in a city, board right away
						EventBoardUnit event = new EventBoardUnit();
						event.mTransporter = transport.mUnitId;
						event.mUnit = unit.mUnitId;
						mInjector.AddEvent(event);
						
						info.AssignTask(unit,  TaskType.AttackingCity);
						info.AssignTask(transport,  TaskType.DeliveringToAttack);
					}
				}
				
				// and then move the transport where we need to
				{
					ArrayList<Position> targetList = new ArrayList<Position>();
					targetList.add(bestCapInfo.transTarget);
					WorldPlot plot = new WorldPlot(state, targetList, transport, info, 999);
					plot.ComputePlot();
					Position bestMove = plot.FindClosestMove(transport, state, info);
					
					if (bestMove != null)
					{
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = transport.mUnitId;
						event.mFrom = transport.GetPosition();
						event.mTo = bestMove;
						mInjector.AddEvent(event);
						
						{
							info.AssignTask(unit, TaskType.AttackingCity);
							info.AssignTask(transport, TaskType.DeliveringToAttack);
						}
						
						if (bestMove.x == bestCapInfo.transTarget.x && bestMove.y == bestCapInfo.transTarget.y)
						{
							// if the transport is at the target, allow the unit off the boat
							info.unitTaskAssignedMap.remove(unit.mUnitId);
						}
						
					
						return true;
					} else
					{
						//System.out.println("No best move :(");
						//System.out.println("Unit: " + transport.GetPosition().x + ", " + transport.GetPosition().y + "Moved: " + transport.bMoved);
					}
				}
				
			}
			// if we've got here, something has gone wrong :(
			//System.out.println("Something has gone wrong :(");
		}
		
		return false;
		
	}
	
	float ChanceOfDamage(IUnit attacker, IUnit defender, TactikonState state)
	{
		Battle battle = new Battle(state, attacker.mUnitId, defender.mUnitId, true);
				
		float damageProb = 0;
		for (Outcome outcome : battle.out)
		{
			if (outcome.unitId1 == defender.mUnitId)
			{
				if (outcome.unitId1Damage > 0) damageProb = damageProb + outcome.probability;
			} else
			{
				if (outcome.unitId2Damage > 0) damageProb = damageProb + outcome.probability;
			}
		}
		
		return damageProb;
	}
	
	boolean MoveToAttackPosition(TactikonState state, int maxTurns)
	{
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state, myPlayerId);
		
		for (IUnit unit : units)
		{
			if (unit instanceof UnitBoatTransport) continue;
			if (unit instanceof UnitHelicopter) continue;

			// make a worldplot with all the enemies that we want to fight
			//ArrayList<IUnit> enemies = new ArrayList<IUnit>();
			ArrayList<Position> targets = new ArrayList<Position>();
			for (Entry<Integer, IUnit> entry : state.units.entrySet())
			{
				IUnit enemy = entry.getValue();
				if (enemy.mUserId == myPlayerId) continue;
				
				if (ChanceOfDamage(unit, enemy, state) > varMoveAttackProbRequired)
				{
					// add points around the enemy to head towards
					int x = enemy.GetPosition().x;
					int y = enemy.GetPosition().y;
					
					if (x > 0) targets.add(new Position(x - 1, y));
					if (y > 0) targets.add(new Position(x, y - 1));
					if (x < state.mapSize - 1) targets.add(new Position(x + 1, y));
					if (y < state.mapSize - 1) targets.add(new Position(x, y+1));

				}
			}
			
			if (unit.mCarriedBy != -1)
			{
				ArrayList<Position> removeTargets = new ArrayList<Position>();
				for (Position pos : targets)
				{
					if (pos.x == unit.GetPosition().x && pos.y == unit.GetPosition().y) removeTargets.add(pos);
					//if (info.blockMap[pos.x][pos.y] == 1) removeTargets.add(pos);
					if (info.myUnits[pos.x][pos.y] == 1) removeTargets.add(pos);
				}
				targets.removeAll(removeTargets);
			}
			
			
			
			WorldPlot unitWorldPlot = new WorldPlot(state, targets, unit, info, maxTurns);
			unitWorldPlot.ComputePlot();
			
			Position targetUnitPos = unitWorldPlot.FindDestination(state, unit.GetPosition());
			if (unitWorldPlot.mTurnsMap[unit.GetPosition().x][unit.GetPosition().y] > maxTurns) continue;
			
			if (unit.GetMaxFuel() != -1)
			{
				if (unitWorldPlot.mDistanceMap[unit.GetPosition().x][unit.GetPosition().y] > unit.fuelRemaining - unit.GetMovementDistance()) continue;
			}
			
			if (targetUnitPos != null)
			{
				Position bestMove = unitWorldPlot.FindClosestMove(unit,  state, info);
				if (bestMove != null)
				{
					EventMoveUnit event = new EventMoveUnit();
					event.mUnitId = unit.mUnitId;
					event.mFrom = unit.GetPosition();
					event.mTo = bestMove;
					mInjector.AddEvent(event);
					
					info.AssignTask(unit, TaskType.AttackingUnit);
					
				
					return true;
				}
			}
			
			
		}
		
		return false;
	}
	
	boolean Attack(TactikonState state)
	{
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.bAttacked == true) continue;
			if (unit.mUserId != myPlayerId) continue;
			
			ArrayList<Integer> attacks = state.GetPossibleAttacks(unit.mUnitId);
			float bestAttackProb = 0.0f;
			int bestAttack = -1;
			for (Integer attack : attacks)
			{
				IUnit enemy = state.GetUnit(attack);
				float chanceOfDamage = ChanceOfDamage(unit, enemy, state);
				if (chanceOfDamage > bestAttackProb)
				{
					bestAttack = enemy.mUnitId;
					bestAttackProb = chanceOfDamage;
				}
			}
			if (bestAttackProb > varAttackProbRequired)
			{
				EventAttackUnit attackEvent = new EventAttackUnit();
				attackEvent.attackingUnitId = unit.mUnitId;
				attackEvent.defendingUnitId = bestAttack;
				mInjector.AddEvent(attackEvent);
				return true;
			}
		}
		
		return false;
	}
	
	boolean Refuel(TactikonState state)
	{
		//return false;
		ArrayList<IUnit> units = info.GetUnitsAvailableToMove(state, myPlayerId);
		for (IUnit unit : units)
		{
			if (unit.GetMaxFuel() == -1) continue;
			
			if (unit.fuelRemaining > unit.GetMaxFuel() / 2) continue;
			if (unit.mFortified == true) continue;
			
			// if we're a helicopter, find the furthest city
			// if we're a bomber or fighter, find a city closer to the enemy
			
			City closestEnemy = null;
			Map<Float, City> cityDists = info.GetCityDistances(unit.GetPosition(), state);
			for (Entry<Float, City> entry : cityDists.entrySet())
			{
				City city = entry.getValue();
				if (city.playerId != myPlayerId && city.playerId != -1)
				{
					closestEnemy = city;
					break;
				}
			}
			
			if (closestEnemy == null) continue;
			
			Map<Float, City> nearToEnemy = info.GetCityDistances(new Position(closestEnemy.x, closestEnemy.y), state);
			
			City closestToEnemy = null;
			City furthestFromEnemy = null;
			
			// now find the friendly city closest to the enemy and furthest from the enemy
			for (Entry<Float, City> entry : nearToEnemy.entrySet())
			{
				City city = entry.getValue();
				if (city.playerId != myPlayerId) continue;
				if (city.playerId == -1) continue;
				if (city.fortifiedUnits.size() >= state.MAX_UNITS_IN_CITY) continue;
				
				int dist = Math.abs(city.x - unit.GetPosition().x) + Math.abs(city.y - unit.GetPosition().y);
				if (dist > unit.fuelRemaining) continue;
				
				if (closestToEnemy == null) closestToEnemy = city;
				furthestFromEnemy = city;
			}
			City headTo = null;
			if (unit instanceof UnitBomber || unit instanceof UnitFighter) headTo = closestToEnemy;
			if (unit instanceof UnitHelicopter && unit.mCarrying.size() > 0) headTo = closestToEnemy;
			if (unit instanceof UnitHelicopter && unit.mCarrying.size() == 0) headTo = furthestFromEnemy;
			
			if (headTo != null)
			{
				// find the move that get's us closest there
				ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
				Position bestMove = null;
				int bestDist = 999;
				for (Position move : moves)
				{
					int dist = Math.abs(move.x - headTo.x) + Math.abs(move.y - headTo.y);
					if (dist < bestDist)
					{
						bestMove = move;
						bestDist = dist;
					}
					
				}
				if (bestMove != null)
				{
					EventMoveUnit event = new EventMoveUnit();
					event.mFrom = unit.GetPosition();
					event.mTo = new Position(bestMove.x, bestMove.y);
					event.mUnitId = unit.mUnitId;
					mInjector.AddEvent(event);
					return true;
				}
			}
		}
		
		return false;
	}
	
	boolean MoveOutOfCrowdedCity(TactikonState state)
	{
		for (City city : state.cities)
		{
			if (city.playerId != myPlayerId) continue;
			if (city.fortifiedUnits.size() < state.MAX_UNITS_IN_CITY) continue;
			// just move a random unit to a random place
			
			IUnit unit = state.GetUnit(city.fortifiedUnits.get(0));
			ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
			if ( moves.size() > 0)
			{
				Position move = moves.get(0);
				EventMoveUnit event = new EventMoveUnit();
				event.mFrom = unit.GetPosition();
				event.mTo = move;
				event.mUnitId = unit.mUnitId;
				mInjector.AddEvent(event);
				return true;
			}
					
		}
		return false;
	}
	
	int restartPoint = 0;
	void TickAI(TactikonState state)
	{
		mUnitList.clear();
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == myPlayerId) mUnitList.add(unit);
		}
		
		for (IUnit unit : mUnitList)
		{
			if (unit.mUserId != myPlayerId) continue;
			if (unit.bMoved == true && unit.bAttacked == true) continue;
			if (DoObviousMoves(unit, state) == true) return;
		}
		
		if (AssignUnitDefence(state) == true) return;
		
		if (DoCapturingTasks(state, 2) == true) return;
		if (DeliverToDefendCity(state, 2) == true) return;
		
		if (Attack(state) == true) return;
		
		if (MoveToAttackPosition(state, 1) == true) return;
		
		restartPoint = 1;
		
		if (DoCapturingTasks(state, 999) == true) return;
		restartPoint = 2;
		
		if (restartPoint < 3)
		{
			if (DeliverToZoneAssignments(state, 999, AIInfo.ZONE_FRONTLINE) == true) return;
			restartPoint = 3;
		}
		
		if (restartPoint < 4)
		{
			if (DeliverToZoneAssignments(state, 999, AIInfo.ZONE_ENEMY) == true) return;
			restartPoint = 4;
		}
		
		if (MoveToAttackPosition(state, 999) == true) return;
		restartPoint = 5;
		
		
		if (Attack(state) == true) return;
		
		if (DoPickupUnits(state) == true) return;
		restartPoint = 6;
		
		
		if (MoveOutOfCrowdedCity(state) == true) return;
		
		if (Refuel(state) == true) return;
		restartPoint = 7;
		
		

		
		// if we get through to here, we've finished the turn
		restartPoint = 0;
		EndTurnEvent(myPlayerId, state);
		return;
	}

}
