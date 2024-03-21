package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoUnitLost2 extends IBubbleInfo2
{

	int playerId = -1;
	public String unitType = "";
	public String unitTypeEnemy = "";
	public int x = -1;
	public int y = -1;
	public int playerIdEnemy = -1;

	@Override
	public Type GetType()
	{
		return Type.UnitKilled;
	}

	BubbleInfoUnitLost2()
	{

	}

	BubbleInfoUnitLost2(IUnit unit, IUnit unit2)
	{
		this.playerId = unit.mUserId;
		this.x = unit.GetPosition().x;
		this.y = unit.GetPosition().y; 
		this.unitType = unit.getClass().getName();
		this.unitTypeEnemy = unit2.getClass().getName();
		this.playerIdEnemy = unit2.mUserId;
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		playerId = stream.readInt();
		x = stream.readInt();
		y = stream.readInt();
		unitType = stream.readUTF();
		unitTypeEnemy = stream.readUTF();
		playerIdEnemy = stream.readInt();
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerId);
		stream.writeInt(x);
		stream.writeInt(y);
		stream.writeUTF(unitType);
		stream.writeUTF(unitTypeEnemy);
		stream.writeInt(playerIdEnemy);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
