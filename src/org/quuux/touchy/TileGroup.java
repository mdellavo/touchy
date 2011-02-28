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
        for(int i=0; i<tiles.size(); i ++)
            tiles.get(i).load(gl);
    }

    public void draw(GL10 gl) {
        for(int i=0; i<tiles.size(); i ++)
            tiles.get(i).draw(gl);
    }

    public ArrayList<Tile> getTiles() {
        return tiles;
    }

    public int collide(TileGroup others, CollisionListener listener) {

        int collisions = 0;

        ArrayList<Tile> other_tiles = others.getTiles();

        for(int i=0; i<tiles.size(); i++) {
            for(int j=0; j<other_tiles.size(); j++) {

                Tile tile = tiles.get(i);
                Tile other = other_tiles.get(j);

                if(tile != other && tile.contains(other)) {
                    listener.onCollision(tile, other);
                    collisions++;
                }
            }
        }

        return collisions;
    }
}
