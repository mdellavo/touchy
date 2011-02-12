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
import android.content.res.AssetManager;
import android.os.Build;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class TouchyActivity extends Activity
{
    private static final String TAG = "TouchyActivity";

    private TouchyGLSurfaceView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ObjLoader.init(this);
        TextureLoader.init(this);

        AsteroidCommandWorld world = new AsteroidCommandWorld();
        Camera camera = new Camera();

        TouchyRenderer renderer = new TouchyRenderer(world, camera);

        view = new TouchyGLSurfaceView(this, renderer);

        view.setGLWrapper(new GLSurfaceView.GLWrapper() {
                public GL wrap(GL gl) {
                    return new MatrixTrackingGL(gl);
                }
            });

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

    protected static final String TAG = "TouchyGLSurfaceView";

    protected TouchyRenderer renderer;

    public TouchyGLSurfaceView(Context context, TouchyRenderer renderer) {
        super(context);

        this.renderer = renderer;
        this.setRenderer(renderer);
    }

    public Vector3 projectTouchToWorld(float x, float y) {
        int width = renderer.getWidth();
        int height = renderer.getHeight();

        int[] view = new int[] {0, 0, width, height};

        float[] touch_position = new float[4];
        int rv = GLU.gluUnProject(x, view[3] - y, 1f, 
                                  renderer.getCurrentModelView(), 0, 
                                  renderer.getCurrentProjection(), 0, 
                                  view, 0,
                                  touch_position, 0);

        touch_position[0] /= touch_position[3];
        touch_position[1] /= touch_position[3];
        touch_position[2] /= touch_position[3];
        touch_position[3] /= touch_position[3];

        return new Vector3(touch_position[0], 
                           touch_position[1], 
                           touch_position[2]); 
    }

    public boolean onTouchEvent(final MotionEvent event) {
        
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            queueEvent(new Runnable() {
                    public void run() {
                        Vector3 vec = projectTouchToWorld(event.getX(), 
                                                          event.getY());

                        AsteroidCommandWorld world = (AsteroidCommandWorld)renderer.getWorld();
                        world.fireAt(vec);
                    }
                });
        }
        
        return true;
    }
}

class TouchyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TouchyRenderer";

    protected World world;
    protected Camera camera;

    protected int frames;
    protected int last_frames;
    protected long last;
    protected long last_fps;

    protected int width;
    protected int height;

    protected MatrixGrabber matrix_grabber;

    public TouchyRenderer(World world, Camera camera) {
        this.world = world;
        this.camera = camera;

        matrix_grabber = new MatrixGrabber();
    }

    public float[] getCurrentProjection() {
        return matrix_grabber.mProjection;
    }

    public float[] getCurrentModelView() {
        return matrix_grabber.mModelView;
    }

    public World getWorld() {
        return world;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d(TAG, "Display: " + Build.DISPLAY);
        Log.d(TAG, "Manufacturer: " + Build.MANUFACTURER);
        Log.d(TAG, "Model: " + Build.MODEL);
        Log.d(TAG, "Product: " + Build.PRODUCT);
        Log.d(TAG, "Device: " + Build.DEVICE);
        Log.d(TAG, "Brand: " + Build.BRAND);

        Log.d(TAG, "Hardware: " + Build.HARDWARE);
        Log.d(TAG, "Vendor: " + gl.glGetString(GL10.GL_VENDOR));
        Log.d(TAG, "Renderer: " + gl.glGetString(GL10.GL_RENDERER));
        Log.d(TAG, "Version: " + gl.glGetString(GL10.GL_VERSION));
        Log.d(TAG, "Extensions: " + gl.glGetString(GL10.GL_EXTENSIONS));

        gl.glClearColor(0f,0f,0f, 0.5f);

        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_LINEAR);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                     /*GL10.GL_REPLACE*/ GL10.GL_MODULATE);
   }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;

        Log.d(TAG, "surface created: " + width + "x" + height);
        
        camera.setup(gl, width, height);       
        world.load(gl);
    }

    public void onDrawFrame(GL10 gl) {

        long now = System.currentTimeMillis();
        long elapsed = now - last;

        frames++;        

        camera.tick(elapsed);
        world.tick(elapsed);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        camera.update(gl);
 
        matrix_grabber.getCurrentState(gl);

        // draw sprites
        world.draw(gl);

        if(now - last_fps > 1000) {
            int fps = frames - last_frames;
            Log.d(TAG, "FPS: " + fps);
            last_frames = frames;
            last_fps = now;
        }

        last = now;
    }
}

class RandomGenerator {
    protected static Random random = new Random();
   
    public static float randomRange(float min, float max) {
        return min + ((max-min) * random.nextFloat());
    }

    public static int randomInt(int min, int max) {
        return min + random.nextInt(max-min);
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
        eye = new Vector3(0, 0, 25);
        center = new Vector3(0, 0, 0);
        up = new Vector3(0f, 1f, 0f);    
        rotation = new Vector3(0, 0, 0);
        fov = 45f;
        znear = .1f;
        zfar = 1000f;
    }

    public void setup(GL10 gl, int width, int height) {
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

    // FIXME add translation, zoom, etc
    public void update(GL10 gl) {
        gl.glRotatef(rotation.x, 1f, 1f, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 1f, 1f);
    }

    public void tick(long elapsed) {
        //rotation.y += .0001f;
    }

}

class TextureLoader {
    private static final String TAG = "TextureLoader";
    
    private static Context context;
    private static Map<String, Bitmap> cache;
    
    public static void init(Context context) {
        TextureLoader.context = context;
        TextureLoader.cache   = new HashMap<String, Bitmap>();
    }

    public static Bitmap get(String key) {
        Bitmap rv = TextureLoader.cache.get(key);

        if(rv == null) {
            rv = openTexture(key);
            cache.put(key, rv);
        }
        
        return rv;
    }

    protected static Bitmap openTexture(String key) {       
        AssetManager asset_manager = context.getAssets();

        Bitmap rv = null;

        try {
            String texture_path = "textures/" + key + ".png";
            
            Log.d(TAG, "Opening texture: " + texture_path);

            InputStream in = asset_manager.open(texture_path);
            rv = BitmapFactory.decodeStream(in);
        } catch(IOException e) {
            Log.d(TAG, "Could not open texture: " + key);
        }

        return rv;
    }

}

class Color {
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

class Material {
    public String name;
    public Color ambient, diffuse, specular;
    public float shininess;
    public int illumination_model;
    public String texture_map; 
}

// https://github.com/lithium/android-game
class GLHelper
{
    private static final String TAG = "GLHelper";

    public static FloatBuffer floatBuffer(int size)
    {
        ByteBuffer byte_buf = ByteBuffer.allocateDirect(size * 4);
        byte_buf.order(ByteOrder.nativeOrder());
        return byte_buf.asFloatBuffer();
    }

    public static int loadTexture(GL10 gl, Bitmap bitmap) {
        int[] texture_ids = new int[1];
        
        gl.glGenTextures(1, texture_ids, 0);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_ids[0]);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, 
                           GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_LINEAR);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);
        
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        Log.d(TAG, "Loaded Texture ID: " + texture_ids[0]);        

        return texture_ids[0];
    }
}

// FIXME going to need to create a model per particle 
class Model {
    private static final String TAG = "Model";
    
    protected int num_vertices;
    protected FloatBuffer vertices;
    protected FloatBuffer uvs;
    protected FloatBuffer normals;
    protected Bitmap bitmap;
    protected int texture_id;

    public Color color;

    public Model(ArrayList<Vector3> vertices, ArrayList<Vector3> uvs,
                 ArrayList<Vector3> normals, Bitmap bitmap) {

        this.vertices = loadVertices(vertices);
        this.uvs = loadUVs(uvs);
        this.normals = loadVertices(normals);

        this.bitmap = bitmap;

        color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    protected FloatBuffer loadUVs(ArrayList<Vector3> vertices) {
        FloatBuffer rv = GLHelper.floatBuffer(vertices.size()*2);

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
        }

        rv.position(0);

        return rv;
    }

    protected FloatBuffer loadVertices(ArrayList<Vector3> vertices) {
        FloatBuffer rv = GLHelper.floatBuffer(vertices.size()*3);

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
            rv.put(vertex.z);
        }

        rv.position(0);

        return rv;
    }

    public void loadTexture(GL10 gl) {
        texture_id = GLHelper.loadTexture(gl, bitmap);
    }

    // FIXME move texture binding to texture manager
    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);

        gl.glColor4f(color.r, color.g, color.b, color.a);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uvs);

        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, num_vertices / 3);
      
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }
}

class ObjLoader {
    private static final String TAG = "ObjLoader";

    private static Context context;
    private static Map<String, Model> cache;
    
    public static void init(Context context) {
        ObjLoader.context = context;
        ObjLoader.cache   = new HashMap<String, Model>();
    }

    public static Model get(String key) {
        Model rv = ObjLoader.cache.get(key);

        if(rv == null) {
            rv = loadModel(key);
            cache.put(key, rv);
        }

        return rv;
    }

    protected static BufferedReader openModel(String key) {       
        AssetManager asset_manager = context.getAssets();

        BufferedReader rv = null;

        try {
            String model_path = "models/" + key + ".obj";

            Log.d(TAG, "Loading model: " + model_path);

            InputStream in = asset_manager.open(model_path);
            InputStreamReader ins = new InputStreamReader(in);
            rv = new BufferedReader(ins);
        } catch(IOException e) {
            Log.d(TAG, "Could not open model: " + key);
        }

        return rv;
    }

    protected static Vector3 parsePoint(String[] parts) {
        return new Vector3( Float.valueOf(parts[1]), 
                            Float.valueOf(parts[2]), 
                            parts.length>3 ? Float.valueOf(parts[3]) : 0 );
    }

    protected static Vector3[][] parseFace( ArrayList<Vector3> v, 
                                            ArrayList<Vector3> vt, 
                                            ArrayList<Vector3> vn, 
                                            String[] parts ) {

        ArrayList[] tables = {v, vt, vn};

        String p1[] = parts[1].split("/");
        String p2[] = parts[2].split("/");
        String p3[] = parts[3].split("/");

        Vector3[][] rv = new Vector3[3][3];

        for(int i=0; i<3; i++) {
            rv[i][0] = (Vector3)tables[i].get(Integer.parseInt(p1[i])-1); 
            rv[i][1] = (Vector3)tables[i].get(Integer.parseInt(p2[i])-1); 
            rv[i][2] = (Vector3)tables[i].get(Integer.parseInt(p3[i])-1); 
        }

        return rv;
    }

    protected static Model loadModel(String key) {
        BufferedReader in = openModel(key);

        if(in == null)
            return null;
        
        ArrayList<Vector3> v = new ArrayList<Vector3>();
        ArrayList<Vector3> vt = new ArrayList<Vector3>();
        ArrayList<Vector3> vn = new ArrayList<Vector3>();

        ArrayList<Vector3> vertices = new ArrayList<Vector3>();
        ArrayList<Vector3> uvs = new ArrayList<Vector3>();
        ArrayList<Vector3> normals = new ArrayList<Vector3>();

        Model rv = null;

        try {
            String line;
        
            while((line = in.readLine()) != null) {
            
                String[] parts = line.split("\\s");

                if(parts[0] == "#")
                    continue;
                else if(parts[0].equals("vt"))
                    vt.add(parsePoint(parts));
                else if(parts[0].equals("vn"))
                    vn.add(parsePoint(parts));
                else if(parts[0].equals("v"))
                    v.add(parsePoint(parts));
                else if(parts[0].equals("f")) {

                    Vector3[][] face = parseFace(v, vt, vn, parts);
                    ArrayList vectors[] = {vertices, uvs, normals};

                    for(int i=0; i<vectors.length; i++)
                        for(int j=0; j<face[i].length; j++)
                            vectors[i].add(face[i][j]);
                }
            }
            
            rv = new Model(vertices, uvs, normals, TextureLoader.get(key));

        } catch(IOException e) {
            Log.d(TAG, "error loading model: " + e);
        }

        return rv;
    }
}

interface Drawable {
    public void draw(GL10 gl);
    public void load(GL10 gl);
}

interface Tickable {
    public void tick(long elapsed);
}

class Vector3 {
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

class Vector2 extends Vector3 {
    public Vector2() {}
    
    public Vector2(float x, float y) {
        super(x, y, 0);
    }

    public Vector2(Vector2 v) {
        super(v);
        z = 0;
    }

    public String toString() {
        return "Vector2(" + this.x + ", " + this.y + ")";
    }

    public void add(float x, float y) {
        add(x, y, 0);
    }

    public void add(Vector2 o) {
        add(o.x, o.y);
    }

    public void subtract(float x, float y) {
        subtract(x, y, 0);
    }

    public void subtract(Vector2 o) {
        subtract(o.x, o.y, 0);
    }
}

abstract class Tile implements Drawable {
    private static final String TAG = "Tile";

    protected World world;
    protected Model model;

    public Vector3 position;
    public Vector3 rotation;
    public Vector3 scale;
    public Vector3 bounds;

    public Tile(World world) {
        this.world = world;

        model = getModel();

        position = new Vector3();
        rotation = new Vector3();
        scale    = new Vector3(1f, 1f, 1f);
        bounds   = new Vector3(.8f, .8f, .8f);
    }

    protected Model getModel() {
        return null;
    }

    public void load(GL10 gl) {
        model.loadTexture(gl);
    }

    public void draw(GL10 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glRotatef(rotation.x, 1f, 0, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 0, 1f);    

        model.draw(gl);

        gl.glPopMatrix();
    }

    // Criteria for intersection:
    // for each dimension:
    //   min edge of B < max edge of A
    //   max edge of B > min edge of A

    public boolean contains(Tile o) {
        return (this.position.x-bounds.x) < (o.position.x+o.bounds.x) &&
               (this.position.x+bounds.x) > (o.position.x-o.bounds.x) &&
               (this.position.y-bounds.y) < (o.position.y+o.bounds.y) && 
               (this.position.y+bounds.y) > (o.position.y-o.bounds.y) && 
               (this.position.z-bounds.z) < (o.position.z+o.bounds.z) && 
               (this.position.z+bounds.z) > (o.position.z-o.bounds.z);
    }
}

abstract class CollisionListener {
    abstract public void onCollision(Tile a, Tile b);
}

class TileGroup implements Drawable {
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

class Sprite extends Tile implements Tickable {
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

class SpriteGroup extends TileGroup implements Tickable {
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

abstract class World implements Drawable, Tickable {
    abstract public void draw(GL10 gl);
    abstract public void load(GL10 gl);
    abstract public void tick(long elapsed);
}

class AsteroidCommandWorld extends World {
    private static final String TAG = "AsteroidCommandWorld";

    public TileGroup   statics     = new TileGroup();
    public SpriteGroup asteroids   = new SpriteGroup();
    public SpriteGroup projectiles = new SpriteGroup();
    public SpriteGroup stations    = new SpriteGroup();

    public Tile ground;
    public Tile sky;

    public int num_asteroids = 50;

    public AsteroidCommandWorld() {
        sky = new BackgroundTile(this);
        statics.add(sky);

        ground = new GroundTile(this);
        statics.add(ground);
        
        for(int i=0; i<num_asteroids; i++)
            spawnAsteroid();
    }

    public void draw(GL10 gl) {
        statics.draw(gl);
        asteroids.draw(gl);
        projectiles.draw(gl);
        stations.draw(gl);
    }

    public void load(GL10 gl) {
        statics.load(gl);
        asteroids.load(gl);
        projectiles.load(gl);
        stations.load(gl);
    }

    CollisionListener AsteroidCollisionListener = new CollisionListener() {
            private static final String TAG = "AsteroidCollisionListener";

            public void onCollision(Tile a, Tile b) {
                Sprite sa = (Sprite)a;
                sa.velocity.scale(-1f);

                Sprite sb = (Sprite)b;
                sb.velocity.scale(-1f);
            }                
        };

    CollisionListener ProjectileCollisionListener = new CollisionListener() {
            private static final String TAG = "ProjectileCollisionListener";

            public void onCollision(Tile a, Tile b) {
                Log.d(TAG, "projectile collision: " + a + ", " + b);
                               
                Sprite projectile = (Sprite)a;
                Sprite asteroid = (Sprite)b;
                               
                int num_fragments = RandomGenerator.randomInt(2, 4);

                for(int i=0; i<num_fragments; i++) {

                    AsteroidSprite fragment = new AsteroidSprite(AsteroidCommandWorld.this);

                    fragment.scale.x = asteroid.scale.x / num_fragments;
                    fragment.scale.y = asteroid.scale.y / num_fragments;
                    fragment.scale.z = asteroid.scale.z / num_fragments;
        
                    fragment.position.x = asteroid.position.x;
                    fragment.position.y = asteroid.position.y;
                    fragment.position.z = asteroid.position.z;

                    fragment.velocity.x = asteroid.velocity.x + 
                        RandomGenerator.randomRange(-.25f, .25f);

                    fragment.velocity.y = asteroid.velocity.y + 
                        RandomGenerator.randomRange(-.25f, .25f);

                    fragment.velocity.z = asteroid.velocity.z + 
                        RandomGenerator.randomRange(-.25f, .25f);
        
                    fragment.rotation.x = RandomGenerator.randomRange(0, 360f);
                    fragment.rotation.y = RandomGenerator.randomRange(0, 360f);
                    fragment.rotation.z = RandomGenerator.randomRange(0, 360f);
        
                    fragment.angular_velocity.x = RandomGenerator.randomRange(0, 2f);
                    fragment.angular_velocity.y = RandomGenerator.randomRange(0, 2f);
                    fragment.angular_velocity.z = RandomGenerator.randomRange(0, 2f);
                    
                    asteroids.spawn(fragment);
                }

                projectile.die();
                asteroid.die();
            }
        };
    
    public void tick(long elapsed) {
        asteroids.tick(elapsed);
        projectiles.tick(elapsed);
        stations.tick(elapsed);

        asteroids.collide(asteroids, AsteroidCollisionListener);        
        projectiles.collide(asteroids, ProjectileCollisionListener);

        rangeFilter(asteroids, 200f);
        rangeFilter(projectiles, 200f);

        asteroids.reap();
        projectiles.reap();
        stations.reap();

        while(asteroids.size() < num_asteroids)
            spawnAsteroid();
    }

    protected int rangeFilter(TileGroup group, float range) {
        int removed = 0;

        ArrayList<Tile> tiles = group.getTiles();    
        Iterator<Tile> iter = tiles.iterator();

        while(iter.hasNext()) {
            Sprite t = (Sprite)iter.next();
            
            if(t.position.magnitude() > range)
                t.die();
        }

        return removed;
    }

    protected void spawnAsteroid() {
        AsteroidSprite a = new AsteroidSprite(this);

        a.scale.x = RandomGenerator.randomRange(.5f, 1.5f);
        a.scale.y = RandomGenerator.randomRange(.5f, 1.5f);
        a.scale.z = RandomGenerator.randomRange(.5f, 1.5f);

        a.position.x = RandomGenerator.randomRange(-25f, 25f);
        a.position.y = -25f + RandomGenerator.randomRange(-5f, 5f);
        a.position.z = -25f + RandomGenerator.randomRange(-5f, 5f);
            
        a.velocity.x = RandomGenerator.randomRange(-.1f, .1f);
        a.velocity.y = RandomGenerator.randomRange(.05f, .1f);
        a.velocity.z = RandomGenerator.randomRange(-.1f, -.3f);

        a.rotation.x = RandomGenerator.randomRange(0, 360f);
        a.rotation.y = RandomGenerator.randomRange(0, 360f);
        a.rotation.z = RandomGenerator.randomRange(0, 360f);

        a.angular_velocity.x = RandomGenerator.randomRange(0, 2f);
        a.angular_velocity.y = RandomGenerator.randomRange(0, 2f);
        a.angular_velocity.z = RandomGenerator.randomRange(0, 2f);

        asteroids.spawn(a);
    }

    public void fireAt(Vector3 p) {
        Log.d(TAG, "Firing at " + p);

        RocketSprite r = new RocketSprite(this, p);
        projectiles.spawn(r);
    }
}

class AsteroidSprite extends Sprite {

    private static final String MODEL_KEY = "asteroid";

    public AsteroidSprite(World world) {
        super(world);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class RocketSprite extends Sprite {

    private static final String MODEL_KEY = "rocket";

    public RocketSprite(World world, Vector3 target) {
        super(world);

        position = new Vector3(0f, -10f, 0f);

        target.normalize();

        float rotation_x = -90f;//(float)Math.toDegrees(Math.atan2(target.y, target.z));
        float rotation_y = (float)Math.toDegrees(Math.atan2(target.x, target.z));
        
        rotation = new Vector3(rotation_x, rotation_y, 0);

        velocity = new Vector3(target);
        velocity.scale(.02f);

        acceleration = new Vector3(velocity);
    };

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }

    public void load(GL10 gl) {
        super.load(gl);
        smoke_trail.load(gl);
    }

    public void draw(GL10 gl) {
        super.draw(gl);
        smoke_trail.draw(gl);
    }

    public void tick(long elapsed) {
        super.tick(elapsed);
        smoke_trail.tick(elapsed);
    }

    protected ParticleEmitter smoke_trail = new ParticleEmitter(50) {

            private final static String TEXTURE = "smoke";

            protected Bitmap getTextureBitmap() {
                return TextureLoader.get(TEXTURE);
            }
        
            public void spawnParticle(Particle p) {
                p.position.copy(position);
                p.velocity.copy(velocity);
                p.acceleration.copy(acceleration);

                p.ttl = RandomGenerator.randomInt(60, 180);
                p.scale = RandomGenerator.randomRange(1f, 2f);
            }

            public void tickParticle(Particle p, long elapsed) {
                super.tickParticle(p, elapsed);
                    
                float percentile = (float)p.age/(float)p.ttl;
                
                p.color.a = 1f - percentile;
                p.size = p.scale * percentile;
            }
        };
}

class Particle {
    public Vector3 position;
    public Vector3 velocity;
    public Vector3 acceleration;
    public Color color;
    public float size;
    public float scale;
    public int ttl;
    public int age;
}

abstract class ParticleEmitter implements Drawable, Tickable {

    protected Particle[] particles;

    protected FloatBuffer vertices;
    protected FloatBuffer sizes;

    protected int texture_id;

    public ParticleEmitter(int num_particles) {

        particles = new Particle[num_particles];

        vertices = GLHelper.floatBuffer(num_particles*3);
        sizes = GLHelper.floatBuffer(num_particles);

        for(int i=0; i<particles.length; i++) {
            particles[i] = new Particle();
            spawnParticle(particles[i]);
        }
    }

    abstract protected Bitmap getTextureBitmap();
    abstract public void spawnParticle(Particle p);
    
    protected void tickParticle(Particle p, long elapsed) {
        p.age++;
        p.velocity.add(p.acceleration);
        p.position.add(p.velocity);
    }

    public void tick(long elapsed) {
        vertices.clear();
        sizes.clear();

        for(int i=0; i<particles.length; i++) {
            if(particles[i].age > particles[i].ttl)
                spawnParticle(particles[i]);

            tickParticle(particles[i], elapsed);

            vertices.put(particles[i].position.x);
            vertices.put(particles[i].position.y);
            vertices.put(particles[i].position.z);
        }

        vertices.position(0);
        sizes.position(0);
    }

    public void load(GL10 gl) {
        gl.glEnable(GL11.GL_POINT_SPRITE_OES);
        texture_id = GLHelper.loadTexture(gl, getTextureBitmap());
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture_id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, particles.length);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
    }
}

class BackgroundTile extends Tile {
    private static final String MODEL_KEY = "background";

    public BackgroundTile(World world) {
        super(world);
        scale = new Vector3(900f, 900f, 900f);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class GroundTile extends Tile {
    private static final String TAG = "GroundTile";
    private static final String MODEL_KEY = "ground";

    public GroundTile(World world) {
        super(world);

        position = new Vector3(0f, -35f, 0f);
        scale    = new Vector3(25f, 25f, 25f);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }

    public boolean contains(Tile o) {
        double d = Math.sqrt(  Math.pow(o.position.x - position.x, 2) + 
                               Math.pow(o.position.y - position.y, 2) + 
                               Math.pow(o.position.z - position.z, 2) );

        return d <= 25f;
    }
}


