package uk.ac.cam.amw223.tinyPlanet;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class modelLoader {

    private List<float[]> vertexBuffer;
    private List<float[]> uvBuffer;
    private List<int[]> vertexIndexBuffer;
    private List<int[]> UVIndexBuffer;

    public modelLoader(String path) {
        vertexBuffer = new ArrayList<>();
        uvBuffer = new ArrayList<>();
        vertexIndexBuffer = new ArrayList<>();
        UVIndexBuffer = new ArrayList<>();
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
                    currentVertexLine = new float[4];
                    currentVertexLine[0] = Float.parseFloat(tokens.get(1));
                    currentVertexLine[1] = Float.parseFloat(tokens.get(2));
                    currentVertexLine[2] = Float.parseFloat(tokens.get(3));
                    if (tokens.get(0).length() == 5) {
                        currentVertexLine[3] = Float.parseFloat(tokens.get(4));
                    } else {
                        currentVertexLine[3] = 1.0f;
                    }
                    // anything not read in defaults to 0 anyway
                    vertexBuffer.add(currentVertexLine);
                } else if (tokens.get(0).equals("vt")) {
                    //texture co-ordinates
                    currentUVLine = new float[3];
                    currentUVLine[0] = Float.parseFloat(tokens.get(1));
                    if (tokens.get(0).length() > 2) {
                        currentUVLine[1] = Float.parseFloat(tokens.get(2));
                    }
                    if (tokens.get(0).length() > 3) {
                        currentUVLine[2] = Float.parseFloat(tokens.get(3));
                    }
                    // anything not read in defaults to 0 anyway
                    uvBuffer.add(currentUVLine);
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
                            currentVertexIndexLine[i - 1] = Integer.parseInt(faceTokens.get(0));
                        }
                        if (faceType > 1) {
                            currentUVIndexLine[i - 1] = Integer.parseInt(faceTokens.get(1));
                        }
                        if (faceType > 2) {
                            // vector normal, not implemented yet
                        }
                    }
                    for (int i = 1; i < currentVertexIndexLine.length - 1; i++) {
                        // triangle i can be made with the following
                        // 0 (i) (i + 1)  [for i in 1..(n - 2)]
                        vertexIndexBuffer.add(
                                new int[]{currentVertexIndexLine[0],
                                        currentVertexIndexLine[i],
                                        currentVertexIndexLine[i + 1]});
                    }
                    for (int i = 0; i < currentUVIndexLine.length - 2; i++) {
                        // triangle i can be made with the following
                        // 0 (i) (i + 1)  [for i in 1..(n - 2)]
                        UVIndexBuffer.add(
                                new int[]{currentUVIndexLine[0],
                                        currentUVIndexLine[i],
                                        currentUVIndexLine[i + 1]});
                    }
                    faceType = 0;
                }
                // other .obj functionality that I don't know how to use yet lol
            }
        }
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

    public float[] getVertexBuffer() {
        float[] vbo = new float[vertexBuffer.size() * 3];
        for (int i = 0; i < vertexBuffer.size(); i++) {
            vbo[3 * i] = vertexBuffer.get(i)[0];
            vbo[3 * i + 1] = vertexBuffer.get(i)[1];
            vbo[3 * i + 2] = vertexBuffer.get(i)[2];
        }
        return vbo;
    }

    public float[] getUVBuffer() {
        float[] vbo = new float[uvBuffer.size() * 2];
        for (int i = 0; i < uvBuffer.size(); i++) {
            vbo[2 * i] = uvBuffer.get(i)[0];
            vbo[2 * i + 1] = uvBuffer.get(i)[1];
        }
        return vbo;
    }

    public int[] getVertexIndexBuffer() {
        int[] vbo = new int[vertexIndexBuffer.size() * 3];
        for (int i = 0; i < vertexIndexBuffer.size(); i++) {
            vbo[3 * i] = vertexIndexBuffer.get(i)[0];
            vbo[3 * i + 1] = vertexIndexBuffer.get(i)[1];
            vbo[3 * i + 2] = vertexIndexBuffer.get(i)[2];
        }
        return vbo;
    }

    public int[] getUVIndexBuffer() {
        int[] vbo = new int[UVIndexBuffer.size() * 3];
        for (int i = 0; i < UVIndexBuffer.size(); i++) {
            vbo[3 * i] = UVIndexBuffer.get(i)[0];
            vbo[3 * i + 1] = UVIndexBuffer.get(i)[1];
            vbo[3 * i + 2] = UVIndexBuffer.get(i)[2];
        }
        return vbo;
    }
}
