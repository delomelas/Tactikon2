package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventChangeProduction extends IEvent
{
	public int cityId;
	public String unitClass;
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		if (before.GetGameState() != IState.GameStatus.InGame) throw new InvalidUpdateException();
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		City city = tactikonState.GetCity(cityId);
		if (city == null) throw new InvalidUpdateException();
		
		if (city.playerId !=  tactikonState.playerToPlay) throw new InvalidUpdateException();
		
		IUnit prodUnit = null;
		for (IUnit unit : tactikonState.GetUnitTypes())
		{
			if (unit.getClass().getSimpleName().compareTo(unitClass) == 0) prodUnit = unit;
		}
		if (prodUnit == null) throw new InvalidUpdateException();
		
		city.productionType = unitClass;
		city.turnsToProduce = prodUnit.GetProductionTime(city, tactikonState);
		
		tactikonState.IncSequence();
				
		return tactikonState;
	}
	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(cityId);
		stream.writeUTF(unitClass);
	}
	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		cityId = stream.readInt();
		unitClass = stream.readUTF();
		
	}
}
