package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Network.IPackage;
import Network.PackageResponse;


public class PackageJoinGame extends IPackage
{
	// query
	public IEvent joinGameEvent;
	public int gameId = -1;
	
	//response
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(gameId);
		IEvent.TypedEventToBinary(joinGameEvent, stream);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			gameId = stream.readInt();
			joinGameEvent = IEvent.TypedBinaryToEvent(stream);
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
		}
	}

	
}
