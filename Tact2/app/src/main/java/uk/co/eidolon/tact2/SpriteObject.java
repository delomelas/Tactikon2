package uk.co.eidolon.tact2;

import uk.co.eidolon.tact2.TextureManager.Texture;

public class SpriteObject extends RenderObject
{
	GLFlatTextured sprite;
	
	String mTextureName;
	
	SpriteObject(TextureManager texManager, String textureName, float alpha)
	{
		Texture texture = texManager.GetTexture(textureName);
		sprite = new GLFlatTextured(texture, 1.0f, 1.0f, 1.0f, alpha, false);
		
	}
	
	SpriteObject(TextureManager texManager, String textureName, float r, float g, float b, float alpha)
	{
		Texture texture = texManager.GetTexture(textureName);
		sprite = new GLFlatTextured(texture, r, g, b, alpha, false);
		
	}
	
	@Override
	public void Render(float[] mProjectionMatrix, float xCamera, float yCamera)
	{
		sprite.SetMatrix(mProjectionMatrix, xPos + 0.5f, yPos + 0.5f, angle, scale, xCamera, yCamera);
		sprite.Render();
	}
		
}
