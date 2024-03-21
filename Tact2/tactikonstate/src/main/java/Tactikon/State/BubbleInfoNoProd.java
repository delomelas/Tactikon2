package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoNoProd extends IBubbleInfo
{

	int playerId = -1;
	public int cityId = -1;
	
	@Override
	public String GetText()
	{
		return "There is no current production in this city.";
	}

	@Override
	public Priority GetPriority()
	{
		return Priority.Attention;
	}

	@Override
	public Type GetType()
	{
		return Type.NoProd;
	}
	
	BubbleInfoNoProd()
	{
		
	}
	
	BubbleInfoNoProd(City city)
	{
		this.playerId = city.playerId;
		this.cityId = city.cityId;
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		playerId = stream.readInt();
		cityId = stream.readInt();
		
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerId);
		stream.writeInt(cityId);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
