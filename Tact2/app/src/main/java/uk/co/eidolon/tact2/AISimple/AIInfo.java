package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;

import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;

public class AIInfo
{
	public int[][] massMap;
	public int[][] dangerMap;
	
	int[][] map;
	int mapSize;
	
	public void AddDanger(int[][] dangerMap, int x, int y)
	{
		if (x >= 0 && x < mapSize && y >= 0 && y < mapSize)
		{
			dangerMap[x][y] = 1;
		}
	}
	
	public void AddUnitDanger(int [][] dangerMap, int x, int y, IUnit unit)
	{
		for (int xx = x - unit.GetAttackRange(); xx <= x + unit.GetAttackRange(); ++xx)
		{
			for (int yy = y - unit.GetAttackRange(); yy <= y + unit.GetAttackRange(); ++yy)
			{
				int dist = Math.abs(x - xx) + Math.abs(y - yy);
				if (dist <= unit.GetAttackRange())
				{
					AddDanger(dangerMap, xx, yy);
				}
			}
		}
	}
	
	public AIInfo(TactikonState state, IUnit unit, int[][] massMap)
	{
		map = state.map;
		mapSize = state.mapSize;
		
		this.massMap = massMap;
		
		dangerMap = new int[mapSize][mapSize];
		// fill with a number from 1 to 10 depending on attack power of whatever is there
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit enemy = entry.getValue();
			if (enemy.mUserId == unit.mUserId) continue;
			
			int[][] unitDanger = new int[mapSize][mapSize];
			AddUnitDanger(unitDanger,enemy.GetPosition().x, enemy.GetPosition().y, enemy);
			
			ArrayList<Position> moves = state.GetPossibleMoves(enemy.mUnitId);
			for (Position move : moves)
			{
				AddUnitDanger(unitDanger, move.x, move.y, enemy);
			}
			
			int threat = enemy.GetAttack(unit, state);
			threat = threat / unit.GetDefence(enemy, state);
			if (threat == 0) threat = 1;
			
			for (int x = 0; x < mapSize; ++x)
			{
				for (int y = 0; y < mapSize; ++y)
				{
					dangerMap[x][y] += unitDanger[x][y];
				}
			}
		}
		
		
	}
	
	static void AddTerritory(int x, int y, int tVal, int[][] map, int mapSize)
	{
		if (x >= 0 && y >= 0 && x < mapSize && y < mapSize)
		{
			if (map[x][y] == -1)
				map[x][y] = tVal;
		}
	}
	
	public static int[][] GenerateTerritoryMap(TactikonState state, int playerId)
	{
		int[][] territoryMap = new int[state.mapSize][state.mapSize];
		
		for (int i = 0; i < state.mapSize; ++i)
		{
			for (int j = 0; j < state.mapSize; ++j)
			{
				territoryMap[i][j] = -1;
			}
		}
		
		for (int i = 0; i < state.mapSize; ++i)
		{
			for (City city : state.cities)
			{
				int tVal = 0;
				if (city.playerId == playerId) tVal = 0;
				if (city.playerId == -1) tVal = 1;
				if (city.playerId != -1 && city.playerId != playerId) tVal = 2;
				
				for (int j = 0; j <= i; ++j)
				{
					int inv = i-j;
					AddTerritory(city.x + j, city.y + inv, tVal, territoryMap, state.mapSize);
					AddTerritory(city.x + j, city.y - inv, tVal, territoryMap, state.mapSize);
					AddTerritory(city.x - j, city.y + inv, tVal, territoryMap, state.mapSize);
					AddTerritory(city.x - j, city.y - inv, tVal, territoryMap, state.mapSize);
				}
				
			}
		}
		
		return territoryMap;
	}
}
