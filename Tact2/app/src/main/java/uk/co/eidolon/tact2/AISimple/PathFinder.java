package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import uk.co.eidolon.tact2.GameView;

import Tactikon.State.IUnit;
import Tactikon.State.IUnit.Domain;
import Tactikon.State.City;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class PathFinder
{
	int startX, startY;
	
	IUnit mUnit;
	
	TactikonState mState;
	
	int[][] blockMap;
	int[][] massMap;
	int[][] costMap;
	Node[][] nodeMap;
	
	ArrayList<Node> mRoute;
	int mapSize;
	int mCost;
	int maxDist;
	
	// consider swapping this out for a pre-populated map of costs to destinations - will be more optimal for cases were we want to query hundreds of routes
	
	
	public PathFinder(TactikonState state, IUnit unit, AIInfo info)
	{
		mUnit = unit;
		mState = state;
		mapSize = state.mapSize;
		startX = unit.GetPosition().x;
		startY = unit.GetPosition().y;
		
		massMap = info.massMap;
		blockMap = new int[state.mapSize][state.mapSize]; // 0 - move freely, 1 - can't stop, 2 - can't move or stop
		costMap = new int[state.mapSize][state.mapSize];
		
		nodeMap = new Node[mapSize][mapSize];
		for (int x = 0; x < mapSize; ++x)
			for (int y = 0; y < mapSize; ++y)
				nodeMap[x][y] = new Node(x, y);
		
		if (unit.GetDomain() != Domain.Air && unit.mCarriedBy == -1) // non-air units are stuck on their own landmass
		{
			// if we're a boat, and we're in a city, need to add the surrounding watermasses
			ArrayList<Integer> masses = new ArrayList<Integer>();
			int unitx = unit.GetPosition().x;
			int unity = unit.GetPosition().y;
			
			if (unit.GetDomain() == Domain.Water && info.map[unitx][unity] == TactikonState.TileType_Port)
			{
				if (unitx > 0 && info.map[unitx - 1][unity] == TactikonState.TileType_Water) masses.add(massMap[unitx-1][unity]);
				if (unity > 0 && info.map[unitx][unity-1] == TactikonState.TileType_Water) masses.add(massMap[unitx][unity-1]);
				if (unitx < mapSize - 1 && info.map[unitx + 1][unity] == TactikonState.TileType_Water) masses.add(massMap[unitx+1][unity]);
				if (unity < mapSize - 1 && info.map[unitx][unity+1] == TactikonState.TileType_Water) masses.add(massMap[unitx][unity+1]);
								
			} else
			{
				masses.add(massMap[unitx][unity]);
			}
			for (int x = 0; x < state.mapSize; ++x)
			{
				for (int y = 0; y < state.mapSize; ++y)
				{
					if (!masses.contains(massMap[x][y])) blockMap[x][y] = 2;
				}
			}
		}
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit checkUnit = entry.getValue();
			if (checkUnit.mUnitId == unit.mUnitId) continue;
			if (checkUnit.mUserId == unit.mUserId)
			{
				if (Math.abs(checkUnit.GetPosition().x - unit.GetPosition().x) + Math.abs(checkUnit.GetPosition().y - unit.GetPosition().y) < unit.GetMovementDistance())
				{
					blockMap[checkUnit.GetPosition().x][checkUnit.GetPosition().y] = 1;
				}
				
			}
			if (checkUnit.mUserId != unit.mUserId)
			{
				blockMap[checkUnit.GetPosition().x][checkUnit.GetPosition().y] = 2;
			}
		}
		
		for (int x = 0; x < mapSize; ++x)
		{
			for (int y = 0; y < mapSize; ++y)
			{
				if (unit.CanMove(unit.GetPosition(), new Position(x, y), state) == false) blockMap[x][y] = 2;
			}
		}
		
		if (unit.mCarriedBy != -1) blockMap[unit.GetPosition().x][unit.GetPosition().y] = 0;
	}
	
	public class Node implements Comparable<Node>
	{
		public int x;
		public int y;
		int f_cost = 0;
		int g_cost = 0;
		boolean closed = false;
		boolean open = false;
		
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
				nodeMap[x][y].g_cost = 0;
				nodeMap[x][y].f_cost = 0;
				nodeMap[x][y].closed = false;
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
			// ***
			
			ArrayList<Node> route = new ArrayList<Node>();
			Node current = best;
			while (current.x != start.x || current.y != start.y)
			{
				route.add(0, current);
				current = current.parent;
			}
			//GameView.route = route;
			
			if (best.x == end.x && best.y == end.y)
			{
				ArrayList<Node> route2 = new ArrayList<Node>();
				Node current2 = best;
				while (current2.x != start.x || current2.y != start.y)
				{
					route2.add(0, current2);
					current2 = current2.parent;
				}
				mCost = nodeMap[end.x][end.y].g_cost;
				return route2;
				
			}

			nodeMap[best.x][best.y].closed = true;
			
			newNodes.clear();
			if (best.x > 0 && blockMap[best.x-1][best.y] == 0) newNodes.add(nodeMap[best.x-1][best.y]);
			if (best.y > 0 && blockMap[best.x][best.y-1] == 0) newNodes.add(nodeMap[best.x][best.y-1]);
			if (best.x < mapSize -1 && blockMap[best.x+1][best.y] == 0) newNodes.add(nodeMap[best.x+1][best.y]);
			if (best.y < mapSize -1 && blockMap[best.x][best.y+1] == 0) newNodes.add(nodeMap[best.x][best.y+1]);
			
			for (Node node : newNodes)
			{
				if (node.closed == true) continue;
				if (costMap[node.x][node.y] == 0) costMap[node.x][node.y] = mUnit.GetMoveCost(new Position(best.x, best.y), new Position(node.x, node.y), mState);
				int score = best.g_cost + costMap[node.x][node.y];
				boolean inOpenSet = node.open;
				
				if (!inOpenSet || score < node.g_cost)
				{
					node.parent = best;
					node.g_cost = score;
					node.f_cost = score + node.dist(nodeMap[end.x][end.y]);
					if (!inOpenSet) openSet.add(node);
					node.open = true;
				}
			}
		}
		
		
		// we haven't managed to get here, so mark as impossible for future users of this pathfinder
		blockMap[end.x][end.y] = 2;
		return null;
		
	}

	Position mPosition;
	
	boolean Calculate(int targetX, int targetY, int maxDist)
	{
		if (Math.abs(targetX - startX) + Math.abs(targetY - startY) > maxDist) return false; 
		
		if (blockMap[targetX][targetY] > 0) return false;
		
		ArrayList<Node> route = aStar(new Position(startX, startY), new Position(targetX, targetY));
		
		//GameView.route = route;
		
		
		if (route == null) return false;
		
		mRoute = route;
		
		return true;
	}
	
	ArrayList<Position> GetRoute()
	{
		ArrayList<Position> route = new ArrayList<Position>();
		for (Node node : mRoute)
		{
			route.add(new Position(node.x, node.y));
		}
		
		return route;
	}
	
	
	
	int GetRouteCost()
	{
		return mRoute.size();
	}
	
	int GetRouteTurns()
	{
		int dis = mUnit.GetMovementDistance();
		int moveCost = 0;
		int turns = 0;
		for (Node node : mRoute)
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
	
}
