package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

public abstract class World implements Drawable, Tickable {
    abstract public void draw(GL10 gl);
    abstract public void load(GL10 gl);
    abstract public void tick(long elapsed);
}
