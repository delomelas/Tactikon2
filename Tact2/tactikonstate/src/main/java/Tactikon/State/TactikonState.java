package Tactikon.State;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import Core.IState;
import Core.PlayerInfo;
import Tactikon.State.TactikonNewGameOptions.AILevel;
import Tactikon.State.TactikonNewGameOptions.WinCondition;

public class TactikonState extends IState
{

	int VERSION_FIRST = 0;
	int VERSION_WITH_INFO_BUBBLES = 1;
	int VERSION_WITH_UPDATETIME = 2;
	int VERSION_WITH_MISSEDTURNCOUNT = 3;
	int VERSION_WITH_MAXTURNSECONDS = 4;
	int VERSION_WITH_AI = 5;
	int VERSION_WITH_CITY_PRODUCTION_INDICATOR = 6;
	int VERSION_WITH_POWERGRAPH = 7;
	int VERSION_WITH_GAME_VERSION = 8;
	int VERSION_WITH_GAMESTATE_MESSAGE = 9;
	int VERSION_WITH_FRIENDSONLY = 10;
	int VERSION_WITH_UNITCONFIG = 11;
	int VERSION_WITH_NEW_COMBAT_TWEAKS = 12;
	int VERSION_WITH_NEW_COMBAT_TWEAKS_2 = 13;
	int VERSION_WITH_NEW_COMBAT_TWEAKS_3 = 14;
	int VERSION_WITH_INTERMEDIATE_AI = 15;
	int VERSION_WITH_TUTORIAL_FLAG = 16;
	int VERSION_WITH_COMPRESSED_UNITS= 17;
	int VERSION_WITH_NERFED_TRANSPORTS= 18;
	int VERSION_WITH_TRACKS = 19;
	int VERSION_WITH_FIXEDATTACKS = 20;
	int VERSION_WITH_NEWBUBBLEINFO = 21;
	
	public int mVersion = VERSION_WITH_NEWBUBBLEINFO;
	
	public static int AI_PLAYER_START = Integer.MAX_VALUE - 8;
	public static int GUEST_PLAYER_START = Integer.MAX_VALUE - 16;
	
	IState.GameStatus gameState; 
	// 0 = not started, waiting for players
	// 1 = play turns happening
	// 2 = game over
	
	public String gameStateMessage = "";
	
	static boolean aiEnabled = true;
	
	public int MAX_UNITS_IN_CITY = 4;
	
	// game state
	public int playerToPlay;

	public int maxTurnSeconds = 60*60*24;
	
	// players and units
	public Map<Long, PlayerInfo> playerInfo = new TreeMap<Long, PlayerInfo>();
	public ArrayList<Integer> players = new ArrayList<Integer>();
	public Map<Integer, IUnit> units = new TreeMap<Integer, IUnit>();
	
	public Map<Long, Tracks> tracks = new TreeMap<Long, Tracks>();
	
	// info bubbles
	public ArrayList<IBubbleInfo> bubbles = new ArrayList<IBubbleInfo>();
	public ArrayList<IBubbleInfo2> bubbles2 = new ArrayList<IBubbleInfo2>();
	
	// missed turn count
	public ArrayList<Integer> missedTurnCount = new ArrayList<Integer>();
	
	public Map<Integer, ArrayList<Integer>> powerGraph = new TreeMap<Integer, ArrayList<Integer>>();
	
	static public int TileType_Water = 0;
	static public int TileType_Land = 1;
	static public int TileType_Port = 2;
	static public int TileType_Mountain = 3;
	static public int TileType_Jungle = 4;
	
	// map
	public int mapSize = 0;
	public int map[][];
	public float mapHeight[][]; // normalised heightmap - [0.0 - 1.0]
	public float seaLevel;
	public int mapBalanceScore;
	public ArrayList<City> cities = new ArrayList<City>();
	
	// rules
	public boolean bFogOfWar = false;
	public WinCondition winCondition;
	public boolean bFriendsOnly = false;
	public String createdByAlias = "";
	public int createdById = -1;
	
	public boolean bLocalGame = false;
	
	public int mGameVersion = 0;
	public boolean bTutorial = false;
	
	public AILevel[] aiLevel = new AILevel[7];
	
	public Map<Integer, ArrayList<Position>> possibleMoveCache = new HashMap<Integer, ArrayList<Position>>();
	
	// fog
	class FogMap
	{
		byte[][] seenTiles;
		int mapSize;
		
		FogMap(int size)
		{
			mapSize = size;
			seenTiles = new byte[size][size];
		}
		
		void BinaryToState(DataInputStream stream) throws IOException
		{
			mapSize = stream.readInt();
			for (int x = 0; x < mapSize; ++x)
			{
				stream.readFully(seenTiles[x]);
			}
		}
		void StateToBinary(DataOutputStream stream) throws IOException
		{
			stream.writeInt(mapSize);
			for (int x = 0; x < mapSize; ++x)
			{
				stream.write(seenTiles[x]);
			}
		}
	}
	public FogMap[] fogMaps;
	
	// internal accounting
	private int nextObjectId = 0;
	private int nextRandomId = 0;
	
	private static Position[][] nodeMap = new Position[80][80];
	
	public TactikonState()
	{
		for (int x = 0; x < 80; ++x)
		{
			for (int y = 0; y < 80; ++y)
			{
				nodeMap[x][y] = new Position(x, y);
			}
		}
		
		for (int i = 0; i < 7; ++i)
		{
			aiLevel[i] = AILevel.Beginner;
		}
	}
	
	// fogmap - 0 = can't see at all, 1 = we've seen this tile, 2 = we can currently see this tile
	
	public byte[][] GetFogMap(int playerId)
	{
		if (bFogOfWar == false) return null;
		
		int index = players.indexOf(playerId);
		if (index == -1) return null;
		
		return fogMaps[index].seenTiles;
	}
	
	public byte[][] GetVisibilityMap(int playerId)
	{
		byte[][] visMap = new byte[mapSize][mapSize];
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerId)
			{
				int xmin, xmax, ymin, ymax;
				int dist = unit.GetVisionDistance(unit.GetPosition(), this);
				int xx = unit.GetPosition().x;
				int yy = unit.GetPosition().y;
				xmin = xx - dist;
				xmin = Math.max(0, xmin);
				xmax = xx + dist;
				xmax = Math.min(mapSize - 1, xmax);
				ymin = yy - dist;
				ymin = Math.max(0, ymin);
				ymax = yy + dist;
				ymax = Math.min(mapSize - 1, ymax);
				
				
				for (int x = xmin; x <= xmax; ++x)
				{
					int xdist = Math.abs(x - xx);
					for (int y = ymin; y <=ymax; ++y)
					{
						if (xdist + Math.abs(y - yy) <= dist)
						{
							visMap[x][y] = 1;
						}
					}
				}
			}
		}
		
		for (City city : cities)
		{
			if (city.playerId == playerId)
			{
				int xmin, xmax, ymin, ymax;
				xmin = city.x - 3;
				xmin = Math.max(0, xmin);
				xmax = city.x + 3;
				xmax = Math.min(mapSize - 1, xmax);
				ymin = city.y - 3;
				ymin = Math.max(0, ymin);
				ymax = city.y + 3;
				ymax = Math.min(mapSize - 1, ymax);
				
				
				for (int x = xmin; x <= xmax; ++x)
				{
					int xDist = Math.abs(x - city.x);
					for (int y = ymin; y <=ymax; ++y)
					{
						if (xDist + Math.abs(y - city.y) <= 3)
						{
							visMap[x][y] = 1;
						}
					}
				}
			}
		}
		
		return visMap;
		
	}
	
	public byte[][] GetResolvedFogMap(int playerId)
	{
		byte[][] resolvedMap = new byte[mapSize][mapSize];
		byte[][] baseMap = GetFogMap(playerId);
		if (baseMap == null) return resolvedMap;
		
		if (GetGameState() == GameStatus.GameOver || GetGameState() == GameStatus.TimeOut)
		{
			for(int x = 0; x < mapSize; ++x)
			{
				for (int y = 0; y < mapSize; ++y)
				{
					resolvedMap[x][y] = 2;
				}
			}
			return resolvedMap;
		}
		
		for (int x = 0; x < mapSize; ++x)
		{
			System.arraycopy(baseMap[x], 0, resolvedMap[x], 0, baseMap.length);
		}
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerId)
			{
				int xmin, xmax, ymin, ymax;
				int dist = unit.GetVisionDistance(unit.GetPosition(), this);
				int xx = unit.GetPosition().x;
				int yy = unit.GetPosition().y;
				xmin = xx - dist;
				xmin = Math.max(0, xmin);
				xmax = xx + dist;
				xmax = Math.min(mapSize - 1, xmax);
				ymin = yy - dist;
				ymin = Math.max(0, ymin);
				ymax = yy + dist;
				ymax = Math.min(mapSize - 1, ymax);
				
				
				for (int x = xmin; x <= xmax; ++x)
				{
					int xdist = Math.abs(x - xx);
					for (int y = ymin; y <=ymax; ++y)
					{
						if (xdist + Math.abs(y - yy) <= dist)
						{
							resolvedMap[x][y] = 2;
						}
					}
				}
			}
		}
		
		for (City city : cities)
		{
			if (city.playerId == playerId)
			{
				int xmin, xmax, ymin, ymax;
				xmin = city.x - 3;
				xmin = Math.max(0, xmin);
				xmax = city.x + 3;
				xmax = Math.min(mapSize - 1, xmax);
				ymin = city.y - 3;
				ymin = Math.max(0, ymin);
				ymax = city.y + 3;
				ymax = Math.min(mapSize - 1, ymax);
				
				
				for (int x = xmin; x <= xmax; ++x)
				{
					int xDist = Math.abs(x - city.x);
					for (int y = ymin; y <=ymax; ++y)
					{
						if (xDist + Math.abs(y - city.y) <= 3)
						{
							resolvedMap[x][y] = 2;
						}
					}
				}
			}
		}
		
		return resolvedMap;
	}
	
	public void UpdateFogMap(int unitId)
	{
		IUnit unit = GetUnit(unitId);
		if (unit == null) return;
		
		byte[][] fogMap = GetFogMap(unit.mUserId);
		if (fogMap == null) return;
		int xmin, xmax, ymin, ymax;
		int dist = unit.GetVisionDistance(unit.GetPosition(), this);
		int xx = unit.GetPosition().x;
		int yy = unit.GetPosition().y;
		xmin = xx - dist;
		xmin = Math.max(0, xmin);
		xmax = xx + dist;
		xmax = Math.min(mapSize - 1, xmax);
		ymin = yy - dist;
		ymin = Math.max(0, ymin);
		ymax = yy + dist;
		ymax = Math.min(mapSize - 1, ymax);
		
		
		for (int x = xmin; x <= xmax; ++x)
		{
			for (int y = ymin; y <=ymax; ++y)
			{
				if (Math.abs(x - xx) + Math.abs(y - yy) <= dist)
				{
					fogMap[x][y] = 1;
				}
			}
		}
	}

	
	static public TactikonState CreateBlankState(int numPlayers, int mapSize, boolean bFogOfWar, int maxTurnSeconds)
	{
		TactikonState state = new TactikonState();
		state.players = new ArrayList<Integer>();
		for (int i = 0; i < numPlayers; ++i)
		{
			state.players.add(-1);
		}
		
		state.map = new int[mapSize][mapSize];
		state.mapHeight = new float[mapSize+1][mapSize+1];
		
		state.mapSize = mapSize;
		state.bFogOfWar = bFogOfWar;
		state.maxTurnSeconds = maxTurnSeconds;
		
		if (bFogOfWar == true)
		{
			state.fogMaps = new FogMap[numPlayers];
			for (int i = 0; i < numPlayers; ++i)
			{
				state.fogMaps[i] = state.new FogMap(mapSize);
				state.fogMaps[i].mapSize = mapSize;
				state.fogMaps[i].seenTiles = new byte[mapSize][mapSize];
			}
		}
	
		state.missedTurnCount = new ArrayList<Integer>();
		for (int i = 0; i < numPlayers; ++i)
		{
			state.missedTurnCount.add(0);
		}
		
		return state;
	}
	
	public boolean IsPlayerAlive(long playerId)
	{
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerId) return true;
		}
		
		for (City city : cities)
		{
			if (city.playerId == playerId) return true;
		}
		
		return false;
	}
	
	public boolean PlayerHasCities(int playerId)
	{
		for (City city : cities)
		{
			if (city.playerId == playerId) return true;
		}
		return false;
	}
	
	public boolean PlayerHasHQ(int playerId)
	{
		for (City city : cities)
		{
			if (city.playerId == playerId && city.bIsHQ == true) return true;
		}
		
		return false;
	}
	
	public void WinGameForPlayer(int playerId)
	{
		gameState = GameStatus.GameOver;
	}
	
	public boolean CheckForWinner()
	{
		if (GetGameState() != GameStatus.InGame) return false;
		if (winCondition == WinCondition.Annihilate)
		{
			int numAlive = 0;
			int playerAlive = -1;
			for (Integer playerId : players)
			{
				if (IsPlayerAlive(playerId))
				{
					numAlive ++;
					playerAlive = playerId;
				}
			}
			
			if (numAlive == 1)
			{
				WinGameForPlayer(playerAlive);
				return true;
			}
			
			return false;
		}
		
		if (winCondition == WinCondition.CaptureAllBases)
		{
			// first kill the leftover units of any player that has no bases left
			for (Integer playerId : players)
			{
				if (PlayerHasCities(playerId) == false)
				{
					// kill off any remaining units the player has
					ArrayList<Integer> killList = new ArrayList<Integer>();
					for (Entry<Integer, IUnit> entry : units.entrySet())
					{
						IUnit unit = entry.getValue();
						if (unit.mUserId == playerId)
						{
							killList.add(unit.mUnitId);
						}
					}
					for (Integer unitToKill : killList)
					{
						KillUnit(unitToKill);
					}
					for (City city : cities)
					{
						if (city.playerId == playerId)
						{
							city.playerId = -1;
							city.fortifiedUnits.clear();
							city.bIsHQ = false;
							city.turnsToProduce = -1;
						}
					}
				}
			}
			
			// now see if there's only one player left
			int numAlive = 0;
			int playerAlive = -1;
			for (Integer playerId : players)
			{
				if (IsPlayerAlive(playerId))
				{
					numAlive ++;
					playerAlive = playerId;
				}
			}
			
			if (numAlive == 1)
			{
				WinGameForPlayer(playerAlive);
				return true;
			}
		}
		
		if (winCondition == WinCondition.CaptureHQ)
		{
			// first kill the leftover units of any player that has no HQ left
			for (Integer playerId : players)
			{
				if (PlayerHasHQ(playerId) == false)
				{
					// kill off any remaining units the player has
					ArrayList<Integer> killList = new ArrayList<Integer>();
					for (Entry<Integer, IUnit> entry : units.entrySet())
					{
						IUnit unit = entry.getValue();
						if (unit.mUserId == playerId)
						{
							killList.add(unit.mUnitId);
						}
					}
					for (Integer unitToKill : killList)
					{
						KillUnit(unitToKill);
					}
					for (City city : cities)
					{
						if (city.playerId == playerId)
						{
							city.playerId = -1;
							city.fortifiedUnits.clear();
							city.bIsHQ = false;
							city.turnsToProduce = -1;
						}
					}
				}
			}
			int numAlive = 0;
			int playerAlive = -1;
			for (Integer playerId : players)
			{
				if (IsPlayerAlive(playerId))
				{
					numAlive ++;
					playerAlive = playerId;
				}
			}
			
			if (numAlive == 1)
			{
				WinGameForPlayer(playerAlive);
				return true;
			}
		}
		
		return false;
	}
	
	public int GetWinner()
	{
		if (gameState == GameStatus.TimeOut) return -1; 
		int numAlive = 0;
		int playerAlive = -1;
		for (Integer playerId : players)
		{
			if (IsPlayerAlive(playerId))
			{
				numAlive ++;
				playerAlive = playerId;
			}
		}
		if (numAlive == 1)
		{
			return playerAlive;
		}
		return -1;
	}
	
	public void SetRandomSeed(int value)
	{
		nextRandomId = value;
	}
	
	public void KillUnit(int unitId)
	{
		IUnit unit = GetUnit(unitId);
		if (unit == null) return;
		
		// remove from fortifications
		if (unit.mFortified)
		{
			for (City city : cities)
			{
				if (city.fortifiedUnits.contains(unitId))
				{
					int index = city.fortifiedUnits.indexOf(unitId);
					city.fortifiedUnits.remove(index);
				}
			}
		}
		
		// kill any units we're carrying
		for (Integer carryId : unit.mCarrying)
		{
			KillUnit(carryId);
		}
		
		units.remove(unitId);
	}
	
	public int GetRandomNumber(int max)
	{
		Random rand = new Random(nextRandomId);
		int randomNumber = rand.nextInt(max);
		nextRandomId++;
		return randomNumber;
	}
	
	public ArrayList<IUnit> GetUnitTypes()
	{
		ArrayList<IUnit> units = new ArrayList<IUnit>();
		units.add(new UnitInfantry());
		units.add(new UnitTank());
		units.add(new UnitFighter());
		units.add(new UnitBomber());
		units.add(new UnitHelicopter());
		units.add(new UnitBoatTransport());
		units.add(new UnitSub());
		units.add(new UnitCarrier());
		units.add(new UnitBattleship());
		
		return units;
	}
	
	public ArrayList<IUnit> GetProductionUnits(int cityId)
	{
		ArrayList<IUnit> units = new ArrayList<IUnit>();
		City city = GetCity(cityId);
		units.add(new UnitInfantry());
		units.add(new UnitTank());
		units.add(new UnitFighter());
		units.add(new UnitBomber());
		units.add(new UnitHelicopter());
		
		if (city.isPort == true)
		{
			units.add(new UnitBoatTransport());
			units.add(new UnitSub());
			units.add(new UnitCarrier());
			units.add(new UnitBattleship());
		}
		
		return units;
	}
	
	public int GetNextObjectId()
	{
		int ret = nextObjectId;
		nextObjectId ++;
		return ret;
	}
	
	@Override
	public ArrayList<Integer> GetPlayers()
	{
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (int i = 0; i < players.size(); ++i)
		{
			if (players.get(i) != -1) ret.add(players.get(i));
		}
		
		return ret;
	}
	
	@Override
	public PlayerInfo GetPlayerInfo(long UserId)
	{
		if (playerInfo.containsKey(UserId))
		{
			return playerInfo.get(UserId);
		}
		
		return null;
	}
	
	public IUnit GetUnit(int unitId)
	{
		if (units.containsKey(unitId) == false) return null;
		return (units.get(unitId));
	}
	
	public City GetCity(int cityId)
	{
		for (City city : cities)
		{
			if (city.cityId == cityId) return city;
		}
		return null;
	}
	
	// inserts an existing unit without creating a new object
	public void InsertUnit(IUnit unit)
	{
		units.put(unit.mUnitId, unit);
	}
	
	// creates and adds a unit that wasn't pre-existing
	public int CreateUnit(IUnit unit)
	{
		unit.mUnitId = GetNextObjectId();
		units.put(unit.mUnitId, unit);
		
		unit.fuelRemaining = unit.GetMaxFuel();
		
		UpdateFogMap(unit.mUnitId);
		
		return unit.mUnitId;
	}
	
	/*
	boolean IsOnMap(Position pos)
	{
		if (pos.x < mapSize && pos.x >= 0 && pos.y >= 0 && pos.y < mapSize) return true;
		return false;
	}*/
	
	void AddMoves(byte[][] scratchMap, Position fromPos, IUnit unit)
	{
		byte movesRemaining = scratchMap[fromPos.x][fromPos.y];
		
		if (movesRemaining <= 0) return;
		
		byte cost;
		
		Position toPos;
				
		LinkedList<Position> openNodes = new LinkedList<Position>();
		
		openNodes.add(fromPos);
		
		while (openNodes.isEmpty() == false)
		{
			fromPos = openNodes.pop();
			int remaining = scratchMap[fromPos.x][fromPos.y];
			
			if (fromPos.x < mapSize - 1)
			{
				toPos = nodeMap[fromPos.x +1][fromPos.y];
				if (unit.CanMove(fromPos, toPos, this) && scratchMap[toPos.x][toPos.y] < remaining)
				{
					cost = (byte)unit.GetMoveCost(fromPos, toPos, this);
					if (remaining - cost > 0)
					{
						scratchMap[toPos.x][toPos.y] = (byte) (remaining - cost);
						openNodes.add(toPos);
					}
				}
			}
			
			if (fromPos.x > 0)
			{
				toPos = nodeMap[fromPos.x -1][fromPos.y];
				if (unit.CanMove(fromPos, toPos, this) && scratchMap[toPos.x][toPos.y] < remaining)
				{
					
					cost = (byte)unit.GetMoveCost(fromPos, toPos, this);
					if (remaining - cost > 0)
					{
					
					scratchMap[toPos.x][toPos.y] = (byte) (remaining - cost);
					openNodes.add(toPos);
					}
				}
			}
			
			if (fromPos.y < mapSize - 1)
			{
				toPos = nodeMap[fromPos.x][fromPos.y+1];
				if (unit.CanMove(fromPos, toPos, this) && scratchMap[toPos.x][toPos.y] < remaining)
				{
					cost = (byte)unit.GetMoveCost(fromPos, toPos, this);
					if (remaining - cost > 0)
					{
					
					scratchMap[toPos.x][toPos.y] = (byte) (remaining - cost);
					openNodes.add(toPos);
					}
				}
			}
			
			if (fromPos.y > 0)
			{
				toPos = nodeMap[fromPos.x][fromPos.y-1];
				if (unit.CanMove(fromPos, toPos, this) && scratchMap[toPos.x][toPos.y] < remaining)
				{
					cost = (byte)unit.GetMoveCost(fromPos, toPos, this);
					if (remaining - cost > 0)
					{
					
					scratchMap[toPos.x][toPos.y] = (byte) (remaining - cost);
					openNodes.add(toPos);
					}
				}
			}
			
			
		}
		
		
		
	}
	
	boolean PlayerIsInGame(int PlayerId)
	{
		for (Integer i : players)
		{
			if (i == PlayerId) return true;
		}
		return false;
	}
	
	boolean PlayerIsAlive(int PlayerId)
	{
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			if (entry.getValue().mUserId == PlayerId) return true;
		}
		
		for (City city : cities)
		{
			if (city.playerId == PlayerId) return true;
		}
		
		return false;
	}
	
	int GetNumJoinedPlayers()
	{
		return players.size();
	}
	
	private byte[][] GetTraversalMap(int unitId)
	{
		IUnit unit = GetUnit(unitId);
		
		
		byte[][] scratchMap = new byte[mapSize][mapSize];

		int maxDist = unit.GetMovementDistance() + 1;
		
		if (unit.GetMaxFuel() != -1)
			if ((unit.fuelRemaining + 1) <= maxDist) maxDist = (unit.fuelRemaining + 1);
		
		scratchMap[unit.GetPosition().x][unit.GetPosition().y] = (byte)(maxDist);
		
		// add enemy units to the scratch map to indicate that we can never cross them at all
		for (Entry <Integer, IUnit> entry : units.entrySet())
		{
			IUnit enemyUnit = entry.getValue();
			if (enemyUnit.mUserId == unit.mUserId) continue;
			
			scratchMap[enemyUnit.GetPosition().x][enemyUnit.GetPosition().y] = 127;
		}
		// add enemy cities to the scratch map to indicate that we can't cross them (unless we can capture)
		for (City city : cities)
		{
			// friendly and unowned cities don't block traversal
			if (city.playerId == unit.mUserId || city.playerId == -1) continue;
			
			if (unit.CanCaptureCity() == true && city.fortifiedUnits.size() == 0) continue;
			
			scratchMap[city.x][city.y] = 127;
		}
		
		// recurse to retrieve all possible moves
		AddMoves(scratchMap, nodeMap[unit.GetPosition().x][unit.GetPosition().y], unit);
		
		return scratchMap;
	}
	
	public ArrayList<Position> GetRoute(int unitId, Position target)
	{
		IUnit unit = GetUnit(unitId);
		ArrayList<Position> route = new ArrayList<Position>();
		route.add(target);
		// confirm that the route is possible...
		// go backwards on the scratchmap to find the origin, taking the lowest numbered neighbour each time
		
		byte[][] scratchMap = GetTraversalMap(unitId);
		
		Position pos = new Position(target.x, target.y);
		while (pos.x != unit.GetPosition().x || pos.y != unit.GetPosition().y)
		{
			// find the lowest numbered neighbour in the scratchmap
			int val = scratchMap[pos.x][pos.y];
			int tryVal;
			Position bestPos = null;
			//int lowest = 999;
			Position tryPos = pos.Add(1, 0);
			if (tryPos.x < mapSize)
			{
				tryVal = scratchMap[tryPos.x][tryPos.y];
				if (tryVal > val && tryVal < 127)
				{
					val = tryVal;
					bestPos = tryPos;
				}
			}
			
			tryPos = pos.Add(-1, 0);
			if (tryPos.x >= 0)
			{
				tryVal = scratchMap[tryPos.x][tryPos.y];
				if (tryVal > val && tryVal < 127)
				{
					val = tryVal;
					bestPos = tryPos;
				}
			}
			
			tryPos = pos.Add(0, 1);
			if (tryPos.y < mapSize)
			{
				tryVal = scratchMap[tryPos.x][tryPos.y];
				if (tryVal > val && tryVal < 127)
				{
					val = tryVal;
					bestPos = tryPos;
				}
			}
			
			tryPos = pos.Add(0, -1);
			if (tryPos.y >= 0)
			{
				tryVal = scratchMap[tryPos.x][tryPos.y];
				if (tryVal > val && tryVal < 127)
				{
					val = tryVal;
					bestPos = tryPos;
				}
			}
			
			
			
			if (bestPos == null) return null; // failed to find a viable route
			
			route.add(0,bestPos);
			
			pos = bestPos;
		}
		
		return route;
	}
	
	public ArrayList<Position> GetFuelRangeMoves(int unitId)
	{
		IUnit unit = GetUnit(unitId);
		if (unit.GetMaxFuel() == -1) return null;
		
		return GetFuelRangeMoves(unitId, unit.GetPosition());
	}
	
	public ArrayList<Position> GetFuelRangeMoves(int unitId, Position pos)
	{
		IUnit unit = GetUnit(unitId);
		if (unit.GetMaxFuel() == -1) return null;
		
		int addDist = Math.abs(unit.GetPosition().x - pos.x) + Math.abs(unit.GetPosition().y - pos.y);
		
		int fuel = unit.fuelRemaining;
		fuel = fuel - addDist;
		
		ArrayList<Position> fuelRange = new ArrayList<Position>();
		
		for (int x = 0; x < mapSize; ++x)
		{
			for (int y = 0; y < mapSize; ++y)
			{
				int dist = Math.abs(x - pos.x) + Math.abs(y - pos.y);
				if (dist == fuel)
				{
					fuelRange.add(new Position(x, y));
				}
			}
		}
		ArrayList<Position> moves = GetPossibleMoves(unitId);
		//fuelRange.removeAll(moves);
		
		return fuelRange;
	}
	
	
	public ArrayList<Position> GetDistanceMoves(int unitId, Position pos)
	{
		IUnit unit = GetUnit(unitId);
		
		ArrayList<Position> moves = new ArrayList<Position>();
		
		for (int x = 0; x < mapSize; ++x)
		{
			for (int y = 0; y < mapSize; ++y)
			{
				int dist = Math.abs(x - pos.x) + Math.abs(y - pos.y);
				if (dist <= unit.GetMovementDistance() && unit.CanMove(unit.GetPosition(), new Position(x, y), this))
				{
					moves.add(new Position(x, y));
				}
			}
		}
		
		return moves;
	}
	
	public ArrayList<Position> GetPotentialMoves(int unitId)
	{
		synchronized(possibleMoveCache)
		{
			if (possibleMoveCache.containsKey(unitId))
			{
				return possibleMoveCache.get(unitId);
			}
		}
		
		ArrayList<Position> moves = new ArrayList<Position>();
		
		IUnit unit = GetUnit(unitId);
		if (unit == null) return moves; // not a unit
		
		//if (unit.mUserId != this.playerToPlay) return moves; // not this players turn
				
		byte[][] scratchMap = GetTraversalMap(unitId);
		
		for (int x = 0; x < mapSize; ++x)
		{
			for (int y = 0; y < mapSize; ++y)
			{
				if (scratchMap[x][y] > 0)
				{
					moves.add(new Position(x, y));
				}
			}
		}
		
		
		IUnit[] unitArray = units.values().toArray(new IUnit[0]);
		
		// remove blocking things from the route
		ArrayList<Position> validMoves = new ArrayList<Position>();
		for (Position pos : moves)
		{
			// can't move to the same square, ever
			if (pos.x == unit.GetPosition().x && pos.y == unit.GetPosition().y) continue;
			
			boolean unitBlocking = false;
			boolean cityBlocking = false;
			
			// are we trying to stop on top of a unit?
			for (IUnit otherUnit : unitArray)
			{
				//IUnit otherUnit = entry.getValue();
				
				if (pos.x == otherUnit.GetPosition().x &&
					pos.y == otherUnit.GetPosition().y &&
					otherUnit.mFortified == false)
				{
					unitBlocking = true;
					break;
				}
			}
			
			// are we trying to stop on a city?
			for (City city : cities)
			{
				if (city.x == pos.x && city.y == pos.y)
				{
					// unowned cities
					if (city.playerId == -1 && unit.CanCaptureCity() == false) 
					{
						cityBlocking = true;
						break;
					}
					
					// player cities
					if (city.playerId == unit.mUserId)
					{
						if (city.fortifiedUnits.size() < MAX_UNITS_IN_CITY) continue;
						cityBlocking = true;
						break;
					}
						
					// enemy cities
					if (city.playerId != unit.mUserId)
					{
						if (unit.CanCaptureCity() == false || city.fortifiedUnits.size() != 0)
						{
							cityBlocking = true;
							break;
						}
					}
				}
			}
			
			if (unitBlocking == false && cityBlocking == false)
				validMoves.add(pos);
			
		}
		
		// can we board a unit??
		for (IUnit otherUnit : unitArray)
		{
			if (otherUnit.mFortified == false &&
				otherUnit.CanCarry(unit) &&
				unit.mCarriedBy != otherUnit.mUnitId && // don't attempt to re-board the unit we're already on
				otherUnit.mUserId == unit.mUserId && 
				otherUnit.mCarrying.size() < otherUnit.CarryCapacity())
			{
				// potential to board this unit, now check if it's close enough...
				int x = otherUnit.GetPosition().x;
				int y = otherUnit.GetPosition().y;
				if ((x < mapSize - 1 && scratchMap[x + 1][y] > 1 && scratchMap[x + 1][y] != 127) ||
					(x > 0 && scratchMap[x - 1][y] > 1 && scratchMap[x - 1][y] != 127) ||
					(y < mapSize - 1 && scratchMap[x][y+1] > 1 && scratchMap[x][y+1] != 127) ||
					(y >0 && scratchMap[x][y-1] > 1 && scratchMap[x][y-1] != 127))
						{
							Position boardPos = new Position(x, y);
							validMoves.add(boardPos);
						}
			}
		}
		
		synchronized(possibleMoveCache)
		{
			possibleMoveCache.put(unitId, validMoves);
		}

		return validMoves;
	}
	
	public ArrayList<Position> GetPossibleMoves(int unitId)
	{
		ArrayList<Position> moves = new ArrayList<Position>();
		
		IUnit unit = GetUnit(unitId);
		
		if (unit != null && unit.bMoved == false)
		{
			moves = GetPotentialMoves(unitId);
		}
		
		return moves;
	}
	
	public ArrayList<Integer> GetPossibleAttacks(int unitId)
	{
		if (mVersion < VERSION_WITH_FIXEDATTACKS)
		{
		IUnit attacker = GetUnit(unitId);
		if (attacker == null) return new ArrayList<Integer>();
		
		int range = attacker.GetAttackRange();
		ArrayList<Integer> attacks = new ArrayList<Integer>();
		
		if (attacker.bAttacked == true) return attacks;
		
		if (attacker.mCarriedBy != -1) return attacks;
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit defender = entry.getValue();
			if (defender.mUserId == attacker.mUserId) continue;
			if (defender.mCarriedBy != -1) continue;
			
			if (Math.abs(defender.GetPosition().x - attacker.GetPosition().x) +
				Math.abs(defender.GetPosition().y - attacker.GetPosition().y) <= range)
			{
				// if there's already an attack at this position, only add it if the probability is better
				int removeId = -1;
				for (Integer defId : attacks)
				{
					IUnit otherDefender = GetUnit(defId);
					if (otherDefender.GetPosition().x != defender.GetPosition().x || otherDefender.GetPosition().y != defender.GetPosition().y) continue;
					
					Battle firstAttack = new Battle(this, unitId, defender.mUnitId);
					Battle otherAttack = new Battle(this, unitId, otherDefender.mUnitId);
					
					int firstDefDamage = (firstAttack.out.get(0).unitId1 == defender.mUnitId) ? firstAttack.out.get(0).unitId1Damage : firstAttack.out.get(0).unitId2Damage;
					int otherDefDamage = (otherAttack.out.get(0).unitId1 == defender.mUnitId) ? otherAttack.out.get(0).unitId1Damage : otherAttack.out.get(0).unitId2Damage;
					
					int firstAttDamage = (firstAttack.out.get(0).unitId1 == unitId) ? firstAttack.out.get(0).unitId1Damage : firstAttack.out.get(0).unitId2Damage;
					int otherAttDamage = (otherAttack.out.get(0).unitId1 == unitId) ? otherAttack.out.get(0).unitId1Damage : otherAttack.out.get(0).unitId2Damage;
					
					if (firstDefDamage > otherDefDamage) removeId = defId;
					if (firstDefDamage == otherDefDamage && firstAttDamage < otherAttDamage) removeId = defId;
					
				}
				if (removeId != -1)
				{
					int index = attacks.indexOf(removeId);
					attacks.remove(index);
				}
				attacks.add(defender.mUnitId);
			}			
		}
		
		return attacks;
		}
		
		IUnit attacker = GetUnit(unitId);
		if (attacker == null) return new ArrayList<Integer>();
		
		int range = attacker.GetAttackRange();
		ArrayList<Integer> attacks = new ArrayList<Integer>();
		
		if (attacker.bAttacked == true) return attacks;
		
		if (attacker.mCarriedBy != -1) return attacks;
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit defender = entry.getValue();
			if (defender.mUserId == attacker.mUserId) continue;
			if (defender.mCarriedBy != -1) continue;
			
			if (Math.abs(defender.GetPosition().x - attacker.GetPosition().x) +
				Math.abs(defender.GetPosition().y - attacker.GetPosition().y) <= range)
			{
				// if there's already an attack at this position, only add it if the probability is better
				int removeId = -1;
				for (Integer defId : attacks)
				{
					IUnit otherDefender = GetUnit(defId);
					if (otherDefender.GetPosition().x != defender.GetPosition().x || otherDefender.GetPosition().y != defender.GetPosition().y) continue;
					
					Battle firstAttack = new Battle(this, unitId, defender.mUnitId);
					Battle otherAttack = new Battle(this, unitId, defId);
					float tprob = 0;
					for (Battle.Outcome outcome : firstAttack.out)
					{
						if (outcome.unitId1 == defender.mUnitId)
						{
							if (outcome.unitId1Damage > 0) tprob = tprob + outcome.probability;
						} else
						{
							if (outcome.unitId2Damage > 0) tprob = tprob + outcome.probability;
						}
					}
					float otprob = 0;
					for (Battle.Outcome outcome : otherAttack.out)
					{
						if (outcome.unitId1 == defId)
						{
							if (outcome.unitId1Damage > 0) otprob = otprob + outcome.probability;
						} else
						{
							if (outcome.unitId2Damage > 0) otprob = otprob + outcome.probability;
						}
					}
					
					if (tprob > otprob)
					{
						removeId = defId;
					}
					
				}
				if (removeId != -1)
				{
					int index = attacks.indexOf(removeId);
					attacks.remove(index);
				}
				attacks.add(defender.mUnitId);
			}			
		}
		
		return attacks;
	
	}

	@Override
	public void StateToBinary(DataOutputStream stream) throws IOException
	{
		DataOutputStream out = stream;
		out.writeInt(mVersion);
		out.writeInt(gameState.ordinal());
		out.writeInt(playerToPlay);
		out.writeInt(winningPlayer);
		out.writeInt(nextObjectId);
		out.writeInt(nextRandomId);
		
		out.writeInt(players.size());
		for (int i = 0; i < players.size(); ++i)
		{
			out.writeInt(players.get(i));
		}
		for (int i = 0; i < players.size(); ++i)
		{
			PlayerInfo info = GetPlayerInfo(players.get(i));
			if (info != null)
			{
				out.writeUTF(info.name);
				out.writeUTF(info.logo);
				out.writeInt(info.r);
				out.writeInt(info.g);
				out.writeInt(info.b);
			}
		}
		
		out.writeInt(sequence);
		out.writeInt(mapSize);
		out.writeFloat(seaLevel);
		out.writeInt(mapBalanceScore);
		out.writeInt(winCondition.ordinal());
		
		int tileLength = 4 * (mapSize);
		int heightLength = 4 * (mapSize + 1);
		ByteBuffer byteBuf = ByteBuffer.allocate(heightLength);
		IntBuffer intBuffer = byteBuf.asIntBuffer();
		
		for (int x = 0; x < mapSize; ++x)
		{
			byteBuf.position(0);
			intBuffer.position(0);
			intBuffer.put(map[x],0,tileLength/4);
			out.write(byteBuf.array(), 0, tileLength);
		}
		
		FloatBuffer floatBuffer = byteBuf.asFloatBuffer();
		
		for (int x = 0; x < mapSize+1; ++x)
		{
			byteBuf.position(0);
			floatBuffer.position(0);
			
			floatBuffer.put(mapHeight[x], 0, heightLength/4);
			out.write(byteBuf.array(), 0, heightLength);
		}

		
		out.writeInt(cities.size());
		for (City city: cities)
		{
			out.writeInt(city.cityId);
			out.writeInt(city.x);
			out.writeInt(city.y);
			out.writeInt(city.playerId);
			out.writeBoolean(city.startingCity);
			out.writeUTF(city.productionType);
			out.writeInt(city.turnsToProduce);
			out.writeBoolean(city.bIsHQ);
			out.writeInt(city.fortifiedUnits.size());
			for (int i = 0; i < city.fortifiedUnits.size(); ++i)
			{
				out.writeInt(city.fortifiedUnits.get(i));
			}
			out.writeBoolean(city.isPort);
		}
		
		out.writeInt(units.size());
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit unit = entry.getValue();
			
			if (mGameVersion >= VERSION_WITH_COMPRESSED_UNITS)
			{
				unit.UnitToBinary(out, true);
			} else
			{
				unit.UnitToBinary(out, false);
			}
		}
		
		out.writeBoolean(bFogOfWar);
		
		if (bFogOfWar == true)
		{
			for (int player = 0; player < players.size(); ++player)
			{
				fogMaps[player].StateToBinary(out);
			}
		}
		
		// VERSION WITH INFO BUBBLES
		out.writeInt(bubbles.size());
		for (IBubbleInfo bubble : bubbles)
		{
			bubble.InfoToBinary(out);
		}
		
		// VERSION WITH UPDATETIME
		out.writeLong(lastUpdateTime);
		
		// VERSION WITH MISSED TURN COUNT
		for (int i = 0; i < players.size(); ++i)
		{
			out.writeInt(missedTurnCount.get(i));
		}
		
		// VERSION WITH MAX_TURN_SECONDS
		
		out.writeInt(maxTurnSeconds);
		
		// VERSION_WITH_AI
		
		out.writeBoolean(bLocalGame);
		
		// VERSION_WITH_CITY_PRODUCTION_INDICATOR
		
		for (City city : cities)
		{
			out.writeBoolean(city.bHasProduced);
		}
		
		// VERSION_WITH_POWERGRAPH
		out.writeInt(powerGraph.size());
		for (Entry<Integer, ArrayList<Integer>> entry : powerGraph.entrySet())
		{
			out.writeInt(entry.getKey());
			ArrayList<Integer> values = entry.getValue();
			out.writeInt(values.size());
			for (Integer value : values)
			{
				out.writeInt(value);
			}
		}
		
		out.writeInt(mGameVersion);
		
		// VERSION_WITH_GAMESTATE_MESSAGE
		
		out.writeUTF(gameStateMessage);
	
		// VERSION WITH_FRIENDSONLY
		out.writeBoolean(bFriendsOnly);
		out.writeUTF(createdByAlias);
		out.writeInt(createdById);
		
		// VERSION_WITH_UNITCONFIG
		
		for (Entry<Integer, IUnit> entry : units.entrySet())
		{
			IUnit unit = entry.getValue();
			out.writeBoolean(unit.bChangedConfig);
			out.writeInt(unit.mConfig);
		}
		
		// VERSION WITH INTERMEDIATE AI
		
		for (int i = 0; i < 7; ++i)
		{
			out.writeInt(aiLevel[i].ordinal());
			//aiLevel[i] = AILevel.values()[in.readInt()];
		}
		
		// VERSION WITH TUTORIAL FLAG
		out.writeBoolean(bTutorial);
		
		// VERSION WITH TRACKS
		out.writeInt(tracks.size());
		for (Entry<Long, Tracks> entry : tracks.entrySet())
		{
			Tracks tracks = entry.getValue();
			Long unitId = entry.getKey();
			out.writeInt(unitId.intValue());
			out.writeByte(tracks.numPoints);
			out.write(tracks.x, 0, tracks.numPoints);
			out.write(tracks.y, 0, tracks.numPoints);
			out.writeByte(tracks.type);
			out.writeInt(tracks.playerId);
		}

		// VERSION WITH NEW BUBBLES

		out.writeInt(bubbles2.size());
		for (IBubbleInfo2 bubble : bubbles2)
		{
			bubble.InfoToBinary(out);
		}
	
	}

	@Override
	public void BinaryToState(DataInputStream stream) throws IOException
	{
		BinaryToStateWithOld(null, stream);
	}

	public void BinaryToStateWithOld(TactikonState oldState, DataInputStream stream) throws IOException
	{
		// TODO Auto-generated method stub
		DataInputStream in = stream;
		int version = in.readInt();
		if (version >= VERSION_FIRST)
		{
			gameState = IState.GameStatus.values()[in.readInt()];
			playerToPlay = in.readInt();
			winningPlayer = in.readInt();
			
			nextObjectId = in.readInt();
			nextRandomId = in.readInt();
			
			int numPlayers = in.readInt();
			players = new ArrayList<Integer>();
			for (int i = 0; i < numPlayers; ++i)
			{
				players.add(in.readInt());
				missedTurnCount.add(0);
			}
			
			for (int i =0 ; i < numPlayers; ++i)
			{
				if (players.get(i) != -1)
				{
					PlayerInfo info = new PlayerInfo();
					info.name = in.readUTF();
					
					if (info.name.contains("@") == true)
					{
						int index = info.name.indexOf("@");
						info.name = info.name.substring(0, index);
					}
					
					info.logo = in.readUTF();
					info.r = in.readInt();
					info.g = in.readInt();
					info.b = in.readInt();
					info.userId = players.get(i);
					playerInfo.put(info.userId, info);
				}
			}
			
			sequence = in.readInt();
			mapSize = in.readInt();
			seaLevel = in.readFloat();
			mapBalanceScore = in.readInt();
			winCondition = WinCondition.values()[in.readInt()];
			
			int heightRowLength = 4 * (mapSize + 1);
			int tileRowLength = 4 * (mapSize );
			
			/*
			if (oldState != null && oldState.map != null)
			{
				map = oldState.map;
				mapHeight = oldState.mapHeight;
				in.skip(tileRowLength * mapSize);
				in.skip(heightRowLength * (mapSize + 1));
			} else*/
			{
				map = new int[mapSize][mapSize];
				mapHeight = new float[mapSize+1][mapSize+1];
				
				ByteBuffer byteBuf = ByteBuffer.allocate(heightRowLength);
				IntBuffer intBuffer = byteBuf.asIntBuffer();
				
				for (int x = 0; x < mapSize; ++x)
				{
					byteBuf.position(0);
					in.readFully(byteBuf.array(), 0, tileRowLength);
					intBuffer.position(0);
					
					intBuffer.get(map[x], 0, tileRowLength/4);
				}
				
				FloatBuffer floatBuffer = byteBuf.asFloatBuffer();
			
				for (int x = 0; x < mapSize+1; ++x)
				{
					byteBuf.position(0);
					in.readFully(byteBuf.array(), 0, heightRowLength);
					floatBuffer.position(0);
					
					floatBuffer.get(mapHeight[x], 0, heightRowLength/4);
				
				}
			}
			
			int numCities = in.readInt();
			for (int i = 0; i < numCities; ++i)
			{
				City city = new City();
				city.cityId = in.readInt();
				city.x = in.readInt();
				city.y = in.readInt();
				city.playerId = in.readInt();
				city.startingCity = in.readBoolean();
				city.productionType = in.readUTF();
				city.turnsToProduce = in.readInt();
				city.bIsHQ = in.readBoolean();
				
				city.fortifiedUnits = new ArrayList<Integer>();
				int numFortified = in.readInt();
				for (int f = 0; f < numFortified; ++f)
				{
					int unitId = in.readInt();
					
					city.fortifiedUnits.add(unitId);
				}
				city.isPort = in.readBoolean();
				
				cities.add(city);
				
			}
			
			int numUnits = in.readInt();
			
			for (int i = 0; i < numUnits; ++i)
			{
				IUnit unit = IUnit.BinaryToUnit(in);
				InsertUnit(unit);
			}
			
			ArrayList<Integer> removeUnit = new ArrayList<Integer>();
			for (City city : cities)
			{
				removeUnit.clear();
				for (Integer fortified : city.fortifiedUnits)
				{
					if (units.containsKey(fortified) == false)
					{
						removeUnit.add(fortified);
					}
				}
				city.fortifiedUnits.removeAll(removeUnit);
			}
			
			
			bFogOfWar = in.readBoolean();
			
			if (bFogOfWar == true)
			{
				fogMaps = new FogMap[players.size()];
				for (int player = 0; player < players.size(); ++player)
				{
					fogMaps[player] = new FogMap(mapSize);
					fogMaps[player].BinaryToState(in);
				}
			}
		}
		
		bubbles = new ArrayList<IBubbleInfo>();
		if (version >= VERSION_WITH_INFO_BUBBLES)
		{
			int numBubbles = in.readInt();	
			for (int i = 0; i < numBubbles; ++i)
			{
				IBubbleInfo info = IBubbleInfo.BinaryToInfo(in);
				bubbles.add(info);
			}
		}
		
		if (version >= VERSION_WITH_UPDATETIME)
		{
			lastUpdateTime = in.readLong();
		}
		
		if (version >= VERSION_WITH_MISSEDTURNCOUNT)
		{
			for (int i = 0; i < players.size(); ++i)
			{
				missedTurnCount.set(i, in.readInt());
			}
		}
		
		if (version >= VERSION_WITH_MAXTURNSECONDS)
		{
			maxTurnSeconds = in.readInt();
		}
		
		if (version >= VERSION_WITH_AI)
		{
			bLocalGame = in.readBoolean();
		}
		
		if (version >= VERSION_WITH_CITY_PRODUCTION_INDICATOR)
		{
			for (City city : cities)
			{
				city.bHasProduced = in.readBoolean();
			}
		}
		
		if (version >= VERSION_WITH_POWERGRAPH)
		{
			int entries = in.readInt();
			for (int i = 0; i < entries; ++i)
			{
				ArrayList<Integer> values = new ArrayList<Integer>();
				int playerId = in.readInt();
				int size = in.readInt();
				for (int j = 0; j < size; ++j)
				{
					values.add(in.readInt());
				}
				powerGraph.put(playerId, values);
			}
		}
		
		if (version >= VERSION_WITH_GAME_VERSION)
		{
			mGameVersion = in.readInt();
		}
		
		if (version >= VERSION_WITH_GAMESTATE_MESSAGE)
		{
			gameStateMessage = in.readUTF();
		}
		
		if (version >= VERSION_WITH_FRIENDSONLY)
		{
			bFriendsOnly = in.readBoolean();
			createdByAlias = in.readUTF();
			createdById = in.readInt();
		}
		
		if (version >= VERSION_WITH_UNITCONFIG)
		{
			for (Entry<Integer, IUnit> entry : units.entrySet())
			{
				IUnit unit = entry.getValue();
				unit.bChangedConfig = in.readBoolean();
				unit.mConfig = in.readInt();
			}
		}
		
		if (version >= VERSION_WITH_INTERMEDIATE_AI)
		{
			for (int i = 0; i < 7; ++i)
			{
				aiLevel[i] = AILevel.values()[in.readInt()];
			}
		}
		
		if (version >= VERSION_WITH_TUTORIAL_FLAG)
		{
			bTutorial = in.readBoolean();
		}
		
		if (version >= VERSION_WITH_TRACKS)
		{
			tracks.clear();
			int numTracks = in.readInt();
			for (int i = 0; i < numTracks; ++i)
			{
				int unitId = in.readInt();
				byte numPoints = in.readByte();
				Tracks newTrack = new Tracks();
				newTrack.numPoints = numPoints;
				newTrack.renderPoints = numPoints;
				// TODO: could speed this up if we re-use data from oldState, where possible
				newTrack.x = new byte[numPoints];
				newTrack.y = new byte[numPoints];
				in.readFully(newTrack.x, 0, numPoints);
				in.readFully(newTrack.y, 0, numPoints);
				newTrack.type = in.readByte();
				newTrack.playerId = in.readInt();
				tracks.put((long) unitId, newTrack);
			}
		}

		bubbles2 = new ArrayList<IBubbleInfo2>();
		if (version >= VERSION_WITH_NEWBUBBLEINFO)
		{
			int numBubbles = in.readInt();
			for (int i = 0; i < numBubbles; ++i)
			{
				IBubbleInfo2 info = IBubbleInfo2.BinaryToInfo(in);
				bubbles2.add(info);
			}
		}
	}
	
	@Override
	public boolean IsFriendsOnly()
	{
		return bFriendsOnly;
	}
	
	@Override
	public int GetCreatorId()
	{
		return createdById;
	}

	@Override
	public GameStatus GetGameState()
	{
		return gameState;
	}
	

		  public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException
		  {
		    final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		    while (src.read(buffer) != -1) {
		      // prepare the buffer to be drained
		      buffer.flip();
		      // write to the channel, may block
		      dest.write(buffer);
		      // If partial transfer, shift remainder down
		      // If buffer is empty, same as doing clear()
		      buffer.compact();
		    }
		    // EOF will leave buffer in fill state
		    buffer.flip();
		    // make sure the buffer is fully drained.
		    while (buffer.hasRemaining())
		    {
		      dest.write(buffer);
		    }
		  }
		
	
	@Override
	public IState CopyState()
	{
		try
		{
			TactikonState copyState = new TactikonState();
			
			// TODO: can we speed this up by writing at the same time as reading? or generally not allocating so much
			// or just passing the reference of any constant data
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			this.StateToBinary(new DataOutputStream(out));
			copyState.BinaryToStateWithOld(this, new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
					
			return copyState;
		} catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public long GetPlayerToPlay()
	{
		if (GetGameState() == GameStatus.InGame) return playerToPlay;
		return -999;
	}
}
