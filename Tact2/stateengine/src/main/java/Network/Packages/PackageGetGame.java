package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;


public class PackageGetGame extends IPackage
{
	// query
	public int gameId = -1;
	public int currentSequenceId = -1;
	
	//response
	public IState state = null;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(gameId);
		stream.writeInt(currentSequenceId);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			gameId = stream.readInt();
			currentSequenceId = stream.readInt();
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(mReturnCode.ordinal());
		if (state != null && currentSequenceId != state.GetSequence())
		{
			stream.writeBoolean(true);
			stream.writeUTF(state.getClass().getName());
			state.StateToBinary(stream);
		} else
		{
			stream.writeBoolean(false);
		}
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			boolean containsState = stream.readBoolean();
			if (containsState == true)
			{
				String stateType = stream.readUTF();
				try
				{
					state = (IState)Class.forName(stateType).newInstance();
				} catch (Exception e)
				{
					throw new IOException();
				}
				state.BinaryToState(stream);
			}
			
		}
	}

	
}
