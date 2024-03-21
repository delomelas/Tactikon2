package uk.co.eidolon.tact2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AboutActivity extends Activity
{


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.about_activity);
		
		Button reviewButton = (Button)findViewById(R.id.review_button);
		
		reviewButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=uk.co.eidolon.tact2"));
				startActivity(intent);
				
			}});
		
		Button feedbackButton = (Button)findViewById(R.id.feedback_button);
		feedbackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
			            "mailto","tactikon@eidolon.co.uk", null));
			emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Tactikon2 feedback...");
			startActivity(Intent.createChooser(emailIntent, "Send feedback..."));
				
			}});

	}
	

	
	@Override
	public void onDestroy() {
	   super.onDestroy();

	}
}
