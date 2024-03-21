package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import uk.co.eidolon.tact2.TextureManager.Texture;

import Tactikon.State.TactikonState;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

public class GLFogEdges extends GLContext
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
	        "   vec4 color1 = texture2D(sTexture1, vTexCoord1);    \n" +
	        "   gl_FragColor = color1;					    \n" +
	        "}              								\n";
	
	public int maPositionHandle;
    public int muMatrix;
    
    int msTextureHandle1;
    int maTextureCoord1;
    
    byte[] pixelComponents;
    ByteBuffer fogTexture;
    int texId;
    int texWidth, texHeight;
    
    GLFogEdges(TactikonState state, long playerId)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
		
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");

        msTextureHandle1 = GLES20.glGetUniformLocation(mShaderProgram, "sTexture1");
        maTextureCoord1 = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord1");
                
		// build the verticies for the map
		renderList = new float[30];
		
		byteBuf = ByteBuffer.allocateDirect(renderList.length * 4);
		
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		
		texWidth = state.mapSize;
		texHeight = state.mapSize;
		
		pixelComponents = new byte[texWidth * texHeight * 4];
		fogTexture = ByteBuffer.wrap(pixelComponents);
		
		final int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		texId = textures[0];
		
		// Bind the texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		// ...and bind it to our array
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
		
		// Create Nearest Filtered Texture
		
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		
		UpdateFog(state, playerId);
	}
	
	void UpdateFog(TactikonState state, long playerId)
	{
		int mapSize = state.mapSize;
		byte[][] fogMap = state.GetResolvedFogMap((int)playerId);
		
		float height = state.mapSize ;
		float width = state.mapSize;
		
		int index = 3;
		for (int y = 0; y < mapSize; ++y)
		{
			for (int x = 0; x < mapSize; ++x)
			{
				if (fogMap[x][y] == 2)
				{
					pixelComponents[index] = 0;
				} else if (fogMap[x][y] == 1)
				{
					pixelComponents[index] = 127;
				} else
				{
					pixelComponents[index] = (byte)(128 + 127);
				}
				index = index + 4;
			}
		}
		
		// Bind the new fog texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
       			texWidth, texHeight, 0, GLES20.GL_RGBA, 
       			GLES20.GL_UNSIGNED_BYTE,fogTexture);
		
		renderIdx = 0;
		renderNum = 0;
		
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1; renderList[renderIdx++] = 0; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1; renderList[renderIdx++] = 1; //tx, ty
		
		renderNum ++;
		
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0 ; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0f; renderList[renderIdx++] = 0f; //tx, ty
		
		renderList[renderIdx++] = 0; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0f; renderList[renderIdx++] = 1; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1; renderList[renderIdx++] = 1; //tx, ty
		
		renderNum ++;
					
		
		vertexBuffer.position(0);
		vertexBuffer.put(renderList, 0, renderIdx);
	}
	
	public void Render()
	{
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        checkGlError("glBindTexture");
        GLES20.glUniform1i(msTextureHandle1, 0);
        checkGlError("glUniform1i");

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(maTextureCoord1, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer);
        checkGlError("glVertexAttribPointer");
        
        
        GLES20.glUniformMatrix4fv(muMatrix, 1, false, mMVPMatrix, 0);
        checkGlError("glUniformMatrix4fv");
        
        // Draw the triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, renderNum * 3);
        checkGlError("glDrawArrays");
        
        GLES20.glDisable(GLES20.GL_BLEND);
        checkGlError("glDisable");
	}

	
	
}
