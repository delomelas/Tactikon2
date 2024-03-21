package uk.co.eidolon.shared.views;


import Core.IState;
import android.content.Context;
import android.view.View;

public interface IStateInfo
{
	IState GetState();
	void PopulateView(Context context, View v);
	int GetGameID();
	long GetUserID();
	void Dispose();
	void PopulateStateInfo(IState state);
	View InflateView(Context context);
}

