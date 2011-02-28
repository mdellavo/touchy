package org.quuux.touchy;

import android.graphics.Bitmap;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public abstract class ParticleEmitter implements Drawable, Tickable {

    private static final String TAG ="ParticleEmitter";

    public boolean loaded = false;

    protected Particle[] particles;

    protected FloatBuffer vertices;
    protected FloatBuffer sizes;
    protected FloatBuffer colors;

    protected Texture texture;

    public ParticleEmitter(int num_particles) {

        this.texture = getTexture();
        particles = new Particle[num_particles];

        vertices = GLHelper.floatBuffer(num_particles * 3);
        sizes = GLHelper.floatBuffer(num_particles);
        colors = GLHelper.floatBuffer(num_particles * 4);

        for(int i=0; i<particles.length; i++) {
            particles[i] = new Particle();
            particles[i].ttl = -1;
        }
    }

    abstract protected Texture getTexture(); 
    abstract public void spawnParticle(Particle p);
   
    protected void tickParticle(Particle p, long elapsed) {
        p.age++;
        p.velocity.add(p.acceleration);
        p.position.add(p.velocity);
    }

    public void tick(long elapsed) {
        vertices.clear();
        colors.clear();
        sizes.clear();

        for(int i=0; i<particles.length; i++) {

            if(particles[i].age > particles[i].ttl) {
                particles[i].age = 0;
                spawnParticle(particles[i]);
            }

            tickParticle(particles[i], elapsed);

            vertices.put(particles[i].position.x);
            vertices.put(particles[i].position.y);
            vertices.put(particles[i].position.z);

            colors.put(particles[i].color.r);
            colors.put(particles[i].color.g);
            colors.put(particles[i].color.b);
            colors.put(particles[i].color.a);

            sizes.put(particles[i].size);
        }

        vertices.position(0);
        colors.position(0);
        sizes.position(0);
    }

    public void load(GL10 gl) {
        if(!texture.loaded)
            texture.load(gl);

        gl.glEnable(GL11.GL_POINT_SPRITE_OES);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, 
                           GL10.GL_NICEST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_NICEST);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);

        gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);
        loaded = true;
    }

    public void draw(GL10 gl) {

        if(!loaded) 
            load(gl);

        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDisable(GL10.GL_DEPTH_TEST);

        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colors);
        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, particles.length);

        gl.glEnable(GL10.GL_DEPTH_TEST);

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
    }
}
