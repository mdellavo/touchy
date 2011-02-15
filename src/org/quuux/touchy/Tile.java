package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

public abstract class Tile implements Drawable {
    private static final String TAG = "Tile";

    protected World world;
    protected Model model;

    public Vector3 position;
    public Vector3 rotation;
    public Vector3 scale;
    public Vector3 bounds;

    public Tile(World world) {
        this.world = world;

        model = getModel();

        position = new Vector3();
        rotation = new Vector3();
        scale    = new Vector3(1f, 1f, 1f);
        bounds   = new Vector3(.8f, .8f, .8f);
    }

    protected Model getModel() {
        return null;
    }

    public void load(GL10 gl) {
        model.loadTexture(gl);
    }

    public void draw(GL10 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glRotatef(rotation.x, 1f, 0, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 0, 1f);    

        model.draw(gl);

        gl.glPopMatrix();
    }

    // Criteria for intersection:
    // for each dimension:
    //   min edge of B < max edge of A
    //   max edge of B > min edge of A

    public boolean contains(Tile o) {
        return (this.position.x-bounds.x) < (o.position.x+o.bounds.x) &&
               (this.position.x+bounds.x) > (o.position.x-o.bounds.x) &&
               (this.position.y-bounds.y) < (o.position.y+o.bounds.y) && 
               (this.position.y+bounds.y) > (o.position.y-o.bounds.y) && 
               (this.position.z-bounds.z) < (o.position.z+o.bounds.z) && 
               (this.position.z+bounds.z) > (o.position.z-o.bounds.z);
    }
}
