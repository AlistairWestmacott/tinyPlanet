package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class gameObject {
    protected String name;

    protected float[] vertexBuffer;
    protected float[] uvBuffer;
    protected float[] normalBuffer;

    protected int[] indexBuffer;

    protected int texID;

    protected Matrix4f modelMatrix;

    protected Vector3f r;
    protected Vector3f v;
    protected Vector3f a;
    protected float m;

    public gameObject(String name) {
        this.name = name;

        modelLoader ml = new modelLoader("resources/" + name + ".obj");

        vertexBuffer = ml.getVertexBuffer();
        uvBuffer = ml.getUVBuffer();
        normalBuffer = ml.getNormalBuffer();
        indexBuffer = ml.getIndexBuffer();

        texID = loadTexture("resources/" + name + ".png");

        r = new Vector3f();
        v = new Vector3f();
        a = new Vector3f();
        m = 0.0f;
    }

    public void nextFrame(float dt) {
        Vector3f accumulator = new Vector3f();
        // a = F/m;

        // r = r_0 + ut + (at^2)/2
        v.mul(dt, accumulator);
        r.add(accumulator);
        a.mul(0.5f * dt * dt, accumulator);
        r.add(accumulator);

        if (r.y < 0) {
            r.set(r.x, 0, r.z);
        }

        // v = u + at
        a.mul(dt, accumulator);
        v.add(accumulator);
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

    public float[] getVertexBuffer() { return vertexBuffer; }

    public float[] getUVBuffer() { return uvBuffer; }

    public float[] getNormalBuffer() { return normalBuffer; }

    public int[] getIndexBuffer() { return indexBuffer; }

    public int getTexID() { return texID; }

    public Matrix4f getModelMatrix() { return modelMatrix; }

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