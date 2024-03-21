package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import uk.co.eidolon.tact2.TextureManager.Texture;

import Tactikon.State.TactikonState;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

public class GLClouds extends GLContext
{
	private final String VertexShader = 
	        "precision mediump float;  						\n" +
			"uniform mat4 MVPMatrix;						\n" +
		    "attribute vec2 aTexCoord1;						\n" +
			"varying vec2 vTexCoord1;						\n" +
	        "attribute vec3 vPosition;  					\n" +
			"void main(){               					\n" +
	        "   gl_Position = MVPMatrix * vec4(vPosition.xyz, 1);	\n" +
	        "   vTexCoord1 = aTexCoord1;					\n" +
	        "}                         						\n";
	    
	private final String PixelShader = 
	        "precision lowp float;  						\n" +
	        "uniform sampler2D sTexture1;					\n" +
	        "varying vec2 vTexCoord1;						\n" +
	        "void main(){              						\n" +
	        "   gl_FragColor = texture2D(sTexture1, vTexCoord1);    \n" +
	        "}              								\n";
	
	public int maPositionHandle;
    public int muMatrix;
    
    int msTextureHandle1;
    int maTextureCoord1;
    
    int size;
    
    Texture cloudTexture;

    
    GLClouds(TactikonState state, TextureManager texMgr)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
		
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");

        msTextureHandle1 = GLES20.glGetUniformLocation(mShaderProgram, "sTexture1");
        maTextureCoord1 = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord1");
                
        size = state.mapSize;
		// build the verticies for the map
		renderList = new float[30];
		
		byteBuf = ByteBuffer.allocateDirect(renderList.length * 4);
		
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		
		cloudTexture = texMgr.GetTexture("clouds");
		
		// Bind the texture
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
				// ...and bind it to our array
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cloudTexture.GLId);
				
				// Create Nearest Filtered Texture
				
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				// Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
				
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
		
		UpdateClouds(0);
	}
	
	void UpdateClouds(float time)
	{
	
		float width = size;
		float height = size;
		renderIdx = 0;
		renderNum = 0;
		float time2 = time / 6;
		time2 = time2 % 1;
		time= time % 1;
		renderList[renderIdx++] = 0 ; renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0 - time; renderList[renderIdx++] = 0 + time2; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0.5f - time; renderList[renderIdx++] = 0 + time2; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0.5f - time; renderList[renderIdx++] = 1 + time2; //tx, ty
		
		renderNum ++;
		
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0 ; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0f - time; renderList[renderIdx++] = 0f + time2; //tx, ty
		
		renderList[renderIdx++] = 0; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0f - time; renderList[renderIdx++] = 1 + time2; //tx, ty
		
		renderList[renderIdx++] = width; renderList[renderIdx++] = height; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0.5f - time; renderList[renderIdx++] = 1 + time2; //tx, ty
		
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cloudTexture.GLId);
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
