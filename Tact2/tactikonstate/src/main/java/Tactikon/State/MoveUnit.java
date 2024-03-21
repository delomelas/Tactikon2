package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class MoveUnit extends IEvent
{
	IUnit unit1;
	IUnit unit2;
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
}
