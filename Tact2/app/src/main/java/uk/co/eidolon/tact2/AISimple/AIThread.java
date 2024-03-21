package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import android.util.Log;

import uk.co.eidolon.tact2.AIHelpers.IAIThread;
import uk.co.eidolon.tact2.AISimple.AIBrain.UnitTypeEvaluator;

import Tactikon.State.Battle;
import Tactikon.State.Battle.Outcome;
import Tactikon.State.IUnit.Domain;
import Tactikon.State.City;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.EventChangeProduction;
import Tactikon.State.EventEndTurnWithPlayerId;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitBomber;
import Tactikon.State.UnitCarrier;
import Tactikon.State.UnitFighter;
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
	
	//public boolean stop = false;
	
	TaskList taskList = new TaskList();
	
	class EventListener extends IEventListener
	{
		@Override
		public void HandleQueuedEvent(IEvent event, IState before, final IState after)
		{
			final TactikonState state = (TactikonState)after;
		
			if (event instanceof EventEndTurnWithPlayerId && state.playerToPlay == myPlayerId)
			{
				StartTurn(state);
			}
			
			mState = state;
		}
	}
	
	EventInjector mInjector;
	EventListener mListener;
	
	int myPlayerId;
	
	AIBrain mBrain;
	
	int[][] territoryMap;
	int[][] massMap;
	StateEngine mEngine;
	ArrayList<IUnit> mUnitList = new ArrayList<IUnit>();
	
	public AIThread(StateEngine stateEngine, int playerId, AIBrain brain)
	{
		mListener = new EventListener();
		stateEngine.AddListener(mListener);
		mEngine = stateEngine;
		mInjector = new EventInjector(stateEngine);
		
		myPlayerId = playerId;
		
		mState = (TactikonState)stateEngine.GetState();
		
		mBrain = brain;
		
		StartTurn(mState);
		
	}
	
	void StartTurn(TactikonState state)
	{
		taskList.StartTurn();
		territoryMap = AIInfo.GenerateTerritoryMap(state, myPlayerId);
		massMap = CreateLandmassMap(state.map, state);
	}
	
	/* (non-Javadoc)
	 * @see uk.co.eidolon.tact2.AISimple.IAIThread#run()
	 */
	@Override
	public void run()
	{
		while (stop == false)
		{
			while (mState.playerToPlay != myPlayerId || mState.GetGameState() != GameStatus.InGame)
			{
				if (stop == true) break;
				try
				{
					Thread.sleep(2);
				} catch (InterruptedException e)
				{
				}
				mListener.PumpQueue();
				
			}
			
			if (mState.GetGameState() != GameStatus.InGame) break;
			
			while(mState.playerToPlay == myPlayerId && mState.GetGameState() == GameStatus.InGame && stop == false)
			{
				if (TickAI(mState) == true)
				{
					//System.out.println("Done AI Tick");
					while (mListener.EventWaiting() == true)
					{
 						mListener.PumpQueue();
					}
					
					taskList.ExpireTasks(mState);
				} else break;
			}
		}
		
		mEngine.RemoveListener(mListener);
		stopped = true;
	}
	
	void SetProductionForCity(TactikonState state, City city)
	{
		ArrayList<IUnit> unitTypeList = state.GetUnitTypes();
		ArrayList<String> unitTypes = new ArrayList<String>();
		for (IUnit unit : unitTypeList)
		{
			unitTypes.add(unit.getClass().getSimpleName());
		}
		
		//unitTypes.remove("UnitCarrier");
		//unitTypes.remove("UnitFighter");
		//unitTypes.remove("UnitBomber");
		//unitTypes.remove("UnitSub");
		//unitTypes.remove("UnitBoatTransport");
		//unitTypes.remove("UnitHelicopter");
				
		if (city.isPort == true) 
		{
			// find the ajoining landmasses
			Set<Integer> seaMassList = new TreeSet<Integer>();
			if (city.x > 0 && state.map[city.x-1][city.y] == TactikonState.TileType_Water) seaMassList.add(massMap[city.x-1][city.y]);
			if (city.y > 0 && state.map[city.x][city.y-1] == TactikonState.TileType_Water) seaMassList.add(massMap[city.x][city.y-1]);
			if (city.x < state.mapSize - 1 && state.map[city.x+1][city.y] == TactikonState.TileType_Water) seaMassList.add(massMap[city.x+1][city.y]);
			if (city.y < state.mapSize - 1 && state.map[city.x][city.y+1] == TactikonState.TileType_Water) seaMassList.add(massMap[city.x][city.y+1]);
			
			// now find their neighbours
			Set<Integer> neighbourLandmasses = new TreeSet<Integer>();
			for (int x = 0; x < state.mapSize; ++x)
			{
				for (int y = 0; y < state.mapSize; ++y)
				{
					if (seaMassList.contains(massMap[x][y]))
					{
						// found some of the sea that ajoins the port
						if (x > 0 && IsLand(x - 1, y, state.map))neighbourLandmasses.add(massMap[x-1][y]); 
						if (y > 0 && IsLand(x, y - 1, state.map))neighbourLandmasses.add(massMap[x][y-1]);
						if (x < state.mapSize - 1 && IsLand(x + 1, y, state.map))neighbourLandmasses.add(massMap[x+1][y]);
						if (y < state.mapSize - 1 && IsLand(x, y + 1, state.map))neighbourLandmasses.add(massMap[x][y+1]);
					}
				}
			}
			neighbourLandmasses.remove(massMap[city.x][city.y]);
			
			Set<Integer> landmassesWithCities = new TreeSet<Integer>();
			for (City otherCity : state.cities)
			{
				landmassesWithCities.add(massMap[otherCity.x][otherCity.y]);
			}
			
			Set<Integer> neighbourLandmassesWithCities = new TreeSet<Integer>();
			neighbourLandmassesWithCities.addAll(neighbourLandmasses); // start with all neighbours
			neighbourLandmassesWithCities.retainAll(landmassesWithCities); // remove if they don't have cities
			
			// are we able to reach a different landmass with cities? enable boats
			if (neighbourLandmassesWithCities.isEmpty())
			{
				unitTypes.remove("UnitBoatTransport");
			}
			
			// are we able to reach enemy cities? enable battleships and carriers
			boolean bEnemyCitiesOnNeighbours = false;
			for (City otherCity : state.cities)
			{
				if (otherCity.playerId != -1 && otherCity.playerId != myPlayerId)
				{
					if (neighbourLandmassesWithCities.contains(massMap[otherCity.x][otherCity.y]))
					{
						bEnemyCitiesOnNeighbours = true;
						break;
					}
				}
			}
			
			if (bEnemyCitiesOnNeighbours == false)
			{
				unitTypes.remove("UnitBattleship");
				unitTypes.remove("UnitCarrier");
			}
			
			// are there enemy boats on the landmass? enable submarines
			boolean bEnemyBoatsOnSeaMasses = false;
			for (Entry<Integer, IUnit> entry : state.units.entrySet())
			{
				IUnit unit = entry.getValue();
				if (unit.mUserId == myPlayerId) continue;
				if (seaMassList.contains(massMap[unit.GetPosition().x][unit.GetPosition().y]) == false) continue;
				if (unit.getClass() == UnitBoatTransport.class)
				{
					bEnemyBoatsOnSeaMasses = true;
					break;
				}
			}
			
			if (bEnemyBoatsOnSeaMasses == false)
			{
				unitTypes.remove("UnitSub");
			}
			
		} else
		{
			// remove boats from non-ports
			for (IUnit unit : unitTypeList)
			{
				if (unit.GetDomain() == Domain.Water) unitTypes.remove(unit.getClass().getSimpleName());
			}
		}
		
		// count how many planes we have
		int enemyBombers = 0;
		int myCarriers = 0;
		int myPlanes = 0;
		int myFighers = 0;
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit cUnit = entry.getValue();
			if (cUnit.mUserId == myPlayerId)
			{
				if (cUnit.GetDomain() == Domain.Air)
					myPlanes ++;
				if (cUnit instanceof UnitCarrier)
					myCarriers ++;
				if (cUnit instanceof UnitFighter)
					myFighers ++;
			} else
			{
				if (cUnit instanceof UnitBomber)
					enemyBombers ++;
			}
		}
		
		// don't build carriers we we have more than enough
		if (myCarriers >= myPlanes /2)
		{
			unitTypes.remove("UnitCarrier");
		}
		
		// don't build fighters unless the enemy has bombers
		if (myFighers >= enemyBombers / 2)
		{
			unitTypes.remove("UnitFighter");
		}
		
		
		
		// now from the unittypes we have remaining, count how many of each unit we have. divide by the desired ratio value. whichever is lowest, build more...
		Map<String, Integer> unitCount = new TreeMap<String, Integer>();
		for (String unitType : unitTypes)
		{
			unitCount.put(unitType, 0);
		}
		
		int totalUnits = 0;
		
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != myPlayerId) continue;
			totalUnits ++;
			if (unitCount.containsKey(unit.getClass().getSimpleName()))
			{
				int current = unitCount.get(unit.getClass().getSimpleName());
				current++;
				unitCount.put(unit.getClass().getSimpleName(), current);
			} else
			{
				unitCount.put(unit.getClass().getSimpleName(), 1);
			}
		}
		
		// add units which are already in production
		for (City prodCity : state.cities)
		{
			if (prodCity.playerId == myPlayerId)
			{
				totalUnits ++;
				String production = prodCity.productionType;
				if (unitCount.containsKey(production))
				{
					int current = unitCount.get(production);
					current++;
					unitCount.put(production, current);
				} else
				{
					unitCount.put(production, 1);
				}
			}
		}
		
		Map<String, Float> unitRatio = new TreeMap<String, Float>();
		for (String unit : unitTypes)
		{
			float currentCount = unitCount.get(unit);
			
			float targetRatio = 0;
			if (totalUnits > 12)
			{
				targetRatio = mBrain.highBuildRatio.get(unit);
			} else
			{
				targetRatio = mBrain.lowBuildRatio.get(unit);
			}
			
			float ratio = (currentCount + 1) / targetRatio;
			unitRatio.put(unit, ratio);
			
			//Log.i("AI", "City production considering: " + unit + "(Current : " + currentCount + ")");
		}
		
		
		// now find the one with the lowest value
		String bestBuild = "";
		float lowestRatioVal = 999999;
		for (Entry<String, Float> entry : unitRatio.entrySet())
		{
			float ratio = entry.getValue();
			String unitType = entry.getKey();
			if (ratio < lowestRatioVal)
			{
				bestBuild = unitType;
				lowestRatioVal = ratio;
			}
		}
		
		if (bestBuild.length() > 0)
		{
			EventChangeProduction event = new EventChangeProduction();
			event.cityId = city.cityId;
			event.unitClass = bestBuild;
			mInjector.AddEvent(event);
		}
		
		//Log.i("AI", "City production changed to: " + event.unitClass);
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
		
		// clear any defendcity tasks
		
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
	
	public boolean IsLand(final int x, final int y, int[][] map)
	{
		if (map[x][y] == TactikonState.TileType_Land) return true;
		if (map[x][y] == TactikonState.TileType_Mountain) return true;
		if (map[x][y] == TactikonState.TileType_Jungle) return true; 
		if (map[x][y] == TactikonState.TileType_Port) return true; 
		
		return false;
	}
	
	int[][] CreateLandmassMap(int[][] map, TactikonState state)
	{
		int[][] massMap = new int[state.mapSize][state.mapSize];
		int val = 0;
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (massMap[x][y] == 0)
				{
					val ++;
					FloodFill(x, y, state.mapSize, val, IsLand(x, y, map), massMap, map);
				}
			}
		}
		
		return massMap;
	}
	
	
	
	Task AssignTask(int unitId, TactikonState state)
	{
		IUnit unit = state.GetUnit(unitId);
		AIInfo info = new AIInfo(state, unit, massMap);
		PathFinder pathFinder = new PathFinder(state, unit, info);
		float bestScore = -1;
		Task bestTask = null;
		
		UnitTypeEvaluator evals = mBrain.evaluators.get(unit.getClass().getSimpleName());
		
		Map<IAIEvaluator, Float> evaluators = null;
		int tVal = territoryMap[unit.GetPosition().x][unit.GetPosition().y];
		if (tVal == 0) evaluators = evals.myTerritoryEvaluation;
		if (tVal == 1) evaluators = evals.disputedTerritoryunitEvaluation;
		if (tVal == 2) evaluators = evals.enemyTerritoryunitEvaluation;
		
		//Log.i("AI", "Considering unit: " + unit.getClass().getSimpleName());
		
		for (Entry<IAIEvaluator, Float>entry : evaluators.entrySet())
		{
			IAIEvaluator evaluator = entry.getKey();
			float weighting = entry.getValue();
			Task task = evaluator.EvaluateTask(info, pathFinder, unit, state, taskList, mBrain);
			
			if (task != null)
			{
				float score = (task.score / (task.turns + 1)) * weighting;
				//Log.i("AI", "Evaluator: " + evaluator.getClass().getSimpleName() + " Score: " + score);
			}
			
			if (task != null)
			{
				task = evaluator.CheckTask(state, task, pathFinder, info, taskList);
				//if (task == null) Log.i("AI", "Evaluator: " + evaluator.getClass().getSimpleName() + " failed check.");
				if (task != null)
				{
					float score = (task.score / (task.turns + 1)) * weighting;
					if (score > bestScore)
					{
						bestTask = task;
						bestScore = score;
					}
				}
			}
		}
		
		if (bestTask != null)
		{
			taskList.AssignUnitTask(unitId, bestTask);
			//Log.i("AI", "Assigned: " + unit.GetName() + " Task: " + bestTask.evaluator.getClass().getSimpleName());
		}
		
		return bestTask;
	}
	
	boolean DoObviousTasks(IUnit unit, TactikonState state)
	{
		
		// return true if we do something
		ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
		if (unit.CanCaptureCity() == true && moves.size() > 0)
		{
			for (City city : state.cities)
			{
				if (city.playerId != unit.mUserId && city.fortifiedUnits.size() == 0)
				for (Position move : moves)
				{
					if (move.x == city.x && move.y == city.y)
					{
						// capture the city
						EventMoveUnit event = new EventMoveUnit();
						event.mUnitId = unit.mUnitId;
						event.mFrom = unit.GetPosition();
						event.mTo = move;
						//System.out.println("Obvious: capturing city");
						mInjector.AddEvent(event);
						return true;
					}
				}
			}
		}
		
		// attack if we're confident enough of success
		ArrayList<Integer> attacks = state.GetPossibleAttacks(unit.mUnitId);
		for (Integer attackId : attacks)
		{
			Battle battle = new Battle(state, unit.mUnitId, attackId);
			// find the probability of doing damage but not receiving any
			float probability = 0;
			for (Outcome outcome : battle.out)
			{
				if (outcome.unitId1 == unit.mUnitId)
				{
					if (outcome.unitId1Damage == 0) probability = probability + outcome.probability;
				} else
				{
					if (outcome.unitId2Damage == 0) probability = probability + outcome.probability;
				}
			}
			
			if (mBrain.unitAggressiveness.get(unit.getClass().getSimpleName()) >= (100 - (probability * 100)))
			{
				EventAttackUnit event = new EventAttackUnit();
				event.attackingUnitId = unit.mUnitId;
				event.defendingUnitId = attackId;
				mInjector.AddEvent(event);
				//System.out.println("Obvious: attacking enemy");
				return true;
			}
		}
		if (moves == null) return false;
		// move to defend a city which is otherwise undefended
		if (moves.size() > 0)
		{
			for (City city : state.cities)
			{
				if (city.playerId != unit.mUserId) continue;
				
				if (city.fortifiedUnits.size() == 0)
				{
					for (Position move : moves)
					{
						if (move.x == city.x && move.y == city.y)
						{
							// then if the enemy can take it...
							for (Entry<Integer, IUnit> entry : state.units.entrySet())
							{
								IUnit enemy = entry.getValue();
								if (enemy.mUserId == unit.mUserId) continue;
								if (enemy.CanCaptureCity())
								{
									int dist = enemy.GetMovementDistance();
									if (enemy.mCarriedBy != -1)
									{
										IUnit carryUnit = state.GetUnit(enemy.mCarriedBy);
										dist = dist + carryUnit.GetMovementDistance();
									}
									
									if (Math.abs(enemy.GetPosition().x - city.x) + Math.abs(enemy.GetPosition().y - city.y) <= dist)
									{
										// then move to defend the city
										EventMoveUnit event = new EventMoveUnit();
										event.mUnitId = unit.mUnitId;
										event.mFrom = unit.GetPosition();
										event.mTo = move;
										//System.out.println("Obvious: defending empty city");
										mInjector.AddEvent(event);
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
				
		
		return false;
	}
	
	boolean TickAI(TactikonState state)
	{
		
		ArrayList<IUnit> updatedList = new ArrayList<IUnit>();
		for (IUnit unit : mUnitList)
		{
			if (mState.units.containsKey(unit.mUnitId))
			{
				updatedList.add(mState.units.get(unit.mUnitId));
			}
		}
		
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (updatedList.contains(unit) == false) updatedList.add(unit);
		}
		
		mUnitList = updatedList;
		
		for (IUnit unit : mUnitList)
		{
			if (unit.mUserId != myPlayerId) continue;
			if (DoObviousTasks(unit, state) == true) return true;
		}
			
		for (IUnit unit : mUnitList)
		{
			if (unit.mUserId != myPlayerId) continue;
		
			if (unit.bAttacked == true && unit.bMoved == true) continue;
			if (taskList.GetTaskForUnitId(unit.mUnitId) == null)
			{
				AssignTask(unit.mUnitId, state);
			}
		}
		
		// for units with a task, check the task is still valid, then action it.
		// TODO: we should move empty transports which are have orders to pick up a unit first
		for (IUnit unit : mUnitList)
		{

			if (unit.mUserId != myPlayerId) continue;
			if (unit.bAttacked == true && unit.bMoved == true) continue;
			Task task = taskList.GetTaskForUnitId(unit.mUnitId);
			if (task != null)
			{
				AIInfo info = new AIInfo(state, unit, massMap);
				PathFinder pathFinder = new PathFinder(state, unit, info);
				IAIEvaluator evaluator = task.evaluator;
				task = evaluator.CheckTask(state, task, pathFinder, info, taskList);
				
				if (task != null && task.actioned == false)
				{
					task.evaluator.ActionTask(mInjector, state, task);
					//Log.i("AIAction", "Task Actioned: " + task.evaluator.getClass().getSimpleName() + " Result: " + task.actioned);
					if (task.finished == true)
					{
						taskList.ClearTask(task.myUnitId);
					}
					if (task.actioned == true) 
					{
						mUnitList.remove(unit);
						mUnitList.add(unit);
						return true;
					}
				} else if (task == null)
				{
					//Log.i("AIAction", "Task Cleared: " + evaluator.getClass().getSimpleName());
					taskList.ClearTask(unit.mUnitId);
					mUnitList.remove(unit);
					mUnitList.add(unit); // move the unit to the back of the list
					return false;
				}
			}
		}
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != myPlayerId) continue;
			if (DoObviousTasks(unit, state) == true) return true;
		}
		
		
		// if we get through to here, we've finished the turn
		EndTurnEvent(myPlayerId, state);
		return true;
	}

}
