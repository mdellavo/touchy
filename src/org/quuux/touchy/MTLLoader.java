package org.quuux.touchy;

import android.content.res.AssetManager;
import android.content.Context;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class MTLLoader {
    private static final String TAG = "MTLLoader";

    private static Context context;
    private static Map<String, Material> cache;
    
    public static void init(Context context) {
        MTLLoader.context = context;
        MTLLoader.cache   = new HashMap<String, Material>();
    }

    public static Material get(String key) {
        return MTLLoader.cache.get(key);
    }

    protected static BufferedReader openMaterial(String key) {       
        AssetManager asset_manager = context.getAssets();

        BufferedReader rv = null;

        try {
            String material_path = "materials/" + key + ".mtl";

            Log.d(TAG, "Loading material: " + material_path);

            InputStream in = asset_manager.open(material_path);
            InputStreamReader ins = new InputStreamReader(in);
            rv = new BufferedReader(ins);
        } catch(IOException e) {
            Log.d(TAG, "Could not open material: " + key);
        }

        return rv;
    }

    protected static Color parseColor(String[] parts) {
        return new Color( Float.parseFloat(parts[2]), 
                          Float.parseFloat(parts[3]), 
                          Float.parseFloat(parts[4]), 1f );
    }

    protected static Material createMaterial(String name) {
        Material rv = new Material(name);
        cache.put(name, rv);
        return rv;
    }

    protected static boolean loadMaterial(String key) {
        BufferedReader in = openMaterial(key);

        if(in == null)
            return false;
        
        Material material = null;

        try {
            String line;
        
            while((line = in.readLine()) != null) {
            
                String[] parts = line.split("\\s");

                if(parts.length == 0 || parts[0] == "#")
                    continue;
                else if(parts[0] == "newmtl")
                    material = createMaterial(parts[1]);
                else if(parts[0] == "Ka")
                    material.ambient = parseColor(parts);
                else if(parts[0] == "Kd")
                    material.diffuse = parseColor(parts);
                else if(parts[0] == "Ks")
                    material.specular = parseColor(parts);
                else if(parts[0] == "Tr" || parts[0] == "d")
                    material.setAlpha(Float.parseFloat(parts[1]));
                else if(parts[0] == "Ns")
                    material.shininess = Float.parseFloat(parts[1]); 
                else if(parts[0] == "illum")
                    material.illumination = Integer.parseInt(parts[1]);
                else if(parts[0] == "map_Ka")
                    material.texture = TextureLoader.get(parts[1]);
            }

        } catch(IOException e) {
            Log.d(TAG, "error loading material: " + e);
        }

        return true;
    }
}
