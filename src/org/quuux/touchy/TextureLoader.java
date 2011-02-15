package org.quuux.touchy;

import android.content.res.AssetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

public class TextureLoader {
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
