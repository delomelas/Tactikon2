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

public class DeliverUnitToCaptureEnemyCity extends IAIEvaluator
{
	public DeliverUnitToCaptureEnemyCity(EventInjector injector)
	{
		super(injector);
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
		if (unitOnBoard.CanCaptureCity() == false)
		{
			return null;
		}
		
		
		
		ArrayList<City> citiesToCapture = new ArrayList<City>();
		for (City city : state.cities)
		{
			if (city.playerId == -1 || city.playerId == unit.mUserId) continue;
			citiesToCapture.add(city);
		}
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, unitOnBoard, info);
		
		int bestDist = 999;
		int bestCost = 999;
		Position bestLandingPoint = null;
		ArrayList<Position> bestRoute = null;
		
		
		// sort the cities by simple distance so that we consider the closer ones first and likely eliminate all others
		Map<Float, City> sortedCities = new TreeMap<Float, City>();
		float offset = 0;
		for (City city : citiesToCapture)
		{
			float dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			dist = dist + offset;
			offset = offset + 0.00001f;
			sortedCities.put(dist, city);
		}
		
		int numCitiesToConsider = 5;
		numCitiesToConsider = Math.max(numCitiesToConsider, sortedCities.size());
		
		ArrayList<Position> landingPoints = new ArrayList<Position>();
		int num = 0;
		for (Entry<Float, City> entry : sortedCities.entrySet())
		{
			if (num > numCitiesToConsider) break;
			
			City city = entry.getValue();
			
			
			for (int x = city.x - 3; x < city.x + 3; ++x)
			{
				if (x < 0 || x > state.mapSize - 1) continue;
				for (int y = city.y - 3; y <= city.y + 3; ++y)
				{
					if (y < 0 || y > state.mapSize - 1) continue;
					Position landingPoint = new Position(x, y);
					if (info.massMap[x][y] == info.massMap[city.x][city.y] && unitOnBoard.CanMove(landingPoint,  landingPoint, state))
					{
						landingPoints.add(landingPoint);
					}
				}
			}
			
		}
		
		
		for (Position landingPoint : landingPoints)
		{
			boolean bHasRoute = transportPathFinder.Calculate(landingPoint.x, landingPoint.y, bestCost);
			
			if (bHasRoute)
			{
				int turns = transportPathFinder.GetWholeRouteTurns();
				
				if (unit.GetMaxFuel() != -1)
				{
					if (unit.fuelRemaining / 2 < transportPathFinder.GetTransporterRouteCost()) continue;
				}
				
				
				if (turns < bestDist)
				{
					bestDist = turns;
					bestCost = transportPathFinder.GetTransporterRouteCost();
					bestLandingPoint = landingPoint;
					bestRoute = transportPathFinder.GetTransporterRoute();
				}
			}
		}
		
		if (bestLandingPoint == null || bestRoute == null) 
		{
			return null;
		}
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 100;
		result.targetPosition = bestLandingPoint;
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
		
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove == null) return null;
		
		Position landingPoint = task.targetPosition;
		
		IUnit transportedUnit = state.GetUnit(unit.mCarrying.get(0));
		
		// if the transported unit can get there quicker walking, consider this task over
		Position transportedBestMove = GetBestMove(state, task.route, transportedUnit);
		if (transportedBestMove != null)
		{
			AIInfo newInfo = new AIInfo(state, transportedUnit, info.massMap);
			PathFinder transportedUnitPathfinder = new PathFinder(state, transportedUnit, newInfo);
			boolean canRoute = transportedUnitPathfinder.Calculate(landingPoint.x, landingPoint.y, 999);
			if (canRoute == true)
			{
				if (transportedUnitPathfinder.GetRouteTurns() < task.turns) return null; 
			}
			
		}
		
		if (task.targetUnitId != transportedUnit.mUnitId) return null;
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, transportedUnit, info);
		boolean bHasRoute = transportPathFinder.Calculate(landingPoint.x, landingPoint.y, 999);
		if (!bHasRoute) return null;
		
		task.route = transportPathFinder.GetTransporterRoute();
		task.turns = transportPathFinder.GetTransporterRouteTurns();
		
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
