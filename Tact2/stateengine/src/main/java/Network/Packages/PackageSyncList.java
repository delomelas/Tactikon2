package Network.Packages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import Network.IPackage;
import Network.PackageResponse;

public class PackageSyncList extends IPackage
{
	// query
	public ArrayList<Integer> addList = new ArrayList<Integer>();
	public ArrayList<Integer> deleteList = new ArrayList<Integer>();
	
	public String syncListName;
		
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	// response
	public ArrayList<Integer> syncList = new ArrayList<Integer>();
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeUTF(syncListName);
		
		stream.writeInt(addList.size());
		for (int i = 0; i < addList.size(); ++i)
		{
			stream.writeInt(addList.get(i));
		}
		
		stream.writeInt(deleteList.size());
		for (int i = 0; i < deleteList.size(); ++i)
		{
			stream.writeInt(deleteList.get(i));
		}
	}

	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			
			syncListName = stream.readUTF();
			
			int addListSize = stream.readInt();
			for (int i = 0; i < addListSize; ++i)
			{
				addList.add(stream.readInt());
			}
			
			int removeListSize = stream.readInt();
			for (int i = 0; i < removeListSize; ++i)
			{
				deleteList.add(stream.readInt());
			}
			
		}
	}
		@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeInt(syncList.size());
		for (int i = 0; i < syncList.size(); i++)
		{
			stream.writeInt(syncList.get(i));
		}
	}
		@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			
			int listSize = stream.readInt();
			for (int i = 0; i < listSize; ++i)
			{
				syncList.add(stream.readInt());
			}
			
		}
	}

}