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

import java.util.Arrays;
import java.util.Vector;
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
                }
                );
        }
        
        return true;
    }
}

class TouchyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TouchyRenderer";

    protected World world;
    protected Camera camera;

    protected int frames;
    protected long last;

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

        frames++;        

        camera.tick();
        world.tick();

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        camera.update(gl);
 
        matrix_grabber.getCurrentState(gl);

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
        eye = new Vector3(0, 0, 25);
        center = new Vector3(0, 0, 0);
        up = new Vector3(0f, 1f, 0f);    
        rotation = new Vector3(0, 0, 0);
        fov = 45f;
        znear = .1f;
        zfar = 100f;
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

    public void tick() {
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

class Model {
    private static final String TAG = "Model";
    
    protected int num_vertices;
    protected FloatBuffer vertices;
    protected FloatBuffer uvs;
    protected FloatBuffer normals;
    protected Bitmap texture;
    protected int texture_id;
    protected float[] color;

    public Model(Vector<Vector3> vertices, Vector<Vector3> uvs,
                 Vector<Vector3> normals, Bitmap texture) {

        this.vertices = loadVertices(vertices);
        this.uvs = loadUVs(uvs);
        this.normals = loadVertices(normals);

        this.texture = texture;

        color = new float[4];
        color[0] = 1.0f;
        color[1] = 1.0f;
        color[2] = 1.0f;
        color[3] = 1.0f;
    }

    protected FloatBuffer loadUVs(Vector<Vector3> vertices) {
        num_vertices = vertices.size()*2;
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(num_vertices*4);
        byte_buffer.order(ByteOrder.nativeOrder());

        FloatBuffer rv = byte_buffer.asFloatBuffer();

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
        }

        rv.position(0);

        return rv;
    }

    protected FloatBuffer loadVertices(Vector<Vector3> vertices) {
        num_vertices = vertices.size()*3;
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(num_vertices*4);
        byte_buffer.order(ByteOrder.nativeOrder());

        FloatBuffer rv = byte_buffer.asFloatBuffer();

        for(Vector3 vertex : vertices) {
            rv.put(vertex.x);
            rv.put(vertex.y);
            rv.put(vertex.z);
        }

        rv.position(0);

        return rv;
    }

    public void loadTexture(GL10 gl) {
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
        
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, texture, 0);
        Log.d(TAG, "Loaded Texture ID: " + texture_id);        
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);

        gl.glColor4f(color[0], color[1], color[2], color[3]);

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

    protected static Vector3[][] parseFace( Vector<Vector3> v, 
                                            Vector<Vector3> vt, 
                                            Vector<Vector3> vn, 
                                            String[] parts ) {

        Vector[] tables = {v, vt, vn};

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
        
        Vector<Vector3> v = new Vector<Vector3>();
        Vector<Vector3> vt = new Vector<Vector3>();
        Vector<Vector3> vn = new Vector<Vector3>();

        Vector<Vector3> vertices = new Vector<Vector3>();
        Vector<Vector3> uvs = new Vector<Vector3>();
        Vector<Vector3> normals = new Vector<Vector3>();

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
                    Vector vectors[] = {vertices, uvs, normals};

                    for(int i=0; i<vectors.length; i++)
                        for(int j=0; j<face[i].length; j++)
                            vectors[i].add(face[i][j]);
                }
            }
            
            Bitmap texture = TextureLoader.get(key);
            rv = new Model(vertices, uvs, normals, texture);

        } catch(IOException e) {
            Log.d(TAG, "error loading model: " + e);
        }

        return rv;
    }
}

interface Drawable {
    void draw(GL10 gl);
    void load(GL10 gl);
}

interface Tickable {
    void tick();
}

class Vector2 {
    public float x, y;

    public Vector2() {}
    
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(Vector2 v) {
        x = v.x;
        y = v.y; 
    }

    public float magnitude() {
        return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public void normalize() { 
        float mag = magnitude();
        x /= mag;
        y /= mag;
    }

    public void scale(float factor) {
        x /= factor;
        y /= factor;
    }

    public String toString() {
        return "Vector2(" + this.x + ", " + this.y + ")";
    }
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
        x /= factor;
        y /= factor;
        z /= factor;
    }

    public String toString() {
        return "Vector3(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}

abstract class Tile implements Drawable {
    private static final String TAG = "Tile";

    protected World world;
    protected Model model;

    protected Vector3 position;
    protected Vector3 rotation;
    protected Vector3 scale;
    protected Vector3 bounds;

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

    public void load(GL10 gl) {
        for(Tile t : tiles)
            t.load(gl);
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

    // FIXME proper math funcions in Vector3
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

    public Vector<Sprite> getSprites() {
        return (Vector<Sprite>)tiles;
    }
}

abstract class World implements Drawable, Tickable {
    abstract public void draw(GL10 gl);
    abstract public void load(GL10 gl);
    abstract public void tick();

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
        
        Random random = new Random();
        for(int i=0; i<num_asteroids; i++) {
            AsteroidSprite a = new AsteroidSprite(this);

            a.scale.x = random.nextFloat();
            a.scale.y = random.nextFloat();
            a.scale.z = random.nextFloat();

            a.position.x = (random.nextFloat() * 50.0f) - 25f;
            a.position.y = 0;
            a.position.z = (random.nextFloat() * 50.0f) - 25f;
            
            a.velocity.x = random.nextFloat() * .01f;
            a.velocity.y = -.08f;
            a.velocity.z = random.nextFloat() * .01f;

            a.rotation.x = random.nextFloat() * 360.0f;
            a.rotation.y = random.nextFloat() * 360.0f;
            a.rotation.z = random.nextFloat() * 360.0f;            

            a.angular_velocity.x = random.nextFloat();
            a.angular_velocity.y = random.nextFloat();
            a.angular_velocity.z = random.nextFloat();

            asteroids.add(a);
        }
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

        for(Tile t: asteroids.getTiles()) {
            if(ground.contains(t)) {

                Log.d(TAG, "ground bounce");

                Sprite s = (Sprite)t;
                s.velocity.x *= -1;
                s.velocity.y *= -1;
                s.velocity.z *= -1;                
            }
        }


        for(Tile t: asteroids.getTiles()) {
            if(sky.contains(t)) {
                Log.d(TAG, "sky bounce");

                Sprite s = (Sprite)t;
                s.velocity.x *= -1;
                s.velocity.y *= -1;
                s.velocity.z *= -1;                
            }
        }

        for(Sprite s: projectiles.getSprites()) {
            if(s.magnitude() > 50f)
                projectiles.remove(s);
        }       
    }

    public void fireAt(Vector3 p) {
        Log.d(TAG, "Firing at " + p);

        RocketSprite r = new RocketSprite(this, p);
        projectiles.add(r);
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
        
        velocity = new Vector3(target);
        velocity.normalize();
        velocity.scale(.001f);

        acceleration = new Vector3(velocity);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class BackgroundTile extends Tile {
    private static final String MODEL_KEY = "background";

    public BackgroundTile(World world) {
        super(world);
        scale = new Vector3(50f, 50f, 50f);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }

    public boolean contains(Tile o) {
        double d = Math.sqrt(  Math.pow(o.position.x, 2) + 
                               Math.pow(o.position.y, 2) + 
                               Math.pow(o.position.z, 2) );
        return d >= 50f;
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

