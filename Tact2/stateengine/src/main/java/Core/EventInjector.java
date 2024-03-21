package Core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class EventInjector
{
	StateEngine mEngine;
	

	public EventInjector(StateEngine engine)
	{
		mEngine = engine;
	}
	
	public void AddEvent(IEvent event)
	{
		List<IEventListener> listeners = mEngine.GetListeners();
		
		IState oldState = mEngine.GetState();
		IState newState;
		try
		{
			int sequence = oldState.GetSequence();
			newState = event.updateState(oldState);
			if (newState.GetSequence() <= sequence) throw new InvalidUpdateException();
			newState.lastUpdateTime = System.currentTimeMillis() / 1000;
			mEngine.SetState(newState);
		} catch (InvalidUpdateException e)
		{
			e.printStackTrace();
			
			return;
		}
		

		for (IEventListener listener : listeners)
		{
			listener.EnqueueEvent(event, oldState, newState);
		}	
	}
	
}
