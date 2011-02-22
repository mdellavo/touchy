package org.quuux.touchy;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Model {
    private static final String TAG = "Model";

    public String name;

    public boolean loaded = false;
    
    protected int num_vertices;

    protected FloatBuffer vertices;
    protected int vertex_id = -1;

    protected FloatBuffer uvs;
    protected int uv_id = -1;

    protected FloatBuffer normals;
    protected int normal_id = -1;

    protected Texture texture;

    public Color color;

    public Model(String name, Vector3[] vertices, Vector2[] uvs, 
                 Vector3[] normals, Texture texture) {

        this.name = name;
        this.texture = texture;

        num_vertices = vertices.length;

        this.vertices = GLHelper.toFloatBuffer(vertices);
        this.uvs = GLHelper.toFloatBuffer(uvs);
        this.normals = GLHelper.toFloatBuffer(normals);

        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void load(GL10 gl) {
        Log.d(TAG, "Loading: " + name);

        vertex_id = GLHelper.loadBufferObject(gl, this.vertices);
        Log.d(TAG, "vertex id: " + vertex_id);

        uv_id = GLHelper.loadBufferObject(gl, this.uvs);
        Log.d(TAG, "uv id: " + uv_id);

        normal_id = GLHelper.loadBufferObject(gl, this.normals);
        Log.d(TAG, "normal id: " + normal_id);

        texture.load(gl);
        
        loaded = true;
    }

    public void draw(GL10 gl) {
        if(!loaded)
            load(gl);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glColor4f(color.r, color.g, color.b, color.a);

        ((GL11)gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, vertex_id);
        ((GL11)gl).glVertexPointer(3, GL10.GL_FLOAT, 0, 0);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        ((GL11)gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, uv_id);
        ((GL11)gl).glTexCoordPointer(2, GL10.GL_FLOAT, 0, 0);

        ((GL11)gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, normal_id);
        ((GL11)gl).glNormalPointer(GL10.GL_FLOAT, 0, 0);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, num_vertices);

        ((GL11)gl).glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);

        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);    
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
