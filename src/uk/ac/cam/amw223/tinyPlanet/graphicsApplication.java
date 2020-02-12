package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;
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

    private final int width = 500;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        try {
            init();
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

    private void init() {
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
        window = glfwCreateWindow(500, 580, "Hello World!", NULL, NULL);
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

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

    }

    private void loop() throws IOException {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        programID = LoadShaders( "resources/simpleVertexShader.glsl", "resources/simpleFragmentShader.glsl" );

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glUseProgram(programID);

        // Projection matrix : 45° Field of View, 4:3 ratio, display range : 0.1 unit <-> 100 units
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(window, windowWidth, windowHeight);

        Matrix4f Projection = new Matrix4f().perspective((float)Math.toRadians(45.0f),
                (float) windowWidth[0] / (float)windowHeight[0],
                0.1f,
                100.0f);

        // Or, for an ortho camera :
        //glm::mat4 Projection = glm::ortho(-10.0f,10.0f,-10.0f,10.0f,0.0f,100.0f); // In world coordinates

        // Camera matrix
            Matrix4f View = new Matrix4f().lookAt(
                    new Vector3f(4,3,3), // Camera is at (4,3,3), in World Space
                    new Vector3f(0,0,0), // and looks at the origin
                    new Vector3f(0,1,0)  // Head is up (set to 0,-1,0 to look upside-down)
        );

        // Model matrix : an identity matrix (model will be at the origin)
        Matrix4f Model = new Matrix4f().identity();
        // Our ModelViewProjection : multiplication of our 3 matrices
        Matrix4f mvp = Projection.mul(View).mul(Model); // Remember, matrix multiplication is the other way around

        // Get a handle for our "MVP" uniform
        // Only during the initialisation
        int MatrixID = glGetUniformLocation(programID, "MVP");

        // Send our transformation to the currently bound shader, in the "MVP" uniform
        // This is done in the main loop since each model will have a different MVP matrix (At least for the M part)
        glUniformMatrix4fv(MatrixID, false, mvp.get(new float[16]));// this new float[] bit could be a source of errors


        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) && glfwGetKey(window, GLFW_KEY_ESCAPE ) != GLFW_PRESS) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // DRAW A TRIANGLE!!
            float[] verteces = new float[] {
                0.0f,  0.5f,
                -0.5f, -0.5f,
                0.5f, -0.5f
            };

            int vao = glGenVertexArrays();
            glBindVertexArray(vao);

            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, verteces, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 2, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(0);

            glDrawArrays(GL_TRIANGLES, 0, 3);
            // OMG I DID IT!!



            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
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
        System.out.printf("Linking program\n");
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

    public static void main(String[] args) {
        new graphicsApplication().run();
    }

}