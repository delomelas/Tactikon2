package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.util.Log;

import uk.co.eidolon.tact2.GameView;
import uk.co.eidolon.tact2.AISimple.PathFinder.Node;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class DeliverUnitToFriendlyCity extends IAIEvaluator
{
	public DeliverUnitToFriendlyCity(EventInjector injector)
	{
		super(injector);
	}
	
	int GetDistToEnemy(int x, int y, TactikonState state, int playerId)
	{
		int minDist = 999;
		
		for (City city : state.cities)
		{
			if (city.playerId != playerId && city.playerId != -1)
			{
				int dist = Math.abs(city.x - x) + Math.abs(city.y - y);
				if (dist < minDist) minDist = dist;
			}
		}
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != playerId)
			{
				int dist = Math.abs(unit.GetPosition().x - x) + Math.abs(unit.GetPosition().y - y);
				if (dist < minDist) minDist = dist;
			}
		}
		
		return minDist;
	}

	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.mCarrying.size() == 0)
		{
			return null;
		}
		
		int carrying = unit.mCarrying.get(0);
		IUnit unitOnBoard = state.GetUnit(carrying);
		
		int myEnemyDist = GetDistToEnemy(unit.GetPosition().x, unit.GetPosition().y, state, unit.mUserId);
		
		ArrayList<City> friendlyCities = new ArrayList<City>();
		for (City city : state.cities)
		{
			if (city.x == unit.GetPosition().x && city.y == unit.GetPosition().y) continue;
			if (city.playerId != unit.mUserId) continue;
			
			if (city.fortifiedUnits.size() == 4) continue;
			
			int cityEnemyDist = GetDistToEnemy(city.x, city.y, state,  unit.mUserId);
			
			if (cityEnemyDist < myEnemyDist)
				friendlyCities.add(city);
		}
		
 
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, unitOnBoard, info);
		
		City closestCity = null;
		int bestDist = 0;
		ArrayList<Position> bestRoute = null;
		
		
		// sort the cities by simple distance so that we consider the closer ones first and likely eliminate all others
		Map<Float, City> sortedCities = new TreeMap<Float, City>();
		float offset = 0;
		for (City city : friendlyCities)
		{
			float dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			dist = dist + offset;
			offset = offset + 0.00001f;
			sortedCities.put(dist, city);
		}
		
		
		for (Entry<Float, City> entry : sortedCities.entrySet())
		{
			City city = entry.getValue();
			boolean bHasRoute = transportPathFinder.Calculate(city.x, city.y, state.mapSize);
			
			if (bHasRoute)
			{
				int turns = transportPathFinder.GetWholeRouteTurns();
				
				if (unit.GetMaxFuel() != -1)
				{
					if (unit.fuelRemaining -3 < transportPathFinder.GetTransporterRouteCost()) continue;
				}
				
				
				if (turns > bestDist && turns > 0 && transportPathFinder.GetTransporterRoute().size() > 0)
				{
					bestDist = turns;
					//bestCost = transportPathFinder.GetTransporterRouteCost();
					closestCity = city;
					bestRoute = transportPathFinder.GetTransporterRoute();
				}
			}
		}
		
		if (closestCity == null || bestRoute == null) 
		{
			return null;
		}
		
		Task result = new Task();
		
		
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 100;
		result.targetCityId = closestCity.cityId;
		result.targetUnitId = unitOnBoard.mUnitId;
		result.turns = bestDist;
		result.route = bestRoute;
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task,
			PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.mCarrying.size() == 0) return null;
		
		City city = state.GetCity(task.targetCityId);
		if (city.playerId != unit.mUserId) return null;
		
		if (city.fortifiedUnits.size() == 4) return null; // can't continue if the city is full - choose another city
		
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove == null) return null;
		
		IUnit transportedUnit = state.GetUnit(unit.mCarrying.get(0));
		
		// if the transported unit can get there quicker walking, consider this task over
		Position transportedBestMove = GetBestMove(state, task.route, transportedUnit);
		if (transportedBestMove != null)
		{
			AIInfo newInfo = new AIInfo(state, transportedUnit, info.massMap);
			PathFinder transportedUnitPathfinder = new PathFinder(state, transportedUnit, newInfo);
			boolean canRoute = transportedUnitPathfinder.Calculate(city.x, city.y, 999);
			if (canRoute == true)
			{
				if (transportedUnitPathfinder.GetRouteTurns() < task.turns) return null; 
			}
			
		}
		
		if (task.targetUnitId != transportedUnit.mUnitId) return null;
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, transportedUnit, info);
		boolean bHasRoute = transportPathFinder.Calculate(city.x, city.y, 999);
		if (!bHasRoute) return null;
		
		if (transportPathFinder.GetTransporterRoute().size() == 0) return null;
		
		task.route = transportPathFinder.GetTransporterRoute();
		task.turns = transportPathFinder.GetTransporterRouteTurns();
		task.targetPosition = transportPathFinder.GetTransitionPoint();
		
		return task;
	}

	@Override
	public void ActionTask(EventInjector injector, TactikonState state,
			Task task)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.bMoved == true) return;
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove != null)
		{
			IEvent event = MoveEvent(state, task.myUnitId, bestMove.x, bestMove.y);
			injector.AddEvent(event);
			task.actioned = true;
		}
	}

}
