package uk.co.eidolon.tact2;


public class RenderSprite
{
	RenderSprite(float x, float y, float scale, float r, float g, float b, float a)
	{
		this.x = x;
		this.y = y;
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		this.scale = scale;
		
	}
	float x, y;
	float scale;
	float r, g, b,a;
}
