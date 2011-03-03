package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class Material {
    private static final String TAG = "Material";

    public static final int ILLUM_FLAT  = 1;
    public static final int ILLUM_SHINY = 2;

    public String name;
    public Color ambient, diffuse, specular, emission;
    public float shininess;
    public int illumination = ILLUM_FLAT;
    public Texture texture; 

    public Material(String name) {
        this.name = name;

        ambient = new Color(0.2f, 0.2f, 0.2f);
        diffuse = new Color(0.8f, 0.8f, 0.8f);
        specular = new Color(0, 0, 0);
        emission = new Color(0, 0, 0);
        shininess = 0;
    }

    public void setAlpha(float alpha) {
        ambient.a = diffuse.a = specular.a = alpha;
    }

    protected void flattenColor(float[] tmp, Color color) {
        tmp[0] = color.r;
        tmp[1] = color.g;
        tmp[2] = color.b;
        tmp[3] = color.a;
    }

    public void load(GL10 gl) {
        texture.load(gl);
    }

    public void enable(GL10 gl) {
        float[] tmp = new float[4];

        flattenColor(tmp, ambient);
        gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_AMBIENT, tmp, 0);

        flattenColor(tmp, diffuse);
        gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_DIFFUSE, tmp, 0);

        flattenColor(tmp, specular);
        gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_SPECULAR, tmp, 0);

        flattenColor(tmp, emission);
        gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_EMISSION, tmp, 0);

        gl.glMaterialf(GL10.GL_FRONT, GL10.GL_SHININESS, shininess);

        if(texture != null)
            texture.enable(gl);
    }
}
