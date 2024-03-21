package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Random;

import android.util.Log;

import Tactikon.State.Battle;
import Tactikon.State.BattleStats;
import Tactikon.State.IUnit;
import Tactikon.State.Position;

public class AttackAnimation extends AnimationItem
{
	int id;
	
	AnimationManager animMgr;// = new AnimationManager();

	RenderUnit attackerUnit;
	RenderUnit defenderUnit;
	
	int attackerTotalHealthLoss;
	int defenderTotalHealthLoss;
	
	boolean round1Hit = false;
	boolean round2Hit = false;
	boolean round3Hit = false;
	
	Position attackerPos;
	Position defenderPos;
	
	boolean bDefenderCanAttack = true;
	
	AttackAnimation(AnimationManager animMgr, RenderUnit attackingUnit, RenderUnit defendingUnit, IUnit attackerAfter, IUnit defenderAfter, AnimationAction action, BattleStats attackerStats, BattleStats defenderStats)
	{
		this.animMgr = animMgr;
		
		this.attackerUnit = attackingUnit;
		this.defenderUnit = defendingUnit;
		
		mCompleteAction = action;
		
		if (attackingUnit == null || defendingUnit == null) return;
		
		attackerPos = new Position((int)attackingUnit.x, (int)attackingUnit.y);
		defenderPos = new Position((int)defendingUnit.x, (int)defendingUnit.y);
		if (attackerAfter == null)
		{
			attackerTotalHealthLoss = attackingUnit.health;
		} else
		{
			attackerTotalHealthLoss = attackingUnit.health - attackerAfter.health;
		}
		
		if (defenderAfter == null)
		{
			defenderTotalHealthLoss = defendingUnit.health;
		} else
		{
			defenderTotalHealthLoss = defendingUnit.health - defenderAfter.health;
		}
		
		if (attackerTotalHealthLoss == 1) round2Hit = true;
		if (defenderTotalHealthLoss == 2) 
		{
			round1Hit = true;
			round3Hit = true;
		} else if (defenderTotalHealthLoss == 1)
		{
			if (attackerAfter == null)
			{
				 round1Hit = true; // must have hit in the first round if the attacker subsequently died
			} else
			{
				Random rand = new Random();
				if (rand.nextInt(100) > 50)
				{
					round1Hit = true;
				} else
				{
					round3Hit = true;
				}
			}
			
		}
		
		if (defenderStats.finalAttackScore <= 0) bDefenderCanAttack = false;

	}
	
	void ResetCarriedUnits(RenderUnit unit)
	{
		int index = 0;
		for (RenderUnit carryUnit : unit.carrying)
		{
			carryUnit.x = unit.x + (0.25f * unit.scale) - (0.5f * unit.scale * index);
			carryUnit.y = unit.y - (0.25f * unit.scale);
			ResetCarriedUnits(carryUnit);
			index++;
		}
	}
	
	float timePerRound = 1.0f;
	int currentRound = 0;
	float prevTime = 0;
	float speed = 1.4f;
	boolean bRoundOver = false;

	@Override
	boolean UpdateAnimation(float time, EffectManager fxMan)
	{
		prevTime = time;
		//animMgr.Update(timeDif);
		
		if (attackerUnit == null || defenderUnit == null) return true;
		
		int nextRound = currentRound;
		if (bRoundOver == true || currentRound == 0)
		{
			nextRound = currentRound + 1;
			if (bDefenderCanAttack == false && currentRound == 1)
			{
				nextRound = 3;
			}
			bRoundOver = false;
		}
		
		//Log.i("Anim", "Current: " + currentRound + " Next: " + nextRound);
		
		if (nextRound == 1 && currentRound != nextRound)
		{
			//Log.i("Anim", "Round 0, !");
			ArrayList<Position> attackerMove = new ArrayList<Position>();
			attackerMove.add(attackerPos); attackerMove.add(defenderPos);
			MoveAnimation moveAnim = new MoveAnimation(attackerMove, 1.1f * speed, attackerUnit, new AnimationAction() {
				@Override
				public void completeAction()
				{
					attackerUnit.carryDepth = 0;
					attackerUnit.x = attackerPos.x;
					attackerUnit.y = attackerPos.y;
					ResetCarriedUnits(attackerUnit);
					bRoundOver = true;
				} });
			animMgr.AddToQueue(moveAnim);
		}
		
		if (currentRound == 1 && currentRound != nextRound)
		{
			if (round1Hit == true)
			{
				defenderUnit.health--;
				if (defenderUnit.health == 0)
				{
					fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "large_explosion", defenderUnit.scale);
				} else
				{
					fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "small_explosion", defenderUnit.scale);
				}
			} else
			{
				fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "smoke", defenderUnit.scale);
			}
		}
		if (currentRound != nextRound && nextRound == 2)
		{
			if (defenderUnit.health > 0)
			{
				ArrayList<Position> defenderMove = new ArrayList<Position>();
				defenderMove.add(defenderPos); defenderMove.add(attackerPos);
				MoveAnimation moveAnim = new MoveAnimation(defenderMove, 1.1f * speed, defenderUnit, new AnimationAction() {
					@Override
					public void completeAction()
					{
						defenderUnit.carryDepth = 0;
						defenderUnit.x = defenderPos.x;
						defenderUnit.y = defenderPos.y;
						ResetCarriedUnits(defenderUnit);
						bRoundOver = true;
						
					} });
				animMgr.AddToQueue(moveAnim);
			} else
			{
				defenderUnit.render = false;
				return true;
			}
			
		}
		
		if (currentRound == 2 && currentRound != nextRound)
		{
			if (round2Hit == true)
			{
				attackerUnit.health--;
				if (attackerUnit.health == 0)
				{
					fxMan.AddEffect(attackerUnit.x,  attackerUnit.y,  "large_explosion", attackerUnit.scale);
				} else
				{
					fxMan.AddEffect(attackerUnit.x,  attackerUnit.y,  "small_explosion", attackerUnit.scale);
				}
			} else
			{
				fxMan.AddEffect(attackerUnit.x,  attackerUnit.y,  "smoke", attackerUnit.scale);
			}
		}
		if (currentRound != nextRound && nextRound == 3)
		{
			//Log.i("Anim", "Round 2, 3");
			if (attackerUnit.health > 0)
			{
				ArrayList<Position> attackerMove = new ArrayList<Position>();
				attackerMove.add(attackerPos); attackerMove.add(defenderPos);
				MoveAnimation moveAnim = new MoveAnimation(attackerMove, 1.1f * speed, attackerUnit, new AnimationAction() {
					@Override
					public void completeAction()
					{
						attackerUnit.carryDepth = 0;
						attackerUnit.x = attackerPos.x;
						attackerUnit.y = attackerPos.y;
						ResetCarriedUnits(attackerUnit);
						bRoundOver = true;
					} });
				animMgr.AddToQueue(moveAnim);
			} else
			{
				attackerUnit.render = false;
				return true;
			}
			
		}
		if (currentRound == 3 && currentRound != nextRound)
		{
			if (round3Hit == true)
			{
				defenderUnit.health--;
				if (defenderUnit.health == 0)
				{
					fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "large_explosion", defenderUnit.scale);
				} else
				{
					fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "small_explosion", defenderUnit.scale);
				}
			} else
			{
				fxMan.AddEffect(defenderUnit.x,  defenderUnit.y,  "smoke", defenderUnit.scale);
			}
			
			
			
			if (defenderUnit.health == 0)
			{
				defenderUnit.render = false;
				return true;
			}
		}
		
		if (time > 4.2f / speed || nextRound == 4) return true;

		currentRound = nextRound;
		return false;
	}

	@Override
	void RunCompleteAction()
	{
		if (mCompleteAction != null)
		{
			mCompleteAction.completeAction();
		}
	}

}
