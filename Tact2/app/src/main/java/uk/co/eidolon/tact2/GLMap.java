package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import uk.co.eidolon.tact2.TextureManager.Texture;

import Tactikon.State.TactikonState;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class GLMap extends GLContext
{
	private final String VertexShader = 
			"uniform mat4 MVPMatrix;						\n" +
		    "attribute vec2 aTexCoord1;						\n" +
			"varying vec2 vTexCoord1;						\n" +
	        "attribute vec3 vPosition;  					\n" +
			"attribute float aHeight;						\n" +
	        "varying float vHeight;							\n" +
			"void main(){               					\n" +
	        "   gl_Position = MVPMatrix * vec4(vPosition.xyz, 1);	\n" +
	        "   vTexCoord1 = aTexCoord1;					\n" +
	        "   vHeight = aHeight;							\n" +
	        "}                         						\n";
	    
	private final String PixelShader = 
	        "precision lowp float;  						\n" +
	        "uniform sampler2D sTexture1;					\n" +
	        "uniform sampler2D sTexture2;					\n" +
	        "varying vec2 vTexCoord1;						\n" +
	        "varying float vHeight;							\n" +
	        "void main(){              						\n" +
	        "   vec4 color1 = texture2D(sTexture1, vTexCoord1) * vHeight;    \n" +
	        "   vec4 color2 = texture2D(sTexture2, vTexCoord1) * (1.0 - vHeight);    \n" +
	        "   gl_FragColor = color1 + color2;    \n" +
	        "}              								\n";
	
	public int maPositionHandle;
    public int muMatrix;
    public int mfHeightHandle;
    
    int msTextureHandle1;
    int msTextureHandle2;
    int maTextureCoord1;
    
    
    Texture mTexture1;
    Texture mTexture2;
    int mTileType;
    
    
    
	GLMap(TactikonState state, Texture texture1, Texture texture2, int tileType, float rangeMin, float rangeMax, RangeFunctor functor)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
		
		mTexture1 = texture1;
		mTexture2 = texture2;
		mTileType = tileType;
		
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");
        //maColourHandle = GLES20.glGetAttribLocation(mShaderProgram, "aColor");
        msTextureHandle1 = GLES20.glGetUniformLocation(mShaderProgram, "sTexture1");
        msTextureHandle2 = GLES20.glGetUniformLocation(mShaderProgram, "sTexture2");
        maTextureCoord1 = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord1");
        mfHeightHandle = GLES20.glGetAttribLocation(mShaderProgram, "aHeight");
                
		// build the verticies for the map
		renderList = new float[36 * state.mapSize * state.mapSize];
		
		
		
	
		for (int x = 0; x < state.mapSize; ++x)
		{
			for (int y = 0; y < state.mapSize; ++y)
			{
				if (state.map[x][y] == mTileType)
				{
					renderList[renderIdx++] = x; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
					float val = (state.mapHeight[x][y] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
										
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = y/4.0f; //tx, ty
					val = (state.mapHeight[x+1][y] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
					val = (state.mapHeight[x][y+1] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
					
					renderNum ++;
					
					renderList[renderIdx++] = x; renderList[renderIdx++] = y +1; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = x/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
					val = (state.mapHeight[x][y+1] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
					
					renderList[renderIdx++] = x +1; renderList[renderIdx++] = y + 1; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y+1)/4.0f; //tx, ty
					val = (state.mapHeight[x+1][y+1] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
					
					renderList[renderIdx++] = x + 1; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
					renderList[renderIdx++] = (x+1)/4.0f; renderList[renderIdx++] = (y)/4.0f; //tx, ty
					val = (state.mapHeight[x+1][y] - rangeMin) / (rangeMax - rangeMin);
					renderList[renderIdx++] = functor.offsetRange(val);
					
					renderNum ++;
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
	
	
	public void Render()
	{
	    if (renderIdx == 0) return;
	    // Add program to OpenGL environment
	    GLES20.glUseProgram(mShaderProgram);
        checkGlError("glUseProgram");
        
        // Prepare the triangle data
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray");
        GLES20.glEnableVertexAttribArray(maTextureCoord1);
        checkGlError("glEnableVertexAttribArray");
        GLES20.glEnableVertexAttribArray(mfHeightHandle);
        checkGlError("glEnableVertexAttribArray");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture1.GLId);
        checkGlError("glBindTexture");
        GLES20.glUniform1i(msTextureHandle1, 0);
        checkGlError("glUniform1i");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture2.GLId);
        checkGlError("glBindTexture");
        GLES20.glUniform1i(msTextureHandle2, 1);
        checkGlError("glUniform1i");

        
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 24, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(maTextureCoord1, 2, GLES20.GL_FLOAT, false, 24, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        vertexBuffer.position(5);
        GLES20.glVertexAttribPointer(mfHeightHandle, 1, GLES20.GL_FLOAT, false, 24, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        GLES20.glUniformMatrix4fv(muMatrix, 1, false, mMVPMatrix, 0);
        checkGlError("glUniformMatrix4fv");
        
        // Draw the triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, renderNum * 3);
        checkGlError("glDrawArrays");
        
	}
}
