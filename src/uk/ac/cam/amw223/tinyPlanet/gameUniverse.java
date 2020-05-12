package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class gameUniverse {

    private List<gameObject> worldObjects;

    private camera cam;

    private int numOfObjects;
    private int modelToProcess = -1;

    private Vector3f lightSource;

    private static final float G = 6.67E-11f;

    public gameUniverse() {
        worldObjects = new ArrayList<>();
        lightSource = new Vector3f();
    }

    public void init() {
        cam = new camera();
        cam.setUniverse(this);
    }

    public void nextFrame(float dt) {
        for (gameObject o : worldObjects) {
            o.nextFrame(dt);
        }
        cam.nextFrame(dt);
    }

    public int addObject(String modelName, String textureName, boolean setMainObject) {
        gameObject o = new gameObject(modelName, textureName);

        return addObject(o, setMainObject);
    }

    public int addObject(gameObject o, boolean setMainObject) {
        worldObjects.add(o);
        o.setUniverse(this);

        numOfObjects++;

        int index = worldObjects.size() - 1;
        if (setMainObject) {
            cam.attach(o);
        }
        return index;
    }

//    public gameObject getMainObject() { return worldObjects.get(mainObjectIndex); }

    public boolean nextObject() {
        // returns false when no more objects to process
        if (numOfObjects == 0) {
            return false;
        }

        if (modelToProcess == numOfObjects) {
            modelToProcess = 0;
        } else {
            modelToProcess++;
        }

        if (modelToProcess == numOfObjects) {
            // reset counter
            modelToProcess = -1;
            return false;
        } else {
            return true;
        }


        // start at zero
    }

    public Matrix4f currentModel() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).modelMatrix();
    }

    public String currentTexPath() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).getTexPath();
    }

    public float[] currentVertexBuffer() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).getVertexBuffer();
    }

    public float[] currentUVBuffer() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).getUVBuffer();
    }

    public float[] currentNormalBuffer() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).getNormalBuffer();
    }

    public int[] currentIndexBuffer() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).getIndexBuffer();
    }

    public String currentObjectName() {
        if (worldObjects.isEmpty()) throw new IndexOutOfBoundsException("No game objects in universe.");
        return worldObjects.get(modelToProcess).name;
    }

    public Vector3f getLightSource() { return lightSource; }

    public void setLightSource(Vector3f value) { lightSource = value; }

    public camera getCam() {
        return cam;
    }

    public Vector3f getGravityAtPoint(gameObject thisObj) {
        Vector3f result = new Vector3f();
        Vector3f direction = new Vector3f();
        float r;
        for (gameObject o : worldObjects) {
            if (o != thisObj) {
                o.position().sub(thisObj.position(), direction);
                r = direction.length();
                if (r > 0.0001) {// for small r, ignore gravity
                    direction.normalize();
                    direction.mul(o.mass() * G / (r * r));
                    result.add(direction);
                }
            }
        }
        return result;
    }
}
