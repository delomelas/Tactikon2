package uk.co.eidolon.tact2;

public abstract class RenderObject
{
	float xPos, yPos;
	float scale = 1;
	float angle = 0;
	
	public abstract void Render(float[] mProjectionMatrix, float xCamera, float yCamera);
	
}
