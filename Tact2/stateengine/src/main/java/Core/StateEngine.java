package Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StateEngine
{
	private List<IEventListener> mListeners = Collections.synchronizedList(new ArrayList<IEventListener>());
	
	volatile IState mState;
	
	private static Lock lock = new ReentrantLock();
	
	public IState GetState()
	{
		return mState;
	}
	
	public void DoDeferedPumping(int remaining)
	{
		lock.lock();
		for (IEventListener listener : mListeners)
		{
			if (listener.bAutoPump == true)
			{
				while (listener.EventWaiting() && listener.QueueSize() > remaining) listener.PumpQueue();
			}
		}
		lock.unlock();
	}
	
	void SetState(IState state)
	{
		mState = state;
	}
	
	public StateEngine(IState state)
	{
		mState = state;
	}
	
	public void AddListener(IEventListener listener)
	{
		lock.lock();
		//System.out.println("Adding new listener: " + listener.getClass().toString());
		mListeners.add(0, listener);
		lock.unlock();
	}
	
	public void RemoveListener(IEventListener listener)
	{
		lock.lock();
		if (mListeners.contains(listener))
		{
			//System.out.println("Removing listener: " + listener.getClass().toString());
			mListeners.remove(listener);
		}
		lock.unlock();
	}
	
	List<IEventListener> GetListeners()
	{
		return mListeners;
	}
}
