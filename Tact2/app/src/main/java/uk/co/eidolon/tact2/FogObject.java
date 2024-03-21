package uk.co.eidolon.tact2;

import Tactikon.State.TactikonState;

public class FogObject extends RenderObject
{
	GLFogEdges fogEdges;
	
	FogObject(TactikonState state, long playerId, TextureManager texMan)
	{
		fogEdges = new GLFogEdges(state, playerId);
	}
	
	void UpdateFog(TactikonState state, long playerId)
	{
		fogEdges.UpdateFog(state, playerId);
	}
	
	@Override
	public void Render(float[] mProjectionMatrix, float xCamera, float yCamera)
	{
		fogEdges.SetMatrix(mProjectionMatrix, xPos, yPos, angle, scale, xCamera, yCamera);
		fogEdges.Render();
	}

}
