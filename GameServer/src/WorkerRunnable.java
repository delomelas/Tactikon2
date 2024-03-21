
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


import Network.PackageHeader;
import Network.PackageResponse;
import Network.UserInfo;
import Network.Packages.PackageEventList;
import Network.Packages.PackageGetGame;
import Network.Packages.PackageGetProfile;
import Network.Packages.PackageJoinGame;
import Network.Packages.PackageLogin;
import Network.Packages.PackageNewGame;
import Network.Packages.PackageSearchAlias;
import Network.Packages.PackageSearchGames;
import Network.Packages.PackageSendChat;
import Network.Packages.PackageSendInvite;
import Network.Packages.PackageSyncList;
import Network.Packages.PackageUpdateAccount;
import Network.Packages.PackageUpdateGCM;


public class WorkerRunnable implements Runnable
{
	
	GameProcessor gameProcessor = new GameProcessor();

	protected Socket clientSocket = null;
	
	public WorkerRunnable(Socket clientSocket)
	{
		this.clientSocket = clientSocket;
	}

	public void run() 
	{
		try
		{
			clientSocket.setSoTimeout(20000);
			
			DataInputStream input = new DataInputStream(new InflaterInputStream((clientSocket.getInputStream())));
			DeflaterOutputStream deflater = new DeflaterOutputStream(clientSocket.getOutputStream(), true);
			DataOutputStream output = new DataOutputStream(deflater);
			
			// read package header
			PackageHeader header = new PackageHeader(input);
			
			System.out.println("Package: [" + header.mp.getClass().getName() + 
					"] IP: [" + clientSocket.getInetAddress().toString() +
					"] UserId: [" + header.mUserId +
					"] Version: [" + header.mAppVersion +
					"] App: [" + header.mAppName + "]");
			
			header.mp.BinaryToQuery(input);
			
			// if we've got a login package, make sure the account exists
			if (header.mp.getClass() == PackageLogin.class)
			{
				PackageLogin loginPackage = (PackageLogin)header.mp;
				UserInfo dummy = new UserInfo();
				dummy.userId = header.mUserId;
				dummy.accountName = loginPackage.accountName;
				
				String alias = loginPackage.accountName;
				int index = alias.lastIndexOf("@");
				if (index > 0) alias = alias.substring(0, index);
				dummy.alias = alias;
				
				dummy.colour = 0;
				dummy.logo = "random";
				FindOrCreateUser(dummy);
				
				loginPackage.mReturnCode = PackageResponse.Success;
				loginPackage.userId = dummy.userId; // tell the user what their userId is
				loginPackage.alias = dummy.alias;
				loginPackage.accountName = dummy.accountName;
				loginPackage.colour = dummy.colour;
				loginPackage.logo = dummy.logo;
				
				header.mUserId = dummy.userId;
				
				// update the registration id in the db
			}
			
			UserInfo userInfo = DBStuff.getInstance().GetUserInfo(header.mUserId);
			if (userInfo == null)
			{
				// user not registered - return error and exit immediately
				header.mp.mReturnCode = PackageResponse.ErrorNoUser;
				header.mp.ResponseToBinary(output);
				output.flush();
				clientSocket.close();
				return;
			}
			
			if (header.mp.getClass() == PackageUpdateGCM.class)
			{
				PackageUpdateGCM gcmPackage = (PackageUpdateGCM)header.mp;
				System.out.println("INFO: Updating GCM registration for user: " + gcmPackage.UserId + " : " + gcmPackage.gcmRegId);
				DBStuff.getInstance().UpdateGCMRegId(gcmPackage.UserId, gcmPackage.gcmRegId);
				gcmPackage.mReturnCode = PackageResponse.Success;
			}
			
			if (header.mp.getClass()== PackageNewGame.class)
			{
				gameProcessor.DoPackageNewGame(userInfo, (PackageNewGame)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass()== PackageSearchGames.class)
			{
				gameProcessor.DoPackageSearchGames(userInfo, (PackageSearchGames)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass()== PackageJoinGame.class)
			{
				PackageJoinGame p = (PackageJoinGame)header.mp;
				gameProcessor.DoPackageJoinGame(userInfo, p, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass()== PackageGetGame.class)
			{
				gameProcessor.DoPackageGetGame(userInfo, (PackageGetGame)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass()== PackageEventList.class)
			{
				gameProcessor.DoPackageEventList(userInfo, (PackageEventList)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageSyncList.class)
			{
				gameProcessor.DoPackageSyncList(userInfo,  (PackageSyncList)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageSendInvite.class)
			{
				gameProcessor.DoPackageSendInvite(userInfo, (PackageSendInvite)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageGetProfile.class)
			{
				gameProcessor.DoPackageGetProfile(userInfo,  (PackageGetProfile)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageUpdateAccount.class)
			{
				gameProcessor.DoPackageUpdateAccount(userInfo,  (PackageUpdateAccount)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageSearchAlias.class)
			{
				gameProcessor.DoPackageSearchAlias(userInfo,  (PackageSearchAlias)header.mp, header.mAppVersion, header.mAppName);
			}
			
			if (header.mp.getClass() == PackageSendChat.class)
			{
				gameProcessor.DoPackageSendChat(userInfo,  (PackageSendChat)header.mp, header.mAppVersion, header.mAppName);
			}
			
			// and send the response to the client
			header.mp.ResponseToBinary(output);
			
			deflater.finish();
			output.flush();
			
			clientSocket.close();

		} catch (IOException e)
		{
			System.out.println("IOException - " + e.getMessage());
		} 

	}
	
	void FindOrCreateUser(UserInfo searchUserInfo)
	{
		UserInfo userInfo = DBStuff.getInstance().GetUserInfo(searchUserInfo.userId);
		if (userInfo == null)
		{
			userInfo = DBStuff.getInstance().GetUserInfo(searchUserInfo.accountName);
			if (userInfo == null)
			{
				userInfo = DBStuff.getInstance().AddUserInfo(searchUserInfo);
				System.out.println("Added user: [" + searchUserInfo.accountName + "] with userId: [" + userInfo.userId + "]");
			}
		}
		
		searchUserInfo.userId = userInfo.userId;
		searchUserInfo.alias = userInfo.alias;
		searchUserInfo.colour = userInfo.colour;
		searchUserInfo.logo = userInfo.logo;
	}

}