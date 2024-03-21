package Core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class IEvent
{
	abstract public IState updateState(IState before) throws InvalidUpdateException;
	abstract public void EventToBinary(DataOutputStream stream) throws IOException;
	abstract public void BinaryToEvent(DataInputStream stream) throws IOException;
	
	public static void TypedEventToBinary(IEvent event, DataOutputStream stream) throws IOException
	{
		stream.writeUTF(event.getClass().getName());
		event.EventToBinary(stream);
	}
	
	public static IEvent TypedBinaryToEvent(DataInputStream stream) throws IOException
	{
		String eventType = stream.readUTF();
		try
		{
			IEvent event = (IEvent) Class.forName(eventType).newInstance();
			event.BinaryToEvent(stream);
			return event;
		} catch (Exception e)
		{
			return null;
		}
	}
	
}
