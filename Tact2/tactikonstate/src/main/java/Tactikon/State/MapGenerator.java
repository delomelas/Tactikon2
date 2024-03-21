package Tactikon.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;


import Tactikon.State.TactikonNewGameOptions.MirrorType;

public class MapGenerator
{
	TactikonNewGameOptions mMapInfo;
	
	int cityDensity = 1;
	
	int mBalanceScore = 0;
	
	Random mRand;
	
	MapGenerator(TactikonNewGameOptions mapInfo)
	{
		mMapInfo = mapInfo;
		
		mRand = new Random(mMapInfo.mapSeed);
		if (mapInfo.cities == 0) cityDensity = 80;
		if (mapInfo.cities == 1) cityDensity = 40;
		if (mapInfo.cities == 2) cityDensity = 25;
		
		if (mapInfo.mirrorType == MirrorType.Horizontal || mapInfo.mirrorType == MirrorType.Both)
		{
			cityDensity = cityDensity * 2;
		}
		
		if (mapInfo.mirrorType == MirrorType.Vertical || mapInfo.mirrorType == MirrorType.Both)
		{
			cityDensity = cityDensity * 2;
		}
	}
	
	float NoiseToTiles(float noiseMap[][], int[][] map, float[][] mapHeight)
	{
		// calculate min and max
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for (int x = 0; x < mMapInfo.mapSize*2+1; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize*2+1; ++y)
			{
				float val = noiseMap[x][y];
				if (val < min) min = val;
				if (val > max) max = val;
			}
		}
		// reduce to 0..1 range
		for (int x = 0; x < mMapInfo.mapSize*2+1; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize*2+1; ++y)
			{
				float val = noiseMap[x][y];
				val = val - min;
				val = val / (max - min);
				noiseMap[x][y] = val;
			}
		}
		
		
		
		
		float landMass = 0.0f;
		float level = 0.5f;
		float offset = 0.5f;
		
		for (int search = 0; search < 20; ++search)
		{
			
			int numLand = 0;
			for (int x = 0; x < mMapInfo.mapSize; ++x)
			{
				for (int y = 0; y < mMapInfo.mapSize; ++y)
				{
					if (noiseMap[x*2+1][y*2+1] > level)
					{
						numLand ++;
						map[x][y] = TactikonState.TileType_Land;
					} else
					{
						map[x][y] = TactikonState.TileType_Water;
					}
				}
			}
			landMass = (float)numLand / (float)(mMapInfo.mapSize * mMapInfo.mapSize);
			
			
			if (landMass > mMapInfo.landMassRatio)
			{
				level = level + offset;
			} else
			{
				level = level - offset;
			}
			
			offset = offset / 2;
		}
		
		for (int x = 0; x < mMapInfo.mapSize+1; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize+1; ++y)
			{
				mapHeight[x][y] = noiseMap[x*2][y*2];
			}
		}
		
		float treeLine = (1 - level) / 3 + level;
		
		if (mMapInfo.bMountains == false)
		{
			treeLine = (1 - level) / 2 + level; // make the trees start higher if there are no mountains
		}
		
		float mountainLine = (1 - treeLine) / 2 + treeLine;
		
		if (mMapInfo.bForest == true)
		{
		// 	convert some into jungle
			for (int x = 0; x < mMapInfo.mapSize;++x)
			{
				for (int y = 0; y < mMapInfo.mapSize; ++y)
				{
					float val = noiseMap[x*2+1][y*2+1];
					if (val > treeLine) map[x][y] = TactikonState.TileType_Jungle;
				}
			}
		}
		
		if (mMapInfo.bMountains == true)
		{
		// 	convert some into mountains
			for (int x = 0; x < mMapInfo.mapSize;++x)
			{
				for (int y = 0; y < mMapInfo.mapSize; ++y)
				{
					if (noiseMap[x*2+1][y*2+1] > mountainLine) map[x][y] = TactikonState.TileType_Mountain;
				}
			}
		}
		return level;
		
	}
	
	
	
	void CreateRandomMap(TactikonState state)
	{
		
		float z = mRand.nextInt() + mRand.nextFloat();
		
		float scale = (mMapInfo.scale / mMapInfo.mapSize);
		if (mMapInfo.mapSize <= 32) scale = scale / 2;
		// generate noise
		float noiseMap[][] = new float[(mMapInfo.mapSize*2)+1][(mMapInfo.mapSize*2)+1];
		ImprovedNoise noise = new ImprovedNoise();
		for (int x = 0; x < (mMapInfo.mapSize*2)+1; ++x)
		{
			float xx = (float)(x/2.0f)*scale;
			double xx2 = xx * 2;
			double xx4 = xx * 4;
			double xx8 = xx * 8;
			for (int y = 0; y < (mMapInfo.mapSize*2)+1; ++y)
			{
				float yy = (float)(y/2.0f)*scale;
				noiseMap[x][y] = (float)noise.noise(((double)xx)  + mMapInfo.mapSeed, ((double)yy), z);
				noiseMap[x][y] += ((float)noise.noise(xx2  + mMapInfo.mapSeed, ((double)yy * 2), z)) / 2;
				noiseMap[x][y] += ((float)noise.noise(xx4  + mMapInfo.mapSeed, ((double)yy * 4), z)) / 4;
				noiseMap[x][y] += ((float)noise.noise(xx8  + mMapInfo.mapSeed, ((double)yy * 8), z)) / 8;
			}
		}
		
		if (mMapInfo.mirrorType == MirrorType.Horizontal || mMapInfo.mirrorType == MirrorType.Both)
		{
			for (int x = 0; x < ((mMapInfo.mapSize * 2) + 1) / 2; ++x)
			{
				for (int y = 0; y < (mMapInfo.mapSize * 2) + 1; ++y)
				{
					noiseMap[mMapInfo.mapSize * 2 - x][y] = noiseMap[x][y];
				}
			}
		}
		if (mMapInfo.mirrorType == MirrorType.Vertical || mMapInfo.mirrorType == MirrorType.Both)
		{
			for (int x = 0; x < ((mMapInfo.mapSize * 2) + 1); ++x)
			{
				for (int y = 0; y < ((mMapInfo.mapSize * 2) + 1)/2; ++y)
				{
					noiseMap[x][mMapInfo.mapSize * 2 - y] = noiseMap[x][y];
				}
			}
		}
		
		state.seaLevel = NoiseToTiles(noiseMap, state.map, state.mapHeight);
	}
	
	class GridPosition implements Comparable<GridPosition>
	{
		GridPosition(int x, int y)
		{
			this.x = x;
			this.y = y;
			this.dist = 9999;
		}
		
		int x;
		int y;
		int dist = 9999;
		
		@Override
		public boolean equals(Object other)
		{
			if (other == null) return false;
			if (other == this) return true;
			
			if (!(other instanceof GridPosition)) return false;
			
			GridPosition pos = (GridPosition)other;
			
			if (pos.x == this.x && pos.y == this.y) return true;
			
			return false;
		}
		
		@Override
		public int compareTo(GridPosition that) {
	        if (this.y < that.y) return -1;
	        if (this.y > that.y) return +1;
	        if (this.x < that.x) return -1;
	        if (this.x > that.x) return +1;
	        return 0;
	    }
	}
	
	private void FloodFill(int x,int y, int size, final int val, final boolean type, int[][] massMap, int[][] map)
	{

		Stack<GridPosition> list = new Stack<GridPosition>();
		list.add(new GridPosition(x, y));
		GridPosition currpos;
		do 
		{
			currpos=list.pop();
			x = currpos.x;
			y = currpos.y;
			massMap[x][y] = val;
			if ((x > 0) && (massMap[x-1][y] == 0) && (type == IsLand(x - 1, y, map)))
				list.push(new GridPosition(x-1,y));
			if ((x < size - 1) && (massMap[x+1][y] == 0) && (type == IsLand(x + 1, y, map)))
				list.push(new GridPosition(x+1,y));
			if ((y > 0) && (massMap[x][y-1] == 0) && (type == IsLand(x, y - 1, map)))
				list.push(new GridPosition(x,y-1));
			if ((y < size - 1) && (massMap[x][y+1] == 0) && (type == IsLand(x , y + 1, map)))
				list.push(new GridPosition(x,y+1));
			
		} while (list.size()>0);
	}
	
	public boolean IsLand(final int x, final int y, int[][] map)
	{
		if (map[x][y] == TactikonState.TileType_Land) return true;
		if (map[x][y] == TactikonState.TileType_Mountain) return true;
		if (map[x][y] == TactikonState.TileType_Jungle) return true; 
		
		return false;
	}
	
	int[][] CreateLandmassMap(int[][] map)
	{
		int[][] massMap = new int[mMapInfo.mapSize][mMapInfo.mapSize];
		int val = 0;
		ArrayList<Integer> massList = new ArrayList<Integer>();
		for (int x = 0; x < mMapInfo.mapSize; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize; ++y)
			{
				if (massMap[x][y] == 0)
				{
					val ++;
					massList.add(val);
					FloodFill(x, y, mMapInfo.mapSize, val, IsLand(x, y, map), massMap, map);
				}
			}
		}
		
		return massMap;
	}
	
	class CoastSection
	{
		int startMass;
		int adjacentMass;
		ArrayList<GridPosition> coastTiles = new ArrayList<GridPosition>();
	}
	
	void AddCoast(int m1, int m2, ArrayList<CoastSection> coastMap, int x, int y)
	{
		boolean added = false;
		for (CoastSection section : coastMap)
		{
			if (section.startMass == m1 && section.adjacentMass == m2)
			{
				section.coastTiles.add(new GridPosition(x, y));
				added = true;
				break;
			}
		}
		if (added == false)
		{
			CoastSection section = new CoastSection();
			section.startMass = m1;
			section.adjacentMass = m2;
			section.coastTiles.add(new GridPosition(x,y));
			coastMap.add(section);
		}
		
	}
	
	
	ArrayList<CoastSection> CreateCoastMap(int[][] massMap, int[][] map)
	{
		ArrayList<CoastSection> coastMap = new ArrayList<CoastSection>();
		for (int x = 0; x < mMapInfo.mapSize; ++x)
			for (int y = 0; y < mMapInfo.mapSize; ++y)
			{
				if (IsLand(x, y, map) == false) continue;
				if (x > 0 && massMap[x][y] != massMap[x-1][y])
				{
					AddCoast(massMap[x][y], massMap[x-1][y], coastMap, x, y);
					continue;
				}
				if (x < (mMapInfo.mapSize - 1) && massMap[x][y] != massMap[x+1][y])
				{
					AddCoast(massMap[x][y], massMap[x+1][y], coastMap, x, y);
					continue;
				}
				if (y > 0 && massMap[x][y] != massMap[x][y-1])
				{
					AddCoast(massMap[x][y], massMap[x][y-1], coastMap, x, y);
					continue;
				}
				if (y < (mMapInfo.mapSize - 1) && massMap[x][y] != massMap[x][y+1])
				{
					AddCoast(massMap[x][y], massMap[x][y+1], coastMap, x, y);
					continue;
				}
			}
			
		
		
		return coastMap;
	}
	
	Map<Integer, Integer> CreateLandmassSize(int[][] massMap)
	{
		Map<Integer, Integer> mapSizes = new TreeMap<Integer, Integer>();
		for (int x = 0; x < mMapInfo.mapSize; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize; ++y)
			{
				if (mapSizes.containsKey(massMap[x][y]))
				{
					int current = mapSizes.get(massMap[x][y]);
					current ++;
					mapSizes.put(massMap[x][y], current);
				} else
				{
					mapSizes.put(massMap[x][y], 1);
				}
			}
		}
		return mapSizes;
	}
	
	// returns a GridPosition that's as far away as possible
	GridPosition ChooseFarPos(ArrayList<GridPosition> positions)
	{
		GridPosition bestGridPos = null;
		int bestDist = -1;
		for (GridPosition pos : positions)
		{
			if (mMapInfo.mirrorType == MirrorType.Horizontal || mMapInfo.mirrorType == MirrorType.Both)
			{
				if (pos.x > (mMapInfo.mapSize/2) -3) continue;
			}
			if (mMapInfo.mirrorType == MirrorType.Vertical || mMapInfo.mirrorType == MirrorType.Both)
			{
				if (pos.y > (mMapInfo.mapSize/2) -3) continue;
			}
			if (pos.dist > bestDist)
			{
				bestDist = pos.dist;
				bestGridPos = pos;
			}
			
		}
		if (bestDist < 4) return null;
		return bestGridPos;
	}
	
	boolean AjacentTileIs(int x, int y, int[][] map, int type)
	{
		if (x > 0 && map[x-1][y] == type) return true;
		if (x < mMapInfo.mapSize - 1 && map[x+1][y] == type) return true;
		if (y > 0 && map[x][y-1] == type) return true;
		if (y < mMapInfo.mapSize - 1 && map[x][y+1] == type) return true;
		
		return false;
	}
	
	void AddCity(ArrayList<GridPosition> tiles, City city)
	{
		for (GridPosition pos : tiles)
		{
			int d = Math.abs(city.x - pos.x) + Math.abs(city.y - pos.y);
			if (d < pos.dist) pos.dist = d;
		}
	}
	
	class LandmassInfo
	{
		int landmassId;
		int numCities;
		ArrayList<City> cities = new ArrayList<City>();
	}
	
	City ClosestNonStartingCity(GridPosition pos, ArrayList<City> cities, ArrayList<City> startingCities)
	{
		int bestDist = 9999;
		City bestCity = null;
		for (City city : cities)
		{
			if (startingCities.contains(city)) continue;
			int dist = Math.abs(city.x - pos.x) + Math.abs(city.y - pos.y);
			if (dist < bestDist)
			{
				bestCity = city;
				bestDist = dist;
			}
		}
		return bestCity;
	}

	class BalanceFactors
	{ // each factor scaled to be out of ten
		int distanceToClosestOtherCity = 0;
		int numberOfCitiesOnSameLandmass = 0;
		int distanceToUsefulPort = 0;
	}
	
	int GetBalanceScore(ArrayList<BalanceFactors> factors)
	{
		int minDistanceToClosestOtherCity = 99999;
		int minNumberOfCitiesOnSameLandmass = 99999;
		int minDistanceToUsefulPort = 99999;
		int maxDistanceToClosestOtherCity = 0;
		int maxNumberOfCitiesOnSameLandmass = 0;
		int maxDistanceToUsefulPort = 0;
		for (BalanceFactors factor : factors)
		{
			if (factor.distanceToClosestOtherCity < minDistanceToClosestOtherCity)
				minDistanceToClosestOtherCity = factor.distanceToClosestOtherCity;
			
			if (factor.distanceToUsefulPort < minDistanceToUsefulPort)
				minDistanceToUsefulPort = factor.distanceToUsefulPort;
			
			if (factor.numberOfCitiesOnSameLandmass < minNumberOfCitiesOnSameLandmass)
				minNumberOfCitiesOnSameLandmass = factor.numberOfCitiesOnSameLandmass;
			
			if (factor.distanceToClosestOtherCity > maxDistanceToClosestOtherCity)
				maxDistanceToClosestOtherCity = factor.distanceToClosestOtherCity;
			
			if (factor.distanceToUsefulPort > maxDistanceToUsefulPort)
				maxDistanceToUsefulPort = factor.distanceToUsefulPort;
			
			if (factor.numberOfCitiesOnSameLandmass > maxNumberOfCitiesOnSameLandmass)
				maxNumberOfCitiesOnSameLandmass = factor.numberOfCitiesOnSameLandmass;
		}
		
		// score these such that 10 would be unacceptable bad for any of them
		int scores[] = new int[3];
		scores[0] = (maxDistanceToClosestOtherCity - minDistanceToClosestOtherCity);
		scores[1] = (maxNumberOfCitiesOnSameLandmass - minNumberOfCitiesOnSameLandmass) * 5;
		scores[2] = (maxDistanceToUsefulPort - minDistanceToUsefulPort);
		
		// and return the largest of these
		
		int largest = 0;
		for (int i = 0; i < scores.length; ++i)
		{
			if (scores[i] > largest) largest = scores[i];
		}
		
		return largest;
	}
	
	int GetDistToClosestOtherCity(City city, ArrayList<City> cities)
	{
		int bestDist = 9999;
		for (City otherCity : cities)
		{
			if (otherCity == city) continue;
			int dist = Math.abs(city.x - otherCity.x) + Math.abs(city.y - otherCity.y);
			if (dist < bestDist)
			{
				bestDist = dist;
			}
		}
		return bestDist;
		
	}
	
	boolean IsOnMap(int x, int y)
	{
		if (x < mMapInfo.mapSize && x >= 0 && y >= 0 && y < mMapInfo.mapSize) return true;
		return false;
	}
	
	ArrayList<Integer> GetLandmassNeighbours(int mass, int[][] massMap)
	{
		ArrayList<Integer> neighbourMasses = new ArrayList<Integer>();
		Set<Integer> neighbourSet = new TreeSet<Integer>();
		for (int x = 0; x < mMapInfo.mapSize; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize; ++y)
			{
				if (massMap[x][y] == mass)
				{
					if (IsOnMap(x+1, y) && massMap[x+1][y] != mass) neighbourSet.add(massMap[x+1][y]);
					if (IsOnMap(x-1, y) && massMap[x-1][y] != mass) neighbourSet.add(massMap[x-1][y]);
					if (IsOnMap(x, y+1) && massMap[x][y+1] != mass) neighbourSet.add(massMap[x][y+1]);
					if (IsOnMap(x, y-1) && massMap[x][y-1] != mass) neighbourSet.add(massMap[x][y-1]);
				}
			}
		}
		
		neighbourMasses.addAll(neighbourSet);
		return neighbourMasses;
	}
	ArrayList<Integer> GetWaterLandmassNeighbours(GridPosition pos, int[][] massMap, int[][] map)
	{
		int x = pos.x;
		int y = pos.y;
		ArrayList<Integer> neighbourMasses = new ArrayList<Integer>();
		Set<Integer> neighbourSet = new TreeSet<Integer>();
		if (IsOnMap(x+1, y) && map[x+1][y] == TactikonState.TileType_Water) neighbourSet.add(massMap[x+1][y]);
		if (IsOnMap(x-1, y) && map[x-1][y] == TactikonState.TileType_Water) neighbourSet.add(massMap[x-1][y]);
		if (IsOnMap(x, y+1) && map[x][y+1] == TactikonState.TileType_Water) neighbourSet.add(massMap[x][y+1]);
		if (IsOnMap(x, y-1) && map[x][y-1] == TactikonState.TileType_Water) neighbourSet.add(massMap[x][y-1]);
		
		neighbourMasses.addAll(neighbourSet);
		return neighbourMasses;
	}
	
	Map<Integer, ArrayList<Integer>> neighbourCache = new TreeMap<Integer, ArrayList<Integer>>();
	
	int GetDistToClosestPort(City city, ArrayList<City> cities, int[][] map, int[][] massMap)
	{
		int bestDist = 9999;
		
		for (City otherCity : cities)
		{
			if (!AjacentTileIs(otherCity.x, otherCity.y, map, TactikonState.TileType_Water)) continue;

			ArrayList<Integer> seasPortServes = GetWaterLandmassNeighbours(new GridPosition(otherCity.x, otherCity.y), massMap, map);
			
			ArrayList<Integer> possibleLandmassDestinations = new ArrayList<Integer>();
			
			for (Integer sea : seasPortServes)
			{
				ArrayList<Integer> lands = null;
				if (neighbourCache.containsKey(sea))
				{
					lands = neighbourCache.get(sea);
				} else
				{
					lands = GetLandmassNeighbours(sea, massMap);
					neighbourCache.put(sea, lands);
				}
				 
				possibleLandmassDestinations.addAll(lands);
			}
			// remove the one the city was from
			int index = possibleLandmassDestinations.indexOf(massMap[otherCity.x][otherCity.y]);
			if (index != -1) possibleLandmassDestinations.remove(index);
			
			if (possibleLandmassDestinations.size() == 0) continue;
			
			// now now count up the cities on these landmasses
			int count = 0;
			for (City destinationCity : cities)
			{
				int mass = massMap[destinationCity.x][destinationCity.y];
				if (possibleLandmassDestinations.contains(mass)) count++;
			}
			if (count == 0) continue;
			
			int dist = Math.abs(city.x - otherCity.x) + Math.abs(city.y - otherCity.y);
			if (dist < bestDist)
			{
				bestDist = dist;
				if (bestDist == 0) break;
			}
		}
		return bestDist;
	}
	
	ArrayList<City> AddCities(TactikonState state)
	{
		ArrayList<City> cities = new ArrayList<City>();
		// work out landmass numbers
		int[][] landMassMap = CreateLandmassMap(state.map);
		//Map<Integer, Integer> landMassSize = CreateLandmassSize(landMassMap);
		
		// find coast tiles for each landmass
		ArrayList<CoastSection> coastMap = CreateCoastMap(landMassMap, state.map);
		
		ArrayList<GridPosition> landTiles = new ArrayList<GridPosition>();
		for (int x = 0; x < mMapInfo.mapSize; ++x)
		{
			for (int y = 0; y < mMapInfo.mapSize; ++y)
			{
				if (state.map[x][y] == TactikonState.TileType_Land || state.map[x][y] == TactikonState.TileType_Jungle) landTiles.add(new GridPosition(x, y)); 
			}
		}
		
		// place one city on each section of coast, minimum (so long as the coast is longer than 5 tiles)
		for (CoastSection coastSection : coastMap)
		{
			if (coastSection.coastTiles.size() > 5)
			{
				for (int i = 0; i < (coastSection.coastTiles.size() / cityDensity) + 1; ++i)
				{
					// add cities, up to the city density
					GridPosition pos = ChooseFarPos(coastSection.coastTiles);
					if (pos == null) break;
					
					City city = new City();
					city.x = pos.x;
					city.y = pos.y;
					city.playerId = -1;
					cities.add(city);
					
					AddCity(landTiles, city);
					for (CoastSection coastSection2 : coastMap)
						AddCity(coastSection2.coastTiles, city);
				}
			}
		}
		
		while ((float)landTiles.size() / cities.size() > cityDensity || cities.size() < (mMapInfo.numHumanPlayers + mMapInfo.numAIPlayers))
		{
			GridPosition pos = ChooseFarPos(landTiles);
			if (pos == null) break; // exceeded capacity
				
			City city = new City();
			city.x = pos.x;
			city.y = pos.y;
			city.playerId = -1;
			cities.add(city);
			
			AddCity(landTiles, city);
			
		}
		
		// if mirroring is on, mirror the cities
		
		if (mMapInfo.mirrorType == MirrorType.Horizontal || mMapInfo.mirrorType == MirrorType.Both)
		{
			ArrayList<City> mirrorCities = new ArrayList<City>();
			for (City city : cities)
			{
				City newCity = new City();
				newCity.x = (mMapInfo.mapSize -1) - city.x;
				newCity.y = city.y;
				newCity.playerId = -1;
				mirrorCities.add(newCity);
			}
			cities.addAll(mirrorCities);
		}
		
		if (mMapInfo.mirrorType == MirrorType.Vertical || mMapInfo.mirrorType == MirrorType.Both)
		{
			ArrayList<City> mirrorCities = new ArrayList<City>();
			for (City city : cities)
			{
				City newCity = new City();
				newCity.y = (mMapInfo.mapSize -1) - city.y;
				newCity.x = city.x;
				newCity.playerId = -1;
				mirrorCities.add(newCity);
			}
			cities.addAll(mirrorCities);
		}
		
		
		// designate some cities as starting cities

		// count the number of cities on each landmass
		Map<Integer, Integer> citiesOnLandmassMap = new TreeMap<Integer, Integer>(); 
		for (City city : cities)
		{
			int landmass = landMassMap[city.x][city.y];
			if (citiesOnLandmassMap.containsKey(landmass))
			{
				int current = citiesOnLandmassMap.get(landmass);
				citiesOnLandmassMap.put(landmass, current + 1);
			} else
			{
				citiesOnLandmassMap.put(landmass, 1);
			}
		}
		
		// sort the landmasses by number of cities
		Map<Float, Integer> sortedLandmassesMap = new TreeMap<Float, Integer>();
		float inc = 0;
		for (Entry <Integer, Integer> entry : citiesOnLandmassMap.entrySet())
		{			
			sortedLandmassesMap.put(inc + entry.getValue(), entry.getKey());
			inc += 0.01f;
		}
		
		// move the sorted list into an array
		ArrayList<LandmassInfo> landmassInfo = new ArrayList<LandmassInfo>();
		for (Entry <Float, Integer> entry : sortedLandmassesMap.entrySet())
		{
			LandmassInfo info = new LandmassInfo();
			info.landmassId = entry.getValue();
			info.numCities = Math.round(entry.getKey());
			landmassInfo.add(info);
		}
		
		Map<Integer, LandmassInfo> infoMap = new TreeMap<Integer, LandmassInfo>();
		for (LandmassInfo info : landmassInfo)
		{
			infoMap.put(info.landmassId, info);
		}
		
		ArrayList<GridPosition> edgeList = new ArrayList<GridPosition>();
		
		for (int x = 0; x < mMapInfo.mapSize; x = x + 2)
		{
			edgeList.add(new GridPosition(x, 0));
		}
		for (int y = 0; y < mMapInfo.mapSize; y = y + 2)
		{
			edgeList.add(new GridPosition(mMapInfo.mapSize - 1, y));
		}
		for (int x = mMapInfo.mapSize - 1; x >= 0; x = x - 2)
		{
			edgeList.add(new GridPosition(x, mMapInfo.mapSize - 1));
		}
		for (int y = mMapInfo.mapSize - 1; y >= 0; y = y - 2)
		{
			edgeList.add(new GridPosition(0, y));
		}
		
		int span = edgeList.size() / (mMapInfo.numHumanPlayers + mMapInfo.numAIPlayers);
		
		ArrayList<ArrayList<City>> startingOptions = new ArrayList<ArrayList<City>>();
		
		for (int spanStart = 0; spanStart < span; ++spanStart)
		{
			ArrayList<City> startCities = new ArrayList<City>();
			for (int p = 0; p < (mMapInfo.numHumanPlayers + mMapInfo.numAIPlayers); ++p)
			{
				City startCity = ClosestNonStartingCity(edgeList.get(spanStart + p * span), cities, startCities);
				startCities.add(startCity);
			}
			
			startingOptions.add(startCities);
		}
		
		neighbourCache.clear();
		
		// now choose the most balanced option from those available
		int bestBalScore = 1000;
		ArrayList<City> bestOption = null;
		for (ArrayList<City> option : startingOptions)
		{
			ArrayList<BalanceFactors> factors = new ArrayList<BalanceFactors>();
			for (City city : option)
			{
				int closestPortDistance = GetDistToClosestPort(city, cities, state.map, landMassMap);
				int closestCityDistance = GetDistToClosestOtherCity(city, cities);
				int numCitiesOnLandMass = infoMap.get(landMassMap[city.x][city.y]).numCities;
				
				BalanceFactors bal = new BalanceFactors();
				bal.distanceToClosestOtherCity = closestCityDistance;
				bal.numberOfCitiesOnSameLandmass = numCitiesOnLandMass;
				bal.distanceToUsefulPort = closestPortDistance;
				factors.add(bal);
			}
			int score = GetBalanceScore(factors);
			if (score < bestBalScore)
			{
				bestBalScore = score;
				bestOption = option;
			}
		}
		
		state.mapBalanceScore = (bestBalScore * 2) / (mMapInfo.numHumanPlayers + mMapInfo.numAIPlayers);
		
		
		for (City city : bestOption)
		{
			city.startingCity = true;
		}
		
		// assign ids to the cities
		for (int i = 0; i < cities.size(); ++i)
		{
			cities.get(i).cityId = i;
		}
		
		// convert suitable land to ports
		for (City city : cities)
		{
			if (AjacentTileIs(city.x, city.y, state.map, TactikonState.TileType_Water))
			{
				state.map[city.x][city.y] = TactikonState.TileType_Port;
				city.isPort = true;
			}
		}
		
		return cities;
	}
	
	void GenerateMap(TactikonState state) 
	{
		CreateRandomMap(state);
	}
	
	public final class ImprovedNoise
	{
		int X, Y, Z;
		double uu, vv, w;
		int A, B, AA, AB, BA, BB;
	   public double noise(double x, double y, double z)
	   {
	      X = (int)Math.floor(x) & 255;                  // FIND UNIT CUBE THAT
	      Y = (int)Math.floor(y) & 255;                  // CONTAINS POINT.
	      Z = (int)Math.floor(z) & 255;
	      x -= Math.floor(x);                                // FIND RELATIVE X,Y,Z
	      y -= Math.floor(y);                                // OF POINT IN CUBE.
	      z -= Math.floor(z);
	      uu = fade(x);                                // COMPUTE FADE CURVES
	      vv = fade(y);                                // FOR EACH OF X,Y,Z.
	      w = fade(z);
	      A = p[X  ]+Y;
	      AA = p[A]+Z;
	      AB = p[A+1]+Z;      // HASH COORDINATES OF
	      B = p[X+1]+Y;
	      BA = p[B]+Z;
	      BB = p[B+1]+Z;      // THE 8 CUBE CORNERS,

	      return lerp(w, lerp(vv, lerp(uu, grad(p[AA  ], x  , y  , z   ),  // AND ADD
	                                     grad(p[BA  ], x-1, y  , z   )), // BLENDED
	                             lerp(uu, grad(p[AB  ], x  , y-1, z   ),  // RESULTS
	                                     grad(p[BB  ], x-1, y-1, z   ))),// FROM  8
	                     lerp(vv, lerp(uu, grad(p[AA+1], x  , y  , z-1 ),  // CORNERS
	                                     grad(p[BA+1], x-1, y  , z-1 )), // OF CUBE
	                             lerp(uu, grad(p[AB+1], x  , y-1, z-1 ),
	                                     grad(p[BB+1], x-1, y-1, z-1 ))));
	   }
	   double fade(double t)
	   {
		   return t * t * t * (t * (t * 6 - 15) + 10);
		   }
	   double lerp(double t, double a, double b)
	   {
		   return a + t * (b - a);
	   }
	   
	   int h;
	   double u,v;
	   
	   double grad(int hash, double x, double y, double z)
	   {
	      h = hash & 15;                      // CONVERT LO 4 BITS OF HASH CODE
	      u = h<8 ? x : y;                 // INTO 12 GRADIENT DIRECTIONS.
	      v = h<4 ? y : h==12||h==14 ? x : z;
	      return ((h&1) == 0 ? u : -u) + ((h&2) == 0 ? v : -v);
	   }
	   final int p[] = new int[512], permutation[] = { 151,160,137,91,90,15,
	   131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
	   190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
	   88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
	   77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
	   102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
	   135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
	   5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
	   223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
	   129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
	   251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
	   49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
	   138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
	   };
	   
	   ImprovedNoise()
	   {
		   for (int i=0; i < 256 ; i++) p[256+i] = p[i] = permutation[i];
	   }
	}

}
