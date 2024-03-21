package Core;

public class InvalidUpdateException extends Exception
{
	private static final long serialVersionUID = 138548589603585503L;

	//Parameterless Constructor
    public InvalidUpdateException() {}

    //Constructor that accepts a message
    public InvalidUpdateException(String message)
    {
       super(message);
    }
}
