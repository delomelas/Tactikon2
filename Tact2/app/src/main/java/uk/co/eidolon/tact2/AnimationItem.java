package uk.co.eidolon.tact2;

public abstract class AnimationItem
{
	AnimationAction mCompleteAction;
	
	abstract boolean UpdateAnimation(float time, EffectManager fxMan);
	abstract void RunCompleteAction();
	
	float animationTime = 0;
}
