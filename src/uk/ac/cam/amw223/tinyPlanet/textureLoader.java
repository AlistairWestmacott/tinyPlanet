package uk.ac.cam.amw223.tinyPlanet;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class textureLoader {

    private byte[] data;
    private int width;
    private int height;

    public textureLoader(String path) {
        try {
            loadTexture(path);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadTexture(String path) throws IOException {
        File imageFile = new File(path);
        BufferedImage image = ImageIO.read(imageFile);
        Color c;

        width = image.getWidth();
        height = image.getHeight();

        data = new byte[width * height * 3];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                c = new Color(image.getRGB(x, y));
                data[3 * (x + y * width)] = (byte) c.getRed();
                data[3 * (x + y * width) + 1] = (byte) c.getGreen();
                data[3 * (x + y * width) + 2] = (byte) c.getBlue();
            }
        }
    }

    public ByteBuffer buffer() {
        ByteBuffer buf = BufferUtils.createByteBuffer(width*height*3);

        for (int i = width * height; i > 0 ; i-- ) {
            buf.put(data[3*i-3]);
            buf.put(data[3*i-2]);
            buf.put(data[3*i-1]);
        }

        buf.flip();
        return buf;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}