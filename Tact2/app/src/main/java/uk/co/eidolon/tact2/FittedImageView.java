package uk.co.eidolon.tact2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FittedImageView extends ImageView
{
	FittedImageView(Context context)
	{
		super(context);
	}
	
	/**
     * @param context
     * @param attrs
     */
    public FittedImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public FittedImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
	
	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int width = MeasureSpec.getSize(widthMeasureSpec);
		setMeasuredDimension(width, width);
	}
}
