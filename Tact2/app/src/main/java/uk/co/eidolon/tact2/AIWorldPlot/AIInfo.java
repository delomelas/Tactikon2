package uk.co.eidolon.tact2.AIWorldPlot;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.TreeMap;

import Tactikon.State.City;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBattleship;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitHelicopter;
import Tactikon.State.UnitInfantry;
import Tactikon.State.UnitTank;

import Core.EventInjector;

public class AIInfo
{
	AIInfo(EventInjector injector)
	{
		mInjector = injector;
	}
	
	EventInjector mInjector;
	
	enum TaskType
	{
		CapturingCity,
		DeliveringUnit,
		DefendingCity,
		WaitingForPickup,
		PickingUp,
		AttackingCity,
		DeliveringToAttack,
		AttackingUnit
	}
	
	class Task
	{
		TaskType taskType;
		int unitId;
	}
	
	Map<Integer, Task> unitTaskAssignedMap = new TreeMap<Integer, Task>();
	
	Map<Integer, Integer> cityUnitCaptureMap = new TreeMap<Integer, Integer>();
	
	Map<Integer, Integer> cityDefenceRequirementMap = new TreeMap<Integer, Integer>();
	
	public static Byte ZONE_SAFE = 0;
	public static Byte ZONE_EXPANSION = 1;
	public static Byte ZONE_FRONTLINE = 2;
	public static Byte ZONE_ENEMY = 3;
	
	byte[][] zoneMap;
	byte[][] blockMap;
	int[][] massMap;
	int[][] dangerMap;
	byte[][] myUnits;
	
	class Route
	{
		Position target;
		int turns;
	}
	
	public Map<Float, City> GetCityDistances(Position pos, TactikonState state)
	{
		Map<Float, City> cityDists = new TreeMap<Float, City>();
		float e = 0.0f;
		for (City city : state.cities)
		{
			int dist = Math.abs(pos.x - city.x) + Math.abs(pos.y - city.y);

			cityDists.put((float)dist + e, city);
			e = e + 0.01f;
		}
		
		return cityDists;
	}
	

	private void FloodFill(int x,int y, int size, final int val, final boolean type, int[][] massMap, int[][] map)
	{
		Stack<Position> list = new Stack<Position>();
		list.add(new Position(x, y));
		Position currpos;
		do 
		{
			currpos=list.pop();
			x = currpos.x;
			y = currpos.y;
			massMap[x][y] = val;
			if ((x > 0) && (massMap[x-1][y] == 0) && (type == IsLand(x - 1, y, map)))
				list.push(new Position(x-1,y));
			if ((x < size - 1) && (massMap[x+1][y] == 0) && (type == IsLand(x + 1, y, map)))
				list.push(new Position(x+1,y));
			if ((y > 0) && (massMap[x][y-1] == 0) && (type == IsLand(x, y - 1, map)))
				list.push(new Position(x,y-1));
			if ((y < size - 1) && (massMap[x][y+1] == 0) && (type == IsLand(x , y + 1, map)))
				list.push(new Position(x,y+1));
			
		} while (list.size()>0);
	}
	
	
	void GenerateLandmassMap(TactikonState state, int myPlayerId)
	{
		massMap = new int[state.mapSize][state.mapSize];
		int val = 0;
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (massMap[x][y] == 0)
				{
					val ++;
					FloodFill(x, y, state.mapSize, val, IsLand(x, y, state.map), massMap, state.map);
				}
			}
		}
		
	}
	
	public boolean IsLand(final int x, final int y, int[][] map)
	{
		if (map[x][y] == TactikonState.TileType_Land) return true;
		if (map[x][y] == TactikonState.TileType_Mountain) return true;
		if (map[x][y] == TactikonState.TileType_Jungle) return true; 
		if (map[x][y] == TactikonState.TileType_Port) return true; 
		
		return false;
	}
	
	
	public void AddDanger(int x, int y, TactikonState state)
	{
		if (x >= 0 && x < state.mapSize && y >= 0 && y < state.mapSize)
		{
			dangerMap[x][y] ++;
		}
	}
	
	public void AddUnitDanger(int x, int y, IUnit unit, TactikonState state)
	{
		for (int xx = x - unit.GetAttackRange(); xx <= x + unit.GetAttackRange(); ++xx)
		{
			for (int yy = y - unit.GetAttackRange(); yy <= y + unit.GetAttackRange(); ++yy)
			{
				int dist = Math.abs(x - xx) + Math.abs(y - yy);
				if (dist <= unit.GetAttackRange())
				{
					AddDanger(xx, yy, state);
				}
			}
		}
	}
	
	void GenerateBlockMap(TactikonState state, int playerId)
	{
		blockMap = new byte[state.mapSize][state.mapSize];
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerId) continue;
			
			blockMap[unit.GetPosition().x][unit.GetPosition().y] = 1;
		}
	}
	
	void GenerateMyUnitsMap(TactikonState state, int playerId)
	{
		myUnits = new byte[state.mapSize][state.mapSize];
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != playerId) continue;
			
			myUnits[unit.GetPosition().x][unit.GetPosition().y] = 1;
		}
	}
	
	
	void GenerateDangermap(TactikonState state, int playerId)
	{
		dangerMap = new int[state.mapSize][state.mapSize];
		// fill with a number from 1 to 10 depending on attack power of whatever is there
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit enemy = entry.getValue();
			if (enemy.mUserId == playerId) continue;
			
			//int[][] unitDanger = new int[state.mapSize][state.mapSize];
			AddUnitDanger(enemy.GetPosition().x, enemy.GetPosition().y, enemy, state);
			
			
		}
	
	}
	
	void GenerateZoneMap(TactikonState state, int playerId)
	{
		int totalCities = state.cities.size();
		int citiesToCount = 3;
		if (totalCities < 10) citiesToCount = 2;
		if (totalCities > 15) citiesToCount = 4;
		if (totalCities > 20) citiesToCount = 5;
		if (totalCities > 30) citiesToCount = 6;
		
		zoneMap = new byte[state.mapSize][state.mapSize];
		
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				Map<Float, City> dists = GetCityDistances(new Position(x, y), state);
				
				// if the closest "expansionzonecities" includes at least one unowned city and only player-owned cities
				// otherwise, we're in the expansion zone
				
				// or... if the closest safeZoneCities are all player-owned cities, we're in the safe zone
				
				// or... we're in a danger zone
				
				int unownedCities = 0;
				int playerCities = 0;
				int enemyCities = 0;
				
				ArrayList<City> closestCities = new ArrayList<City>();
				for (Entry<Float, City> nearCity : dists.entrySet())
				{
					closestCities.add(nearCity.getValue());
					
					if (closestCities.size() >= citiesToCount) break;
				}
				
				for (City city : closestCities)
				{
					if (city.playerId == playerId)
					{
						playerCities ++;
					} else if (city.playerId == -1) 
					{
						unownedCities++;
					} else
					{
						enemyCities++;
					}
					
				}
				byte zone;
				if (playerCities == citiesToCount)
				{
					zone = ZONE_SAFE;
				} else if (enemyCities == 0 && unownedCities > 0)
				{
					zone = ZONE_EXPANSION;
				} else if (playerCities > 0)
				{
					zone = ZONE_FRONTLINE;
				} else
				{
					zone = ZONE_ENEMY;
				}
				
				zoneMap[x][y] = zone;
				
			}
		}
	}
	
	void AssignTask(IUnit unit, TaskType taskType)
	{
		Task task = new Task();
		task.unitId = unit.mUnitId;
		task.taskType = taskType;
		unitTaskAssignedMap.put(unit.mUnitId, task);
		return;
	}
	
	
	int GetCurrentCityDefence(City city, TactikonState state)
	{
		int defence = 0;
		for (Integer unitId : city.fortifiedUnits)
		{
			IUnit defender = state.GetUnit(unitId);
			
			if (defender instanceof UnitInfantry) defence++;
			if (defender instanceof UnitTank) defence += 2;
		}
		
		return defence;
	}
	
	int GetCityDefenceRequirement(City city, TactikonState state)
	{
		int defence = 0;
	/*
	 *   - if there's a capturing transport nearby, +1
   
     - worldplot to generate a map of nearby transports and where they can deliver to
       - +1 requirement from two turns away
   
   - if we're on the front line +1
     - if the one of the two closest cities is enemy
     */
		/*
		if (zoneMap[city.x][city.y] == ZONE_FRONTLINE)
		{
			defence++;
		}*/
		
		// just set if there's a capturing unit neatby
		boolean bDanger = false;
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == city.playerId) continue;
			if (unit.CanCaptureCity() == false) continue;
			int dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			
			if (unit.mCarriedBy == -1)
			{
				if (massMap[unit.GetPosition().x][unit.GetPosition().y] != massMap[city.x][city.y])
				{
					dist = dist + 999;
				}
			}
			
			int move = unit.GetMovementDistance();
			if (unit.mCarriedBy != -1)
			{
				IUnit carrier = state.GetUnit(unit.mCarriedBy);
				move = move + carrier.GetMovementDistance();
			}
			if (dist <= move) bDanger = true;
		}
		if (bDanger == true) defence ++;
		
		boolean bUnitNear = false;
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == city.playerId) continue;
			int dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			if (dist <=3) bUnitNear = true;
		}
		
		if (bUnitNear == true) defence++;
		
		if (zoneMap[city.x][city.y] == AIInfo.ZONE_ENEMY) defence ++;
		
		/*
     
   - if there are enemy units nearby +1
     - within two sqares, ie attacking now
     
   - if it's the HQ (and it's past turn 10) +1
	 */
		return defence;
	}
	
	int GetCityDefenceShortfall(City city, TactikonState state)
	{
		int dif = GetCityDefenceRequirement(city, state) - GetCurrentCityDefence(city, state);
		
		if (dif < 0) dif = 0;
		
		return dif;
	}
	
	ArrayList<IUnit> GetUnitsAvailableToMove(TactikonState state, int playerId)
	{
		ArrayList<IUnit> units = new ArrayList<IUnit>();
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.bMoved == true) continue;
			if (unit.mUserId != playerId) continue;
			if (unitTaskAssignedMap.containsKey(unit.mUnitId)) continue;
			
			units.add(unit);
		}
		
		return units;
	}
	
	ArrayList<IUnit> GetUnitsAvailableToMoveAllowingTask(TactikonState state, int playerId, TaskType taskType)
	{
		ArrayList<IUnit> units = new ArrayList<IUnit>();
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.bMoved == true) continue;
			if (unit.mUserId != playerId) continue;
			if (unitTaskAssignedMap.containsKey(unit.mUnitId))
			{
				if (unitTaskAssignedMap.get(unit.mUnitId).taskType != taskType) continue;
			}
			units.add(unit);
		}
		
		return units;
	}
	
	
	
	ArrayList<IUnit> GetUnitsAvailable(TactikonState state, int playerId)
	{
		ArrayList<IUnit> units = new ArrayList<IUnit>();
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			//if (unit.bMoved == true) continue;
			if (unit.mUserId != playerId) continue;
			if (unitTaskAssignedMap.containsKey(unit.mUnitId)) continue;
			
			units.add(unit);
		}
		
		return units;
	}
	
	/*
	Route GetWalkingRouteToNearestUnownedCity(IUnit unit, TactikonState state, AIInfo info)
	{
		if (unit.CanCaptureCity() == false) return null;
		
		// worldplot to unowned cities, return a route for the nearest one
		ArrayList<Position> unownedCities = new ArrayList<Position>();
		for (City city : state.cities)
		{
			if (city.fortifiedUnits.size() != 0) continue;
			if (city.playerId == -1)
			{
				unownedCities.add(new Position(city.x, city.y));
			}
		}
		
		WorldPlot capturePlot = new WorldPlot(state, unownedCities, unit, info);
		capturePlot.ComputePlot();
		Position destination = capturePlot.FindDestination(state, unit.GetPosition());
		
		if (destination == null) return null;
		
		// we can get there, so work out how long it'll take
		Route route = new Route();
		
		route.target = destination;
		route.turns = capturePlot.mTurnsMap[destination.x][destination.y];
		
		System.out.println("Walking route to nearest unowned city: " + route.turns + " turns.");
		
		return route;
		
	}*/
	/*
	Route GetTransportRouteToNearestUnownedCity(IUnit unit, TactikonState state, AIInfo info)
	{
		if (unit.bMoved == true) return null;
		if (unit.CanCaptureCity() == false) return null;
		int turns = 0;
		IUnit transport = null;
		if (unit.mCarriedBy == -1)
		{
			// can we board a transport that's on the same sqaure
			for (Entry <Integer, IUnit> entry : state.units.entrySet())
			{
				IUnit trans = entry.getValue();
				if (trans.CanCarry(unit) == false) continue;
				if (trans.mUserId != unit.mUserId) continue;
				if (trans.mCarrying.size() >= trans.CarryCapacity()) continue;
				if (trans.GetPosition().x == unit.GetPosition().x && trans.GetPosition().y == unit.GetPosition().y)
				{
					transport = trans;
				}
			}
			
			if (transport == null)
			{
			
			// can we board a transport in one turn?
			ArrayList<IUnit> transports = new ArrayList<IUnit>();

			ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
			for (Entry <Integer, IUnit> entry : state.units.entrySet())
			{
				IUnit trans = entry.getValue();
				if (trans.CanCarry(unit) == false) continue;
				if (trans.mUserId != unit.mUserId) continue;
				if (trans.mCarrying.size() >= trans.CarryCapacity()) continue;
			
				for (Position move : moves)
				{
					if (move.x == trans.GetPosition().x && move.y == trans.GetPosition().y)
					{
						transport = trans;
						break;
					}
				}
			}
				
			}
		} else
		{
			transport = state.GetUnit(unit.mCarriedBy);
		}
		
		if (transport == null) return null;
		
		ArrayList<Position> unownedCities = new ArrayList<Position>();
		for (City city : state.cities)
		{
			if (city.fortifiedUnits.size() != 0) continue;
			if (city.playerId == -1)
			{
				unownedCities.add(new Position(city.x, city.y));
			}
		}
		
		WorldPlot targetPlot = new WorldPlot(state, unownedCities, unit, info);
		targetPlot.ComputePlot();
		
		ArrayList<Position> carrierStart = new ArrayList<Position>();
		carrierStart.add(transport.GetPosition());
		WorldPlot carrierPlot = new WorldPlot(state, carrierStart, transport, info);
		carrierPlot.ComputePlot();
		
		// delete points on the targetplot where enemy units are
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit enemyUnit = entry.getValue();
			if (enemyUnit.mUserId == unit.mUserId) continue;
			targetPlot.mDistanceMap[enemyUnit.GetPosition().x][enemyUnit.GetPosition().y] = Byte.MAX_VALUE;
			carrierPlot.mDistanceMap[enemyUnit.GetPosition().x][enemyUnit.GetPosition().y] = Byte.MAX_VALUE;
		}
		
		// now check each plot, finding the point where the two plots have adjacent turns, with the lowest total
		Position bestTarget = null;
		Position carrierTarget = null;
		int bestTurns = 999;
		int actualTurns = 999;
		for (int x = 1; x < state.mapSize - 1; ++x)
		{
			for (int y = 1; y < state.mapSize - 1; ++y)
			{
				int carrierTurns = carrierPlot.mDistanceMap[x][y];
				if (carrierTurns == Byte.MAX_VALUE) continue;
				
				for (int xx = -1; xx <= 1; ++xx)
				{
					for (int yy = -1; yy <= 1; ++yy)
					{
						if (Math.abs(xx) + Math.abs(yy) != 1) continue;
						int tx = x + xx;
						int ty = y + yy;
						int targetTurns = targetPlot.mDistanceMap[tx][ty];
						if (targetTurns == Byte.MAX_VALUE) continue;
						
						if (transport instanceof UnitHelicopter && targetTurns < 3) continue;
						
						int totalTurns = carrierTurns + targetTurns;
						
						
						if (totalTurns < bestTurns)
						{
							bestTarget = new Position(tx, ty);
							carrierTarget = new Position(x, y);
							bestTurns = totalTurns;
							
							int ct = carrierPlot.mTurnsMap[carrierTarget.x][carrierTarget.y];//TurnsForDestination(carrierTarget);
							int ut = targetPlot.mTurnsMap[bestTarget.x][bestTarget.y];//TurnsForDestination(bestTarget);
							
							actualTurns = (ct + ut) - 1;
							if (transport.bMoved == true) actualTurns ++;
						}
					}
				}
			}
		}
		
		if (bestTarget != null)
		{
			System.out.println("Turns for transport delivery: "  + actualTurns);
			Route route = new Route();
			route.target = bestTarget;
			route.turns = actualTurns;
			return route;
		}

		
		
		return null;
	}
	*/
}
