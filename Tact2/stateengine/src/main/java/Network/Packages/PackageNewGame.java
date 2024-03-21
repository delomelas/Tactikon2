package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Network.IPackage;
import Network.PackageResponse;


public class PackageNewGame extends IPackage
{
	// query
	public IEvent newGameEvent;
	
	//response
	
	public int mGameID;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		IEvent.TypedEventToBinary(newGameEvent, stream);
		
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			newGameEvent = IEvent.TypedBinaryToEvent(stream);
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mGameID);
		stream.writeInt(mReturnCode.ordinal());
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mGameID = stream.readInt();
			mReturnCode = PackageResponse.values()[stream.readInt()];
		}
	}

	
}
