package Core;

import java.util.LinkedList;

public abstract class IEventListener
{
	public boolean bAutoPump = false;
	public boolean bNoStateRecord = false;
	
	class QueuedEvent
	{
		IState before;
		IState after;
		IEvent event;
		
		QueuedEvent(IEvent event, IState before, IState after)
		{
			this.before = before;
			this.after = after;
			this.event = event;
		}
	}
	
	volatile LinkedList<QueuedEvent> mEventQueue = new LinkedList<QueuedEvent>();
	
	public int QueueSize()
	{
		return mEventQueue.size();
	}

	public void EnqueueEvent(IEvent event, IState before, IState after)
	{
		QueuedEvent queuedEvent = null;
		if (bNoStateRecord == false)
		{
			queuedEvent = new QueuedEvent(event, before, after);
		} else
		{
			queuedEvent = new QueuedEvent(event, null, null);
		}
		synchronized(mEventQueue)
		{
			mEventQueue.addLast(queuedEvent);
		}
	}
	
	public void ClearQueue()
	{
		mEventQueue.clear();
	}
	
	public boolean EventWaiting()
	{
		synchronized(mEventQueue)
		{
			if (mEventQueue.isEmpty()) return false;
		}
		return true; 
	}
	
	public void PumpQueue()
	{
		if (EventWaiting() == false) return;
		
		synchronized(mEventQueue)
		{
			QueuedEvent event = mEventQueue.poll();
			HandleQueuedEvent(event.event, event.before, event.after);
		}
	}
	
	public abstract void HandleQueuedEvent(IEvent event, IState before, IState after);

}
