package org.quuux.touchy;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
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

    public Model(ArrayList<Vector3> vertices, ArrayList<Vector3> uvs,
                 ArrayList<Vector3> normals, String texture) {

        num_vertices = vertices.size();

        this.vertices = loadVertices(vertices);
        this.uvs = loadUVs(uvs);
        this.normals = loadVertices(normals);

        this.texture = texture;

        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    protected FloatBuffer loadUVs(ArrayList<Vector3> vertices) {
        FloatBuffer rv = GLHelper.floatBuffer(vertices.size()*2);

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
        }

        rv.position(0);

        return rv;
    }

    protected FloatBuffer loadVertices(ArrayList<Vector3> vertices) {
        FloatBuffer rv = GLHelper.floatBuffer(vertices.size()*3);

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
            rv.put(vertex.z);
        }

        rv.position(0);

        return rv;
    }

    public void loadTexture(GL10 gl) {
        Log.d(TAG, "Loading Texture Bitmap: " + texture);
        Bitmap bitmap = TextureLoader.get(texture);
        texture_id = GLHelper.loadTexture(gl, bitmap);
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
