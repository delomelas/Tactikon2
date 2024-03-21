package Tactikon.State; 

// tracks for units
public class Tracks
{
	public byte type = 0; //0 = air, 1 = wake, 2 = footprints, 3 = tracks
	public byte[] x;
	public byte[] y;
	public int numPoints;
	public int renderPoints;
	public int playerId;
}