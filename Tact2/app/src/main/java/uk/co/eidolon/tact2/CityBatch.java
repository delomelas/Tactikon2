package uk.co.eidolon.tact2;

import java.util.ArrayList;
import uk.co.eidolon.tact2.TextureManager.Texture;
import uk.co.eidolon.tact2.TextureManager.UnitDefinition;
import Core.PlayerInfo;
import Tactikon.State.City;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;

public class CityBatch
{
	GLTexturedSpriteBatch renderBatch;
	
	ArrayList<City> cities;
	
	Texture baseTex;
	Texture colourTex;
	Texture starTex;
	Texture colourLightsTex;
	Texture baseLightsTex;
	Texture abandonedTex;
	Texture spannerTex;
	
	int userId;
	
	CityBatch(TextureManager texManager, int userId)
	{
		UnitDefinition unitDefinition = texManager.GetUnitDefinition("City", 0);
		UnitDefinition unitDefLights = texManager.GetUnitDefinition("CityLights", 0);
		UnitDefinition unitDefAbandoned = texManager.GetUnitDefinition("CityAbandoned", 0);
		renderBatch = new GLTexturedSpriteBatch();
		baseTex = unitDefinition.baseTexture;
		colourTex = unitDefinition.colourTexture;
		baseLightsTex = unitDefLights.baseTexture;
		colourLightsTex = unitDefLights.colourTexture;
		abandonedTex = unitDefAbandoned.baseTexture;
		starTex = texManager.GetTexture("silver_star");
		spannerTex = texManager.GetTexture("spanner");
		this.userId = userId;
	}
	

	public void Render(TactikonState state, ArrayList<City> cities, float[] mProjectionMatrix, float xCamera, float yCamera)
	{
		renderBatch.SetMatrix(mProjectionMatrix, 0, 0, 0, 1, xCamera, yCamera);
		renderBatch.Start();
		
		byte[][] fogMap = null;
		if (state.bFogOfWar == true)
		{
			fogMap = state.GetResolvedFogMap(userId);
		}
		
		for (City city : cities)
		{
			if (state.bFogOfWar == true && fogMap[city.x][city.y] == 0) continue;
			
			Texture tex = null;
			if (city.playerId == -1)
			{
				tex = abandonedTex;
			} else if (city.fortifiedUnits.size() == 0)
			{
				tex = baseTex;
			} else
			{
				tex = baseLightsTex;
			}
			
			renderBatch.AddObject(tex, (float)city.x + 0.5f, (float)city.y + 0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, true, 0, true);
			
		}
		for (City city : cities)
		{
			if (state.bFogOfWar == true && fogMap[city.x][city.y] == 0) continue;
			float r, g, b;
			r = 1f;
			g = 1f;
			b = 1f;
			if (city.playerId != -1)
			{
				PlayerInfo info = state.GetPlayerInfo(city.playerId);
				r = (float)info.r / 256f;
				g = (float)info.g / 256f;
				b = (float)info.b / 256f;
			}
			
			Texture tex = null;
			if (city.playerId == -1)
			{
				tex = abandonedTex;
			} else if (city.fortifiedUnits.size() == 0)
			{
				tex = colourTex;
			} else
			{
				tex = colourLightsTex;
			}
			
			renderBatch.AddObject(tex, (float)city.x + 0.5f, (float)city.y + 0.5f, 1.0f, r, g, b, 1.0f, true, 0, true);
			
		}
		
		for (City city : cities)
		{
			if (state.bFogOfWar == true && fogMap[city.x][city.y] == 0) continue;
			if (city.bIsHQ == true)
				renderBatch.AddObject(starTex, (float)city.x + 0.8f, (float)city.y + 0.8f, 1.0f, 1.0f, 1.0f, 1.0f, 0.5f, false, 1, true);
			
			boolean bRepairing = false;
			for (int unitId : city.fortifiedUnits)
			{
				IUnit unit = state.GetUnit(unitId);
				if (unit.health < 3) bRepairing = true;
			}
			if (city.playerId == userId && bRepairing == true)
			{
				renderBatch.AddObject(spannerTex, (float)city.x + 0.7f, (float)city.y + 0.35f, 1.0f, 1.0f, 1.0f, 1.0f, 0.8f, false, 1, true);
			}
		}
		
		renderBatch.Render(0);
		
	}
}
