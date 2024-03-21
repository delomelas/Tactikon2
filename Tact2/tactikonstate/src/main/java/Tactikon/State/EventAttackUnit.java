package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Tactikon.State.Battle.Outcome;

import Core.IEvent;
import Core.IState;
import Core.InvalidUpdateException;

public class EventAttackUnit extends IEvent
{
	public int attackingUnitId;
	public int defendingUnitId;
	
	public EventAttackUnit()
	{
		
	}
	
	public EventAttackUnit(int attackingUnitId, int defendingUnitId)
	{
		this.attackingUnitId = attackingUnitId;
		this.defendingUnitId = defendingUnitId;
	}

	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException();
		
		// check that it's a valid move for that unit
		TactikonState state = (TactikonState)before.CopyState();
		IUnit attacker = state.GetUnit(attackingUnitId);
		IUnit defender = state.GetUnit(defendingUnitId);
		
		// Validation
		if (attacker == null) throw new InvalidUpdateException();
		if (defender == null) throw new InvalidUpdateException();
		if (attacker.mUserId != state.playerToPlay) throw new InvalidUpdateException();
		if (attacker.mUserId == defender.mUserId) throw new InvalidUpdateException();
		if (attacker.bAttacked == true) throw new InvalidUpdateException();
		
		Battle battle = new Battle(state, attackingUnitId, defendingUnitId);
		int randomNumber = state.GetRandomNumber(1000);
		
		float val = (float)randomNumber / 1000.0f;
		float count = 0;
		Outcome useOutcome = null;
		for (Outcome outcome : battle.out)
		{
			count = count + outcome.probability;
			if (count >= val) 
			{
				useOutcome = outcome;
				break;
			}
		}
		if (useOutcome == null)
		{
			useOutcome = battle.out.get(0);
			
		}
		
		int attLoss = 0;
		int defLoss = 0;
		if (useOutcome.unitId1 == attackingUnitId)
		{
			attLoss = useOutcome.unitId1Damage;
			defLoss = useOutcome.unitId2Damage;
		} else
		{
			attLoss = useOutcome.unitId2Damage;
			defLoss = useOutcome.unitId1Damage;
		}
		
		attacker.health = attacker.health - attLoss;
		defender.health = defender.health - defLoss;
		
		if (attacker.health == 0)
		{
			defender.kills ++;
			if (defender.kills > 3) defender.mIsVeteran = true;
			state.KillUnit(attackingUnitId);
		}
		
		if (defender.health == 0)
		{
			attacker.kills ++;
			if (attacker.kills > 3) attacker.mIsVeteran = true;
			BubbleInfoUnitLost bubbleInfo = new BubbleInfoUnitLost(state.GetUnit(defendingUnitId));
			state.bubbles.add(bubbleInfo);
			BubbleInfoUnitLost2 bubbleInfo2 = new BubbleInfoUnitLost2(state.GetUnit(defendingUnitId), state.GetUnit(attackingUnitId));
			state.bubbles2.add(bubbleInfo2);
			
			state.KillUnit(defendingUnitId);
		}
		
		attacker.bAttacked = true;
		
		state.CheckForWinner();
		
		state.IncSequence();
		
		return state;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(attackingUnitId);
		stream.writeInt(defendingUnitId);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		attackingUnitId = stream.readInt();
		defendingUnitId = stream.readInt();
		
	}

}
