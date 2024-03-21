package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;

public class PackageGetProfile extends IPackage
{
	// query
	public int userId;
	
	//response
	public String alias="";
	public String logo="";
	public int colour;
	
	public TreeMap<Integer, Integer> profile = new TreeMap<Integer, Integer>();
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(userId);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			userId = stream.readInt();
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeUTF(alias);
		stream.writeUTF(logo);
		stream.writeInt(colour);
		stream.writeInt(profile.size());
		
		for (Entry<Integer, Integer> entry : profile.entrySet())
		{
			stream.writeInt(entry.getKey());
			stream.writeInt(entry.getValue());
		}
		
		
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			
			alias = stream.readUTF();
			logo = stream.readUTF();
			colour = stream.readInt();
			int profileSize = stream.readInt();
			
			profile = new TreeMap<Integer, Integer>();
			
			for (int profileNum = 0; profileNum < profileSize; ++profileNum)
			{
				int key = stream.readInt();
				int value = stream.readInt();
				profile.put(key, value);
			}
			
		}
	}

	
}
