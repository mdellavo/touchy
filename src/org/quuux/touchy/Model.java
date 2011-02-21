package org.quuux.touchy;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

public class Model {
    private static final String TAG = "Model";
    
    protected int num_vertices;
    protected FloatBuffer vertices;
    protected FloatBuffer uvs;
    protected FloatBuffer normals;
    protected String texture;
    protected int texture_id = -1;

    public Color color;

    public Model(Vector3[] vertices, Vector2[] uvs, Vector3[] normals, 
                 String texture) {

        num_vertices = vertices.length;

        this.vertices = GLHelper.toFloatBuffer(vertices);
        this.uvs = GLHelper.toFloatBuffer(uvs);
        this.normals = GLHelper.toFloatBuffer(normals);

        this.texture = texture;

        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void loadTexture(GL10 gl) {
        Log.d(TAG, "Loading Texture Bitmap: " + texture);
        Bitmap bitmap = TextureLoader.get(texture);
        texture_id = GLHelper.loadTexture(gl, bitmap);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, 
                           GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);
    }

    public void draw(GL10 gl) {
        if(texture_id == -1)
            loadTexture(gl);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glColor4f(color.r, color.g, color.b, color.a);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uvs);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, num_vertices);

        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);    
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
