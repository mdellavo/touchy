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

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

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
    private TouchyGLSurfaceView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        view = new TouchyGLSurfaceView(this);

        ObjLoader.init(this);
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

        // BackgroundTile b = new BackgroundTile(world);        
        // world.statics.add(b);

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
        world.load(gl);
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
        eye = new Vector3(25, 0, 25);
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
        this.uvs = loadVertices(uvs);
        this.normals = loadVertices(normals);

        this.texture = texture;

        color = new float[4];
        color[0] = 1.0f;
        color[1] = 1.0f;
        color[2] = 1.0f;
        color[3] = 1.0f;
    }

    protected FloatBuffer loadVertices(Vector<Vector3> vertices) {
        num_vertices = vertices.size()*3;
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(num_vertices*4);
        byte_buffer.order(ByteOrder.nativeOrder());

        FloatBuffer rv = byte_buffer.asFloatBuffer();

        for(Vector3 vertex : vertices) {
            Log.d(TAG, "Loading vertex" + vertex);

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
        Log.d(TAG, "parts: " + java.util.Arrays.deepToString(parts));

        

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


                    Log.d(TAG, "Face: " + face);


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

        gl.glRotatef(rotation.x, 1f, 0, 0);
        gl.glRotatef(rotation.y, 0, 1f, 0);
        gl.glRotatef(rotation.z, 0, 0, 1f);    
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);

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
    abstract public void load(GL10 gl);
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
    private static final String MODEL_KEY = "background";

    public BackgroundTile(World world) {
        super(world);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class GroundTile extends Tile {
    private static final String MODEL_KEY = "ground";

    public GroundTile(World world) {
        super(world);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

