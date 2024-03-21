package uk.co.eidolon.tact2;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import uk.co.eidolon.shared.utils.IAppWrapper;
import uk.co.eidolon.tact2.TextureManager.UnitDefinition;
import Core.EventInjector;
import Core.StateEngine;
import Tactikon.State.Battle;
import Tactikon.State.Battle.Outcome;
import Tactikon.State.City;
import Tactikon.State.EventAttackUnit;
import Tactikon.State.EventBoardUnit;
import Tactikon.State.EventChangeProduction;
import Tactikon.State.EventLeaveTransport;
import Tactikon.State.IBattleBonus;
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
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AttackFragment extends Fragment
{
	public AttackFragment()
	{
	}
	
	StateEngine mEngine;
	EventInjector mInjector;
	TactikonState mState;
	
	int mAttackId;
	int mDefendId;
	
	public void SetDetails(StateEngine engine, int attackId, int defendId)
	{
		mEngine = engine;
		mInjector = new EventInjector(engine);
		mState = (TactikonState)mEngine.GetState();
		
		
		mAttackId = attackId;
		mDefendId = defendId;
	}
	
	@Override
	public void onConfigurationChanged(Configuration config)
	{
		DismissAttackButton();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		//setRetainInstance(true);
		
		AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();
		StateEngine engine = appWrapper.GetCurrentStateEngine();
		int attackId = getArguments().getInt("AttackId");
		int defendId = getArguments().getInt("DefendId");
		SetDetails(engine, attackId, defendId);
	}
	
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.attack_fragment, container, false);
        
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
	
        Button attackButton = (Button)view.findViewById(R.id.attack_button);
        attackButton.setTypeface(appWrapper.pixelFont);
        
        TextView attAttText = (TextView)view.findViewById(R.id.att_att_title);
        attAttText.setTypeface(appWrapper.pixelFont);
        TextView attDefText = (TextView)view.findViewById(R.id.att_def_title);
        attDefText.setTypeface(appWrapper.pixelFont);
        TextView defAttText = (TextView)view.findViewById(R.id.def_att_title);
        defAttText.setTypeface(appWrapper.pixelFont);
        TextView defDefText = (TextView)view.findViewById(R.id.def_def_title);
        defDefText.setTypeface(appWrapper.pixelFont);
        
        TextView youText = (TextView)view.findViewById(R.id.you_text);
        youText.setTypeface(appWrapper.pixelFont);
        TextView chanceText = (TextView)view.findViewById(R.id.chance_text);
        chanceText.setTypeface(appWrapper.pixelFont);
        TextView enemyText = (TextView)view.findViewById(R.id.enemy_text);
        enemyText.setTypeface(appWrapper.pixelFont);
        
        
        
        TextView textTitle = (TextView)view.findViewById(R.id.title_text);
        textTitle.setTypeface(appWrapper.pixelFont);
        TextView textVs = (TextView)view.findViewById(R.id.vs_text);
        textVs.setTypeface(appWrapper.pixelFont);
        
        TextView attackerAttackStat = (TextView)view.findViewById(R.id.att_att_stat);
        attackerAttackStat.setTypeface(appWrapper.pixelFont);
        TextView attackerDefendStat = (TextView)view.findViewById(R.id.att_def_stat);
        attackerDefendStat.setTypeface(appWrapper.pixelFont);
        TextView defenderAttackStat = (TextView)view.findViewById(R.id.def_att_stat);
        defenderAttackStat.setTypeface(appWrapper.pixelFont);
        TextView defenderDefendStat = (TextView)view.findViewById(R.id.def_def_stat);
        defenderDefendStat.setTypeface(appWrapper.pixelFont);
		
		
		TextView resultsText = (TextView)view.findViewById(R.id.results_text);
		resultsText.setTypeface(appWrapper.pixelFont);
        
        TextView dismissText =(TextView)view.findViewById(R.id.dismiss_text);
        dismissText.setTypeface(appWrapper.pixelFont);
        
        dismissText.setClickable(true);
        dismissText.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				DismissAttackButton();
			}
        });
        
        //Button attackButton = (Button)view.findViewById(R.id.attack_button);
        attackButton.setOnClickListener(new OnClickListener()
        {
        	public void onClick(View arg0)
        	{
        		AttackButton();
        	}
        });
		
		Setup(view);
        
        return view;
	}
	
	void AttackButton()
	{
		// we want to shift this unit into the target unit
		EventAttackUnit attackEvent = new EventAttackUnit(mAttackId, mDefendId);
		
		mInjector.AddEvent(attackEvent);
		//SetDetails(mEngine, mAttackId, mDefendId);
		DismissAttackButton();
	}
	
	void DismissAttackButton()
	{
		if (getView() == null) return;
		Activity hostActivity = (Activity)getView().getContext();
		if (hostActivity == null) return;
		FragmentManager fragmentManager = hostActivity.getFragmentManager();
	    {
	    	Fragment fragment = fragmentManager.findFragmentByTag("AttackFragment");
	    	if (fragment != null)
	    	{
	    		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	    		//fragmentTransaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
	    		fragmentTransaction.remove(fragment);
	    		fragmentTransaction.commit();
	    	}
	    }
	}
	
	void SetLCDUnitImage(ImageView imageView, IUnit unit)
	{	
		if (unit == null) return;
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
        		UnitDefinition texCarry = texMgr.GetUnitDefinition(carryUnit.getClass().getSimpleName(), unit.mConfig);
        		if (texCarry == null) texCarry = texMgr.GetUnitDefinition("UnitTank", unit.mConfig);
                Bitmap bmap1 = Bitmap.createScaledBitmap(texCarry.mergedTexture.bitmap, 32, 32, false);
        		canvas.drawBitmap(bmap1, (width * 2) - xPos, (width * 3)-32, null);
        		
                xPos = xPos + 32;
        	}
        }
        
        Canvas healthCanvas = new Canvas(bmOverlay);
        Bitmap healthPip = texMgr.GetTexture("health_pip").bitmap;
        for (int i = 0; i < unit.health; ++i)
        {
        	healthCanvas.drawBitmap(healthPip, i * 12, 0, null);
        }
        
        
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmOverlay);
        
        ColorMatrix bwMatrix = new ColorMatrix();
        bwMatrix.setSaturation(0);
        bwMatrix.setScale(0.3f, 0.4f, 0.2f, 0.8f);
        final ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(bwMatrix);
        imageView.setColorFilter(colorFilter);
        imageView.setImageDrawable(bitmapDrawable);
	}
	
	
	private void SetHealthPips(ImageView image, int health, int loss)
	{
		AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();
        TextureManager texMgr = appWrapper.GetTextureManager();
        
        Bitmap healthPip = texMgr.GetTexture("health_pip_large").bitmap;
        Bitmap crossHealthPip = texMgr.GetTexture("cross_health_pip").bitmap;
        
        Bitmap bmOverlay = Bitmap.createBitmap(32*3, 32, healthPip.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        
        
        for (int i = 0; i < health; ++i)
        {
        	if (i>= (health-loss))
        	{
        		canvas.drawBitmap(crossHealthPip, i*28, 0, null);
        	} else
        	{
        		canvas.drawBitmap(healthPip, i*28, 0, null);
        	}
        }
        
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmOverlay);
        
        image.setImageDrawable(bitmapDrawable);
		
	}
	
	public void Setup(View view)
	{
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		AppWrapper appWrapper = (AppWrapper) getActivity().getApplicationContext();
        
		ImageView attackerImage = (ImageView)view.findViewById(R.id.attack_image);
		ImageView defenderImage = (ImageView)view.findViewById(R.id.defender_image);
		
		TextView outcomeText = (TextView)view.findViewById(R.id.results_text);
		
		IUnit attackerUnit = mState.GetUnit(mAttackId);
		IUnit defenderUnit = mState.GetUnit(mDefendId);
		
		if (attackerUnit == null || defenderUnit == null)
		{
			DismissAttackButton();
			return;
		}
		
		SetLCDUnitImage(attackerImage, attackerUnit);
		SetLCDUnitImage(defenderImage, defenderUnit);
		
		Battle battle = new Battle(mState, mAttackId, mDefendId);
		
		TextView attackerAttackStat = (TextView)view.findViewById(R.id.att_att_stat);
        TextView attackerDefendStat = (TextView)view.findViewById(R.id.att_def_stat);
        TextView defenderAttackStat = (TextView)view.findViewById(R.id.def_att_stat);
        TextView defenderDefendStat = (TextView)view.findViewById(R.id.def_def_stat);
        
		
        attackerAttackStat.setText(Integer.toString(attackerUnit.GetAttack(defenderUnit, mState)));
        attackerDefendStat.setText(Integer.toString(attackerUnit.GetDefence(defenderUnit, mState)));
        defenderAttackStat.setText(Integer.toString(defenderUnit.GetAttack(attackerUnit, mState)));
        defenderDefendStat.setText(Integer.toString(defenderUnit.GetDefence(attackerUnit, mState)));
		
		ArrayList<IBattleBonus> attackBonuses = attackerUnit.GetBonuses(mState);
		ArrayList<IBattleBonus> defendBonuses = defenderUnit.GetBonuses(mState);
		
		
		LinearLayout bonusArea = (LinearLayout)view.findViewById(R.id.modifier_area);
		bonusArea.removeAllViews();
		int i = 0;
		while (i < attackBonuses.size() || i < defendBonuses.size())
		{
			View bonusView = inflater.inflate(R.layout.bonus_row, bonusArea, false);
			if (i < attackBonuses.size())
			{
				TextView bonusText = (TextView)bonusView.findViewById(R.id.att_bonus);
				bonusText.setTypeface(appWrapper.pixelFont);
				
				IBattleBonus bonus = attackBonuses.get(i);
				bonusText.setText(bonus.GetBonusName());
				
				if (bonus.GetAtkValue() != 0)
				{
					TextView text = (TextView)bonusView.findViewById(R.id.att_att_stat);
					text.setTypeface(appWrapper.pixelFont);
					String plus = "";
					if (bonus.GetAtkValue() > 0) plus = "+";
					text.setText(plus + bonus.GetAtkValue());
				}
				if (bonus.GetDefValue() != 0)
				{
					TextView text = (TextView)bonusView.findViewById(R.id.att_def_stat);
					text.setTypeface(appWrapper.pixelFont);
					String plus = "";
					if (bonus.GetDefValue() > 0) plus = "+";
					text.setText(plus + bonus.GetDefValue());
				}
				
				
				
			}
			if (i < defendBonuses.size())
			{
				TextView bonusText = (TextView)bonusView.findViewById(R.id.def_bonus);
				bonusText.setTypeface(appWrapper.pixelFont);
				
				IBattleBonus bonus = defendBonuses.get(i);
				bonusText.setText(bonus.GetBonusName());
				
				if (bonus.GetAtkValue() != 0)
				{
					TextView text = (TextView)bonusView.findViewById(R.id.def_att_stat);
					text.setTypeface(appWrapper.pixelFont);
					String plus = "";
					if (bonus.GetAtkValue() > 0) plus = "+";
					text.setText(plus + bonus.GetAtkValue());
				}
				if (bonus.GetDefValue() != 0)
				{
					TextView text = (TextView)bonusView.findViewById(R.id.def_def_stat);
					text.setTypeface(appWrapper.pixelFont);
					String plus = "";
					if (bonus.GetDefValue() > 0) plus = "+";
					text.setText(plus + bonus.GetDefValue());
				}
			}
			
			bonusArea.addView(bonusView);
			i++;
			
		}
		
		LinearLayout outcomeArea = (LinearLayout)view.findViewById(R.id.results_area);
		outcomeArea.removeAllViews();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		// if the user has friends-only invites enabled, don't do anything
		boolean detailedCombat = prefs.getBoolean("detailedStats", false);
		
		if (detailedCombat == true)
		{
			outcomeText.setText("DAMAGE ODDS    ");
		float bestChance = 0;
		for (Outcome outcome : battle.out)
		{
			if (outcome.probability > bestChance)
			{
				bestChance = outcome.probability;
			}
		}
		
		for (Outcome outcome : battle.out)
		{
			View resultView = inflater.inflate(R.layout.result_row, outcomeArea, false);
			
			ImageView attackerLossImage = (ImageView)resultView.findViewById(R.id.attacker_loss_image);
			ImageView defenderLossImage = (ImageView)resultView.findViewById(R.id.defender_loss_image);
			TextView probabilityText = (TextView)resultView.findViewById(R.id.probability_text);
			
			probabilityText.setTypeface(appWrapper.pixelFont);
			
			SetHealthPips(attackerLossImage, attackerUnit.health, outcome.unitId1Damage);
			SetHealthPips(defenderLossImage, defenderUnit.health, outcome.unitId2Damage);
			
			int percentage = Math.round(outcome.probability * 100.0f);
			
			probabilityText.setText(Integer.toString(percentage) + "%");
			
			if (outcome.probability == bestChance)
			{
				probabilityText.setAlpha(1.0f);
			} else
			{
				probabilityText.setAlpha(0.65f);
			}
			
			outcomeArea.addView(resultView);
		}
		} else
		{
			outcomeText.setText("HIT CHANCE       ");
			View resultView = inflater.inflate(R.layout.result_row_simple, outcomeArea, false);
			TextView attackerDamage = (TextView)resultView.findViewById(R.id.attacker_damage);
			TextView defenderDamage = (TextView)resultView.findViewById(R.id.defender_damage);
			TextView resultChance = (TextView)resultView.findViewById(R.id.probability_text);
			
			resultChance.setText("1 hit");
			resultChance.setTypeface(appWrapper.pixelFont);
			
			attackerDamage.setTypeface(appWrapper.pixelFont);
			defenderDamage.setTypeface(appWrapper.pixelFont);
			float attDamagePerc = 0;
			float defDamagePerc = 0;
			for (Outcome outcome : battle.out)
			{
				if (outcome.unitId1Damage == 1)
				{
					if (outcome.unitId1 == attackerUnit.mUnitId)
					{
						attDamagePerc += outcome.probability;
					} else
					{
						defDamagePerc += outcome.probability;
					}
				}
			
				if (outcome.unitId2Damage == 1)
				{
					if (outcome.unitId2 == attackerUnit.mUnitId)
					{
						attDamagePerc += outcome.probability;
					} else
					{
						defDamagePerc += outcome.probability;
					}
				}
				
			}
			String attDamage = Integer.toString(Math.round(attDamagePerc * 100.0f)) + "%";
			String defDamage = Integer.toString(Math.round(defDamagePerc * 100.0f)) + "%";
			attackerDamage.setText(defDamage);
			defenderDamage.setText(attDamage);
			outcomeArea.addView(resultView);
			
			// SECOND ATTACK - attacker only!

			resultView = inflater.inflate(R.layout.result_row_simple, outcomeArea, false);
			attackerDamage = (TextView)resultView.findViewById(R.id.attacker_damage);
			defenderDamage = (TextView)resultView.findViewById(R.id.defender_damage);
			resultChance = (TextView)resultView.findViewById(R.id.probability_text);
			
			resultChance.setText("2 hits");
			resultChance.setTypeface(appWrapper.pixelFont);
			
			attackerDamage.setTypeface(appWrapper.pixelFont);
			defenderDamage.setTypeface(appWrapper.pixelFont);
			attDamagePerc = 0;
			defDamagePerc = 0;
			for (Outcome outcome : battle.out)
			{
				if (outcome.unitId1Damage == 2)
				{
					if (outcome.unitId1 == attackerUnit.mUnitId)
					{
						attDamagePerc += outcome.probability;
					} else
					{
						defDamagePerc += outcome.probability;
					}
				}
			
				if (outcome.unitId2Damage == 2)
				{
					if (outcome.unitId2 == attackerUnit.mUnitId)
					{
						attDamagePerc += outcome.probability;
					} else
					{
						defDamagePerc += outcome.probability;
					}
				}
				
			}
			attDamage = Integer.toString(Math.round(attDamagePerc * 100.0f)) + "%";
			defDamage = Integer.toString(Math.round(defDamagePerc * 100.0f)) + "%";
			attackerDamage.setText(defDamage);
			defenderDamage.setText("");
			outcomeArea.addView(resultView);

			
		}
		
	}
	
	
	
		
}
