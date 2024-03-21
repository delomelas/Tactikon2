package uk.co.eidolon.tact2;

import java.util.ArrayList;

import uk.co.eidolon.tact2.TextureManager.Texture;


public class UnitBatch
{
	GLTexturedSpriteBatch renderBatch;
	
	Texture healthPip1;
	Texture healthPip2;
	Texture healthPip3;
	Texture killsPip1;
	Texture killsPip2;
	Texture killsPip3;
	TextureManager texMan;
	Texture veteranBadge;
	
	UnitBatch(TextureManager texManager)
	{
		texMan = texManager;
		
		renderBatch = new GLTexturedSpriteBatch();
		
		healthPip1 = texManager.GetTexture("healthpip1");
		healthPip2 = texManager.GetTexture("healthpip2");
		healthPip3 = texManager.GetTexture("healthpip3");
		killsPip1 = texManager.GetTexture("killspip1");
		killsPip2 = texManager.GetTexture("killspip2");
		killsPip3 = texManager.GetTexture("killspip3");
		veteranBadge = texManager.GetTexture("veteran");
	}
	
	public void Render(ArrayList<RenderUnit> units, float[] mProjectionMatrix, float xCamera, float yCamera, float frameTime)
	{
		renderBatch.SetMatrix(mProjectionMatrix, 0, 0, 0, 1, xCamera, yCamera);
		renderBatch.Start();
		
		for (RenderUnit renderUnit : units)
		{
			if (renderUnit.render == false) continue;
			
			float xPos = renderUnit.x;
			float yPos = renderUnit.y;
			float scale = renderUnit.scale;
			int health = renderUnit.health;
			boolean veteran = renderUnit.veteran;
			
			float r, g, b;
			r = renderUnit.r;
			g = renderUnit.g;
			b = renderUnit.b;
			
			if (renderUnit.bHasAction == false)
			{
				r = r / 2;
				g = g / 2;
				b = b / 2;
			}
			
			int renderPass = (renderUnit.carryDepth * 4);
			if (renderUnit.bAnimating == true) renderPass = renderPass + 10;
			
			if (renderUnit.shadowTexture != null)
			{
				renderBatch.AddObject(renderUnit.shadowTexture, xPos + 0.55f, yPos + 0.45f, 0.25f, 0, 0, 0, scale, false, renderPass, renderUnit.bShowAnimation); // shadow
			} else
			{
				renderBatch.AddObject(renderUnit.baseTexture, xPos + 0.55f, yPos + 0.45f, 0.25f, 0, 0, 0, scale, false, renderPass, renderUnit.bShowAnimation); // shadow
			}
			renderBatch.AddObject(renderUnit.baseTexture, xPos + 0.5f, yPos + 0.5f, 1,1,1,1,scale, false, renderPass + 1, renderUnit.bShowAnimation); // base
			renderBatch.AddObject(renderUnit.colourTexture, xPos + 0.5f, yPos + 0.5f, 1, r, g, b, scale, false, renderPass+2, renderUnit.bShowAnimation); // colour
			
			if (health == 1)
			{
				renderBatch.AddObject(healthPip1, xPos + 0.5f, yPos + 0.5f, 1, 0.9f, 0.1f, 0.1f, scale, false, renderPass+3, false);
			} else if (health == 2)
			{
				renderBatch.AddObject(healthPip2, xPos + 0.5f, yPos + 0.5f, 1, 0.9f, 0.9f, 0.1f, scale, false, renderPass+3, false);
			} else if (health == 3)
			{
				renderBatch.AddObject(healthPip3, xPos + 0.5f, yPos + 0.5f, 1, 0.1f, 0.9f, 0.1f, scale, false, renderPass+3, false);
			}
			
			if (renderUnit.kills == 1)
			{
				renderBatch.AddObject(killsPip1, xPos + 0.5f, yPos + 0.5f, 1, 0.3f, 0.3f, 0.9f, scale, false, renderPass+3, false);
			} else if (renderUnit.kills == 2)
			{
				renderBatch.AddObject(killsPip2, xPos + 0.5f, yPos + 0.5f, 1, 0.3f, 0.3f, 0.9f, scale, false, renderPass+3, false);
			} else if (renderUnit.kills == 3)
			{
				renderBatch.AddObject(killsPip3, xPos + 0.5f, yPos + 0.5f, 1, 0.3f, 0.3f, 0.9f, scale, false, renderPass+3, false);
			}
			
			if (veteran == true)
			{
				renderBatch.AddObject(veteranBadge, xPos + 0.5f + (0.35f * scale), yPos + 0.5f + (0.3f * scale), 1,1,1,1, scale * 0.5f, false, renderPass+3, false);
			}
		}
		
		renderBatch.Render(frameTime);
	}
}
