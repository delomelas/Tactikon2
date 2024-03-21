package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;



public class PackageUpdateGCM extends IPackage
{
	// query
	public String gcmRegId = "";
	public long UserId = -1;
	
	//response
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeUTF(gcmRegId);
		stream.writeLong(UserId);

	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			gcmRegId = stream.readUTF();
			UserId = stream.readLong();
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
