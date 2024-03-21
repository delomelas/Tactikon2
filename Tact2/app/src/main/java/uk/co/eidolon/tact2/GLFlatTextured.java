package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import uk.co.eidolon.tact2.TextureManager.Texture;

import android.opengl.GLES20;

public class GLFlatTextured extends GLContext
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
    
    Texture mTex;
    
    float colR = 1, colG = 1, colB = 1, colA = 1;
    
    GLFlatTextured(Texture tex, float colR, float colG, float colB, float colA, boolean fixScale)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
    	
    	mTex = tex;
    	
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandleFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");
        maColourHandleFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "aColor");
        msTextureHandleFlatTextured = GLES20.glGetUniformLocation(mShaderProgram, "sTexture");
        maTextureCoordFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "aTexCoord");
        
        renderList = new float[36];
        
		byteBuf = ByteBuffer.allocateDirect(renderList.length * 4);
		
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		
		float pixWidth = 1.0f;
		float pixHeight = 1.0f;
		if (fixScale == true)
		{
			pixWidth = ((1.0f / 32.0f) * mTex.width);
			pixHeight = ((1.0f / 32.0f) * mTex.height);
		}
		
		renderList[renderIdx++] = -pixWidth/2; renderList[renderIdx++] = -0.5f; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 1; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = pixWidth/2f; renderList[renderIdx++] = -0.5f; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1; renderList[renderIdx++] = 1; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		renderList[renderIdx++] = -pixWidth/2f; renderList[renderIdx++] = -0.5f + pixHeight; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		
		//renderNum ++;
		
		/*
		renderList[renderIdx++] = pixWidth/2f; renderList[renderIdx++] = -0.5f; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1f; renderList[renderIdx++] = 1f; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		*/
		renderList[renderIdx++] = pixWidth/2f; renderList[renderIdx++] = -0.5f + pixHeight; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 1f; renderList[renderIdx++] = 0; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		/*
		renderList[renderIdx++] = -pixWidth/2f; renderList[renderIdx++] = -0.5f + pixHeight; renderList[renderIdx++] = 0; // x, y, z
		renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; //tx, ty
		renderList[renderIdx++] = colR; renderList[renderIdx++] = colG; renderList[renderIdx++] = colB; renderList[renderIdx++] = colA; //r, g, b, a
		*/
		renderNum ++;
		
		vertexBuffer.position(0);
		vertexBuffer.put(renderList, 0, renderIdx);
	}
	
	
	public void Render()
	{
	    GLES20.glEnable(GLES20.GL_BLEND);
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    
	    // Add program to OpenGL environment
	    GLES20.glUseProgram(mShaderProgram);
        
     // Set the sampler texture unit to 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTex.GLId);
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, renderNum * 4);
        
        GLES20.glDisable(GLES20.GL_BLEND);
	}


}
