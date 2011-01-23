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
        int num_asteroids = 50;
        for(int i=0; i<num_asteroids; i++) {
            AsteroidSprite a = new AsteroidSprite(world);

            a.position.x = (random.nextFloat() * 20.0f) - 10f;
            a.position.y = (random.nextFloat() * 20.0f);
            a.position.z = (random.nextFloat() * 20.0f) - 10f;
            
            a.velocity.x = (random.nextFloat() * .02f) - .1f;
            a.velocity.y = (random.nextFloat() * .02f) - .1f;
            a.velocity.z = (random.nextFloat() * .02f) - .1f;

            a.angular_velocity.x = random.nextFloat() * -.01f;
            a.angular_velocity.y = random.nextFloat() * -.01f;
            a.angular_velocity.z = random.nextFloat() * -.01f;

            //a.rotation.z = random.nextFloat() * 360.0f;
            //a.angular_velocity.z = random.nextFloat() - .5f;

            world.asteroids.add(a);
        }

        BackgroundTile b = new BackgroundTile(world);        
        world.statics.add(b);

        GroundTile g = new GroundTile(world);
        world.statics.add(g);

        Camera camera = new Camera();

        TouchyRenderer renderer = new TouchyRenderer(world, camera);
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
    protected Camera camera;

    protected int frames;
    protected long last;

    public TouchyRenderer(World world, Camera camera) {
        this.world = world;
        this.camera = camera;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0f,0f,0f, 0.5f);

        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_LINEAR);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                     /*GL10.GL_REPLACE*/ GL10.GL_MODULATE);
   }

    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(TAG, "surface created: " + width + "x" + height);
        
        camera.setup(gl, width, height);
        world.loadTexture(gl);
    }

    public void onDrawFrame(GL10 gl) {

        long now = System.currentTimeMillis();

        frames++;        

        camera.tick();
        world.tick();

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        camera.update(gl);

        // draw sprites
        world.draw(gl);

        if(now-last > 1000) {
            Log.d(TAG, "FPS: " + frames);
            last = now;
            frames = 0;
        }
    }
}

class Camera {

    protected Vector3 eye;
    protected Vector3 center;
    protected Vector3 up;
    protected Vector3 rotation;
    protected float fov;
    protected float znear; 
    protected float zfar;

    public Camera() {
        eye = new Vector3(25, 25, 25);
        center = new Vector3(0, 0, 0);
        up = new Vector3(0f, 1f, 0f);    
        rotation = new Vector3(0, 0, 0);
        fov = 90f;
        znear = .1f;
        zfar = 100f;
    }

    void setup(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);        

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        GLU.gluPerspective(gl, fov, (float)width/(float)height, znear, zfar);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        GLU.gluLookAt(gl, eye.x, eye.y, eye.z, 
                          center.x, center.y, center.z,
                          up.x, up.y, up.z );
    }

    // FIXME add transation and other 
    void update(GL10 gl) {
        gl.glRotatef(rotation.x, 1f, 1f, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 1f, 1f);
    }

    void tick() {
        rotation.y += .0001f;
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

class ObjLoader {
    private static Context context;
    private static Map<String, Float[]> cache;
    
    // public static void init(Context context) {
    //     ObjLoader.context = context;
    //     ObjLoader.cache   = new HashMap<Integer, Float[]>();
    // }

    // public static float[] get(String key) {
    //     float[] rv = ObjLoader.cache.get(key);

    //     if(rv == null) {
    //     }

    //     return rv;
    // }    
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
        scale    = new Vector3(1f, 1f, 1f);
        bounds   = new Vector3(.8f, .8f, .8f);
        
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
        gl.glPushMatrix();

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex_array);

        gl.glColor4f(color[0], color[1], color[2], color[3]);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texture_array);

        gl.glRotatef(rotation.x, 1f, 0, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 0, 1f);    
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);

        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
      
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glPopMatrix();
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
               (this.position.y+bounds.y) > (o.position.y-o.bounds.y) && 
               (this.position.z-bounds.z) < (o.position.z+o.bounds.z) && 
               (this.position.z+bounds.z) > (o.position.z-o.bounds.z);
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

        for(Tile tile : tiles) {
            for(Tile other : others.getTiles()) {
                if(tile != other && tile.contains(other)) {
                    rv.add(other);
                }
            }
        }

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

        //Log.d(TAG, toString());
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
    public int width = 25, height = 25, depth = 25;

    abstract public void draw(GL10 gl);
    abstract public void loadTexture(GL10 gl);
    abstract public void tick();

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

        Vector<Tile> collisions = asteroids.collide(asteroids);
        for(Tile t: collisions) {
            Sprite s = (Sprite)t;
            s.velocity.x *= -1;
            s.velocity.y *= -1;
            s.velocity.z *= -1;
        }        
    }
}

class AsteroidSprite extends Sprite {

    float vertices[] = {
        // Face 1
        0.262869f	, -0.525738f	, 0.809012f,
        0.425323f	, -0.850654f	, 0.309011f,
        0.723600f	, -0.447215f	, 0.525720f,
        // Face 2
        0.425323f	, -0.850654f	, 0.309011f,
        0.262869f	, -0.525738f	, 0.809012f,
        -0.162456f	, -0.850654f	, 0.499995f,
        // Face 3
        -0.276385f	, -0.447215f	, 0.850640f,
        -0.162456f	, -0.850654f	, 0.499995f,
        0.262869f	, -0.525738f	, 0.809012f,
        // Face 4
        -0.162456f	, -0.850654f	, 0.499995f,
        0.000000f	, -1.000000f	, 0.000000f,
        0.425323f	, -0.850654f	, 0.309011f,
        // Face 5
        0.850648f	, -0.525736f	, 0.000000f,
        0.723600f	, -0.447215f	, 0.525720f,
        0.425323f	, -0.850654f	, 0.309011f,
        // Face 6
        0.425323f	, -0.850654f	, 0.309011f,
        0.425323f	, -0.850654f	, -0.309011f,
        0.850648f	, -0.525736f	, 0.000000f,
        // Face 7
        0.723600f	, -0.447215f	, -0.525720f,
        0.850648f	, -0.525736f	, 0.000000f,
        0.425323f	, -0.850654f	, -0.309011f,
        // Face 8
        0.425323f	, -0.850654f	, 0.309011f,
        0.000000f	, -1.000000f	, 0.000000f,
        0.425323f	, -0.850654f	, -0.309011f,
        // Face 9
        -0.688189f	, -0.525736f	, 0.499997f,
        -0.162456f	, -0.850654f	, 0.499995f,
        -0.276385f	, -0.447215f	, 0.850640f,
        // Face 10
        -0.162456f	, -0.850654f	, 0.499995f,
        -0.688189f	, -0.525736f	, 0.499997f,
        -0.525730f	, -0.850652f	, 0.000000f,
        // Face 11
        -0.894425f	, -0.447215f	, 0.000000f,
        -0.525730f	, -0.850652f	, 0.000000f,
        -0.688189f	, -0.525736f	, 0.499997f,
        // Face 12
        -0.525730f	, -0.850652f	, 0.000000f,
        0.000000f	, -1.000000f	, 0.000000f,
        -0.162456f	, -0.850654f	, 0.499995f,
        // Face 13
        -0.688189f	, -0.525736f	, -0.499997f,
        -0.525730f	, -0.850652f	, 0.000000f,
        -0.894425f	, -0.447215f	, 0.000000f,
        // Face 14
        -0.525730f	, -0.850652f	, 0.000000f,
        -0.688189f	, -0.525736f	, -0.499997f,
        -0.162456f	, -0.850654f	, -0.499995f,
        // Face 15
        -0.276385f	, -0.447215f	, -0.850640f,
        -0.162456f	, -0.850654f	, -0.499995f,
        -0.688189f	, -0.525736f	, -0.499997f,
        // Face 16
        -0.162456f	, -0.850654f	, -0.499995f,
        0.000000f	, -1.000000f	, 0.000000f,
        -0.525730f	, -0.850652f	, 0.000000f,
        // Face 17
        0.262869f	, -0.525738f	, -0.809012f,
        -0.162456f	, -0.850654f	, -0.499995f,
        -0.276385f	, -0.447215f	, -0.850640f,
        // Face 18
        -0.162456f	, -0.850654f	, -0.499995f,
        0.262869f	, -0.525738f	, -0.809012f,
        0.425323f	, -0.850654f	, -0.309011f,
        // Face 19
        0.723600f	, -0.447215f	, -0.525720f,
        0.425323f	, -0.850654f	, -0.309011f,
        0.262869f	, -0.525738f	, -0.809012f,
        // Face 20
        0.425323f	, -0.850654f	, -0.309011f,
        0.000000f	, -1.000000f	, 0.000000f,
        -0.162456f	, -0.850654f	, -0.499995f,
        // Face 21
        0.951058f	, 0.000000f	, 0.309013f,
        0.723600f	, -0.447215f	, 0.525720f,
        0.850648f	, -0.525736f	, 0.000000f,
        // Face 22
        0.850648f	, -0.525736f	, 0.000000f,
        0.951058f	, -0.000000f	, -0.309013f,
        0.951058f	, 0.000000f	, 0.309013f,
        // Face 23
        0.894425f	, 0.447215f	, -0.000000f,
        0.951058f	, 0.000000f	, 0.309013f,
        0.951058f	, -0.000000f	, -0.309013f,
        // Face 24
        0.951058f	, -0.000000f	, -0.309013f,
        0.850648f	, -0.525736f	, 0.000000f,
        0.723600f	, -0.447215f	, -0.525720f,
        // Face 25
        0.000000f	, 0.000000f	, 1.000000f,
        -0.276385f	, -0.447215f	, 0.850640f,
        0.262869f	, -0.525738f	, 0.809012f,
        // Face 26
        0.262869f	, -0.525738f	, 0.809012f,
        0.587786f	, 0.000000f	, 0.809017f,
        0.000000f	, 0.000000f	, 1.000000f,
        // Face 27
        0.276385f	, 0.447215f	, 0.850640f,
        0.000000f	, 0.000000f	, 1.000000f,
        0.587786f	, 0.000000f	, 0.809017f,
        // Face 28
        0.587786f	, 0.000000f	, 0.809017f,
        0.262869f	, -0.525738f	, 0.809012f,
        0.723600f	, -0.447215f	, 0.525720f,
        // Face 29
        -0.951058f	, 0.000000f	, 0.309013f,
        -0.894425f	, -0.447215f	, 0.000000f,
        -0.688189f	, -0.525736f	, 0.499997f,
        // Face 30
        -0.688189f	, -0.525736f	, 0.499997f,
        -0.587786f	, 0.000000f	, 0.809017f,
        -0.951058f	, 0.000000f	, 0.309013f,
        // Face 31
        -0.723600f	, 0.447215f	, 0.525720f,
        -0.951058f	, 0.000000f	, 0.309013f,
        -0.587786f	, 0.000000f	, 0.809017f,
        // Face 32
        -0.587786f	, 0.000000f	, 0.809017f,
        -0.688189f	, -0.525736f	, 0.499997f,
        -0.276385f	, -0.447215f	, 0.850640f,
        // Face 33
        -0.587786f	, -0.000000f	, -0.809017f,
        -0.276385f	, -0.447215f	, -0.850640f,
        -0.688189f	, -0.525736f	, -0.499997f,
        // Face 34
        -0.688189f	, -0.525736f	, -0.499997f,
        -0.951058f	, -0.000000f	, -0.309013f,
        -0.587786f	, -0.000000f	, -0.809017f,
        // Face 35
        -0.723600f	, 0.447215f	, -0.525720f,
        -0.587786f	, -0.000000f	, -0.809017f,
        -0.951058f	, -0.000000f	, -0.309013f,
        // Face 36
        -0.951058f	, -0.000000f	, -0.309013f,
        -0.688189f	, -0.525736f	, -0.499997f,
        -0.894425f	, -0.447215f	, 0.000000f,
        // Face 37
        0.587786f	, -0.000000f	, -0.809017f,
        0.723600f	, -0.447215f	, -0.525720f,
        0.262869f	, -0.525738f	, -0.809012f,
        // Face 38
        0.262869f	, -0.525738f	, -0.809012f,
        0.000000f	, -0.000000f	, -1.000000f,
        0.587786f	, -0.000000f	, -0.809017f,
        // Face 39
        0.276385f	, 0.447215f	, -0.850640f,
        0.587786f	, -0.000000f	, -0.809017f,
        0.000000f	, -0.000000f	, -1.000000f,
        // Face 40
        0.000000f	, -0.000000f	, -1.000000f,
        0.262869f	, -0.525738f	, -0.809012f,
        -0.276385f	, -0.447215f	, -0.850640f,
        // Face 41
        0.688189f	, 0.525736f	, 0.499997f,
        0.951058f	, 0.000000f	, 0.309013f,
        0.894425f	, 0.447215f	, -0.000000f,
        // Face 42
        0.951058f	, 0.000000f	, 0.309013f,
        0.688189f	, 0.525736f	, 0.499997f,
        0.587786f	, 0.000000f	, 0.809017f,
        // Face 43
        0.276385f	, 0.447215f	, 0.850640f,
        0.587786f	, 0.000000f	, 0.809017f,
        0.688189f	, 0.525736f	, 0.499997f,
        // Face 44
        0.587786f	, 0.000000f	, 0.809017f,
        0.723600f	, -0.447215f	, 0.525720f,
        0.951058f	, 0.000000f	, 0.309013f,
        // Face 45
        -0.262869f	, 0.525738f	, 0.809012f,
        0.000000f	, 0.000000f	, 1.000000f,
        0.276385f	, 0.447215f	, 0.850640f,
        // Face 46
        0.000000f	, 0.000000f	, 1.000000f,
        -0.262869f	, 0.525738f	, 0.809012f,
        -0.587786f	, 0.000000f	, 0.809017f,
        // Face 47
        -0.723600f	, 0.447215f	, 0.525720f,
        -0.587786f	, 0.000000f	, 0.809017f,
        -0.262869f	, 0.525738f	, 0.809012f,
        // Face 48
        -0.587786f	, 0.000000f	, 0.809017f,
        -0.276385f	, -0.447215f	, 0.850640f,
        0.000000f	, 0.000000f	, 1.000000f,
        // Face 49
        -0.850648f	, 0.525736f	, -0.000000f,
        -0.951058f	, 0.000000f	, 0.309013f,
        -0.723600f	, 0.447215f	, 0.525720f,
        // Face 50
        -0.951058f	, 0.000000f	, 0.309013f,
        -0.850648f	, 0.525736f	, -0.000000f,
        -0.951058f	, -0.000000f	, -0.309013f,
        // Face 51
        -0.723600f	, 0.447215f	, -0.525720f,
        -0.951058f	, -0.000000f	, -0.309013f,
        -0.850648f	, 0.525736f	, -0.000000f,
        // Face 52
        -0.951058f	, -0.000000f	, -0.309013f,
        -0.894425f	, -0.447215f	, 0.000000f,
        -0.951058f	, 0.000000f	, 0.309013f,
        // Face 53
        -0.262869f	, 0.525738f	, -0.809012f,
        -0.587786f	, -0.000000f	, -0.809017f,
        -0.723600f	, 0.447215f	, -0.525720f,
        // Face 54
        -0.587786f	, -0.000000f	, -0.809017f,
        -0.262869f	, 0.525738f	, -0.809012f,
        0.000000f	, -0.000000f	, -1.000000f,
        // Face 55
        0.276385f	, 0.447215f	, -0.850640f,
        0.000000f	, -0.000000f	, -1.000000f,
        -0.262869f	, 0.525738f	, -0.809012f,
        // Face 56
        0.000000f	, -0.000000f	, -1.000000f,
        -0.276385f	, -0.447215f	, -0.850640f,
        -0.587786f	, -0.000000f	, -0.809017f,
        // Face 57
        0.688189f	, 0.525736f	, -0.499997f,
        0.587786f	, -0.000000f	, -0.809017f,
        0.276385f	, 0.447215f	, -0.850640f,
        // Face 58
        0.587786f	, -0.000000f	, -0.809017f,
        0.688189f	, 0.525736f	, -0.499997f,
        0.951058f	, -0.000000f	, -0.309013f,
        // Face 59
        0.894425f	, 0.447215f	, -0.000000f,
        0.951058f	, -0.000000f	, -0.309013f,
        0.688189f	, 0.525736f	, -0.499997f,
        // Face 60
        0.951058f	, -0.000000f	, -0.309013f,
        0.723600f	, -0.447215f	, -0.525720f,
        0.587786f	, -0.000000f	, -0.809017f,
        // Face 61
        0.162456f	, 0.850654f	, 0.499995f,
        0.276385f	, 0.447215f	, 0.850640f,
        0.688189f	, 0.525736f	, 0.499997f,
        // Face 62
        0.688189f	, 0.525736f	, 0.499997f,
        0.525730f	, 0.850652f	, -0.000000f,
        0.162456f	, 0.850654f	, 0.499995f,
        // Face 63
        0.000000f	, 1.000000f	, -0.000000f,
        0.162456f	, 0.850654f	, 0.499995f,
        0.525730f	, 0.850652f	, -0.000000f,
        // Face 64
        0.525730f	, 0.850652f	, -0.000000f,
        0.688189f	, 0.525736f	, 0.499997f,
        0.894425f	, 0.447215f	, -0.000000f,
        // Face 65
        -0.425323f	, 0.850654f	, 0.309011f,
        -0.723600f	, 0.447215f	, 0.525720f,
        -0.262869f	, 0.525738f	, 0.809012f,
        // Face 66
        -0.262869f	, 0.525738f	, 0.809012f,
        0.162456f	, 0.850654f	, 0.499995f,
        -0.425323f	, 0.850654f	, 0.309011f,
        // Face 67
        0.000000f	, 1.000000f	, -0.000000f,
        -0.425323f	, 0.850654f	, 0.309011f,
        0.162456f	, 0.850654f	, 0.499995f,
        // Face 68
        0.162456f	, 0.850654f	, 0.499995f,
        -0.262869f	, 0.525738f	, 0.809012f,
        0.276385f	, 0.447215f	, 0.850640f,
        // Face 69
        -0.425323f	, 0.850654f	, -0.309011f,
        -0.723600f	, 0.447215f	, -0.525720f,
        -0.850648f	, 0.525736f	, -0.000000f,
        // Face 70
        -0.850648f	, 0.525736f	, -0.000000f,
        -0.425323f	, 0.850654f	, 0.309011f,
        -0.425323f	, 0.850654f	, -0.309011f,
        // Face 71
        0.000000f	, 1.000000f	, -0.000000f,
        -0.425323f	, 0.850654f	, -0.309011f,
        -0.425323f	, 0.850654f	, 0.309011f,
        // Face 72
        -0.425323f	, 0.850654f	, 0.309011f,
        -0.850648f	, 0.525736f	, -0.000000f,
        -0.723600f	, 0.447215f	, 0.525720f,
        // Face 73
        0.162456f	, 0.850654f	, -0.499995f,
        0.276385f	, 0.447215f	, -0.850640f,
        -0.262869f	, 0.525738f	, -0.809012f,
        // Face 74
        -0.262869f	, 0.525738f	, -0.809012f,
        -0.425323f	, 0.850654f	, -0.309011f,
        0.162456f	, 0.850654f	, -0.499995f,
        // Face 75
        0.000000f	, 1.000000f	, -0.000000f,
        0.162456f	, 0.850654f	, -0.499995f,
        -0.425323f	, 0.850654f	, -0.309011f,
        // Face 76
        -0.425323f	, 0.850654f	, -0.309011f,
        -0.262869f	, 0.525738f	, -0.809012f,
        -0.723600f	, 0.447215f	, -0.525720f,
        // Face 77
        0.525730f	, 0.850652f	, -0.000000f,
        0.894425f	, 0.447215f	, -0.000000f,
        0.688189f	, 0.525736f	, -0.499997f,
        // Face 78
        0.688189f	, 0.525736f	, -0.499997f,
        0.162456f	, 0.850654f	, -0.499995f,
        0.525730f	, 0.850652f	, -0.000000f,
        // Face 79
        0.000000f	, 1.000000f	, -0.000000f,
        0.525730f	, 0.850652f	, -0.000000f,
        0.162456f	, 0.850654f	, -0.499995f,
        // Face 80
        0.162456f	, 0.850654f	, -0.499995f,
        0.688189f	, 0.525736f	, -0.499997f,
        0.276385f	, 0.447215f	, -0.850640f,
    };

    float textures[] = {
        // Face 1
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 2
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 3
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 4
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 5
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 6
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 7
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 8
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 9
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 10
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 11
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 12
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 13
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 14
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 15
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 16
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 17
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 18
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 19
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 20
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 21
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 22
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 23
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 24
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 25
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 26
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 27
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 28
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 29
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 30
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 31
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 32
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 33
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 34
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 35
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 36
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 37
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 38
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 39
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 40
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 41
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 42
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 43
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 44
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 45
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 46
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 47
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 48
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 49
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 50
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 51
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 52
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 53
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 54
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 55
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 56
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 57
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 58
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 59
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 60
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 61
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 62
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 63
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 64
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 65
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 66
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 67
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 68
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 69
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 70
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 71
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 72
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 73
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 74
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 75
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 76
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 77
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 78
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 79
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
        // Face 80
        0.000000f	, 0.000000f,
        8.000000f	, 0.000000f,
        8.000000f	, 8.000000f,
    };
    
    float normals[] = {
        // Face 1
        0.471318f	, -0.661687f	, 0.583121f,
        0.471318f	, -0.661687f	, 0.583121f,
        0.471318f	, -0.661687f	, 0.583121f,
        // Face 2
        0.187594f	, -0.794658f	, 0.577345f,
        0.187594f	, -0.794658f	, 0.577345f,
        0.187594f	, -0.794658f	, 0.577345f,
        // Face 3
        -0.038547f	, -0.661687f	, 0.748789f,
        -0.038547f	, -0.661687f	, 0.748789f,
        -0.038547f	, -0.661687f	, 0.748789f,
        // Face 4
        0.102381f	, -0.943523f	, 0.315090f,
        0.102381f	, -0.943523f	, 0.315090f,
        0.102381f	, -0.943523f	, 0.315090f,
        // Face 5
        0.700228f	, -0.661687f	, 0.268049f,
        0.700228f	, -0.661687f	, 0.268049f,
        0.700228f	, -0.661687f	, 0.268049f,
        // Face 6
        0.607061f	, -0.794656f	, 0.000000f,
        0.607061f	, -0.794656f	, 0.000000f,
        0.607061f	, -0.794656f	, 0.000000f,
        // Face 7
        0.700228f	, -0.661688f	, -0.268049f,
        0.700228f	, -0.661688f	, -0.268049f,
        0.700228f	, -0.661688f	, -0.268049f,
        // Face 8
        0.331305f	, -0.943524f	, 0.000000f,
        0.331305f	, -0.943524f	, 0.000000f,
        0.331305f	, -0.943524f	, 0.000000f,
        // Face 9
        -0.408939f	, -0.661686f	, 0.628443f,
        -0.408939f	, -0.661686f	, 0.628443f,
        -0.408939f	, -0.661686f	, 0.628443f,
        // Face 10
        -0.491120f	, -0.794657f	, 0.356821f,
        -0.491120f	, -0.794657f	, 0.356821f,
        -0.491120f	, -0.794657f	, 0.356821f,
        // Face 11
        -0.724044f	, -0.661694f	, 0.194735f,
        -0.724044f	, -0.661694f	, 0.194735f,
        -0.724044f	, -0.661694f	, 0.194735f,
        // Face 12
        -0.268034f	, -0.943523f	, 0.194736f,
        -0.268034f	, -0.943523f	, 0.194736f,
        -0.268034f	, -0.943523f	, 0.194736f,
        // Face 13
        -0.724044f	, -0.661694f	, -0.194735f,
        -0.724044f	, -0.661694f	, -0.194735f,
        -0.724044f	, -0.661694f	, -0.194735f,
        // Face 14
        -0.491120f	, -0.794657f	, -0.356821f,
        -0.491120f	, -0.794657f	, -0.356821f,
        -0.491120f	, -0.794657f	, -0.356821f,
        // Face 15
        -0.408939f	, -0.661686f	, -0.628443f,
        -0.408939f	, -0.661686f	, -0.628443f,
        -0.408939f	, -0.661686f	, -0.628443f,
        // Face 16
        -0.268034f	, -0.943523f	, -0.194736f,
        -0.268034f	, -0.943523f	, -0.194736f,
        -0.268034f	, -0.943523f	, -0.194736f,
        // Face 17
        -0.038547f	, -0.661687f	, -0.748789f,
        -0.038547f	, -0.661687f	, -0.748789f,
        -0.038547f	, -0.661687f	, -0.748789f,
        // Face 18
        0.187594f	, -0.794658f	, -0.577345f,
        0.187594f	, -0.794658f	, -0.577345f,
        0.187594f	, -0.794658f	, -0.577345f,
        // Face 19
        0.471318f	, -0.661687f	, -0.583121f,
        0.471318f	, -0.661687f	, -0.583121f,
        0.471318f	, -0.661687f	, -0.583121f,
        // Face 20
        0.102381f	, -0.943523f	, -0.315090f,
        0.102381f	, -0.943523f	, -0.315090f,
        0.102381f	, -0.943523f	, -0.315090f,
        // Face 21
        0.904981f	, -0.330393f	, 0.268049f,
        0.904981f	, -0.330393f	, 0.268049f,
        0.904981f	, -0.330393f	, 0.268049f,
        // Face 22
        0.982246f	, -0.187599f	, 0.000000f,
        0.982246f	, -0.187599f	, 0.000000f,
        0.982246f	, -0.187599f	, 0.000000f,
        // Face 23
        0.992077f	, 0.125631f	, -0.000000f,
        0.992077f	, 0.125631f	, -0.000000f,
        0.992077f	, 0.125631f	, -0.000000f,
        // Face 24
        0.904981f	, -0.330393f	, -0.268049f,
        0.904981f	, -0.330393f	, -0.268049f,
        0.904981f	, -0.330393f	, -0.268049f,
        // Face 25
        0.024726f	, -0.330396f	, 0.943519f,
        0.024726f	, -0.330396f	, 0.943519f,
        0.024726f	, -0.330396f	, 0.943519f,
        // Face 26
        0.303531f	, -0.187597f	, 0.934171f,
        0.303531f	, -0.187597f	, 0.934171f,
        0.303531f	, -0.187597f	, 0.934171f,
        // Face 27
        0.306568f	, 0.125651f	, 0.943519f,
        0.306568f	, 0.125651f	, 0.943519f,
        0.306568f	, 0.125651f	, 0.943519f,
        // Face 28
        0.534590f	, -0.330395f	, 0.777851f,
        0.534590f	, -0.330395f	, 0.777851f,
        0.534590f	, -0.330395f	, 0.777851f,
        // Face 29
        -0.889698f	, -0.330386f	, 0.315092f,
        -0.889698f	, -0.330386f	, 0.315092f,
        -0.889698f	, -0.330386f	, 0.315092f,
        // Face 30
        -0.794656f	, -0.187595f	, 0.577348f,
        -0.794656f	, -0.187595f	, 0.577348f,
        -0.794656f	, -0.187595f	, 0.577348f,
        // Face 31
        -0.802607f	, 0.125648f	, 0.583125f,
        -0.802607f	, 0.125648f	, 0.583125f,
        -0.802607f	, 0.125648f	, 0.583125f,
        // Face 32
        -0.574584f	, -0.330397f	, 0.748793f,
        -0.574584f	, -0.330397f	, 0.748793f,
        -0.574584f	, -0.330397f	, 0.748793f,
        // Face 33
        -0.574584f	, -0.330397f	, -0.748793f,
        -0.574584f	, -0.330397f	, -0.748793f,
        -0.574584f	, -0.330397f	, -0.748793f,
        // Face 34
        -0.794655f	, -0.187595f	, -0.577348f,
        -0.794655f	, -0.187595f	, -0.577348f,
        -0.794655f	, -0.187595f	, -0.577348f,
        // Face 35
        -0.802607f	, 0.125648f	, -0.583125f,
        -0.802607f	, 0.125648f	, -0.583125f,
        -0.802607f	, 0.125648f	, -0.583125f,
        // Face 36
        -0.889698f	, -0.330386f	, -0.315092f,
        -0.889698f	, -0.330386f	, -0.315092f,
        -0.889698f	, -0.330386f	, -0.315092f,
        // Face 37
        0.534590f	, -0.330395f	, -0.777851f,
        0.534590f	, -0.330395f	, -0.777851f,
        0.534590f	, -0.330395f	, -0.777851f,
        // Face 38
        0.303531f	, -0.187597f	, -0.934171f,
        0.303531f	, -0.187597f	, -0.934171f,
        0.303531f	, -0.187597f	, -0.934171f,
        // Face 39
        0.306568f	, 0.125651f	, -0.943519f,
        0.306568f	, 0.125651f	, -0.943519f,
        0.306568f	, 0.125651f	, -0.943519f,
        // Face 40
        0.024726f	, -0.330396f	, -0.943519f,
        0.024726f	, -0.330396f	, -0.943519f,
        0.024726f	, -0.330396f	, -0.943519f,
        // Face 41
        0.889698f	, 0.330386f	, 0.315092f,
        0.889698f	, 0.330386f	, 0.315092f,
        0.889698f	, 0.330386f	, 0.315092f,
        // Face 42
        0.794655f	, 0.187595f	, 0.577348f,
        0.794655f	, 0.187595f	, 0.577348f,
        0.794655f	, 0.187595f	, 0.577348f,
        // Face 43
        0.574584f	, 0.330397f	, 0.748793f,
        0.574584f	, 0.330397f	, 0.748793f,
        0.574584f	, 0.330397f	, 0.748793f,
        // Face 44
        0.802607f	, -0.125648f	, 0.583125f,
        0.802607f	, -0.125648f	, 0.583125f,
        0.802607f	, -0.125648f	, 0.583125f,
        // Face 45
        -0.024726f	, 0.330396f	, 0.943519f,
        -0.024726f	, 0.330396f	, 0.943519f,
        -0.024726f	, 0.330396f	, 0.943519f,
        // Face 46
        -0.303531f	, 0.187597f	, 0.934171f,
        -0.303531f	, 0.187597f	, 0.934171f,
        -0.303531f	, 0.187597f	, 0.934171f,
        // Face 47
        -0.534590f	, 0.330395f	, 0.777851f,
        -0.534590f	, 0.330395f	, 0.777851f,
        -0.534590f	, 0.330395f	, 0.777851f,
        // Face 48
        -0.306568f	, -0.125651f	, 0.943519f,
        -0.306568f	, -0.125651f	, 0.943519f,
        -0.306568f	, -0.125651f	, 0.943519f,
        // Face 49
        -0.904981f	, 0.330393f	, 0.268049f,
        -0.904981f	, 0.330393f	, 0.268049f,
        -0.904981f	, 0.330393f	, 0.268049f,
        // Face 50
        -0.982246f	, 0.187599f	, -0.000000f,
        -0.982246f	, 0.187599f	, -0.000000f,
        -0.982246f	, 0.187599f	, -0.000000f,
        // Face 51
        -0.904981f	, 0.330393f	, -0.268049f,
        -0.904981f	, 0.330393f	, -0.268049f,
        -0.904981f	, 0.330393f	, -0.268049f,
        // Face 52
        -0.992077f	, -0.125631f	, 0.000000f,
        -0.992077f	, -0.125631f	, 0.000000f,
        -0.992077f	, -0.125631f	, 0.000000f,
        // Face 53
        -0.534590f	, 0.330395f	, -0.777851f,
        -0.534590f	, 0.330395f	, -0.777851f,
        -0.534590f	, 0.330395f	, -0.777851f,
        // Face 54
        -0.303531f	, 0.187597f	, -0.934171f,
        -0.303531f	, 0.187597f	, -0.934171f,
        -0.303531f	, 0.187597f	, -0.934171f,
        // Face 55
        -0.024726f	, 0.330396f	, -0.943519f,
        -0.024726f	, 0.330396f	, -0.943519f,
        -0.024726f	, 0.330396f	, -0.943519f,
        // Face 56
        -0.306568f	, -0.125651f	, -0.943519f,
        -0.306568f	, -0.125651f	, -0.943519f,
        -0.306568f	, -0.125651f	, -0.943519f,
        // Face 57
        0.574584f	, 0.330397f	, -0.748793f,
        0.574584f	, 0.330397f	, -0.748793f,
        0.574584f	, 0.330397f	, -0.748793f,
        // Face 58
        0.794656f	, 0.187595f	, -0.577348f,
        0.794656f	, 0.187595f	, -0.577348f,
        0.794656f	, 0.187595f	, -0.577348f,
        // Face 59
        0.889698f	, 0.330386f	, -0.315092f,
        0.889698f	, 0.330386f	, -0.315092f,
        0.889698f	, 0.330386f	, -0.315092f,
        // Face 60
        0.802607f	, -0.125648f	, -0.583125f,
        0.802607f	, -0.125648f	, -0.583125f,
        0.802607f	, -0.125648f	, -0.583125f,
        // Face 61
        0.408939f	, 0.661686f	, 0.628443f,
        0.408939f	, 0.661686f	, 0.628443f,
        0.408939f	, 0.661686f	, 0.628443f,
        // Face 62
        0.491120f	, 0.794657f	, 0.356821f,
        0.491120f	, 0.794657f	, 0.356821f,
        0.491120f	, 0.794657f	, 0.356821f,
        // Face 63
        0.268034f	, 0.943523f	, 0.194736f,
        0.268034f	, 0.943523f	, 0.194736f,
        0.268034f	, 0.943523f	, 0.194736f,
        // Face 64
        0.724044f	, 0.661694f	, 0.194735f,
        0.724044f	, 0.661694f	, 0.194735f,
        0.724044f	, 0.661694f	, 0.194735f,
        // Face 65
        -0.471318f	, 0.661687f	, 0.583121f,
        -0.471318f	, 0.661687f	, 0.583121f,
        -0.471318f	, 0.661687f	, 0.583121f,
        // Face 66
        -0.187594f	, 0.794658f	, 0.577345f,
        -0.187594f	, 0.794658f	, 0.577345f,
        -0.187594f	, 0.794658f	, 0.577345f,
        // Face 67
        -0.102381f	, 0.943523f	, 0.315090f,
        -0.102381f	, 0.943523f	, 0.315090f,
        -0.102381f	, 0.943523f	, 0.315090f,
        // Face 68
        0.038547f	, 0.661687f	, 0.748789f,
        0.038547f	, 0.661687f	, 0.748789f,
        0.038547f	, 0.661687f	, 0.748789f,
        // Face 69
        -0.700228f	, 0.661687f	, -0.268049f,
        -0.700228f	, 0.661687f	, -0.268049f,
        -0.700228f	, 0.661687f	, -0.268049f,
        // Face 70
        -0.607061f	, 0.794656f	, 0.000000f,
        -0.607061f	, 0.794656f	, 0.000000f,
        -0.607061f	, 0.794656f	, 0.000000f,
        // Face 71
        -0.331305f	, 0.943524f	, 0.000000f,
        -0.331305f	, 0.943524f	, 0.000000f,
        -0.331305f	, 0.943524f	, 0.000000f,
        // Face 72
        -0.700228f	, 0.661688f	, 0.268049f,
        -0.700228f	, 0.661688f	, 0.268049f,
        -0.700228f	, 0.661688f	, 0.268049f,
        // Face 73
        0.038547f	, 0.661687f	, -0.748789f,
        0.038547f	, 0.661687f	, -0.748789f,
        0.038547f	, 0.661687f	, -0.748789f,
        // Face 74
        -0.187594f	, 0.794658f	, -0.577345f,
        -0.187594f	, 0.794658f	, -0.577345f,
        -0.187594f	, 0.794658f	, -0.577345f,
        // Face 75
        -0.102381f	, 0.943523f	, -0.315090f,
        -0.102381f	, 0.943523f	, -0.315090f,
        -0.102381f	, 0.943523f	, -0.315090f,
        // Face 76
        -0.471318f	, 0.661687f	, -0.583121f,
        -0.471318f	, 0.661687f	, -0.583121f,
        -0.471318f	, 0.661687f	, -0.583121f,
        // Face 77
        0.724044f	, 0.661694f	, -0.194735f,
        0.724044f	, 0.661694f	, -0.194735f,
        0.724044f	, 0.661694f	, -0.194735f,
        // Face 78
        0.491120f	, 0.794657f	, -0.356821f,
        0.491120f	, 0.794657f	, -0.356821f,
        0.491120f	, 0.794657f	, -0.356821f,
        // Face 79
        0.268034f	, 0.943523f	, -0.194736f,
        0.268034f	, 0.943523f	, -0.194736f,
        0.268034f	, 0.943523f	, -0.194736f,
        // Face 80
        0.408939f	, 0.661686f	, -0.628443f,
        0.408939f	, 0.661686f	, -0.628443f,
        0.408939f	, 0.661686f	, -0.628443f,
    };
 
    protected static int texture_drawable = R.drawable.marble;

    public AsteroidSprite(World world) {
        super(world);
    }

    protected float[] getVertices() {
        return vertices;
    }

    protected float[] getTextureVertices() {
        return textures;
    }

    protected int getTextureDrawable() {
        return texture_drawable;
    }

    public void tick() {
        super.tick();
        
        if(position.x <= -world.width || position.x >= world.width) {
            velocity.x *= -1;
        }
        
        if(position.y <= 0 || position.y >= world.height) {
            velocity.y *= -1;
        }

        if(position.z <= -world.depth || position.z >= world.depth) {
            velocity.z *= -1;
        }

    }
}

class BackgroundTile extends Tile {
    protected static float vertices[] = {
        -50f , -50f , -25f,
        50f  , -50f , -25f,
        -50f , 50f  , -25f, 
        50f  , 50f  , -25f
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
        return vertices;
    }

    protected float[] getTextureVertices() {
        return texture_vertices;
    }

    protected int getTextureDrawable() {
        return texture_drawable;
    }
}

class GroundTile extends Tile {
    protected static float vertices[] = {

        -25f , 0, -25f ,
        25f  , 0, -25f ,
        -25f , 0, 25f  ,
        25f  , 0, 25f  
    };

    protected static float texture_vertices[] = {
        0  , 4f ,
        4f , 4f ,
        0  , 0  ,
        4f , 0
    };

    protected static int texture_drawable = R.drawable.checker;

    public GroundTile(World world) {
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
}

