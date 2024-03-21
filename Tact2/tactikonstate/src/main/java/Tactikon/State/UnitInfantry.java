package Tactikon.State;

import java.util.ArrayList;

import Tactikon.State.IUnit.Domain;


public class UnitInfantry extends IUnit
{

	@Override
	public int GetMovementDistance()
	{
		return 2;
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
		
		if (mCarriedBy != -1)
		{
			if (toType != TactikonState.TileType_Mountain)
				fromType = toType;
		}
		
		if (ValidMoveTile(fromType)  && ValidMoveTile(toType)) return true;
		
		return false;
	}
	
	@Override
	public boolean ValidMoveTile(int type)
	{
		if (type == TactikonState.TileType_Port) return true; 
		if (type == TactikonState.TileType_Land) return true;
		if (type == TactikonState.TileType_Mountain) return true;
		if (type == TactikonState.TileType_Jungle) return true;
		return false;
	}
	
	@Override
	public int GetUnitProductionTime()
	{
		return 2;
	}

	@Override
	public String GetName()
	{
		return "Infantry";
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
		
		if (state.mGameVersion >= state.VERSION_WITH_GAME_VERSION)
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 3;
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
			return 2;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 4;
		}
		
		return 0;
	}

	@Override
	public int GetDefence(IUnit unit, TactikonState state)
	{
		
		if (unit.GetDomain() == Domain.Land)
		{ 
			return 3;
		} else if (unit.GetDomain() == Domain.Water)
		{
			return 2;
		} else if (unit.GetDomain() == Domain.Air)
		{
			return 3;
		}
		
		return 0;
	}
	
	@Override
	public ArrayList<IBattleBonus> GetTerrainBonus(int type)
	{
		ArrayList<IBattleBonus> bonuses = new ArrayList<IBattleBonus>();
		if (type == TactikonState.TileType_Mountain)
			bonuses.add(new BonusTerrain(TactikonState.TileType_Mountain, 1, 2));
		
		if (type == TactikonState.TileType_Jungle)
			bonuses.add(new BonusTerrain(TactikonState.TileType_Jungle, 0, 3));
		
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
