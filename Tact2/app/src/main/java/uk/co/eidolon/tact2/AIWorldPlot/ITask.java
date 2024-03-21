package uk.co.eidolon.tact2.AIWorldPlot;

import java.util.ArrayList;

import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitInfantry;
import Tactikon.State.UnitTank;

public abstract class ITask
{
	IUnit mUnit;
	TactikonState mState;
	AIInfo mInfo;
	
	protected ITask(IUnit unit, TactikonState state, AIInfo info)
	{
		mUnit = unit;
		mState = state;
		mInfo = info;
	}
	
	abstract boolean AttemptTask();
	
	abstract boolean IsTaskOver();

	int NumCityDefenders(City city, TactikonState state)
	{
		int num = 0;
		for (Integer fortUnitId : city.fortifiedUnits)
		{
			IUnit unit = state.GetUnit(fortUnitId);
			if (unit instanceof UnitInfantry) num++;
			if (unit instanceof UnitTank) num++;
		}
		return num;
	}
	
	Position FindClosestMove(IUnit unit, TactikonState state, Position target)
	{
		ArrayList<Position> possibleMoves = state.GetPossibleMoves(unit.mUnitId);
		Position bestMove = null;
		int bestDist = 999;
		for (Position move : possibleMoves)
		{
			int dist = Math.abs(move.x - target.x) + Math.abs(move.y - target.y);
			if (dist <= bestDist)
			{
				bestDist = dist;
				bestMove = move;
			}
		}
		
		return bestMove;
	}
	
	
	
	Position FindClosestMove(IUnit unit, WorldPlot plot, TactikonState state)
	{
		ArrayList<Position> possibleMoves = state.GetPossibleMoves(unit.mUnitId);
		
		// try each possible move, choose the one that gets closest
		Position bestMove = null;
		int currentDist = plot.mDistanceMap[unit.GetPosition().x][unit.GetPosition().y];
		int currentBest = Byte.MAX_VALUE;
		
		for (Position move : possibleMoves)
		{
			int newDist = plot.mDistanceMap[move.x][move.y];
			if (newDist < currentDist && newDist < currentBest)
			{
				bestMove = move;
				currentBest = newDist;
			}
		}
		
		
		return bestMove;
	}
	
}
