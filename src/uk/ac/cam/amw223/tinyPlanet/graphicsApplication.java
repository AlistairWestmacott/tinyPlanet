package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.IOException;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class graphicsApplication {

    // The window handle
    private long window;
    private int programID;

    // locations for various uniforms in the vertex shader (N.B. not the location in world space)
    private int mvpLocation;
    private int mvpNormalLocation;
    private int mvLocation;
    private int cameraLocation;
    private int lightSourceLocation;
    private int shadeModeLocation;

    private int vao;
    private int vertexBuffer;
    private int uvBuffer;
    private int normalBuffer;
    private int indexBuffer;

    private Vector4f lightSource = new Vector4f();
    gameObject light;

    private Matrix4f projection;
    private Matrix4f mvp = new Matrix4f();

    double lastTime;
    double currentTime;

    int demo;

    private gameUniverse universe = new gameUniverse();

    int shadingMode;
    final int shadingModes = 2;

    public void run() {
        try {
            init();

            // todo: while (!finished) {finished = loop();}
            //  this will basically mean that loop only includes loop code and not initialisation code too
            loop();

            // Free the window callbacks and destroy the window
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);

            // Terminate GLFW and free the error callback
            glfwTerminate();
            glfwSetErrorCallback(null).free();

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void init() throws IOException {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // fix version number
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);


        // Create the window
        window = glfwCreateWindow(1920, 1080, "Tiny Planet", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        glfwShowWindow(window);

        GL.createCapabilities();

        programID = LoadShaders("resources/shaders/textureVertexShader.glsl", "resources/shaders/toonFragmentShader.glsl");

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        if (demo == 1) {
            glClearColor(0, 0, 0, 0);
        } else {
            glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
        }

        glUseProgram(programID);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(window, windowWidth, windowHeight);

        universe.init();
        universe.getCam().setAxisOfRotation(new Vector3f(0, 1, 0));

        // object for the light source
        light = new gameObject("smooth-sphere", "checkerboard");
        light.setRotationSpeed(-0.2);
        light.setPosition(new Vector3f(-10, 0, 0));
        universe.addObject(light, false);

        if (demo == 1) {
            /*

            Solar

            This demo is a simulation of the solar system.

            The problem is that all the real life values for distances and velocities and stuff are way too huge to be
            reasonable to simulate. All the units are in metres, but since metres are an arbitrary unit in real life,
            it might be quite easy to use a different arbitrary unit such as Astronomical Units that shrinks down the
            scale of the solar system to make it easier to simulate. Just need to remember to scale the constants too
            to keep the simulation accurate.

             */
            gameObject planet;
            float[] masses = new float[] {
                    0.330f,
                    4.87f,
                    5.97f,
                    0.073f,
                    0.642f,
                    1898f,
                    568f,
                    86.8f,
                    102f
            };
            float[] distanceFromSun = new float[] {
                    57.9f,
                    108.2f,
                    149.6f,
                    0.384f,
                    227.9f,
                    778.6f,
                    1433.5f,
                    2872.5f,
                    4495.1f,
            };
            float[] orbitalVelocity = new float[] {
                    47.4f,
                    35.0f,
                    29.8f,
                    1.0f,
                    24.1f,
                    13.1f,
                    9.7f,
                    6.8f,
                    5.4f
            };
            for (int i = 0; i < 9; i++) {
                planet = new gameObject("smooth-sphere", "checkerboard");
                planet.setPosition(new Vector3f(distanceFromSun[i] * 1E+9f, 0, 0));
                planet.setVelocity(new Vector3f(0, 0, orbitalVelocity[i] * 1E+3f));
                planet.setMass(masses[i] * 1E+24f);
                universe.addObject(planet, true);
            }
            universe.getCam().setZoom(1E+10f);
        } else if (demo == 0) {
            /*

            Sandbox

            Some test shapes and textures for testing new features added to the code.

             */

            gameObject greenCube = new gameObject("magic-cube", "green-face");
            greenCube.setPosition(new Vector3f(0, 0, 10));
            greenCube.setAxisOfRotation(new Vector3f(0, 1, 0));
            greenCube.setRotationSpeed(0.5);
            universe.addObject(greenCube, false);

            gameObject userObj = new gameObject("smooth-monkey", "blue-face");
            userObj.setVelocity(new Vector3f(-0.4f, 0.1f, 0));
            userObj.setRotationSpeed(0.8);
            userObj.setAxisOfRotation(new Vector3f(1, 1, 1));
            universe.addObject(userObj, true);

            universe.getCam().cycleMode();
        }

        projection = new Matrix4f().perspective((float)Math.toRadians(45.0f),
                (float) windowWidth[0] / (float) windowHeight[0],
                0.1f,
                100.0f);

        mvpLocation = glGetUniformLocation(programID, "MVP");
        mvpNormalLocation = glGetUniformLocation(programID, "normalMVP");
        mvLocation = glGetUniformLocation(programID, "MV");
        cameraLocation = glGetUniformLocation(programID, "cameraV");
        lightSourceLocation = glGetUniformLocation(programID, "lightV");
        shadeModeLocation = glGetUniformLocation(programID, "shaderMode");

        currentTime = glfwGetTime();
    }

    private void loop() {

        Matrix4f model, view, mv = new Matrix4f();
        Vector4f cameraHomo;

        Matrix3f mvpNormal;

        int texID;

        float deltaTime;

        boolean cameraToggle = false, shadingToggle = false;
        int last = 0;

        // todo: how is the callback different to the getKey part of the while condition? Can one be trimmed? why?
        while ( !glfwWindowShouldClose(window) && glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS) {

            Vector3f velocity = new Vector3f();
            float dv = 1.5f;
            if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
                dv = 3;
            }
            double dtheta = 0.75f;

            // movement
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
                velocity.add(new Vector3f(0, 0, dv));
            }
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
                velocity.add(new Vector3f(0, 0, -dv));
            }
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
                velocity.add(new Vector3f(dv, 0, 0));
            }
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
                velocity.add(new Vector3f(-dv, 0, 0));
            }
            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                velocity.add(new Vector3f(0, dv, 0));
            }
            if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
                velocity.add(new Vector3f(0, -dv, 0));
            }
            velocity.mul(universe.getCam().getRotation());
            universe.getCam().setVelocity(velocity);
            // rotation
            if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) {
                universe.getCam().setRotationSpeed(dtheta);
            } else if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) {
                universe.getCam().setRotationSpeed(-dtheta);
            } else {
                universe.getCam().setRotationSpeed(0);
            }

            // camera/shading settings should only be cycled one step per button press
            // not one step per frame

            // camera mode
            if (glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS) {
                if (!cameraToggle) {
                    universe.getCam().cycleMode();
                    cameraToggle = true;
                }
            } else {
                cameraToggle = false;
            }
            // shading mode
            if (glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS) {
                if (!shadingToggle) {
                    shadingMode = (shadingMode + 1) % shadingModes;
                    shadingToggle = true;
                }
            } else {
                shadingToggle = false;
            }


            glUniform1i(shadeModeLocation, shadingMode);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            lastTime = currentTime;
            currentTime = glfwGetTime();
            deltaTime = (float)(currentTime - lastTime);

            universe.nextFrame(deltaTime);
//            lightSource = new Vector3f(universe.getMainObject().position());
//
//            lightSource.mul(-1);

            view = universe.getCam().viewMatrix();

            lightSource = new Vector4f(light.position(), 1);

            lightSource.mul(view);

            cameraHomo = new Vector4f(universe.getCam().position(), 1);
            cameraHomo.mul(view);

//            lightSource.mul(light.modelMatrix());
//            lightSource.mul(view);
//            lightSource.mul(projection);

            normaliseHomogeneous(lightSource);
            normaliseHomogeneous(cameraHomo);

            glUniform3fv(cameraLocation, new float[]{cameraHomo.x, cameraHomo.y, cameraHomo.z});
            glUniform3fv(lightSourceLocation, new float[]{lightSource.x, lightSource.y, lightSource.z});

            // model loop, one iteration per model in the scene
            while (universe.nextObject()) {

                model = universe.currentModel();
                projection.mul(view, mvp);
                mvp.mul(model);

                glUniformMatrix4fv(mvpLocation, false, mvp.get(new float[16]));

                // Transformation by a non-orthogonal matrix does not preserve angles

                mvpNormal = new Matrix3f();
                model.get3x3(mvpNormal);
                mvpNormal = mvpNormal.invert();
                mvpNormal = mvpNormal.transpose();

                glUniformMatrix3fv(mvpNormalLocation, false, mvpNormal.get(new float[9]));


                view.mul(model, mv);

                glUniformMatrix4fv(mvLocation, false, mv.get(new float[16]));


                texID = loadTexture(universe.currentTexPath());


                // todo: which parts of the buffer uploading can be moved to init?

                // todo: what does this line do? Is it a remnant from an older version of the code?
                vao = glGenVertexArrays();
                glBindVertexArray(vao);

                vertexBuffer = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
                glBufferData(GL_ARRAY_BUFFER, universe.currentVertexBuffer(), GL_STATIC_DRAW);
                glVertexAttribPointer(0, 3, GL_FLOAT, false,0, 0);
                glEnableVertexAttribArray(0);

                uvBuffer = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
                glBufferData(GL_ARRAY_BUFFER, universe.currentUVBuffer(), GL_STATIC_DRAW);
                glVertexAttribPointer(1, 2, GL_FLOAT, false,0, 0);
                glEnableVertexAttribArray(1);

                normalBuffer = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, normalBuffer);
                glBufferData(GL_ARRAY_BUFFER, universe.currentNormalBuffer(), GL_STATIC_DRAW);
                glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
                glEnableVertexAttribArray(2);

                indexBuffer = glGenBuffers();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, universe.currentIndexBuffer(), GL_STATIC_DRAW);


                glDrawElements(
                        GL_TRIANGLES,
                        universe.currentIndexBuffer().length,
                        GL_UNSIGNED_INT,
                        0
                );
            }

            glfwSwapBuffers(window);

            glfwPollEvents();
        }
    }

    private void normaliseHomogeneous(Vector4f v) {
        if (v.w != 1) {
            v.x = v.x / v.w;
            v.y = v.y / v.w;
            v.z = v.z / v.w;
            v.w = 1;
        }
    }

    public void uploadMatrix4f(Matrix4f m, String target) {
        int location = glGetUniformLocation(programID, target);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        m.get(buffer);
        glUniformMatrix4fv(location, false, buffer);
    }


    static int LoadShaders(String vertex_file_path, String fragment_file_path) throws IOException {

        // Create the shaders
        int VertexShaderID = glCreateShader(GL_VERTEX_SHADER);
        int FragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);

        // Read the Vertex Shader code from the file
        String VertexShaderCode = new String(Files.readAllBytes(Path.of(vertex_file_path)));

        // Read the Fragment Shader code from the file
        String FragmentShaderCode = new String(Files.readAllBytes(Path.of(fragment_file_path)));

        int Result = GL_FALSE;
        int InfoLogLength;

        // Compile Vertex Shader
        System.out.printf("Compiling shader : %s\n", vertex_file_path);
        glShaderSource(VertexShaderID, VertexShaderCode);
        glCompileShader(VertexShaderID);

        // Check Vertex Shader
        glGetShaderi(VertexShaderID, GL_COMPILE_STATUS);
        InfoLogLength = glGetShaderi(VertexShaderID, GL_INFO_LOG_LENGTH);
        if ( InfoLogLength > 0 ){
            System.out.println(glGetShaderInfoLog(VertexShaderID));
        }

        // Compile Fragment Shader
        System.out.printf("Compiling shader : %s\n", fragment_file_path);
        glShaderSource(FragmentShaderID, FragmentShaderCode);
        glCompileShader(FragmentShaderID);

        // Check Fragment Shader
        glGetShaderi(FragmentShaderID, GL_COMPILE_STATUS);
        InfoLogLength = glGetShaderi(FragmentShaderID, GL_INFO_LOG_LENGTH);
        if ( InfoLogLength > 0 ){
            System.out.println(glGetShaderInfoLog(FragmentShaderID));
        }

        // Link the program
        System.out.println("Linking program");
        int ProgramID = glCreateProgram();
        glAttachShader(ProgramID, VertexShaderID);
        glAttachShader(ProgramID, FragmentShaderID);
        glLinkProgram(ProgramID);

        // Check the program
        glGetProgrami(ProgramID, GL_LINK_STATUS);
        InfoLogLength = glGetProgrami(ProgramID, GL_INFO_LOG_LENGTH);
        if ( InfoLogLength > 0 ){
            System.out.println(glGetProgramInfoLog(ProgramID));
        }

        glDetachShader(ProgramID, VertexShaderID);
        glDetachShader(ProgramID, FragmentShaderID);

        glDeleteShader(VertexShaderID);
        glDeleteShader(FragmentShaderID);
        return ProgramID;
    }

    private int loadTexture(String path) {

        int textureID;

        textureLoader tex = new textureLoader(path);


        textureID = glGenTextures();

        glEnable(GL_TEXTURE_2D);

        // "Bind" the newly created texture : all future texture functions will modify this texture
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Give the image to OpenGL
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, tex.getWidth(), tex.getHeight(), 0, GL_RGB, GL_UNSIGNED_BYTE, tex.buffer());

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        return textureID;
    }

    public boolean setDemo(String name) {
        if (name.equals("sandbox")) {
            demo = 0;
        } else if (name.equals("solar")) {
            demo = 1;
        } else if (name.equals("bounce")) {
            demo = 2;
        } else {
            return false; // not a valid demo
        }
        return true;
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.printf("Error unexpected number of arguments, got %d expected 1", args.length);
        } else {
            graphicsApplication ga = new graphicsApplication();
            if (ga.setDemo(args[0])) {
                ga.run();
            } else {
                System.out.println("Invalid demo.");
            }
        }
    }

}