package uk.co.eidolon.tact2.AIWorldPlot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;

import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class WorldPlot
{
	ArrayList<Position> mDestinations;
	TactikonState mState;
	IUnit mUnit;
	byte[][] mDistanceMap;
	byte[][] mExtraMap;
	byte[][] mTurnsMap; // TODO - fill this in as we go for each destination with the number of turns for the sqaure
	byte[][] mBlockMap;
	
	int maxTurns = 999;
	
	WorldPlot(TactikonState state, IUnit unit, ArrayList<IUnit> destUnits, AIInfo info, int maxTurns)
	{
		this.maxTurns = maxTurns;
		mBlockMap = info.blockMap;
		mUnit = unit;
		mDestinations = new ArrayList<Position>();
		for (IUnit destUnit : destUnits)
		{
			mDestinations.add(destUnit.GetPosition());
		}
		mState = state;
		mDistanceMap = new byte[mState.mapSize][mState.mapSize];
		mExtraMap = new byte[mState.mapSize][mState.mapSize];
		mTurnsMap = new byte[mState.mapSize][mState.mapSize];
		
		SetupInitialValues();
	}
	
	void SetupInitialValues()
	{
		for (int x = 0; x < mState.mapSize; ++x)
		{
			for (int y = 0; y < mState.mapSize; ++y)
			{
				mDistanceMap[x][y] = Byte.MAX_VALUE;
				mTurnsMap[x][y] = Byte.MAX_VALUE;
			}
		}
		
		
		/*
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit friendlyUnit = entry.getValue();
			if (friendlyUnit.mUserId == mUnit.mUserId)
			{
				mExtraMap[friendlyUnit.GetPosition().x][friendlyUnit.GetPosition().y] = 1;
			}
		}*/
		
		mExtraMap[mUnit.GetPosition().x][mUnit.GetPosition().y] = 1;
		/*if (mUnit.mCarriedBy != -1)
		{
			IUnit carrier = mState.GetUnit(mUnit.mCarriedBy);
			mExtraMap[carrier.GetPosition().x][carrier.GetPosition().y] = 1;
		}*/
	}
	
	WorldPlot(TactikonState state, ArrayList<Position> destinations, IUnit unit, AIInfo info, int maxTurns)
	{
		this.maxTurns = maxTurns;
		mBlockMap = info.blockMap;
		mUnit = unit;
		mDestinations = destinations;
		mState = state;
		mDistanceMap = new byte[mState.mapSize][mState.mapSize];
		mExtraMap = new byte[mState.mapSize][mState.mapSize];
		mTurnsMap = new byte[mState.mapSize][mState.mapSize];
		
		SetupInitialValues();
	}
	
	class PosCost
	{
		PosCost(int x, int y, int cost)
		{
			this.x = x;
			this.y = y;
			this.cost = cost;
		}
		
		int x, y, cost;
	}
	
	void AddMove(Position dest, int turnCost)
	{
		LinkedList<PosCost> exploreList = new LinkedList<PosCost>();
		exploreList.add(new PosCost(dest.x, dest.y, turnCost));
		Position[] moves = new Position[4];
		moves[0] = new Position(0,0);
		moves[1] = new Position(0,0);
		moves[2] = new Position(0,0);
		moves[3] = new Position(0,0);
		Position ss = new Position(0,0);
		while (exploreList.isEmpty() == false)
		{
			PosCost item = exploreList.removeFirst();
			ss.x = item.x; ss.y = item.y;
			turnCost = item.cost;
		
		
		moves[0].x = ss.x; moves[0].y = ss.y + 1;
		moves[1].x = ss.x; moves[1].y = ss.y - 1;
		moves[2].x = ss.x - 1; moves[2].y = ss.y;
		moves[3].x = ss.x + 1; moves[3].y = ss.y;
		//moves[0] = ss.Add(0, 1);
		//moves[1] = ss.Add(0, -1);
		//moves[2] = ss.Add(-1, 0);
		//moves[3] = ss.Add(1, 0);
		
		int cost = -1;
		int newTurnCost = -1;
		int turns = -1;
		
		for (Position move : moves)
		{
			if (move.x < mState.mapSize && move.y < mState.mapSize && move.x >= 0 && move.y >= 0)
			{
				if (mBlockMap[move.x][move.y] == 0 && (mUnit.CanMove(ss, move, mState) || mExtraMap[move.x][move.y] == 1))
				{
					cost = mDistanceMap[ss.x][ss.y] + mUnit.GetMoveCost(ss,  move,  mState);
					turns = mTurnsMap[ss.x][ss.y];
					
					if (mDistanceMap[move.x][move.y] > cost)
					{
						mDistanceMap[move.x][move.y] = (byte)cost;
						newTurnCost = turnCost + mUnit.GetMoveCost(ss,  move,  mState);
						if (newTurnCost > mUnit.GetMovementDistance())
						{
							turns++;
							newTurnCost = 0;
						}
						mTurnsMap[move.x][move.y] = (byte)turns;
						if (turns <= maxTurns)
							exploreList.add(new PosCost(move.x, move.y, newTurnCost));
							
					}
				}
			}
		}
		}
	}
	
	void FillTurnsMap(Position dest)
	{
		mDistanceMap[dest.x][dest.y] = 0;
		mTurnsMap[dest.x][dest.y] = 1;
		AddMove(dest, 0);
	}
	
	void ComputePlot()
	{
		
		for (Position dest : mDestinations)
		{
			// fan out from the destination, filling the number of turns into the turnsmap
			FillTurnsMap(dest);
		}
	}
	
	Position FindDestination(TactikonState state, Position start)
	{
		Position current = new Position(start.x, start.y);
		
		int mapSize = state.mapSize;
	
		boolean bFirstMove = true;
		
		while(true)
		{
			for (Position dest : mDestinations)
			{
				if (current.x == dest.x && current.y == dest.y) return dest; 
			}
			Position up = current.Add(0, 1);
			Position down = current.Add(0, -1);
			Position left = current.Add(-1, 0);
			Position right = current.Add(1, 0);
			ArrayList<Position> moves = new ArrayList<Position>();
			moves.add(up); moves.add(down); moves.add(left); moves.add(right);
			int bestCost = Byte.MAX_VALUE;
			Position bestMove = null;
			for (Position move : moves)
			{
				if (move.x > mapSize - 1) continue;
				if (move.x < 0) continue;
				if (move.y > mapSize - 1) continue;
				if (move.y < 0) continue;
				
				
				if (mUnit.CanMove(current, move, mState) || mExtraMap[current.x][current.y] == 1 || bFirstMove == true)
				{
					if (mDistanceMap[move.x][move.y] < bestCost && mDistanceMap[move.x][move.y] < mDistanceMap[current.x][current.y])
					{
						bestMove = move;
						bestCost = mDistanceMap[move.x][move.y];
					}
				}
			}
			if (bestMove == null) return null;
			current = bestMove;
			bFirstMove = false;
		}
		
		
	}
	
	Position FindClosestMove(IUnit unit, TactikonState state, AIInfo info)
	{
		ArrayList<Position> possibleMoves = state.GetPossibleMoves(unit.mUnitId);
		
		// try each possible move, choose the one that gets closest
		
		// if there are multiple moves, that end up the same closest distance, choose the least dangerous one
		
		
		Position bestMove = null;
		int currentDist = mDistanceMap[unit.GetPosition().x][unit.GetPosition().y];
		int currentBest = currentDist;
		
		for (Position move : possibleMoves)
		{
			int newDist = mDistanceMap[move.x][move.y];
			if (newDist < currentBest)
			{
				bestMove = move;
				currentBest = newDist;
			}
		}
		
		// now select the safest move out of the moves that have equal distance
		if (bestMove == null) return null;
		int distance = mDistanceMap[bestMove.x][bestMove.y];
		int bestDanger = 999;
		for (Position move : possibleMoves)
		{
			if (mDistanceMap[move.x][move.y] == distance)
			{
				if (info.dangerMap[move.x][move.y] < bestDanger)
				{
					bestMove = move;
					bestDanger = info.dangerMap[move.x][move.y];
				}
			}
		}
		
		return bestMove;
	}
	
}
