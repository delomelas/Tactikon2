package Tactikon.State;

import java.util.ArrayList;

import Tactikon.State.IUnit.Domain;

public class UnitSub extends IUnit
{

	@Override
	public int GetMovementDistance()
	{
		if (mConfig == 0)
		{
			return 7;
		} else
		{
			return 8;
		}
	}
	
	@Override
	public byte GetMoveCost(Position from, Position to, TactikonState state)
	{
		return 1;
	}

	@Override
	public boolean UnitCanMove(Position from, Position to, TactikonState state)
	{
		int fromType = state.map[from.x][from.y];
		int toType = state.map[to.x][to.y];
		
		if (mCarriedBy != -1) fromType = toType;
		
		if (ValidMoveTile(fromType)  && ValidMoveTile(toType)) return true;
		
		return false;
	}
	
	@Override
	public boolean ValidMoveTile(int type)
	{
		if (type == TactikonState.TileType_Port && mConfig == 0) return true; 
		if (type == TactikonState.TileType_Water) return true;
		return false;
	}
	
	@Override
	public int GetUnitProductionTime()
	{
		return 8;
	}

	@Override
	public String GetName()
	{
		return "Sub";
	}
	
	@Override
	public boolean CanCaptureCity()
	{
		return false;
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
		if (state.mGameVersion >= state.VERSION_WITH_NEW_COMBAT_TWEAKS_2)
		{
			if (mConfig == 0) // surface
			{
				if (unit.GetDomain() == Domain.Land)
				{ 
					return 1;
				} else if (unit.GetDomain() == Domain.Water)
				{
					return 8;
				} else if (unit.GetDomain() == Domain.Air)
				{
					return 5;
				}
			} else // submerged
			{
				if (unit.GetDomain() == Domain.Land)
				{ 
					return 0;
				} else if (unit.GetDomain() == Domain.Water)
				{
					return 14;
				} else if (unit.GetDomain() == Domain.Air)
				{
					return 0;
				}
			}
		}
		
		if (mConfig == 0) // surface
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 1;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 8;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 5;
			}
		} else // submerged
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 0;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 14;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 0;
			}
		}
			
		return 0;
	}

	@Override
	public int GetDefence(IUnit unit, TactikonState state)
	{
		if (state.mGameVersion >= state.VERSION_WITH_NEW_COMBAT_TWEAKS_2)
		{
			if (mConfig == 0) // surface
			{
				if (unit.GetDomain() == Domain.Land)
				{ 
					return 7;
				} else if (unit.GetDomain() == Domain.Water)
				{
					return 6;
				} else if (unit.GetDomain() == Domain.Air)
				{
					return 8;
				}
			} else // submerged
			{
				if (unit.GetDomain() == Domain.Land)
				{ 
					return 14;
				} else if (unit.GetDomain() == Domain.Water)
				{
					return 8;
				} else if (unit.GetDomain() == Domain.Air)
				{
					return 12;
				}
			}
		}
		
		if (mConfig == 0) // surface
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 7;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 4;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 8;
			}
		} else // submerged
		{
			if (unit.GetDomain() == Domain.Land)
			{ 
				return 14;
			} else if (unit.GetDomain() == Domain.Water)
			{
				return 4;
			} else if (unit.GetDomain() == Domain.Air)
			{
				return 12;
			}
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
		bonuses.add(new BonusCity(0,-1));
		return bonuses;
	}

	@Override
	public Domain GetDomain()
	{
		return Domain.Water;
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
		if (state.mGameVersion < state.VERSION_WITH_UNITCONFIG) return false;
		
		if (state.map[GetPosition().x][GetPosition().y] != TactikonState.TileType_Water) return false;
		
		if (bChangedConfig == false) return true;
		return false;
	}

	@Override
	public String GetConfigChangeType()
	{
		if (mConfig == 0)
		{
			return "Submerge";
		} else if (mConfig == 1)
		{
			return "Rise";
		}
		return null;
	}

	@Override
	public boolean IsStealth()
	{
		if (mConfig == 1) return true;
		return false;
	}
}
