package uk.co.eidolon.shared.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import Core.IState;
import Network.PackageResponse;
import Network.Packages.PackageSearchGames;
import uk.co.eidolon.gamelib.R;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.EndlessScrollListener;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.IStateInfo;

public class SearchResultsActivity extends Activity
{
	ListView mList;
	Context mContext;
	
	GameListAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_search_results);
		
		mContext = this;
		
		mList = (ListView)findViewById(R.id.resultslist);
		
		ArrayList<IStateInfo> infoList = new ArrayList<IStateInfo>(); 
		
        mAdapter = new GameListAdapter(mContext, 0, infoList);
		
		mList.setAdapter(mAdapter);
		
		mList.setOnScrollListener(new EndlessScrollListener()
		{
	        @Override
	        public void onLoadMore(int page, int totalItemsCount)
	        {
	            customLoadMoreData(page); 
	        }
	    });
		
		mList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				Intent joinGameIntent = new Intent(mContext, JoinGameActivity.class);
				
				IStateInfo stateInfo = mAdapter.getItem(arg2);
				
				IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
				appWrapper.StoreState(stateInfo.GetState(), "GameToJoin");
				
				joinGameIntent.putExtra("GameID", stateInfo.GetGameID());
				
	        	startActivity(joinGameIntent);
	        	finish();
			}
		});
		
	}

    // Append more data into the adapter
    public void customLoadMoreData(final int page)
    {
    	final PackageSearchGames p = new PackageSearchGames();
		
		p.maxResults = 12;
		p.searchPlayerId = -1;
		p.searchState = IState.GameStatus.WaitingForPlayers;
		p.startOffset = (page - 1) * p.maxResults;
		
		PackageDelivery sender = new PackageDelivery(this, p, new ISendEvents(){

			@Override
			public void preExecute()
			{
				SearchResultsActivity.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
				
				LinearLayout progressAreaLayout = (LinearLayout)findViewById(R.id.progress_area);
				if (mAdapter.getCount() == 0)
				{
					progressAreaLayout.setVisibility(View.VISIBLE);
					TextView progressText = (TextView)findViewById(R.id.text_progress);
					progressText.setText("Please wait while searching for available games...");
				}
			}

			@Override
			public void postExecute()
			{
				SearchResultsActivity.this.setProgressBarIndeterminateVisibility(Boolean.FALSE);
				
				LinearLayout progressAreaLayout = (LinearLayout)findViewById(R.id.progress_area);
				
				if (p.mReturnCode == PackageResponse.Success)
				{
					if (mAdapter.getCount() == 0 && p.numReturned == 0)
					{
						progressAreaLayout.setVisibility(View.VISIBLE);
						TextView progressText = (TextView)findViewById(R.id.text_progress);
						progressText.setText("Sorry, there aren't any games available to join. Why not start a new one?");
						
						Button newgameButton = (Button)findViewById(R.id.new_game);
						newgameButton.setVisibility(View.VISIBLE);
						
						newgameButton.setOnClickListener(new OnClickListener(){

							@Override
							public void onClick(View arg0)
							{
								// TODO Auto-generated method stub
								Intent newGameIntent = new Intent(SearchResultsActivity.this, uk.co.eidolon.shared.activities.NewGameActivity.class);
					        	newGameIntent.putExtra("Network", 1);
					        	startActivity(newGameIntent);
					        	finish();
					        	
							}});
						
					} else
					{
						progressAreaLayout.setVisibility(View.GONE);
					}
	
					if (p.numReturned > 0)
					{
						if (p.resultGameIDs.size() != p.numReturned) return;  // should never happen, but avoid a crash if it does
						if (p.resultData.size() != p.numReturned) return; // should never happen, but avoid a crash if it does
						for (int i = 0; i < p.numReturned; ++i)
						{
							IAppWrapper appWrapper = (IAppWrapper)getApplicationContext();
							int UserID = appWrapper.GetUserId();
							
							// filter games we've already joined
							boolean bAlreadyAdded = false;
							for (IStateInfo info : mAdapter.mItems)
							{
								if (info.GetGameID() == p.resultGameIDs.get(i)) bAlreadyAdded = true;
							}
							if (bAlreadyAdded == true) continue;
							
							if (p.resultData.get(i).GetPlayers().contains(UserID) == false)
							{
								mAdapter.add(appWrapper.StateInfoFactory(p.resultGameIDs.get(i), UserID, p.resultData.get(i)));
							}
						}

						if (mAdapter.getCount() == 0)
						{
							progressAreaLayout.setVisibility(View.VISIBLE);
							TextView progressText = (TextView)findViewById(R.id.text_progress);
							progressText.setText("Sorry, there aren't any games available to join. Why not start a new one?");

							Button newgameButton = (Button)findViewById(R.id.new_game);
							newgameButton.setVisibility(View.VISIBLE);

							newgameButton.setOnClickListener(new OnClickListener(){

								@Override
								public void onClick(View arg0)
								{
									// TODO Auto-generated method stub
									Intent newGameIntent = new Intent(SearchResultsActivity.this, uk.co.eidolon.shared.activities.NewGameActivity.class);
									newGameIntent.putExtra("Network", 1);
									startActivity(newGameIntent);
									finish();

								}});

						}
					}
				} else 
				{
					progressAreaLayout.setVisibility(View.VISIBLE);
					TextView progressText = (TextView)findViewById(R.id.text_progress);
					progressText.setText("Sorry, there was a network error while fetching the results.");
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
		
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search_results, menu);
		return true;
	}
	
	public class GameListAdapter extends ArrayAdapter<IStateInfo>
	{
		
	    private ArrayList<IStateInfo> mItems;
	    
	    Context mContext;

	    public GameListAdapter(Context context, int textViewResourceId, ArrayList<IStateInfo> items)
	    {
	            super(context, textViewResourceId, items);
	            this.mItems = items;
	            mContext = context;
	    }
	    
	    void UpdateItem(Integer position)
	    {
	    	IStateInfo o = mItems.get(position );
	    	View v = mList.getChildAt(position - mList.getFirstVisiblePosition());
	    	
	    	o.PopulateView(mContext, v);
	    }
	    
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	View view = convertView;
	    	IStateInfo o = mItems.get(position);
	    	if (view == null)
	    	{
	    		view = o.InflateView(mContext);
	    	}
	    	o.PopulateView(mContext, view);
	    	
	        
	        return view;
	    }
	}
	
	protected void onListItemLongClick(int position)
	{
		// launch the context menu...
	}
	
	protected void onListItemClick(int position)
	{
		
	}
	
}
