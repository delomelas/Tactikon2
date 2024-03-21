package Network.Packages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import Core.IEvent;
import Network.EventQueueItem;
import Network.IPackage;
import Network.PackageResponse;


public class PackageEventList extends IPackage
{
	// query
	public ArrayList<EventQueueItem> eventList = new ArrayList<EventQueueItem>();
	
	//response
	public int mSuccessCount;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(eventList.size());
		for (int i = 0; i < eventList.size(); ++i)
		{
			IEvent event = eventList.get(i).event;
			stream.writeInt(eventList.get(i).GameID);
			
			IEvent.TypedEventToBinary(event, stream);
		}
		
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			int numEvents = stream.readInt();
			eventList = new ArrayList<EventQueueItem>();
			for (int i = 0; i < numEvents; ++i)
			{
				EventQueueItem item = new EventQueueItem();
				
				item.GameID = stream.readInt();
				
				IEvent event = IEvent.TypedBinaryToEvent(stream);
				
				item.event = event;
				
				eventList.add(item);
				
			}
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		stream.writeInt(mSuccessCount);
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			mSuccessCount = stream.readInt();
		}
	}

}
