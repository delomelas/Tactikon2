package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;

import uk.co.eidolon.tact2.AISimple.PathFinder.Node;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.EventBoardUnit;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public abstract class IAIEvaluator implements Comparable<IAIEvaluator>
{
	EventInjector mInjector;
	
	IAIEvaluator(EventInjector injector)
	{
		mInjector = injector;
	}
	
	public int compareTo(IAIEvaluator arg0)
	{
		return this.getClass().getSimpleName().compareTo(arg0.getClass().getSimpleName());
	}
	
	Position GetBestMove(TactikonState state, ArrayList<Position> route, IUnit unit)
	{
		ArrayList<Position> moves = state.GetPotentialMoves(unit.mUnitId);
		
		Position bestMove = null;
		int bestIndex = -1;
		for (Position move : moves)
		{
			int idx = 0;
			for (Position pos : route)
			{
				if (pos.x == move.x && pos.y == move.y)
				{
					if (idx > bestIndex)
					{
						bestIndex = idx;
						bestMove = move;
					}
				}
				idx ++;
			}
		}
		
		return bestMove;
	}
	

	
	public IEvent MoveEvent(TactikonState state, int unitId, int toX, int toY)
	{
		EventMoveUnit event = new EventMoveUnit();
		IUnit unit = state.GetUnit(unitId); 
		event.mFrom = unit.GetPosition();
		event.mTo = new Position(toX, toY);
		event.mUnitId = unitId;
		return event;
	}
	
	public IEvent BoardEvent(TactikonState state, int unitId, int transporterUnitId)
	{
		EventBoardUnit event = new EventBoardUnit();
		event.mTransporter = transporterUnitId;
		event.mUnit = unitId;
		return event;
	}
	
	public IEvent AttackEvent(TactikonState state, int unitId, int enemyId)
	{
		EventAttackUnit event = new EventAttackUnit();
		event.attackingUnitId = unitId;
		event.defendingUnitId = enemyId;
		return event;
	}
	
	public abstract Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit, TactikonState state, TaskList taskList, AIBrain brain);
	public abstract Task CheckTask(TactikonState state, Task task, PathFinder pathFinder, AIInfo info, TaskList taskList);
	public abstract void ActionTask(EventInjector injector, TactikonState state, Task task);
}
