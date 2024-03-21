package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import Network.IPackage;
import Network.PackageResponse;


public class PackageSendInvite extends IPackage
{
	// query
	public int fromUserId = -1;
	public int toUserId = -1;
	public String fromAlias = "";
	public int gameId = -1;
	
	//response
	public boolean bSent;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(fromUserId);
		stream.writeInt(toUserId);
		stream.writeUTF(fromAlias);
		
		stream.writeInt(gameId);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			fromUserId = stream.readInt();
			toUserId = stream.readInt();
			fromAlias = stream.readUTF();
			gameId = stream.readInt();
				
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeBoolean(bSent);
		
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			
			bSent = stream.readBoolean();
		
		}
	}

	
}
