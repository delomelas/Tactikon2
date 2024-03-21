package Tactikon.State;

import java.util.ArrayList;


	public class BattleStats
	{
		int baseAttackScore;
		int baseDefenceScore;
		public int finalAttackScore;
		public int finalDefenceScore;
		ArrayList<IBattleBonus> attackBonuses = new ArrayList<IBattleBonus>();
		ArrayList<IBattleBonus> defenceBonuses = new ArrayList<IBattleBonus>();
		public BattleStats(TactikonState state, IUnit attacker, IUnit defender, boolean bHypothetical)
		{
			baseAttackScore = attacker.GetAttack(defender, state);
			baseDefenceScore = defender.GetDefence(attacker, state);
			
			attackBonuses = attacker.GetBonuses(state);
			defenceBonuses = defender.GetBonuses(state);
					
			finalAttackScore = baseAttackScore;
			finalDefenceScore = baseDefenceScore;
			for (IBattleBonus bonus : attackBonuses)
			{
				finalAttackScore += bonus.mAtkValue;
			}
			for (IBattleBonus bonus : defenceBonuses)
			{
				finalDefenceScore += bonus.mDefValue;
			}
			if (finalDefenceScore < 0)
			{
				finalDefenceScore = 0;
			}
			if (finalAttackScore < 0)
			{
				finalAttackScore = 0;
			}
			
			if (bHypothetical == false)
			{
				// 		check distances
				int dist = Math.abs(attacker.GetPosition().x - defender.GetPosition().x) + Math.abs(attacker.GetPosition().y - defender.GetPosition().y);
				if (dist > attacker.GetAttackRange())
				{
					baseAttackScore = 0;
					finalAttackScore = 0; // can't attack if out of range
				}
			}
		}
	}
