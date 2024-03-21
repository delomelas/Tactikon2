package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoCityLost extends IBubbleInfo
{
	public int cityId = -1;
	int playerId = -1;
	
	@Override
	public String GetText()
	{
		return "Your city has been captured.";
	}

	@Override
	public Priority GetPriority()
	{
		return Priority.Warning;
	}

	@Override
	public Type GetType()
	{
		return Type.CityTaken;
	}
	
	BubbleInfoCityLost()
	{
		
	}
	
	BubbleInfoCityLost(int playerId, int cityId)
	{
		this.playerId = playerId;
		this.cityId = cityId;
	}

	@Override
	void DoBinaryToInfo(DataInputStream stream) throws IOException 
	{
		cityId = stream.readInt();
		playerId = stream.readInt();
		
	}

	@Override
	void DoInfoToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(cityId);
		stream.writeInt(playerId);
	}

	@Override
	public boolean MessageForPlayer(int playerId)
	{
		if (this.playerId == playerId) return true;
		return false;
	}
	

}
