package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class gameObject {
    protected String name;

    protected float[] vertexBuffer;
    protected float[] uvBuffer;
    protected float[] normalBuffer;

    protected int[] indexBuffer;

    protected int texID;
    protected String texPath;

    protected Vector3f r;
    protected Vector3f v;
    protected Vector3f a;
    protected float m;

    protected Quaternionf rotation;
    protected Quaternionf rotationVelocity;
    protected Quaternionf rotationAcceleration;// related to thrust

    public gameObject(String modelName, String textureName) {
        this.name = modelName + ":" + textureName;

        modelLoader ml = new modelLoader("resources/objects/" + modelName + ".obj");

        vertexBuffer = ml.getVertexBuffer();
        uvBuffer = ml.getUVBuffer();
        normalBuffer = ml.getNormalBuffer();
        indexBuffer = ml.getIndexBuffer();

        texPath = "resources/textures/" + textureName + ".png";
        texID = loadTexture(texPath);

        r = new Vector3f();
        v = new Vector3f();
        a = new Vector3f();
        m = 0.0f;

        rotation = new Quaternionf(0, 0, 0, 1);
    }

    public void nextFrame(float dt) {
        Vector3f accumulator = new Vector3f();
        // a = F/m;

        // r = r_0 + ut + (at^2)/2
        v.mul(dt, accumulator);
        r.add(accumulator);
        a.mul(0.5f * dt * dt, accumulator);
        r.add(accumulator);

        // simple collision detection, objects cannot go below 0
        if (r.y < 0) {
            r.set(r.x, 0, r.z);
        }

        // v = u + at
        a.mul(dt, accumulator);
        v.add(accumulator);

        // todo: add rotational physics
    }

    protected static int loadTexture(String path) {
        int textureID;

        textureLoader tl = new textureLoader(path);

        textureID = glGenTextures();

        glEnable(GL_TEXTURE_2D);

        // "Bind" the newly created texture : all future texture functions will modify this texture
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Give the image to OpenGL
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, tl.getWidth(), tl.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, tl.buffer());

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        return textureID;
    }

    public Matrix4f modelMatrix() {
        Matrix4f result = new Matrix4f();
        Matrix4f translation = new Matrix4f(
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                r.x, r.y, r.z, 0
        );
        rotation.get(result);
        result.add(translation);
        return result;
    }

    public float[] getVertexBuffer() { return vertexBuffer; }

    public float[] getUVBuffer() { return uvBuffer; }

    public float[] getNormalBuffer() { return normalBuffer; }

    public int[] getIndexBuffer() { return indexBuffer; }

    public int getTexID() { return texID; }

    public String getTexPath() { return texPath; }

    // Getters and setters for physics values

    public Vector3f position() { return r; }

    public Vector3f velocity() { return v; }

    public Vector3f acceleration() { return a; }

    public float mass() { return m; }

    public void setPosition(Vector3f value) { r = value; }

    public void setVelocity(Vector3f value) { v = value; }

    public void setAcceleration(Vector3f value) { a = value; }

    public void setMass(float value) { m = value; }
}