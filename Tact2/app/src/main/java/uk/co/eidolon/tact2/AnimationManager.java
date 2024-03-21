package uk.co.eidolon.tact2;

import java.util.ArrayList;

public class AnimationManager
{
	ArrayList<AnimationItem> animItems = new ArrayList<AnimationItem>();
	
	EffectManager mFxMan;
	
	AnimationManager(EffectManager fxMan)
	{
		mFxMan = fxMan;
	}
	
	void AddToQueue(AnimationItem item)
	{
		animItems.add(item);
	}
	
	void Update(float timeDif)
	{
		ArrayList<AnimationItem> list = (ArrayList<AnimationItem>) animItems.clone();
		ArrayList<AnimationItem> removeList = new ArrayList<AnimationItem>();
		for (AnimationItem item : list)
		{
			item.animationTime += timeDif;
			boolean bFinished = item.UpdateAnimation(item.animationTime, mFxMan);
			if (bFinished)
			{
				item.RunCompleteAction();
				removeList.add(item);
			}
		}
		
		animItems.removeAll(removeList);
		
		
	}
}
