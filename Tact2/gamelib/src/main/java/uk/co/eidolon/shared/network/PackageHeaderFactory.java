package uk.co.eidolon.shared.network;

import uk.co.eidolon.shared.utils.IAppWrapper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import Network.IPackage;
import Network.PackageHeader;
import Network.UserInfo;

public class PackageHeaderFactory
{
	static public PackageHeader GetPackageHeader(Context context, IPackage p)
	{
		String appName;
		int appVersion;
		
		UserInfo userInfo = new UserInfo();
		
		PackageInfo pInfo = null;
		try
		{
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e)
		{
		}
		
		if (pInfo != null)
		{
			appVersion = pInfo.versionCode;
			appName = pInfo.packageName;
		} else
		{
			appVersion = -1;
			appName = "[unknown]";
		}

		IAppWrapper appWrapper = (IAppWrapper)context.getApplicationContext();
		userInfo.userId = appWrapper.GetUserId();
		
		PackageHeader header = new PackageHeader(p, userInfo.userId, appVersion, appName);
		return header;
	}
}
