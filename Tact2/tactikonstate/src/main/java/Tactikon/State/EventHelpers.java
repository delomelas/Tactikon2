package Tactikon.State;

import java.util.ArrayList;
import java.util.Map.Entry;

import Tactikon.State.IUnit.Domain;

public class EventHelpers
{
	int ComputePowerForPlayer(long playerId, TactikonState state)
	{
		int power = 0;
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId == playerId)
			{
				power = power + 1;
			}
		}
		
		for (City city : state.cities)
		{
			if (city.playerId == playerId)
			{
				power = power + 4;
			}
		}
		
		return power;
	}

	void DoPlayerEndTurnActions(TactikonState tactikonState)
	{
		// *** fortify any units in cities
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			if (unit.mFortified == true)
				continue;
			if (unit.mCarriedBy != -1)
				continue; // don't attempt to fortify carried units

			// now see if it's on a friendly city tile?
			for (City city : tactikonState.cities)
			{
				if (city.playerId != tactikonState.playerToPlay)
					continue;

				if (city.x == unit.GetPosition().x
						&& city.y == unit.GetPosition().y)
				{
					if (city.fortifiedUnits.size() < tactikonState.MAX_UNITS_IN_CITY)
					{
						city.fortifiedUnits.add(unit.mUnitId);
						unit.mFortified = true;
						break;
					}
				}
			}
		}

		// *** use a block of fuel for bombers and fighters which aren't landed
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			if (unit.mFortified == true)
				continue;
			if (unit.GetMaxFuel() == -1)
				continue;
			if (unit.mCarriedBy != -1)
				continue;
			if (unit.bMoved == true)
				continue;
			if (unit.CanLandOnLand() && tactikonState.map[unit.GetPosition().x][unit.GetPosition().y] == TactikonState.TileType_Land) continue;

			unit.fuelRemaining--;
		}

		// * Allow units to move again next turn
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			unit.bMoved = false;
			unit.bAttacked = false;
			unit.bChangedConfig = false;
		}

		// *** re-fuel any aircraft which are carried or in a city
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			if (unit.GetMaxFuel() == -1)
				continue;

			if (unit.mCarriedBy == -1 && unit.mFortified == false)
				continue;

			unit.fuelRemaining = unit.GetMaxFuel();
		}
		
		// remove bubbleinfo for this player
		ArrayList<IBubbleInfo> bubblesToDelete = new ArrayList<IBubbleInfo>();
		for (IBubbleInfo info : tactikonState.bubbles)
		{
			if (info.MessageForPlayer(tactikonState.playerToPlay) == true)
			{
				bubblesToDelete.add(info);
			}
		}

		tactikonState.bubbles.removeAll(bubblesToDelete);

		// remove bubbleinfo for this player
		ArrayList<IBubbleInfo2> bubblesToDelete2 = new ArrayList<IBubbleInfo2>();
		for (IBubbleInfo2 info : tactikonState.bubbles2)
		{
			if (info.MessageForPlayer(tactikonState.playerToPlay) == true)
			{
				bubblesToDelete2.add(info);
			}
		}

		tactikonState.bubbles2.removeAll(bubblesToDelete2);

		// *** kill any aircraft which haven't landed
		ArrayList<Integer> killList = new ArrayList<Integer>();
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			if (unit.GetMaxFuel() == -1)
				continue;

			if (unit.mCarriedBy != -1)
				continue;
			if (unit.mFortified == true)
				continue;

			if (unit.fuelRemaining <= 0)
			{
				BubbleInfoCrashedPlane bubbleInfo = new BubbleInfoCrashedPlane(unit);
				tactikonState.bubbles.add(bubbleInfo);
				BubbleInfoCrashedPlane2 bubbleInfo2 = new BubbleInfoCrashedPlane2(unit);
				tactikonState.bubbles2.add(bubbleInfo2);
				killList.add(unit.mUnitId);
			}
		}
		
		for (Integer unitToKill : killList)
		{
			tactikonState.KillUnit(unitToKill);
		}
		
		int power = ComputePowerForPlayer(tactikonState.playerToPlay, tactikonState);
		if (tactikonState.powerGraph.containsKey(tactikonState.playerToPlay) == true)
		{
			ArrayList<Integer> values = tactikonState.powerGraph.get(tactikonState.playerToPlay);
			values.add(power);
		} else
		{
			ArrayList<Integer> values = new ArrayList<Integer>();
			values.add(power);
			tactikonState.powerGraph.put(tactikonState.playerToPlay, values);
		}
		
		
	}

	void DoPlayerStartTurnActions(TactikonState tactikonState)
	{
		// remove old tracks
		ArrayList<Long> removeTrackList = new ArrayList<Long>();
		for (Entry<Long, Tracks> entry : tactikonState.tracks.entrySet())
		{
			if (entry.getValue().playerId == tactikonState.playerToPlay) removeTrackList.add(entry.getKey());
		}
		for(Long item : removeTrackList)
		{
			tactikonState.tracks.remove(item);
		}
		
		// heal fortified units
		ArrayList<City> healedCities = new ArrayList<City>();
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			if (unit.mFortified == false)
				continue;
			if (unit.health < 3)
			{
				City city = null;
				for (City fortCity : tactikonState.cities)
				{
					if (fortCity.x == unit.GetPosition().x
							&& fortCity.y == unit.GetPosition().y)
					{
						city = fortCity;
						break;
					}
				}

				if (healedCities.contains(city))
					continue; // each city can only heal one unit per turn

				if (city != null)
				{
					healedCities.add(city);
					unit.health++;
				}
			}
		}
		
		// heal planes on carriers
		
		for (Entry<Integer, IUnit> entry : tactikonState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != tactikonState.playerToPlay)
				continue;
			for (Integer carryUnitId : unit.mCarrying)
			{
				IUnit carryUnit = tactikonState.GetUnit(carryUnitId);
				if (carryUnit.GetDomain() == Domain.Air)
				{
					if (carryUnit.health < 3)
					{
						carryUnit.health ++;
						break;
					}
				}
			}
		}

		// *** Make cities produce things
		for (City city : tactikonState.cities)
		{
			//if (city.playerId == tactikonState.playerToPlay)
			city.bHasProduced = false;
			
			if (healedCities.contains(city))
				continue; // cities that have healed don't produce

			if (city.playerId == tactikonState.playerToPlay)
			{
				if (city.turnsToProduce > 0)
				{
					city.turnsToProduce--;
				}
				
				if (city.turnsToProduce == -1)
				{
					if (tactikonState.GetSequence() >= tactikonState.players.size() * 3) // only do no production messages after the first turn (or thereabouts)
					{
						BubbleInfoNoProd bubbleInfo = new BubbleInfoNoProd(city);
						tactikonState.bubbles.add(bubbleInfo);
						BubbleInfoNoProd2 bubbleInfo2 = new BubbleInfoNoProd2(city);
						tactikonState.bubbles2.add(bubbleInfo2);
					}
				}

				// can't produce a unit if there's no free fortification slot
				if (city.fortifiedUnits.size() >= tactikonState.MAX_UNITS_IN_CITY
						&& city.turnsToProduce == 0)
				{
					BubbleInfoProductionOnHold bubbleInfo = new BubbleInfoProductionOnHold(city);
					tactikonState.bubbles.add(bubbleInfo);
					BubbleInfoProductionOnHold2 bubbleInfo2 = new BubbleInfoProductionOnHold2(city);
					tactikonState.bubbles2.add(bubbleInfo2);
				}
				if (city.turnsToProduce == 0
						&& city.fortifiedUnits.size() < tactikonState.MAX_UNITS_IN_CITY)
				{
					// create a new unit
					IUnit createUnit = null;
					for (IUnit unit : tactikonState.GetUnitTypes())
					{
						if (unit.getClass().getSimpleName()
								.compareTo(city.productionType) == 0)
						{
							createUnit = unit;
						}
					}

					// we've worked out what type to make, now create and add to
					// the state
					if (createUnit != null)
					{
						createUnit.SetPosition(city.x, city.y);
						createUnit.mUserId = tactikonState.playerToPlay;

						tactikonState.CreateUnit(createUnit);

						city.turnsToProduce = createUnit.GetProductionTime(city, tactikonState);
						city.bHasProduced = true;

						BubbleInfoUnitProduced bubbleInfo = new BubbleInfoUnitProduced(createUnit);
						tactikonState.bubbles.add(bubbleInfo);
						BubbleInfoUnitProduced2 bubbleInfo2 = new BubbleInfoUnitProduced2(createUnit);
						tactikonState.bubbles2.add(bubbleInfo2);
					}
				}
			}
		}
	}

}
