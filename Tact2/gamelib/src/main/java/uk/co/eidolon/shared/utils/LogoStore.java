package uk.co.eidolon.shared.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LogoStore
{
	Context mContext;
	
	public LogoStore(Context context)
	{
		mContext = context;
	}
	
	public String[] GetLogoList()
	{
		try
		{
			return mContext.getAssets().list("logo");
		} catch (IOException e)
		{
			return null;
		}
	}
	
	Map<String, Drawable> drawableCache = new TreeMap<String, Drawable>();
	
	public Drawable GetLogo(String name)
	{
		if (drawableCache.containsKey(name)) 
		{
			Drawable baseDrawable = drawableCache.get(name);
			return baseDrawable;
		}
		
		InputStream ims = null;
		try
		{
			ims = mContext.getAssets().open("logo/" + name);
		} catch (IOException e)
		{
			return null;
		}
		// load image as Drawable
		Drawable d = Drawable.createFromStream(ims, null);
		
		drawableCache.put(name, d);
		
		return d;
	}
}
