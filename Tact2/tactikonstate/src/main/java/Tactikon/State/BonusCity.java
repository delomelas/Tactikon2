package Tactikon.State;

public class BonusCity extends IBattleBonus
{
	BonusCity(int atkValue, int defValue)
	{
		mName = "City";
		mDefValue = atkValue;
		mAtkValue = defValue;
	}

}
