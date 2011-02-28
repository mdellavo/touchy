package org.quuux.touchy;

public class Material {
    public static final int ILLUM_FLAT  = 1;
    public static final int ILLUM_SHINY = 2;

    public String name;
    public Color ambient, diffuse, specular;
    public float shininess;
    public int illumination = ILLUM_FLAT;
    public Texture texture; 

    public Material(String name) {
        this.name = name;

        ambient = new Color(0.2f, 0.2f, 0.2f);
        diffuse = new Color(0.8f, 0.8f, 0.8f);
        specular = new Color(1.0f, 1.0f, 1.0f);
        shininess = 0;  
    }

    public void setAlpha(float alpha) {
        ambient.a = diffuse.a = specular.a = alpha;
    }
}
