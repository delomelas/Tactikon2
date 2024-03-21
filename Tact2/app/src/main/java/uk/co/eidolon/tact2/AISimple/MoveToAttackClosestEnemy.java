package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;


import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.Battle;
import Tactikon.State.City;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.Battle.Outcome;

public class MoveToAttackClosestEnemy extends IAIEvaluator
{
	public MoveToAttackClosestEnemy(EventInjector injector)
	{
		super(injector);
	}



	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.bMoved == true)
		{
			return null;
		}
		
		if (unit.mCarrying.size() != 0) return null;
		
		if (unit.getClass().getSimpleName().compareTo("UnitBoatTransport") == 0) return null;
		if (unit.getClass().getSimpleName().compareTo("UnitHelicopter") == 0) return null;
		
		// if we can already attack, return false - we don't need to move to be able to attack
		ArrayList<Integer> attacks = state.GetPossibleAttacks(unit.mUnitId);
		if (attacks.size() > 0)
		{
			return null;
		}
		
		// scan through all the enemies
		
		IUnit closestEnemy = null;
		int bestTurns = 999;
		int bestCost = 999;
		ArrayList<Position> bestRoute = null;
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			// try every point at which we can attack the enemy from
			ArrayList<Position> attackPoints = new ArrayList<Position>();
			IUnit enemy = entry.getValue();
			if (enemy.mUserId == unit.mUserId) continue;
			
			// check that this enemy is worth attacking
			Battle battle = new Battle(state, unit.mUnitId, enemy.mUnitId);
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
			
			if (brain.unitAggressiveness.get(unit.getClass().getSimpleName()) <= (100 - (probability * 100)))
			{
				continue;
			}
			
			for (int x = enemy.GetPosition().x - unit.GetAttackRange(); x<= enemy.GetPosition().x + unit.GetAttackRange(); ++x)
			{
				for (int y = enemy.GetPosition().y - unit.GetAttackRange(); y<= enemy.GetPosition().y + unit.GetAttackRange(); ++y)
				{
					if (x < 0 || y < 0 || x > info.mapSize - 1 || y > info.mapSize - 1) continue;
					
					int dist = Math.abs(x - enemy.GetPosition().x) + Math.abs(y - enemy.GetPosition().y);
					if (dist <= unit.GetAttackRange())
					{
						attackPoints.add(new Position(x, y));
					}
				}
			}
			
			for (Position attackPoint : attackPoints)
			{
				boolean bHasRoute = pathFinder.Calculate(attackPoint.x, attackPoint.y, bestCost);
				if (bHasRoute)
				{
					if (unit.GetMaxFuel() != -1)
					{
						if (unit.fuelRemaining / 2 < pathFinder.GetRouteCost()) continue;
					}
					
					int dist = pathFinder.GetRouteTurns();
					if (dist < bestTurns)
					{
						bestTurns = dist;
						closestEnemy = enemy;
						bestCost = pathFinder.GetRouteCost();
						bestRoute = pathFinder.GetRoute();
					}
				}
			}
		}
		
		if (closestEnemy == null || bestRoute == null) 
		{
			return null;
		}
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.score = 100; // score should be related to probability of killing enemy
		result.targetUnitId = closestEnemy.mUnitId;
		result.turns = bestTurns;
		result.route = bestRoute; 
		
		
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task, PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit enemy = state.GetUnit(task.targetUnitId);
		if (enemy == null) return null;
		
		IUnit unit = state.GetUnit(task.myUnitId);
		
		// check there isn't something that wants to use us as a transport
		{
			ArrayList<Task> tasks = taskList.GetTasksTargettingUnit(task.myUnitId);
			for (Task otherTask : tasks)
			{
				if (otherTask.evaluator.getClass() == BoardTransport.class)
				{
					return null;
				}
			}
		}
		
		
		
		// check i'm not being delivered somewhere faster than I can walk
		//IUnit unit = state.GetUnit(task.myUnitId);
		if (unit.mCarriedBy != -1)
		{
			ArrayList<Task> tasks = taskList.GetTasksTargettingUnit(task.myUnitId);
			for (Task otherTask : tasks)
			{
				if (otherTask.evaluator.getClass() == DeliverUnitToCaptureCity.class || 
					otherTask.evaluator.getClass() == DeliverUnitToFriendlyCity.class ||
					otherTask.evaluator.getClass() == DeliverUnitToCaptureEnemyCity.class)
				{
				// 	can the transport move any further along the route
					IUnit transporter = state.GetUnit(otherTask.myUnitId);
					if (transporter == null) continue;
					Position transportMove = otherTask.evaluator.GetBestMove(state, otherTask.route, transporter);
					if (transportMove != null) return null;
				}
			}
			//Log.i("AI", this.getClass().getSimpleName() + " used instead of delivery for unit " + unit.getClass().getSimpleName());
			
		}
		
		ArrayList<Position> attackPoints = new ArrayList<Position>();
		for (int x = enemy.GetPosition().x - unit.GetAttackRange(); x<= enemy.GetPosition().x + unit.GetAttackRange(); ++x)
		{
			for (int y = enemy.GetPosition().y - unit.GetAttackRange(); y<= enemy.GetPosition().y + unit.GetAttackRange(); ++y)
			{
				if (x < 0 || y < 0 || x > info.mapSize - 1 || y > info.mapSize - 1) continue;
				
				int dist = Math.abs(x - enemy.GetPosition().x) + Math.abs(y - enemy.GetPosition().y);
				if (dist <= unit.GetAttackRange())
				{
					attackPoints.add(new Position(x, y));
				}
			}
		}
		
		int bestTurns = 999;
		int bestCost = 999;
		ArrayList<Position> bestRoute = null;
		for (Position attackPoint : attackPoints)
		{
			boolean bHasRoute = pathFinder.Calculate(attackPoint.x, attackPoint.y, 999);
			if (bHasRoute)
			{
				int dist = pathFinder.GetRouteTurns();
				if (dist < bestTurns)
				{
					bestTurns = dist;
					bestCost = pathFinder.GetRouteCost();
					bestRoute = pathFinder.GetRoute();
				}
			}
		}
		
		if (bestRoute == null) return null;
		
		// check i'm not going to run out of fuel
		if (unit.GetMaxFuel() != -1)
		{
			if (unit.fuelRemaining / 2 < bestCost) return null;
		}
		
		// need to also check that there's still no danger along the route
		
		task.route = bestRoute;
		task.turns = bestTurns;
		
		return task;
	}
	

	@Override
	public void ActionTask(EventInjector injector, TactikonState state, Task task)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		
		ArrayList<Integer> attacks = state.GetPossibleAttacks(unit.mUnitId);
		for (Integer attackId : attacks)
		{
			IUnit attackUnit = state.GetUnit(attackId);
			IUnit targetUnit = state.GetUnit(task.targetUnitId);
			if (attackUnit.GetPosition().x == targetUnit.GetPosition().x || attackUnit.GetPosition().y == targetUnit.GetPosition().y)
			{
				if (unit.bAttacked == false)
				{
					IEvent event = AttackEvent(state, task.myUnitId, attackId);
					injector.AddEvent(event);
					task.actioned = true;
				}
				return;
			}
		}
		
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove != null)
		{
			if (unit.bMoved == false)
			{
				IEvent event = MoveEvent(state, task.myUnitId, bestMove.x, bestMove.y);
				injector.AddEvent(event);
				task.actioned = true;
			}
		}
		
	}

}
