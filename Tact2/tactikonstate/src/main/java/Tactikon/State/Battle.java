package Tactikon.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Battle
{
	IUnit mAttacker;
	IUnit mDefender;
	
	
	
	public ArrayList<Outcome> out = new ArrayList<Outcome>();
	
	boolean mHypothetical = false;
	
	
	public class OutcomeComparator  implements Comparator<Outcome>
	{
		@Override
		public int compare(Outcome arg0, Outcome arg1)
		{
			if (arg1.probability > arg0.probability) return 1;
			if (arg1.probability < arg0.probability) return -1;
			return 0;
		}
	}
	
	public class Outcome
	{
		public int unitId1;
		public int unitId2;
		public int unitId1Damage;
		public int unitId2Damage;
		public float probability;
		Outcome(int id1, int id2, int health1, int health2, float probability)
		{
			this.unitId1 = id1;
			this.unitId2 = id2;
			this.unitId1Damage = health1;
			this.unitId2Damage = health2;
			this.probability = probability;
		}
		
	}
	
	class Fight
	{
		ArrayList<Outcome> outcomes = new ArrayList<Outcome>();
		Fight(BattleStats stats, IUnit attacker, IUnit defender)
		{
			float total = stats.finalAttackScore + stats.finalDefenceScore;
			float hitProbability = ((float)stats.finalAttackScore)/total;
			
			outcomes.add(new Outcome(defender.mUnitId, attacker.mUnitId, 1, 0, hitProbability));
			outcomes.add(new Outcome(defender.mUnitId, attacker.mUnitId, 0, 0, (1-hitProbability)));
		}
		
		Fight(Outcome outcome, BattleStats stats, IUnit attacker, IUnit defender)
		{
			float total = stats.finalAttackScore + stats.finalDefenceScore;
			float hitProbability = ((float)stats.finalAttackScore)/total;
			
			int defHealth = defender.health;
			int attHealth = attacker.health;
			int defLoss = 0;
			int attLoss = 0;
			if (outcome.unitId1 == defender.mUnitId)
			{
				defLoss = outcome.unitId1Damage;
				attLoss = outcome.unitId2Damage;
			} else
			{
				defLoss = outcome.unitId2Damage;
				attLoss = outcome.unitId1Damage;
			}
			
			if (defHealth - defLoss <= 0)
			{
				outcomes.add(outcome);
			} else if (attHealth - attLoss <= 0)
			{
				outcomes.add(outcome); 
			} else
			{
				outcomes.add(new Outcome(defender.mUnitId, attacker.mUnitId, defLoss + 1, attLoss, outcome.probability * hitProbability));
				outcomes.add(new Outcome(defender.mUnitId, attacker.mUnitId, defLoss, attLoss, outcome.probability * (1-hitProbability)));
			}
		}
	}
	
	void GenerateBattle(TactikonState state, int attackerId, int defenderId)
	{
		mAttacker = state.GetUnit(attackerId);
		mDefender = state.GetUnit(defenderId);
		
		BattleStats roundOneBattleStats = new BattleStats(state, mAttacker, mDefender, mHypothetical);
		BattleStats roundTwoBattleStats = new BattleStats(state, mDefender, mAttacker, mHypothetical);
		BattleStats roundThreeBattleStats = new BattleStats(state, mAttacker, mDefender, mHypothetical);
		
		Fight firstAttack = new Fight(roundOneBattleStats, mAttacker, mDefender);
		
		ArrayList<Outcome> outcomes2 = new ArrayList<Outcome>();
		for (Outcome outcome : firstAttack.outcomes)
		{
			Fight fight = new Fight(outcome, roundTwoBattleStats, mDefender, mAttacker);
			outcomes2.addAll(fight.outcomes);
		}
		
		ArrayList<Outcome> outcomes3 = new ArrayList<Outcome>();
		for (Outcome outcome : outcomes2)
		{
			Fight fight =new Fight(outcome, roundThreeBattleStats, mAttacker, mDefender);
			outcomes3.addAll(fight.outcomes);
		}
		
		// find matching probabilities and collapse them
		TreeMap<Integer, Float> outcomeMap = new TreeMap<Integer, Float>();
		
		for (Outcome outcome : outcomes3)
		{
			int defLoss, attLoss;
			if (outcome.unitId1 == mDefender.mUnitId)
			{
				defLoss = outcome.unitId1Damage;
				attLoss = outcome.unitId2Damage;
			} else
			{
				defLoss = outcome.unitId2Damage;
				attLoss = outcome.unitId1Damage;
			}
			
			int key = (10 - defLoss) + (attLoss) * 100;
			//System.out.println(key);
			if (outcomeMap.containsKey(key))
			{
				float prob = outcome.probability + outcomeMap.get(key);
				outcomeMap.put(key, prob);
			} else
			{
				outcomeMap.put(key, outcome.probability);
			}
				
		}
		
		for (Entry<Integer, Float> entry : outcomeMap.entrySet())
		{
			int attLoss = ((entry.getKey() / 100));
			
			int defLoss = (10-(entry.getKey() % 100));
			//System.out.println("Key: " + entry.getKey() + " AttLoss: " + attLoss + " DefLoss: " + defLoss);
			Outcome outcome = new Outcome(mAttacker.mUnitId, mDefender.mUnitId, attLoss, defLoss, entry.getValue());
			out.add(outcome);
		}
		
		Collections.sort(out, new OutcomeComparator());

	}
	
	public Battle(TactikonState state, int attackerId, int defenderId)
	{
		GenerateBattle(state, attackerId, defenderId);
	}
	
	public Battle(TactikonState state, int attackerId, int defenderId, boolean bHypothetical)
	{
		mHypothetical = bHypothetical;
		GenerateBattle(state, attackerId, defenderId);		
	}

}
