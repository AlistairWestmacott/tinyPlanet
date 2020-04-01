package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix3f;
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

    private Matrix4f projection;
    private Matrix4f mvp = new Matrix4f();

    public void run() {

        try {
            init();

            // todo: while (!finished) {finished = loop();}
            // this will basically mean that loop only includes loop code and not initialisation code too
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
        window = glfwCreateWindow(500, 580, "Tiny Planet", NULL, NULL);
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



        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        programID = LoadShaders("resources/textureVertexShader.glsl", "resources/toonFragmentShader.glsl");

        // Enable depth test
        glEnable(GL_DEPTH_TEST);
        // Accept fragment if it closer to the camera than the former one
        glDepthFunc(GL_LESS);

        // Set the clear color
        glClearColor(0.5f, 0.5f, 0.5f, 0.0f);

        glUseProgram(programID);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(window, windowWidth, windowHeight);

        // projection matrix is constant
        // Projection matrix : 45° Field of View, 4:3 ratio, display range : 0.1 unit <-> 100 units
        projection = new Matrix4f().perspective((float)Math.toRadians(45.0f),
                (float) windowWidth[0] / (float)windowHeight[0],
                0.1f,
                100.0f);

    }

    private void loop() {

        // Camera matrix

        // todo: put view matrix generation inside loop and parameterize it based on main game objects location
        Matrix4f View = new Matrix4f().lookAt(
            new Vector3f(4,3,3), // Camera is at (4,3,3), in World Space
            new Vector3f(0,0,0), // and looks at the origin
            new Vector3f(0,1,0)  // Head is up (set to 0,-1,0 to look upside-down)
        );

        // Model matrix : an identity matrix (model will be at the origin)
        // todo: model matrix is generated by game object?
        // main game object has identity model matrix
        // all other game objects determine model matrix based on main game object (inverse of main game objects?)
        Matrix4f Model = new Matrix4f().identity();
        // Our ModelViewProjection : multiplication of our 3 matrices

        projection.mul(View, mvp);
        mvp.mul(Model); // Remember, matrix multiplication is the other way around

        // Transformation by a nonorthogonal matrix does not preserve angles
        // Thus we need a separate transformation matrix for normals
        Matrix3f normal_matrix;

        // Get a handle for our "MVP" uniform
        // Only during the initialisation
        int MatrixID = glGetUniformLocation(programID, "MVP");

        // Send our transformation to the currently bound shader, in the "MVP" uniform
        // This is done in the main loop since each model will have a different MVP matrix (At least for the M part)
        glUniformMatrix4fv(MatrixID, false, mvp.get(new float[16]));// this new float[] bit could be a source of errors

        float time = 0f;
        float[] rgb = new float[3];
        boolean running = true;
        boolean toggle = false;//for debouncing?
        int i;

        // texture is unused because the texture is bound to openGL within loadBMP_custom()
        int Texture = loadTexture("resources/smooth-sphere.png");

        modelLoader model = new modelLoader("resources/smooth-sphere.obj");

        float[] g_vertex_buffer_data = model.getVertexBuffer();
        float[] g_uv_buffer_data = model.getUVBuffer();
        float[] g_normal_buffer_data = model.getNormalBuffer();
        int[] g_index_buffer_data = model.getIndexBuffer();

        // position
        Vector3f position = new Vector3f( 0, 0, 5 );
        // horizontal angle : toward -Z
        float horizontalAngle = 3.14f;
        // vertical angle : 0, look at the horizon
        float verticalAngle = 0.0f;
        // Initial Field of View
        float initialFoV = 45.0f;

        float speed = 3.0f; // 3 units / second
        float mouseSpeed = 0.05f;

        double xpos, ypos;
        DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);

        double lastTime = 0.0;
        double currentTime = glfwGetTime();
        float deltaTime;

        // Right vector
        Vector3f right = new Vector3f(
                (float)Math.sin(horizontalAngle - 3.14f/2.0f),
                0.0f,
                (float)Math.cos(horizontalAngle - 3.14f/2.0f)
        );
        Vector3f direction = new Vector3f(
                (float)Math.cos(verticalAngle) * (float)Math.sin(horizontalAngle),
                (float)Math.sin(verticalAngle),
                (float)Math.cos(verticalAngle) * (float)Math.cos(horizontalAngle)
        );

        // Up vector : perpendicular to both direction and right
        Vector3f up = new Vector3f();
        right.cross(direction, up);
        Vector3f tempDirection = new Vector3f();


        int mvpLocation = glGetUniformLocation(programID, "MVP");
        int normalMVPLocation = glGetUniformLocation(programID, "normalMVP");
        FloatBuffer bufferMVP = BufferUtils.createFloatBuffer(16);
        FloatBuffer bufferNormalMVP = BufferUtils.createFloatBuffer(9);


        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) && glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS) {
            // clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                if (!toggle) {
                    running = !running;
                    toggle = true;
                }
            } else {
                toggle = false;
            }

            // SHADING

            mvp.get(bufferMVP);
            glUniformMatrix4fv(mvpLocation, false, bufferMVP);

            // Transformation by a nonorthogonal matrix does not preserve angles
            // Thus we need a separate transformation matrix for normals
            normal_matrix = new Matrix3f();
            // Calculate normal transformation matrix
            Model.get3x3(normal_matrix);
            normal_matrix = normal_matrix.invert();
            normal_matrix = normal_matrix.transpose();

            normal_matrix.get(bufferNormalMVP);
            glUniformMatrix3fv(normalMVPLocation, false, bufferNormalMVP);

            // CAMERA CONTROLS


            lastTime = currentTime;
            currentTime = glfwGetTime();
            deltaTime = (float)(currentTime - lastTime);
/*
//            // reading the mouse
//            glfwGetCursorPos(window, xBuffer, yBuffer);
//            xpos = xBuffer.get(0);
//            ypos = yBuffer.get(0);
//            // Reset mouse position for next frame
//            glfwSetCursorPos(window, windowWidth[0]/2, windowHeight[0]/2);
//            // Compute new orientation
//            horizontalAngle += mouseSpeed * deltaTime * (float)(windowWidth[0]/2.0f - xpos );
//            verticalAngle   += mouseSpeed * deltaTime * (float)(windowHeight[0]/2.0f - ypos );
//
//            // Direction : Spherical coordinates to Cartesian coordinates conversion
//            direction = new Vector3f(
//                    (float)Math.cos(verticalAngle) * (float)Math.sin(horizontalAngle),
//                    (float)Math.sin(verticalAngle),
//                    (float)Math.cos(verticalAngle) * (float)Math.cos(horizontalAngle)
//            );
//
//            right = new Vector3f(
//                    (float)Math.sin(horizontalAngle - 3.14f/2.0f),
//                    0.0f,
//                    (float)Math.cos(horizontalAngle - 3.14f/2.0f)
//            );
//
//            // Move forward
//            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS){
//                direction.mul(deltaTime * speed, tempDirection);
//                position.add(tempDirection);
//            }
//            // Move backward
//            if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS){
//                direction.mul(deltaTime * speed, tempDirection);
//                position.sub(tempDirection);
//            }
//            // Strafe right
//            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS){
//                right.mul(deltaTime * speed, tempDirection);
//                position.add(tempDirection);
//            }
//            // Strafe left
//            if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS){
//                right.mul(deltaTime * speed, tempDirection);
//                position.sub(tempDirection);
//            }
//
//            // Projection matrix : 45&deg; Field of View, 4:3 ratio, display range : 0.1 unit <-> 100 units
//            Projection = new Matrix4f().perspective((float)Math.toRadians(initialFoV),
//                    (float) windowWidth[0] / (float)windowHeight[0],
//                    0.1f,
//                    100.0f);
//
//            position.add(direction, tempDirection);
//            // Camera matrix
//            View = new Matrix4f().lookAt(
//                    position,          // Camera is here
//                    tempDirection,     // and looks here : at the same position, plus "direction"
//                    up                 // Head is up (set to 0,-1,0 to look upside-down)
//            );
//            // Our ModelViewProjection : multiplication of our 3 matrices
//            mvp = Projection.mul(View).mul(Model); // Remember, matrix multiplication is the other way around
//
//            // Send our transformation to the currently bound shader, in the "MVP" uniform
//            // This is done in the main loop since each model will have a different MVP matrix (At least for the M part)
//            glUniformMatrix4fv(MatrixID, false, mvp.get(new float[16]));// this new float[] bit could be a source of errors
*/

            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS){
                // change model matrix
                Model.add(new Matrix4f().set(
                        new float[]{
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                deltaTime, 0f, 0f, 0f}
                                ));
            }
            if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS){
                // change model matrix
                Model.add(new Matrix4f().set(
                        new float[]{
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                -deltaTime, 0f, 0f, 0f}
                ));
            }
            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS){
                // change model matrix
                Model.add(new Matrix4f().set(
                        new float[]{
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, deltaTime, 0f, 0f}
                ));
            }
            if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS){
                // change model matrix
                Model.add(new Matrix4f().set(
                        new float[]{
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, 0f, 0f, 0f,
                                0f, -deltaTime, 0f, 0f}
                ));
            }
            // Our ModelViewProjection : multiplication of our 3 matrices
            projection.mul(View, mvp);
            mvp.mul(Model); // Remember, matrix multiplication is the other way around

            int vao = glGenVertexArrays();
            glBindVertexArray(vao);

            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, g_vertex_buffer_data, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(0);

            int uvBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
            glBufferData(GL_ARRAY_BUFFER, g_uv_buffer_data, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(1);

            //glEnableVertexAttribArray();

            int normalBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, normalBuffer);
            glBufferData(GL_ARRAY_BUFFER, g_normal_buffer_data, GL_STATIC_DRAW);
            // 3rd attribute buffer : normals
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(2);

            // Generate a buffer for the indices
            int indexBuffer = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, g_index_buffer_data, GL_STATIC_DRAW);


            //glDrawArrays(GL_TRIANGLES, 0, g_vertex_buffer_data.length);

            // Draw the triangles !
            glDrawElements(
                    GL_TRIANGLES,      // mode
                    g_index_buffer_data.length,    // count
                    GL_UNSIGNED_INT,   // type
                    0           // element array buffer offset
            );


            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
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


    public static void main(String[] args) {
        new graphicsApplication().run();
    }

}