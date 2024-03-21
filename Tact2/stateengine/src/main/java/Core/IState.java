package Core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class IState implements Cloneable
{

	
	public enum GameStatus
	{
		WaitingForPlayers,
		InGame,
		GameOver,
		TimeOut
	}
	
	protected int sequence = 0;
	
	protected int winningPlayer;
	
	public long lastUpdateTime = -1;
	
	abstract public GameStatus GetGameState();
	public int GetSequence()
	{
		return sequence;
	}
	
	public long GetLastUpdateTime()
	{
		return lastUpdateTime;
	}
	
	public void IncSequence()
	{
		sequence ++;
	}
	
	abstract public void StateToBinary(DataOutputStream stream) throws IOException;
	abstract public void BinaryToState(DataInputStream stream) throws IOException;
	
	abstract public IState CopyState();
	abstract public ArrayList<Integer> GetPlayers();
	abstract public long GetPlayerToPlay();
	abstract public PlayerInfo GetPlayerInfo(long playerId);
	abstract public boolean IsPlayerAlive(long playerId);
	abstract public boolean IsFriendsOnly();
	abstract public int GetCreatorId();
	
}
