package uk.co.eidolon.tact2;

import java.util.ArrayList;

import uk.co.eidolon.tact2.TextureManager.UnitDefinition;
import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;
import Tactikon.State.UnitBoatTransport;
import Tactikon.State.UnitFighter;
import Tactikon.State.UnitTank;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class UnitDetailsView extends LinearLayout
{
	
	void SetLCDUnitImage(ImageView imageView, IUnit unit, TextureManager texMan)
	{	
		if (unit == null) return;

        UnitDefinition tex = texMan.GetUnitDefinition(unit.getClass().getSimpleName(), unit.mConfig);
        if (tex == null) tex = texMan.GetUnitDefinition("UnitTank", unit.mConfig);
        //Bitmap.Config config = new Bitmap.Config();
        
        int width = tex.mergedTexture.width;
        int height = tex.mergedTexture.height;
        
        Bitmap bmOverlay = Bitmap.createBitmap(width, height, tex.mergedTexture.bitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay); 
        canvas.drawBitmap(tex.mergedTexture.bitmap, 0, 0, null);
        
        bmOverlay = Bitmap.createScaledBitmap(bmOverlay, width * 3, height * 3, false);
        
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmOverlay);
        
        ColorMatrix bwMatrix = new ColorMatrix();
        bwMatrix.setSaturation(0);
        bwMatrix.setScale(0.3f, 0.4f, 0.2f, 0.8f);
        final ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(bwMatrix);
        imageView.setColorFilter(colorFilter);
        imageView.setImageDrawable(bitmapDrawable);
	}
	

	public UnitDetailsView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	    LayoutInflater  mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View view = mInflater.inflate(R.layout.unit_details, this, true);
	    
	    AppWrapper appWrapper = (AppWrapper)context.getApplicationContext();
	    TextView turnsText = (TextView)view.findViewById(R.id.text_turns);
	    TextView movesText = (TextView)view.findViewById(R.id.text_moves);
	    
	    TextView airDetailsText = (TextView)view.findViewById(R.id.text_vs_air_details);
	    TextView seaDetailsText = (TextView)view.findViewById(R.id.text_vs_sea_details);
	    TextView landDetailsText = (TextView)view.findViewById(R.id.text_vs_land_details);
	    
	    TextView airText = (TextView)view.findViewById(R.id.text_vs_air);
	    TextView seaText = (TextView)view.findViewById(R.id.text_vs_sea);
	    TextView landText = (TextView)view.findViewById(R.id.text_vs_land);
	    
	    turnsText.setTypeface(appWrapper.pixelFont);
	    movesText.setTypeface(appWrapper.pixelFont);
	    
	    airDetailsText.setTypeface(appWrapper.pixelFont);
	    landDetailsText.setTypeface(appWrapper.pixelFont);
	    seaDetailsText.setTypeface(appWrapper.pixelFont);
	    
	    airText.setTypeface(appWrapper.pixelFont);
	    landText.setTypeface(appWrapper.pixelFont);
	    seaText.setTypeface(appWrapper.pixelFont);
	    
	    ImageView unitImage = (ImageView)view.findViewById(R.id.image_unit);
	    
	    UnitTank landUnit = new UnitTank();
	    TactikonState state = new TactikonState();
	    state.mGameVersion = state.mVersion;
	    
	    String unitName = attrs.getAttributeValue("http://schemas.android.com/apk/res/uk.co.eidolon.tact2", "unitType");
	    ArrayList<IUnit> units = state.GetUnitTypes();
	    IUnit unit = null;
	    for (IUnit findUnit : units)
	    {
	    	if (findUnit.getClass().getSimpleName().compareTo(unitName) == 0) unit = findUnit;
	    }
	    
	    if (unit == null) return;
	    
	    int config = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/uk.co.eidolon.tact2", "config", 0);
	    unit.mConfig = config;
	    
	    UnitBoatTransport seaUnit = new UnitBoatTransport();
	    UnitFighter airUnit = new UnitFighter();
	    SetLCDUnitImage(unitImage, unit, appWrapper.GetTextureManager());
	    
	    airDetailsText.setText("ATT: " + unit.GetAttack(airUnit, state) + " DEF: " + unit.GetDefence(airUnit, state));
	    landDetailsText.setText("ATT: " + unit.GetAttack(landUnit, state) + " DEF: " + unit.GetDefence(landUnit, state));
	    seaDetailsText.setText("ATT: " + unit.GetAttack(seaUnit, state) + " DEF: " + unit.GetDefence(seaUnit, state));
	    
	    turnsText.setText("TURNS: " + unit.GetProductionTime(null, state));
	    movesText.setText("MOVES: " + unit.GetMovementDistance());
		
	}
	

}
