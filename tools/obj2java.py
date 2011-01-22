import sys
from pprint import pprint

point = lambda parts: [float(i) for i in parts]
components = lambda part: [int(i) if i else None for i in part.split('/')]
face = lambda parts: zip(*(components(part) for part in parts))
face_point = lambda verts, indexes: [verts[i-1] if i else None for i in indexes]

def parse(lines):

    v  = []
    vt = []
    vn = []

    vertices = []
    textures = []
    normals  = []

    for line in lines:

        if line[0] == '#': continue

        parts = line.split()
        
        if parts[0] == 'v'    : v.append(point(parts[1:]))
        elif parts[0] == 'vt' : vt.append(point(parts[1:]))
        elif parts[0] == 'vn' : vn.append(point(parts[1:]))
        elif parts[0] == 'f':
            for rv, verts, indexes in zip( (vertices, textures, normals),
                                           (v, vt, vn), 
                                           face(parts[1:]) ):
                rv.append(face_point(verts, indexes))

    return vertices, textures, normals

def render_array(name, vertices):
    
    parts = [  "\tfloat %s[] = {" % name ]
   
    for i, face_points in enumerate(vertices):
        parts.append('\t\t// Face %d' % (i+1))

        for verts in face_points:            
            row = "\t\t" + "\t, ".join(['%.06ff' % i for i in verts]) + ',' 
            parts.append(row)

    parts.append('\t};')

    return "\n".join(parts)

def render(src, vertices, textures, normals):

    if src.endswith('.obj'):
        src = src[:-4]

    parts = [ '// Source: ' + src, 
              'class %s {' % src.title() ]
    
    for name, faces in zip( ('vertices', 'textures', 'normals'),
                            (vertices, textures, normals) ):

        if faces[0][0] is not None:
            parts.append(render_array(name, faces))            
            parts.append('')

    parts.append('};')
             
    return "\n".join(parts)

def main(*args):    
    for arg in args:
        with open(arg) as f:
            print render(arg, *parse(f))

    return 0

if __name__ == '__main__':
    sys.exit(main(*sys.argv[1:]))
