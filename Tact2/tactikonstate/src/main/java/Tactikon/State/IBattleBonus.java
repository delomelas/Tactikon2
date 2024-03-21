package Tactikon.State;

public abstract class IBattleBonus
{
	int mDefValue;
	int mAtkValue;
	String mName;
	
	public int GetAtkValue()
	{
		return mAtkValue;
	}
	
	public int GetDefValue()
	{
		return mDefValue;
	}
	public String GetBonusName()
	{
		return mName;
	}
}
