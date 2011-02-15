package org.quuux.touchy;

public class Color {
    public float r,g,b,a;

    public Color(float r, float g, float b, float a) {
	this.r = r;
	this.g = g;
	this.b = b;
	this.a = a;
    }

    public Color(float r, float g, float b) {
	this(r, g, b, 1.0f);
    }

    public String toString() {
	return "Color(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}

