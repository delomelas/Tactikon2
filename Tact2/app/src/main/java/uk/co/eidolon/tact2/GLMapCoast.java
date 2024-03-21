package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import uk.co.eidolon.tact2.TextureManager.Texture;

import Tactikon.State.TactikonState;
import android.opengl.GLES20;
import android.util.Log;

public class GLMapCoast extends GLContext
{
	private final String VertexShader = 
			"uniform mat4 MVPMatrix;						\n" +
		    "attribute vec2 aTexCoord1;						\n" +
			"varying vec2 vTexCoord1;						\n" +
	        "attribute vec3 vPosition;  					\n" +
			"void main(){               					\n" +
	        "   gl_Position = MVPMatrix * vec4(vPosition.xyz, 1);	\n" +
	        "   vTexCoord1 = aTexCoord1;					\n" +
	        "}                         						\n";
	    
	private final String PixelShader = 
	        "precision mediump float;  						\n" +
	        "uniform sampler2D sTexture1;					\n" +
	        "varying vec2 vTexCoord1;						\n" +
	        "void main(){              						\n" +
	        "   gl_FragColor = texture2D(sTexture1, vTexCoord1);    \n" +
	        "}              								\n";
	
	public int maPositionHandle;
    public int muMatrix;
    
    int msTextureHandle1;
    int maTextureCoord1;
    
    Texture mHorizontal;
    Texture mVertical;
    Texture mCorners;
    int mTileTypeMid;
    int mTileTypeSurround;
    
    
    int horizontalStart, verticalStart, cornerStart;
    
    
    
    GLMapCoast(TactikonState state, Texture horizontal, Texture vertical, Texture corners, int mid, int surround)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
		
		mHorizontal = horizontal;
		mVertical = vertical;
		mCorners = corners;
		
		mTileTypeMid = mid;
		mTileTypeSurround = surround;
		
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");

        //maColourHandle = GLES20.glGetAttribLocation(mShaderProgram, "aColor");
        msTextureHandle1 = GLES20.glGetUniformLocation(mShaderProgram, "sTexture1");
        maTextureCoord1 = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord1");
                
		// build the verticies for the map
		renderList = new float[36 * 2 * state.mapSize * state.mapSize];
		
		horizontalStart = 0;
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (state.map[x][y] == mTileTypeSurround)
				{
					if (y > 0 && state.map[x][y - 1] == mTileTypeMid)
					{

					renderList[renderIdx++] = x; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
										
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
					
					renderNum ++;
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y +0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
					
					renderList[renderIdx++] = x +1; renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
					
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y)/4.0f; //tx, ty
					
					renderNum ++;
					}
					
					if (y < state.mapSize - 1 && state.map[x][y + 1] == mTileTypeMid)
					{
					renderList[renderIdx++] = x; renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y + 0.5f)/4.0f; //tx, ty
										
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y + 0.5f)/4.0f; //tx, ty
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y + 1.0f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1.0f)/4.0f; //tx, ty
					
					renderNum ++;
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y +1.0f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1.0f)/4.0f; //tx, ty
					
					renderList[renderIdx++] = x +1; renderList[renderIdx++] = y + 1.0f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y+1.0f)/4.0f; //tx, ty
					
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y + 0.5f)/4.0f; //tx, ty
					
					renderNum ++;
					}
				}
			}
		}
		
		verticalStart = renderNum;
		
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize ; ++y)
			{
				if (state.map[x][y]== mTileTypeSurround)
				{
					if (x > 0 && state.map[x - 1][y] == mTileTypeMid)
					{
						renderList[renderIdx++] = x; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
						
						renderList[renderIdx++] = x; renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderNum ++;
						
						renderList[renderIdx++] = x; renderList[renderIdx++] = y +1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderList[renderIdx++] = x +0.5f; renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = (y)/4.0f; //tx, ty
						
						renderNum ++;
					}
					
					if (x < state.mapSize - 1 && state.map[x + 1][y] == mTileTypeMid)
					{
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x + 0.5f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 1f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
						
						renderList[renderIdx++] = (x + 0.5f); renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x + 0.5f)/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderNum ++;
						
						renderList[renderIdx++] = (x + 0.5f); renderList[renderIdx++] = y +1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x + 0.5f)/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderList[renderIdx++] = x +1f; renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
						
						renderList[renderIdx++] = x + 1f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = (y)/4.0f; //tx, ty
						
						renderNum ++;
					}
				}
			}
		}
		
		cornerStart = renderNum;
		
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize ; ++y)
			{
				if (state.map[x][y] == mTileTypeSurround)
				{
					if (x > 0 && y > 0 && state.map[x-1][y-1] == mTileTypeMid)
					{
						renderList[renderIdx++] = x; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
						
						renderList[renderIdx++] = (x); renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x)/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
						
						renderNum ++;
					}
					
					if (x < state.mapSize - 1 && y > 0 && state.map[x+1][y-1] == mTileTypeMid)
					{
						renderList[renderIdx++] = (x+1f); renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
						
						renderList[renderIdx++] = (x+1f); renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
						
						renderNum ++;
					}
					
					if (x < state.mapSize - 1 && y < state.mapSize - 1 && state.map[x+1][y+1] == mTileTypeMid)
					{
						renderList[renderIdx++] = (x+1f); renderList[renderIdx++] = y+1f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = (y+1f)/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y+1f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = (y+1f)/4.0f; //tx, ty
						
						renderList[renderIdx++] = (x+1f); renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+1f)/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
						
						renderNum ++;
					}
					
					if (x > 0 && y < state.mapSize - 1 && state.map[x-1][y+1] == mTileTypeMid)
					{
						renderList[renderIdx++] = (x); renderList[renderIdx++] = y+1f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x)/4.0f; renderList[renderIdx++] = (y+1f)/4.0f; //tx, ty
											
						renderList[renderIdx++] = x + 0.5f; renderList[renderIdx++] = y+1f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x+0.5f)/4.0f; renderList[renderIdx++] = (y+1f)/4.0f; //tx, ty
						
						renderList[renderIdx++] = (x); renderList[renderIdx++] = y + 0.5f; renderList[renderIdx++] = 0; // x, y, z
						renderList[renderIdx++] = (x)/4.0f; renderList[renderIdx++] = (y+0.5f)/4.0f; //tx, ty
						
						renderNum ++;
					}
				
				}
			}
		}
		
		renderList = resizeArray(renderIdx, renderList);
		
		byteBuf = ByteBuffer.allocateDirect(renderList.length * 4);
		
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		
		vertexBuffer.position(0);
		vertexBuffer.put(renderList, 0, renderIdx);
	}

	
	public void RenderPart(Texture tex, int renderStart, int renderLength)
	{
		if (renderLength == 0) return;
		GLES20.glEnable(GLES20.GL_BLEND);
	    checkGlError("glEnable");
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    checkGlError("glBlendFunc");
	    
	    // Add program to OpenGL environment
	    GLES20.glUseProgram(mShaderProgram);
        checkGlError("glUseProgram");
        
        // Prepare the triangle data
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray");
        GLES20.glEnableVertexAttribArray(maTextureCoord1);
        checkGlError("glEnableVertexAttribArray");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.GLId);
        checkGlError("glBindTexture");
        GLES20.glUniform1i(msTextureHandle1, 0);
        checkGlError("glUniform1i");

        vertexBuffer.position(0 + renderStart * 15);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        vertexBuffer.position(3 + renderStart * 15);
        GLES20.glVertexAttribPointer(maTextureCoord1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        
        GLES20.glUniformMatrix4fv(muMatrix, 1, false, mMVPMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        
        // Draw the triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, renderLength * 3);
        checkGlError("glDrawArrays");
        
        GLES20.glDisable(GLES20.GL_BLEND);
        checkGlError("glDisable");
	}
	
	public void Render()
	{
	    RenderPart(mHorizontal, 0, verticalStart);
	    RenderPart(mVertical, verticalStart, cornerStart - verticalStart);
	    RenderPart(mCorners, cornerStart, renderNum - cornerStart);
	}
}
