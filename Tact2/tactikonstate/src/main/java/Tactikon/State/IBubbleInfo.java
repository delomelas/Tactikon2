package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class IBubbleInfo
{
	public enum Priority
	{
		Info,
		Warning, 
		Attention
	}
	
	public enum Type
	{
		CityTaken,
		UnitKilled,
		ProductionOnHold,
		UnitProduced,
		PlaneCrash,
		NoProd
	}
	
	abstract public Priority GetPriority();
	abstract public String GetText();
	abstract public Type GetType();
	abstract void DoBinaryToInfo(DataInputStream stream) throws IOException;
	abstract void DoInfoToBinary(DataOutputStream stream) throws IOException;
	abstract public boolean MessageForPlayer(int playerId);
	
	static IBubbleInfo BinaryToInfo(DataInputStream stream) throws IOException
	{
		Type type = Type.values()[stream.readInt()];
		
		IBubbleInfo info = null;
		
		if (type == Type.CityTaken)
		{
			info = new BubbleInfoCityLost();
		} else if (type == Type.ProductionOnHold)
		{
			info = new BubbleInfoProductionOnHold();
		} else if (type == Type.UnitKilled)
		{
			info = new BubbleInfoUnitLost();
		} else if (type == Type.UnitProduced)
		{
			info = new BubbleInfoUnitProduced();
		} else if (type == Type.PlaneCrash)
		{
			info = new BubbleInfoCrashedPlane();
		}else if (type == Type.NoProd)
		{
			info = new BubbleInfoNoProd();
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
