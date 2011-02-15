package org.quuux.touchy;

import android.content.res.AssetManager;
import android.content.Context;

import android.util.Log;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class ObjLoader {
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
            
            rv = new Model(vertices, uvs, normals, key);

        } catch(IOException e) {
            Log.d(TAG, "error loading model: " + e);
        }

        return rv;
    }
}
