package Network;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;


public class PackageHeader
{
	public int mAppVersion;
	public String mAppName;
	public IPackage mp;
	public long mUserId;
	String mClassName;
	
	public PackageHeader(IPackage p, long userId, int appVersion, String appName)
	{
		mAppVersion = appVersion;
		mAppName = appName;
		mp = p;
		mUserId = userId;
	}
	
	
	public void HeaderToBinary(DataOutputStream stream) throws IOException
	{
		stream.writeInt(mAppVersion);
		stream.writeUTF(mAppName);
		stream.writeLong(mUserId);
		stream.writeUTF(mp.getClass().getName());
	}
	
	public void BinaryToHeader(DataInputStream stream) throws IOException
	{
		mAppVersion = stream.readInt();
		mAppName = stream.readUTF();
		mUserId = stream.readLong();
		mClassName = stream.readUTF();
	}
	
	public PackageHeader(DataInputStream stream) throws IOException
	{
		BinaryToHeader(stream);
		
		mp = null;
		
		try
		{
			mp = (IPackage) Class.forName(mClassName).newInstance();
			
		} catch (InstantiationException e)
		{
			mp = null;
			System.out.println("Error: Unable to instansiate class package for " +mClassName +" (InstantiationException)");
		} catch (IllegalAccessException e)
		{
			mp = null;
			System.out.println("Error: Unable to instansiate class package for " +mClassName +" (IllegalAccessException)");
		} catch (ClassNotFoundException e)
		{
			mp = null;
			System.out.println("Error: Unable to instansiate class package for " +mClassName +" (ClassNotFoundException)");
		}
	}
	
	
}
