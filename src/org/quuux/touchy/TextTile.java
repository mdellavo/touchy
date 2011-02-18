package org.quuux.touchy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.Color;

import android.util.Log;

import android.opengl.GLUtils;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

// FIMXE is a point sprite better? no ortho needed

public class TextTile implements Drawable {

    private static final String TAG ="TextTile";

    public Vector3 position;
    public float width;
    public float height;

    protected Bitmap bitmap;
    protected Canvas canvas;
    protected Paint paint;
    protected int texture;
    protected boolean dirty;
    protected String text;

    public TextTile(float width, float height) {
        this(width, height, null);
    }

    public TextTile(float width, float height, Paint paint) {

        this.position = new Vector3();
        
        this.width = width;
        this.height = height;

        if(paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(32);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
        }        
        this.paint = paint;

        bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_4444);
        canvas = new Canvas(bitmap);
    }

    public void setText(String text) {
        this.text = text;
        dirty = true;
    }

    public void renderText(GL10 gl) {
        bitmap.eraseColor(0);

        // FIXME needs to be text size
        canvas.drawText(text, 0, 32, paint);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    }

    public void load(GL10 gl) {
        int[] textures = new int[1];

        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        gl.glGenTextures(1, textures, 0);
        texture = textures[0];
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_NEAREST);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_REPLACE);

        int[] crop = {0, (int)height, (int)width, -(int)height}; 
        ((GL11)gl).glTexParameteriv(GL10.GL_TEXTURE_2D, 
                                    GL11Ext.GL_TEXTURE_CROP_RECT_OES, crop, 0);

    }

    public void draw(GL10 gl) {
        
        if(dirty) {
            renderText(gl);
            dirty=false;
        }

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);

        ((GL11Ext) gl).glDrawTexfOES(position.x, 
                                     position.y, 
                                     position.z, 
                                     width,
                                     height);
    }
}
