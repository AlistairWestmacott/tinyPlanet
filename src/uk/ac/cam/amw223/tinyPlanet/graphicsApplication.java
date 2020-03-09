package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGR;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class graphicsApplication {

    // The window handle
    private long window;

    float[] g_vertex_buffer_data;

    // One UV for each vertex.
    float[] g_uv_buffer_data;

    int[] g_index_buffer_data;

    public void run() {

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

        int programID = LoadShaders("resources/textureVertexShader.glsl", "resources/textureFragmentShader.glsl");

        // Enable depth test
        glEnable(GL_DEPTH_TEST);
        // Accept fragment if it closer to the camera than the former one
        glDepthFunc(GL_LESS);

        // Set the clear color
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        glUseProgram(programID);

        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        glfwGetWindowSize(window, windowWidth, windowHeight);

        // Projection matrix : 45° Field of View, 4:3 ratio, display range : 0.1 unit <-> 100 units
        Matrix4f Projection = new Matrix4f().perspective((float)Math.toRadians(45.0f),
                (float) windowWidth[0] / (float)windowHeight[0],
                0.1f,
                100.0f);

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

        float time = 0f;
        float[] rgb = new float[3];
        boolean running = true;
        boolean toggle = false;//for debouncing?
        int i;

        // texture is unused because the texture is bound to openGL within loadBMP_custom()
        int Texture = loadTexture("resources/magic-arrow.png");

        modelLoader model = new modelLoader("resources/monkey.obj");

        g_vertex_buffer_data = model.getVertexBuffer();
        g_uv_buffer_data = model.getUVBuffer();
        g_index_buffer_data = model.getIndexBuffer();

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

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) && glfwGetKey(window, GLFW_KEY_ESCAPE ) != GLFW_PRESS) {
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


            // CAMERA CONTROLS

//            lastTime = currentTime;
//            currentTime = glfwGetTime();
//            deltaTime = (float)(currentTime - lastTime);
//
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

            int vao = glGenVertexArrays();
            glBindVertexArray(vao);

            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            //glBufferData(GL_ARRAY_BUFFER, verteces, GL_STATIC_DRAW);
            glBufferData(GL_ARRAY_BUFFER, g_vertex_buffer_data, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(0);

            int uvBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
            glBufferData(GL_ARRAY_BUFFER, g_uv_buffer_data, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(1);


            // Generate a buffer for the indices
            int elementBuffer = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, g_index_buffer_data, GL_STATIC_DRAW);
            //glEnableVertexAttribArray();

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
    
    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    } 

    private int loadBMP_custom(String imagePath) {
        // Data read from the header of the BMP file
        byte[] header = new byte[54]; // Each BMP file begins by a 54-bytes header
        int dataPos;     // Position in the file where the actual data begins
        int width = 0, height = 0;
        int imageSize;   // = width*height*3
        // Actual RGB data
        byte[] data;
        ByteBuffer buf = null;

        InputStream is = null;
        try {
            // new input stream created
            is = new FileInputStream(imagePath);

            // read stream data into buffer
            if (is.read(header) != 54) {
                System.out.println("Not a correct BMP file");
                return -1;
            }
            if ((char)header[0] != 'B' || (char)header[1] != 'M') {
                System.out.println("Not a correct BMP file");
                return -1;
            }

            // Read ints from the byte array
            dataPos = header[0x0A];
            imageSize = header[0x22];
            width = header[0x12];
            height = header[0x16];

            // Some BMP files are misformatted, guess missing information
            if (imageSize == 0) {
                imageSize = width*height*3; // 3 : one byte for each Red, Green and Blue component
            }
            if (dataPos == 0) {
                dataPos = 54; // The BMP header is done that way
            }

            // Create a buffer
            data = new byte[imageSize];

            // Read the actual data from the file into the buffer
            if (is.read(data,0, imageSize) < imageSize) {
                System.out.println("Error reading image. Not enough bytes read.");
            }

            // Wrap a byte array into a buffer
            //buf = ByteBuffer.wrap(data);
            //buf.order(ByteOrder.nativeOrder());
            buf = BufferUtils.createByteBuffer(width*height*3);
            for(int i=0; i<width*height; i++)
            {
                buf.put(data[i*3]);
                buf.put(data[i*3+1]);
                buf.put(data[i*3+2]);
            }
        
            //buf = BufferUtils.createByteBuffer(width*height*3);
            buf.flip();
            is.close();
        } catch(IOException e) {
            // if any I/O error occurs
            e.printStackTrace();
        }

        int textureID;
        textureID = glGenTextures();

        glEnable(GL_TEXTURE_2D);
                
        // "Bind" the newly created texture : all future texture functions will modify this texture
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Give the image to OpenGL
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buf);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        return textureID;
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