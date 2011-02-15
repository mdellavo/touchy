package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.Iterator;

public class SpriteGroup extends TileGroup implements Tickable {
    private static final String TAG = "SpriteGroup";

    protected ArrayList<Sprite> spawned = new ArrayList<Sprite>();

    protected void spawn(Sprite s) {
        spawned.add(s);
    }

    public void tick(long elapsed) {
        for(Sprite s: spawned)
            tiles.add(s);

        spawned.clear();

        for(Tile t : tiles)
            ((Sprite)t).tick(elapsed);
    }

    protected int reap() {
        int removed = 0;
     
        Iterator<Tile> iter = tiles.iterator();

        while(iter.hasNext()) {
            Sprite t = (Sprite)iter.next();
            
            if(!t.isAlive()) {
                iter.remove();
                removed++;
            }
        }
        
        return removed;
    }
}
