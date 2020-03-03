package uk.ac.cam.amw223.tinyPlanet;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class modelLoader {

//    private List<float[]> vertexBuffer;
//    private List<float[]> uvBuffer;
//    private List<int[]> vertexIndexBuffer;
//    private List<int[]> uvIndexBuffer;

    List<float[]> vertexBufferList = new ArrayList<>();
    List<float[]> uvBufferList = new ArrayList<>();
    List<int[]> vertexIndexBufferList = new ArrayList<>();
    List<int[]> uvIndexBufferList = new ArrayList<>();

    private float[] vertexBuffer;
    private float[] uvBuffer;

    // used in construction of indexBuffer
    private int[] vertexIndexBuffer;
    private int[] uvIndexBuffer;

    private int[] indexBuffer;

    public modelLoader(String path) {
        try {
            loadBuffers(path);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadBuffers(String path) throws IOException {


        List<String> file = Files.readAllLines(Path.of(path));
        List<String> tokens;
        List<String> faceTokens;
        int faceType = 0;
        float[] currentVertexLine;
        float[] currentUVLine;
        int[] currentVertexIndexLine;
        int[] currentUVIndexLine;

        for (String line : file) {
            tokens = parseLine(line, ' ');
            // ensure not empty line
            if (tokens.size() > 0) {
                if (tokens.get(0).equals("v")) {
                    // vertex coordinates
                    currentVertexLine = new float[3];
                    currentVertexLine[0] = Float.parseFloat(tokens.get(1));
                    currentVertexLine[1] = Float.parseFloat(tokens.get(2));
                    currentVertexLine[2] = Float.parseFloat(tokens.get(3));
                    if (tokens.get(0).length() == 5) {
//                        currentVertexLine[3] = Float.parseFloat(tokens.get(4));
                    } else {
//                        currentVertexLine[3] = 1.0f;
                    }
                    // anything not read in defaults to 0 anyway
                    vertexBufferList.add(currentVertexLine);
                } else if (tokens.get(0).equals("vt")) {
                    //texture co-ordinates
                    currentUVLine = new float[2];
                    currentUVLine[0] = Float.parseFloat(tokens.get(1));
                    if (tokens.size() > 2) {
                        currentUVLine[1] = Float.parseFloat(tokens.get(2));
                    }
                    if (tokens.size() > 3) {
//                        currentUVLine[2] = Float.parseFloat(tokens.get(3));
                    }
                    // anything not read in defaults to 0 anyway
                    uvBufferList.add(currentUVLine);
                } else if (tokens.get(0).equals("f")) {
                    // face information
                    currentVertexIndexLine = new int[tokens.size() - 1];
                    currentUVIndexLine = new int[tokens.size() - 1];
                    for (int i = 1; i < tokens.size(); i++) {
                        // triangle i can be made with the following
                        // 0 (i) (i + 1)  [for i in 1..(n - 2)]
                        faceTokens = parseLine(tokens.get(i), '/');
                        if (faceType == 0) {
                            faceType = tokens.size();
                            if (faceTokens.get(1).equals("")) {
                                faceType++;
                            }
                        }
                        // 1 : f v1 v2 v3 ...
                        // 2 : f v1/vt1 v2/vt2 v3/vt3 ...
                        // 3 : f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                        // 4 : f v1//vn1 v2//vn2 v3//vn3 ...
                        if (faceType > 0) {
                            currentVertexIndexLine[i - 1] = Integer.parseInt(faceTokens.get(0)) - 1;
                        }
                        if (faceType > 1) {
                            currentUVIndexLine[i - 1] = Integer.parseInt(faceTokens.get(1)) - 1;
                        }
                        if (faceType > 2) {
                            // vector normal, not implemented yet
                        }
                    }
                    for (int i = 1; i < currentVertexIndexLine.length - 1; i++) {
                        // triangle i can be made with the following
                        // 0 (i) (i + 1)  [for i in 1..(n - 2)]
                        vertexIndexBufferList.add(
                                new int[]{currentVertexIndexLine[0],
                                        currentVertexIndexLine[i],
                                        currentVertexIndexLine[i + 1]});
                    }
                    for (int i = 1; i < currentUVIndexLine.length - 1; i++) {
                        // triangle i can be made with the following
                        // 0 (i) (i + 1)  [for i in 1..(n - 2)]
                        uvIndexBufferList.add(
                                new int[]{currentUVIndexLine[0],
                                        currentUVIndexLine[i],
                                        currentUVIndexLine[i + 1]});
                    }
                    faceType = 0;
                }
                // other .obj functionality that I don't know how to use yet lol
            }
        }

        generateVertexBuffer();
        generateVertexIndexBuffer();
        generateUVBuffer();
        generateUVIndexBuffer();

        // this ties together the two index buffers into one buffer that openGL can use
        // (it also modifies the orderings in the vertex and UV buffers so they match the
        // new unified buffer)
        generateIndexBuffer();
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

    private void generateVertexBuffer() {
        float[] vbo = new float[vertexBufferList.size() * 3];
        for (int i = 0; i < vertexBufferList.size(); i++) {
            vbo[3 * i] = vertexBufferList.get(i)[0];
            vbo[3 * i + 1] = vertexBufferList.get(i)[1];
            vbo[3 * i + 2] = vertexBufferList.get(i)[2];
        }
        vertexBuffer = vbo;
    }

    private void generateUVBuffer() {
        float[] vbo = new float[uvBufferList.size() * 2];
        for (int i = 0; i < uvBufferList.size(); i++) {
            vbo[2 * i] = uvBufferList.get(i)[0];
            vbo[2 * i + 1] = uvBufferList.get(i)[1];
        }
        uvBuffer = vbo;
    }

    private void generateVertexIndexBuffer() {
        int[] vbo = new int[vertexIndexBufferList.size() * 3];
        for (int i = 0; i < vertexIndexBufferList.size(); i++) {
            vbo[3 * i] = vertexIndexBufferList.get(i)[0];
            vbo[3 * i + 1] = vertexIndexBufferList.get(i)[1];
            vbo[3 * i + 2] = vertexIndexBufferList.get(i)[2];
        }
        vertexIndexBuffer = vbo;
    }

    private void generateUVIndexBuffer() {
        int[] vbo = new int[uvIndexBufferList.size() * 3];
        for (int i = 0; i < uvIndexBufferList.size(); i++) {
            vbo[3 * i] = uvIndexBufferList.get(i)[0];
            vbo[3 * i + 1] = uvIndexBufferList.get(i)[1];
            vbo[3 * i + 2] = uvIndexBufferList.get(i)[2];
        }
        uvIndexBuffer = vbo;
    }

    private void generateIndexBuffer() {

        List<Integer> newIndexBuffer = new ArrayList<>();

        // helper matrix stores index of a given vertex, UV pair
        int[][] helperMatrix = new int[vertexBuffer.length][uvBuffer.length];
        for (int[] row : helperMatrix) {
            Arrays.fill(row, -1);
        }

        List<float[]> newVertexBuffer = new ArrayList<>();
        List<float[]> newUVBuffer = new ArrayList<>();

        int count = 0;
        int v, t;

        for (int i = 0; i < vertexIndexBuffer.length; i++) {
            v = vertexIndexBuffer[i];
            t = uvIndexBuffer[i];

            if (helperMatrix[v][t] == -1) {

                // new pair found
                newVertexBuffer.add(new float[] {
                        vertexBuffer[3 * v],
                        vertexBuffer[3 * v + 1],
                        vertexBuffer[3 * v + 2]
                });
                newUVBuffer.add(new float[] {
                        uvBuffer[2 * t],
                        uvBuffer[2 * t + 1]
                });

                // index and memoise new pair location
                newIndexBuffer.add(count);
                helperMatrix[v][t] = count;
                count++;

            } else {
                // pair found previously
                newIndexBuffer.add(helperMatrix[v][t]);
            }
        }

        vertexBufferList = newVertexBuffer;
        uvBufferList = newUVBuffer;

        generateVertexBuffer();
        generateUVBuffer();

        int[] vbo = new int[newIndexBuffer.size()];
        for (int i = 0; i < newIndexBuffer.size(); i++) {
            vbo[i] = newIndexBuffer.get(i);
        }

        indexBuffer = vbo;
    }

    public float[] getVertexBuffer() {
        return vertexBuffer;
    }

    public float[] getUVBuffer() {
        return uvBuffer;
    }

    public int[] getIndexBuffer() {
        return indexBuffer;
    }
}
