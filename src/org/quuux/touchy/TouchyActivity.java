package org.quuux.touchy;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.content.Context;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.PointF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.DisplayMetrics;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class TouchyActivity extends Activity
{
    private TouchyGLSurfaceView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        view = new TouchyGLSurfaceView(this);

        TextureLoader.init(this);

        AsteroidCommandWorld world = new AsteroidCommandWorld();
        
        Random random = new Random();
        int num_asteroids = 100;
        for(int i=0; i<num_asteroids; i++) {
            AsteroidSprite a = new AsteroidSprite(world);

            a.position.x = 150.0f;
            a.position.y = 150.0f;

            a.velocity.x = (random.nextFloat() - .5f) * .01f;
            a.velocity.y = (random.nextFloat() - .5f) * .01f;

            a.rotation.z = random.nextFloat() * 360.0f;
            a.angular_velocity.z = random.nextFloat() - .5f;

            world.asteroids.add(a);
        }

        BackgroundTile b = new BackgroundTile(world);
        world.statics.add(b);

        TouchyRenderer renderer = new TouchyRenderer(world);
        view.setRenderer(renderer);
        setContentView(view);      
          
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
    }
}

class TouchyGLSurfaceView extends GLSurfaceView {

    public TouchyGLSurfaceView(Context context) {
        super(context);
    }

    public boolean onTouchEvent(final MotionEvent event) {

        // queueEvent(new Runnable() {
        //         public void run() {
        //         }
        //     }
        // );

        return true;
    }
}

class TouchyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TouchyRenderer";

    protected World world;
    protected int frames;

    protected long last;

    public TouchyRenderer(World world) {
        this.world = world;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f,0f,0f, 0.5f);
        gl.glDisable(GL10.GL_DEPTH_TEST);

        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                     /*GL10.GL_REPLACE*/ GL10.GL_MODULATE);
   }

    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(TAG, "surface created: " + width + "x" + height);

        gl.glViewport(0, 0, width, height);
        
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluOrtho2D(gl, 0f, (float)width, 0f, (float)height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        world.setSize(width, height);
        world.loadTexture(gl);
    }

    public void onDrawFrame(GL10 gl) {

        long now = System.currentTimeMillis();

        frames++;        
        
        world.tick();

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // draw sprites
        gl.glPushMatrix();            
        world.draw(gl);
        gl.glPopMatrix();

        if(now-last > 1000) {
            Log.d(TAG, "FPS: " + frames);
            last = now;
            frames = 0;
        }
    }
}

class TextureLoader {
    private static Context context;
    private static Map<Integer, Bitmap> cache;
    
    public static void init(Context context) {
        TextureLoader.context = context;
        TextureLoader.cache   = new HashMap<Integer, Bitmap>();
    }

    public static Bitmap get(int id) {
        Bitmap rv = TextureLoader.cache.get(Integer.valueOf(id));

        if(rv == null)
            rv = BitmapFactory.decodeResource(TextureLoader.context.getResources(), id);
        
        return rv;
    }
}

interface Drawable {
    void draw(GL10 gl);
    void loadTexture(GL10 gl);
}

interface Tickable {
    void tick();
}

class Vector3 {
    public float x, y, z;

    public Vector3() {}

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String toString() {
        return "Vector3(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}

abstract class Tile implements Drawable {
    private static final String TAG = "Tile";

    protected World world;

    protected Vector3 position;
    protected Vector3 rotation;
    protected Vector3 scale;
    protected Vector3 bounds;

    protected float vertices[];
    protected float texture_vertices[];
    protected int texture_drawable;
    protected int num_elements;

    protected FloatBuffer vertex_array;
    protected float color[];
    protected Bitmap texture;
    protected FloatBuffer texture_array;
    protected int texture_id = -1;

    public Tile(World world) {
        this.world = world;

        position = new Vector3();
        rotation = new Vector3();
        scale    = new Vector3(1f,1f,1f);
        bounds   = new Vector3();
        
        color = new float[4];
        color[0] = 1.0f;
        color[1] = 1.0f;
        color[2] = 1.0f;
        color[3] = 1.0f;
    }

    protected float[] getVertices() {
        return null;
    }

    protected float[] getTextureVertices() {
        return null;
    }

    protected int getTextureDrawable() {
        return 0;
    }

    protected boolean getTextureRepeat() {
        return false;
    }

    protected FloatBuffer loadVertices(float[] vertices) {
        Log.d(TAG, "Loading vertices: " + vertices);             

        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(vertices.length*4);
        byte_buffer.order(ByteOrder.nativeOrder());

        FloatBuffer rv = byte_buffer.asFloatBuffer();
        rv.put(vertices);
        rv.position(0);

        return rv;
    }

    public void loadTexture(GL10 gl) {

        vertices         = getVertices();
        texture_vertices = getTextureVertices();
        texture_drawable = getTextureDrawable();

        vertex_array     = loadVertices(vertices);
        texture_array    = loadVertices(texture_vertices);

        int[] texture_ids = new int[1];

        gl.glGenTextures(1, texture_ids, 0);
        texture_id = texture_ids[0];

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                           GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);
        
        Log.d(TAG, "loading texture: " + texture_drawable);

        Bitmap texture = TextureLoader.get(texture_drawable);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0);

        Log.d(TAG, "Loaded Texture ID: " + texture_id);
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex_array);

        gl.glColor4f(color[0], color[1], color[2], color[3]);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texture_array);
    
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glRotatef(rotation.x, 1f, 0, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 0, 1f);

        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
      
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    // Criteria for intersection:
    // for each dimension:
    //   min edge of B < max edge of A
    //   max edge of B > min edge of A

    // FIXME z with ortho flag
    public boolean contains(Tile o) {
        return (this.position.x-bounds.x) < (o.position.x+o.bounds.x) &&
               (this.position.x+bounds.x) > (o.position.x-o.bounds.x) &&
               (this.position.y-bounds.y) < (o.position.y+o.bounds.y) && 
               (this.position.y+bounds.y) > (o.position.y-o.bounds.y);
    }
}

class TileGroup implements Drawable {
    private static final String TAG = "TileGroup";

    protected Vector<Tile> tiles = new Vector<Tile>();
    
    public boolean add(Tile tile) {
        return tiles.add(tile);
    }

    public void loadTexture(GL10 gl) {
        for(Tile t : tiles)
            t.loadTexture(gl);
    }

    public void draw(GL10 gl) {
        for(Tile t : tiles)
            t.draw(gl);
    }

    public Vector<Tile> getTiles() {
        return tiles;
    }

    // FIXME pass in output param to avoid churning objects ?
    public Vector<Tile> collide(TileGroup others) {
        
        Vector<Tile> rv = new Vector<Tile>();

        for(Tile tile : tiles)
            for(Tile other : others.getTiles())
                if(tile.contains(other))
                    rv.add(other);

        return rv;
    }
}

class Sprite extends Tile implements Tickable {
    private static final String TAG = "Sprite";

    protected Vector3 velocity;
    protected Vector3 acceleration;
    
    protected Vector3 angular_velocity;
    protected Vector3 angular_acceleration;

    public Sprite(World world) {
        super(world);

        velocity = new Vector3();
        acceleration = new Vector3();
        angular_velocity = new Vector3();
        angular_acceleration = new Vector3();
    }

    public void tick() {
        velocity.x = velocity.x + acceleration.x;
        velocity.y = velocity.y + acceleration.y;
        velocity.z = velocity.z + acceleration.z;

        position.x = position.x + velocity.x;
        position.y = position.y + velocity.y;
        position.z = position.z + velocity.z;

        angular_velocity.x = angular_velocity.x + angular_acceleration.x;
        angular_velocity.y = angular_velocity.y + angular_acceleration.y;
        angular_velocity.z = angular_velocity.z + angular_acceleration.z;

        rotation.x = rotation.x + angular_velocity.x;
        rotation.y = rotation.y + angular_velocity.y;
        rotation.z = rotation.z + angular_velocity.z;
    }

    public String toString() {
        return "Sprite(pos: " + position.toString() + ", vel: " + 
            velocity.toString() + ")";
    }
}

class SpriteGroup extends TileGroup implements Tickable {
    private static final String TAG = "SpriteGroup";

    public void tick() {
        for(Tile t : tiles) {
            ((Sprite)t).tick();
        }
    }
}

abstract class World implements Drawable, Tickable {
    public int width, height;

    abstract public void draw(GL10 gl);
    abstract public void loadTexture(GL10 gl);
    abstract public void tick();

    public void setSize(int width, int height) {
        this.width  = width;
        this.height = height;
    }
}

class AsteroidCommandWorld extends World {
    private static final String TAG = "AsteroidCommandWorld";

    public TileGroup   statics     = new TileGroup();
    public SpriteGroup asteroids   = new SpriteGroup();
    public SpriteGroup projectiles = new SpriteGroup();
    public SpriteGroup stations    = new SpriteGroup();

    public void draw(GL10 gl) {
        statics.draw(gl);
        asteroids.draw(gl);
        projectiles.draw(gl);
        stations.draw(gl);
    }

    public void loadTexture(GL10 gl) {
        statics.loadTexture(gl);
        asteroids.loadTexture(gl);
        projectiles.loadTexture(gl);
        stations.loadTexture(gl);
    }

    public void tick() {
        asteroids.tick();
        projectiles.tick();
        stations.tick();

        // Vector<Tile> collisions = asteroids.collide(asteroids);
        // for(Tile t : collisions)
        //     Log.d(TAG, "collision: " + t);
        
    }
}

class AsteroidSprite extends Sprite {
    protected static float vertices[] = {
        -16f , -16f , 0,
        16f  , -16f , 0,
        -16f , 16f  , 0,
        16f  , 16f  , 0
    };

    protected static float texture_vertices[] = {
        0    , 1.0f ,
        1.0f , 1.0f ,
        0    , 0    ,
        1.0f , 0
    };
 
    protected static int texture_drawable = R.drawable.asteroid;

    protected static Vector3 bounds = new Vector3();

    public AsteroidSprite(World world) {
        super(world);
    }

    protected float[] getVertices() {
        return vertices;
    }

    protected float[] getTextureVertices() {
        return texture_vertices;
    }

    protected int getTextureDrawable() {
        return texture_drawable;
    }

    public void tick() {
        super.tick();
        
        if(position.x < 0 || position.x > world.width)
            velocity.x *= -1;

        if(position.y < 0 || position.y > world.height)
            velocity.y *= -1;
    }
}

class BackgroundTile extends Tile {
    protected static float vertices[] = {
        0f   , 0f   , 0, 
        300f , 0f   , 0,
        0f   , 500f , 0,
        300f , 500f , 0
    };

    protected static float texture_vertices[] = {
        0  , 4f ,
        4f , 4f ,
        0  , 0  ,
        4f , 0
    };

    protected static int texture_drawable = R.drawable.space;

    public BackgroundTile(World world) {
        super(world);
    }

    protected float[] getVertices() {
        vertices[3]  = world.width;
        vertices[9]  = world.width;        
        vertices[7]  = world.height;
        vertices[10] = world.height;        

        return vertices;
    }

    protected float[] getTextureVertices() {
        return texture_vertices;
    }

    protected int getTextureDrawable() {
        return texture_drawable;
    }
}