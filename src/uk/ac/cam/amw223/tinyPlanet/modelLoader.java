package uk.ac.cam.amw223.tinyPlanet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class modelLoader {

    List<float[]> vertexBuffer;
    List<float[]> uvBuffer;
    List<int[]> indexBuffer;

    public modelLoader(String path) {

    }

    private void loadBuffers(String path) throws IOException {

        List<String> file = Files.readAllLines(Path.of(path));
        List<String> tokens;
        List<String> faceTokens;
        int faceType;
        float[] currentVertexLine;
        int[] currentIndexLine;

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
                    currentVertexLine = new float[3];
                    currentVertexLine[0] = Float.parseFloat(tokens.get(1));
                    if (tokens.get(0).length() > 2) {
                        currentVertexLine[1] = Float.parseFloat(tokens.get(2));
                    }
                    if (tokens.get(0).length() > 3) {
                        currentVertexLine[2] = Float.parseFloat(tokens.get(3));
                    }
                    // anything not read in defaults to 0 anyway
                    uvBuffer.add(currentVertexLine);
                } else if (tokens.get(0).equals("f")) {
                    // face information

                    for (int i = 1; i < tokens.size(); i++) {
                        faceTokens = parseLine(tokens.get(i), '/');
                        if (faceTokens.size() == 1) {
                            // f v1 v2 v3 ...
                        } else if (faceTokens.size() == 2) {
                            // f v1/vt1 v2/vt2 v3/vt3 ...
                        } else if (faceTokens.size() == 3 && faceTokens.get(0).equals("")) {
                            // f v1//vn1 v2//vn2 v3//vn3 ...
                        } else if (faceTokens.size() == 3) {
                            // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                        }
                    }
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
            vbo[3 * i] = uvBuffer.get(i)[0];
            vbo[3 * i + 1] = uvBuffer.get(i)[1];
        }
        return vbo;
    }

    public int[] getIndexBuffer() {
        int[] vbo = new int[indexBuffer.size() * 3];
        for (int i = 0; i < indexBuffer.size(); i++) {
            vbo[3 * i] = indexBuffer.get(i)[0];
            vbo[3 * i + 1] = indexBuffer.get(i)[1];
            vbo[3 * i + 2] = indexBuffer.get(i)[2];
        }
        return vbo;
    }

}