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

public class DeliverUnitToCaptureCity extends IAIEvaluator
{
	public DeliverUnitToCaptureCity(EventInjector injector)
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
			if (city.playerId != -1) continue;
			citiesToCapture.add(city);
		}
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, unitOnBoard, info);
		
		City closestCity = null;
		int bestDist = 999;
		int bestCost = 999;
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
		
		for (Entry<Float, City> entry : sortedCities.entrySet())
		{
			City city = entry.getValue();
			boolean bHasRoute = transportPathFinder.Calculate(city.x, city.y, bestCost);
			
			if (bHasRoute)
			{
				int turns = transportPathFinder.GetWholeRouteTurns();
				
				if (unit.GetMaxFuel() != -1)
				{
					if (unit.fuelRemaining -2 < transportPathFinder.GetTransporterRouteCost()) continue;
				}
				boolean bDanger = false;
				if (turns > 1)
				{
					
					ArrayList<TransportPathFinder.Node> route = transportPathFinder.mTransporterRoute;
					for (TransportPathFinder.Node node : route)
					{
						if (info.dangerMap[node.x][node.y] > 0) 
						{
							bDanger = true;
							break;
						}
					}
				}
				
				// check there isn't another task that's already delivering a unit to this city
				ArrayList<Task> tasks = taskList.GetCityTasks(this.getClass(), city.cityId);
				tasks.addAll(taskList.GetCityTasks(MoveToCaptureCityNoDanger.class, city.cityId));
				for (Task task : tasks)
				{
					if (task.turns <= turns) turns = bestDist + 1;
				}
				
				if (bDanger == true) turns = turns * 2; // consider dangerous cities to be twice as many turns away
				
				if (turns < bestDist)
				{
					bestDist = turns;
					bestCost = transportPathFinder.GetTransporterRouteCost();
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
		if (city.playerId != -1) return null;
		
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
				if (transportedUnitPathfinder.GetRouteTurns() < task.turns)
				{	
					return null; 
				}
			}
			
		}
		
		if (task.targetUnitId != transportedUnit.mUnitId) return null;
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, transportedUnit, info);
		boolean bHasRoute = transportPathFinder.Calculate(city.x, city.y, 999);
		if (!bHasRoute) return null;
		
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
