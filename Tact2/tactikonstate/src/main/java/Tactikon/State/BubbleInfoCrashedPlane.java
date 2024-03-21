package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoCrashedPlane extends IBubbleInfo
{
	public int x,y;
	int playerId;
	
	@Override
	public String GetText()
	{
		return "Plane crash.";
	}

	@Override
	public Priority GetPriority()
	{
		return Priority.Warning;
	}

	@Override
	public Type GetType()
	{
		return Type.PlaneCrash;
	}
	
	BubbleInfoCrashedPlane()
	{
		
	}
	
	BubbleInfoCrashedPlane(IUnit unit)
	{
		this.x = unit.GetPosition().x;
		this.y = unit.GetPosition().y;
		this.playerId = unit.mUserId;
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		x = stream.readInt();
		y = stream.readInt();
		playerId = stream.readInt();
		
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(x);
		stream.writeInt(y);
		stream.writeInt(playerId);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
