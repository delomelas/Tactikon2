package Tactikon.State;

public class Position implements Comparable
{
	public int x, y;
	
	public Position(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Position Add(int x, int y)
	{
		return new Position(this.x + x, this.y + y);
	}
	
	public void Set(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public boolean equals(Object obj)
	{
       // check for reference equality
       if(this == obj) return true;

       // type check
       if( !(obj instanceof Position) ) return false;

       // cast to correct type
       Position p = (Position)obj;

       // compare significant fields
       return (this.x == p.x && this.y == p.y);
	 }

	@Override
	public int compareTo(Object arg0)
	{
		if (this == arg0) return 0;
		
		Position other = (Position)arg0;
		
		return (this.x * 256 + this.y) - (other.x * 256 + other.y);
	}
}
