package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class IBubbleInfo2
{
	public enum Type
	{
		CityTaken,
		UnitKilled,
		ProductionOnHold,
		UnitProduced,
		PlaneCrash,
		NoProd
	}
	
	abstract public Type GetType();
	abstract void DoBinaryToInfo(DataInputStream stream) throws IOException;
	abstract void DoInfoToBinary(DataOutputStream stream) throws IOException;
	abstract public boolean MessageForPlayer(int playerId);

	static IBubbleInfo2 BinaryToInfo(DataInputStream stream) throws IOException
	{
		Type type = Type.values()[stream.readInt()];
		
		IBubbleInfo2 info = null;
		
		if (type == Type.CityTaken)
		{
			info = new BubbleInfoCityLost2();
		} else if (type == Type.ProductionOnHold)
		{
			info = new BubbleInfoProductionOnHold2();
		} else if (type == Type.UnitKilled)
		{
			info = new BubbleInfoUnitLost2();
		} else if (type == Type.UnitProduced)
		{
			info = new BubbleInfoUnitProduced2();
		} else if (type == Type.PlaneCrash)
		{
			info = new BubbleInfoCrashedPlane2();
		}else if (type == Type.NoProd)
		{
			info = new BubbleInfoNoProd2();
		}
		
		info.DoBinaryToInfo(stream);
		
		return info;
	}
	
	void InfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(GetType().ordinal());
		
		DoInfoToBinary(stream);
	}
}
