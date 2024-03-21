package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import uk.co.eidolon.tact2.TextureManager.UnitDefinition;
import Core.EventInjector;
import Core.StateEngine;
import Tactikon.State.City;
import Tactikon.State.EventBoardUnit;
import Tactikon.State.EventChangeProduction;
import Tactikon.State.EventLeaveTransport;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBattleship;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitBomber;
import Tactikon.State.UnitCarrier;
import Tactikon.State.UnitFighter;
import Tactikon.State.UnitHelicopter;
import Tactikon.State.UnitInfantry;
import Tactikon.State.UnitSub;
import Tactikon.State.UnitTank;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class CityFragment extends DialogFragment
{
	public CityFragment()
	{
	}
	
	StateEngine mEngine;
	EventInjector mInjector;
	int mCityId;
	
	City mCity;
	TactikonState mState;
	
	ImageView mFortified[] = new ImageView[4];
	IUnit mFortUnit[] = new IUnit[4];
	
	ImageView mDragImage;
	RelativeLayout mDragArea;
	IUnit mDragUnit;
	
	int mSelectedUnit = -1;
	
	@Override
	public void onConfigurationChanged(Configuration config)
	{
		DismissCityButton();
	}
	
	public void SetDetails(StateEngine engine, int cityId)
	{
		mEngine = engine;
		mInjector = new EventInjector(engine);
		mCityId = cityId;
		
		mState = (TactikonState)mEngine.GetState();
		mCity = mState.GetCity(cityId);
		
		if (getView() != null)
		{
			Setup(getView());
		}
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//setRetainInstance(true);
		
		AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();
		StateEngine engine = appWrapper.GetCurrentStateEngine();
		int cityId = getArguments().getInt("CityId");
		SetDetails(engine, cityId);
	}
	
	void SetLCDUnitImage(ImageView imageView, IUnit unit, boolean addHealthPips)
	{	
		if (unit == mDragUnit)
		{
			imageView.setImageDrawable(null);
			return;
		}
        AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();
        TextureManager texMgr = appWrapper.GetTextureManager();
        UnitDefinition tex = texMgr.GetUnitDefinition(unit.getClass().getSimpleName(), unit.mConfig);
        if (tex == null) tex = texMgr.GetUnitDefinition("UnitTank", unit.mConfig);
        //Bitmap.Config config = new Bitmap.Config();
        
        int width = tex.mergedTexture.width;
        int height = tex.mergedTexture.height;
        
        Bitmap bmOverlay = Bitmap.createBitmap(width, height, tex.mergedTexture.bitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay); 
        canvas.drawBitmap(tex.mergedTexture.bitmap, 0, 0, null);
        
        bmOverlay = Bitmap.createScaledBitmap(bmOverlay, width * 3, height * 3, false);
        
        if (unit.mCarrying.size() > 0)
        {
        	int xPos = 0;
        	canvas = new Canvas(bmOverlay); 
        	for (Integer i : unit.mCarrying)
        	{
        		IUnit carryUnit = mState.GetUnit(i);
        		if (carryUnit != mDragUnit)
        		{
        			UnitDefinition texCarry = texMgr.GetUnitDefinition(carryUnit.getClass().getSimpleName(), carryUnit.mConfig);
        			if (texCarry == null) texCarry = texMgr.GetUnitDefinition("UnitTank", 0);
                //	Bitmap.Config config = new Bitmap.Config();
        			
        			Bitmap bmap1 = Bitmap.createScaledBitmap(texCarry.mergedTexture.bitmap, 32, 32, false);
        			canvas.drawBitmap(bmap1, (width * 2) - xPos, (width * 3)-32, null);
        		}
                
                xPos = xPos + 32;
        	}
        }
        
        if (addHealthPips)
        {
        	Canvas healthCanvas = new Canvas(bmOverlay);
        	Bitmap healthPip = texMgr.GetTexture("health_pip").bitmap;
        	for (int i = 0; i < unit.health; ++i)
        	{
        		healthCanvas.drawBitmap(healthPip, i * 12, 0, null);
        	}
        }
        
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmOverlay);
        
        ColorMatrix bwMatrix = new ColorMatrix();
        bwMatrix.setSaturation(0);
        bwMatrix.setScale(0.3f, 0.4f, 0.2f, 0.8f);
        final ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(bwMatrix);
        imageView.setColorFilter(colorFilter);
        imageView.setImageDrawable(bitmapDrawable);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.city_fragment, container, false);

        // Block touches from going through the city dialog
        view.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
            /** Intercepts touches from going through. */
            return true;
            }
        });

        AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();


        // Set font to LCD for all text
        TextView textProduction = (TextView)view.findViewById(R.id.text_production);
        textProduction.setTypeface(appWrapper.pixelFont);

        TextView textTurns = (TextView)view.findViewById(R.id.curr_text_turns);
        textTurns.setTypeface(appWrapper.pixelFont);

        TextView textNumber = (TextView)view.findViewById(R.id.curr_text_number);
        textNumber.setTypeface(appWrapper.pixelFont);

        TextView textFortified = (TextView)view.findViewById(R.id.text_fortified);
        textFortified.setTypeface(appWrapper.pixelFont);

        Button productionButton = (Button)view.findViewById(R.id.button_change);
        productionButton.setTypeface(appWrapper.pixelFont);

        TextView textDismiss = (TextView)view.findViewById(R.id.text_dismiss);
        textDismiss.setTypeface(appWrapper.pixelFont);

		TextView textReason = (TextView)view.findViewById(R.id.text_onholdreason);
		textReason.setTypeface(appWrapper.pixelFont);

        // Setup production pager
        final ViewPager unitPager = (ViewPager) view.findViewById(R.id.unit_select_gallery);
		PagerAdapter unitAdapter = new UnitAdapter(getActivity());
		unitPager.setAdapter(unitAdapter);
		//Necessary or the pager will only have one extra page to show
		// make this at least however many pages you can see
		unitPager.setOffscreenPageLimit(9);
		//A little space between pages
		unitPager.setPageMargin(0);

		//If hardware acceleration is enabled, you should also remove
		// clipping on the pager for its children.
		unitPager.setClipChildren(false);

		unitAdapter.notifyDataSetChanged();

		mDragArea = (RelativeLayout)view.findViewById(R.id.drag_area);

		// Set up the production choice area
		productionButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				ChangeProductionButton();
			}
        });

		textDismiss.setClickable(true);
		textDismiss.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				DismissCityButton();
			}
        });

		Setup(view);

		view.setVisibility(View.VISIBLE);

        return view;

	}
	
	void DismissCityButton()
	{
		Activity hostActivity = (Activity)getView().getContext();
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
	    //if (mFragment == null)
	    {
	    	Fragment fragment = fragmentManager.findFragmentByTag("CityFragment");
	    	if (fragment != null)
	    	{
	    		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	    		//fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
	    		fragmentTransaction.remove(fragment);
	    		fragmentTransaction.commit();
	    	}
	    }
	}
	
	ViewPropertyAnimator anim1;
	ViewPropertyAnimator anim2;
	
	void CrossFade(final View fromView, View toView, int duration)
	{
		toView.clearAnimation();
		fromView.clearAnimation();
		
		if (anim1 != null)
		{
			anim1.cancel();
		}
		
		if (anim2 != null)
		{
			anim2.cancel();
		}
		
		toView.setAlpha(0f);
		toView.setVisibility(View.VISIBLE);
		
	    // Animate the content view to 100% opacity, and clear any animation
	    // listener set on the view.
		anim1 = toView.animate()
	            .alpha(1f)
	            .setDuration(duration)
	            .setListener(null);

	    // Animate the loading view to 0% opacity. After the animation ends,
	    // set its visibility to GONE as an optimization step (it won't
	    // participate in layout passes, etc.)
	    anim2 = fromView.animate()
	            .alpha(0f)
	            .setDuration(duration)
	            .setListener(new AnimatorListenerAdapter() {
	                @Override
	                public void onAnimationEnd(Animator animation) {
	                	fromView.setVisibility(View.INVISIBLE);
	                }
	            });
	}
	
	void ChangeProductionButton()
	{
		final ViewPager unitPager = (ViewPager) getView().findViewById(R.id.unit_select_gallery);
		UnitAdapter unitAdapter = (UnitAdapter)unitPager.getAdapter();
		
		for (IUnit unit : unitAdapter.mUnits)
		{
			if (unit == null) continue;
			if (unit.getClass().getSimpleName().compareTo(mCity.productionType) == 0)
			{
				unitPager.setCurrentItem(unitAdapter.mUnits.indexOf(unit) - 1, true);
			}
		}
		
		// hide the change area and show the pager
		LinearLayout chooseLayout = (LinearLayout)getView().findViewById(R.id.choose_production_layout);
		LinearLayout currentLayout = (LinearLayout)getView().findViewById(R.id.current_production_layout);
		
		CrossFade(currentLayout, chooseLayout, 300);
		
		
	}
	
	public void ClickedToWakeFortifiedUnit(int unitId)
	{
		View gameActivityView = (View)getActivity().findViewById(R.id.gameplayarea);
		GameView gameView = (GameView) gameActivityView.findViewWithTag("GameView");
		gameView.SetSelectedUnit(mState.GetUnit(unitId));
		
		DismissCityButton();
	}
	
	boolean bDragging = false;
	
	int dragX = 0;
	int dragY = 0;
	
	void StartDragging(View view, IUnit unit)
	{
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		
		bDragging = true;
    	
    	mDragImage = new ImageView((Context)getActivity().getApplicationContext());
    	SetLCDUnitImage(mDragImage, unit, true);
    	mDragImage.setMaxHeight(52);
    	mDragImage.setMaxWidth(52);
    	mDragImage.setMinimumHeight(52);
    	mDragImage.setMinimumWidth(52);
    	mDragImage.setScaleType(ScaleType.CENTER);
		mDragImage.setVisibility(View.INVISIBLE);
    	
		mDragArea.addView(mDragImage, params);
		mDragUnit = unit;
		
		dragX = mDragArea.getWidth();
		dragY = mDragArea.getHeight();
		
		Setup(getView());
	}
	
	void DragTouch(View view, MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_MOVE)
		{
			
			if (bDragging == true)
			{
    		// 	render the drag version
				int[] loc = new int[2];
				view.getLocationInWindow(loc);
				
				int[] loc2 = new int[2];
				mDragArea.getLocationInWindow(loc2);
				
				float ex = event.getX();
				float ey = event.getY();
				
				float x = (ex + loc[0] - loc2[0]);
				float y = (ey + loc[1] - loc2[1]);
				
				if (x > dragX - 70)
				{
					x = dragX - 70;
				}
				if (y > dragY - 50)
				{
					y = dragY - 50;
				}
				if (x < 20) x = 20;
				if (y < 20) y = 20;
				
				y = y - 60;
				x = x - 25;
				
				mDragImage.setVisibility(View.VISIBLE);				
				mDragImage.setPadding((int) x, (int) y, 0, 0);
				mDragImage.invalidate();
				
			}
		}
	}
	
	void StopDragging(View view, MotionEvent event)
	{
		if (bDragging ==false) return;
		mDragImage.setVisibility(View.GONE);
		bDragging = false;
		mDragArea.removeView(mDragImage);
		
    	
    	if (mDragImage == null) return;
    	if (mDragUnit == null) return;
    	
    	
    	int[] eventOffset = new int[2];
		view.getLocationInWindow(eventOffset);
		
		Point eventPoint = new Point();
		eventPoint.x = (int)(event.getX() + eventOffset[0]);
		eventPoint.y = (int)(event.getY() + eventOffset[1]);
		

		
		IUnit targetUnit = null;
    	for (int i = 0; i < 4; ++i)
    	{
    		Rect targetRect = new Rect();
    		mFortified[i].getGlobalVisibleRect(targetRect);
    		if (targetRect.contains(eventPoint.x, eventPoint.y))
    		{
    			targetUnit = mFortUnit[i];
    		}
    	}
    	
    	// we've hit the unit we started on - wake the unit
    	if (targetUnit == mDragUnit)
    	{
    		ClickedToWakeFortifiedUnit(mDragUnit.mUnitId);
    	}
    	// we've hit a unit we can board - board it
    	if (targetUnit != null && targetUnit.CanCarry(mDragUnit) && targetUnit.mCarrying.size() < targetUnit.CarryCapacity())
    	{
    		// we want to shift this unit into the target unit
    		EventBoardUnit boardEvent = new EventBoardUnit();
    		boardEvent.mTransporter = targetUnit.mUnitId;
    		boardEvent.mUnit = mDragUnit.mUnitId;
    		mInjector.AddEvent(boardEvent);
    		SetDetails(mEngine, mCityId);
    	}
    	
    	// we've hit an empty space - unboard it
    	if (targetUnit == null && mDragUnit.mCarriedBy != -1)
    	{
    		EventLeaveTransport leaveEvent = new EventLeaveTransport();
    		leaveEvent.mTransporter = mDragUnit.mCarriedBy;
    		leaveEvent.mUnit = mDragUnit.mUnitId;
    		mInjector.AddEvent(leaveEvent);
    		SetDetails(mEngine, mCityId);
    	}
    	
    	// dispatch event if we have one, get the new version of the state
    	
    	
    	mDragUnit = null;
    	Setup(getView());
    	
    	
	}
	
	public void Setup(View view)
	{
		// what's currently being produced?
		ArrayList<IUnit> units = mState.GetUnitTypes();
		IUnit productionUnit = null;
		for (IUnit unit : units)
		{
			if (unit.getClass().getSimpleName().compareTo(mCity.productionType) == 0)
			{
				productionUnit = unit;
			}
		}
		
		if (productionUnit != null)
		{
			ImageView prodImage = (ImageView)view.findViewById(R.id.image_production);
			SetLCDUnitImage(prodImage, productionUnit, false);
        
			TextView textNumber = (TextView)view.findViewById(R.id.curr_text_number);
			int cityTurns = mCity.turnsToProduce;
			if (cityTurns == 0) cityTurns = 1;
			textNumber.setText(Integer.toString(cityTurns));
			
			TextView textTurns = (TextView)view.findViewById(R.id.curr_text_turns);
			TextView textReason = (TextView)view.findViewById(R.id.text_onholdreason);
			
			boolean bOnHold = false;
			String strOnHoldReason = "";
			if (mCity.fortifiedUnits.size() == mState.MAX_UNITS_IN_CITY)
			{
				bOnHold = true;
				strOnHoldReason = "[NO SPACE]";
			}
			if (mCity.fortifiedUnits.size() > 0)
			{
				for (Integer unitId : mCity.fortifiedUnits)
				{
					IUnit unit = mState.GetUnit(unitId);
					if (unit.health < 3)
					{
						bOnHold = true;
						strOnHoldReason = "[REPAIRS]";
					}
					for (Integer carryUnitId : unit.mCarrying)
					{
						IUnit carryUnit = mState.GetUnit(carryUnitId);
						if (carryUnit.health < 3)
						{
							bOnHold = true;
							strOnHoldReason = "[REPAIRS]";
						}
					}
				}
			}
			
			if (bOnHold)
			{
				textNumber.setText("");
				textTurns.setText("ON HOLD");
				textReason.setText(strOnHoldReason);
				textReason.setVisibility(View.VISIBLE);
			} else
			{
				textReason.setVisibility(View.GONE);
				if (cityTurns == 1)
				{
			
					textTurns.setText("TURN");
				} else
				{
					textTurns.setText("TURNS");
				}
			}
			
			textTurns.invalidate();
			
		} else
		{
			TextView textReason = (TextView)view.findViewById(R.id.text_onholdreason);
			textReason.setVisibility(View.GONE);
			
			ImageView prodImage = (ImageView)view.findViewById(R.id.image_production);
			prodImage.setImageDrawable(null);
			TextView textNumber = (TextView)view.findViewById(R.id.curr_text_number);
			textNumber.setText("");
			TextView textTurns = (TextView)view.findViewById(R.id.curr_text_turns);
			textTurns.setText("NOTHING");
			textTurns.invalidate();
		}
		
		
		// and populate the fortified units
		mFortified[0] = (ImageView)view.findViewById(R.id.fortified1);
		mFortified[1] = (ImageView)view.findViewById(R.id.fortified2);
		mFortified[2] = (ImageView)view.findViewById(R.id.fortified3);
		mFortified[3] = (ImageView)view.findViewById(R.id.fortified4);
		
		mFortUnit = new IUnit[4];
		
		int fortIndex = 0;
		for (Integer unitId : mCity.fortifiedUnits)
		{
			final IUnit unit = mState.GetUnit(unitId);
			
			SetLCDUnitImage(mFortified[fortIndex], unit, true);
			mFortified[fortIndex].setClickable(true);
			mFortUnit[fortIndex] = unit;
			
			/*
			mFortified[fortIndex].setOnClickListener(new OnClickListener()
			{
				public void onClick(View arg0)
				{
					ClickedToWakeFortifiedUnit(unit.mUnitId);
				}
	        });*/
			
			//if (unit.mCarrying.size() > 0)
			{
				mFortified[fortIndex].setOnTouchListener(new View.OnTouchListener() {
					public boolean onTouch(View v, MotionEvent event) {
						
						if (v == null) return false;
						if (event.getAction() == MotionEvent.ACTION_DOWN)
						{
							if (bDragging == false)
							{
								//Log.i("Info", "TransX: " + v.getTranslationX());
								//Log.i("Info", "Event: (" + event.getX() + ", " + event.getY() + ") V: " + v.getX() + ", " + v.getY() + ") Vdim: ("+v.getWidth() + ", " + v.getHeight() +")");
								if (unit.mCarrying.size() > 0 && event.getX() > (v.getWidth() * 0.7) && event.getY() > (v.getHeight() * 0.7))
								{
									IUnit carryUnit = mState.GetUnit(unit.mCarrying.get(0));
									StartDragging(v, carryUnit);
									return true;
								}
								if (unit.mCarrying.size() > 1 && event.getX() < (v.getWidth() * 0.7) && event.getY() > (v.getHeight() * 0.7))
								{
									IUnit carryUnit = mState.GetUnit(unit.mCarrying.get(1));
									StartDragging(v, carryUnit);
									return true;
								}
								if (event.getX() < (v.getWidth() * 0.4) || event.getY() < (v.getHeight() * 0.7))
								{
									StartDragging(v, unit);
									return true;
								}
							}
							return true;
						}
						
						if (event.getAction() == MotionEvent.ACTION_MOVE)
						{
							if (bDragging == true) DragTouch(v, event);
							return true;
						}
						
						if (event.getAction() == MotionEvent.ACTION_CANCEL)
						{
							bDragging = false;
							return true;
						}
						
						if (event.getAction() == MotionEvent.ACTION_UP)
						{
							
							StopDragging(v, event);
							return true;
						}
						
						return false;
					}
	    		});
			}
			
			
			fortIndex ++;
		}
		
		for (;fortIndex < 4; ++fortIndex)
		{
			mFortified[fortIndex].setImageDrawable(null);
		}

		UnitAdapter unitAdapter = new UnitAdapter(getActivity());
		
		final ViewPager unitPager = (ViewPager) view.findViewById(R.id.unit_select_gallery);
		int i = unitPager.getCurrentItem();
		unitPager.setAdapter(unitAdapter);
		
		for (IUnit unit : unitAdapter.mUnits)
		{
			if (unit == null) continue;
			if (unit.getClass().getSimpleName().compareTo(mCity.productionType) == 0)
			{
				mSelectedUnit = unitAdapter.mUnits.indexOf(unit);
			}
		}
		
		unitPager.setAdapter(unitAdapter);
		unitPager.setCurrentItem(i, false);
		
		view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    	view.invalidate();
		
	}
	
	void SetProductionItem(String unitClass)
	{
		LinearLayout chooseLayout = (LinearLayout)getView().findViewById(R.id.choose_production_layout);
		LinearLayout currentLayout = (LinearLayout)getView().findViewById(R.id.current_production_layout);
		
		CrossFade(chooseLayout, currentLayout, 300);
		
		if (unitClass.compareTo(mCity.productionType) != 0)
		{
		
			EventChangeProduction changeProductionEvent = new EventChangeProduction();
			changeProductionEvent.cityId = mCityId;
			changeProductionEvent.unitClass = unitClass;
			
			mInjector.AddEvent(changeProductionEvent);
			
			SetDetails(mEngine, mCityId);
		}
		
		Setup(getView());
		
		//getView().invalidate();
	}
	
	private class UnitAdapter extends PagerAdapter
	 {
		 Context mContext;
		 
		 ArrayList<IUnit> mUnits = new ArrayList<IUnit>();
		 
		 UnitAdapter(Context context)
		 {
			mContext = context;
			
			mUnits = mState.GetProductionUnits(mCity.cityId);
			mUnits.add(0, null);
			mUnits.add(null);
		 }
		 
		 void ClickedProductionItem(String unitClass)
		 {
			 SetProductionItem(unitClass);
		 }
		 
		 @Override
		 public Object instantiateItem(ViewGroup container, int position)
		 {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final IUnit unit = mUnits.get(position);
			
			 
			final LinearLayout view = (LinearLayout)inflater.inflate(R.layout.unit_select_item, null);
			
			AppWrapper appWrapper = (AppWrapper)getActivity().getApplication();
			
			ImageView unitImage = (ImageView)view.findViewById(R.id.image_unit);
			TextView unitName = (TextView)view.findViewById(R.id.text_name);
			TextView unitTurns = (TextView)view.findViewById(R.id.text_turns);
			
			if (unit == null)
			{
				unitName.setText("");
				unitTurns.setText("");
				unitImage.setImageDrawable(null);
			} else
			{
				unitName.setTypeface(appWrapper.pixelFont);
				unitTurns.setTypeface(appWrapper.pixelFont);
				unitName.setText(unit.GetName());
				unitTurns.setText(unit.GetProductionTime(mCity, mState) + " TURNS");
				
				
				if (position == mSelectedUnit)
				{
					unitImage.setBackgroundColor(Color.argb(30,0,0,0));
					
					if (mCity.turnsToProduce > 1)
					{
						unitTurns.setText(mCity.turnsToProduce + " TURNS");
					} else if (mCity.turnsToProduce >= 0)
					{
						unitTurns.setText(mCity.turnsToProduce + " TURN");
					}
					
				}
				
				SetLCDUnitImage(unitImage, unit, false);
				
				view.setClickable(true);
				
				view.setOnClickListener(new OnClickListener()
				{
					public void onClick(View arg0)
					{
						
						ClickedProductionItem(unit.getClass().getSimpleName());
					}
		        });
			}
			 
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
			 return mUnits.size();
		 }
		  
		 @Override
		 public boolean isViewFromObject(View view, Object object)
		 {
			 return (view == object);
		 }

		 @Override
		 public float getPageWidth(int pos)
		 {
			 return 0.3f;
		 }
		 

	}
	
	
}
