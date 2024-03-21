package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.util.Log;

import uk.co.eidolon.tact2.AISimple.PathFinder.Node;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class MoveToCaptureCityNoDanger extends IAIEvaluator
{
	public MoveToCaptureCityNoDanger(EventInjector injector)
	{
		super(injector);
	}

	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.CanCaptureCity() == false || unit.bMoved == true)
		{
			return null;
		}
		
		// collect up the cities we might be able to capture
		
		ArrayList<City> citiesToCapture = new ArrayList<City>();
		for (City city : state.cities)
		{
			if (city.playerId == unit.mUserId || city.fortifiedUnits.size() > 0) continue;
			citiesToCapture.add(city);
		}
		
		Map<Float, City> sortedCities = new TreeMap<Float, City>();
		float offset = 0;
		for (City city : citiesToCapture)
		{
			float dist = Math.abs(unit.GetPosition().x - city.x) + Math.abs(unit.GetPosition().y - city.y);
			dist = dist + offset;
			offset = offset + 0.00001f;
			sortedCities.put(dist, city);
		}
		
		City closestCity = null;
		int bestTurns = 999;
		int bestCost = 999;
		ArrayList<Position> bestRoute = null;
		for (Entry<Float, City> entry : sortedCities.entrySet())
		{
			City city = entry.getValue();
			boolean bHasRoute = pathFinder.Calculate(city.x, city.y, bestCost);
			
			if (bHasRoute)
			{
				int turns = pathFinder.GetRouteTurns();
				
				if (turns > 1)
				{
					boolean bDanger = false;
					ArrayList<Node> route = pathFinder.mRoute;
					for (Node node : route)
					{
						if (info.dangerMap[node.x][node.y] > 0) 
						{
							bDanger = true;
							break;
						}
					}
					if (bDanger == true) continue; // there's danger along this route, try a different one
					

				}
				
				ArrayList<Task> tasks = taskList.GetCityTasks(this.getClass(),  city.cityId);
				for (Task otherTask : tasks)
				{
					if (otherTask.myUnitId == unit.mUnitId) continue;
					if (turns >= otherTask.turns) turns = bestTurns + 1;
				}
				
				
				if (turns < bestTurns)
				{
					bestTurns = turns;
					bestCost = pathFinder.GetRouteCost();
					closestCity = city;
					bestRoute = pathFinder.GetRoute();
				}
			}
		}
		
		if (closestCity == null || bestRoute == null) 
		{
			return null;
		}
		
		Task taskResult = new Task();
		taskResult.myUnitId = unit.mUnitId;
		taskResult.evaluator = this;
		taskResult.score = 100;
		taskResult.targetCityId = closestCity.cityId;
		taskResult.turns = bestTurns;
		taskResult.route = bestRoute;
		
			
		return taskResult;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task, PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		City city = state.GetCity(task.targetCityId);
		if (city.playerId != -1) return null;
		int targetX = city.x;
		int targetY = city.y;
		boolean possible = pathFinder.Calculate(targetX, targetY, 999);
		if (!possible) return null;
		
		// need to also check that there's still no danger along the route
		
		task.route = pathFinder.GetRoute();
		task.turns = pathFinder.GetRouteTurns();
		
		// check no one else is doing this task better than i can
		ArrayList<Task> tasks = taskList.GetCityTasks(this.getClass(),  city.cityId);
		for (Task otherTask : tasks)
		{
			if (otherTask.myUnitId == task.myUnitId) continue;
			if (task.turns >= otherTask.turns) return null;
		}
		
		// check i'm not being delivered somewhere faster than I can walk
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.mCarriedBy != -1)
		{
			tasks = taskList.GetTasksTargettingUnit(task.myUnitId);
			for (Task otherTask : tasks)
			{
				if (otherTask.evaluator.getClass() == DeliverUnitToCaptureCity.class || 
					otherTask.evaluator.getClass() == DeliverUnitToFriendlyCity.class ||
					otherTask.evaluator.getClass() == DeliverUnitToCaptureEnemyCity.class)
				{
				// 	can the transport move any further along the route
					IUnit transporter = state.GetUnit(otherTask.myUnitId);
					Position transportMove = otherTask.evaluator.GetBestMove(state, otherTask.route, transporter);
					if (transportMove != null) return null;
				}
			}
			//Log.i("AI", this.getClass().getSimpleName() + " used instead of delivery for unit " + unit.getClass().getSimpleName());
		}
		
		return task;
	}

	@Override
	public void ActionTask(EventInjector injector, TactikonState state, Task task)
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
