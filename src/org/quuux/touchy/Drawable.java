package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

public interface Drawable {
    public void draw(GL10 gl);
    public void load(GL10 gl);
}
