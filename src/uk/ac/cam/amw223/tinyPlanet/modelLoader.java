package uk.ac.cam.amw223.tinyPlanet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class modelLoader {

    List<float[]> vertexBufferList = new ArrayList<>();
    List<float[]> uvBufferList = new ArrayList<>();
    List<float[]> normalBufferList = new ArrayList<>();
    List<int[]> vertexIndexBufferList = new ArrayList<>();
    List<int[]> uvIndexBufferList = new ArrayList<>();
    List<int[]> normalIndexBufferList = new ArrayList<>();

    private float[] vertexBuffer;
    private float[] uvBuffer;
    private float[] normalBuffer;

    // used in construction of indexBuffer
    private int[] vertexIndexBuffer;
    private int[] uvIndexBuffer;
    private int[] normalIndexBuffer;

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
        float[] currentNormalLine;
        int[] currentVertexIndexLine;
        int[] currentUVIndexLine;
        int[] currentNormalIndexLine;

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
                } else if (tokens.get(0).equals("vn")) {
                    currentNormalLine = new float[3];
                    currentNormalLine[0] = Float.parseFloat(tokens.get(1));
                    currentNormalLine[1] = Float.parseFloat(tokens.get(2));
                    currentNormalLine[2] = Float.parseFloat(tokens.get(3));
                    normalBufferList.add(currentNormalLine);
                } else if (tokens.get(0).equals("f")) {
                    // face information
                    currentVertexIndexLine = new int[tokens.size() - 1];
                    currentUVIndexLine = new int[tokens.size() - 1];
                    currentNormalIndexLine = new int[tokens.size() - 1];
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
                            currentNormalIndexLine[i - 1] = Integer.parseInt(faceTokens.get(2)) - 1;
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
                    for (int i = 1; i < currentNormalIndexLine.length - 1; i++) {
                        normalIndexBufferList.add(
                                new int[]{currentNormalIndexLine[0],
                                        currentNormalIndexLine[i],
                                        currentNormalIndexLine[i + 1]}
                                        );
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
        generateNormalBuffer();
        generateNormalIndexBuffer();

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

    private void generateNormalBuffer() {
        float[] vbo = new float[normalBufferList.size() * 3];
        for (int i = 0; i < normalBufferList.size(); i++) {
            vbo[3 * i] = normalBufferList.get(i)[0];
            vbo[3 * i + 1] = normalBufferList.get(i)[1];
            vbo[3 * i + 2] = normalBufferList.get(i)[2];
        }
        normalBuffer = vbo;
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

    private void generateNormalIndexBuffer() {
        int[] vbo = new int[normalIndexBufferList.size() * 3];
        for (int i = 0; i < normalIndexBufferList.size(); i++) {
            vbo[3 * i] = normalIndexBufferList.get(i)[0];
            vbo[3 * i + 1] = normalIndexBufferList.get(i)[1];
            vbo[3 * i + 2] = normalIndexBufferList.get(i)[2];
        }
        normalIndexBuffer = vbo;
    }

    private void generateIndexBuffer() {

        List<Integer> newIndexBuffer = new ArrayList<>();

        Map<Integer, int[]> foundIndeces = new HashMap<>();
        int[] foundIndex;

        List<float[]> newVertexBuffer = new ArrayList<>();
        List<float[]> newUVBuffer = new ArrayList<>();
        List<float[]> newNormalBuffer = new ArrayList<>();

        int count = 0;
        int v, t, n;
        boolean found;

        for (int i = 0; i < vertexIndexBuffer.length; i++) {
            v = vertexIndexBuffer[i];
            t = uvIndexBuffer[i];
            n = normalIndexBuffer[i];

            found = false;

            // if index hasn't been found yet
            for (int j = 0; j < foundIndeces.size(); j++) {
                foundIndex = foundIndeces.get(j);
                if (foundIndex[0] == v && foundIndex[1] == t && foundIndex[2] == n) {
                    found = true;
                    // pair found previously
                    newIndexBuffer.add(j);
                }
            }
            if (!found) {// new pair found
                newVertexBuffer.add(new float[]{
                        vertexBuffer[3 * v],
                        vertexBuffer[3 * v + 1],
                        vertexBuffer[3 * v + 2]
                });
                newUVBuffer.add(new float[]{
                        uvBuffer[2 * t],
                        uvBuffer[2 * t + 1]
                });
                newNormalBuffer.add(new float[]{
                        normalBuffer[3 * n],
                        normalBuffer[3 * n + 1],
                        normalBuffer[3 * n + 2]
                });

                // index and memoise new pair location
                newIndexBuffer.add(count);
                foundIndeces.put(count, new int[]{v, t, n});
                count++;
            }
        }

        vertexBufferList = newVertexBuffer;
        uvBufferList = newUVBuffer;
        normalBufferList = newNormalBuffer;

        generateVertexBuffer();
        generateUVBuffer();
        generateNormalBuffer();

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

    public float[] getNormalBuffer() {
        return normalBuffer;
    }

    public int[] getIndexBuffer() {
        return indexBuffer;
    }
}
