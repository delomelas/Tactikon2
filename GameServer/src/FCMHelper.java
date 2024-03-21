import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class FCMHelper 

{
	public final static String AUTH_KEY_FCM = "FCM_TOKEN_HERE";
	public final static String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
	
	public static int sendPushNotification(JSONObject message, String deviceToken)
	        throws IOException
	{
	    int result = 0;
	    URL url = new URL(API_URL_FCM);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	
	    conn.setUseCaches(false);
	    conn.setDoInput(true);
	    conn.setDoOutput(true);
	
	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
	    conn.setRequestProperty("Content-Type", "application/json");
	
	    JSONObject json = new JSONObject();
	
	    json.put("to", deviceToken.trim());
	    JSONObject info = new JSONObject();
	                                                           // body
	    json.put("data", message);
	    try {
	        OutputStreamWriter wr = new OutputStreamWriter(
	                conn.getOutputStream());
	        wr.write(json.toString());
	        wr.flush();
	
	        BufferedReader br = new BufferedReader(new InputStreamReader(
	                (conn.getInputStream())));
	
	        String output;
	        System.out.println("Output from Server .... \n");
	        while ((output = br.readLine()) != null) {
	            System.out.println(output);
	        }
	        result = 0;
	    } catch (Exception e) {
	        e.printStackTrace();
	        result = 1;
	    }
	    System.out.println("GCM Notification is sent successfully");
	
	    return result;
	}
	
	public static int sendMultipleDataMessages(JSONObject message, ArrayList<String> tokens) throws IOException
	{
		int fails = 0;
		for (String token : tokens)
		{
			int results = sendPushNotification(message, token);
			fails = fails + results;
		}
		return fails;
	}

}
