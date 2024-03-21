package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import uk.co.eidolon.tact2.AISimple.PathFinder.Node;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.IUnit.Domain;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class BoardTransport extends IAIEvaluator
{
	public BoardTransport(EventInjector injector)
	{
		super(injector);
	}
	
	int GetDist(Position one, Position two)
	{
		return Math.abs(one.x - two.x) + Math.abs(one.y - two.y); 
	}

	

	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit, TactikonState state, TaskList taskList, AIBrain brain)
	{
		if (unit.bMoved == true || unit.mCarriedBy != -1)
		{
			return null;
		}
		
		ArrayList<IUnit> possibleTransports = new ArrayList<IUnit>();
		
		// find an available transport that can transport me
		for (Entry <Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit transporter = entry.getValue();
			if (transporter.mUserId != unit.mUserId) continue;
			if (!transporter.CanCarry(unit)) continue;
			if (transporter.mCarrying.size() != 0) continue;
			
			Task task = taskList.GetTaskForUnitId(transporter.mUnitId);
			if (task != null)
			{
				if (task.evaluator.getClass() == PickupUnit.class && task.targetUnitId != unit.mUnitId) continue;
			}
			
			// and now need to see if it's possible to rendezous with this transporter
			if (transporter.GetDomain() == Domain.Air)
			{
				int distance = GetDist(transporter.GetPosition(), unit.GetPosition());
				if (distance < transporter.fuelRemaining / 2)
					possibleTransports.add(transporter);
			} else
			{
				possibleTransports.add(transporter);
			}
			
			// don't consider transports which are already tasked with picking up a different unit
			
			
		}
		
		int bestTurns = 999;
		IUnit bestTransporter = null;
		ArrayList<Position> bestRoute = null;
		Position bestPosition = null;
		int bestCost = 999;
		
		ArrayList<Position> possibleMoves = state.GetPossibleMoves(unit.mUnitId);
		
		for (IUnit transporter : possibleTransports)
		{
			// now find the one that's the lowest number of turns to get there
			if (transporter.GetPosition().x == unit.GetPosition().x && transporter.GetPosition().y == unit.GetPosition().y)
			{
				bestTurns = 0;
				bestTransporter = transporter;
				break;
			}
			
			// if we can board it from the current position, then count that as one turn
			for (Position move : possibleMoves)
			{
				if (move.x == transporter.GetPosition().x && move.y == transporter.GetPosition().y)
				{
					bestTurns = 1;
					bestTransporter = transporter;
					bestRoute = null;
					continue;
				}
			}
			
			TransportPathFinder transportPathFinder = new TransportPathFinder(state, transporter, unit, info);
			boolean bHasRoute = transportPathFinder.Calculate(unit.GetPosition().x, unit.GetPosition().y, bestCost);
			
			if (bHasRoute)
			{
				//int turns = transportPathFinder.GetRouteTurns();
				// transport is capable of picking up this unit
				// now pathfind to the transition point to work out the destination
				Position transition = transportPathFinder.GetTransitionPoint();
				boolean bCanReachTransition = pathFinder.Calculate(transition.x, transition.y, 999);
				
				if (bCanReachTransition)
				{
					int turns = pathFinder.GetRouteTurns();
					
					if (turns < bestTurns)
					{
						bestPosition = transition;
						bestCost = pathFinder.GetRouteCost();
						bestTurns = turns;
						bestTransporter = transporter;
						bestRoute = pathFinder.GetRoute();
					}
				}
			}
		}
		
		if (bestTransporter == null)
		{
			return null;
		}
		
		// now pathfind to the furthest point on the route that we can get to
		
				
		Task result = new Task();
		result.myUnitId = unit.mUnitId;
		result.evaluator = this;
		result.turns = bestTurns;
		result.route = bestRoute;
		result.targetPosition = bestPosition;
		result.targetUnitId = bestTransporter.mUnitId;
		result.score = 120;
		
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task,
			PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		// check that the transport we're heading for is still alive
		IUnit transporter = state.GetUnit(task.targetUnitId);
		if (transporter == null) return null;
		
		if (transporter.mCarrying.contains(task.myUnitId)) return null; // can't board if we've already boarded
		
		IUnit unit = state.GetUnit(task.myUnitId);
		
		if (unit.mCarriedBy != -1) return null;
		
		// not enough room!
		if (!transporter.CanCarry(unit) || transporter.CarryCapacity() <= transporter.mCarrying.size()) return null;
		
		boolean bCanBoardNow = false;
		// if we're on the same tile, board now
		if (unit.GetPosition().x == transporter.GetPosition().x && unit.GetPosition().y == transporter.GetPosition().y)
		{
			bCanBoardNow = true;
		}
		
		if (bCanBoardNow == false)
		{
		// 	if we can board now, then do so
			ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
			for (Position move : moves)
			{
				if (move.x == transporter.GetPosition().x && move.y == transporter.GetPosition().y)
				{
					bCanBoardNow = true;
				}
			}
		}
		
		if (bCanBoardNow == false)
		{
			// confirm that we can still reach the transporter
			if (transporter.mCarrying.size() != 0) return null; // give up if the transport has another inhabitant
			
			TransportPathFinder transportPathFinder = new TransportPathFinder(state, transporter, unit, info);
			boolean bHasRoute = transportPathFinder.Calculate(unit.GetPosition().x, unit.GetPosition().y, 999);
			if (!bHasRoute) return null;
			Position transition = transportPathFinder.GetTransitionPoint();
			boolean bCanReachTransition = pathFinder.Calculate(transition.x, transition.y, 999);
			if (!bCanReachTransition) return null;
				
			task.route = pathFinder.GetRoute();
			task.turns = pathFinder.GetRouteTurns();
		}
		
		return task;
	}

	@Override
	public void ActionTask(EventInjector injector, TactikonState state, Task task)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		IUnit transporter = state.GetUnit(task.targetUnitId);
		
		if (!transporter.CanCarry(unit) || transporter.CarryCapacity() <= transporter.mCarrying.size()) return;
		
		// if we're on the same tile, board now
		if (unit.GetPosition().x == transporter.GetPosition().x && unit.GetPosition().y == transporter.GetPosition().y)
		{
			IEvent event = BoardEvent(state, task.myUnitId, task.targetUnitId);
			injector.AddEvent(event);
			task.actioned = true;
			task.finished = true;
			return;
		}

		
		if (unit.bMoved == true) return;
		
		// if we can board now, then do so
		ArrayList<Position> moves = state.GetPossibleMoves(unit.mUnitId);
		for (Position move : moves)
		{
			if (move.x == transporter.GetPosition().x && move.y == transporter.GetPosition().y)
			{
				IEvent event = MoveEvent(state, unit.mUnitId, move.x, move.y);
				injector.AddEvent(event);
				task.actioned = true;
				return;
			}
		}
		
		// otherwise find the largest move on the route that we can get to
		Position bestMove = GetBestMove(state, task.route, unit);
		if (bestMove != null)
		{
			IEvent event = MoveEvent(state, unit.mUnitId, bestMove.x, bestMove.y);
			injector.AddEvent(event);
			task.actioned = true;
		} 
		
	}

}
