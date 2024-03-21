package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public abstract class IUnit
{
	public int mUnitId = -1;
	private Position position = new Position(0,0);
	public boolean mFortified = false;
	public ArrayList<Integer> mCarrying = new ArrayList<Integer>();
	public int mCarriedBy = -1;
	public int health = 3;
	public int kills = 0;
	public boolean mIsVeteran = false;
	public int fuelRemaining = -1;
	public boolean bMoved = false;
	public boolean bAttacked = false;
	public boolean bChangedConfig = false;
	public int mConfig = 0;
		
	public int mUserId;
	
	static int VERSION_FIRST = 0;
	static int VERSION_COMPRESSED = 1;
	static int VERSION_WITHTRACKS = 2;
	
	//int mVersion = VERSION_FIRST;
	
	public int GetUnitID()
	{
		return mUnitId;
	}
	
	public Position GetPosition()
	{
		return new Position(position.x, position.y);
	}
	
	public void SetPosition(int x, int y)
	{
		position.Set(x, y);
	}
	
	abstract public int GetMovementDistance();
	abstract public byte GetMoveCost(Position from, Position to, TactikonState state);
	abstract public boolean UnitCanMove(Position from, Position to, TactikonState state);
	
	public int GetVisionDistance(Position from, TactikonState state)
	{
		return GetMovementDistance();
	}
	
	public int GetProductionTime(City city, TactikonState state)
	{
		
		return GetUnitProductionTime();
	}
	
	abstract public int GetUnitProductionTime();
	
	abstract public boolean CanChangeConfig(TactikonState state);
	abstract public String GetConfigChangeType();
	abstract public String GetName();
	abstract public boolean ValidMoveTile(int type);
	abstract public boolean CanCaptureCity();
	abstract public boolean CanCarry(IUnit unit);
	abstract public int CarryCapacity();
	abstract public int GetAttack(IUnit unit, TactikonState state);
	abstract public int GetDefence(IUnit unit, TactikonState state);
	abstract public ArrayList<IBattleBonus> GetTerrainBonus(int type);
	abstract public ArrayList<IBattleBonus> GetCityBonus();
	abstract public Domain GetDomain();
	abstract public int GetAttackRange();
	abstract public int GetMaxFuel();
	abstract public boolean CanLandOnLand();
	abstract public boolean IsStealth();
	
	public enum Domain
	{
		Land,
		Water,
		Air
	}
	
	public ArrayList<IBattleBonus> GetBonuses(TactikonState state)
	{
		ArrayList<IBattleBonus> bonuses = new ArrayList<IBattleBonus>();
		
		if (mIsVeteran) bonuses.add(new BonusVeteran());
		
		for (City city : state.cities)
		{
			if (city.x == position.x && city.y == position.y)
			{
				bonuses.addAll(GetCityBonus());
				break;
			}
		}
		
		
		bonuses.addAll(GetTerrainBonus(state.map[position.x][position.y]));
		
		return bonuses;
	}
	
	
	// checks terrain and other constants for movement ability
	public boolean CanMove(Position from, Position to, TactikonState state)
	{
		// check the unit-specific movement code
		if (UnitCanMove(from, to, state) == false) return false;
		
		return true;
	}
	
	void UnitToBinary(DataOutputStream stream, boolean bCompressed) throws IOException
	{
		if (bCompressed == true)
		{
			stream.writeInt(VERSION_COMPRESSED);
		} else
		{
			stream.writeInt(VERSION_FIRST);
		}
		
		if (bCompressed == false)
		{
			stream.writeUTF(this.getClass().getName());
		
			stream.writeInt(mUnitId);
			stream.writeInt(position.x);
			stream.writeInt(position.y);
			stream.writeInt(mUserId);
			stream.writeBoolean(mFortified);
			
			stream.writeInt(mCarriedBy);
			stream.writeInt(mCarrying.size());
			for (int i = 0; i < mCarrying.size(); ++i)
			{
				stream.writeInt(mCarrying.get(i));
			}
			
			stream.writeInt(health);
			stream.writeBoolean(mIsVeteran);
			stream.writeInt(kills);
			stream.writeInt(fuelRemaining);
			stream.writeBoolean(bMoved);
			stream.writeBoolean(bAttacked);
		} else
		{
			if (this.getClass() == UnitTank.class)
			{
				stream.writeByte(0);
			} else if (this.getClass() == UnitInfantry.class)
			{
				stream.writeByte(1);
			} else if (this.getClass() == UnitSub.class)
			{
				stream.writeByte(2);
			} else if (this.getClass() == UnitHelicopter.class)
			{
				stream.writeByte(3);
			} else if (this.getClass() == UnitCarrier.class)
			{
				stream.writeByte(4);
			} else if (this.getClass() == UnitBomber.class)
			{
				stream.writeByte(5);
			} else if (this.getClass() == UnitBoatTransport.class)
			{
				stream.writeByte(6);
			} else if (this.getClass() == UnitBattleship.class)
			{
				stream.writeByte(7);
			} else if (this.getClass() == UnitFighter.class)
			{
				stream.writeByte(8);
			}
			
			stream.writeInt(mUnitId);
			stream.writeByte(position.x);
			stream.writeByte(position.y);
			stream.writeInt(mUserId);
			stream.writeBoolean(mFortified);
			
			stream.writeInt(mCarriedBy);
			stream.writeByte(mCarrying.size());
			for (int i = 0; i < mCarrying.size(); ++i)
			{
				stream.writeInt(mCarrying.get(i));
			}
			
			stream.writeByte(health);
			stream.writeBoolean(mIsVeteran);
			stream.writeInt(kills);
			stream.writeByte(fuelRemaining);
			stream.writeBoolean(bMoved);
			stream.writeBoolean(bAttacked);	
		}
	} 
		
	static IUnit BinaryToUnit(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		IUnit unit = null;
		
		if (version == VERSION_FIRST)
		{
			String type = stream.readUTF();
			try
			{
				unit = (IUnit) Class.forName(type).newInstance();
			} catch (Exception e)
			{
				return null;
			}
			
			unit.mUnitId = stream.readInt();
			int x = stream.readInt();
			int y = stream.readInt();
			unit.position = new Position(x, y);
			unit.mUserId = stream.readInt();
			unit.mFortified = stream.readBoolean();
			
			unit.mCarriedBy = stream.readInt();
			int numCarrying = stream.readInt();
			
			for (int i = 0; i < numCarrying; ++i)
			{
				unit.mCarrying.add(stream.readInt());
			}
			unit.health = stream.readInt();
			unit.mIsVeteran = stream.readBoolean();
			unit.kills = stream.readInt();
			unit.fuelRemaining = stream.readInt();
			unit.bMoved = stream.readBoolean();
			unit.bAttacked = stream.readBoolean();
			
		} else
		{
			byte unitType = stream.readByte();
			
			if (unitType == 0)
			{
				unit = new UnitTank();
			} else if (unitType == 1)
			{
				unit = new UnitInfantry();
			} else if (unitType == 2)
			{
				unit = new UnitSub();
			} else if (unitType == 3)
			{
				unit = new UnitHelicopter();
			} else if (unitType == 4)
			{
				unit = new UnitCarrier();
			} else if (unitType == 5)
			{
				unit = new UnitBomber();
			} else if (unitType == 6)
			{
				unit = new UnitBoatTransport();
			} else if (unitType == 7)
			{
				unit = new UnitBattleship();
			} else if (unitType == 8)
			{
				unit = new UnitFighter();
			}
			
			unit.mUnitId = stream.readInt();
			int x = stream.readByte();
			int y = stream.readByte();
			unit.position = new Position(x, y);
			unit.mUserId = stream.readInt();
			unit.mFortified = stream.readBoolean();
			
			unit.mCarriedBy = stream.readInt();
			int numCarrying = stream.readByte();
			
			for (int i = 0; i < numCarrying; ++i)
			{
				unit.mCarrying.add(stream.readInt());
			}
			unit.health = stream.readByte();
			unit.mIsVeteran = stream.readBoolean();
			unit.kills = stream.readInt();
			unit.fuelRemaining = stream.readByte();
			unit.bMoved = stream.readBoolean();
			unit.bAttacked = stream.readBoolean();
			
		}

		return unit;
	}
}
