package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import Tactikon.State.TactikonState;
import android.opengl.GLES20;

public class GLMapLines extends GLContext
{
	private final String VertexShader = 
			"uniform mat4 MVPMatrix;						\n" +
		    "attribute vec3 vPosition;  					\n" +
			"void main(){               					\n" +
	        "   gl_Position = MVPMatrix * vec4(vPosition.xyz, 1);	\n" +
	        "}                         						\n";
	    
	private final String PixelShader = 
	        "precision mediump float;  						\n" +
	        "uniform vec4 uColor;							\n" +
	        "void main(){              						\n" +
	        "   gl_FragColor = uColor;   					\n" +
	        "}              								\n";
	
	public int maPositionHandleFlatTextured;
    public int muMatrix;
    public int muColourHandleFlatTextured;
    
    float[] color = new float[4];
    
	GLMapLines(TactikonState state)
	{
		// set up the render buffers according to the state
		// load required textures
		// initialise shaders
		
		color[0] = 0;
		color[1] = 0;
		color[2] = 0;
		color[3] = 0.5f;
		
		SetShaders(PixelShader, VertexShader);
		
		maPositionHandleFlatTextured = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        muMatrix = GLES20.glGetUniformLocation(mShaderProgram, "MVPMatrix");
        muColourHandleFlatTextured = GLES20.glGetUniformLocation(mShaderProgram, "uColor");
        
		// build the verticies for the map
		renderList = new float[6 * 2 * (state.mapSize + 1)];
		

		
		for (int x = 0; x < state.mapSize + 1; ++x)
		{
			renderList[renderIdx++] = x; renderList[renderIdx++] = 0; renderList[renderIdx++] = 0; // x, y, z
			renderList[renderIdx++] = x; renderList[renderIdx++] = state.mapSize + 1; renderList[renderIdx++] = 0; // x, y, z
			
			renderNum ++;
				
		}
		for (int y = 0; y < state.mapSize + 1; ++y)
		{
			renderList[renderIdx++] = 0; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
			renderList[renderIdx++] = state.mapSize + 1; renderList[renderIdx++] = y; renderList[renderIdx++] = 0; // x, y, z
			
			renderNum ++;
				
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
	    GLES20.glEnable(GLES20.GL_BLEND);
	    checkGlError("glEnable");
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	    checkGlError("glBlendFunc");
	    
	    // Add program to OpenGL environment
	    GLES20.glUseProgram(mShaderProgram);
        checkGlError("glUseProgram");
        
        // Prepare the triangle data
        GLES20.glEnableVertexAttribArray(maPositionHandleFlatTextured);
        
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(maPositionHandleFlatTextured, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
        
        GLES20.glUniformMatrix4fv(muMatrix, 1, false, mMVPMatrix, 0);

        GLES20.glUniform4fv(muColourHandleFlatTextured, 1, color, 0);
        
        // Draw the triangles
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, renderNum * 2);
        
        GLES20.glDisable(GLES20.GL_BLEND);
	}
}
