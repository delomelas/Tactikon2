package uk.co.eidolon.shared.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import uk.co.eidolon.shared.utils.IAppWrapper;

import Network.IPackage;
import Network.PackageHeader;
import Network.PackageResponse;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import javax.net.ssl.*;
import java.security.cert.*;
import java.security.SecureRandom;

public class PackageDelivery
{
	IPackage mPackage;
	ISendEvents mEvents;
	Context mContext;

	private static class AcceptAllTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() { return null; }
	}
	
	public PackageDelivery(Context context, IPackage p, ISendEvents sendEvents)
	{
		mPackage = p;
		mEvents = sendEvents;
		mContext = context;
	}
	
	public int Send()
	{
		new SendPackageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return 0;
	}
	
	private boolean isNetworkAvailable()
	{

	    return false;
	}
	
	
	public PackageResponse DoSend()
	{
		if (isNetworkAvailable() == false)
		{
			mPackage.mReturnCode = PackageResponse.ErrorNoNetwork;
			return PackageResponse.ErrorNoNetwork;
		}
		
		Socket socket;
		
		try
		{


			SSLContext sslctx = SSLContext.getInstance("TLS");
			sslctx.init(new KeyManager[0], new TrustManager[] {new AcceptAllTrustManager()}, new SecureRandom());
			SSLSocketFactory factory = sslctx.getSocketFactory();
			socket = (SSLSocket) factory.createSocket("tactikon.co.uk", 8443);

			new NotificationUtils().IncLoadingStack(mContext);
			//socket = new Socket("gameserver.tactikon.co.uk", 443);
			socket.setSoTimeout(10000);

			DataInputStream input = new DataInputStream(new InflaterInputStream(socket.getInputStream()));
			DeflaterOutputStream deflater = new DeflaterOutputStream(socket.getOutputStream());
			DataOutputStream output = new DataOutputStream(deflater);
			
			PackageHeader header = PackageHeaderFactory.GetPackageHeader(mContext, mPackage);
			
			header.HeaderToBinary(output);
			mPackage.QueryToBinary(output);
			deflater.finish();
			deflater.flush();
			
			mPackage.BinaryToResponse(input);
			
			socket.close();
		} catch (Exception e)
		{
			new NotificationUtils().DecLoadingStack(mContext);
			mPackage.mReturnCode = PackageResponse.ErrorNoNetwork;
			return PackageResponse.ErrorIOError;
		}
		
		new NotificationUtils().DecLoadingStack(mContext);
		
		return PackageResponse.Success;
		
	}
	
	
	private class SendPackageTask extends AsyncTask<String, Void, PackageResponse>
	{
		@Override
		protected void onPreExecute()
		{
			if (mEvents != null)
			{
				mEvents.preExecute();
			}
		}
		
		@Override
		protected void onPostExecute(PackageResponse i)
		{
			if (mEvents != null)
			{
				mEvents.postExecute();
			}
		}
		
		
		@Override
		protected PackageResponse doInBackground(String... string)
		{
			PackageResponse response = DoSend();
			if (mEvents != null) mEvents.postExecuteBackground();
			return response;
		}
	}
}
