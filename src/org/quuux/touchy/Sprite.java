package org.quuux.touchy;

public class Sprite extends Tile implements Tickable {
    private static final String TAG = "Sprite";

    public Vector3 velocity;
    public Vector3 acceleration;
    
    public Vector3 angular_velocity;
    public Vector3 angular_acceleration;

    protected int age;
    protected boolean alive;

    public Sprite(World world) {
        super(world);

        alive = true;

        velocity = new Vector3();
        acceleration = new Vector3();
        angular_velocity = new Vector3();
        angular_acceleration = new Vector3();
    }

    public boolean isAlive() {
        return alive;
    }

    public void die() {
        alive = false;
    }

    public void tick(long elapsed) {
        age++;

        position.add(velocity);
        velocity.add(acceleration);

        rotation.add(angular_velocity);
        angular_velocity.add(angular_acceleration);
    }

    public String toString() {
        return "Sprite(pos: " + position.toString() + ", vel: " + 
            velocity.toString() + ")";
    }
}
