package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import android.util.Log;

import uk.co.eidolon.tact2.GameView;

import Tactikon.State.IUnit;
import Tactikon.State.City;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class TransportPathFinder
{
	int startX, startY;
	
	IUnit mUnit1;
	IUnit mUnit2;
	
	TactikonState mState;
	
	int[][] blockMap1;
	int[][] blockMap2;
	
	int[][] massMap;
	int[][] costMap;
	Node[][] nodeMap;
	
	ArrayList<Node> mTransporterRoute;
	ArrayList<Node> mWholeRoute;
	Position mTransitionPoint;
	int mapSize;
	int mCost;
	
	ArrayList<Integer> endMasses = new ArrayList<Integer>();;
	
	// consider swapping this out for a pre-populated map of costs to destinations - will be more optimal for cases were we want to query hundreds of routes
	
	void FillBlockMap(int[][] blockMap, IUnit unit, AIInfo info)
	{
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit checkUnit = entry.getValue();
			if (checkUnit.mUnitId == unit.mUnitId) continue;
			if (checkUnit.mUserId == unit.mUserId)
			{
				blockMap[checkUnit.GetPosition().x][checkUnit.GetPosition().y] = 1;
			}
			if (checkUnit.mUserId != unit.mUserId)
			{
				blockMap[checkUnit.GetPosition().x][checkUnit.GetPosition().y] = 2;
			}
		}
		
		// unblock friendly cities with less than the number of fortified units
		for (City city : mState.cities)
		{
			if (city.playerId == unit.mUserId && city.fortifiedUnits.size() < mState.MAX_UNITS_IN_CITY)
			{
				blockMap[city.x][city.y] = 0;
			}
		}
		
		for (int x = 0; x < mapSize; ++x)
		{
			for (int y = 0; y < mapSize; ++y)
			{
				if (unit.CanMove(unit.GetPosition(), new Position(x, y), mState) == false) blockMap[x][y] = 2;
			}
		}
	}
	
	public TransportPathFinder(TactikonState state, IUnit transporter, IUnit unit, AIInfo info)
	{
		mUnit1 = transporter;
		mUnit2 = unit;
		mState = state;
		mapSize = state.mapSize;
		startX = transporter.GetPosition().x;
		startY = transporter.GetPosition().y;
		
		massMap = info.massMap;
		blockMap1 = new int[state.mapSize][state.mapSize]; // 0 - move freely, 1 - can't stop, 2 - can't move or stop
		blockMap2 = new int[state.mapSize][state.mapSize]; // 0 - move freely, 1 - can't stop, 2 - can't move or stop
		costMap = new int[state.mapSize][state.mapSize];
		
		nodeMap = new Node[mapSize][mapSize];
		for (int x = 0; x < mapSize; ++x)
			for (int y = 0; y < mapSize; ++y)
				nodeMap[x][y] = new Node(x, y);
		
		FillBlockMap(blockMap1, transporter, info);
		FillBlockMap(blockMap2, unit, info);
		
		// unblock the start and end points of the route
		blockMap1[transporter.GetPosition().x][transporter.GetPosition().y] = 0;
		blockMap2[transporter.GetPosition().x][transporter.GetPosition().y] = 0;
		blockMap1[unit.GetPosition().x][unit.GetPosition().y] = 0;
		blockMap2[unit.GetPosition().x][unit.GetPosition().y] = 0;
	}
	
	public class Node implements Comparable<Node>
	{
		public int x;
		public int y;
		public int f_cost = 0;
		public int g_cost = 0;
		public boolean closed = false;
		boolean open = false;
		boolean transition = false;
		
		int unitNum = 1;
		
		Node parent;
		
		Node(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		
		int dist(Node other)
		{
			return Math.abs(x - other.x) + Math.abs(y - other.y);
		}

		@Override
		public int compareTo(Node otherNode)
		{
			return (f_cost - otherNode.f_cost);
		}
	}
	


	ArrayList<Node> aStar(Position start, Position end)
	{
		ArrayList<Node> newNodes = new ArrayList<Node>();
		
		for (int x = 0; x < mapSize; ++x)
			for (int y = 0; y < mapSize; ++y)
			{
				nodeMap[x][y].g_cost = 99999;
				nodeMap[x][y].f_cost = 0;
				nodeMap[x][y].closed = false;
				nodeMap[x][y].unitNum = 1;
				nodeMap[x][y].transition = false;
				nodeMap[x][y].open = false;
			}
		
		PriorityQueue<Node> openSet = new PriorityQueue<Node>();
		openSet.add(nodeMap[start.x][start.y]);
		nodeMap[start.x][start.y].f_cost = nodeMap[start.x][start.y].dist(nodeMap[end.x][end.y]);
		nodeMap[start.x][start.y].g_cost = 0;
		
		while(!openSet.isEmpty())
		{
			Node best = openSet.remove();
			best.open = false;
			if (best.x == end.x && best.y == end.y)
			{
				ArrayList<Node> route = new ArrayList<Node>();
				Node current = best;
				route.add(0, current);
				while (current.x != start.x || current.y != start.y)
				{
					route.add(0, current);
					current = current.parent;
				}
				mCost = nodeMap[end.x][end.y].g_cost;
				return route;
				
			}

			
			best.closed = true;
			
			newNodes.clear();
			if (best.unitNum == 1)
			{
				if (best.x > 0 && blockMap1[best.x-1][best.y] == 0) newNodes.add(nodeMap[best.x-1][best.y]);
				if (best.y > 0 && blockMap1[best.x][best.y-1] == 0) newNodes.add(nodeMap[best.x][best.y-1]);
				if (best.x < mapSize -1 && blockMap1[best.x+1][best.y] == 0) newNodes.add(nodeMap[best.x+1][best.y]);
				if (best.y < mapSize -1 && blockMap1[best.x][best.y+1] == 0) newNodes.add(nodeMap[best.x][best.y+1]);
				
				// do the transition
				//if (best.x > 0) Log.i("AI", "EndMass: " + massMap[end.x][end.y] + " leftMap: " + massMap[best.x-1][best.y]);
				//if (best.parent != null) // never transition on the first move
				{
				if (best.x > 0 && blockMap1[best.x-1][best.y] == 2 && blockMap2[best.x-1][best.y] == 0 && endMasses.contains(massMap[best.x-1][best.y]))
				{
					newNodes.add(nodeMap[best.x-1][best.y]);
					nodeMap[best.x-1][best.y].transition = true;
				}
				if (best.y > 0 && blockMap1[best.x][best.y-1] == 2 && blockMap2[best.x][best.y-1] == 0 && endMasses.contains(massMap[best.x][best.y-1]))
				{
					newNodes.add(nodeMap[best.x][best.y-1]);
					nodeMap[best.x][best.y-1].transition = true;
				}
				if (best.x < mapSize -1 && blockMap1[best.x+1][best.y] == 2 && blockMap2[best.x+1][best.y] == 0 && endMasses.contains(massMap[best.x+1][best.y]))
				{
					newNodes.add(nodeMap[best.x+1][best.y]);
					nodeMap[best.x+1][best.y].transition = true;
				}
				if (best.y < mapSize -1 && blockMap1[best.x][best.y+1] == 2 && blockMap2[best.x][best.y+1] == 0 && endMasses.contains(massMap[best.x][best.y+1]))
				{
					newNodes.add(nodeMap[best.x][best.y+1]);
					nodeMap[best.x][best.y+1].transition = true;
				}
				}
			} else
			{
				if (best.x > 0 && blockMap2[best.x-1][best.y] == 0) newNodes.add(nodeMap[best.x-1][best.y]);
				if (best.y > 0 && blockMap2[best.x][best.y-1] == 0) newNodes.add(nodeMap[best.x][best.y-1]);
				if (best.x < mapSize -1 && blockMap2[best.x+1][best.y] == 0) newNodes.add(nodeMap[best.x+1][best.y]);
				if (best.y < mapSize -1 && blockMap2[best.x][best.y+1] == 0) newNodes.add(nodeMap[best.x][best.y+1]);
			}
			
			for (Node node : newNodes)
			{
				if (node.closed == true) continue;
				
				node.unitNum = best.unitNum;
				
				if (node.unitNum == 1 && node.transition == false)
				{
					if (costMap[node.x][node.y] == 0) 
						costMap[node.x][node.y] = mUnit1.GetMoveCost(new Position(best.x, best.y), new Position(node.x, node.y), mState);
				} else
				{
					if (costMap[node.x][node.y] == 0)
						costMap[node.x][node.y] = mUnit2.GetMoveCost(new Position(best.x, best.y), new Position(node.x, node.y), mState) * 3;
				}
				int score = best.g_cost + costMap[node.x][node.y];
				boolean inOpenSet = node.open;

				if (!inOpenSet || score < node.g_cost)
				{
					if (node.transition == true)
					{
						node.unitNum = 2;
					} 
					
					node.parent = best;
					node.g_cost = score;
					node.f_cost = score + node.dist(nodeMap[end.x][end.y]);
					if (!inOpenSet) openSet.add(node);
					node.open = true;
				}
				
			}
			
		}
		// we haven't managed to get here, so mark as impossible for future users of this pathfinder
		//blockMap[end.x][end.y] = 2;
		return null;
		
	}

	
	ArrayList<Position> GetTransporterRoute()
	{
		ArrayList<Position> route = new ArrayList<Position>();
		for (Node node : mTransporterRoute)
		{
			route.add(new Position(node.x, node.y));
		}
		
		return route;
	}
	
	boolean Calculate(int targetX, int targetY, int maxDist)
	{
		if (targetX < 0) return false;
		if (targetY < 0) return false;
		if (targetX >= mapSize) return false;
		if (targetY >= mapSize) return false;
		
		if (Math.abs(targetX - startX) + Math.abs(targetY - startY) > maxDist)
		{
			//Log.i("AI", "Rejected too long route");
			return false;
		}
		
		endMasses = new ArrayList<Integer>();
		if (mState.map[targetX][targetY] == TactikonState.TileType_Port)
		{
			if (targetX > 0) endMasses.add(massMap[targetX-1][targetY]);
			if (targetY > 0) endMasses.add(massMap[targetX][targetY-1]);
			if (targetX < mapSize - 1) endMasses.add(massMap[targetX+1][targetY]); 
			if (targetY < mapSize - 1) endMasses.add(massMap[targetX][targetY+1]);
		} else
		{
			endMasses.add(massMap[targetX][targetY]);
		}
		
		//if (blockMap[targetX][targetY] > 0) return false;
		//route2 = nodeMap;
		mWholeRoute = aStar(new Position(startX, startY), new Position(targetX, targetY));
		
		if (mWholeRoute == null) return false;
		
		mTransporterRoute = new ArrayList<Node>();
		
		boolean bHasTransitioned = false;
		boolean bCanStillMove = true;
		for (Node node : mWholeRoute)
		{
			if (node.transition == true)
			{
				bHasTransitioned = true;
				mTransitionPoint = new Position(node.x, node.y);
			}
			
			if (mUnit1.CanMove(new Position(node.x, node.y), new Position(node.x, node.y), mState) == false) bCanStillMove = false;
			
			if (bCanStillMove == true) mTransporterRoute.add(node);
		}
		
		if (bHasTransitioned == false)
		{
			// then need to work out the half-way point (this is useful for helicopters picking up units on their own landmass)
			// move from the end of the route to the mid-point, choose the point that's the appropriate landmass for the unit
			Node bestNode = null;
			for (int i = mWholeRoute.size() - 1; i >= 0; --i)
			{
				Node node = mWholeRoute.get(i);
				if (massMap[node.x][node.y] == massMap[targetX][targetY]) bestNode = node;
			}
			Node node = bestNode;
			if (node == null) return false; 
			mTransitionPoint = new Position(node.x, node.y);
		}
		
		
		
		return true;
	}
	
	Position GetTransitionPoint()
	{
		return mTransitionPoint;
	}
	
	int GetTransporterRouteCost()
	{
		return mTransporterRoute.size();
	}
	
	int GetTransporterRouteTurns()
	{
		int dis = mUnit1.GetMovementDistance();
		int moveCost = 0;
		int turns = 0;
		for (Node node : mTransporterRoute)
		{
			moveCost = moveCost + costMap[node.x][node.y];
			if (moveCost > dis)
			{
				turns++;
				moveCost = costMap[node.x][node.y];
			}

		}
		return turns;
	}
	
	int GetWholeRouteTurns()
	{
		int dis = mUnit1.GetMovementDistance();
		int moveCost = 0;
		int turns = 0;
		for (Node node : mWholeRoute)
		{
			if (node.transition == true)
			{
				dis = mUnit2.GetMovementDistance();
			}
			moveCost = moveCost + costMap[node.x][node.y];
			if (moveCost > dis)
			{
				turns++;
				moveCost = costMap[node.x][node.y];
			}

		}
		return turns;
	}
	
}
