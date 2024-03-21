package Tactikon.State;

import java.util.ArrayList;

import Tactikon.State.IUnit.Domain;


public class UnitTank extends IUnit
{

	@Override
	public int GetMovementDistance()
	{
		return 4;
	}
	
	@Override
	public byte GetMoveCost(Position from, Position to, TactikonState state)
	{
		if (state.map[from.x][from.y] == TactikonState.TileType_Jungle || state.map[to.x][to.y] == TactikonState.TileType_Jungle) return 2;
		return 1;
	}
	
	@Override
	public int GetVisionDistance(Position from, TactikonState state)
	{
		if (state.map[from.x][from.y] == TactikonState.TileType_Jungle) return GetMovementDistance() - 1;
		return GetMovementDistance();
	}

	@Override
	public boolean UnitCanMove(Position from, Position to, TactikonState state)
	{
		int fromType = state.map[from.x][from.y];
		int toType = state.map[to.x][to.y];
		
		if (toType == TactikonState.TileType_Mountain) return false;
		
		if (mCarriedBy != -1)
		{
			if (toType != TactikonState.TileType_Mountain)
				fromType = toType;
		}
		
		if (!ValidMoveTile(fromType)  || !ValidMoveTile(toType)) return false;
		
		return true;
	}
	
	@Override
	public boolean ValidMoveTile(int type)
	{
		if (type == TactikonState.TileType_Port) return true; 
		if (type == TactikonState.TileType_Land) return true;
		if (type == TactikonState.TileType_Jungle) return true;
		return false;
	}
	
	@Override
	public int GetUnitProductionTime()
	{
		return 4;
	}

	@Override
	public String GetName()
	{
		return "Tank";
	}

	@Override
	public boolean CanCaptureCity()
	{
		return true;
	}
	
	@Override
	public boolean CanCarry(IUnit unit)
	{
		return false;
	}

	@Override
	public int CarryCapacity()
	{
		return 0;
	}
	
	@Override
	public int GetAttack(IUnit unit, TactikonState state)
	{
		if (state.mGameVersion >= state.VERSION_WITH_NEW_COMBAT_TWEAKS_3)
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 7;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 6;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 1;
			}
		}
		
		if (unit.GetDomain() == Domain.Land)
		{ 
			return 7;
		} else if (unit.GetDomain() == Domain.Water)
		{
			return 6;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 2;
		}
		
		
		return 0;
	}

	@Override
	public int GetDefence(IUnit unit, TactikonState state)
	{

		if (unit.GetDomain() == Domain.Land)
		{ 
			return 5;
		} else if (unit.GetDomain() == Domain.Water)
		{
			return 5;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 5;
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
		bonuses.add(new BonusCity(1,1));
		return bonuses;
	}

	@Override
	public Domain GetDomain()
	{
		return Domain.Land;
	}
	
	@Override
	public int GetAttackRange()
	{
		return 1;
	}
	
	@Override
	public int GetMaxFuel()
	{
		return -1;
	}
	
	@Override
	public boolean CanLandOnLand()
	{
		return false;
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
