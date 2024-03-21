package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoUnitProduced2 extends IBubbleInfo2
{

	public int playerId = -1;
	public int x;
	public int y;
	public String unitType;

	@Override
	public Type GetType()
	{
		return Type.UnitProduced;
	}

	BubbleInfoUnitProduced2()
	{

	}

	BubbleInfoUnitProduced2(IUnit unit)
	{
		this.playerId = unit.mUserId;
		this.unitType = unit.getClass().getName();
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
