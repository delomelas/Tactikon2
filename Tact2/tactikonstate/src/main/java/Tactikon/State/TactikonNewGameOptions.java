package Tactikon.State;

import Support.INewGameOptions;

public class TactikonNewGameOptions extends INewGameOptions
{
	public enum WinCondition
	{
		CaptureAllBases,
		CaptureHQ,
		Annihilate,
	}
	
	public enum MirrorType
	{
		None,
		Vertical,
		Horizontal,
		Both
	}
	
	public enum AILevel
	{
		Beginner,
		Intermediate
	}

	
	public int numHumanPlayers;
	public int numAIPlayers;
	public int mapSeed;
	public int mapSize;
	public float scale;
	public int cities;
	public float landMassRatio;
	public boolean fogOfWar;
	public WinCondition winCondition;
	public MirrorType mirrorType;
	public boolean bMountains = true;
	public boolean bForest = true;
	public int turnTimeOut = 24;
	public boolean bLocalGame = false;
	public boolean bFriendsOnly = false;
	public String createdByAlias = "";
	public int createdById = -1;
	public AILevel[] aiLevel = new AILevel[7];
	public boolean bTutorial = false;
	
	public TactikonNewGameOptions()
	{
		for (int i = 0; i < 7; ++i)
		{
			aiLevel[i] = AILevel.Intermediate;
		}
	}
	
}
