package uk.co.eidolon.tact2;

import Tactikon.State.TactikonState;

public class MapObject extends RenderObject
{
	GLMap landObject;
	GLWaterMap waterObject;
	GLMap portObject;
	GLMapLines mapLinesObject;
	GLMapCoast mapCoastObject;
	GLMapBorder mapBorder;
	GLMap mountainObject;
	GLMap jungleObject;
	GLMapCoast mapMountainEdgeObject;
	GLMapCoast mapMountainEdgeCoastObject;
	GLMapCoast mapMountainEdgeJungleObject;
	GLMapCoast mapJungleEdgeLandObject;
	GLMapCoast mapJungleEdgeWaterObject;
	GLClouds cloudObject;
	
	MapObject(TactikonState state, TextureManager texMan)
	{
		landObject = new GLMap(state,
			texMan.GetTexture("grass2"),
			texMan.GetTexture("grass1"),
			TactikonState.TileType_Land,
			state.seaLevel,
			0.8f, new RangeFunctor()
			{
				public float offsetRange(float value)
				{
					if (value > 1.2f) value = 1.2f;
					return Math.max(0.0f, value - 0.2f);
				}
	        });
		
		waterObject = new GLWaterMap(state,
			texMan.GetTexture("water2"),
			texMan.GetTexture("water1"),
			TactikonState.TileType_Water,
			0,
			state.seaLevel,
			new RangeFunctor()
			{
				public float offsetRange(float value)
				{
					return value*value;
				}
			});
		mountainObject = new GLMap(state,
				texMan.GetTexture("mountains"),
				texMan.GetTexture("mountains"),
				TactikonState.TileType_Mountain,
				0,
				0.8f,
				new RangeFunctor()
				{
					public float offsetRange(float value)
					{
						return value;
					}
				});
		
		jungleObject = new GLMap(state,
				texMan.GetTexture("jungle"),
				texMan.GetTexture("jungle"),
				TactikonState.TileType_Jungle,
				0,
				0.8f,
				new RangeFunctor()
				{
					public float offsetRange(float value)
					{
						return value;
					}
				});
		
		portObject = new GLMap(state,
				texMan.GetTexture("grass2"),
				texMan.GetTexture("grass1"),
				TactikonState.TileType_Port,
				state.seaLevel,
				1.0f,
				new RangeFunctor()
				{
					public float offsetRange(float value)
					{
						return Math.max(0.0f, value - 0.2f);
					}
				});
		
		mapCoastObject = new GLMapCoast(state, texMan.GetTexture("coast_horizontal"), texMan.GetTexture("coast_vertical"), texMan.GetTexture("coast_corners"), TactikonState.TileType_Land, TactikonState.TileType_Water);
		mapMountainEdgeObject = new GLMapCoast(state, texMan.GetTexture("mountains_horizontal"), texMan.GetTexture("mountains_vertical"), texMan.GetTexture("mountains_corners"), TactikonState.TileType_Mountain, TactikonState.TileType_Land);
		mapMountainEdgeJungleObject = new GLMapCoast(state, texMan.GetTexture("mountains_horizontal"), texMan.GetTexture("mountains_vertical"), texMan.GetTexture("mountains_corners"), TactikonState.TileType_Mountain, TactikonState.TileType_Land);
		mapMountainEdgeCoastObject = new GLMapCoast(state, texMan.GetTexture("mountains_horizontal"), texMan.GetTexture("mountains_vertical"), texMan.GetTexture("mountains_corners"), TactikonState.TileType_Mountain, TactikonState.TileType_Jungle);
		mapJungleEdgeLandObject = new GLMapCoast(state, texMan.GetTexture("jungle_horizontal"), texMan.GetTexture("jungle_vertical"), texMan.GetTexture("jungle_corners"), TactikonState.TileType_Jungle, TactikonState.TileType_Land);
		mapJungleEdgeWaterObject = new GLMapCoast(state, texMan.GetTexture("jungle_horizontal"), texMan.GetTexture("jungle_vertical"), texMan.GetTexture("jungle_corners"), TactikonState.TileType_Jungle, TactikonState.TileType_Water);
		
		mapLinesObject = new GLMapLines(state);
		
		mapBorder = new GLMapBorder(state);
		
		cloudObject = new GLClouds(state, texMan);
	}
	
	float mTime;
	
	public void SetTime(float time)
	{
		mTime = time / 100f;
	}
	
	@Override
	public void Render(float[] projectionMatrix, float xCamera, float yCamera)
	{
		Render(projectionMatrix, xCamera, yCamera, false);
	}
	
	public void Render(float[] projectionMatrix, float xCamera, float yCamera, boolean bSimpleGraphics)
	{
		landObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		landObject.Render();
		
		waterObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		
		if (bSimpleGraphics == false)
		{
			waterObject.Render(mTime);
		} else
		{
			waterObject.Render(0);
		}
		
		portObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		portObject.Render();
		
		mapCoastObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapCoastObject.Render();
		
		mountainObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mountainObject.Render();
		
		mapMountainEdgeObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapMountainEdgeObject.Render();
		
		jungleObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		jungleObject.Render();
		
		mapMountainEdgeCoastObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapMountainEdgeCoastObject.Render();
		
		mapMountainEdgeJungleObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapMountainEdgeJungleObject.Render();
		
		mapJungleEdgeLandObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapJungleEdgeLandObject.Render();
		
		mapJungleEdgeWaterObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapJungleEdgeWaterObject.Render();
		
		mapLinesObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapLinesObject.Render();
		
		if (bSimpleGraphics == false)
		{
			cloudObject.UpdateClouds(mTime);
			cloudObject.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
			cloudObject.Render();
		}
		
		mapBorder.SetMatrix(projectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		mapBorder.Render();
		
	}
}
