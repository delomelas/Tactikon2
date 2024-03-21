package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoCrashedPlane2 extends IBubbleInfo2
{
	public int x,y;
	int playerId;
	public String unitType;

	@Override
	public Type GetType()
	{
		return Type.PlaneCrash;
	}

	BubbleInfoCrashedPlane2()
	{

	}

	BubbleInfoCrashedPlane2(IUnit unit)
	{
		this.x = unit.GetPosition().x;
		this.y = unit.GetPosition().y;
		this.playerId = unit.mUserId;
		this.unitType = unit.getClass().getName();
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		x = stream.readInt();
		y = stream.readInt();
		playerId = stream.readInt();
		unitType = stream.readUTF();
		
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(x);
		stream.writeInt(y);
		stream.writeInt(playerId);
		stream.writeUTF(unitType);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
