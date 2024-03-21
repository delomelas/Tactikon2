package uk.co.eidolon.tact2;

import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.SharedPreferences;

public class TutorialBubbles
{
	Context mContext;
	
	enum Tutorial
	{
		FirstMessage,
		FirstSelectedUnit,
		EndTurnTime,
		FirstCitySelected
	}
	
	Map<Tutorial, Boolean> mSeenTuts = new TreeMap<Tutorial, Boolean>();
	
	TutorialBubbles(Context context)
	{
		mContext = context;
		
		
		SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		
		for (int i = 0; i < Tutorial.values().length; ++i)
		{
			boolean getTutStatus = prefs.getBoolean("Tutorial_" + i, false);
			mSeenTuts.put(Tutorial.values()[i], getTutStatus);
		}
	}
	
	String GetTutorialText(Tutorial tutType)
	{
		if (tutType == Tutorial.FirstMessage)
		{
			return "Tap your first unit to select it.";
		} else if (tutType == Tutorial.FirstSelectedUnit)
		{
			return "Tap in the highlighted area to select the destination, or elsewhere to de-select it.";
		} else if (tutType == Tutorial.EndTurnTime)
		{
			return "When you're done for this turn, tap the 'End Turn' button and wait for your opponents to take their turns.";
		} else if (tutType == Tutorial.FirstCitySelected)
		{
			return "When a city is selected, you can choose what is produced by the city.";
		}
		
		return "";
	}
	
	boolean HasSeenTutorual(Tutorial tutType)
	{
		return mSeenTuts.get(tutType);
	}
	
	void SetTutorialSeen(Tutorial tutType)
	{
		mSeenTuts.put(tutType, true);
	}
	
	void WriteTutorialStatus()
	{
		SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 0; i < Tutorial.values().length; ++i)
		{
			boolean getTutStatus = mSeenTuts.get(Tutorial.values()[i]);
			editor.putBoolean("Tutorial_" + i, getTutStatus);
		}
		editor.commit();
	}
}
