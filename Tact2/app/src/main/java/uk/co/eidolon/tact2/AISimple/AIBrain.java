package uk.co.eidolon.tact2.AISimple;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;

import Core.EventInjector;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitTank;

public class AIBrain
{
	public class UnitTypeEvaluator
	{
		public Map<IAIEvaluator, Float> myTerritoryEvaluation = new TreeMap<IAIEvaluator, Float>();
		public Map<IAIEvaluator, Float> disputedTerritoryunitEvaluation = new TreeMap<IAIEvaluator, Float>();
		public Map<IAIEvaluator, Float> enemyTerritoryunitEvaluation = new TreeMap<IAIEvaluator, Float>();
	}
	
	public Map<String, UnitTypeEvaluator> evaluators = new TreeMap<String, UnitTypeEvaluator>();
	
	public Map<String, Float> unitAggressiveness = new TreeMap<String, Float>();
	
	public Map<String, Float> lowBuildRatio = new TreeMap<String, Float>();
	public Map<String, Float> highBuildRatio = new TreeMap<String, Float>();
	public float numUnitsConsideredHigh = 20;
	
	void AddEvaluators(Map<IAIEvaluator, Float> evaluator, EventInjector injector)
	{
		evaluator.put(new MoveToCaptureCityNoDanger(injector), 100.0f);
		evaluator.put(new MoveToAttackClosestEnemy(injector), 200.0f);
		evaluator.put(new MoveToSafety(injector), 50.0f);
		evaluator.put(new BoardTransport(injector), 10.0f);
		evaluator.put(new PickupUnit(injector), 100.0f);
		evaluator.put(new Refuel(injector), 150.0f);
		evaluator.put(new DeliverUnitToCaptureCity(injector), 100.0f);
		evaluator.put(new DeliverUnitToFriendlyCity(injector), 60.0f);
		evaluator.put(new DeliverUnitToCaptureEnemyCity(injector), 20.0f);
		evaluator.put(new MoveToFriendlyCity(injector), 30.0f);
		evaluator.put(new DefendCity(injector), 30.0f);
	}
	
	
	int InitEvaluators(float[] data, int index)
	{
		IAIEvaluator defendCity = null;
		IAIEvaluator moveToSafety = null;
		for (Entry<String, UnitTypeEvaluator> entry1 : evaluators.entrySet())
		{
			UnitTypeEvaluator eval = entry1.getValue();
			for (Entry<IAIEvaluator, Float> entry2 : eval.disputedTerritoryunitEvaluation.entrySet())
			{
				eval.disputedTerritoryunitEvaluation.put(entry2.getKey(), data[index++]);
			}
			for (Entry<IAIEvaluator, Float> entry2 : eval.myTerritoryEvaluation.entrySet())
			{
				eval.myTerritoryEvaluation.put(entry2.getKey(), data[index++]);
				if (entry2.getKey() instanceof DefendCity) defendCity = entry2.getKey();
				if (entry2.getKey() instanceof MoveToSafety) moveToSafety = entry2.getKey();
			}
			for (Entry<IAIEvaluator, Float> entry2 : eval.enemyTerritoryunitEvaluation.entrySet())
			{
				eval.enemyTerritoryunitEvaluation.put(entry2.getKey(), data[index++]);
			}
			
			/*
			// hack!
			if (entry1.getKey().compareTo(UnitTank.class.getSimpleName()) == 0)
			{
				float safetyScore = eval.myTerritoryEvaluation.get(moveToSafety);
				float defendScore = eval.myTerritoryEvaluation.get(defendCity);
				if (defendScore < safetyScore) eval.myTerritoryEvaluation.put(defendCity, safetyScore + 10);
			}
			*/
			
		}
		return index;
	}
	
	public void OutputData()
	{
		TactikonState state = new TactikonState();
		for (IUnit unitType : state.GetUnitTypes())
		{
			String unitName = unitType.getClass().getSimpleName();
			System.out.println("Unit: " + unitType.GetName());
			System.out.println("Agressiveness: " + unitAggressiveness.get(unitName) + " Low build ratio: " + lowBuildRatio.get(unitName) + " High build ratio: " + highBuildRatio.get(unitName));
			
			UnitTypeEvaluator eval = evaluators.get(unitName);
			System.out.print("Disputed territory evaluators: ");
			for (Entry<IAIEvaluator, Float> entry : eval.disputedTerritoryunitEvaluation.entrySet())
			{
				System.out.print(entry.getKey().getClass().getSimpleName() + ": " + entry.getValue() + "  ");
			}
			System.out.print("\r\n");
			
			System.out.print("My territory evaluators: ");
			for (Entry<IAIEvaluator, Float> entry : eval.myTerritoryEvaluation.entrySet())
			{
				System.out.print(entry.getKey().getClass().getSimpleName() + ": " + entry.getValue() + "  ");
			}
			System.out.print("\r\n");
			
			System.out.print("Enemy territory evaluators: ");
			for (Entry<IAIEvaluator, Float> entry : eval.enemyTerritoryunitEvaluation.entrySet())
			{
				System.out.print(entry.getKey().getClass().getSimpleName() + ": " + entry.getValue() + "  ");
			}
			System.out.print("\r\n");
		}
	}
	
	int DumpEvaluators(float[] data, int index)
	{
		for (Entry<String, UnitTypeEvaluator> entry1 : evaluators.entrySet())
		{
			UnitTypeEvaluator eval = entry1.getValue();
			for (Entry<IAIEvaluator, Float> entry2 : eval.disputedTerritoryunitEvaluation.entrySet())
			{
				float val = eval.disputedTerritoryunitEvaluation.get(entry2.getKey());
				data[index++] = val;
				
			}
			for (Entry<IAIEvaluator, Float> entry2 : eval.myTerritoryEvaluation.entrySet())
			{
				float val = eval.myTerritoryEvaluation.get(entry2.getKey());
				data[index++] = val;
			}
			for (Entry<IAIEvaluator, Float> entry2 : eval.enemyTerritoryunitEvaluation.entrySet())
			{
				float val = eval.enemyTerritoryunitEvaluation.get(entry2.getKey());
				data[index++] = val;
			}
		}
		
		return index;
	}
	
	public AIBrain(EventInjector injector)
	{
		TactikonState state = new TactikonState();
		
		numUnitsConsideredHigh = 10;
		
		for (IUnit unitType : state.GetUnitTypes())
		{
			unitAggressiveness.put(unitType.getClass().getSimpleName(), 50.0f);
			lowBuildRatio.put(unitType.getClass().getSimpleName(), 2.0f);
			highBuildRatio.put(unitType.getClass().getSimpleName(), 2.0f);
			
			UnitTypeEvaluator evals = new UnitTypeEvaluator();
			AddEvaluators(evals.disputedTerritoryunitEvaluation, injector);
			AddEvaluators(evals.enemyTerritoryunitEvaluation, injector);
			AddEvaluators(evals.myTerritoryEvaluation, injector);
			
			evaluators.put(unitType.getClass().getSimpleName(), evals);
		}
	}
	
	public void InitialiseBrain(float[] dna)
	{
		int index = 0;
		TactikonState state = new TactikonState();
		ArrayList<IUnit> unitTypes = state.GetUnitTypes();
		
		for (IUnit unitType : unitTypes)
		{
			unitAggressiveness.put(unitType.getClass().getSimpleName(), dna[index++]);
			lowBuildRatio.put(unitType.getClass().getSimpleName(), dna[index++]);
			highBuildRatio.put(unitType.getClass().getSimpleName(), dna[index++]);
		}
		
		index = InitEvaluators(dna, index);
		
		numUnitsConsideredHigh = dna[index++];
	}
	
	public int DumpBrain(float[] dna)
	{
		int index = 0;
		TactikonState state = new TactikonState();
		ArrayList<IUnit> unitTypes = state.GetUnitTypes();
		
		for (IUnit unitType : unitTypes)
		{
			float val = unitAggressiveness.get(unitType.getClass().getSimpleName());
			dna[index++] = val;
			
			val = lowBuildRatio.get(unitType.getClass().getSimpleName());
			dna[index++] = val;
			
			val = highBuildRatio.get(unitType.getClass().getSimpleName());
			dna[index++] = val;
		}
		
		index = DumpEvaluators(dna, index);
		
		dna[index++] = numUnitsConsideredHigh;
		
		return index;
	}
	
	
	float GetScoreForUnitAndEvaluator(int[][] territoryMap, IUnit unit, IAIEvaluator evaluator)
	{
		int tVal = territoryMap[unit.GetPosition().x][unit.GetPosition().y];
		UnitTypeEvaluator eval = evaluators.get(unit.getClass().getSimpleName());
		if (tVal == 0) return eval.myTerritoryEvaluation.get(evaluator);
		if (tVal == 1) return eval.disputedTerritoryunitEvaluation.get(evaluator);
		return eval.enemyTerritoryunitEvaluation.get(evaluator);
	}
}
