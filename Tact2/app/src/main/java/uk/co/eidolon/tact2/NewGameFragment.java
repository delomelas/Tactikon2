package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// import com.google.android.gms.drive.internal.r;

import uk.co.eidolon.shared.activities.JoinGameActivity;
import uk.co.eidolon.shared.database.GameDatabase;
import uk.co.eidolon.shared.database.SyncListDB;
import uk.co.eidolon.shared.network.PackageDelivery;
import uk.co.eidolon.shared.utils.ColourAdapter;
import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.shared.views.GameListView;
import uk.co.eidolon.shared.views.IStateInfo;

import Core.IState;
import Core.InvalidUpdateException;
import Core.PlayerInfo;
import Network.PackageResponse;
import Network.SyncList;
import Network.Packages.PackageNewGame;
import Support.INewGameOptions;
import Tactikon.State.EventJoinGame;
import Tactikon.State.EventNewGame;
import Tactikon.State.TactikonNewGameOptions;
import Tactikon.State.TactikonNewGameOptions.AILevel;
import Tactikon.State.TactikonNewGameOptions.MirrorType;
import Tactikon.State.TactikonNewGameOptions.WinCondition;
import Tactikon.State.TactikonState;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import uk.co.eidolon.shared.network.ISendEvents;
import uk.co.eidolon.shared.network.PackageDelivery;

public class NewGameFragment extends Fragment
{
	static TactikonNewGameOptions mOptions;
	
	boolean mCreateEnabled = true;
	
	boolean mNetwork = false;
	
	void SetDetails(boolean bNetwork)
	{
		mNetwork = bNetwork;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Random rand = new Random();
		
		
		if (mOptions == null)
		{
			mOptions = new TactikonNewGameOptions();
			mOptions.mirrorType = MirrorType.None;
			mOptions.cities = 1;
			mOptions.landMassRatio = 0.25f;
			mOptions.numHumanPlayers = 2;
			mOptions.scale = 6;
			mOptions.mapSize = 32;
			mOptions.fogOfWar = false;
			mOptions.winCondition = WinCondition.CaptureAllBases;
			
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			mOptions.createdByAlias = appWrapper.GetAlias();
			mOptions.createdById = (int)appWrapper.GetUserId();
			
			
		}
		
		mOptions.mapSeed = rand.nextInt();
		
		if (mNetwork == true)
		{
			mOptions.bLocalGame = false;
		} else
		{
			mOptions.bLocalGame = true;
			mOptions.bFriendsOnly = false;
		}

		
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}
	
	@Override
	public void onResume()
	{		
		super.onResume();
		
		ImageView imageView = (ImageView) getView().findViewById(R.id.map_image);
        LayoutParams params = imageView.getLayoutParams();
        params.width = LayoutParams.FILL_PARENT;
        params.height = params.width ;
        imageView.setLayoutParams(params);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();

	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
		
        View view = inflater.inflate(R.layout.new_game_fragment, container, false);
        
        ImageView previewImage = (ImageView)view.findViewById(R.id.map_image);
        previewImage.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				NewMapSeed();
			}
        });
        
        LinearLayout networkLayout = (LinearLayout)view.findViewById(R.id.network_options);
        LinearLayout localLayout = (LinearLayout)view.findViewById(R.id.local_options);
        
        if (mNetwork == true)
        {
        	networkLayout.setVisibility(View.VISIBLE);
        	localLayout.setVisibility(View.GONE);
        } else
        {
        	networkLayout.setVisibility(View.GONE);
        	localLayout.setVisibility(View.VISIBLE);
        }
        
        SetupSpinners(view);
        OptionsToSpinners(view);
        
        return view;
    }
	
	void SetupSpinners(final View view)
	{
		
		Spinner sizeSpinner = (Spinner) view.findViewById(R.id.size_spinner);
		List<String> list = new ArrayList<String>();
		list.add("Tiny - 16x16");
		list.add("Small - 32x32");
		list.add("Medium - 48x48");
		list.add("Large - 64x64");
		list.add("Huge - 80x80");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		sizeSpinner.setAdapter(dataAdapter);
		
		sizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged(); 
		    	FillPlayersSpinners(view);
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner citiesSpinner = (Spinner) view.findViewById(R.id.cities_spinner);
		list = new ArrayList<String>();
		list.add("Few");
		list.add("Average");
		list.add("Lots");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		citiesSpinner.setAdapter(dataAdapter);
		
		citiesSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner balanceSpinner = (Spinner) view.findViewById(R.id.balance_spinner);
		list = new ArrayList<String>();
		list.add("None");
		list.add("Horizontal");
		list.add("Vertical");
		list.add("Both");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		balanceSpinner.setAdapter(dataAdapter);
		
		balanceSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner scaleSpinner = (Spinner) view.findViewById(R.id.smooth_spinner);
		list = new ArrayList<String>();
		list.add("Small");
		list.add("Medium");
		list.add("Large");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		scaleSpinner.setAdapter(dataAdapter);
		
		scaleSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner sealevelSpinner = (Spinner) view.findViewById(R.id.sealevel_spinner);
		list = new ArrayList<String>();
		list.add("Low");
		list.add("Medium");
		list.add("High");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		sealevelSpinner.setAdapter(dataAdapter);
		
		sealevelSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner playersSpinner = (Spinner) view.findViewById(R.id.players_spinner);
		list = new ArrayList<String>();
		list.add("2");
		list.add("3");
		list.add("4");
		list.add("5");
		list.add("6");
		list.add("7");
		list.add("8");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		playersSpinner.setAdapter(dataAdapter);
		
		playersSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
		
		Spinner localHumanPlayersSpinner = (Spinner) view.findViewById(R.id.human_players_spinner);
		list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("4");
		list.add("5");
		list.add("6");
		list.add("7");
		list.add("8");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		localHumanPlayersSpinner.setAdapter(dataAdapter);
		
		localHumanPlayersSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();
		    	FillPlayersSpinners(view);
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
		
		Spinner localAIPlayersSpinner = (Spinner) view.findViewById(R.id.ai_players_spinner);
		list = new ArrayList<String>();
		list.add("0");
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("4");
		list.add("5");
		list.add("6");
		list.add("7");
		list.add("8");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		localAIPlayersSpinner.setAdapter(dataAdapter);
		
		localAIPlayersSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	  
		    	FillPlayersSpinners(view);
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
	
		
		
		
		Spinner fogSpinner = (Spinner) view.findViewById(R.id.fog_spinner);
		list = new ArrayList<String>();
		list.add("Off");
		list.add("On");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		fogSpinner.setAdapter(dataAdapter);
		
		fogSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	Random rand = new Random();
		    	mOptions.mapSeed = rand.nextInt();
		    	//mOptions.mapSeed = -252332814;//rand.nextInt();
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner winSpinner = (Spinner) view.findViewById(R.id.win_spinner);
		list = new ArrayList<String>();
		list.add("Capture Enemy Cities");
		list.add("Capture Enemy HQ");
		list.add("Annihilate");
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		winSpinner.setAdapter(dataAdapter);
		
		winSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		Spinner timeSpinner = (Spinner) view.findViewById(R.id.timelimit_spinner);
		list = new ArrayList<String>();
		list.add("6h");
		list.add("12h");
		list.add("24h");
		list.add("36h");
		list.add("48h");
		
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		timeSpinner.setAdapter(dataAdapter);
		
		timeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});
		
		CheckBox mountainCheck = (CheckBox)view.findViewById(R.id.mountains_check);
		mountainCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1)
			{
				SpinnersChanged();
			}});
		
		CheckBox forestCheck = (CheckBox)view.findViewById(R.id.forest_check);
		forestCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1)
			{
				SpinnersChanged();
			}});
		
		CheckBox friendsCheck = (CheckBox)view.findViewById(R.id.friends_check);
		friendsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1)
			{
				SpinnersChanged();
			}});
		
		
		SetupAISpinner(R.id.ai_player_1_options, R.id.ai_player_1_spinner, view);
		SetupAISpinner(R.id.ai_player_2_options, R.id.ai_player_2_spinner, view);
		SetupAISpinner(R.id.ai_player_3_options, R.id.ai_player_3_spinner, view);
		SetupAISpinner(R.id.ai_player_4_options, R.id.ai_player_4_spinner, view);
		SetupAISpinner(R.id.ai_player_5_options, R.id.ai_player_5_spinner, view);
		SetupAISpinner(R.id.ai_player_6_options, R.id.ai_player_6_spinner, view);
		SetupAISpinner(R.id.ai_player_7_options, R.id.ai_player_7_spinner, view);
		
	}
	
	void SetupAISpinner(int area, int spinner, View view)
	{
		Spinner aiSpinner = (Spinner) view.findViewById(spinner);
		ArrayList<String> list = new ArrayList<String>();
		list.add("Beginner");
		list.add("Intermediate");
		
		aiSpinner.setSelection(1);	
		
		ArrayAdapter<String>dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		dataAdapter.setDropDownViewResource(R.layout.spinner_text_item);
		aiSpinner.setAdapter(dataAdapter);
		
		aiSpinner.setSelection(1);
		
		aiSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
		    {
		    	SpinnersChanged();    	    
		    }
	
			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
	
				
			}
		});

	}
	
	int GetMaxPlayers(int mapSizePos)
	{
		int maxPlayers = 0;
		if (mapSizePos == 0)
		{
			maxPlayers = 2;
		} else if (mapSizePos == 1)
		{
			maxPlayers = 4;
		} else if (mapSizePos == 2)
		{
			maxPlayers = 6;
		} else if (mapSizePos == 3)
		{
			maxPlayers = 8;
		} else if (mapSizePos >3 )
		{
			maxPlayers = 8;
		}
		
		
		
		return maxPlayers;
	}
	
	void ValidateSpinners()
	{
		if (getView() == null) return;
		Spinner sizeSpinner = (Spinner) getView().findViewById(R.id.size_spinner);
		
		int sizePos = sizeSpinner.getSelectedItemPosition();
		int maxPlayers = GetMaxPlayers(sizePos);
		
		if (mNetwork == true)
		{
			Spinner playersSpinner = (Spinner) getView().findViewById(R.id.players_spinner);
			int playersPos = playersSpinner.getSelectedItemPosition();
			int players = playersPos + 2;
			if (players > maxPlayers)
			{
				playersSpinner.setSelection(maxPlayers - 2);
			}
		}
		
		
		if (mNetwork == false)
		{
			Spinner humanPlayersSpinner = (Spinner) getView().findViewById(R.id.human_players_spinner);
			int humanPlayers = humanPlayersSpinner.getSelectedItemPosition() + 1;
			Spinner aiPlayersSpinner = (Spinner) getView().findViewById(R.id.ai_players_spinner);
			int aiPlayers = aiPlayersSpinner.getSelectedItemPosition();
			
			if (humanPlayers + aiPlayers < 2)
			{
				humanPlayersSpinner.setSelection(0);
				aiPlayersSpinner.setSelection(1);
			}
			
			if (humanPlayers + aiPlayers > maxPlayers)
			{
				aiPlayersSpinner.setSelection(0);
				humanPlayersSpinner.setSelection(maxPlayers - 1);
			}
		}
		
	}
	
	void NewMapSeed()
	{
		Random rand = new Random();
		mOptions.mapSeed = rand.nextInt();
		//mOptions.mapSeed = -252332814;//rand.nextInt();
		GenerateMapPreview();
	}
	
	void SpinnersChanged()
	{
		ValidateSpinners();
		SpinnersToOptions();
		
		GenerateMapPreview();
	}
	
	void GenerateMapPreview()
	{
		if (mbGenerating == true) return;
		new GenerateNewGame().execute();
		
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.new_game, menu);
		
		if (mCreateEnabled)
		{
			menu.findItem(R.id.new_game).setEnabled(true);
		} else
		{
			menu.findItem(R.id.new_game).setEnabled(false);
		}
	}
	
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
		if (item.getItemId() == R.id.new_game)
		{
			CreateNewGame();
            return true;
		}
        
        return super.onOptionsItemSelected(item);
        
    }
	
	void FillPlayersSpinners(View view)
	{
		Spinner sizeSpinner = (Spinner) view.findViewById(R.id.size_spinner);
		int mapSizePos = sizeSpinner.getSelectedItemPosition();
		
		if (mNetwork == false)
		{
		Spinner humanPlayersSpinner = (Spinner) view.findViewById(R.id.human_players_spinner);
		Spinner aiPlayersSpinner = (Spinner) view.findViewById(R.id.ai_players_spinner);
		
		ArrayList<String> list = new ArrayList<String>();
		if (GetMaxPlayers(mapSizePos) >= 1) list.add("1");
		if (GetMaxPlayers(mapSizePos) >= 2) list.add("2");
		if (GetMaxPlayers(mapSizePos) >= 3) list.add("3");
		if (GetMaxPlayers(mapSizePos) >= 4) list.add("4");
		if (GetMaxPlayers(mapSizePos) >= 5) list.add("5");
		if (GetMaxPlayers(mapSizePos) >= 6) list.add("6");
		if (GetMaxPlayers(mapSizePos) >= 7) list.add("7");
		if (GetMaxPlayers(mapSizePos) >= 8) list.add("8");
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		if (humanPlayersSpinner.getAdapter().getCount() != dataAdapter.getCount())
		{
			int humanPos = humanPlayersSpinner.getSelectedItemPosition();
			humanPlayersSpinner.setAdapter(dataAdapter);
			humanPlayersSpinner.setSelection(humanPos);
			
		}
		
		
		list = new ArrayList<String>();
		
		list.add("0");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 1 )) list.add("1");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 2 )) list.add("2");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 3 )) list.add("3");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 4 )) list.add("4");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 5 )) list.add("5");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 6 )) list.add("6");
		if ((GetMaxPlayers(mapSizePos) - mOptions.numHumanPlayers >= 7 )) list.add("7");
		
		dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		if (aiPlayersSpinner.getAdapter().getCount() != dataAdapter.getCount())
		{
			int aiPos = aiPlayersSpinner.getSelectedItemPosition();
			aiPlayersSpinner.setAdapter(dataAdapter);
			aiPlayersSpinner.setSelection(aiPos);
		}
		
		if (mOptions.bLocalGame == true)
		{
			HideSpinner(R.id.ai_player_1_options, view);
			HideSpinner(R.id.ai_player_2_options, view);
			HideSpinner(R.id.ai_player_3_options, view);
			HideSpinner(R.id.ai_player_4_options, view);
			HideSpinner(R.id.ai_player_5_options, view);
			HideSpinner(R.id.ai_player_6_options, view);
			HideSpinner(R.id.ai_player_7_options, view);
			if (mOptions.numAIPlayers > 0)
			{
				ShowSpinner(R.id.ai_player_1_options, view);
			}
			if (mOptions.numAIPlayers > 1)
			{
				ShowSpinner(R.id.ai_player_2_options, view);
			}
			if (mOptions.numAIPlayers > 2)
			{
				ShowSpinner(R.id.ai_player_3_options, view);
			}
			if (mOptions.numAIPlayers > 3)
			{
				ShowSpinner(R.id.ai_player_4_options, view);
			}
			if (mOptions.numAIPlayers > 4)
			{
				ShowSpinner(R.id.ai_player_5_options, view);
			}
			if (mOptions.numAIPlayers > 5)
			{
				ShowSpinner(R.id.ai_player_6_options, view);
			}
			if (mOptions.numAIPlayers > 6)
			{
				ShowSpinner(R.id.ai_player_7_options, view);
			}
			
		}
		
		} else
		{
		Spinner playersSpinner = (Spinner) view.findViewById(R.id.players_spinner);
		
		ArrayList<String>list = new ArrayList<String>();
		if (GetMaxPlayers(mapSizePos) >= 2) list.add("2");
		if (GetMaxPlayers(mapSizePos) >= 3) list.add("3");
		if (GetMaxPlayers(mapSizePos) >= 4) list.add("4");
		if (GetMaxPlayers(mapSizePos) >= 5) list.add("5");
		if (GetMaxPlayers(mapSizePos) >= 6) list.add("6");
		if (GetMaxPlayers(mapSizePos) >= 7) list.add("7");
		if (GetMaxPlayers(mapSizePos) >= 8) list.add("8");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), R.layout.spinner_text_item, list);
		if (playersSpinner.getAdapter().getCount() != dataAdapter.getCount()) playersSpinner.setAdapter(dataAdapter);
		}
				
	}
	
	
	
	void OptionsToSpinners(View view)
	{
		Spinner sizeSpinner = (Spinner) view.findViewById(R.id.size_spinner);
		if (mOptions.mapSize == 16)
		{
			sizeSpinner.setSelection(0);
		} else if (mOptions.mapSize == 32)
		{
			sizeSpinner.setSelection(1);
		} else if (mOptions.mapSize == 48)
		{
			sizeSpinner.setSelection(2);
		} else if (mOptions.mapSize == 64)
		{
			sizeSpinner.setSelection(3);
		} else if (mOptions.mapSize == 80)
		{
			sizeSpinner.setSelection(4);
		}
		
		Spinner sealevelSpinner = (Spinner) view.findViewById(R.id.sealevel_spinner);
		if (mOptions.landMassRatio == 0.25f)
		{
			sealevelSpinner.setSelection(2);
		} else if (mOptions.landMassRatio == 0.5f)
		{
			sealevelSpinner.setSelection(1);
		} else if (mOptions.landMassRatio == 0.75f)
		{
			sealevelSpinner.setSelection(0);
		}
		
		
		Spinner scaleSpinner = (Spinner) view.findViewById(R.id.smooth_spinner);
		if (mOptions.scale == 3)
		{
			scaleSpinner.setSelection(0);
		} else if (mOptions.scale == 6)
		{
			scaleSpinner.setSelection(1);
		} else if (mOptions.scale == 12)
		{
			scaleSpinner.setSelection(2);
		}
		
		Spinner citiesSpinner = (Spinner) view.findViewById(R.id.cities_spinner);
		citiesSpinner.setSelection(mOptions.cities);
				
		Spinner balanceSpinner = (Spinner) view.findViewById(R.id.balance_spinner);
		if (mOptions.mirrorType == MirrorType.None)
		{
			balanceSpinner.setSelection(0);
		} else if (mOptions.mirrorType == MirrorType.Horizontal)
		{
			balanceSpinner.setSelection(1);
		} else if (mOptions.mirrorType == MirrorType.Vertical)
		{
			balanceSpinner.setSelection(2);
		} else if (mOptions.mirrorType == MirrorType.Both)
		{
			balanceSpinner.setSelection(3);
		}
		
		Spinner winSpinner = (Spinner) view.findViewById(R.id.win_spinner);
		if (mOptions.winCondition == WinCondition.Annihilate)
		{
			winSpinner.setSelection(2);
		} else if (mOptions.winCondition == WinCondition.CaptureAllBases)
		{
			winSpinner.setSelection(0);
		} else
		{
			winSpinner.setSelection(1);
		}
		
		Spinner timeSpinner = (Spinner) view.findViewById(R.id.timelimit_spinner);
		if (mOptions.turnTimeOut == 6)
		{
			timeSpinner.setSelection(0);
		} else if (mOptions.turnTimeOut == 12)
		{
			timeSpinner.setSelection(1);
		} else if (mOptions.turnTimeOut == 24)
		{
			timeSpinner.setSelection(2);
		} else if (mOptions.turnTimeOut == 36)
		{
			timeSpinner.setSelection(3);
		} else
		{
			timeSpinner.setSelection(4);
		}
		
		CheckBox mountainsCheck = (CheckBox)view.findViewById(R.id.mountains_check);
		if (mOptions.bMountains == true)
		{
			mountainsCheck.setChecked(true);
		} else
		{
			mountainsCheck.setChecked(false);
		}
		
		CheckBox forestCheck = (CheckBox)view.findViewById(R.id.forest_check);
		if (mOptions.bForest == true)
		{
			forestCheck.setChecked(true);
		} else
		{
			forestCheck.setChecked(false);
		}
		
		CheckBox friendsCheck = (CheckBox)view.findViewById(R.id.friends_check);
		if (mOptions.bFriendsOnly == true)
		{
			friendsCheck.setChecked(true);
		} else
		{
			friendsCheck.setChecked(false);
		}
		
		
		
		mOptions.mapSeed = mOptions.mapSeed;
		
		SetAILevelSpinner(R.id.ai_player_1_spinner, mOptions.aiLevel[0], view);
		SetAILevelSpinner(R.id.ai_player_2_spinner, mOptions.aiLevel[1], view);
		SetAILevelSpinner(R.id.ai_player_3_spinner, mOptions.aiLevel[2], view);
		SetAILevelSpinner(R.id.ai_player_4_spinner, mOptions.aiLevel[3], view);
		SetAILevelSpinner(R.id.ai_player_5_spinner, mOptions.aiLevel[4], view);
		SetAILevelSpinner(R.id.ai_player_6_spinner, mOptions.aiLevel[5], view);
		SetAILevelSpinner(R.id.ai_player_7_spinner, mOptions.aiLevel[6], view);
		
		 
	}
	
	void HideSpinner(int spinnerId, View view)
	{
		View spinner = (View)view.findViewById(spinnerId);
		spinner.setVisibility(View.GONE);
	}
	
	void ShowSpinner(int spinnerId, View view)
	{
		View spinner = (View)view.findViewById(spinnerId);
		spinner.setVisibility(View.VISIBLE);
	}
	
	void SetAILevelSpinner(int spinner, AILevel level, View view)
	{
		Spinner aiSpinner = (Spinner)view.findViewById(spinner);
		if (level == AILevel.Beginner)
		{
			aiSpinner.setSelection(0);
		} else
		{
			aiSpinner.setSelection(1);
		}
	}
	
	void SpinnersToOptions()
	{
		if (getView() == null) return;
		Spinner sizeSpinner = (Spinner) getView().findViewById(R.id.size_spinner);
		int sizePos = sizeSpinner.getSelectedItemPosition();
		if (sizePos == 0)
		{
			mOptions.mapSize = 16;
		} else if (sizePos == 1)
		{
			mOptions.mapSize = 32;
		} else if (sizePos == 2)
		{
			mOptions.mapSize = 48;
		} else if (sizePos == 3)
		{
			mOptions.mapSize = 64;
		} else if (sizePos == 4)
		{
			mOptions.mapSize = 80;
		}
		
		Spinner sealevelSpinner = (Spinner) getView().findViewById(R.id.sealevel_spinner);
		int sealevelPos = sealevelSpinner.getSelectedItemPosition();
		if (sealevelPos == 0)
		{
			mOptions.landMassRatio = 0.75f;
		} else if (sealevelPos == 1)
		{
			mOptions.landMassRatio = 0.50f;
		} else if (sealevelPos == 2)
		{
			mOptions.landMassRatio = 0.25f;
		}
		
		Spinner scaleSpinner = (Spinner) getView().findViewById(R.id.smooth_spinner);
		int scalePos = scaleSpinner.getSelectedItemPosition();
		if (scalePos == 0)
		{
			mOptions.scale = 3;
		} else if (scalePos == 1)
		{
			mOptions.scale = 6;
		} else if (scalePos == 2)
		{
			mOptions.scale = 12;
		}
		
		Spinner citiesSpinner = (Spinner) getView().findViewById(R.id.cities_spinner);
		mOptions.cities = citiesSpinner.getSelectedItemPosition();
		
		Spinner balanceSpinner = (Spinner) getView().findViewById(R.id.balance_spinner);
		int balancePos = balanceSpinner.getSelectedItemPosition();
		
		if (balancePos == 0)
		{
			mOptions.mirrorType = MirrorType.None;
		} else if (balancePos == 1)
		{
			mOptions.mirrorType = MirrorType.Horizontal;
		}else if (balancePos == 2)
		{
			mOptions.mirrorType = MirrorType.Vertical;
		}else if (balancePos == 3)
		{
			mOptions.mirrorType = MirrorType.Both;
		}
		
		Spinner fogSpinner = (Spinner) getView().findViewById(R.id.fog_spinner);
		int fogPos = fogSpinner.getSelectedItemPosition();
		if (fogPos == 0)
		{
			mOptions.fogOfWar = false;
		} else
		{
			mOptions.fogOfWar = true;
		}
		
		Spinner winSpinner = (Spinner) getView().findViewById(R.id.win_spinner);
		int winPos = winSpinner.getSelectedItemPosition();
		if (winPos == 0)
		{
			mOptions.winCondition = WinCondition.CaptureAllBases;
		} else if (winPos == 1)
		{
			mOptions.winCondition = WinCondition.CaptureHQ;
		} else if (winPos == 2)
		{
			mOptions.winCondition = WinCondition.Annihilate;
		}
		
		CheckBox forestCheck = (CheckBox)getView().findViewById(R.id.forest_check);
		if (forestCheck.isChecked())
		{
			mOptions.bForest = true;
		} else
		{
			mOptions.bForest = false;
		}
		
		CheckBox mountainCheck = (CheckBox)getView().findViewById(R.id.mountains_check);
		if (mountainCheck.isChecked())
		{
			mOptions.bMountains = true;
		} else
		{
			mOptions.bMountains = false;
		}
		
		Spinner timeSpinner = (Spinner) getView().findViewById(R.id.timelimit_spinner);
		if (timeSpinner.getSelectedItemPosition() == 0)
		{
			mOptions.turnTimeOut =6;
		} else if (timeSpinner.getSelectedItemPosition() == 1)
		{
			mOptions.turnTimeOut =12;
		} else if (timeSpinner.getSelectedItemPosition() == 2)
		{
			mOptions.turnTimeOut = 24;
		} else if (timeSpinner.getSelectedItemPosition() == 3)
		{
			mOptions.turnTimeOut = 36;
		}else if (timeSpinner.getSelectedItemPosition() == 4)
		{
			mOptions.turnTimeOut = 48;
		}
		
		CheckBox friendsCheck = (CheckBox)getView().findViewById(R.id.friends_check);
		if (friendsCheck.isChecked())
		{
			mOptions.bFriendsOnly = true;
		} else
		{
			mOptions.bFriendsOnly = false;
		}
		
		mOptions.mapSeed = mOptions.mapSeed;

		Spinner playersSpinner = (Spinner) getView().findViewById(R.id.players_spinner);
		Spinner humanPlayersSpinner = (Spinner) getView().findViewById(R.id.human_players_spinner);
		Spinner AIPlayersSpinner = (Spinner) getView().findViewById(R.id.ai_players_spinner);
		
		if (mNetwork == false)
		{
			int humanPos = humanPlayersSpinner.getSelectedItemPosition();
			mOptions.numHumanPlayers = humanPos + 1;
			
			int aiPos = AIPlayersSpinner.getSelectedItemPosition();
			mOptions.numAIPlayers = aiPos;
		} else
		{
			int humanPos = playersSpinner.getSelectedItemPosition();
			mOptions.numHumanPlayers = humanPos + 2;
			mOptions.numAIPlayers = 0;
		}
		
		mOptions.aiLevel[0] = GetAISpinnerValue(R.id.ai_player_1_spinner, getView());
		mOptions.aiLevel[1] = GetAISpinnerValue(R.id.ai_player_2_spinner, getView());
		mOptions.aiLevel[2] = GetAISpinnerValue(R.id.ai_player_3_spinner, getView());
		mOptions.aiLevel[3] = GetAISpinnerValue(R.id.ai_player_4_spinner, getView());
		mOptions.aiLevel[4] = GetAISpinnerValue(R.id.ai_player_5_spinner, getView());
		mOptions.aiLevel[5] = GetAISpinnerValue(R.id.ai_player_6_spinner, getView());
		mOptions.aiLevel[6] = GetAISpinnerValue(R.id.ai_player_7_spinner, getView());
		
		
		
	}
	
	AILevel GetAISpinnerValue(int spinnerId, View view)
	{
		Spinner spinner = (Spinner)view.findViewById(spinnerId);
		if (spinner.getSelectedItemPosition() == 0)
		{
			return AILevel.Beginner;
		}
		
		return AILevel.Intermediate;
	}
	
	boolean bCreated = false;
	
	void CreateNewGame()
	{
		if (bCreated == true) return;
		bCreated = true;
		if (mOptions.bLocalGame == true)
		{
			// create a new game but don't send it to the server...
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			IState state = appWrapper.RetreiveState("GameToJoin");
			
			try
			{
				EventJoinGame joinEvent = new EventJoinGame();
				joinEvent.playerIdToJoin = (int)appWrapper.GetUserId();
				joinEvent.r = Color.red(appWrapper.GetColour());
				joinEvent.g = Color.green(appWrapper.GetColour());
				joinEvent.b = Color.blue(appWrapper.GetColour());
				
				joinEvent.logo = appWrapper.GetLogo();
				joinEvent.name = appWrapper.GetAlias();
				
				state = joinEvent.updateState(state);
				
				Random rand = new Random();
				
				ColourAdapter colours = new ColourAdapter(getActivity());
				
				int guestNum = 0;
				while (state.GetPlayers().size() < (mOptions.numHumanPlayers))
				{
					EventJoinGame aiJoin = new EventJoinGame();
					aiJoin.playerIdToJoin = TactikonState.GUEST_PLAYER_START + (guestNum);
					
					boolean bGoodColour = false;
					while (bGoodColour == false)
					{
						bGoodColour = true;
						
						int colourIndex = rand.nextInt(colours.mColours.size());
						aiJoin.r = colours.mColours.get(colourIndex).r;
						aiJoin.g = colours.mColours.get(colourIndex).g;
						aiJoin.b = colours.mColours.get(colourIndex).b;
						for (int i = 0; i < state.GetPlayers().size(); ++i)
						{
							PlayerInfo info = state.GetPlayerInfo(state.GetPlayers().get(i));
							if (info.r > aiJoin.r - 30 && info.r < aiJoin.r + 30 &&
								info.b > aiJoin.b - 30 && info.b < aiJoin.b + 30 &&
								info.g > aiJoin.g - 30 && info.g < aiJoin.g + 30)
							{
								bGoodColour = false;
							} 
						}

					}
					int logoNum = rand.nextInt(appWrapper.GetLogoStore().GetLogoList().length);
					aiJoin.logo = appWrapper.GetLogoStore().GetLogoList()[logoNum];
					aiJoin.name = "Guest" + (guestNum + 1);
					state = aiJoin.updateState(state);
					
					guestNum++;
				}
				
				int aiNum = 0;
				while (state.GetPlayers().size() < (mOptions.numHumanPlayers + mOptions.numAIPlayers))
				{
					EventJoinGame aiJoin = new EventJoinGame();
					aiJoin.playerIdToJoin = TactikonState.AI_PLAYER_START + aiNum;
					
					boolean bGoodColour = false;
					while (bGoodColour == false)
					{
						bGoodColour = true;
						int colourIndex = rand.nextInt(colours.mColours.size());
						aiJoin.r = colours.mColours.get(colourIndex).r;
						aiJoin.g = colours.mColours.get(colourIndex).g;
						aiJoin.b = colours.mColours.get(colourIndex).b;
						for (int i = 0; i < state.GetPlayers().size(); ++i)
						{
							PlayerInfo info = state.GetPlayerInfo(state.GetPlayers().get(i));
							if (info.r > aiJoin.r - 40 && info.r < aiJoin.r + 40 &&
								info.b > aiJoin.b - 40 && info.b < aiJoin.b + 40 &&
								info.g > aiJoin.g - 40 && info.g < aiJoin.g + 40)
							{
								bGoodColour = false;
							} 
						}
					}
					int logoNum = rand.nextInt(appWrapper.GetLogoStore().GetLogoList().length);
					aiJoin.logo = appWrapper.GetLogoStore().GetLogoList()[logoNum];
					AILevel aiLevel = mOptions.aiLevel[aiNum];
					aiJoin.name = "AI-" + aiLevel.toString();
					state = aiJoin.updateState(state);
					
					aiNum++;
				}
				
				int minId = 0;
				SyncList gameList = SyncListDB.getInstance(this.getActivity()).GetList("GameList", (int)appWrapper.GetUserId());
				ArrayList<Integer> games = gameList.GetList();
				for (Integer searchGameId : games)
				{
					if (searchGameId < minId) minId = searchGameId;
					
				}
				int gameId = minId - 1;
				
				GameDatabase.getInstance(this.getActivity()).AddGame(gameId, appWrapper.GetUserId());
				GameDatabase.getInstance(this.getActivity()).UpdateGame(gameId, appWrapper.GetUserId(), state);
				GameDatabase.getInstance(this.getActivity()).SetSequence(gameId, appWrapper.GetUserId(), state.GetSequence());
				
				ArrayList<Integer> list = new ArrayList<Integer>();
				list.add(gameId);
				SyncListDB.getInstance(this.getActivity()).AddToSyncList(list, "GameList", (int)appWrapper.GetUserId());
				
				// clear out any stored camera positions for this game
				for (Integer playerId : state.GetPlayers())
				{
					SharedPreferences prefs = getActivity().getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
			        SharedPreferences.Editor editor = prefs.edit();
			        
			        editor.remove(Integer.toString(gameId) + "_" + Long.toString(playerId) + "_CameraX");
					editor.remove(Integer.toString(gameId) + "_" + Long.toString(playerId) + "_CameraY");
					editor.remove(Integer.toString(gameId) + "_" + Long.toString(playerId) + "_ScaleFactor");
					editor.commit();
				}
				
				getActivity().finish();
				
			} catch (InvalidUpdateException e)
			{
				// error joining players to the game
			}
			
		} else
		{
			
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			mOptions.createdByAlias = appWrapper.GetAlias();
			mOptions.createdById = (int)appWrapper.GetUserId();
			final PackageNewGame p = new PackageNewGame();
			
			p.newGameEvent = appWrapper.NewGameEventFactory(mOptions);
			PackageDelivery sender = new PackageDelivery(getActivity(), p, new ISendEvents() {
	
				@Override
				public void preExecute()
				{
					getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);
					final LinearLayout progressArea = (LinearLayout)getView().findViewById(R.id.progress_area);
					progressArea.setY(-100);
					progressArea.setVisibility(View.VISIBLE);
					progressArea.animate()
		            	.translationY(0)
		            	.setDuration(200)
		            	.setListener(null);
					
					TextView progressText = (TextView)getView().findViewById(R.id.text_progress);
					progressText.setText("Creating game - please wait...");
					
					mCreateEnabled = false;
					getActivity().invalidateOptionsMenu();
				}
					
	
				@Override
				public void postExecute()
				{
					if (getView() == null || getActivity() == null) return;
					
					getActivity().setProgressBarIndeterminateVisibility(Boolean.FALSE);
					
					if (p.mReturnCode == PackageResponse.Success)
					{
						
						final LinearLayout progressArea = (LinearLayout)getView().findViewById(R.id.progress_area);
						progressArea.animate()
						.translationY(-100)
						.setDuration(200)
						.setListener(new AnimatorListenerAdapter() {
			                	@Override
			                	public void onAnimationEnd(Animator animation) {
			                		progressArea.setVisibility(View.GONE);
			                	}
			            	});
						
						Activity context = getActivity();
						if (context == null) return;
						
						getActivity().finish();
						
						// launch the "join game" activity
						Intent joinGameIntent = new Intent(getActivity(), JoinGameActivity.class);
						joinGameIntent.putExtra("GameID", p.mGameID);
						startActivity(joinGameIntent);
						
					} else
					{
						mCreateEnabled = true;
						Activity context = getActivity();
						if (context == null) return;
						context.invalidateOptionsMenu();
						
						TextView progressText = (TextView)getView().findViewById(R.id.text_progress);
						progressText.setText("Error during game creation. Sorry!");
						
						mCreateEnabled = true;
						getActivity().invalidateOptionsMenu();
	
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
		
	}
	
	boolean mbGenerating = false;;
	
	private class GenerateNewGame extends AsyncTask<String, Integer, Integer>
	{
		TactikonState state = null;
		
		GenerateNewGame()
		{
			super();
		}
				
		@Override
		protected void onPreExecute()
		{
			mbGenerating = true;
		}
		
		@Override
		protected void onPostExecute(Integer code)
		{
			mbGenerating = false;

			if (getActivity() == null) return;
			if (state == null) return;
			IAppWrapper appWrapper = (IAppWrapper)getActivity().getApplicationContext();
			
			BitmapDrawable drawable = MapGraphics.GeneratePreview(state, appWrapper.GetUserId());
			if (getView() == null || getActivity() == null) return;
			
			
			appWrapper.StoreState(state, "GameToJoin");
			
			ImageView view = (ImageView)getView().findViewById(R.id.map_image);
			view.setImageDrawable(drawable);
			getView().requestLayout();
			
			TextView balanceText = (TextView)getView().findViewById(R.id.balance_text);
			if (state.mapBalanceScore == 0)
			{
				balanceText.setText("Great");
			} else if (state.mapBalanceScore < 2)
			{
				balanceText.setText("Good");
			} else if (state.mapBalanceScore < 4)
			{
				balanceText.setText("Fair");
			} else if (state.mapBalanceScore < 7)
			{
				balanceText.setText("Poor");
			}else 
			{
				balanceText.setText("Unbalanced");
			}

		}
		
		@Override
	    protected void onProgressUpdate(Integer... v)
		{
	        super.onProgressUpdate(v);
	    }
		
		@Override
		protected Integer doInBackground(String... string)
		{
			EventNewGame event = new EventNewGame(mOptions);
			try
			{
				state = (TactikonState) event.updateState(state);
			} catch (InvalidUpdateException e)
			{
				
			}
			
			
			return 0;
		}
		
		
	    
	   
	}

	
}
