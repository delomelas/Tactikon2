package uk.co.eidolon.tact2;

import java.util.ArrayList;

import android.util.Log;

import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.Tracks;

public class MoveAnimation extends AnimationItem
{
	ArrayList<Position> mRoute;
	float mSpeed;
	RenderUnit mUnit;
	
	// speed is number of tiles moved per second
	MoveAnimation(ArrayList<Position> route, float speed, RenderUnit unit, AnimationAction completeAction)
	{
		mRoute = route;
		mSpeed = speed;
		mUnit = unit;
		mCompleteAction = completeAction;
		
		if (mUnit == null) return;
		
		mUnit.scale = 1.0f;
	}
	
	public void RunCompleteAction()
	{
		if (mCompleteAction != null)
			mCompleteAction.completeAction();
	}
	
	public void UpdateCarried(RenderUnit unit)
	{
		int index = 0;
		for (RenderUnit carryUnit : unit.carrying)
		{
			carryUnit.x = unit.x + (0.25f * unit.scale) - (0.5f * unit.scale * index);
			carryUnit.y = unit.y - (0.25f * unit.scale);
			carryUnit.bAnimating = unit.bAnimating;
			UpdateCarried(carryUnit);
			index++;
		}
	}
	
	
	// returns true if the animation hasn't finished
	@Override
	boolean UpdateAnimation(float time, EffectManager fxMan)
	{
		boolean bFinished = false;
		float animationPosition = time * mSpeed;
		if (mUnit == null) return true;
		if (mUnit == null || mRoute == null || animationPosition >= mRoute.size() - 1.001f)
		{ 
			mUnit.bAnimating = false;
			if (mUnit.tracks != null) mUnit.tracks.renderPoints = mRoute.size();
			UpdateCarried(mUnit);
			return true;
		}
		
		int beforeRouteItem = (int)Math.floor(animationPosition);
		int afterRouteItem = (int)Math.floor(animationPosition) + 1;
		float fraction = animationPosition - beforeRouteItem;
		
		if (mUnit.tracks != null)
		{
			int pathItem = (int)Math.floor(animationPosition + 0.5f) + 1;
			if (pathItem > mRoute.size()) pathItem = mRoute.size();
			mUnit.tracks.renderPoints = pathItem;
			//System.out.println("UnitId: " + mUnit.unitId + "Setting Point: " + pathItem);
		}
		
		
		float x = (float)mRoute.get(beforeRouteItem).x * (1-fraction) + (float)mRoute.get(afterRouteItem).x * (fraction);
		float y = (float)mRoute.get(beforeRouteItem).y * (1-fraction) + (float)mRoute.get(afterRouteItem).y * (fraction);
		mUnit.x = x;
		mUnit.y = y;
		mUnit.bAnimating = true;
		
		UpdateCarried(mUnit);
		
		return bFinished;
	}
	
}
