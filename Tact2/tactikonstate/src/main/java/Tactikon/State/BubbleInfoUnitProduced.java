package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoUnitProduced extends IBubbleInfo
{

	public int playerId = -1;
	public int x;
	public int y;
	public String unitType;
	
	@Override
	public String GetText()
	{
		return "A new " + unitType + " has been produced in this city.";
	}

	@Override
	public Priority GetPriority()
	{
		return Priority.Warning;
	}

	@Override
	public Type GetType()
	{
		return Type.UnitProduced;
	}
	
	BubbleInfoUnitProduced()
	{
		
	}
	
	BubbleInfoUnitProduced(IUnit unit)
	{
		this.playerId = unit.mUserId;
		this.unitType = unit.GetName();
		this.x = unit.GetPosition().x;
		this.y = unit.GetPosition().y;
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		playerId = stream.readInt();
		unitType = stream.readUTF();
		x = stream.readInt();
		y = stream.readInt();
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerId);
		stream.writeUTF(unitType);
		stream.writeInt(x);
		stream.writeInt(y);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
