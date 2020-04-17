package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class gameUniverse {

    private int mainObjectIndex;
    private List<gameObject> worldObjects;

    private int numOfObjects;
    private int modelToProcess = -1;

    private Vector3f lightSource;

    public gameUniverse() {
        worldObjects = new ArrayList<gameObject>();
        lightSource = new Vector3f();
    }

    public void nextFrame(float dt) {
        for (gameObject o : worldObjects) {
            o.nextFrame(dt);
        }
    }

    public int addObject(String modelName, String textureName, boolean setMainObject) {
        worldObjects.add( new gameObject(modelName, textureName) );

        numOfObjects++;

        int index = worldObjects.size() - 1;
        if (setMainObject) {
            setMainObject(index);
        }
        return index;
    }

    public int addObject(gameObject o, boolean setMainObject) {
        worldObjects.add(o);

        numOfObjects++;

        int index = worldObjects.size() - 1;
        if (setMainObject) {
            setMainObject(index);
        }
        return index;
    }

    public void setMainObject(int i) {
        mainObjectIndex = i;
    }

    public gameObject getMainObject() {
        return worldObjects.get(mainObjectIndex);
    }

    private int getMainObjectIndex() {
        return mainObjectIndex;
    }

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
}
