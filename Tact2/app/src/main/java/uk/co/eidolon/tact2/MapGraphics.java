package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import Core.PlayerInfo;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;

public class MapGraphics
{
	static ArrayList<Integer> landColours = new ArrayList<Integer>();
	static ArrayList<Integer> seaColours = new ArrayList<Integer>();
	static ArrayList<Integer> coastColours = new ArrayList<Integer>();
	static ArrayList<Integer> mntColours = new ArrayList<Integer>();
	static ArrayList<Integer> jungleColours = new ArrayList<Integer>();
	
	
	private static void SetupColours()
	{
		landColours.add(Color.parseColor("#707936"));
		landColours.add(Color.parseColor("#737a38"));
		landColours.add(Color.parseColor("#747b38"));
		landColours.add(Color.parseColor("#717d36"));
		landColours.add(Color.parseColor("#7a7b3c"));
		landColours.add(Color.parseColor("#767d39"));
		
		seaColours.add(Color.parseColor("#3f79a0"));
		seaColours.add(Color.parseColor("#407da5"));
		seaColours.add(Color.parseColor("#437da7"));
		seaColours.add(Color.parseColor("#3d80ab"));
		seaColours.add(Color.parseColor("#467faa"));
		
		mntColours.add(Color.parseColor("#888888"));
		mntColours.add(Color.parseColor("#aaaaaa"));
		mntColours.add(Color.parseColor("#888898"));
		mntColours.add(Color.parseColor("#bbbbcc"));
		
		jungleColours.add(Color.parseColor("#558157"));
		jungleColours.add(Color.parseColor("#08392e"));
		jungleColours.add(Color.parseColor("#42704f"));
		jungleColours.add(Color.parseColor("#7d915f"));
		
		//seaColours.add(Color.parseColor("#5b89b1"));		
	}
	
	static Random rand = new Random(0);
	
	static int GetSeaColour()
	{
		return seaColours.get(rand.nextInt(seaColours.size()));
	}
	
	static int GetLandColour()
	{
		return landColours.get(rand.nextInt(seaColours.size()));
	}
	
	static int GetMntColour()
	{
		return mntColours.get(rand.nextInt(mntColours.size()));
	}
	
	static int GetJungleColour()
	{
		return jungleColours.get(rand.nextInt(jungleColours.size()));
	}
	
	public static BitmapDrawable GeneratePreview(TactikonState state, long UserID)
	{
		SetupColours(); // Canvas
		rand = new Random(0);
		
		Bitmap miniMap = Bitmap.createBitmap(state.mapSize, state.mapSize, Bitmap.Config.RGB_565 );
		int cityCol = Color.rgb(192, 192, 192);
		int startCityCol = Color.rgb(255, 0, 0);
		
		byte[][] fogMap = state.GetResolvedFogMap((int) UserID);
		byte[][] visMap = state.GetVisibilityMap((int)UserID);
		
		Map<Integer, Integer> playerColours = new TreeMap<Integer, Integer>();
		for (int i = 0; i < state.players.size(); ++i)
		{
			PlayerInfo info = state.GetPlayerInfo(state.players.get(i));
			if (info != null)
			{
				playerColours.put(state.players.get(i), Color.rgb(info.r, info.g, info.b));
			}
		}
		
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (state.map[x][y] == TactikonState.TileType_Land) miniMap.setPixel(x, (state.mapSize - 1) - y, GetLandColour());
				if (state.map[x][y] == TactikonState.TileType_Water) miniMap.setPixel(x, (state.mapSize - 1) - y, GetSeaColour());
				if (state.map[x][y] == TactikonState.TileType_Mountain) miniMap.setPixel(x, (state.mapSize - 1) - y, GetMntColour());
				if (state.map[x][y] == TactikonState.TileType_Jungle) miniMap.setPixel(x, (state.mapSize - 1) - y, GetJungleColour());
			}
		}
		
		for (City city : state.cities)
		{
			if (state.bFogOfWar == true && fogMap[city.x][city.y] != 2) continue;
			if (city.startingCity == false)
			{
				miniMap.setPixel(city.x, (state.mapSize - 1) - city.y, cityCol);
			} else
			{
				miniMap.setPixel(city.x, (state.mapSize - 1) - city.y, startCityCol);
			}
			
			if (city.playerId != -1)
			{
				miniMap.setPixel(city.x, (state.mapSize - 1) - city.y, playerColours.get(city.playerId));
			}
		}
		
		for (Entry<Integer, IUnit>entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (state.bFogOfWar == true && fogMap[unit.GetPosition().x][unit.GetPosition().y] != 2) continue;
			if (unit.IsStealth() == true && visMap[unit.GetPosition().x][unit.GetPosition().y] != 1) continue;
			miniMap.setPixel(unit.GetPosition().x, (state.mapSize - 1) - unit.GetPosition().y, playerColours.get(unit.mUserId));
		}
		
		if (state.bFogOfWar == true)
		{
			
			for (int x = 0; x < state.mapSize; ++x)
			{
				for (int y = 0; y < state.mapSize; ++y)
				{
					if (fogMap[x][y] == 1)
					{
						int pix = miniMap.getPixel(x,  (state.mapSize - 1) -y);
						int newPix = Color.rgb(Color.red(pix)/2, Color.green(pix)/2, Color.blue(pix)/2);
						miniMap.setPixel(x, (state.mapSize - 1) -y, newPix);
					} else if (fogMap[x][y] == 0)
					{
						miniMap.setPixel(x, (state.mapSize - 1) -y,  Color.rgb(0,0,0));
					}
				}
			}
		}
		
		BitmapDrawable ret = new BitmapDrawable(miniMap);
		ret.setDither(false);
		ret.setAntiAlias(true);
		ret.setFilterBitmap(false);

		return ret;
		
	}
}
