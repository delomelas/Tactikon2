package Network.Packages;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import Network.IPackage;
import Network.PackageResponse;


public class PackageSendChat extends IPackage
{
	// query
	public int fromUserId = -1;
	public int toUserId = -1;
	public int toGameId = -1;
	
	public int chatOTPKey = -1;
	public String messageStr = "";
	public String idToken = "";
	
	//response
	public boolean bSent;
	public boolean bGCMUpdateRequired;
	
	int VERSION_FIRST = 0;
	int mVersion = VERSION_FIRST;
	
	
	@Override
	public void QueryToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		
		stream.writeInt(fromUserId);
		stream.writeInt(toUserId);
		stream.writeInt(toGameId);
		stream.writeInt(chatOTPKey);
		
		// write the encoded message
		
		if (messageStr.length() > 255)
		{
			messageStr = messageStr.substring(0, 256);
		}
		
		stream.writeInt(messageStr.length());
		Random rand = new Random(chatOTPKey);
		
		for (int i = 0; i < messageStr.length(); ++i)
		{
			char c = messageStr.charAt(i);
			char encodedChar = (char) (c ^ rand.nextInt());
			stream.writeChar(encodedChar);
		}
		
		
		stream.writeUTF(idToken);
	}


	@Override
	public void BinaryToQuery(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			fromUserId = stream.readInt();
			toUserId = stream.readInt();
			toGameId = stream.readInt();
			chatOTPKey = stream.readInt();
			
			int length = stream.readInt();
			
			//messageStr = stream.readUTF();
			
			messageStr = "";
			Random rand = new Random(chatOTPKey);
			for (int i = 0; i < length; ++i)
			{
				char c = stream.readChar();
				char decodedChar = (char)(c ^ rand.nextInt());
				messageStr += decodedChar;
			}
			
			idToken = stream.readUTF();
			
		}
	}

	@Override
	public void ResponseToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mVersion);
		stream.writeInt(mReturnCode.ordinal());
		
		stream.writeBoolean(bSent);
		stream.writeBoolean(bGCMUpdateRequired);
	}

	@Override
	public void BinaryToResponse(DataInputStream stream) throws IOException
	{
		int version = stream.readInt();
		
		if (version >= VERSION_FIRST)
		{
			mReturnCode = PackageResponse.values()[stream.readInt()];
			
			bSent = stream.readBoolean();
			bGCMUpdateRequired = stream.readBoolean();
		}
	}

	
}
