package Tactikon.State;

import java.util.ArrayList;

public class City
{
	public int cityId;
	public int x;
	public int y;
	public int playerId = -1;
	public boolean startingCity = false;
	
	public ArrayList<Integer> fortifiedUnits = new ArrayList<Integer>();
	public int turnsToProduce = -1;
	public String productionType = "";
	
	public boolean bHasProduced = false;
	
	public boolean bIsHQ = false;
	
	public boolean isPort = false;
	
}
