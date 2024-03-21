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

public class PickupUnit extends IAIEvaluator
{
	public PickupUnit(EventInjector injector)
	{
		super(injector);
	}

	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		
		if (unit.mCarrying.size() != 0)
		{
			return null;
		}
		
		ArrayList<IUnit> pickupUnits = new ArrayList<IUnit>();
		
		// we'll only attempt this action if there's a unit with a "boardtransport" event pending - so find the closest one that matches
		ArrayList<Task> tasks = taskList.GetTasksTargettingUnit(unit.mUnitId);
		for (Task task : tasks)
		{
			if (task.evaluator.getClass() == BoardTransport.class || task.evaluator.getClass() == Refuel.class)
			{
				IUnit taskUnit = state.GetUnit(task.myUnitId);
				if (taskUnit != null) pickupUnits.add(taskUnit);
			}
		}
		
		
		int bestTurns = 999;
		ArrayList<Position> bestRoute = null;
		IUnit bestPickup = null;
		int bestCost = 999;
		for (IUnit pickupUnit : pickupUnits)
		{
			TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, pickupUnit, info);	
		
			boolean bHasRoute = transportPathFinder.Calculate(pickupUnit.GetPosition().x, pickupUnit.GetPosition().y, bestCost);
			
			
			if (bHasRoute)
			{
				int turns = transportPathFinder.GetTransporterRouteTurns();
				
				if (unit.GetMaxFuel() != -1)
				{
					if (unit.fuelRemaining /2 < transportPathFinder.GetTransporterRouteCost()) continue;
				}
								
				if (turns < bestTurns)
				{
					bestTurns = turns;
					bestCost = transportPathFinder.GetTransporterRouteCost();
					bestPickup = pickupUnit;
					bestRoute = transportPathFinder.GetTransporterRoute();
				}
			}
		}
		
		if (bestRoute == null) 
		{
			return null;
		}
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 100;
		result.targetUnitId = bestPickup.mUnitId;
		result.turns = bestTurns;
		result.route = bestRoute;
		
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task,
			PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.mCarrying.size() != 0) return null;
				
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove == null) return null;
		
		IUnit pickupUnit = state.GetUnit(task.targetUnitId);
		if (pickupUnit == null) return null;
		
		TransportPathFinder transportPathFinder = new TransportPathFinder(state, unit, pickupUnit, info);
		boolean bHasRoute = transportPathFinder.Calculate(pickupUnit.GetPosition().x, pickupUnit.GetPosition().y, 999);
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
