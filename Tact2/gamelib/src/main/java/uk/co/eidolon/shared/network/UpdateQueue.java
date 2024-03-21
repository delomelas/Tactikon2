package uk.co.eidolon.shared.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class UpdateQueue
{
    Context mContext;
    String mQueueName;


    UpdateQueue(Context context, String queueName, long userId)
    {
        mContext = context;
        mQueueName = queueName + Long.toString(userId);

    }

    void AddGameIDToQueue(int gameId)
    {
        String queueName = "TactQueue" + mQueueName;

        SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);

        Set<String> ids = prefs.getStringSet(queueName, new HashSet<String>());
        ids.add(Integer.toString(gameId));

        SharedPreferences.Editor editor = prefs.edit();

        editor.putStringSet(queueName, ids);
        editor.commit();
    }

    int GetNextGameIDFromQueue()
    {
        String queueName = "TactQueue" + mQueueName;

        SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);

        Set<String> ids = prefs.getStringSet(queueName, new TreeSet<String>());
        if (ids.isEmpty()) return -1;

        String firstId = ids.iterator().next();

        return Integer.valueOf(firstId);
    }

    void ClearQueue()
    {
        String queueName = "TactQueue" + mQueueName;

        SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);

        Set<String> ids = new HashSet<String>();

        SharedPreferences.Editor editor = prefs.edit();

        editor.putStringSet(queueName, ids);
        editor.commit();
    }

    void RemoveGameIDFromQueue(int gameId)
    {
        String queueName = "TactQueue" + mQueueName;

        SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.gameLib", Context.MODE_MULTI_PROCESS);

        Set<String> ids = prefs.getStringSet(queueName, new HashSet<String>());
        ids.remove(Integer.toString(gameId));

        SharedPreferences.Editor editor = prefs.edit();

        editor.putStringSet(queueName, ids);
        editor.commit();
    }
}
