package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;



public class PackageSearchAlias extends IPackage
{
	// query
	public String searchAlias = "";
	public int searchId = -1;
	
	//response
	public long userId;
	public String logo = "";
	public int colour = 0;
	public String alias = "";
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeUTF(searchAlias);
		stream.writeInt(searchId);

	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			searchAlias = stream.readUTF();
			searchId = stream.readInt();

		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeLong(userId);
		stream.writeUTF(logo);
		stream.writeInt(colour);
		stream.writeUTF(alias);
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			userId = stream.readLong();
			logo = stream.readUTF();
			colour = stream.readInt();
			alias = stream.readUTF();
		}
	}

	
}
