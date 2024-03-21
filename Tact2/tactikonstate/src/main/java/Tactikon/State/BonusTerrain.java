package Tactikon.State;

public class BonusTerrain extends IBattleBonus
{
	int mTileType;
	BonusTerrain(int tile, int atkValue, int defValue)
	{
		mDefValue = defValue;
		mAtkValue = atkValue;
		mTileType = tile;
		mName = "Terrain";
	}

}
