package uk.co.eidolon.shared.network;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import Core.IState;
import Network.PackageResponse;
import Network.Packages.PackageGetGame;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.utils.IAppWrapper;

public class UpdateQueuePump
{
    Context mContext;
    String mUpdateStateIntent;
    UpdateQueuePump(Context context)
    {
        mContext = context;
        IAppWrapper appWrapper = (IAppWrapper) context.getApplicationContext();
        mUpdateStateIntent = appWrapper.GetStateUpdatedIntentAction();
    }


    private void NotifyStateUpdated(int GameID, int UserId)
    {
        Intent intent = new Intent(mUpdateStateIntent);
        intent.putExtra("GameID", GameID);
        intent.putExtra("UserID", (long)UserId);
        Log.i("Tact2", "GCMIntentService sending a refresh broadcast");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    void PumpQueueNow(int userId)
    {
        UpdateQueue queue = new UpdateQueue(mContext, "GetGameUpdate", userId);

        int gameId = queue.GetNextGameIDFromQueue();

        int backoff = 1;

        while (gameId != -1)
        {
            try {
                Thread.sleep(backoff * 1000);
            } catch (Exception e)
            {

            }
            gameId = queue.GetNextGameIDFromQueue();

            final PackageGetGame p = new PackageGetGame();
            p.gameId = gameId;
            p.currentSequenceId = 0; // force update if the server tells us to

            PackageDelivery sender = new PackageDelivery(mContext, p, null);

            sender.DoSend();

            if (p.mReturnCode == PackageResponse.Success) {
                if (p.state != null) {
                    IState oldState = GameDatabase.getInstance(mContext).GetGame(gameId, userId);
                    GameDatabase.getInstance(mContext).UpdateGame(gameId, userId, p.state);
                    NotifyStateUpdated(gameId, userId);

                    new NotificationUtils().DoNotification(mContext, oldState, p.state, gameId);


                }
                queue.RemoveGameIDFromQueue(gameId);
            }

            backoff = backoff * 2;
        }

    }
}
