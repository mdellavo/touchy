package org.quuux.touchy;

import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;

public class TileGroup implements Drawable {
    private static final String TAG = "TileGroup";

    protected ArrayList<Tile> tiles   = new ArrayList<Tile>();
    
    public int size() {
        return tiles.size();
    }
    
    public boolean add(Tile tile) {
        return tiles.add(tile);
    }

    public void load(GL10 gl) {
        for(Tile t : tiles)
            t.load(gl);
    }

    public void draw(GL10 gl) {
        for(Tile t : tiles)
            t.draw(gl);
    }

    public ArrayList<Tile> getTiles() {
        return tiles;
    }

    public int collide(TileGroup others, CollisionListener listener) {

        int collisions = 0;

        for(Tile tile : tiles) {
            for(Tile other : others.getTiles()) {
                if(tile != other && tile.contains(other)) {
                    listener.onCollision(tile, other);
                    collisions++;
                }
            }
        }

        return collisions;
    }
}
