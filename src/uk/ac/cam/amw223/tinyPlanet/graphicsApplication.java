package uk.ac.cam.amw223.tinyPlanet;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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


    static float[] g_vertex_buffer_data = {
            -1.0f,-1.0f,-1.0f, // triangle 1 : begin
            -1.0f,-1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f, // triangle 1 : end
            1.0f, 1.0f,-1.0f, // triangle 2 : begin
            -1.0f,-1.0f,-1.0f,
            -1.0f, 1.0f,-1.0f, // triangle 2 : end
            1.0f,-1.0f, 1.0f,// 3
            -1.0f,-1.0f,-1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f,-1.0f,//4
            1.0f,-1.0f,-1.0f,
            -1.0f,-1.0f,-1.0f,
            -1.0f,-1.0f,-1.0f,//5
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f,-1.0f,
            1.0f,-1.0f, 1.0f,//6
            -1.0f,-1.0f, 1.0f,
            -1.0f,-1.0f,-1.0f,
            -1.0f, 1.0f, 1.0f,//7
            -1.0f,-1.0f, 1.0f,
            1.0f,-1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f,-1.0f,
            1.0f,-1.0f,-1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f,-1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f,-1.0f,
            -1.0f, 1.0f,-1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f,-1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f,-1.0f, 1.0f,
    };

    // One UV for each vertex.
    float[] g_uv_buffer_data = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,

    };

    // One color for each vertex. They were generated randomly.
    float[] g_color_buffer_data = {
            0.583f,  0.771f,  0.014f,
            0.609f,  0.115f,  0.436f,
            0.327f,  0.483f,  0.844f,
            0.822f,  0.569f,  0.201f,
            0.435f,  0.602f,  0.223f,
            0.310f,  0.747f,  0.185f,
            0.597f,  0.770f,  0.761f,
            0.559f,  0.436f,  0.730f,
            0.359f,  0.583f,  0.152f,
            0.483f,  0.596f,  0.789f,
            0.559f,  0.861f,  0.639f,
            0.195f,  0.548f,  0.859f,
            0.014f,  0.184f,  0.576f,
            0.771f,  0.328f,  0.970f,
            0.406f,  0.615f,  0.116f,
            0.676f,  0.977f,  0.133f,
            0.971f,  0.572f,  0.833f,
            0.140f,  0.616f,  0.489f,
            0.997f,  0.513f,  0.064f,
            0.945f,  0.719f,  0.592f,
            0.543f,  0.021f,  0.978f,
            0.279f,  0.317f,  0.505f,
            0.167f,  0.620f,  0.077f,
            0.347f,  0.857f,  0.137f,
            0.055f,  0.953f,  0.042f,
            0.714f,  0.505f,  0.345f,
            0.783f,  0.290f,  0.734f,
            0.722f,  0.645f,  0.174f,
            0.302f,  0.455f,  0.848f,
            0.225f,  0.587f,  0.040f,
            0.517f,  0.713f,  0.338f,
            0.053f,  0.959f,  0.120f,
            0.393f,  0.621f,  0.362f,
            0.673f,  0.211f,  0.457f,
            0.820f,  0.883f,  0.371f,
            0.982f,  0.099f,  0.879f
    };
    


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

        int programID = LoadShaders("resources/textureVertexShader.glsl", "resources/textureFragmentShader.glsl");

        g_color_buffer_data = new float [12*3*3];
        for (int v = 0; v < 12*3 ; v++){
            g_color_buffer_data[3*v+0] = v/36.0f;
            g_color_buffer_data[3*v+1] = 0f;
            g_color_buffer_data[3*v+2] = 0f;
        }

        g_vertex_buffer_data = new float[]{
                // left face
                -1.0f,-1.0f,-1.0f,
                -1.0f,-1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,

                -1.0f,-1.0f,-1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f,-1.0f,

                // back face
                1.0f, 1.0f,-1.0f,
                1.0f,-1.0f,-1.0f,
                -1.0f,-1.0f,-1.0f,

                1.0f, 1.0f,-1.0f,
                -1.0f,-1.0f,-1.0f,
                -1.0f, 1.0f,-1.0f,

                // bottom face
                1.0f,-1.0f, 1.0f,
                -1.0f,-1.0f,-1.0f,
                1.0f,-1.0f,-1.0f,

                1.0f,-1.0f, 1.0f,
                -1.0f,-1.0f, 1.0f,
                -1.0f,-1.0f,-1.0f,

                // front face
                -1.0f, 1.0f, 1.0f,
                -1.0f,-1.0f, 1.0f,
                1.0f,-1.0f, 1.0f,

                -1.0f, 1.0f, 1.0f,
                1.0f,-1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,

                // right face
                1.0f, 1.0f, 1.0f,
                1.0f,-1.0f,-1.0f,
                1.0f, 1.0f,-1.0f,

                1.0f, 1.0f, 1.0f,
                1.0f,-1.0f,-1.0f,
                1.0f,-1.0f, 1.0f,

                // top face
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f,-1.0f,
                -1.0f, 1.0f,-1.0f,

                1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f,-1.0f,
                -1.0f, 1.0f, 1.0f,
                
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
        };
        

        // Enable depth test
        glEnable(GL_DEPTH_TEST);
        // Accept fragment if it closer to the camera than the former one
        glDepthFunc(GL_LESS);

        // Set the clear color
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

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
                    new Vector3f(-4,3,3), // Camera is at (4,3,3), in World Space
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

        int Texture = loadBMP_custom("resources/checkerboard.bmp");

        float[][] buffers = loadBuffers("resources/magic-cube.obj");
        g_vertex_buffer_data = buffers[0];
        g_uv_buffer_data = buffers[1];

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



            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                if (!toggle) {
                    running = !running;
                    toggle = true;
                }
            } else {
                toggle = false;
            }

//            for (int v = 0; v < 12*3 ; v++){
//                i = v/6 + 1;
//                g_color_buffer_data[3*v+0] = (float)Math.abs(Math.sin(time)) * (i/6f);
//                g_color_buffer_data[3*v+1] = (float)Math.abs(Math.sin(time + 2*Math.PI/3)) * (i/6f);
//                g_color_buffer_data[3*v+2] = (float)Math.abs(Math.sin(time + 4*Math.PI/3)) * (i/6f);
//            }
//            if (running) {
//                time += 0.02f;
//                if (time > Math.PI * 2) {
//                    time = 0f;
//                }
//            }

            // Our vertices. Three consecutive floats give a 3D vertex; Three consecutive vertices give a triangle.
            // A cube has 6 faces with 2 triangles each, so this makes 6*2=12 triangles, and 12*3 vertices

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
            glEnableVertexAttribArray(1);
            glBindBuffer(GL_ARRAY_BUFFER, uvBuffer);
            glVertexAttribPointer(1, 2, GL_FLOAT, false,0, 0);
            glEnableVertexAttribArray(1);


            // Generate a buffer for the indices
            int elementbuffer;
            int[] gl_index_buffer_data = null;// TODO
            elementbuffer = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementbuffer);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, gl_index_buffer_data, GL_STATIC_DRAW);

            
            int colorBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, colorBuffer);
            glBufferData(GL_ARRAY_BUFFER, g_color_buffer_data, GL_STATIC_DRAW);
            // 2nd attribute buffer : colors
            glEnableVertexAttribArray(2);
            glBindBuffer(GL_ARRAY_BUFFER, colorBuffer);
            glVertexAttribPointer(
                    2,                                // attribute. No particular reason for 1, but must match the layout in the shader.
                    3,                                // size
                    GL_FLOAT,                         // type
                    false,                         // normalized?
                    0,                                // stride
                    0                          // array buffer offset
            );
            glEnableVertexAttribArray(2);


            glDrawArrays(GL_TRIANGLES, 0, 12*3);
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
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGB, width, height, 0, GL_BGR, GL_UNSIGNED_BYTE, buf);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        return textureID;
    }

    private float[][] loadBuffers(String path) throws IOException {
        float[][] buffers = new float[2][];
        // buffers[0] contains the vertex buffer
        // buffers[1] contains the UV buffer

        List<float[]> vertexBuffer = new ArrayList<>();
        List<float[]> uvBuffer = new ArrayList<>();
        List<float[]> indexBuffer = new ArrayList<>();

        List<String> file = Files.readAllLines(Path.of(path));
        List<String> tokens;
        float[] currentBuffer;

        for (String line : file) {
            tokens = parseLine(line, ' ');
            // ensure not empty line
            if (tokens.size() > 0) {
                if (tokens.get(0).equals("v")) {
                    // vertex coordinates
                    currentBuffer = new float[4];
                    currentBuffer[0] = Float.parseFloat(tokens.get(1));
                    currentBuffer[1] = Float.parseFloat(tokens.get(2));
                    currentBuffer[2] = Float.parseFloat(tokens.get(3));
                    if (tokens.get(0).length() == 5) {
                        currentBuffer[3] = Float.parseFloat(tokens.get(4));
                    } else {
                        currentBuffer[3] = 1.0f;
                    }
                    // anything not read in defaults to 0 anyway
                    vertexBuffer.add(currentBuffer);
                } else if (tokens.get(0).equals("vt")) {
                    //texture co-ordinates
                    currentBuffer = new float[3];
                    currentBuffer[0] = Float.parseFloat(tokens.get(1));
                    if (tokens.get(0).length() > 2) {
                        currentBuffer[1] = Float.parseFloat(tokens.get(2));
                    }
                    if (tokens.get(0).length() > 3) {
                        currentBuffer[2] = Float.parseFloat(tokens.get(3));
                    }
                    // anything not read in defaults to 0 anyway
                    uvBuffer.add(currentBuffer);
                } else if (tokens.get(0).equals("f")) {
                    // face information

                }
                // other .obj functionality that I don't know how to use yet lol
            }
        }


//        if (vertexBuffer.size() != uvBuffer.size()) {
//            throw new IOException("Buffers are different lengths.");
//        }


        buffers[0] = new float[vertexBuffer.size() * 3];
        buffers[1] = new float[uvBuffer.size() * 2];

        for (int i = 0; i < vertexBuffer.size(); i++) {
            // vertex buffer
            buffers[0][3 * i] = vertexBuffer.get(i)[0];
            buffers[0][3 * i + 1] = vertexBuffer.get(i)[1];
            buffers[0][3 * i + 2] = vertexBuffer.get(i)[2];
            // UV buffer
            buffers[0][2 * i] = vertexBuffer.get(i)[0];
            buffers[0][2 * i + 1] = vertexBuffer.get(i)[1];
        }

        return buffers;
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        String current = "";
        for (char c : line.toCharArray()) {
            if (c == delimiter) {
                tokens.add(current);
                current = "";
            } else {
                current += c;
            }
        }
        tokens.add(current);
        return tokens;
    }

    public static void main(String[] args) {
        new graphicsApplication().run();
    }

}