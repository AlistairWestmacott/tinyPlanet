package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class camera extends gameObject {

    private gameObject attached;

    private mode m;

    private float zoom = 10;// todo: add controls for zoom

    public camera() {
        // todo: make model and textures for camera
        super("magic-cube", "checkerboard");
        m = mode.FREE;
        attached = null;
    }

    @Override
    public void nextFrame(float dt) {
        super.nextFrame(dt);
//        freeLookRotationMatrix.mul(rotation);
        if (m == mode.ATTACHED) {
            r = new Vector3f(attached.position());
            r.add(new Vector3f(0, zoom, -zoom));
        } else if (m == mode.POLE) {
            // camera Location Homogeneous
            Vector4f cLH = new Vector4f(0, zoom, -zoom, 1);
            cLH.mulProject(attached.modelMatrix());
            r = new Vector3f(cLH.x, cLH.y, cLH.z);
        } else if (m == mode.FIRST_PERSON) {
            r = attached.position();
        }
    }

    public Matrix4f viewMatrix() {

        Vector3f facing, up;

        // FACING

        if (m == mode.FREE) {
            facing = new Vector3f(0, 0, 1);
            facing.mul(rotation);
            facing.add(r);
        } else if (m == mode.FIRST_PERSON) {
            facing = new Vector3f(0, 0, 1);
            facing.mul(attached.getRotation());
            facing.add(r);
        } else {
            facing = attached.position();
        }

        // UP

        up = new Vector3f(0, 1, 0);
        if (m == mode.POLE || m == mode.FIRST_PERSON) {
            up.mul(attached.getRotation());
        }

        return new Matrix4f().lookAt(
                r,
                facing,
                up
        );
    }

    public void attach(gameObject o) {
        attached = o;
    }

    public void cycleMode() {
        m = m.next();
        if (m == mode.FREE) {
            r = new Vector3f();
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float value) {
        zoom = value;
    }

    public void zoomIn() {
        zoom--;
    }

    public void zoomOut() {
        zoom++;
    }

    enum mode {
        FREE(0),
        ATTACHED(1),
        POLE(2),
        FIRST_PERSON(3);

        static Map<Integer, mode> values = new HashMap<>();
        static int types;

        static {
            for (mode m : mode.values()) {
                values.put(m.type, m);
            }
            types = values.size();
        }

        int type;

        mode(int v) {
            type = v;
        }

        public static mode of(int i) {
            return values.get(i);
        }

        public mode next() {
            return values.get((type + 1) % types);
        }

        int getType() {
            return type;
        }
    }

}
