package uk.co.eidolon.shared.network;

import android.content.Context;

import Core.IEvent;
import Core.IEventListener;
import Core.IState;
import Network.EventQueueItem;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class GameNetworkListener extends IEventListener
{
	int mGameID;
	long mUserID;
	Context mContext;
	
	public GameNetworkListener(Context context, int GameID, long UserID)
	{
		mGameID = GameID;
		mUserID = UserID;
		mContext = context;
		
		// network listener only needs events, I think
		bNoStateRecord = true;
	}
	
	@Override
	public void HandleQueuedEvent(IEvent event, IState before, IState after)
	{
		if (mGameID < 0) return;
		// add event to the server upload queue
		// occasionally flush the queue
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		EventQueueItem item = new EventQueueItem();
		item.event = event;
		item.GameID = mGameID;
		
		appWrapper.GetUploadQueue().AddToQueue(item);
	}
	
	public void FlushQueue()
	{
		IAppWrapper appWrapper = (IAppWrapper)mContext.getApplicationContext();
		appWrapper.GetUploadQueue().FlushQueue();
	}
	
}
