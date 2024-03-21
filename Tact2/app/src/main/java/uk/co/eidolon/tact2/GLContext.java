package uk.co.eidolon.tact2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public abstract class GLContext
{
	int mVertexShader;
	int mPixelShader;
	int mShaderProgram;
	
	FloatBuffer vertexBuffer;
	ByteBuffer byteBuf;
	
	float renderList[];
	int renderIdx = 0;
	int renderNum = 0;
	
	float[] mModelMatrix = new float[16];
	float[] mMVPMatrix = new float[16];
	
	protected void checkGlError(String op)
	{
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
        {
            Log.e("GLView", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
	
	float[] resizeArray(int newLength, float[] oldArray)
    {
    	float[] newArray = new float[newLength];
    	for (int i = 0; i < newLength; ++i)
    	{
    		newArray[i] = oldArray[i];
    	}
    	
    	return newArray;
    }
	
	private int AddShader(String shaderCode, int type)
	{
		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    final int shader = GLES20.glCreateShader(type);
	    checkGlError("glCreateShader");
	    
	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    
	    GLES20.glCompileShader(shader);
	    int[] compiled = new int[1];
	    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);

	    if (compiled[0] == 0)
	    {     
	      Log.d("Load Shader Failed", "Compilation\n"+GLES20.glGetShaderInfoLog(shader));
	      return 0;
	    }
	    
	    return shader;
	    
	}
	
	void SetShaders(String pixelShader, String vertexShader)
	{
		mVertexShader = AddShader(vertexShader, GLES20.GL_VERTEX_SHADER);
		mPixelShader = AddShader(pixelShader, GLES20.GL_FRAGMENT_SHADER);
		
		mShaderProgram = GLES20.glCreateProgram();
		checkGlError("glCreateProgram");
        GLES20.glAttachShader(mShaderProgram, mVertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(mShaderProgram, mPixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(mShaderProgram);
        checkGlError("glLinkProgram");
        int[] link = new int[1];
        GLES20.glGetProgramiv(mShaderProgram, GLES20.GL_LINK_STATUS, link, 0);

        if (link[0] <= 0)
        {

          Log.d("Load Program", "Linking Failed");
        }
	}
	
	public void SetMatrix(float[] projMatrix, float xPos, float yPos, float angle, float scale, float xCamera, float yCamera)
	{
		Matrix.setIdentityM(mModelMatrix, 0);
		
		Matrix.translateM(mModelMatrix, 0, xPos - xCamera, yPos - yCamera, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, angle, 0, 0, 1);
		Matrix.scaleM(mModelMatrix, 0, scale, scale, 1);
        
        Matrix.multiplyMM(mMVPMatrix, 0, projMatrix, 0, mModelMatrix, 0);
	}
	
}
