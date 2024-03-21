package uk.co.eidolon.shared.activities;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.database.SyncListDB.ISyncEvents;
import uk.co.eidolon.shared.network.GCMUtils;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.ColourAdapter;
import uk.co.eidolon.shared.utils.ColourAdapter.ColourItem;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.utils.LogoStore;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.Scopes;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import Network.Packages.PackageLogin;
import Network.Packages.PackageUpdateAccount;
import Network.Packages.PackageUpdateGCM;
import Support.Preferences;
import Network.PackageResponse;
import Network.UserInfo;

public class QueryAccountActivity extends Activity
{
	int mSelectedColour;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.queryaccountactivity);
		
		Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		
		if (accounts.length == 1)
		{
			// use the only account without letting the user know what we're doing...
			Button signInButton = (Button)findViewById(R.id.google_sign_in);
			signInButton.setVisibility(View.GONE);
			EditText accountText = (EditText)findViewById(R.id.identity_text);
			accountText.setVisibility(View.GONE);
			String accountName = accounts[0].name;
			DoLogin(accountName);
			LinearLayout identityTitle = (LinearLayout)findViewById(R.id.identity_text_title);
			identityTitle.setVisibility(View.GONE);
		} else 
		{
			
			Button signInButton = (Button)findViewById(R.id.google_sign_in);
			signInButton.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0)
				{
					Intent intent = AccountPicker.newChooseAccountIntent
							(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
					try
					{
						startActivityForResult(intent, 0);
					}
					catch (ActivityNotFoundException ex)
					{
						// google play services not installed on target device... do something else
					}
				}
	        });
		}
			
		
		EditText accountText = (EditText)findViewById(R.id.identity_text);
		accountText.setClickable(true);
		accountText.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0)
			{
				Intent intent = AccountPicker.newChooseAccountIntent
						(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
				try
				{
					startActivityForResult(intent, 0);
				}
				catch (ActivityNotFoundException ex)
				{
					// google play services not installed on target device... do something else
				}
			}
        });
		accountText.setKeyListener(null);
		
		final ViewPager colourPager = (ViewPager) findViewById(R.id.colour_gallery);
		PagerAdapter adapter = new ColourAdapter(this);
		colourPager.setAdapter(adapter);
		//Necessary or the pager will only have one extra page to show
		// make this at least however many pages you can see
		colourPager.setOffscreenPageLimit(9);
		//A little space between pages
		colourPager.setPageMargin(15);
		 
		//If hardware acceleration is enabled, you should also remove
		// clipping on the pager for its children.
		colourPager.setClipChildren(false);
		
		final ViewPager logoPager = (ViewPager) findViewById(R.id.logo_gallery);
		PagerAdapter logoAdapter = new LogoAdapter(this);
		logoPager.setAdapter(logoAdapter);
		//Necessary or the pager will only have one extra page to show
		// make this at least however many pages you can see
		logoPager.setOffscreenPageLimit(9);
		//A little space between pages
		logoPager.setPageMargin(15);
		 
		//If hardware acceleration is enabled, you should also remove
		// clipping on the pager for its children.
		logoPager.setClipChildren(false);
		
		logoAdapter.notifyDataSetChanged();
		
		
		
		colourPager.setOnPageChangeListener(new OnPageChangeListener() {
		    public void onPageScrollStateChanged(int state)
		    {
		    	
		    }
		    
		    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
		    {
		    	
		    }

		    public void onPageSelected(int position)
		    {
		    	ColourAdapter adapter = (ColourAdapter)colourPager.getAdapter();
		    	ColourItem item = adapter.mColours.get(position);
		    	if (item == null) return;
		    	int col = Color.rgb(item.r, item.g, item.b);
		    	mSelectedColour = col;
		    	
		    	PagerAdapter logoAdapter = logoPager.getAdapter();
		    	logoAdapter.notifyDataSetChanged();
		    	
		    }
		});
		
		final EditText editAlias = (EditText)findViewById(R.id.edit_alias);
		
		editAlias.setOnKeyListener(new OnKeyListener()
		{
		    public boolean onKey(View v, int keyCode, KeyEvent event)
		    {
		        
		    return false;
		    }
		  });
		
		InputFilter alphaNumericFilter = new InputFilter() {   
        @Override  
        public CharSequence filter(CharSequence arg0, int arg1, int arg2, Spanned arg3, int arg4, int arg5)  
        {  
           for (int k = arg1; k < arg2; k++)
           {   
        	   if (!Character.isLetterOrDigit(arg0.charAt(k)))
        	   {   
        		   return "";   
        	   }   
           }   
           return null;   
        }   
      };   

		editAlias.setFilters(new InputFilter[] { new InputFilter.LengthFilter(24), alphaNumericFilter });
		
		editAlias.setOnKeyListener(new OnKeyListener()
		{
	        public boolean onKey(View v, int keyCode, KeyEvent event)
	        {
	            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	                if (editAlias.getText().length() < 6)
	                {
	                	editAlias.setError("Alias must be 6 or more characters");
	                } else if (editAlias.getText().length() > 14)
					{
						editAlias.setError("Alias must be 14 or less characters");
					} else
	                {
	                	editAlias.setError(null);
	                }
	                return true;
	            }
	            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))
		        {
		           UpdateAccountDetails();
		           return true;
		         }
	            return false;
	        }
	    });

		IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
		
		UserInfo info = new UserInfo();		
		info.accountName = appWrapper.GetAccountName();
		info.alias = appWrapper.GetAlias();
		info.colour = appWrapper.GetColour();
		info.logo = appWrapper.GetLogo();
		info.userId = appWrapper.GetUserId();
		SetupOptions(info);
	}
	
	UserInfo GetUserInfo()
	{
		IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
		UserInfo info = new UserInfo();
		info.accountName = appWrapper.GetAccountName();
		info.userId = appWrapper.GetUserId();
		
		EditText aliasText = (EditText) findViewById(R.id.edit_alias);
		info.alias = aliasText.getText().toString();
		
		ViewPager colourPager = (ViewPager) findViewById(R.id.colour_gallery);
		ColourAdapter adapter = (ColourAdapter)colourPager.getAdapter();
    	ColourItem item = adapter.mColours.get(colourPager.getCurrentItem());
    	
    	info.colour = item.colour;
    	
    	ViewPager logoPager = (ViewPager) findViewById(R.id.logo_gallery);
		LogoAdapter logoAdapter = (LogoAdapter)logoPager.getAdapter();
    	String logo = logoAdapter.mLogos.get(logoPager.getCurrentItem());
		
    	info.logo = logo;
    	
    	return info;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.account, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		if (item.getItemId() == R.id.action_done_account)
		{
			final EditText editAlias = (EditText)findViewById(R.id.edit_alias);
			if (editAlias.getText().length() < 6)
			{
				editAlias.setError("Alias must be 6 or more characters.");
				return true;
			} else if (editAlias.getText().length() > 14)
			{
				editAlias.setError("Alias must be 14 or less characters.");
				return true;
			}
            finish();
		}
        
        return super.onOptionsItemSelected(item);
        
    }
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
		
		UserInfo info = GetUserInfo();
		
		if (info.alias.length() >= 6 && info.accountName.length() > 0)
		{
			appWrapper.SetAccountDetails(info);
			UpdateAccountDetails();
		}
    	
    	
	}
	
	@Override
	public void onBackPressed()
	{
		final EditText editAlias = (EditText)findViewById(R.id.edit_alias);
		if (editAlias.getText().length() < 6)
		{
			editAlias.setError("Alias must be 6 or more characters.");
		} else
		{
			super.onBackPressed();
		}
	}
	
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		if (requestCode == 0 && resultCode == RESULT_OK)
	    {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			DoLogin(accountName);
	    }
	}
	

	void SetupOptions(UserInfo info)
	{
		if (info.userId == -1)
		{
			TextView initialText = (TextView)findViewById(R.id.initial_text);
			initialText.setVisibility(View.VISIBLE);
			initialText.setText("Account set up");
			// account has been set yet
			TextView identityText = (TextView)findViewById(R.id.identity_text);
			identityText.setText("");
			Button signInButton = (Button)findViewById(R.id.google_sign_in);
			signInButton.setText("Select Google Account");
			
			LinearLayout layout = (LinearLayout) findViewById(R.id.options_area);
			
			layout.setVisibility(View.GONE);

		} else
		{	TextView initialText = (TextView)findViewById(R.id.initial_text);
			initialText.setText("Account setup:");
			final LinearLayout layout = (LinearLayout) findViewById(R.id.options_area);
			if (layout.getVisibility() == View.GONE)
			{
				layout.setX(-400);
				layout.setVisibility(View.VISIBLE);
				layout.animate()
					.setInterpolator(new AccelerateDecelerateInterpolator())
	            	.translationX(0)
	            	.setDuration(400)
	            	.setListener(null);
			}
			
			
			TextView identityText = (TextView)findViewById(R.id.identity_text);
			identityText.setText(info.accountName);
			Button signInButton = (Button)findViewById(R.id.google_sign_in);
			signInButton.setText("Change");
		}
		
		// populate the UI based on what was returned
		final ViewPager colourPager = (ViewPager) findViewById(R.id.colour_gallery);
		ColourAdapter colourAdapter = (ColourAdapter) colourPager.getAdapter();
		
		int colourIndex = colourAdapter.FindColour(info.colour);
		Random rand = new Random();
		if (colourIndex == -1)
		{
			colourIndex = rand.nextInt(colourAdapter.getCount());
		}
		
		colourPager.setCurrentItem(colourIndex);
		
		ColourItem item = colourAdapter.mColours.get(colourIndex);
    	if (item == null) return;
    	int col = Color.rgb(item.r, item.g, item.b);
    	mSelectedColour = col;
		
		final ViewPager logoPager = (ViewPager) findViewById(R.id.logo_gallery);
		LogoAdapter logoAdapter = (LogoAdapter) logoPager.getAdapter();
		
		int logoIndex = logoAdapter.FindLogo(info.logo);
		if (logoIndex == -1)
		{
			logoIndex = rand.nextInt(logoAdapter.getCount());
		}
		
		logoPager.setCurrentItem(logoIndex);
		logoAdapter.notifyDataSetChanged();
		
		final EditText aliasText = (EditText) findViewById(R.id.edit_alias);
		aliasText.setText(info.alias);
	}
	
	public void UpdateAccountDetails()
	{
		final PackageUpdateAccount p = new PackageUpdateAccount();
		UserInfo info = GetUserInfo();
		p.alias = info.alias;
		p.colour = info.colour;
		p.logo = info.logo;

		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
			}

			@Override
			public void postExecute()
			{
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (p.bAliasAlreadyInUse == true)
					{
						final EditText aliasText = (EditText) findViewById(R.id.edit_alias);
						aliasText.setError("Alias already in use");
						aliasText.requestFocus();
					} else
					{
						final EditText aliasText = (EditText) findViewById(R.id.edit_alias);
						aliasText.setError(null);
					}
				}
			}

			@Override
			public void postExecuteBackground()
			{
				// TODO Auto-generated method stub
				
			}

		});
		
		sender.Send();
		
	}
	
	private static final ScheduledExecutorService worker = 
			  Executors.newSingleThreadScheduledExecutor();

	
	private boolean isNetworkAvailable()
	{
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	public void DoLogin(final String accountName)
	{
		
		final PackageLogin p = new PackageLogin();
		p.accountName = accountName;
		
		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
				IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
				
		
				// don't do this if we don't have a network connection... :-)
				if (isNetworkAvailable() == true)
				{
					UserInfo info = new UserInfo();
					info.userId = -1;
					info.accountName = "";
					info.colour = 0;
					info.logo = "";
					info.alias = "";
					appWrapper.SetAccountDetails(info);
				}
				
				
				QueryAccountActivity.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
				Button signInButton = (Button)findViewById(R.id.google_sign_in);
				signInButton.setEnabled(false);
			}

			@Override
			public void postExecute()
			{
				Button signInButton = (Button)findViewById(R.id.google_sign_in);
				signInButton.setEnabled(true);
				QueryAccountActivity.this.setProgressBarIndeterminateVisibility(Boolean.FALSE);
				
				//LinearLayout progressAreaLayout = (LinearLayout)findViewById(R.id.progress_area);
				
				if (p.mReturnCode != PackageResponse.Success)
				{
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
					    DoLogin(accountName);
					  }
					}, 1000);
					TextView initialText = (TextView)findViewById(R.id.initial_text);
					initialText.setVisibility(View.VISIBLE);
					initialText.setText("Account set up - network error communicating with server");
				} else if (p.mReturnCode == PackageResponse.Success)
				{
					TextView initialText = (TextView)findViewById(R.id.initial_text);
					initialText.setVisibility(View.VISIBLE);
					initialText.setText("Account set up");
					//progressAreaLayout.setVisibility(View.GONE);
					IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
					UserInfo info = new UserInfo();
					info.userId = p.userId;
					info.accountName = p.accountName;
					info.colour = p.colour;
					info.logo = p.logo;
					info.alias = p.alias;
					appWrapper.SetAccountDetails(info);

					SetupOptions(info);
					
					SyncListDB.getInstance(QueryAccountActivity.this).SyncWithServer("GameList", (int)info.userId, null);
					
					// notify that the state list has been updated
					String updateStatesIntent = appWrapper.GetStateUpdatedIntentAction();
					Intent intent = new Intent(updateStatesIntent);
					intent.putExtra("GameListChanged", "true");
					LocalBroadcastManager.getInstance(QueryAccountActivity.this).sendBroadcast(intent);
					
					// and also update the GCM Reg id
					final PackageUpdateGCM gcmPackage = new PackageUpdateGCM();
					GCMUtils gcmUtils = new GCMUtils(QueryAccountActivity.this);
					gcmPackage.gcmRegId = gcmUtils.getRegistrationId(QueryAccountActivity.this);
					gcmPackage.UserId = p.userId;
					PackageDelivery gcmSender = new PackageDelivery(QueryAccountActivity.this, gcmPackage, new ISendEvents(){

						@Override
						public void preExecute()
						{
						}

						@Override
						public void postExecute()
						{
						}

						@Override
						public void postExecuteBackground()
						{
							/*
							// TODO Auto-generated method stub
							try
							{
								String result = GoogleAuthUtil.getTokenWithNotification(QueryAccountActivity.this, "james.payne.eidolon@gmail.com", "oauth2:openid", null);
								Log.i("GameLib", "AuthToken: " + result);
							} catch (UserRecoverableAuthException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (GoogleAuthException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							*/
						}
					
					});
					gcmSender.Send();
					
				}
			}

			@Override
			public void postExecuteBackground()
			{
				// TODO Auto-generated method stub
				
			}

		});
		
		sender.Send();
	}
	
	
	
		 
	 private class LogoAdapter extends PagerAdapter
	 {
		 Context mContext;
		 
		 ArrayList<String> mLogos = new ArrayList<String>();
		 
		 int FindLogo(String logo)
		 {
			 for (int i = 0; i < mLogos.size(); ++i)
			 {
				 if (mLogos.get(i) == null) continue;
				 if (logo.compareTo(mLogos.get(i)) == 0) return i;
			 }
			 return -1;
		 }
		 
		 LogoAdapter(Context context)
		 {
			mContext = context;
			IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
			LogoStore logoStore = appWrapper.GetLogoStore();
			String[] fileNames = logoStore.GetLogoList();
			
			 for(String name:fileNames)
			 {
			      mLogos.add(name);
			 }

		 }
		 
		 @Override
		 public Object instantiateItem(ViewGroup container, int position)
		 {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			 
			ImageView view = (ImageView)inflater.inflate(R.layout.logo_item, null);
			String item = mLogos.get(position);
			if (item == null) return view;
			
			IAppWrapper appWrapper = (IAppWrapper)QueryAccountActivity.this.getApplication();
			LogoStore logoStore = appWrapper.GetLogoStore();
			
			Drawable d = logoStore.GetLogo(item);
			
			// set image to ImageView
			view.setImageDrawable(d);
			view.setBackgroundColor(mSelectedColour);
			 
			container.addView(view);
			return view;
		 }
		  
		 @Override
		 public void destroyItem(ViewGroup container, int position, Object object)
		 {
			 container.removeView((View)object);
		 }
		  
		 @Override
		 public int getCount()
		 {
			 return mLogos.size();
		 }
		  
		 @Override
		 public boolean isViewFromObject(View view, Object object)
		 {
			 return (view == object);
		 }
		 
		 public int getItemPosition(Object item)
		 {
			 ImageView image = (ImageView)item;
			 Drawable drawable = image.getDrawable();
			 //drawable.setColorFilter(mSelectedColour, Mode.MULTIPLY);
			 image.setBackgroundColor(mSelectedColour);
			 
			 return POSITION_UNCHANGED;
		 }
		 
	

		 }
	 
	 
		
}
