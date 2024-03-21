package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoUnitLost extends IBubbleInfo
{

	int playerId = -1;
	String unitType = "";
	public int x = -1;
	public int y = -1;
	
	@Override
	public String GetText()
	{
		return "Your " + unitType + " was destroyed.";
	}

	@Override
	public Priority GetPriority()
	{
		return Priority.Info;
	}

	@Override
	public Type GetType()
	{
		return Type.UnitKilled;
	}
	
	BubbleInfoUnitLost()
	{
		
	}
	
	BubbleInfoUnitLost( IUnit unit)
	{
		this.playerId = unit.mUserId;
		this.x = unit.GetPosition().x;
		this.y = unit.GetPosition().y; 
		this.unitType = unit.getClass().getSimpleName();
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		playerId = stream.readInt();
		x = stream.readInt();
		y = stream.readInt();
		unitType = stream.readUTF();
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerId);
		stream.writeInt(x);
		stream.writeInt(y);
		stream.writeUTF(unitType);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
