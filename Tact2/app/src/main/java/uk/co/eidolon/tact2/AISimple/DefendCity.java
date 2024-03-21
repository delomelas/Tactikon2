package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import Core.EventInjector;
import Core.IEvent;
import Tactikon.State.City;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitHelicopter;

public class DefendCity extends IAIEvaluator
{
	public DefendCity(EventInjector injector)
	{
		super(injector);
	}

	int CalcDangerScore(IUnit unit, City inCity, TactikonState state, AIInfo info)
	{
		int score = 0;
		if (inCity.fortifiedUnits.size() < 1) score = score + 50;
		 
		if (info.dangerMap[inCity.x][inCity.y] > 0) score = score + 50;
		
		// now to decide if we're on the front lines...
		// if the closest city is an enemy city, that's an easy one
		int closestEnemyCity = 999;
		int closestFriendlyCity = 999;
		for (City city : state.cities)
		{
			int dist = (Math.abs(city.x - inCity.x) + Math.abs(city.y - inCity.y));
			if (city.playerId != -1 && city.playerId != unit.mUserId)
			{
				if (dist < closestEnemyCity)
				{
					closestEnemyCity = dist;
				}
			}
			if ((city.playerId == unit.mUserId || city.playerId == -1) && city != inCity)
			{
				if (dist < closestFriendlyCity)
				{
					closestFriendlyCity = dist;
				}
			}
		}
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit enemy = entry.getValue();
			if (enemy.mUserId == unit.mUserId) continue;
			if (enemy.CanCaptureCity() )
			{
				int dist = enemy.GetMovementDistance();
				if (enemy.mCarriedBy != -1)
				{
					IUnit carryUnit = state.GetUnit(enemy.mCarriedBy);
					dist = dist + carryUnit.GetMovementDistance();
				}
				
				if (Math.abs(enemy.GetPosition().x - inCity.x) + Math.abs(enemy.GetPosition().y - inCity.y) <= dist)
				{
					score = score + 50;
				}
			}
		}
		
		if (closestEnemyCity < closestFriendlyCity) score = score + 50;
		
		if (score > 150) score = 150;
		
		return score;
	}
	
	@Override
	public Task EvaluateTask(AIInfo info, PathFinder pathFinder, IUnit unit,
			TactikonState state, TaskList taskList, AIBrain brain)
	{
		// if we're in a city, and we're the only unit there...
		City inCity = null;
		for (City city : state.cities)
		{
			if (unit.GetPosition().x == city.x && unit.GetPosition().y == city.y)
			{
				inCity = city;
				break;
			}
		}
		
		if (inCity == null) return null;
		
		int score = CalcDangerScore(unit, inCity, state, info);
		
		if (score == 0) return null;
		
		
		Task result = new Task();
		
		result.myUnitId = unit.mUnitId;
		result.targetCityId = inCity.cityId;
		result.evaluator = this;
		result.score = score;
		result.turns = 1;
		result.route = null;
		result.targetPosition = null;
			
		return result;
	}

	@Override
	public Task CheckTask(TactikonState state, Task task,
			PathFinder pathFinder, AIInfo info, TaskList taskList)
	{
		IUnit unit = state.GetUnit(task.myUnitId);
		City inCity = state.GetCity(task.targetCityId);
		if (unit == null) return null;
		
		int score = CalcDangerScore(unit, inCity, state, info);
		if (score != task.score) return null; 
		
		return task;
	}

	@Override
	public void ActionTask(EventInjector injector, TactikonState state, Task task)
	{
		// we're just staying in the same place, please :)
		task.actioned = true;
	}

}
