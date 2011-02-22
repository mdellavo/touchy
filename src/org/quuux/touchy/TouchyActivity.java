package org.quuux.touchy;

import android.app.Activity;

import android.os.Bundle;
import android.os.Build;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import android.content.Context;

import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.OnDoubleTapListener;

import android.graphics.Bitmap;

import android.util.Log;
import android.util.DisplayMetrics;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;

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

class TouchyGLSurfaceView extends GLSurfaceView 
    implements OnDoubleTapListener, OnGestureListener {

    protected static final String TAG = "TouchyGLSurfaceView";

    protected TouchyRenderer renderer;
    protected GestureDetector gesture_detector;

    public TouchyGLSurfaceView(Context context, TouchyRenderer renderer) {
        super(context);

        this.renderer = renderer;
        this.setRenderer(renderer);

        gesture_detector = new GestureDetector(context, this);
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

        return new Vector3( touch_position[0],
                            touch_position[1],
                            touch_position[2] );
    }

    public boolean onTouchEvent(final MotionEvent e) {
        gesture_detector.onTouchEvent(e);
        return true;
    }

    public boolean onDown(MotionEvent e) {
        Log.d(TAG, "down");
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, 
                           float velocityX, float velocityY) {
        Log.d(TAG, "fling");
        return true;
    }

    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "long press");
    }

    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            float distanceX, float distanceY) {
        queueEvent(new Runnable() {
                public void run() {
                    Vector3 vec = projectTouchToWorld(e2.getX(), e2.getY());
                    ((AsteroidCommandWorld)renderer.getWorld()).fireBeamAt(vec);
                }
            });

        return true;
    }

    public void onShowPress(MotionEvent e) {        
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    public boolean onSingleTapConfirmed(final MotionEvent e) {
        queueEvent(new Runnable() {
                public void run() {
                    Vector3 vec = projectTouchToWorld(e.getX(), e.getY());
                    ((AsteroidCommandWorld)renderer.getWorld()).fireBeamAt(vec);
                }
            });

        return true;
    }

    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    public boolean onDoubleTap(final MotionEvent e) {
        queueEvent(new Runnable() {
                public void run() {
                    Vector3 vec = projectTouchToWorld(e.getX(), e.getY());
                    ((AsteroidCommandWorld)renderer.getWorld()).fireRocketAt(vec);
                }
            });

        return true;
    }
}

class TouchyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TouchyRenderer";

    protected World world;
    protected Camera camera;

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

        GLHelper.init(gl);

        Log.d(TAG, "Vendor: " + GLHelper.getVendor());
        Log.d(TAG, "Renderer: " + GLHelper.getRenderer());
        Log.d(TAG, "Version: " + GLHelper.getVersion());
        Log.d(TAG, "Extensions: " + GLHelper.getExtensions());

        gl.glClearColor(0f,0f,0f, 0.5f);

        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_LINEAR);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                     GL10.GL_MODULATE);

        gl.glEnable(GL10.GL_BLEND);
        gl.glEnable(GL10.GL_DEPTH_TEST);

        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
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

        //Log.d(TAG, "elapsed: " + elapsed);

        // FIXME add slow frame detection

        camera.tick(elapsed);
        world.tick(elapsed);

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        camera.update(gl);
 
        matrix_grabber.getCurrentState(gl);

        // draw sprites
        world.draw(gl);

        last = now;
    }
}

class AsteroidCommandWorld extends World {
    private static final String TAG = "AsteroidCommandWorld";

    public TileGroup   statics     = new TileGroup();
    public SpriteGroup asteroids   = new SpriteGroup();
    public SpriteGroup projectiles = new SpriteGroup();
    public SpriteGroup stations    = new SpriteGroup();

    // FIXME need to fix my groups
    public FPSSprite fps = new FPSSprite();
    public BeamEmitter beam_emitter;

    public Tile ground;
    public Tile sky;

    public int num_asteroids = 75;

    public AsteroidCommandWorld() {

        sky = new BackgroundTile();
        statics.add(sky);

        ground = new GroundTile();
        statics.add(ground);
        
        for(int i=0; i<num_asteroids; i++)
            spawnAsteroid();

        beam_emitter = new BeamEmitter(new Vector3(1f, 1f, -1f));
    }

    public void draw(GL10 gl) {
        statics.draw(gl);
        asteroids.draw(gl);
        projectiles.draw(gl);
        fps.draw(gl);
        stations.draw(gl);
        beam_emitter.draw(gl);
    }

    public void load(GL10 gl) {
        statics.load(gl);
        asteroids.load(gl);
        projectiles.load(gl);
        stations.load(gl);
        fps.load(gl);
        beam_emitter.load(gl);
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

                    AsteroidSprite fragment = new AsteroidSprite();

                    fragment.scale.x = asteroid.scale.x / num_fragments;
                    fragment.scale.y = asteroid.scale.y / num_fragments;
                    fragment.scale.z = asteroid.scale.z / num_fragments;

                    fragment.bounds.x = asteroid.bounds.x / num_fragments;
                    fragment.bounds.y = asteroid.bounds.y / num_fragments;
                    fragment.bounds.z = asteroid.bounds.z / num_fragments;
                    
                    fragment.position.copy(asteroid.position);

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
        fps.tick(elapsed);

        asteroids.tick(elapsed);
        projectiles.tick(elapsed);
        stations.tick(elapsed);
        beam_emitter.tick(elapsed);

        asteroids.collide(asteroids, AsteroidCollisionListener);        
        projectiles.collide(asteroids, ProjectileCollisionListener);

        rangeFilter(asteroids, 1000f);
        rangeFilter(projectiles, 1000f);

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
        AsteroidSprite a = new AsteroidSprite();

        a.scale.x = RandomGenerator.randomRange(.5f, 3f);
        a.scale.y = RandomGenerator.randomRange(.5f, 3f);
        a.scale.z = RandomGenerator.randomRange(.5f, 3f);
        
        a.bounds.copy(a.scale);

        a.position.x = RandomGenerator.randomRange(-45f, 45f);
        a.position.y = -25f + RandomGenerator.randomRange(-10f, 10f);
        a.position.z = -25f + RandomGenerator.randomRange(-10f, 10f);
            
        a.velocity.x = RandomGenerator.randomRange(-.2f, .2f);
        a.velocity.y = RandomGenerator.randomRange(.1f, .2f);
        a.velocity.z = RandomGenerator.randomRange(-.2f, -.6f);

        a.rotation.x = RandomGenerator.randomRange(0, 360f);
        a.rotation.y = RandomGenerator.randomRange(0, 360f);
        a.rotation.z = RandomGenerator.randomRange(0, 360f);

        a.angular_velocity.x = RandomGenerator.randomRange(0, 2f);
        a.angular_velocity.y = RandomGenerator.randomRange(0, 2f);
        a.angular_velocity.z = RandomGenerator.randomRange(0, 2f);

        asteroids.spawn(a);
    }

    public void fireRocketAt(Vector3 p) {
        RocketSprite r = new RocketSprite(p);
        projectiles.spawn(r);
    }

    public void fireBeamAt(Vector3 p) {
        beam_emitter.target.copy(p);
        beam_emitter.target.normalize();
    }
}

class FPSSprite extends TextTile implements Tickable {
    private static final String TAG = "FPSSprite";

    protected long total_elapsed;
    protected long frames;

    public FPSSprite() {
        super(128, 32);
    }

    public void tick(long elapsed) {
        total_elapsed += elapsed;
        frames++;
        
        if(total_elapsed > 1000) {
            setText("FPS: " + frames);
            
            total_elapsed = 0;
            frames = 0;
        }
    }
}

class AsteroidSprite extends Sprite {

    private static final String MODEL_KEY = "asteroid";

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class BeamEmitter extends ParticleEmitter {
    private static final String TAG = "BeamEmitter";

    private static final String TEXTURE_KEY = "smoke";

    protected Vector3 position;
    public Vector3 target;

    public BeamEmitter(Vector3 target) {
        super(500);
        position = new Vector3(0f, -10f, 0f);
        
        target.normalize();
        this.target = target;
    }

    protected Texture getTexture() {
        return TextureLoader.get(BeamEmitter.TEXTURE_KEY);
    }
                
    public void spawnParticle(Particle p) {
        p.position.copy(position);
        p.velocity.copy(target);
        p.velocity.scale(.5f);
        p.ttl = RandomGenerator.randomInt(1, 300);
        p.size = 16f;
        p.color.r = .8f;
        p.color.a = .6f;       
    }

    public void tickParticle(Particle p, long elapsed) {
        super.tickParticle(p, elapsed);

        float percentile = (float)p.age/(float)p.ttl;   
        p.color.a = .6f * (1f - percentile);
        p.size = 10f*(1f - percentile);
    }
}

class RocketSprite extends Sprite {

    private static final String MODEL_KEY = "rocket";
    private static final String SMOKE_TEXTURE_KEY = "smoke";

    protected ParticleEmitter smoke_trail;

    public RocketSprite(Vector3 target) {
        super();

        position = new Vector3(0f, -10f, 0f);

        target.normalize();

        float rotation_x = -90f;//(float)Math.toDegrees(Math.atan2(target.y, target.z));
        float rotation_y = (float)Math.toDegrees(Math.atan2(target.x, target.z));
        
        rotation = new Vector3(rotation_x, rotation_y, 0);

        velocity = new Vector3(target);
        velocity.scale(.5f);

        acceleration = new Vector3(velocity);

        smoke_trail = new ParticleEmitter(100) {
                private final static String TAG = "SmokeTrail";

                protected Texture getTexture() {
                    return TextureLoader.get(RocketSprite.SMOKE_TEXTURE_KEY);
                }
                
                public void spawnParticle(Particle p) {
                    p.position.copy(position);
                    p.position.z += 1;

                    p.velocity.copy(velocity);
                    p.velocity.scale(-.3f);
                    p.velocity.x += RandomGenerator.randomRange(-.1f, .1f);
                    p.velocity.y += RandomGenerator.randomRange(-.1f, .1f);

                    p.ttl = RandomGenerator.randomInt(1, 90);
                    p.scale = RandomGenerator.randomRange(32f, 128f);

                    p.color.a = .8f;
                }
                
                public void tickParticle(Particle p, long elapsed) {
                    super.tickParticle(p, elapsed);
                    
                    float percentile = (float)p.age/(float)p.ttl;
                    
                    p.color.a = .8f * (1f - percentile);
                    p.size = p.scale * percentile;
                }
            };       
    }

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
}


class BackgroundTile extends Tile {
    private static final String MODEL_KEY = "background";

    public BackgroundTile() {
        super();
        scale = new Vector3(900f, 900f, 900f);
    }

    protected Model getModel() {
        return ObjLoader.get(MODEL_KEY);
    }
}

class GroundTile extends Tile {
    private static final String TAG = "GroundTile";
    private static final String MODEL_KEY = "ground";

    public GroundTile() {
        super();
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

class SwarmBoid {
    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();
    public Vector3 acceleration = new Vector3();
    public Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
}

class SwarmSprite extends Sprite {
    private static final String TAG = "GroundTile";
    private static final String MODEL_KEY = "ground";
}