package Tactikon.State;

import java.util.ArrayList;

import Tactikon.State.IUnit.Domain;

public class UnitHelicopter extends IUnit
{

	@Override
	public int GetMovementDistance()
	{
		return 6;
	}
	
	@Override
	public byte GetMoveCost(Position from, Position to, TactikonState state)
	{
		return 1;
	}

	@Override
	public boolean UnitCanMove(Position from, Position to, TactikonState state)
	{
		int fromType = state.map[from.y][from.y];
		int toType = state.map[to.x][to.y];
		
		if (mCarriedBy != -1) fromType = toType;
		
		if (ValidMoveTile(fromType)  && ValidMoveTile(toType)) return true;
		
		return false;
	}
	
	@Override
	public boolean ValidMoveTile(int type)
	{
		return true;
	}
	
	@Override
	public int GetUnitProductionTime()
	{
		return 4;
	}

	@Override
	public String GetName()
	{
		return "Chopper";
	}
	
	@Override
	public boolean CanCaptureCity()
	{
		return false;
	}
	
	@Override
	public boolean CanCarry(IUnit unit)
	{
		if (unit.getClass() == UnitInfantry.class) return true;
		return false;
	}

	@Override
	public int CarryCapacity()
	{
		return 2;
	}
	
	@Override
	public int GetAttack(IUnit unit, TactikonState state)
	{
		// special case for helicopter - can't attack if fuel is zero
		if (fuelRemaining == 0) return 0;
		
		
		
		if (state.mGameVersion >= state.VERSION_WITH_NEW_COMBAT_TWEAKS_2)
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 1;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 1;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 2;
			}
		}
		
		if (unit.GetDomain() == Domain.Land)
		{ 
			return 3;
		} else if (unit.GetDomain() == Domain.Water)
		{
			return 3;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 4;
		}
		
		return 0;
	}

	@Override
	public int GetDefence(IUnit unit, TactikonState state)
	{
		if (state.mGameVersion >= state.VERSION_WITH_NERFED_TRANSPORTS)
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 2;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 2;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 1;
			}
		}
		
		if (unit.GetDomain() == Domain.Land)
		{ 
			return 3;
		} else if (unit.GetDomain() == Domain.Water)
		{
			return 3;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 4;
		}
		
		return 0;
	}
	
	@Override
	public ArrayList<IBattleBonus> GetTerrainBonus(int type)
	{
		ArrayList<IBattleBonus> bonuses = new ArrayList<IBattleBonus>();
		
		return bonuses;
	}

	@Override
	public ArrayList<IBattleBonus> GetCityBonus()
	{
		ArrayList<IBattleBonus> bonuses = new ArrayList<IBattleBonus>();
		bonuses.add(new BonusCity(0,-2));
		return bonuses;
	}

	@Override
	public Domain GetDomain()
	{
		return Domain.Air;
	}
	
	@Override
	public int GetAttackRange()
	{
		return 1;
	}
	
	@Override
	public int GetMaxFuel()
	{
		return 40;
	}
	
	@Override
	public boolean CanLandOnLand()
	{
		return true;
	}
	
	@Override
	public boolean CanChangeConfig(TactikonState state)
	{
		return false;
	}

	@Override
	public String GetConfigChangeType()
	{

		return null;
	}

	@Override
	public boolean IsStealth()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
