package org.quuux.touchy;

public class Vector3 {
    public float x,y,z;

    public Vector3() {}

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3(Vector3 v) {
        copy(v);
    }

    public void copy(Vector3 v) {
        x = v.x;
        y = v.y; 
        z = v.z;
    }

    public float magnitude() {
        return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public void normalize() { 
        float mag = magnitude();
        x /= mag;
        y /= mag;
        z /= mag;
    }

    public void scale(float factor) {
        x *= factor;
        y *= factor;
        z *= factor;
    }

    public String toString() {
        return "Vector3(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public float dot(Vector3 v) {
        return x*v.x + y*v.y + z*v.z;
    }

    public void add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void add(Vector3 o) {
        add(o.x, o.y, o.z);
    }

    public void subtract(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
    }

    public void subtract(Vector3 o) {
        subtract(o.x, o.y, o.z);
    }
}
