package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;



public class PackageUpdateAccount extends IPackage
{
	// query
	public String logo;
	public int colour;
	public String alias;
	
	//response
	public boolean bAliasAlreadyInUse;
	public String oldAlias = "";
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeUTF(logo);
		stream.writeInt(colour);
		stream.writeUTF(alias);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			logo = stream.readUTF();
			colour = stream.readInt();
			alias = stream.readUTF();
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		stream.writeBoolean(bAliasAlreadyInUse);
		stream.writeUTF(oldAlias);
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			bAliasAlreadyInUse = stream.readBoolean();
			oldAlias = stream.readUTF();
		}
	}

	
}
