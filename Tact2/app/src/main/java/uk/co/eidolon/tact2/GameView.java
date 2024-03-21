package uk.co.eidolon.tact2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import Core.EventInjector;
import Core.IEvent;
import Core.IEventListener;
import Core.IState;
import Core.PlayerInfo;
import Core.StateEngine;
import Tactikon.State.BattleStats;
import Tactikon.State.BubbleInfoCityLost;
import Tactikon.State.BubbleInfoCityLost2;
import Tactikon.State.BubbleInfoCrashedPlane;
import Tactikon.State.BubbleInfoCrashedPlane2;
import Tactikon.State.BubbleInfoNoProd;
import Tactikon.State.BubbleInfoNoProd2;
import Tactikon.State.BubbleInfoProductionOnHold;
import Tactikon.State.BubbleInfoProductionOnHold2;
import Tactikon.State.BubbleInfoUnitLost;
import Tactikon.State.BubbleInfoUnitLost2;
import Tactikon.State.BubbleInfoUnitProduced;
import Tactikon.State.BubbleInfoUnitProduced2;
import Tactikon.State.City;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.EventBoardUnit;
import Tactikon.State.EventChangePlayerInfo;
import Tactikon.State.EventChangeProduction;
import Tactikon.State.EventChangeUnitConfig;
import Tactikon.State.EventEndTurnWithPlayerId;
import Tactikon.State.EventFortifyUnit;
import Tactikon.State.EventLeaveTransport;
import Tactikon.State.EventMoveUnit;
import Tactikon.State.IBubbleInfo;
import Tactikon.State.IBubbleInfo2;
import Tactikon.State.IUnit;
import Tactikon.State.Position;
import Tactikon.State.TactikonNewGameOptions.AILevel;
import Tactikon.State.TactikonState;
import Tactikon.State.Tracks;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitCarrier;
import Tactikon.State.UnitHelicopter;
import Tactikon.State.UnitTank;
import uk.co.eidolon.tact2.AIHelpers.IAIThread;
import uk.co.eidolon.tact2.AISimple.AIBrain;
import uk.co.eidolon.tact2.AIWorldPlot.AIThread;
import uk.co.eidolon.tact2.TextureManager.UnitDefinition;

//import uk.co.eidolon.tact2.AISimple.AIThread;

public class GameView extends GLSurfaceView implements Renderer
{
	Handler mHandler = new Handler();
	
	class EventListener extends IEventListener
	{
		@Override
		public void HandleQueuedEvent(IEvent event, IState before, final IState after)
		{
			// initiate animation or whatever the IEvent is
			TactikonState beforeState = (TactikonState)before;
			
			if (event instanceof EventMoveUnit)
			{
				EventMoveUnit moveEvent = (EventMoveUnit)event;
				
				mUIState = UIState.Animating;
				
				ArrayList<Position> route = beforeState.GetRoute(moveEvent.mUnitId, moveEvent.mTo);
				
				IUnit gameUnit = beforeState.GetUnit(moveEvent.mUnitId);
				gameUnit.mFortified = false;
			
				FocusOn(gameUnit.GetPosition());
				
				TactikonState afterState = (TactikonState)after;
				
				GetRenderUnit(moveEvent.mUnitId).tracks = afterState.tracks.get(Long.valueOf(moveEvent.mUnitId));
				
				MoveAnimation moveAnim = new MoveAnimation(route, 4, GetRenderUnit(moveEvent.mUnitId), new AnimationAction()
				{
					@Override
					public void completeAction()
					{
						mUIState = UIState.UnitSelected;
						mState = (TactikonState)after;
						bFocussing = false;
						if (mState.GetUnit(mSelectedUnitId) == null) 
						{
							mUIState = UIState.NothingSelected;
							mSelectedUnitId = -1;
						}
						if (mState.GetUnit(mSelectedAttack) == null) mSelectedAttack = -1;
						
						if (mSelectedUnitId != -1)
						{
							ResetSelectedUnit(mState.GetUnit(mSelectedUnitId));
						}
						
						StateUpdated();
					}
				});
				
				animManager.AddToQueue(moveAnim);
			}
			
			if (event instanceof EventLeaveTransport)
			{
				mState = (TactikonState)after;
				StateUpdated();
			}

			if (event instanceof EventChangeUnitConfig)
			{
				mState = (TactikonState)after;
				StateUpdated();
				
				if (mSelectedUnitId != -1)
				{
					ResetSelectedUnit(mState.GetUnit(mSelectedUnitId));
				}
			}
			
			if (event instanceof EventBoardUnit)
			{
				mState = (TactikonState)after;
				StateUpdated();
			}
			
			if (event instanceof EventFortifyUnit)
			{
				mState = (TactikonState)after;
				StateUpdated();

			}
			;
			if (event instanceof EventChangePlayerInfo)
			{
				mState = (TactikonState)after;
				StateUpdated();

			}
			
			if (event instanceof EventChangeProduction)
			{
				mState = (TactikonState)after;
				StateUpdated();

			}
			
			if (event instanceof EventEndTurnWithPlayerId)
			{
				StoreCamera();
				mState = (TactikonState)after;
				
				StateUpdated();
				
				SetNothingSelected();
				
				if (mTutorialStage == TutorialStage.END_TURN)
				{
					SetTutorialStage(TutorialStage.CAPTURE_CITY);
				}
				
				if (mTutorialStage == TutorialStage.ON_OWN_NOW)
				{
					SetTutorialStage(TutorialStage.END);
				}
				
				mStateEngine.DoDeferedPumping(QueueSize());
				
				bFocussing = false;
				
				Runnable mRetreiveCamera = new Runnable()
				{
				       public void run() 
				       {
				    	   if (mTutorialMode == false)
								GetSavedCamera();
				       }
				};
				
				mHandler.postDelayed(mRetreiveCamera, 250);
				
			}
			
			if (event instanceof EventAttackUnit)
			{
				final TactikonState afterState = (TactikonState)after;
				EventAttackUnit attackEvent = (EventAttackUnit)event;
				
				mUIState = UIState.Animating;
				
				IUnit gameUnit1 = beforeState.GetUnit(attackEvent.attackingUnitId);
				gameUnit1.mFortified = false;
				IUnit gameUnit2 = beforeState.GetUnit(attackEvent.defendingUnitId);
				gameUnit2.mFortified = false;
				
				((Activity)mContext).runOnUiThread(new Runnable() {
			        public void run()
			        {
			        	HideAttackDialog();
			        }
				});
				
				
				FocusOn(gameUnit1.GetPosition());
				BattleStats attackerStats = new BattleStats(mState, gameUnit1, gameUnit2, false);
				BattleStats defenderStats = new BattleStats(mState, gameUnit2, gameUnit1, false);
				
				AttackAnimation attackAnim = new AttackAnimation(
						animManager, GetRenderUnit(attackEvent.attackingUnitId),
						GetRenderUnit(attackEvent.defendingUnitId),
						afterState.GetUnit(attackEvent.attackingUnitId),
						afterState.GetUnit(attackEvent.defendingUnitId), new AnimationAction()
						{
							@Override
							public void completeAction()
							{
								mUIState = UIState.UnitSelected;
								mState = afterState;
								bFocussing = false;
								if (mState.GetUnit(mSelectedUnitId) == null)
								{
									mSelectedUnitId = -1;
									mUIState = UIState.NothingSelected;
								}
								if (mState.GetUnit(mSelectedAttack) == null) mSelectedAttack = -1;
								
								if (mSelectedUnitId != -1)
								{
									ResetSelectedUnit(mState.GetUnit(mSelectedUnitId));
								}
								
								fog.UpdateFog(mState, GetCurrentUserId());
								
								StateUpdated();
							}
						}, attackerStats, defenderStats);
				
				animManager.AddToQueue(attackAnim);
						
			}
			
		}

	}

	Context mContext;
	
	EventListener mListener;
	
	MapObject map;
	SpriteObject selectReticule;
	SpriteBatch moveHighlight;
	
	SpriteObject mapTouchHighlight;
	
	SpriteObject attackReticule;
	SpriteBatch attackHighlight;
	SpriteBatch fuelHighlight;
	SpriteBatch tacticalHighlightAllUnits;
	SpriteBatch tacticalHighlightUnmovedUnits;
	SpriteBatch tacticalHighlightNewUnits;
	
	// SpriteObject straightArrow;
	// SpriteObject leftArrow;
	// SpriteObject rightArrow;
	// SpriteObject finalArrow;
	
	class TrackParts
	{
		TrackParts(TextureManager texManager, String strStraight, String strLeft, String strRight, String strEnd, float r, float g, float b)
		{
			straight = new SpriteObject(texManager, strStraight, r, g, b, 0.5f);
			left = new SpriteObject(texManager, strLeft, r, g, b, 0.5f);
			right = new SpriteObject(texManager, strRight, r, g, b, 0.5f);
			end = new SpriteObject(texManager, strEnd, r, g, b, 0.5f);
		}
		SpriteObject straight;
		SpriteObject left;
		SpriteObject right;
		SpriteObject end;
	}
	
	TrackParts arrowParts;
	TrackParts contrailParts;
	TrackParts wakeParts;
	TrackParts treadParts;
	TrackParts bootParts;
	
	enum TutorialStage
	{
		LOOK_IN_CITY,
		SELECT_TANK,
		MOVE_TANK,
		DO_MOVE_TANK,
		SET_PRODUCTION,
		END_TURN,
		CAPTURE_CITY,
		SELECT_TRANSPORT,
		LOAD_TRANSPORT,
		ADJACENT_TRANSPORT,
		ON_OWN_NOW,
		END
	}
	
	TutorialStage mTutorialStage = TutorialStage.LOOK_IN_CITY;
	
	Map<Integer, SpriteObject> cogs = new TreeMap<Integer, SpriteObject>();;
	
	GestureDetector gestureDetector;
	
	UnitBatch unitBatch;
	
	FogObject fog;
	
	boolean bInitDone = false;
	
	TactikonState mState;
	EventInjector mInjector;
	
	String mConfigChangeString;
	
	float tilePixels = 32;
	
	boolean bCameraChanged = true;
	
	boolean mTutorialMode = false;
	int frame = 0;
	
	float mWidth = 480;
	float mHeight = 800;
	
	//
	// boolean bTacticalView = false;
	
	boolean bShowAllUnits = true;
	Set<Position> tacticalViewHLUnMoved = null;
	Set<Position> tacticalViewHLAllUnits = null;
	Set<Position> tacticalViewHLNewUnits = null;
	
	ArrayList<IAIThread> aiThreads = new ArrayList<IAIThread>();
	
	class InfoBubble
	{
		int x;
		int y;
		public View view;
		public IBubbleInfo info;
		public IBubbleInfo2 info2;
	}
	
	InfoBubble tutorialBubble = null;
	
	InfoBubble unitConfigChangeButton = null;
	
	ArrayList<InfoBubble> infoBubbles = new ArrayList<InfoBubble>();
	
	TutorialBubbles tutBubbles;
	
	public static float[] mProjectionMatrix = new float[16];

	private ScaleGestureDetector mScaleDetector;
	
	float xCamera = 16;
	float yCamera = 16;
	float xCameraFocus = 16;
	float yCameraFocus = 16;
	boolean bFocussing = false;
	private float mScaleFactor = 4.f;
	int mSelectedUnitId = -1;
	int mSelectedAttack = -1;
	int mSelectedCityId = -1;
	Position mSelectedMove = null;
		
	private float mSelectedTime = 0.0f;
	
	ArrayList<RenderUnit> units = new ArrayList<RenderUnit>();
	CityBatch cityBatch;
	
	ArrayList<Position> possibleMoves = new ArrayList<Position>();
	ArrayList<Integer> possibleAttacks = new ArrayList<Integer>();
	ArrayList<Position> fuelRangeMoves = new ArrayList<Position>();
	ArrayList<Position> selectedRoute = new ArrayList<Position>();
	
	AnimationManager animManager;
	EffectManager effectsManagew;
	
	enum UIState
	{
		NothingSelected,
		UnitSelected,
		MoveSelected,
		CitySelected,
		AttackSelected,
		Animating,
		ConfigChange
	}
	
	UIState mUIState = UIState.NothingSelected;
	
		
	//long mUserId = -1;
	long mCurrentUserId = -1;
	long GetCurrentUserId()
	{
		if (mState.bLocalGame == false)
		{
			return mCurrentUserId;
		}
		
		if (mState.bLocalGame == true)
		{
			long playerToPlay = mState.GetPlayerToPlay();
			
			if (playerToPlay < TactikonState.AI_PLAYER_START)
				return mState.GetPlayerToPlay();
			
		}
		
		return -1;
	}
	int mGameId = -1;
	
	StateEngine mStateEngine;
	
	boolean mAiAutoFollow = true;
	boolean mSimpleGraphics = false;
	
	public GameView(final Context context, StateEngine stateEngine, int gameId)
	{
		super(context);
		
		this.setTag("GameView");
		
		mContext = context;
		
		mStateEngine = stateEngine;
		
		setEGLContextClientVersion(2);
		setEGLConfigChooser(8, 8, 8, 8, 0, 0);
		getHolder().setFormat(PixelFormat.RGBA_8888);
		setRenderer(this);
	    setFocusable(true);
	    
	    AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
	    mCurrentUserId = appWrapper.GetUserId();
	    
	    mGameId = gameId;
	    
	    mListener = new EventListener();
	    
	    mState = (TactikonState)stateEngine.GetState();
	    
	    stateEngine.AddListener(mListener);
	    
	    mInjector = new EventInjector(stateEngine);
	    
	    mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	    
	    GetSavedCamera();
	    
	    appWrapper.SetCurrentStateEngine(stateEngine);
	    
	    setPreserveEGLContextOnPause(true);
	   
		gestureDetector = new GestureDetector(context, new SimpleOnGestureListener() {
			
			@Override
			public void onLongPress(MotionEvent e)
			{
				float x = e.getX();
				float y = e.getY();
				MapLongPressed(x, y);
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e)
			{
				float x = e.getX();
				float y = e.getY();
				MapTapped(x, y);
				return true;
			}
			
			@Override 
			public boolean onDoubleTap(MotionEvent e)
			{
				float x = e.getX();
				float y = e.getY();
				MapTapped(x, y);
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY)
			{
				MapDragged(distanceX, distanceY);
				return true;
			}

			
			});
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		mAiAutoFollow = prefs.getBoolean("aiAutoFollow", true);
		mSimpleGraphics = prefs.getBoolean("simpleGraphics", false);
        
		mTutorialMode = mState.bTutorial; //prefs.getBoolean("TUTORIAL_MODE", false);
		
		UpdateEndTurnButton();
		
		if (mTutorialMode == true)
		{
			UpdateTutorial();
		}
		
		

	}
	
	void StateUpdated()
	{
		tacticalViewHLUnMoved = null;
		tacticalViewHLAllUnits = null;
		
		fog.UpdateFog(mState, GetCurrentUserId());
		AddUnits(mState);
		AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
		cityBatch = new CityBatch(appWrapper.GetTextureManager(), (int) GetCurrentUserId());
		
		((Activity)mContext).runOnUiThread(new Runnable() {
	        public void run()
	        {
	        	UpdateEndTurnButton();
	        	UpdateEndBanners();
	        	if (mTutorialMode == true)
	        	{
	        		UpdateTutorial();
	        	}
	        }
		});
	}
	
	void SetTutorialStage(TutorialStage stage)
	{
		mTutorialStage = stage;
		
		((Activity)mContext).runOnUiThread(new Runnable() {
	        public void run()
	        {
	        	UpdateTutorial();
	        }
		});
	}
	
	void ShowConfigChangeButton()
	{
		Activity parent = (Activity)mContext;
	    RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
		
	    if (unitConfigChangeButton != null)
	    {
	    	gameArea.removeView(unitConfigChangeButton.view);
	    	unitConfigChangeButton = null;
	    }
	    
		if (unitConfigChangeButton == null)
		{
			unitConfigChangeButton = new InfoBubble();
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        unitConfigChangeButton.view = vi.inflate(R.layout.config_bubble, null);
			gameArea.addView(unitConfigChangeButton.view);
		}
		
		IUnit unit = mState.GetUnit(mSelectedUnitId);
		
		unitConfigChangeButton.x = unit.GetPosition().x;
		unitConfigChangeButton.y = unit.GetPosition().y;
		
		TextView configText = (TextView)unitConfigChangeButton.view.findViewById(R.id.config_text);
		configText.setText(mConfigChangeString);
		
		unitConfigChangeButton.view.setVisibility(View.VISIBLE);
		unitConfigChangeButton.view.setAlpha(0);
		unitConfigChangeButton.view.animate()
        	.alpha(1.0f)
        	.setDuration(400);
		
		unitConfigChangeButton.view.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v)
			{
				EventChangeUnitConfig configEvent = new EventChangeUnitConfig();
				configEvent.mUnit = mSelectedUnitId;
				mInjector.AddEvent(configEvent);
				mUIState = UIState.UnitSelected;
				HideConfigChangeButton();
			}});
		
		UpdateBubbles(0, xCamera, yCamera, mScaleFactor);
	}
	
	void FocusOn(Position pos)
	{
		if (mTutorialMode == true) return;
		if (mState.bFogOfWar == true) return;
		if (mState.GetPlayerToPlay() >= TactikonState.AI_PLAYER_START)
		{
			bFocussing = true;
			xCameraFocus = pos.x;
	        yCameraFocus = pos.y;
	        
	        bCameraChanged=true;
		}
	}
	
	void HideConfigChangeButton()
	{
		if (unitConfigChangeButton != null)
		{
			unitConfigChangeButton.view.setVisibility(View.GONE);
		}
	}
	
	void UpdateTutorial()
	{
		AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
		int humanPlayer = (int)appWrapper.GetUserId();
		
		// find the tutorial bubble, if it exists
		if (tutorialBubble == null)
		{
			tutorialBubble = new InfoBubble();
			Activity parent = (Activity)mContext;
		    LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        
		    tutorialBubble.view = vi.inflate(R.layout.tutorial_bubble, null);
		    		
			RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
			
			tutorialBubble.view.setAlpha(0);
			tutorialBubble.view.animate()
            	.alpha(0.8f)
            	.setDuration(400);
			
			gameArea.addView(tutorialBubble.view);
		}
		
		TextView tutorialText = (TextView)tutorialBubble.view.findViewById(R.id.info_text);
		
		City myCity = null;
		int numCities = 0;
		for (City city : mState.cities)
		{
			if (city.playerId == humanPlayer)
			{
				numCities ++;
				myCity = city;
			}
		}
		
		if (myCity == null) return;
		
		
		IUnit tank = null;
		IUnit boat = null;
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != humanPlayer) continue;
			if (unit instanceof UnitTank) tank = unit;
			if (unit instanceof UnitBoatTransport) boat = unit;
		}
		
		if (boat == null) return;
		if (tank == null) return;
		
		if (mTutorialStage == TutorialStage.DO_MOVE_TANK)
		{
			if (tank.GetPosition().x != 11 || tank.GetPosition().y != 0) mTutorialStage = TutorialStage.SET_PRODUCTION;
		}
		
		if (mTutorialStage == TutorialStage.SET_PRODUCTION)
		{
			if (myCity.productionType.isEmpty() == false) mTutorialStage = TutorialStage.END_TURN;
		}
		
		if (mTutorialStage == TutorialStage.CAPTURE_CITY)
		{
			if (numCities > 1) mTutorialStage = TutorialStage.SELECT_TRANSPORT;
		}
		
		if (mTutorialStage == TutorialStage.SELECT_TRANSPORT)
		{
			if (mSelectedUnitId == boat.mUnitId) mTutorialStage = TutorialStage.LOAD_TRANSPORT;
		}
		
		if (mTutorialStage == TutorialStage.LOAD_TRANSPORT)
		{
			if (tank.mCarriedBy != -1 || boat.mCarrying.size() != 0)
			{
				mTutorialStage = TutorialStage.ON_OWN_NOW;
			} else if (boat.GetPosition().x != 11 || boat.GetPosition().y != 0)
			{
				mTutorialStage = TutorialStage.ADJACENT_TRANSPORT;
			}
		}
		
		if (mTutorialStage == TutorialStage.ADJACENT_TRANSPORT)
		{
			if (tank.mCarriedBy != -1 || boat.mCarrying.size() != 0)
			{
				mTutorialStage = TutorialStage.ON_OWN_NOW;
			}
		}
		
		if (mTutorialStage == TutorialStage.LOOK_IN_CITY)
		{
			tutorialText.setText("You start with just one city. Tap it to see what units are fortified.");
			tutorialBubble.x = myCity.x;
			tutorialBubble.y = myCity.y;
		} else if (mTutorialStage == TutorialStage.SELECT_TANK)
		{
			tutorialText.setText("Tap the fortified tank to select it.");
			tutorialBubble.x = myCity.x;
			tutorialBubble.y = myCity.y;
		} else if (mTutorialStage == TutorialStage.MOVE_TANK)
		{
			tutorialText.setText("Move the tank closer to the uninhabited city to the north-east.");
			tutorialBubble.x = myCity.x + 1;
			tutorialBubble.y = myCity.y + 3;
		} else if (mTutorialStage == TutorialStage.DO_MOVE_TANK)
		{
			tutorialText.setText("Tap again to confirm the move. You want to capture as many cities as possible.");
			tutorialBubble.x = myCity.x + 1;
			tutorialBubble.y = myCity.y + 3;
		} else if (mTutorialStage == TutorialStage.SET_PRODUCTION)
		{
			tutorialText.setText("Cities produce new units. Tap your city to choose what to produce.");
			tutorialBubble.x = myCity.x;
			tutorialBubble.y = myCity.y;
		} else if (mTutorialStage == TutorialStage.END_TURN)
		{
			tutorialText.setText("That's it for this turn. Tap the END TURN button up above.");
			tutorialBubble.x = myCity.x;
			tutorialBubble.y = myCity.y;
		} else if (mTutorialStage == TutorialStage.CAPTURE_CITY)
		{
			tutorialText.setText("Capture this city by moving your Tank into it");
			tutorialBubble.x = myCity.x + 3;
			tutorialBubble.y = myCity.y + 5;
		}
		else if (mTutorialStage == TutorialStage.SELECT_TRANSPORT)
		{
			tutorialText.setText("To conquer other islands, you'll need to load an Infantry or Tank onto the Transport. Select your boat.");
			tutorialBubble.x = boat.GetPosition().x;
			tutorialBubble.y = boat.GetPosition().y;
		}
		else if (mTutorialStage == TutorialStage.LOAD_TRANSPORT)
		{
			tutorialText.setText("Move the boat out of the city around the coast.");
			tutorialBubble.x = 8;
			tutorialBubble.y = 5;
		}
		else if (mTutorialStage == TutorialStage.ADJACENT_TRANSPORT)
		{
			tutorialText.setText("Next turn, move the boat closer to the new city and board it with the tank.");
			tutorialBubble.x = boat.GetPosition().x;
			tutorialBubble.y = boat.GetPosition().y;
		} else if  (mTutorialStage == TutorialStage.ON_OWN_NOW)
		{
			tutorialText.setText("You've got the basics - now capture more cities and attack the enemy. Check the Help screens for detailed info.");
			tutorialBubble.x = myCity.x;
			tutorialBubble.y = myCity.y;
		} else if (mTutorialStage == TutorialStage.END)
		{
			tutorialBubble.view.setVisibility(View.GONE);
		}
		
		UpdateBubbles(0, xCamera, yCamera, mScaleFactor);
		
	}
	
	void AddTacticalToggleButton()
	{
		Activity parent = (Activity)mContext;
		RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
		
		final ImageView unitsButton = new ImageView(mContext.getApplicationContext());
		unitsButton.setImageResource(R.drawable.button_moves);
		unitsButton.setClickable(true);
		unitsButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0)
			{
				bShowAllUnits = !bShowAllUnits;
				
				if (bShowAllUnits == true)
				{
					unitsButton.setAlpha(1.0f);
				} else
				{
					unitsButton.setAlpha(0.5f);
				}
				
				
			}});
		
		
		
		
		LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		
		int buttonSize = 92;
		if (getResources().getDisplayMetrics().densityDpi < 240)
		{
			buttonSize = 48;
		}
		
		params.topMargin = (buttonSize  /2 );
		params.height = buttonSize;
		params.width = buttonSize;
		
		unitsButton.setLayoutParams(params);
		unitsButton.setAlpha(0.8f);
		gameArea.addView(unitsButton);
		
		/*
		LayoutParams params2 = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params2.height = buttonSize;
		params2.width = buttonSize;
		params2.topMargin = (buttonSize /2) + buttonSize + 10;

		allUnitsButton.setLayoutParams(params2);
		allUnitsButton.setAlpha(0.8f);
		gameArea.addView(allUnitsButton);*/
	}
	
	boolean mButtonDisplayed = true;
	
	void DoStartTurnActions()
	{
		
	}
	
	long mCurrentPlayer = -1;
	
	void UpdateEndTurnButton()
	{
		if (mState == null) return;
		
		if (mState.GetPlayerToPlay() == GetCurrentUserId())
		{
			if (mState.bLocalGame == true && mCurrentPlayer != GetCurrentUserId())
			{
				AddHUDElements();
			}
		} else
		{
			if (mState.bLocalGame == true)
			{
				// remove the bubbles if it's not a human players turn
				Activity parent = (Activity)mContext;
				RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
				
				if (gameArea == null) return;
			    
			    if (infoBubbles != null && infoBubbles.size() > 0)
				{
			    	for (InfoBubble bubble : infoBubbles)
			    	{
			    		gameArea.removeView(bubble.view);
			    	}
				}
			}
		}
		
		mCurrentPlayer = GetCurrentUserId();
		
	}

	void SetUnitImage(ImageView imageView, IUnit unit, int color, TextureManager texMan)
	{
		if (unit == null) return;

		UnitDefinition tex = texMan.GetUnitDefinition(unit.getClass().getSimpleName(), unit.mConfig);
		if (tex == null) return;
		//Bitmap.Config config = new Bitmap.Config();

		int width = tex.mergedTexture.width;
		int height = tex.mergedTexture.height;

		Bitmap bmOverlay = Bitmap.createBitmap(width, height, tex.baseTexture.bitmap.getConfig());
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawBitmap(tex.baseTexture.bitmap, 0, 0, null);

		Paint paint = new Paint();
		paint.setColorFilter(new PorterDuffColorFilter(
				color, PorterDuff.Mode.MULTIPLY));

		canvas.drawBitmap(tex.colourTexture.bitmap, 0, 0, paint);

		bmOverlay = Bitmap.createScaledBitmap(bmOverlay, width * 3, height * 3, false);

		BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmOverlay);

		imageView.setImageDrawable(bitmapDrawable);
	}
	
	void AddInfoBubbles()
	{
		if (mState == null) return;
		
		Activity parent = (Activity)mContext;
	    LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
	    RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
	    
	    infoBubbles = new ArrayList<InfoBubble>();

		AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();

		if (mState.bubbles2.size() == 0) {

			for (IBubbleInfo bubble : mState.bubbles) {
				if (!bubble.MessageForPlayer((int) GetCurrentUserId())) continue;
				InfoBubble viewBubble = new InfoBubble();

				viewBubble.view = vi.inflate(R.layout.new_info_bubble, null);
				viewBubble.view.setVisibility(View.GONE);

				TextView bubbleText = (TextView) viewBubble.view.findViewById(R.id.info_text);

				bubbleText.setTypeface(appWrapper.pixelFont);

				ImageView bubbleImage = (ImageView) viewBubble.view.findViewById(R.id.info_image);
				bubbleImage.setVisibility(View.VISIBLE);
				if (bubble.GetType() == IBubbleInfo.Type.UnitProduced) {
					BubbleInfoUnitProduced info = (BubbleInfoUnitProduced) bubble;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info = info;
					bubbleImage.setImageResource(R.drawable.plusone);
					bubbleText.setText("");
				} else if (bubble.GetType() == IBubbleInfo.Type.ProductionOnHold) {
					BubbleInfoProductionOnHold info = (BubbleInfoProductionOnHold) bubble;
					City city = mState.GetCity(info.cityId);
					if (city.playerId != GetCurrentUserId()) break;
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info = info;
					bubbleImage.setImageResource(R.drawable.onhold);
					bubbleText.setText("");
				} else if (bubble.GetType() == IBubbleInfo.Type.NoProd) {
					BubbleInfoNoProd info = (BubbleInfoNoProd) bubble;
					City city = mState.GetCity(info.cityId);
					if (city.playerId != GetCurrentUserId()) break;
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info = info;
					bubbleImage.setImageResource(R.drawable.noprod);
					bubbleText.setText("");
				} else if (bubble.GetType() == IBubbleInfo.Type.CityTaken) {
					BubbleInfoCityLost info = (BubbleInfoCityLost) bubble;
					City city = mState.GetCity(info.cityId);
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info = info;
					bubbleImage.setImageResource(R.drawable.flag);
					PlayerInfo playerInfo = mState.GetPlayerInfo(city.playerId);
					if (playerInfo == null) continue;
					ColorMatrix bwMatrix = new ColorMatrix();
					bwMatrix.setSaturation(0);
					bwMatrix.setScale((float) playerInfo.r / 256.0f, (float) playerInfo.g / 256.0f, (float) playerInfo.b / 256.0f, 1.0f);
					final ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(bwMatrix);
					bubbleImage.setColorFilter(colorFilter);
					bubbleText.setText("");
				} else if (bubble.GetType() == IBubbleInfo.Type.PlaneCrash) {
					BubbleInfoCrashedPlane info = (BubbleInfoCrashedPlane) bubble;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info = info;
					bubbleText.setText("");
					bubbleImage.setImageResource(R.drawable.crash_icon);
				} else if (bubble.GetType() == IBubbleInfo.Type.UnitKilled) {
					BubbleInfoUnitLost info = (BubbleInfoUnitLost) bubble;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info = info;
					bubbleText.setText("unit lost");
					bubbleImage.setImageResource(R.drawable.unitlost);
				}

				if (viewBubble.info != null) {
					final View finalView = viewBubble.view;

					finalView.setVisibility(View.INVISIBLE);
					finalView.setClickable(true);
					finalView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// animate the view until it is gone...
							finalView.animate()
									.alpha(0f)
									.setDuration(400)
									.setListener(new AnimatorListenerAdapter() {
										@Override
										public void onAnimationEnd(Animator animation) {
											finalView.setVisibility(View.GONE);
										}
									});
						}

					});

					finalView.setAlpha(0);
					finalView.animate()
							.alpha(1.0f)
							.setDuration(400);

					gameArea.addView(viewBubble.view);

					infoBubbles.add(viewBubble);
				}
			}
		} else
		{
			PlayerInfo playerInfo = mState.GetPlayerInfo(GetCurrentUserId());
			TextureManager texMan = appWrapper.GetTextureManager();

			for (IBubbleInfo2 bubble2 : mState.bubbles2) {
				if (!bubble2.MessageForPlayer((int) GetCurrentUserId())) continue;
				InfoBubble viewBubble = new InfoBubble();

				viewBubble.view = vi.inflate(R.layout.new_info_bubble, null);
				viewBubble.view.setVisibility(View.GONE);

				TextView bubbleText = (TextView) viewBubble.view.findViewById(R.id.info_text);

				bubbleText.setTypeface(appWrapper.pixelFont);

				ImageView bubbleImage = (ImageView) viewBubble.view.findViewById(R.id.info_image);
				ImageView bubbleImage2 = (ImageView) viewBubble.view.findViewById(R.id.info_image2);

				if (bubble2.GetType() == IBubbleInfo2.Type.UnitProduced) {
					BubbleInfoUnitProduced2 info = (BubbleInfoUnitProduced2) bubble2;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info2 = info;

					IUnit tempUnit = null;
					try {
						tempUnit = (IUnit) Class.forName(info.unitType).newInstance();
					} catch (Exception e)
					{
						continue;
					}

					SetUnitImage(bubbleImage, tempUnit,
							Color.rgb(playerInfo.r, playerInfo.g, playerInfo.b), texMan);

					bubbleText.setText(" built");
				} else if (bubble2.GetType() == IBubbleInfo2.Type.ProductionOnHold) {
					BubbleInfoProductionOnHold2 info = (BubbleInfoProductionOnHold2) bubble2;
					City city = mState.GetCity(info.cityId);
					if (city.playerId != GetCurrentUserId()) break;
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info2 = info;
					bubbleImage.setImageResource(R.drawable.warning_round);
					bubbleText.setText(" on hold");
				} else if (bubble2.GetType() == IBubbleInfo2.Type.NoProd) {
					BubbleInfoNoProd2 info = (BubbleInfoNoProd2) bubble2;
					City city = mState.GetCity(info.cityId);
					if (city.playerId != GetCurrentUserId()) break;
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info2 = info;
					bubbleImage.setImageResource(R.drawable.warning_round);
					bubbleText.setText(" no production");
				} else if (bubble2.GetType() == IBubbleInfo2.Type.CityTaken) {
					BubbleInfoCityLost2 info = (BubbleInfoCityLost2) bubble2;
					City city = mState.GetCity(info.cityId);
					viewBubble.x = city.x;
					viewBubble.y = city.y;
					viewBubble.info2 = info;
					bubbleImage.setImageResource(R.drawable.info_round);
					PlayerInfo cityPlayerInfo = mState.GetPlayerInfo(city.playerId);

					bubbleText.setText(" city taken");
				} else if (bubble2.GetType() == IBubbleInfo2.Type.PlaneCrash) {
					BubbleInfoCrashedPlane2 info = (BubbleInfoCrashedPlane2) bubble2;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info2 = info;
					bubbleText.setText(" out of fuel");

					IUnit tempUnit = null;
					try {
						tempUnit = (IUnit) Class.forName(info.unitType).newInstance();
					} catch (Exception e)
					{
						continue;
					}

					SetUnitImage(bubbleImage, tempUnit,
							Color.rgb(playerInfo.r, playerInfo.g, playerInfo.b), texMan);

				} else if (bubble2.GetType() == IBubbleInfo2.Type.UnitKilled) {
					BubbleInfoUnitLost2 info = (BubbleInfoUnitLost2) bubble2;
					viewBubble.x = info.x;
					viewBubble.y = info.y;
					viewBubble.info2 = info;
					bubbleText.setText(" destroyed by ");
					bubbleImage.setImageResource(R.drawable.unitlost);

					IUnit tempUnit = null;
					try {
						tempUnit = (IUnit) Class.forName(info.unitType).newInstance();
					} catch (Exception e)
					{
						continue;
					}
					IUnit tempUnit2 = null;
					try {
						tempUnit2 = (IUnit) Class.forName(info.unitTypeEnemy).newInstance();
					} catch (Exception e)
					{
						continue;
					}

					PlayerInfo playerInfo2 = mState.GetPlayerInfo(info.playerIdEnemy);

					SetUnitImage(bubbleImage, tempUnit,
							Color.rgb(playerInfo.r, playerInfo.g, playerInfo.b), texMan);
					SetUnitImage(bubbleImage2, tempUnit2,
							Color.rgb(playerInfo2.r, playerInfo2.g, playerInfo2.b), texMan);

					bubbleImage2.setVisibility(View.VISIBLE);
				}

				if (viewBubble.info2 != null) {
					final View finalView = viewBubble.view;

					finalView.setVisibility(View.INVISIBLE);
					finalView.setClickable(true);
					finalView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							// animate the view until it is gone...
							finalView.animate()
									.alpha(0f)
									.setDuration(400)
									.setListener(new AnimatorListenerAdapter() {
										@Override
										public void onAnimationEnd(Animator animation) {
											finalView.setVisibility(View.GONE);
										}
									});
						}

					});

					finalView.setAlpha(0);
					finalView.animate()
							.alpha(1.0f)
							.setDuration(400);

					gameArea.addView(viewBubble.view);

					infoBubbles.add(viewBubble);
				}
			}
		}
		
		UpdateBubbles(0, xCamera, yCamera, mScaleFactor);
	}
	
	@Override
	public void onPause()
	{
		
		StoreCamera();
		HideCityDialog();
		HideAttackDialog();
		
		if (mState.bLocalGame == true)
		{
			for (IAIThread aiThread : aiThreads)
			{
				Log.i("Tact", "Stopping ai");
				aiThread.stop = true;
				//while(aiThread.stopped == false);
			}
		}
		super.onPause();
	}
	
	void AddWinnerBanner()
	{
		if (bWinnerBanner == true) return;
		bWinnerBanner = true;
		ImageView imageView = new ImageView(mContext.getApplicationContext());
		
		LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		imageView.setLayoutParams(params);
		imageView.setImageResource(R.drawable.winner);
		Activity parent = (Activity)mContext;
		RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
		
		gameArea.addView(imageView);
		
		imageView.setX(-1000);
		imageView.animate()
			.x(0)
			.setDuration(1000);
	
	}
	
	boolean bWinnerBanner = false;
	boolean bDefeatBanner = false;
	
	void AddDefeatBanner()
	{
		if (bDefeatBanner == true) return;
		bDefeatBanner = true;
		ImageView imageView = new ImageView(mContext.getApplicationContext());
		
		LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		//params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		imageView.setLayoutParams(params);
		imageView.setImageResource(R.drawable.defeat);
		Activity parent = (Activity)mContext;
		RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
		
		gameArea.addView(imageView);
		
		//view.animate()
		  //      		.x(0)
		    //    		.setListener(null)
		      //  		.setDuration(300);
		
		imageView.setX(-1000);
		imageView.animate()
			.x(0)
			.setDuration(1000);
	}
	
	City GetUsersCity()
	{
		for (City city : mState.cities)
		{
			if (city.playerId == GetCurrentUserId()) return city;
		}
		return null;
	}
	
	void StartAIThreads()
	{
		aiThreads.clear();
		int aiNum = 0;
    	for (int i = 0; i < mState.players.size(); i++)
    	{
    		if (mState.GetPlayers().get(i) >= TactikonState.AI_PLAYER_START)
    		{
    			AIBrain brain = new AIBrain(mInjector);
    			
    			// load the brain DNA
    			try
				{
					final InputStream brainData = mContext.getAssets().open("brains/BestBrain.dat");
					DataInputStream stream = new DataInputStream(brainData);
					float[] dna = new float[2000];
					int index = 0;
					while (stream.available() > 0)
					{
						dna[index++] = stream.readFloat();
					}
					brain.InitialiseBrain(dna);
				} catch (IOException e3)
				{
				}
    			
    			//AIThread aiThread = new AIThread(mStateEngine, mState.GetPlayers().get(i), brain);
    			if (mState.aiLevel[aiNum] == AILevel.Intermediate)
    			{
    				IAIThread aiThread = new AIThread(mStateEngine, mState.GetPlayers().get(i));
    				aiThreads.add(aiThread);
    				aiThread.start();
    			} else
    			{
    				IAIThread aiThread = new uk.co.eidolon.tact2.AISimple.AIThread(mStateEngine, mState.GetPlayers().get(i), brain);
    				aiThreads.add(aiThread);
    				aiThread.start();
    			}
    			aiNum++;
    		}
    	}
	}
	
	
	public void UpdateEndBanners()
	{
		// add victory or defeat logo
		if (mState.bLocalGame == false)
		{
			if (mState.players.contains((int)GetCurrentUserId()) && mState.IsPlayerAlive(GetCurrentUserId()) == false)
			{
				AddDefeatBanner();
			}
			if (mState.GetWinner() == GetCurrentUserId() && mState.GetWinner() != -1)
			{
				AddWinnerBanner();
			}
		} else
		{
			if (mState.GetWinner() != -1)
			{
				if (mState.GetWinner() < TactikonState.AI_PLAYER_START)
				{
					AddWinnerBanner();
				} else
				{
					AddDefeatBanner();
				}
			}
		}
		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		AddHUDElements();
		UpdateEndBanners();
		
		if (mState.bLocalGame == true)
		{
			StartAIThreads();
		}
		
		UpdateEndTurnButton();
	}
	
	void AddHUDElements()
	{
		Activity parent = (Activity)mContext;
		RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
	    gameArea.removeAllViews();
	    tutorialBubble = null;
	    unitConfigChangeButton = null;
	    
	    AddInfoBubbles();
	    
	    AddTacticalToggleButton();
	    
	    if (mTutorialMode == true)
	    	UpdateTutorial();
	}
	
	public void StoreCamera()
	{
		long player = mState.GetPlayerToPlay();
		SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat(Integer.toString(mGameId) + "_" + Long.toString(player) + "_CameraX", xCamera);
		editor.putFloat(Integer.toString(mGameId) + "_" + Long.toString(player) + "_CameraY", yCamera);
		editor.putFloat(Integer.toString(mGameId) + "_" + Long.toString(player) + "_ScaleFactor", mScaleFactor);
		editor.commit();
	}
	
	void GetSavedCamera()
	{
		SharedPreferences prefs = mContext.getSharedPreferences("uk.co.eidolon.tact2.prefs", Context.MODE_MULTI_PROCESS);
		long player = mState.GetPlayerToPlay();
		// find the players HQ, if there is one
		float startX = 16;
		float startY = 16;
		for (City city : mState.cities)
		{
			if (city.playerId == GetCurrentUserId() && city.startingCity == true)
			{
				startX = city.x;
				startY = city.y;
			}
		}
		
        xCamera = prefs.getFloat(Integer.toString(mGameId) + "_" + Long.toString(player)+ "_CameraX", startX);
        yCamera = prefs.getFloat(Integer.toString(mGameId) + "_" + Long.toString(player) + "_CameraY", startY);
        mScaleFactor = prefs.getFloat(Integer.toString(mGameId) + "_" + Long.toString(player)+ "_ScaleFactor", 3.0f);
	}
	
	class UnitPos
	{
		float x, y;
		float scale;
		int carryDepth;
		
		UnitPos(float x, float y, float scale, int carryDepth)
		{
			this.x = x;
			this.y = y;
			this.scale = scale;
			this.carryDepth = carryDepth;
		}
		
	}
	
	UnitPos GetUnitRenderPosition(int unitId)
	{
		IUnit unit = mState.GetUnit(unitId);
		
		if (unit.mCarriedBy == -1)
			return new UnitPos(unit.GetPosition().x, unit.GetPosition().y, 1.0f, 0);
		
		UnitPos parentPosition = GetUnitRenderPosition(unit.mCarriedBy);
		
		IUnit carrierUnit = mState.GetUnit(unit.mCarriedBy);
		int index = carrierUnit.mCarrying.indexOf(unitId);
		
		UnitPos myPosition = new UnitPos(parentPosition.x + (0.25f * parentPosition.scale) - (0.5f * parentPosition.scale * index),
										 parentPosition.y - (0.25f * parentPosition.scale),
										 parentPosition.scale / 2f, parentPosition.carryDepth + 1);
		
		return myPosition;
		
	}
	
	void AddUnits(TactikonState state)
	{
		units.clear();
		
		AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
		TextureManager texManager = appWrapper.GetTextureManager();
		
		//byte[][] fogMap = mState.GetResolvedFogMap((int) mUserId);
		
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			
			PlayerInfo info = state.GetPlayerInfo(unit.mUserId);
			UnitDefinition unitDefinition = texManager.GetUnitDefinition(unit.getClass().getSimpleName(), unit.mConfig);
			
			RenderUnit object = new RenderUnit();
			UnitPos pos = GetUnitRenderPosition(unit.mUnitId);
			object.x = pos.x;
			object.y = pos.y;
			object.carryDepth = pos.carryDepth;
			object.scale = pos.scale;
			object.unitId = unit.GetUnitID();
			object.baseTexture = unitDefinition.baseTexture;
			object.colourTexture = unitDefinition.colourTexture;
			object.shadowTexture = unitDefinition.shadowTexture;
			object.kills = unit.kills;

			
			object.r = (float)(info.r) / 256.0f;
			object.g = (float)(info.g) / 256.0f;
			object.b = (float)(info.b) / 256.0f;
			
			object.health = unit.health;
			object.veteran = unit.mIsVeteran;
			
			if (unit.fuelRemaining == 0) object.bShowAnimation = false;
			
			if (state.GetPlayerToPlay() != unit.mUserId || unit.bMoved == false || state.GetPossibleAttacks(unit.mUnitId).size() > 0 || unit.CanChangeConfig(mState) == true)
			{
				object.bHasAction = true;
			} else
			{
				object.bHasAction = false;
			}
			
			// add carried units to the end of the list, carriers to the front
			if (unit.mCarriedBy != -1)
			{
				units.add(object);
			} else
			{
				units.add(0, object);
			}
		}
		for (Entry<Integer, IUnit> entry : state.units.entrySet())
		{
			IUnit unit = entry.getValue();
			RenderUnit renderUnit = GetRenderUnit(unit.mUnitId);
			for (Integer carrying : unit.mCarrying)
			{
				RenderUnit carryUnit = GetRenderUnit(carrying);
				if (carryUnit != null) renderUnit.carrying.add(carryUnit);
			}
		}
		

		
	}
	
	RenderUnit GetRenderUnit(int unitId)
	{
		for (RenderUnit unit : units)
		{
			if (unit.unitId == unitId) return unit;
		}
		return null;
	}
	
	void FirstInit()
	{
		HideCityDialog();
		HideAttackDialog();
		
		AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
		TextureManager texManager = appWrapper.GetTextureManager();
		
		effectsManagew = new EffectManager(texManager);
		
		animManager = new AnimationManager(effectsManagew);
		
		map = new MapObject(mState, texManager);
	    
	    cityBatch = new CityBatch(texManager, (int) GetCurrentUserId());

	    AddUnits(mState);
	    
	    unitBatch = new UnitBatch(texManager);
	    
	    fuelHighlight = new SpriteBatch(texManager.GetTexture("square"), 0.9f, 0.1f, 0.1f, 0.5f, 0.8f);
	    
	    PlayerInfo info = mState.GetPlayerInfo(GetCurrentUserId());
	    if (info != null)
	    {
	    	tacticalHighlightUnmovedUnits = new SpriteBatch(texManager.GetTexture("circle_border"), 1.0f, 1.0f, 1.0f, 0.6f, 1.4f);
	    	tacticalHighlightNewUnits = new SpriteBatch(texManager.GetTexture("circle_border"), 0.5f, 1.0f, 0.5f, 1.0f, 1.4f);
	    	tacticalHighlightAllUnits = new SpriteBatch(texManager.GetTexture("circle"), (float)info.r/256, (float)info.g/256, (float)info.b/256, 0.6f, 1.4f);
	    }
	    
		texManager.BindTextures();
		
		selectReticule = new SpriteObject(texManager, "selected", 0.6f);
		selectReticule.xPos = 0;
		selectReticule.yPos = 0;
		
		for (Integer playerId : mState.players)
		{
			info = mState.GetPlayerInfo(playerId);
			if (info == null) continue;
			SpriteObject cogObject = new SpriteObject(texManager, "cog", (float)info.r / 256, (float)info.g / 256, (float)info.b /256, 1.0f);
			cogs.put(playerId, cogObject);
		}
		
		mapTouchHighlight = new SpriteObject(texManager, "square", 1,1,1, 0.35f);
		mapTouchHighlight.xPos =-100;
		mapTouchHighlight.yPos = -100;
		
		moveHighlight = new SpriteBatch(texManager.GetTexture("square"), 1, 1, 1, 0.3f, 0.8f);
		
		attackHighlight = new SpriteBatch(texManager.GetTexture("square"), 1, 0.1f, 0.1f, 0.3f, 0.8f);
		
		attackReticule = new SpriteObject(texManager, "crosshair", 1.0f,1.0f,1.0f, 0.8f);
		attackReticule.scale = 1.2f;
	
		arrowParts = new TrackParts(texManager, "straight", "left", "right", "final", 0.396f, 0.914f, 0.929f);
		contrailParts = new TrackParts(texManager, "air_track_forward", "air_track_left", "air_track_right", "air_track_end", 1.0f, 1.0f, 1.0f);
		wakeParts = new TrackParts(texManager, "boat_track_forward", "boat_track_left", "boat_track_right", "boat_track_end", 1.0f, 1.0f, 1.0f);
		treadParts = new TrackParts(texManager, "tank_track_forward", "tank_track_left", "tank_track_right", "tank_track_end", 1.0f, 1.0f, 1.0f);
		bootParts = new TrackParts(texManager, "boot_track_forward", "boot_track_left", "boot_track_right", "boot_track_end", 1.0f, 1.0f, 1.0f);
		
		fog = new FogObject(mState, GetCurrentUserId(), texManager);
		
		bInitDone = true;
	}
	


	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		boolean result = mScaleDetector.onTouchEvent(event);
		
		boolean isScaling = mScaleDetector.isInProgress();
	    if (!isScaling)
	    {
		//	First send event to gesture detector to find out, if it's a gesture
	    	if(gestureDetector != null && gestureDetector.onTouchEvent(event))
	    	{
	    		return true;
	    	}
	    }
 
		//If not a gesture handle the event here...
	    
	    if (isScaling == true) return true;
 
		return result ? result : super.onTouchEvent(event);
	}
	
	
	void MapTapped(float x, float y)
	{
		HandleViewTouch(x, y);
	}
	
	void MapLongPressed(float x, float y)
	{
		HandleViewLongPressed(x, y);
	}
	
	void MapDragged(float x, float y)
	{
		final float dx = -x;
        final float dy = -y;
    
        xCamera = xCamera - (dx / mScaleFactor) / 32;
        yCamera = yCamera + (dy / mScaleFactor) / 32;
        
        bCameraChanged=true;
        
        bFocussing = false;
        
        float xMax = mState.mapSize + 2;
        float yMax = mState.mapSize + 2;
        float xMin = -2;
        float yMin = -2;
        
        xCamera = Math.min(xCamera, xMax);
        xCamera = Math.max(xCamera, xMin);
        yCamera = Math.min(yCamera, yMax);
        yCamera = Math.max(yCamera, yMin);

        invalidate();
		
        
	}
	
	void HandleViewLongPressed(float x, float y)
	{
		if (mapTouchHighlight == null) return;
		// translate to map coordinates
		float[] invProj = new float[16];
		Matrix.invertM(invProj, 0, mProjectionMatrix, 0);
		
		float[] screenPos = new float[4];
		screenPos[0] = (x * 2) / mWidth - 1.0f;
		screenPos[1] = ((mHeight - y) * 2) / mHeight - 1.0f;
		screenPos[2] = -1;
		screenPos[3] = 1;
		float[] mapPos = new float[4];
		
		Matrix.multiplyMV(mapPos, 0, invProj, 0, screenPos, 0);
		
		mapPos[0] = mapPos[0] / mapPos[3] + xCamera;
		mapPos[1] = mapPos[1] / mapPos[3] + yCamera;
		
		int gridX = (int) Math.abs(mapPos[0]);
		int gridY = (int) Math.abs(mapPos[1]);
		
		mapTouchHighlight.xPos = gridX;
		mapTouchHighlight.yPos = gridY;
		
		if (gridX >= 0 && gridX < mState.mapSize &&
			gridY >= 0 && gridY < mState.mapSize)
		{
			DismissInfoBubbles(gridX, gridY);
			HandleMapLongPress(gridX, gridY);
		} else
		{
			SetNothingSelected();
		}
	}
	
	void HandleViewTouch(float x, float y)
	{
		if (mapTouchHighlight == null) return;
		// translate to map coordinates
		float[] invProj = new float[16];
		Matrix.invertM(invProj, 0, mProjectionMatrix, 0);
		
		float[] screenPos = new float[4];
		screenPos[0] = (x * 2) / mWidth - 1.0f;
		screenPos[1] = ((mHeight - y) * 2) / mHeight - 1.0f;
		screenPos[2] = -1;
		screenPos[3] = 1;
		float[] mapPos = new float[4];
		
		Matrix.multiplyMV(mapPos, 0, invProj, 0, screenPos, 0);
		
		//Log.i("Tact2", "MapPos: " + mapPos[0] + " " + mapPos[1] + " " + mapPos[2] + " " + mapPos[3]);
		
		mapPos[0] = mapPos[0] / mapPos[3] + xCamera;
		mapPos[1] = mapPos[1] / mapPos[3] + yCamera;
		
		int gridX = (int) (mapPos[0]);
		int gridY = (int) (mapPos[1]);
		
		mapTouchHighlight.xPos = gridX;
		mapTouchHighlight.yPos = gridY;
		
		if (gridX >= 0 && gridX < mState.mapSize &&
			gridY >= 0 && gridY < mState.mapSize)
		{
			DismissInfoBubbles(gridX, gridY);
			HandleMapTouch(gridX, gridY);
		} else
		{
			SetNothingSelected();
		}
	}
	
	void DismissInfoBubbles(int x, int y)
	{
		for (InfoBubble info : infoBubbles)
		{
			if (info.info == null) continue;
			if (info.x == x && info.y == y)
			{
				if (info.view != null && info.view.getVisibility() != View.GONE)
				{
					final View finalInfo = info.view;
					finalInfo.animate()
		            	.alpha(0f)
		            	.setDuration(400)
		            	.setListener(new AnimatorListenerAdapter() {
		            		@Override
		                	public void onAnimationEnd(Animator animation) {
		            			finalInfo.setVisibility(View.GONE);
		                }
		            });
				}
			}
		}
	}

	int GetUnitAtPosition(int x, int y)
	{
		IUnit currentSelected = null;
		if (mSelectedUnitId != -1) currentSelected = mState.GetUnit(mSelectedUnitId);
		
		if (currentSelected == null || currentSelected.GetPosition().x != x || currentSelected.GetPosition().y != y)
		{
			// first pass and nothing currently selected on this square, so pick the best choice
			IUnit carrier = null;
			for (Entry<Integer, IUnit> entry  :mState.units.entrySet())
			{
				IUnit searchUnit = entry.getValue();
				
				if (searchUnit.GetPosition().x != x || searchUnit.GetPosition().y != y) continue;
				if (searchUnit.mFortified == true) continue;
				
				if (searchUnit instanceof UnitBoatTransport || searchUnit instanceof UnitHelicopter || searchUnit instanceof UnitCarrier)
				{
					if (searchUnit.bMoved == false)
					{
						carrier = searchUnit;
					} else
					{
						if (searchUnit.mCarrying.size() > 0)
						{
							carrier = mState.GetUnit(searchUnit.mCarrying.get(0));
						}
					}
				}
				
			}
			if (carrier != null) return carrier.mUnitId;
		}
		
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			Position pos = unit.GetPosition();
			if (pos.x == x && pos.y == y && unit.mFortified == false && unit.mUnitId > mSelectedUnitId)
			{
				return unit.mUnitId;
			}

		}
		
		// second pass through in case the restriction on selectedUnitId failed
		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			Position pos = unit.GetPosition();
			if (pos.x == x && pos.y == y && unit.mFortified == false)
			{
				return unit.mUnitId;
			}
		}
		return -1;
	}
	
	int GetCityAtPosition(int x, int y)
	{
		for (City city : mState.cities)
		{
			if (city.x == x && city.y == y)
			{
				return city.cityId;
			}
		}
		return -1;
	}
	
	void ShowAttackDialog(int attackUnitId, int defendUnitId)
	{
		Activity hostActivity = (Activity)mContext;
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
		
		Bundle args = new Bundle();
		args.putInt("AttackId", attackUnitId);
		args.putInt("DefendId", defendUnitId);
		Fragment attackFragment = fragmentManager.findFragmentByTag("AttackFragment");
		if (attackFragment != null)
		{
			AttackFragment f = (AttackFragment)attackFragment;
			AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
			f.SetDetails(appWrapper.GetCurrentStateEngine(), attackUnitId, defendUnitId);
			return;
		}
	    
		Fragment newFragment = new AttackFragment();
		
		newFragment.setArguments(args);
	    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        //fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
	        fragmentTransaction.add(R.id.dialog_layer, newFragment, "AttackFragment");
	        fragmentTransaction.commit();
	}
	
	
	void ShowCityDialog(int cityId)
	{
		Activity hostActivity = (Activity)mContext;
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
		
		Fragment cityFragment = fragmentManager.findFragmentByTag("CityFragment");
		Bundle args = new Bundle();
		args.putInt("CityId", cityId);
		if (cityFragment != null)
		{
			CityFragment f = (CityFragment)cityFragment;
			AppWrapper appWrapper = (AppWrapper)mContext.getApplicationContext();
			f.SetDetails(appWrapper.GetCurrentStateEngine(), cityId);
			final View view = f.getView();
			view.setVisibility(View.VISIBLE);
			view.invalidate();
			view.bringToFront();
//			view.animate()
  //      		.x(0)
    //    		.setListener(null)
      //  		.setDuration(300);
			return;
		}
	    
		DialogFragment newFragment = new CityFragment();
		newFragment.setArguments(args);
		//newFragment.show(fragmentManager, "CityFragment");
		
		
		
	    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        //fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
	        fragmentTransaction.add(R.id.dialog_layer, newFragment, "CityFragment");
	        fragmentTransaction.commit();
	    
	        fragmentManager.executePendingTransactions();
	     
	}
	
	public void HideCityDialog()
	{
		Activity hostActivity = (Activity)mContext;
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag("CityFragment");

    		if (fragment != null)
        	{
        		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        		//fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
        		fragmentTransaction.remove(fragment);
        		fragmentTransaction.commit();
        	}
    	
	}
	
	
	public void HideAttackDialog()
	{
		Activity hostActivity = (Activity)mContext;
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
		
		Fragment fragment = fragmentManager.findFragmentByTag("AttackFragment");
			
    	if (fragment != null)
    	{
    		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    		//fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
    		fragmentTransaction.remove(fragment);
    		fragmentTransaction.commit();
    	}
	}
	
	public void ResetSelectedUnit(IUnit unit)
	{
		mUIState = UIState.UnitSelected;
		mSelectedUnitId = unit.GetUnitID();
		
		if (GetCurrentUserId() == unit.mUserId)
		{
			possibleMoves = mState.GetPossibleMoves(mSelectedUnitId);
		} else
		{
			possibleMoves = mState.GetDistanceMoves(mSelectedUnitId, unit.GetPosition());
		}
		
		possibleAttacks = mState.GetPossibleAttacks(mSelectedUnitId);
		
		if (GetCurrentUserId() == unit.mUserId)
		{
			fuelRangeMoves = mState.GetFuelRangeMoves(mSelectedUnitId);
		} else
		{
			fuelRangeMoves = null;
		}
	}
	
	public void SetSelectedUnit(IUnit unit)
	{
		mUIState = UIState.UnitSelected;
		mSelectedUnitId = unit.GetUnitID();
		mSelectedTime = 0;
		if (GetCurrentUserId() == unit.mUserId)
		{
			possibleMoves = mState.GetPossibleMoves(mSelectedUnitId);
		} else
		{
			possibleMoves = mState.GetDistanceMoves(mSelectedUnitId, unit.GetPosition());
		}
		possibleAttacks = mState.GetPossibleAttacks(mSelectedUnitId);
		
		if (GetCurrentUserId() == unit.mUserId)
		{
			fuelRangeMoves = mState.GetFuelRangeMoves(mSelectedUnitId);
		} else
		{
			fuelRangeMoves = null;
		}
		
		if (mTutorialMode == true)
		{
			if (mTutorialStage == TutorialStage.SELECT_TANK)
			{
				if (unit instanceof UnitTank)
				{
					SetTutorialStage(TutorialStage.MOVE_TANK);
				}
			}
			
			if (mTutorialMode == true)
			{
				if (mTutorialStage == TutorialStage.SELECT_TRANSPORT)
				{
					if (unit instanceof UnitBoatTransport)
					{
						SetTutorialStage(TutorialStage.LOAD_TRANSPORT);
					}
				}
			}
		}

	}

	boolean CheckClickedUnit(int x, int y)
	{
		if (mState.bFogOfWar == true)
		{
			byte[][] fogMap = mState.GetResolvedFogMap((int)GetCurrentUserId());
			if (fogMap[x][y] != 2) return false;
		}
		int unitIdAtPosition = GetUnitAtPosition(x, y);
		IUnit unitAtPosition = mState.GetUnit(unitIdAtPosition);
		
		if (unitAtPosition != null)
		{
			if (unitAtPosition.mUserId == GetCurrentUserId())
			{
				SetSelectedUnit(unitAtPosition);
				
				return true;
			}
		
			if ((unitAtPosition.IsStealth() && 
			   stateVisMap[unitAtPosition.GetPosition().x][unitAtPosition.GetPosition().y] == 1) ||
			     unitAtPosition.IsStealth() == false)
			{
				SetSelectedUnit(unitAtPosition);
				return true;
			}
		
					
					
			
		}
		return false;
	}
	
	boolean CheckClickedCity(int x, int y)
	{
		if (mUIState == UIState.Animating) return false;
		int cityIdAtPosition = GetCityAtPosition(x, y);
		City cityAtPosition = mState.GetCity(cityIdAtPosition);
		if (cityAtPosition != null && cityAtPosition.playerId == GetCurrentUserId())
		{
			mUIState = UIState.CitySelected;
			mSelectedCityId = cityAtPosition.cityId;
			mSelectedTime = 0;
			HideAttackDialog();
			ShowCityDialog(cityAtPosition.cityId);
			
			if (mTutorialMode == true)
			{
				if (mTutorialStage == TutorialStage.LOOK_IN_CITY)
					SetTutorialStage(TutorialStage.SELECT_TANK);
				
			}
			
			return true;
		}
		return false;
	}
	
	void SetNothingSelected()
	{
		mapTouchHighlight.xPos = -100;
		mapTouchHighlight.yPos = -100;
		if (mSelectedUnitId != -1)
		{
			IUnit unit = mState.GetUnit(mSelectedUnitId);
			if (unit != null)
			{
				for (City city : mState.cities)
				{
					if (city.x == unit.GetPosition().x && city.y == unit.GetPosition().y && city.playerId == unit.mUserId)
					{
						if (unit.mFortified == false && unit.mCarriedBy == -1)
						{
							EventFortifyUnit fortifyEvent = new EventFortifyUnit();
							fortifyEvent.cityIdToFortifyIn = city.cityId;
							fortifyEvent.unitIdToFortify = unit.mUnitId;
							mInjector.AddEvent(fortifyEvent);
						}
					}
				}
			}
		}
		
		HideConfigChangeButton();
		HideCityDialog();
		HideAttackDialog();
		mUIState = UIState.NothingSelected;
		mSelectedUnitId = -1;
		possibleMoves = null;
		possibleAttacks = null;
		fuelRangeMoves = null;
	}
	
	void HandleMapLongPress(int x, int y)
	{
		if (mState == null) return;
		if (mUIState == UIState.Animating) return;
		if (CheckClickedUnit(x, y))
		{
			
			IUnit unit = mState.GetUnit(mSelectedUnitId);
			if (unit.mUserId != GetCurrentUserId()) return;
			if (unit.CanChangeConfig(mState) == true)
			{
				mUIState = UIState.ConfigChange;
				mConfigChangeString = unit.GetConfigChangeType();
				
				ShowConfigChangeButton();

				return;
			}
		}
	}
	
	void HandleMapTouch(int x, int y)
	{
		if (mState == null) return;
		
		if (mUIState == UIState.Animating) return;
		
		if (mUIState == UIState.NothingSelected)
		{
			if (CheckClickedUnit(x, y)) return;
			if (CheckClickedCity(x, y)) return;
		}
		
		if (mUIState == UIState.ConfigChange)
		{
			if (CheckClickedUnit(x, y)) return;
			if (CheckClickedCity(x, y)) return;
			
			SetNothingSelected();
		}
		
		if (mUIState == UIState.UnitSelected)
		{
			// we've tapped while a unit was selected
			// 1. Have we tapped on the movement area for the unit?
			
			IUnit unit = mState.GetUnit(mSelectedUnitId);
			
			if (unit == null)
			{
				SetNothingSelected();
				return;
			}
			
			if (possibleMoves != null && unit.mUserId == GetCurrentUserId() && mState.GetPlayerToPlay() == unit.mUserId)
			{
				for (Position pos : possibleMoves)
				{
					if (pos.x == x && pos.y == y)
					{
						mUIState = UIState.MoveSelected;
						mSelectedMove = pos;
						selectedRoute = mState.GetRoute(mSelectedUnitId, pos);
						
						if (GetCurrentUserId() == unit.mUserId)
						{
							fuelRangeMoves = mState.GetFuelRangeMoves(mSelectedUnitId, pos);
						}
						
						if (mTutorialMode == true)
						{
							if (mTutorialStage == TutorialStage.MOVE_TANK)
							{
								if (unit instanceof UnitTank)
								{
									SetTutorialStage(TutorialStage.DO_MOVE_TANK);
								}
							}
						}
						
						return;
					}
				}
			}
			
			
			// 2. Have we tapped on the attack area for the unit?
			if (possibleAttacks != null && unit.mUserId == GetCurrentUserId())
			{
				for (Integer i : possibleAttacks)
				{
					IUnit defUnit = mState.GetUnit(i);
					if (defUnit == null) continue;
					Position pos = defUnit.GetPosition();
					if (pos.x == x && pos.y == y)
					{
						mUIState = UIState.AttackSelected;
						mSelectedAttack = i;
						HideCityDialog();
						ShowAttackDialog(mSelectedUnitId, mSelectedAttack);
						return;
					}
				}
			}
			

			
			// 3. Have we tapped another friendly unit?
			if (CheckClickedUnit(x, y))
			{
				return;
			}
			
			// 4. Have we clicked a city
			if (CheckClickedCity(x, y))
			{
				return;
			}
			
			SetNothingSelected();
			
			
		}
		
		if (mUIState == UIState.MoveSelected)
		{
			// 1. tap on the move again - do the move
			if (mSelectedMove.x == x && mSelectedMove.y == y)
			{
				// we've hit the target move - issue a move event
				IUnit unit = mState.GetUnit(mSelectedUnitId);
				Position from = new Position(unit.GetPosition().x, unit.GetPosition().y);
				Position to = new Position(mSelectedMove.x, mSelectedMove.y);
				EventMoveUnit moveEvent = new EventMoveUnit(mSelectedUnitId, from, to);
				mInjector.AddEvent(moveEvent); // as soon as event is added, mState is likely out of date
				
				mUIState = UIState.UnitSelected;
				return;
			}
			
			// 2. tap on a different move - select that move
			if (possibleMoves != null)
			{
				for (Position pos : possibleMoves)
				{
					if (pos.x == x && pos.y == y)
					{
						mUIState = UIState.MoveSelected;
						mSelectedMove = pos;
						selectedRoute = mState.GetRoute(mSelectedUnitId, pos);
						IUnit unit = mState.GetUnit(mSelectedUnitId);
						if (GetCurrentUserId() == unit.mUserId)
						{
							fuelRangeMoves = mState.GetFuelRangeMoves(mSelectedUnitId, pos);
						}
						return;
					}
				}
			}
			
			
			SetNothingSelected();
			

			
			// 3. tap on a different unit - select that unit
			if (CheckClickedUnit(x, y)) return;
			if (CheckClickedCity(x, y)) return;
			
		}
		
		if (mUIState == UIState.AttackSelected)
		{
			// have we clicked the attack again? if so, do the attack
			IUnit defUnit = mState.GetUnit(mSelectedAttack);
			Position pos = defUnit.GetPosition();
			if (pos.x == x && pos.y == y)
			{
				mUIState = UIState.UnitSelected;
				
				EventAttackUnit atackEvent = new EventAttackUnit(mSelectedUnitId, mSelectedAttack);
				mInjector.AddEvent(atackEvent); // as soon as event is added, mState is likely out of date
				
				mUIState = UIState.UnitSelected;
				return;
			}
			
			SetNothingSelected();
			
			// 2. tap on a different attack - select that attack
			
			// 3. tap on a different unit - select that unit
			if (CheckClickedUnit(x, y)) return;
			if (CheckClickedCity(x, y)) return;
		}
		
		if (mUIState == UIState.CitySelected)
		{
			// selected a unit instead
			if (CheckClickedUnit(x, y))
			{
				HideCityDialog();
				return;
			}
			
			// selected a different city
			if (CheckClickedCity(x, y))
			{
				// TODO: set new city for the dialog
				return;
			}
			
			// selected nothing
			SetNothingSelected();
			
		}
	}
	
	float pivotPointX = 0f;
	float pivotPointY = 0f;
	
	private class ScaleListener extends
    ScaleGestureDetector.SimpleOnScaleGestureListener 
    {
		@Override
		public boolean onScale(ScaleGestureDetector detector)
		{
			mScaleFactor *= detector.getScaleFactor();
			
			pivotPointX = detector.getFocusX();
			pivotPointY = detector.getFocusY();

			mScaleFactor = Math.max(0.2f, mScaleFactor);
			mScaleFactor = Math.min(8f, mScaleFactor);
			
			bCameraChanged = true;
			
			invalidate();
			return true;
		}
    }
	
	void UpdateBubble(InfoBubble bubble, int frame, float camX, float camY, float scaleFactor)
	{
		bubble.view.measure(0, 0);
		float[] mModelMatrix = new float[16];
		float[] mMVPMatrix = new float[16];
		Matrix.setIdentityM(mModelMatrix, 0);


		Matrix.translateM(mModelMatrix, 0, 0 - xCamera, 0 - yCamera , 0.0f);
		
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelMatrix, 0);
		
        float[] mapPos = new float[16];
        
		mapPos[0] = bubble.x+1;
		mapPos[1] = bubble.y + 0.5f;
		mapPos[2] = 0;
		mapPos[3] = 1;
		float[] screenPos = new float[4];
		
		Matrix.multiplyMV(screenPos, 0, mMVPMatrix, 0, mapPos, 0);
		
		screenPos[0] = ((screenPos[0] + 1) * (mWidth)) / 2.0f;// * scaleFactor;
		screenPos[1] = ((1 - screenPos[1]) * (mHeight)) / 2.0f;// * scaleFactor;
		
		
		if (bubble.view != null)
		{
			if (bubble.view.getVisibility() == View.INVISIBLE && frame >= 2) bubble.view.setVisibility(View.VISIBLE);
			bubble.view.setX(screenPos[0]);
			bubble.view.setY(screenPos[1] - (bubble.view.getMeasuredHeight() / 2));
			
			Activity parent = (Activity)mContext;
			RelativeLayout gameArea = (RelativeLayout)parent.findViewById(R.id.hud_layer);
			gameArea.invalidate();
			bubble.view.invalidate();
		}
	}
	
	void UpdateBubbles(int frame, float camX, float camY, float scaleFactor)
	{
		if (tutorialBubble != null)
		{
			UpdateBubble(tutorialBubble, frame, camX, camY, scaleFactor);
		}
		
		if (unitConfigChangeButton != null)
		{
			UpdateBubble(unitConfigChangeButton, frame, camX, camY, scaleFactor);
		}
		
		for (InfoBubble bubble : infoBubbles)
		{
			UpdateBubble(bubble, frame, camX, camY, scaleFactor);
		}
		
		Activity parent = (Activity)mContext;
		RelativeLayout view = (RelativeLayout)parent.findViewById(R.id.toplevel);
		view.invalidate();
		view.setClipChildren(false);
		view.requestLayout();
	}
	
	long time = 0;
	
	// direction 0, 1, 2, 3
	// left, up, right, down
	
	int GetDirection(Position from, Position to)
	{
		if (to.x > from.x) return 0;
		if (to.y > from.y) return 1;
		if (to.x < from.x) return 2;
		return 3;
		
	}
	
	private void RenderPath(TrackParts track, ArrayList<Position> path, boolean bPulsate)
	{
		if (path.size() < 2) return;
		int prevDir = GetDirection(path.get(0), path.get(1));
		
		for (int pathSeg = 2; pathSeg < path.size() + 1; ++pathSeg)
		{
			Position pos = new Position(path.get(pathSeg-1).x,path.get(pathSeg-1).y);
			int currDir = -1;
			if (pathSeg < path.size())
			{
				currDir = GetDirection(path.get(pathSeg -1), path.get(pathSeg));
			} else
			{
				currDir = prevDir; // final segment, always staight
			}
			
			float scale = 1.0f;
			if (bPulsate == true)
			{
				scale = ((float)Math.sin(-(mSelectedTime*5.0f) + ((float)pathSeg / 1.2f)));
				scale = (scale + 1.0f) / 2.0f;
			//	scale = scale * scale;
				scale = 1-scale;
				scale = scale/7;
				scale = scale + 1;
			}
			
			
			if (prevDir == currDir) // straight
			{
				if (pathSeg < path.size())
				{
					track.straight.xPos = pos.x;
					track.straight.yPos = pos.y;
					track.straight.angle = prevDir * 90 + 270;
					track.straight.scale = scale;
					track.straight.Render(mProjectionMatrix, xCamera, yCamera);
				} else
				{
					track.end.xPos = pos.x;
					track.end.yPos = pos.y;
					track.end.angle = prevDir * 90 + 270;  
					track.end.scale = scale;
					track.end.Render(mProjectionMatrix, xCamera, yCamera);
				}
			} else if (prevDir == currDir + 1 || (prevDir == 0 && currDir == 3))
			{
				track.right.xPos = pos.x;
				track.right.yPos = pos.y;
				track.right.angle = prevDir * 90 + 270;
				track.right.scale = scale;
				track.right.Render(mProjectionMatrix, xCamera, yCamera);
			} else
			{
				track.left.xPos = pos.x;
				track.left.yPos = pos.y;
				track.left.angle = prevDir * 90 + 270;
				track.left.scale = scale;
				track.left.Render(mProjectionMatrix, xCamera, yCamera);
			}
			
			prevDir = currDir;
			
		}
	}
	
	private void RenderTrack(Tracks myPath, float xCam, float yCam)
	{
		if (myPath == null) return;
		TrackParts track = null;
		if (myPath.type == 0) track = contrailParts;
		if (myPath.type == 1) track = wakeParts;
		if (myPath.type == 2) track = bootParts;
		if (myPath.type == 3) track = treadParts;
		
		if (track == null) return;
		
		ArrayList<Position> path = new ArrayList<Position>();
		for (int i = 0; i < myPath.renderPoints; ++i)
		{
			path.add(new Position(myPath.x[i],myPath.y[i]));
		}
		
		if (path.size() < 2) return;
		int prevDir = GetDirection(path.get(0), path.get(1));
		
		for (int pathSeg = 1; pathSeg < path.size() + 1; ++pathSeg)
		{
			Position pos = new Position(path.get(pathSeg-1).x,path.get(pathSeg-1).y);
			int currDir = -1;
			if (pathSeg < path.size())
			{
				currDir = GetDirection(path.get(pathSeg -1), path.get(pathSeg));
			} else
			{
				currDir = prevDir; // final segment, always staight
			}
			
			float scale = 1.0f;
			
			if (mState.bFogOfWar == true && stateFogMap!= null && stateFogMap[pos.x][pos.y] != 2)
			{
				prevDir = currDir;
				continue;
			}
						
			if (prevDir == currDir) // straight
			{
				if (pathSeg == 1)
				{
					track.end.xPos = pos.x;
					track.end.yPos = pos.y;
					track.end.angle = prevDir * 90 + 90;  
					track.end.scale = scale;
					track.end.Render(mProjectionMatrix, xCam, yCam);
				} else	if (pathSeg < path.size())
				{
					track.straight.xPos = pos.x;
					track.straight.yPos = pos.y;
					track.straight.angle = prevDir * 90 + 270;
					track.straight.scale = scale;
					track.straight.Render(mProjectionMatrix, xCam, yCam);
				} else
				{
					track.end.xPos = pos.x;
					track.end.yPos = pos.y;
					track.end.angle = prevDir * 90 + 270;  
					track.end.scale = scale;
					track.end.Render(mProjectionMatrix, xCam, yCam);
				}
			} else if (prevDir == currDir + 1 || (prevDir == 0 && currDir == 3))
			{
				track.right.xPos = pos.x;
				track.right.yPos = pos.y;
				track.right.angle = prevDir * 90 + 270;
				track.right.scale = scale;
				track.right.Render(mProjectionMatrix, xCam, yCam);
			} else
			{
				track.left.xPos = pos.x;
				track.left.yPos = pos.y;
				track.left.angle = prevDir * 90 + 270;
				track.left.scale = scale;
				track.left.Render(mProjectionMatrix, xCam, yCam);
			}
			
			prevDir = currDir;
			
		}
	}
	
	int frames = 0;
	double frame100 = 0;
	
	float mTimeElapsed = 0;
	
	void DrawAICog(float time)
	{
		float xx = (mWidth);
		float yy = (mHeight);
		
		int playerToPlay = (int)mState.GetPlayerToPlay();
		PlayerInfo info = mState.GetPlayerInfo(playerToPlay);
		
		float[] projMatrix = new float[16];
		Matrix.orthoM(projMatrix, 0, 0, xx, 0, yy, -20, 20);
		SpriteObject cog = cogs.get(playerToPlay);
		if (cog == null) return;
		cog.angle = -time * 50;
		cog.xPos = 50;
		cog.yPos = mHeight - 50;
		cog.scale = 100;
		
		cog.sprite.colA = (float)info.r / 256;
		cog.sprite.colG = (float)info.g / 256;
		cog.sprite.colB = (float)info.b / 256;
		cog.Render(projMatrix, 0, 0);
	}

	byte[][] stateFogMap = null;
	byte[][] stateVisMap = null;
	TactikonState fogState = null;
	TactikonState visState = null;
	
	void GenerateTacticalViewHighlights()
	{
		tacticalViewHLUnMoved = new TreeSet<Position>();
		tacticalViewHLAllUnits = new TreeSet<Position>();

		for (Entry<Integer, IUnit> entry : mState.units.entrySet())
		{
			IUnit unit = entry.getValue();
			if (unit.mUserId != GetCurrentUserId()) continue;
			
			Position pos = new Position(unit.GetPosition().x, unit.GetPosition().y);
			if (unit.bMoved == false || mState.GetPossibleAttacks(unit.mUnitId).size() > 0)
			{
				tacticalViewHLUnMoved.add(pos);
			}
			
			{
				tacticalViewHLAllUnits.add(pos);
			}
			
		}
		
		for (City city : mState.cities)
		{
			if (city.playerId != GetCurrentUserId() || city.playerId == -1) continue;
			Position pos = new Position(city.x, city.y);
			
			if (tacticalViewHLUnMoved.contains(pos) == false)
				tacticalViewHLAllUnits.add(pos);
		}
	}
	
	float fps100time = 0;
	
	@Override
	public void onDrawFrame(GL10 gl)
	{
		if (bInitDone == false) return;
		frame ++;
		
		if (mUIState != UIState.Animating)
			mListener.PumpQueue();
		
		frames ++;
		
		long newTime = SystemClock.elapsedRealtime();
		long timeDif = newTime - time;
		time = newTime;
		if (timeDif > 1000) timeDif = 1000;
		
		frame100 += timeDif / 1000.0;
		
		if (bCameraChanged == true || frame < 5)
		{
			((Activity)mContext).runOnUiThread(new Runnable() {
		        public void run()
		        {
		        	UpdateBubbles(frame, xCamera, yCamera, mScaleFactor);
		        }
			});
			bCameraChanged = false;
		}
		
		double frameSeconds = timeDif / 1000.0f;
		
		fps100time += frameSeconds;
		
		if (frames % 100 == 0)
		{
			fps100time = 0;
		}
		
		
		animManager.Update((float)frameSeconds);
		
		mSelectedTime += frameSeconds;
		mTimeElapsed += frameSeconds;
		
		if (bFocussing == true && mAiAutoFollow)
		{
			xCamera = ((xCamera * 5) + xCameraFocus) / 6;
			yCamera = ((yCamera * 5) + yCameraFocus) / 6;
			bCameraChanged = true;
		}
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		if ((bShowAllUnits == true) && (tacticalViewHLUnMoved == null || tacticalViewHLAllUnits == null))
		{
			GenerateTacticalViewHighlights();
		}
		
		float xx = (mWidth / tilePixels) / mScaleFactor;
		xx = xx / 2;
		float yy = (xx * (mHeight / mWidth));
		
		float xCam = xCamera;
		float yCam = yCamera;
		
		Matrix.orthoM(mProjectionMatrix, 0, -xx, xx, -yy, yy, -20, 20);
		
		map.SetTime(mTimeElapsed);
		map.Render(mProjectionMatrix, xCam, yCam, mSimpleGraphics);
		
		cityBatch.Render(mState, mState.cities, mProjectionMatrix, xCam, yCam);
		
		// if the fog state map needs updating, update it
		if (mState.bFogOfWar == true && (fogState != mState || stateFogMap ==  null))
		{
			fogState = mState;
			if (fogState != null)
			{
				stateFogMap = mState.GetResolvedFogMap((int) GetCurrentUserId());
			}
		}
		
		// highlight players units
		if (bShowAllUnits == true && tacticalHighlightAllUnits != null && tacticalHighlightUnmovedUnits != null)
		{
			PlayerInfo info = mState.GetPlayerInfo(GetCurrentUserId());
			if (info != null)
			{
				tacticalHighlightAllUnits.a =  0.35f;
				tacticalHighlightAllUnits.r = info.r/256.0f;
				tacticalHighlightAllUnits.g = info.g/256.0f;
				tacticalHighlightAllUnits.b = info.b/256.0f;
				tacticalHighlightAllUnits.Render(tacticalViewHLAllUnits, mProjectionMatrix, xCam, yCam);
				
				tacticalHighlightUnmovedUnits.a =  0.45f;
				//tacticalHighlightUnmovedUnits.r = info.r/256.0f;
				//tacticalHighlightUnmovedUnits.g = info.g/256.0f;
				//tacticalHighlightUnmovedUnits.b = info.b/256.0f;
				
				tacticalHighlightUnmovedUnits.Render(tacticalViewHLUnMoved, mProjectionMatrix, xCam, yCam);
			}
		}
		
		// if the vismap needs updating, do so now
		if (visState != mState || stateVisMap == null)
		{
			visState = mState;
			if (visState != null)
			{
				stateVisMap = mState.GetVisibilityMap((int) GetCurrentUserId());
			}
		}
		
		// TODO: need to render ground tracks first, contrails after
		// subs don't leave tracks when submerged
		// need to hide tracks in fog
		for (Entry<Long, Tracks> entry : mState.tracks.entrySet())
		{
			RenderTrack(entry.getValue(), xCam, yCam);
		}
		
		for (RenderUnit unit : units)
		{
			unit.render = false;
			IUnit gameUnit = mState.GetUnit(unit.unitId);
			
			if (gameUnit == null) continue;
			
			// don't render if it's fortified and not the currently selected unit
			boolean bSelected = false;
			if (mSelectedUnitId == unit.unitId) bSelected = true;
			if (mSelectedAttack == unit.unitId) bSelected = true; // show enemies being attacked (they might be fortified)
			if (gameUnit.mCarriedBy != -1)
			{
				IUnit carryUnit = mState.GetUnit(gameUnit.mCarriedBy);
				if (carryUnit.mUnitId == mSelectedUnitId) bSelected = true;
			}
			
			// if we're animating, render units in and out of fog correctly
			
			if (mState.bFogOfWar == true && stateFogMap[Math.round(unit.x)][Math.round(unit.y)] != 2) continue;
			
			// don't render stealth units we can't see
			if (gameUnit.IsStealth() == true)
			{
				if (stateVisMap[Math.round(unit.x)][Math.round(unit.y)] != 1) continue;
			}
			
			if (gameUnit.mFortified == true && bSelected == false) 
			{
				if (mUIState != UIState.Animating) continue;
				
				// check it's actually in the city (it might have moved)
				boolean bInCity = false;
				for (City city : mState.cities)
				{
					if (city.x == Math.round(unit.x) && city.y == Math.round(unit.y)) bInCity = true;
				}
				if (bInCity == true) continue;
			}
			
			unit.render = true;
			
			if (unit.tracks != null)
			{
				RenderTrack(unit.tracks, xCam, yCam);
			}
			
			
		}
		unitBatch.Render(units, mProjectionMatrix, xCam, yCam, (float)frameSeconds);
		
		if (mState.bFogOfWar == true)
		{
			fog.Render(mProjectionMatrix, xCam, yCam);
		}
		
		if (mUIState == UIState.NothingSelected)
		{
			mapTouchHighlight.Render(mProjectionMatrix, xCam, yCam);
		}
		
		/*
		if (bShowMoves == true && tacticalHighlightUnmovedUnits != null)
		{
			tacticalHighlightUnmovedUnits.a =  (float) (Math.sin(mSelectedTime * 4) * 0.07f) + 0.5f;
			tacticalHighlightUnmovedUnits.Render(tacticalViewHLUnMoved, mProjectionMatrix, xCam, yCam);
		}
		*/
		
		if (mUIState == UIState.UnitSelected)
		{
			RenderUnit selectedUnit = GetRenderUnit(mSelectedUnitId);
			IUnit gameUnit = mState.GetUnit(mSelectedUnitId);
			if (selectedUnit != null && gameUnit != null)
			{
				selectReticule.xPos = selectedUnit.x;
				selectReticule.yPos = selectedUnit.y;
				
				if (mSelectedTime < 0.3f)
				{
					float spinTime = 0.3f - mSelectedTime;
					selectReticule.angle = spinTime * 250;
					selectReticule.scale = 1.2f + spinTime * 35;
				} else
				{
					selectReticule.angle = 0;
					selectReticule.scale = 1.2f + (float)Math.sin((mSelectedTime * 2) - 0.3f) * 0.15f;
				}
				
				if (gameUnit.mCarriedBy != -1) selectReticule.scale = selectReticule.scale * 0.5f;
				selectReticule.Render(mProjectionMatrix, xCam,  yCam);
				
				if (possibleMoves != null && possibleMoves.size() > 0 && moveHighlight != null)
				{
					moveHighlight.Render(possibleMoves, mProjectionMatrix, xCam, yCam);
				}
				
				if (fuelRangeMoves != null) fuelHighlight.Render(fuelRangeMoves, mProjectionMatrix, xCam, yCam);
				
				if (possibleAttacks != null && possibleAttacks.size() > 0)
				{
					ArrayList<Position> attacks = new ArrayList<Position>();
					for (Integer attackUnitId : possibleAttacks)
					{
						IUnit unit = mState.GetUnit(attackUnitId);
						if (unit != null) attacks.add(unit.GetPosition());
					}
					attackHighlight.Render(attacks, mProjectionMatrix, xCam, yCam);
					
				}
			}
		}
		
		
		
		if (mUIState == UIState.MoveSelected)
		{
			if (mSelectedMove != null && selectedRoute != null)
			{
				moveHighlight.Render(possibleMoves, mProjectionMatrix, xCam, yCam);
				if (fuelRangeMoves != null) fuelHighlight.Render(fuelRangeMoves, mProjectionMatrix, xCam, yCam);
				RenderPath(arrowParts, selectedRoute, true);
			}
		}
		
		/*
		if (AIInfo.zoneMap != null)
		{
			ArrayList<Position> zone_safe = new ArrayList<Position>();
			ArrayList<Position> zone_front = new ArrayList<Position>();
			ArrayList<Position> zone_enemy = new ArrayList<Position>();
			ArrayList<Position> zone_expansion = new ArrayList<Position>();
			
			for (int x = 0; x < mState.mapSize; ++x)
			{
				for (int y = 0; y < mState.mapSize; ++y)
				{
					if (AIInfo.zoneMap[x][y] == AIInfo.ZONE_SAFE) zone_safe.add(new Position(x, y));
					if (AIInfo.zoneMap[x][y] == AIInfo.ZONE_ENEMY) zone_enemy.add(new Position(x, y));
					if (AIInfo.zoneMap[x][y] == AIInfo.ZONE_EXPANSION) zone_expansion.add(new Position(x, y));
					if (AIInfo.zoneMap[x][y] == AIInfo.ZONE_FRONTLINE) zone_front.add(new Position(x, y));
				}
			}
			
			moveHighlight.a = 0.2f;
			moveHighlight.r = 1.0f;
			moveHighlight.g = 0.0f;
			moveHighlight.b = 0.0f;
			moveHighlight.Render(zone_enemy, mProjectionMatrix, xCam, yCam);
			
			moveHighlight.a = 0.2f;
			moveHighlight.r = 0.0f;
			moveHighlight.g = 0.0f;
			moveHighlight.b = 1.0f;
			moveHighlight.Render(zone_expansion, mProjectionMatrix, xCam, yCam);
			
			moveHighlight.a = 0.4f;
			moveHighlight.r = 1.0f;
			moveHighlight.g = 1.0f;
			moveHighlight.b = 0.0f;
			moveHighlight.Render(zone_front, mProjectionMatrix, xCam, yCam);

			
			moveHighlight.a = 0.2f;
			moveHighlight.r = 0.0f;
			moveHighlight.g = 1.0f;
			moveHighlight.b = 0.0f;
			moveHighlight.Render(zone_safe, mProjectionMatrix, xCam, yCam);

			
		}
		*/
		effectsManagew.RenderEffects((float)frameSeconds, mProjectionMatrix, xCam, yCam);
		
		if (mUIState == UIState.AttackSelected)
		{
			if (mSelectedAttack != -1)
			{
				IUnit attackUnit = mState.GetUnit(mSelectedAttack);
				if (attackUnit != null)
				{
					//effectsManagew.AddEffect(attackUnit.GetPosition().x, attackUnit.GetPosition().y, "small_explosion");
					Position pos = attackUnit.GetPosition();
					attackReticule.xPos = pos.x;
					attackReticule.yPos = pos.y;
					float spinTime = mSelectedTime;
					attackReticule.angle = spinTime * 50;
					attackReticule.Render(mProjectionMatrix, xCam, yCam);
				}
			}
		}
		
		if (mState.bLocalGame == true)
		{
			if (mState.GetPlayerToPlay() != GetCurrentUserId())
			{
				DrawAICog(mTimeElapsed);
			}
		}
		
		
		//Log.i("TactLog", "Frame!");
	}
	
	@Override
	public void onSurfaceChanged(final GL10 gl, final int width, final int height)
	{
		mWidth = width;
		mHeight = height;
		GLES20.glViewport(0, 0, width, height);
		GLES20.glDepthMask(false);
					 
		Log.i("TactLog", "onSurfaceChanged - " + mWidth + ", " + mHeight);
	}


	@Override
	public void onSurfaceCreated(final GL10 gl, final EGLConfig config)
	{
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		Log.i("TactLog", "onSurfaceCreated");
	
		//if (bInitDone == false)
		FirstInit();
	}

}
