package uk.co.eidolon.tact2;

import java.util.ArrayList;

import uk.co.eidolon.tact2.TextureManager.Texture;

public class EffectManager
{
	TextureManager mTexMan;
	
	GLFlatTexturedAnimated renderContext;
	
	class Effect
	{
		float xPos;
		float yPos;
		Texture tex;
		float time;
		int frame;
		float scale;
	}
	
	ArrayList<Effect> effects = new ArrayList<Effect>();
	
	EffectManager(TextureManager texMan)
	{
		mTexMan = texMan;
		
		renderContext = new GLFlatTexturedAnimated();
	}

	void AddEffect(float xPos, float yPos, String texName, float scale)
	{
		Effect effect = new Effect();
		effect.xPos = xPos;
		effect.yPos = yPos;
		effect.time = 0;
		effect.tex = mTexMan.GetTexture(texName);
		effect.scale = scale;
		
		effects.add(effect);
	}
	
	void RenderEffects(float frameTime, float[] mProjectionMatrix, float xCam, float yCam)
	{
		renderContext.SetMatrix(mProjectionMatrix, 0, 0, 0, 1, xCam, yCam);
		
		ArrayList<Effect> deleteList = new ArrayList<Effect>();
		for (Effect effect : effects)
		{
			effect.time = effect.time + frameTime;
			effect.frame = (int)Math.round(effect.time / ((double)effect.tex.frameRate / 1000));
			
			if (effect.frame >= effect.tex.frames) 
			{
				deleteList.add(effect);
			} else
			{
				RenderEffect(effect, mProjectionMatrix, xCam, yCam);
			}
		}
		effects.removeAll(deleteList);
	}
	
	void RenderEffect(Effect effect, float[] projectionMatrix, float xCam, float yCam)
	{
		renderContext.SetMatrix(projectionMatrix, effect.xPos, effect.yPos, 0, effect.scale, xCam, yCam);
		renderContext.RenderAnimation(effect.tex, effect.frame);
	}

}
