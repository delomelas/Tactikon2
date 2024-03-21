package uk.co.eidolon.tact2;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

public class NewGameView extends LinearLayout
{
	Context mContext;
	public NewGameView(final Context context)
	{
		super(context);
		
		mContext = context;

		LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        Button btn = new Button(context);
        btn.setId(1);
		btn.setText("button");
        l.addView(btn);
        this.addView(l);
        
	}
	
}
