package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;

public class SpriteGroup extends TileGroup implements Tickable {
    private static final String TAG = "SpriteGroup";

    protected ArrayList<Sprite> spawned = new ArrayList<Sprite>();

    protected void spawn(Sprite s) {
        spawned.add(s);
    }

    public int size() {
        return tiles.size() + spawned.size();
    }

    public void tick(long elapsed) {
        for(int i=0; i<spawned.size(); i++)
            tiles.add(spawned.get(i));

        spawned.clear();

        for(int i=0; i<tiles.size(); i++)
            ((Sprite)tiles.get(i)).tick(elapsed);
    }

    protected int reap() {
        int removed = 0;

        for(int i=0; i<tiles.size(); i++) {

            Sprite t = (Sprite)tiles.get(i);

            if(!t.isAlive()) {
                tiles.remove(i);
                removed++;
                i--;
            }
            
        }
        
        return removed;
    }
}
