package uk.co.eidolon.tact2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.microedition.khronos.opengles.GL10;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLES20;

public class TextureManager
{
	class Texture
	{
		String name;
		ByteBuffer texData;
		int width;
		int height;
		int GLId;
		Bitmap bitmap;
		boolean lowQuality = false;
		boolean filtered = false;
		int frames = 1;
		int frameRate = 20;
	}
	
	ArrayList<Texture> textures = new ArrayList<Texture>();
	
	Context mContext;
	
	TextureManager(Context context)
	{
		mContext = context;
		PreloadTextures(mContext, "graphics.xml");
	}
	
	class UnitDefinition
	{
		Texture baseTexture;
		Texture colourTexture;
		Texture mergedTexture;
		Texture shadowTexture = null;
	}
	
	
	TreeMap<String, UnitDefinition> unitTextures = new TreeMap<String, UnitDefinition>();
	
	TreeMap<String, Texture> miscTextures = new TreeMap<String, Texture>();
	
	UnitDefinition GetUnitDefinition(String unitName, int config)
	{
		String configName = unitName + "_" + config;
		return unitTextures.get(configName);
	}
	
	void PreloadTextures(Context context, String textureSetXml)
	{
		
		// load the xml file
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try
		{
			//Using factory get an instance of document builder
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final InputStream levelXml = context.getAssets().open(textureSetXml);
			final Document dom = db.parse(levelXml);
			
			final Element root = dom.getDocumentElement();
			
			final NodeList nodeList = root.getElementsByTagName("Unit");
			for (int i = 0; i < nodeList.getLength(); ++i)
	 		{
	 			final Element n = (Element)nodeList.item(i);
	 			
	 			String name = n.getAttribute("name");
	 			String baseTexture = n.getAttribute("baseTexture");
	 			String colourTexture = n.getAttribute("colourTexture");
	 			
	 			int frames = 1;
	 			if (n.hasAttribute("frames"))
	 			{
	 				frames = Integer.valueOf(n.getAttribute("frames"));
	 			}
	 			
	 			int config = 0;
	 			if (n.hasAttribute("config"))
	 			{
	 				config = Integer.valueOf(n.getAttribute("config"));
	 			}
	 			
	 			UnitDefinition def = new UnitDefinition();
	 			
	 			def.baseTexture = LoadTexture(baseTexture, false, false);
	 			def.colourTexture = LoadTexture(colourTexture, false, false);
	 			def.baseTexture.lowQuality = false;
	 			def.baseTexture.filtered = false;
	 			
	 			if (n.hasAttribute("shadowTexture"))
	 			{
	 				String shadowTexture = n.getAttribute("shadowTexture");
	 				def.shadowTexture = LoadTexture(shadowTexture, false, false);
	 				def.shadowTexture.lowQuality = false;
		 			def.shadowTexture.filtered = false;
		 			def.shadowTexture.frames = frames;
	 			}
	 			
	 			if (n.hasAttribute("frameRate")) // time in 30ths of a second a frame stays for
	 			{
	 				def.baseTexture.frameRate = Integer.valueOf(n.getAttribute("frameRate"));
	 				def.colourTexture.frameRate = Integer.valueOf(n.getAttribute("frameRate"));
	 				if (def.shadowTexture != null) def.shadowTexture.frameRate = Integer.valueOf(n.getAttribute("frameRate"));
	 			}
	 			
	 			def.mergedTexture = MergedTexture(def.baseTexture, def.colourTexture, frames);
	 			
	 			def.baseTexture.frames = frames;
	 			def.colourTexture.frames = frames;
	 			
	 			name = name + "_" + config;
	 			unitTextures.put(name, def);
	 		}
			
			final NodeList miscList = root.getElementsByTagName("Misc");
			for (int i = 0; i < miscList.getLength(); ++i)
			{
				final Element n = (Element)miscList.item(i);
				String name = n.getAttribute("name");
				String texture = n.getAttribute("texture");
				
				String filtered = n.getAttribute("filtered");
				String quality = n.getAttribute("quality");

				boolean bLowQuality = false;
				boolean bFiltered = false;
				if (quality.compareTo("low") == 0) bLowQuality = true;
				if (filtered.compareTo("true") == 0) bFiltered = true;
				
				Texture newTexture = LoadTexture(texture, bLowQuality, bFiltered);
				
				
				if (n.hasAttribute("frameRate")) // time in 30ths of a second a frame stays for
	 			{
					newTexture.frameRate = Integer.valueOf(n.getAttribute("frameRate"));
	 			}
				if (n.hasAttribute("frames"))
	 			{
	 				newTexture.frames = Integer.valueOf(n.getAttribute("frames"));
	 			}
				
				miscTextures.put(name,  newTexture);
			}
			
		} catch (SAXException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	Texture MergedTexture(Texture base, Texture colour, int frames)
	{
		Texture merged = new Texture();
		merged.width = base.width / frames;
		merged.height = base.height;
		merged.lowQuality = false;
		merged.name= base.name;
		merged.filtered = base.filtered;
		
		merged.bitmap = Bitmap.createBitmap(merged.width, merged.height, base.bitmap.getConfig());
		Canvas canvas = new Canvas(merged.bitmap);
		
		canvas.drawBitmap(base.bitmap, 0, 0, null);
		canvas.drawBitmap(colour.bitmap,  0,  0, null);
		
		return merged;
	}
	
	Texture LoadTexture(String texture, boolean lowQuality, boolean filtered)
	{
		Texture tex = new Texture();
		// load the png and fill the Texture structure
		
		// get input stream
    	InputStream ims = null;
		try
		{
			ims = mContext.getAssets().open(texture);
		} catch (IOException e)
		{
			return null;
		}
		
    	Bitmap bitmap = BitmapFactory.decodeStream(ims);
    	
		final int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
    	final byte[] pixelComponents = new byte[pixels.length*4]; 
    	int byteIndex = 0;
    	bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight()); 
    	for (int i = 0; i < pixels.length; i++)
    	{ 
        	 final int p = pixels[i];
             // Convert to byte representation RGBA required by gl.glTexImage2D. 
             // We don't use intbuffer, because then we 
             // would be relying on the intbuffer wrapping to write the ints in 
             // big-endian format, which means it would work for the wrong 
             // reasons, and it might brake on some hardware. 
        	 pixelComponents[byteIndex++] = (byte) ((p >> 16) & 0xFF); // red 
        	 pixelComponents[byteIndex++] = (byte) ((p >> 8) & 0xFF); // green 
        	 pixelComponents[byteIndex++] = (byte) ((p) & 0xFF); // blue 
        	 pixelComponents[byteIndex++] = (byte) (p >> 24);  // alpha 
                 
        } 
        tex.texData = ByteBuffer.wrap(pixelComponents); 
        tex.width = bitmap.getWidth();
        tex.height = bitmap.getHeight();
        
        tex.bitmap = bitmap;
        tex.filtered = filtered;
        tex.lowQuality = lowQuality;
        //bitmap.recycle();
		
		return tex;
	}
	
	boolean IsPowerOfTwo(int x)
	{
	    return (x & (x - 1)) == 0;
	}
	
	void BindTexture(Texture tex)
	{
		final int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		tex.GLId = textures[0];
		
		// Bind the texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		// ...and bind it to our array
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.GLId);
		
		// Create Nearest Filtered Texture
		
		//gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
	    //gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		//gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                ////GL10.GL_REPLACE 
                //GL10.GL_MODULATE 
                //);
		
		if (tex.filtered == false)
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		} else
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		}
		
		
		// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		
		if (!IsPowerOfTwo(tex.width) || !IsPowerOfTwo(tex.height)) 
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		} else
		{
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
		}
		
		if (tex.lowQuality == false)
		{
		
		}
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
				tex.width, tex.height, 0, GLES20.GL_RGBA, 
				GLES20.GL_UNSIGNED_BYTE, tex.texData);
		
	}
	
	void BindTextures()
	{
		// TODO: needs to walk all the textures and fill the GLId for each
		for (Entry<String, UnitDefinition> entry : unitTextures.entrySet())
		{
			UnitDefinition unitDef = entry.getValue();
			BindTexture(unitDef.baseTexture);
			BindTexture(unitDef.colourTexture);
			if (unitDef.shadowTexture != null) BindTexture(unitDef.shadowTexture);
		}
		
		for (Entry<String, Texture> entry : miscTextures.entrySet())
		{
			BindTexture(entry.getValue());
		}
	}
	
	public Texture GetTexture(final String name)
	{
		if (miscTextures.containsKey(name) == true)
		{
			return miscTextures.get(name);
		}
		return null;
	}
}
