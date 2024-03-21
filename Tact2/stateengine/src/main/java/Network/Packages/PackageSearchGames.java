package Network.Packages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import Core.IState;
import Network.IPackage;
import Network.PackageResponse;

public class PackageSearchGames extends IPackage
{
	final int VERSION_FIRST = 0;
	final int mVersion = VERSION_FIRST;
	
	// query
	// state = waiting, playing, finished
	public IState.GameStatus searchState = IState.GameStatus.WaitingForPlayers;
	// containing playerId (-1 for any)
	// NOTE: not currently implemented
	public int searchPlayerId;
	// start offset
	public int startOffset;
	// max results to return
	public int maxResults;
	
	// response
	// number returned
	public int numReturned = 0;
	// list of games returned
	public ArrayList<Integer> resultGameIDs = new ArrayList<Integer>();
	public ArrayList<IState> resultData = new ArrayList<IState>();
	public boolean bMoreResults = false;
	// number remaining to return

	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(searchState.ordinal());
		stream.writeInt(searchPlayerId);
		stream.writeInt(startOffset);
		stream.writeInt(maxResults);
	}

	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			searchState = IState.GameStatus.values()[stream.readInt()];
			searchPlayerId = stream.readInt();
			startOffset = stream.readInt();
			maxResults = stream.readInt();
		}
		
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeInt(numReturned);
		
		if (numReturned != resultData.size()) throw new IOException();
		if (numReturned != resultGameIDs.size()) throw new IOException();
		
		for (int i = 0; i < numReturned; ++i)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			stream.writeUTF(resultData.get(i).getClass().getName());
			
			(resultData.get(i)).StateToBinary(new DataOutputStream(baos));
			
			stream.writeInt(baos.size());
			System.out.println("Writing a state with " + baos.size() + " bytes.");
			
			stream.write(baos.toByteArray(), 0, baos.size());
		}
		for (int i = 0; i < numReturned; ++i)
		{
			stream.writeInt(resultGameIDs.get(i));
		}
		stream.writeBoolean(bMoreResults);

	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			numReturned = stream.readInt();
			
			resultData = new ArrayList<IState>();
			resultData.ensureCapacity(numReturned);
			resultGameIDs = new ArrayList<Integer>();
			resultGameIDs.ensureCapacity(numReturned);
			for (int i = 0; i < numReturned; ++i)
			{
				String stateType = stream.readUTF();
				IState state;
				try
				{
					state = (IState) Class.forName(stateType).newInstance();
				} catch (Exception e)
				{
					throw new IOException();
				}
				
				int size = stream.readInt();
				
				byte[] bytes = new byte[size];
				
				stream.readFully(bytes,0,size);	
				
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				
				state.BinaryToState(new DataInputStream(bais));
				resultData.add(state);
				
			}
			for (int i = 0; i < numReturned; ++i)
			{
				resultGameIDs.add(stream.readInt());
			}
			
			bMoreResults = stream.readBoolean();
		}
		
	}

}
