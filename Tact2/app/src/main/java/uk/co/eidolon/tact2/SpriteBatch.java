package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Collection;

import Tactikon.State.Position;

import uk.co.eidolon.tact2.TextureManager.Texture;

public class SpriteBatch
{
	GLTexturedSpriteBatch renderBatch;
	
	Texture texture;
	
	float r, g, b, a, scale;
	
	
	SpriteBatch(Texture tex, float r, float g, float b, float a, float scale)
	{
		texture = tex;
		renderBatch = new GLTexturedSpriteBatch();
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		this.scale = scale;
	}
	
	public void Render(Collection<Position> sprites, float[] mProjectionMatrix, float xCamera, float yCamera)
	{
		if (sprites == null || sprites.size() == 0) return;
		
		renderBatch.SetMatrix(mProjectionMatrix, 0, 0, 0, 1, xCamera, yCamera);
		renderBatch.Start();
		for (Position sprite : sprites)
		{
			renderBatch.AddObject(texture, (float)sprite.x + 0.5f, (float)sprite.y + 0.5f, a, r, g, b, scale, false, 0, true);
		}
		
		renderBatch.Render(0);
		
	}
}
