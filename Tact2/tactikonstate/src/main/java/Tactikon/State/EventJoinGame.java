package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;
import Core.PlayerInfo;

public class EventJoinGame extends IEvent
{
	public int playerIdToJoin = -1;
	public int r, g, b;
	public String logo;
	public String name;
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before == null) throw new InvalidUpdateException("Null state");
		if (before.GetGameState() != IState.GameStatus.WaitingForPlayers)
		{
			throw new InvalidUpdateException("Not waiting for players");
		}
		
		TactikonState tactikonState = (TactikonState)before.CopyState();
		
		boolean joined = false;
		
		// check player isn't already in this game
		for (int i = 0; i < tactikonState.players.size(); ++i)
		{
			if (tactikonState.players.get(i) == playerIdToJoin)
			{
				throw new InvalidUpdateException("Player already in this game");
			}
		}
		
		// check the colour hasn't already been used
		for (Integer player: tactikonState.GetPlayers())
		{
			PlayerInfo info = tactikonState.GetPlayerInfo(player);
			if (info.r == r && info.g == g && info.b == b)
			{
				throw new InvalidUpdateException("Player colour already in use.");
			}
		}
		
		// add player to a free slot if there is one
		for (int i = 0; i < tactikonState.players.size(); ++i)
		{
			if (tactikonState.players.get(i) == -1)
			{
				// add player to game
				tactikonState.players.set(i, playerIdToJoin);
				
				// make one of the starting cities the players
				for (City city : tactikonState.cities)
				{
					if (city.startingCity == true && city.playerId == -1)
					{
						city.playerId = playerIdToJoin;
						city.bIsHQ = true;
						
						
						
						if (city.isPort == true)
						{
							UnitTank tank = new UnitTank();
							tank.mUserId = playerIdToJoin;
							tank.SetPosition(city.x, city.y);
							tank.mFortified = true;
							
							int tankId = tactikonState.CreateUnit(tank);
							city.fortifiedUnits.add(tankId);
							
							UnitBoatTransport transport = new UnitBoatTransport();
							transport.mUserId = playerIdToJoin;
							transport.SetPosition(city.x,  city.y);
							transport.mFortified = true;
							int transportId = tactikonState.CreateUnit(transport);
							city.fortifiedUnits.add(transportId);
						} else
						{
							UnitInfantry infantry = new UnitInfantry();
							infantry.mUserId = playerIdToJoin;
							infantry.SetPosition(city.x, city.y);
							infantry.mFortified = true;
							int infId = tactikonState.CreateUnit(infantry);
							city.fortifiedUnits.add(infId);
							
							
							UnitHelicopter chopper = new UnitHelicopter();
							chopper.mUserId = playerIdToJoin;
							chopper.SetPosition(city.x,  city.y);
							chopper.mFortified = true;
							int chopperId = tactikonState.CreateUnit(chopper);
							city.fortifiedUnits.add(chopperId);
							
						}
						
						
						break;
					}
				}
				joined = true;
				break;
			}
		}
		
		// no space for new players
		if (joined == false) throw new InvalidUpdateException("No space to join game");
		
		PlayerInfo info = new PlayerInfo();
		info.userId = playerIdToJoin;
		info.r = r;
		info.g = g;
		info.b = b;
		info.name = name;
		info.logo = logo;
		tactikonState.playerInfo.put((long)playerIdToJoin, info);
		
		// if we've added all the players for this game, set the gamestate to "playing"
		boolean allPlaying = true;
		for (int i = 0; i < tactikonState.players.size(); ++i)
		{
			if (tactikonState.players.get(i) == -1) allPlaying = false;
		}
		
		if (allPlaying == true)
		{
			tactikonState.gameState = IState.GameStatus.InGame;
			tactikonState.playerToPlay = tactikonState.players.get(0); // set the first player to join as the first to play
		}
		
		tactikonState.IncSequence();
		
		return tactikonState;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(playerIdToJoin);
		stream.writeUTF(name);
		stream.writeUTF(logo);
		stream.writeInt(r);
		stream.writeInt(g);
		stream.writeInt(b);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		playerIdToJoin = stream.readInt();
		name = stream.readUTF();
		logo = stream.readUTF();
		r = stream.readInt();
		g = stream.readInt();
		b = stream.readInt();
	}
	
}
