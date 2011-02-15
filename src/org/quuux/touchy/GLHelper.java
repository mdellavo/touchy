package org.quuux.touchy;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// https://github.com/lithium/android-game
public class GLHelper
{
    private static final String TAG = "GLHelper";

    public static FloatBuffer floatBuffer(int size)
    {
        ByteBuffer byte_buf = ByteBuffer.allocateDirect(size * 4);
        byte_buf.order(ByteOrder.nativeOrder());
        return byte_buf.asFloatBuffer();
    }

    public static int loadTexture(GL10 gl, Bitmap bitmap) {
        int[] texture_ids = new int[1];
        
        gl.glGenTextures(1, texture_ids, 0);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_ids[0]);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, 
                           GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);
        
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        Log.d(TAG, "Loaded Texture ID: " + texture_ids[0]);        

        return texture_ids[0];
    }
}
