package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.co.eidolon.tact2.TextureManager.Texture;

import android.opengl.GLES20;
import android.util.Log;

public class GLTexturedSpriteBatch extends GLContext
{

	private final String VertexShader = 
			"uniform mat4 MVPMatrix;						\n" +
		    "attribute vec4 aColor;							\n" +
		    "attribute vec2 aTexCoord;						\n" +
			"varying vec2 vTexCoord;						\n" +
			"varying vec4 vColor;							\n" +
	        "attribute vec3 vPosition;  					\n" +
	        "void main(){              						\n" +
	        "   gl_Position = MVPMatrix * vec4(vPosition.xyz, 1);	\n" +
	        "   vTexCoord = aTexCoord;						\n" +
	        "   vColor = aColor;							\n" +
	        "}                         						\n";
	    
	private final String PixelShader = 
	        "precision mediump float;  						\n" +
	        "uniform sampler2D sTexture;					\n" +
	        "varying vec2 vTexCoord;						\n" +
	        "varying vec4 vColor;     						\n" +
	        "void main(){              						\n" +
	        "   gl_FragColor = texture2D(sTexture, vTexCoord) * vColor;    \n" +
	        "}              								\n";
	
	int maPositionHandleFlatTextured;
    int muMatrix;
    int maColourHandleFlatTextured;
    int msTextureHandleFlatTextured;
    int maTextureCoordFlatTextured;
    
    int mCurrentPipelineTexture = -1;
    int mCurrentRenderNum = -1;
    
    int maxBatchSize = 600;
    
    double mTime;
    
    
    
    class TxMap
    {
    	Map<Integer, ArrayList<GLBatchItem>> txItems = new TreeMap<Integer, ArrayList<GLBatchItem>>();
    }
    
    Map<Integer, TxMap> pxMap = new TreeMap<Integer, TxMap>();
    
    //Set<Integer> passList = new TreeSet<Integer>();
   
    GLTexturedSpriteBatch()
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
    	
    	SetShaders(PixelShader, VertexShader);
		
		maPositionHandleFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");
        maColourHandleFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "aColor");
        msTextureHandleFlatTextured = GLES20.glGetUniformLocation(mShaderProgram, "sTexture");
        maTextureCoordFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord");
        
        renderList = new float[9 * 3 * maxBatchSize];
        
		byteBuf = ByteBuffer.allocateDirect(renderList.length * 4);
		
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
	}
    
    public void Start()
    {
    	for (Entry<Integer, TxMap> entry : pxMap.entrySet())
    	{
    		entry.getValue().txItems.clear();
    	}
    	//pxMap.clear();
    	//txItems.clear();
    	
    }
    
    public void AddObject(Texture texture, float x, float y, float colA, float colR, float colG, float colB, float scale, boolean fixScale, int renderPass, boolean bShowAnimation)
    {
    	GLBatchItem item = new GLBatchItem();
    	item.xPos = x;
    	item.yPos = y;
    	item.scale = scale;
    	item.a = colA;
    	item.b = colB;
    	item.g = colG;
    	item.r = colR;
    	item.texture = texture;
    	item.fixScale = fixScale;
    	item.renderPass = renderPass;
    	item.bShowAnimation = bShowAnimation;
    	
    	TxMap txItems = pxMap.get(renderPass);
    	if (txItems == null)
    	{
    		txItems = new TxMap();
    		pxMap.put(renderPass,  txItems);
    	}
	
    	
    	ArrayList<GLBatchItem> items = txItems.txItems.get(item.texture.GLId);
    	if (items == null)
    	{
    		items = new ArrayList<GLBatchItem>();
    		txItems.txItems.put(item.texture.GLId, items);
    	}
    	items.add(item);

    }
    
   // public static int gFrameCount = 0;
    
    void AddToRenderList(GLBatchItem item)
    {
    	float x = item.xPos;
    	float y = item.yPos;
    	float colR = item.r;
    	float colB = item.b;
    	float colG = item.g;
    	float colA = item.a;
    	float scale = item.scale;
    	boolean fixScale = item.fixScale;
    	Texture tex = item.texture;
    	
    	float x1, y1, x2, y2;
		if (fixScale == true)
		{
    		float pixWidth = ((1.0f / 32.0f) * tex.width) * scale;
			float pixHeight = ((1.0f / 32.0f) * tex.height) * scale;
			x1 = x - pixWidth / 2;
			x2 = x + pixWidth / 2;
			y1 = y - 0.5f;
			y2 = y -0.5f + pixHeight;
		} else
		{
			float pixWidth = 1.0f * scale;
			float pixHeight = 1.0f * scale;
			
			x1 = x - pixWidth / 2;
			x2 = x + pixWidth / 2;
			y1 = y - pixHeight / 2;
			y2 = y + pixHeight / 2;
		}
		
		int frame = (int)Math.round(mTime / ((double)tex.frameRate / 1000)) % tex.frames;
		//int frame = (gFrameCount / tex.frameRate) % tex.frames;
		
		if (item.bShowAnimation == false) frame = 0;
		
		float frameWidth = 1.0f / (float)tex.frames;
		float left = (frame * frameWidth);
		float right = (frame + 1) * frameWidth;
		float top = 1;
		float bottom = 0;
		
		renderList[renderIdx++] = x1; renderList[renderIdx++] = y1; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = left; renderList[renderIdx++] = top; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = x2; renderList[renderIdx++] = y1; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = right; renderList[renderIdx++] = top; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = x1; renderList[renderIdx++] = y2; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = left; renderList[renderIdx++] = bottom; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderNum ++;
		
	
		renderList[renderIdx++] = x2; renderList[renderIdx++] = y1; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = right; renderList[renderIdx++] = top; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = x2; renderList[renderIdx++] = y2; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = right; renderList[renderIdx++] = bottom; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = x1; renderList[renderIdx++] = y2; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = left; renderList[renderIdx++] = bottom; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderNum ++;
		
		if (renderNum >= maxBatchSize)
		{
			DoRender(tex.GLId);
		}
		
    }
    
    void DoRender(int texId)
    {
    	vertexBuffer.position(0);
		vertexBuffer.put(renderList, 0, renderIdx);
		
	    GLES20.glEnable(GLES20.GL_BLEND);
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    //GLES20.glEnable( GLES20.GL_DEPTH_TEST );
	    
	    // Add program to OpenGL environment
	    GLES20.glUseProgram(mShaderProgram);
        
     // Set the sampler texture unit to 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glUniform1i(msTextureHandleFlatTextured, 0);
        
        // Prepare the triangle data
        GLES20.glEnableVertexAttribArray(maPositionHandleFlatTextured);
        GLES20.glEnableVertexAttribArray(maColourHandleFlatTextured);
        
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandleFlatTextured, 3, GLES20.GL_FLOAT, false, 36, vertexBuffer);
        
        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(maTextureCoordFlatTextured, 2, GLES20.GL_FLOAT, false, 36, vertexBuffer);
        
        vertexBuffer.position(5);
        GLES20.glVertexAttribPointer(maColourHandleFlatTextured, 4, GLES20.GL_FLOAT, false, 36, vertexBuffer);
        
        GLES20.glUniformMatrix4fv(muMatrix, 1, false, mMVPMatrix, 0);

        // Draw the triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, renderNum * 3);
        
        GLES20.glDisable(GLES20.GL_BLEND);
        //GLES20.glDisable( GLES20.GL_DEPTH_TEST );
        
        renderNum = 0;
        renderIdx = 0;
        count ++;
    }
    
    int count = 0;
	
	public void Render(float frameTime)
	{
		mTime = mTime + frameTime;
		count = 0;
		
		for (Entry<Integer, TxMap> entry : pxMap.entrySet())
		{
			TxMap txItems = entry.getValue();
			for (Entry<Integer, ArrayList<GLBatchItem>> entry2 : txItems.txItems.entrySet())
			{
				int texId = entry2.getKey();
				renderNum = 0;
			    renderIdx = 0;
				for (GLBatchItem item : entry2.getValue())
				{
					AddToRenderList(item);
				}
				DoRender(texId);
				
			}
		}
		
	}


}
