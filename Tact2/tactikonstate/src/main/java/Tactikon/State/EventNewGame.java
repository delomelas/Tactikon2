package Tactikon.State;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import Support.INewGameOptions;
import Tactikon.State.TactikonNewGameOptions.MirrorType;
import Tactikon.State.TactikonNewGameOptions.WinCondition;

import Core.IEvent;
import Core.InvalidUpdateException;
import Core.IState;

public class EventNewGame extends IEvent
{
	public TactikonNewGameOptions mapInfo;
	
	int VERSION_FIRST = 0;
	int VERSION_WITH_TERRAIN_CONFIG = 1;
	int VERSION_WITH_FRIENDSONLY = 2;
	int mVersion = VERSION_WITH_FRIENDSONLY;
	
	public EventNewGame(INewGameOptions options)
	{
		mapInfo = (TactikonNewGameOptions)options;
	}
	
	public EventNewGame()
	{
		mapInfo = new TactikonNewGameOptions();
	}
	
	@Override
	public IState updateState(IState before) throws InvalidUpdateException
	{
		if (before != null) throw new InvalidUpdateException();
		
		if (mapInfo == null) throw new InvalidUpdateException();
		
		// some validation
		if (mapInfo.bLocalGame == false) mapInfo.numAIPlayers = 0;
		
		int totalPlayers = mapInfo.numHumanPlayers + mapInfo.numAIPlayers;
		
		if (mapInfo.mapSize < 8) throw new InvalidUpdateException();
		if (mapInfo.mapSize > 128) throw new InvalidUpdateException();
		if (totalPlayers > 16) throw new InvalidUpdateException();
		if (totalPlayers < 2) throw new InvalidUpdateException();
		
		TactikonState newState = TactikonState.CreateBlankState(totalPlayers, mapInfo.mapSize, mapInfo.fogOfWar, mapInfo.turnTimeOut * 60 * 60);
		
		newState.gameState = IState.GameStatus.WaitingForPlayers;
		
		MapGenerator mapGenerator = new MapGenerator(mapInfo);
		
		mapGenerator.GenerateMap(newState);
		newState.cities = mapGenerator.AddCities(newState);
		
		newState.winCondition = mapInfo.winCondition;
		
		newState.bLocalGame = mapInfo.bLocalGame;
		
		newState.mGameVersion = newState.mVersion;
		
		newState.bFriendsOnly = mapInfo.bFriendsOnly;
		newState.createdByAlias = mapInfo.createdByAlias;
		newState.createdById = mapInfo.createdById;
		
		newState.aiLevel = mapInfo.aiLevel;
		newState.bTutorial = mapInfo.bTutorial;
		
		newState.IncSequence();
		
		return newState;
	}

	@Override
	public void EventToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(mapInfo.numHumanPlayers);
		stream.writeInt(mapInfo.mapSeed);
		stream.writeInt(mapInfo.mapSize);
		stream.writeInt(mapInfo.mirrorType.ordinal());
		stream.writeFloat(mapInfo.landMassRatio);
		stream.writeFloat(mapInfo.scale);
		stream.writeBoolean(mapInfo.fogOfWar);
		stream.writeInt(mapInfo.winCondition.ordinal());
		stream.writeInt(mapInfo.cities);
		stream.writeInt(mapInfo.turnTimeOut);
		stream.writeBoolean(mapInfo.bForest);
		stream.writeBoolean(mapInfo.bMountains);
		stream.writeBoolean(mapInfo.bFriendsOnly);
		stream.writeUTF(mapInfo.createdByAlias);
		stream.writeInt(mapInfo.createdById);
	}

	@Override
	public void BinaryToEvent(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		if (version >= VERSION_FIRST)
		{
			mapInfo = new TactikonNewGameOptions();
			mapInfo.numHumanPlayers = stream.readInt();
			mapInfo.mapSeed = stream.readInt();
			mapInfo.mapSize = stream.readInt();
			mapInfo.mirrorType = MirrorType.values()[stream.readInt()];;
			mapInfo.landMassRatio = stream.readFloat();
			mapInfo.scale = stream.readFloat();
			mapInfo.fogOfWar = stream.readBoolean();
			mapInfo.winCondition = WinCondition.values()[stream.readInt()];
			mapInfo.cities = stream.readInt();
			mapInfo.turnTimeOut = stream.readInt();
		}
		
		if (version >= VERSION_WITH_TERRAIN_CONFIG)
		{
			mapInfo.bForest = stream.readBoolean();
			mapInfo.bMountains = stream.readBoolean();
		}
		
		if (version >= VERSION_WITH_FRIENDSONLY)
		{
			mapInfo.bFriendsOnly = stream.readBoolean();
			mapInfo.createdByAlias = stream.readUTF();
			mapInfo.createdById = stream.readInt();
		}
		
	}
}
	

