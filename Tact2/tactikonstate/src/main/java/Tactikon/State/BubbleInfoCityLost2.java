package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BubbleInfoCityLost2 extends IBubbleInfo2
{
	public int cityId = -1;
	int playerId = -1;

	@Override
	public Type GetType()
	{
		return Type.CityTaken;
	}

	BubbleInfoCityLost2()
	{

	}

	BubbleInfoCityLost2(int playerId, int cityId)
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
