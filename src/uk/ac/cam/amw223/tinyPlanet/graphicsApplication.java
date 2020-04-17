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
    private int cameraLocation;
    private int lightSourceLocation;

    private int vao;
    private int vertexBuffer;
    private int uvBuffer;
    private int normalBuffer;
    private int indexBuffer;

    private Vector3f cameraPosition;
    private Vector4f lightSource = new Vector4f();
    gameObject light;

    private Matrix4f projection;
    private Matrix4f mvp = new Matrix4f();

    double lastTime;
    double currentTime;

    private gameUniverse universe = new gameUniverse();

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

        glClearColor(0.5f, 0.5f, 0.5f, 0.0f);

        glUseProgram(programID);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(window, windowWidth, windowHeight);

        // todo: remove hard coded test values

        universe.addObject("smooth-sphere", "blue-face", true);
        universe.getMainObject().setVelocity(new Vector3f(0, 0.5f, 0));
        universe.addObject("smooth-sphere", "red-face", true);
//        universe.getMainObject().setVelocity(new Vector3f(0.1f, 0, 0));
        universe.getMainObject().setPosition(new Vector3f(1, 1, 1));
        light = new gameObject("magic-cube", "checkerboard");
        light.setPosition(new Vector3f(0, 5, 10));
//        light.setVelocity(new Vector3f(0.1f, 0.1f, 0.1f));
        universe.addObject("smooth-sphere", "green-face", true);
        universe.getMainObject().setVelocity(new Vector3f(0, -0.5f, -0.6f));
        universe.addObject(light, true);

        projection = new Matrix4f().perspective((float)Math.toRadians(45.0f),
                (float) windowWidth[0] / (float) windowHeight[0],
                0.1f,
                100.0f);

        mvpLocation = glGetUniformLocation(programID, "MVP");
        mvpNormalLocation = glGetUniformLocation(programID, "normalMVP");
        cameraLocation = glGetUniformLocation(programID, "camera");
        lightSourceLocation = glGetUniformLocation(programID, "lightSourcePosition");


        currentTime = glfwGetTime();
    }

    private void loop() {

        Matrix4f model, view;

        Matrix3f mvpNormal;

        int texID;

        float deltaTime;

        boolean toggle = true;
        int last = 0;

        // todo: how is the callback different to the getKey part of the while condition? Can one be trimmed? why?
        while ( !glfwWindowShouldClose(window) && glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS) {

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            lastTime = currentTime;
            currentTime = glfwGetTime();
            deltaTime = (float)(currentTime - lastTime);

            universe.nextFrame(deltaTime);
//            lightSource = new Vector3f(universe.getMainObject().position());
//
//            lightSource.mul(-1);

            view = generateViewMatrix();

            lightSource = new Vector4f(light.r, 1);

//            lightSource.mul(light.modelMatrix());
            lightSource.mul(view);
            lightSource.mul(projection);

            lightSource.x = lightSource.x / lightSource.w;
            lightSource.y = lightSource.y / lightSource.w;
            lightSource.z = lightSource.z / lightSource.w;
            lightSource.x = 1;

            glUniform3fv(cameraLocation, new float[]{cameraPosition.x, cameraPosition.y, cameraPosition.z});
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

    private Matrix4f generateViewMatrix() {

        float zoom = 10;// todo: add controls for zoom

        // camera Location Homogeneous
        Vector4f cLH = new Vector4f(0, zoom, -zoom, 1);
        cLH.mulProject(universe.getMainObject().modelMatrix());

        cameraPosition = new Vector3f(cLH.x, cLH.y, cLH.z);

        Vector3f facing = universe.getMainObject().r;

        // normalised cartesian model matrix (i.e. no translations) todo: is this mathematically valid?
        Matrix3f cartesianModel = new Matrix3f();
        universe.getMainObject().modelMatrix().get3x3(cartesianModel);

        Vector3f up = new Vector3f(0, 1, 0);
        up.mul(cartesianModel);

        // todo: remove debug values
        cameraPosition = new Vector3f(0, 0, -10);
        facing = new Vector3f(0, 0, 0);
        up = new Vector3f(0, 1, 0);

        return new Matrix4f().lookAt(
                cameraPosition,
                facing,
                up
        );
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


    public static void main(String[] args) {
        new graphicsApplication().run();
    }

}