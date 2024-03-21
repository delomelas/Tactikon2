package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

import uk.co.eidolon.tact2.GameView;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class Refuel extends IAIEvaluator
{
	public Refuel(EventInjector injector)
	{
		super(injector);
	}

	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.GetMaxFuel() == -1) return null;
		
		ArrayList<Position> refuelPoints = new ArrayList<Position>();
		
		for (City city : state.cities)
		{
			if (city.playerId != unit.mUserId) continue;
			if (city.fortifiedUnits.size() == state.MAX_UNITS_IN_CITY) continue;
			
			refuelPoints.add(new Position(city.x, city.y));
		}
		
		int refuelCarrierId = -1;
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit carrier = entry.getValue();
			if (carrier.CanCarry(unit) && carrier.mUserId == unit.mUserId)
			{
				refuelPoints.add(new Position(carrier.GetPosition().x, carrier.GetPosition().y));
			}
		}
		
		int bestTurns = 999;
		int bestCost =999;
		ArrayList<Position> bestRoute = null;
		Position bestRefuel = null;
		for (Position refuelPoint : refuelPoints)
		{
			boolean bHasRoute = pathFinder.Calculate(refuelPoint.x, refuelPoint.y, bestCost);
			
			if (bHasRoute)
			{
				int turns = pathFinder.GetRouteTurns();
				
				if (unit.fuelRemaining  < pathFinder.GetRouteCost()) continue;
								
				if (turns < bestTurns)
				{
					bestTurns = turns;
					bestRefuel = refuelPoint;
					bestRoute = pathFinder.GetRoute();
					bestCost = pathFinder.GetRouteCost();
				}
			}
		}
		
		if (bestRoute == null) 
		{
			return null;
		}
		
		// we've got enough fuel to get to the refuel point next turn
		if (unit.fuelRemaining - (unit.GetMovementDistance() * 2) > bestCost)
		{
			return null;
		}
		
		// is the refuel point a carrier?
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit carrier = entry.getValue();
			if (carrier.GetPosition().x == bestRefuel.x && carrier.GetPosition().y == bestRefuel.y)
			{
				refuelCarrierId = carrier.mUnitId;
				break;
			}
		}
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 2000;
		result.targetUnitId = refuelCarrierId;
		result.turns = bestTurns;
		result.route = bestRoute;
		result.targetPosition = bestRefuel;
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task,
			PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
				
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove == null) return null;
		
		if (task.targetUnitId != -1)
		{
			IUnit carrier = state.GetUnit(task.targetUnitId);
			if (carrier == null) return null;
			task.targetPosition = new Position(carrier.GetPosition().x, carrier.GetPosition().y);
		}
		
		boolean bHasRoute = pathFinder.Calculate(task.targetPosition.x, task.targetPosition.y, 999);
		if (!bHasRoute) return null;
		
		task.route = pathFinder.GetRoute();
		task.turns = pathFinder.GetRouteTurns();
		
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
