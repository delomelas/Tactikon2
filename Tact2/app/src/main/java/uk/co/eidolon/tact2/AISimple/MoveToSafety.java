package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;


import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class MoveToSafety extends IAIEvaluator
{
	public MoveToSafety(EventInjector injector)
	{
		super(injector);
	}



	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.bMoved == true || unit.mCarriedBy != -1)
		{
			return null;
		}
		
		// if we can attack, we should stick around instead in order to attack
		ArrayList<Integer> attacks = state.GetPossibleAttacks(unit.mUnitId);
		if (attacks.size() > 0)
		{
			return null;
		}
		
		if (info.dangerMap[unit.GetPosition().x][unit.GetPosition().y] == 0) return null;
		
		ArrayList<Position> safeMoves = new ArrayList<Position>();
		ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
		for (Position move : moves)
		{
			if (info.dangerMap[move.x][move.y] == 0)
			{
				safeMoves.add(move);
			}
		}
		
		int closestDist = 999;
		Position safeMove = null;
		for (Position potentialSafeMove : safeMoves)
		{
			int dist = Math.abs(potentialSafeMove.x - unit.GetPosition().x) + Math.abs(potentialSafeMove.y - unit.GetPosition().y);
			if (dist < closestDist)
			{
				safeMove = potentialSafeMove;
				closestDist = dist;
			}
		}
		
		if (safeMove == null) return null;
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 100;
		result.targetPosition = safeMove;
		result.turns = 0;
		
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task, PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit enemy = state.GetUnit(task.targetUnitId);
		if (enemy == null) return null;
		
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.bMoved == true) return null;
		
		// check that it's still a possible move...
		ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
		
		boolean bFound = false;
		for (Position move : moves)
		{
			if (move.x == task.targetPosition.x && move.y == task.targetPosition.y)
			{
				bFound = true;
				break;
			}
		}
		if (bFound == false) return null;
		
		// check i'm not going to run out of fuel
		if (unit.GetMaxFuel() != -1)
		{
			if (unit.fuelRemaining < unit.GetMovementDistance()) return null;
		}
		
		if (info.dangerMap[task.targetPosition.x][task.targetPosition.y] != 0) return null;
		
		return task;
	}
	

	@Override
	public void ActionTask(EventInjector injector, TactikonState state, Task task)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.bMoved == true) return;
		IEvent event = MoveEvent(state, task.myUnitId, task.targetPosition.x, task.targetPosition.y);
		injector.AddEvent(event);
		task.actioned = true;
		
	}

}
