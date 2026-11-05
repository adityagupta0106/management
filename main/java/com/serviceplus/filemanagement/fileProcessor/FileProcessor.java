package com.serviceplus.filemanagement.fileProcessor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public interface FileProcessor {

    String generateThumbnail(InputStream fileBytes, String uploadId, String bucket);

    String getSupportedType();

    default BufferedImage textToImage(String text){

            int width = 600;
            int height = 400;

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 14));

            int y = 20;
            for (String line : text.split("\n")) {
                g.drawString(line, 10, y);
                y += 15;
                if (y > height) break;
            }

            g.dispose();

            return img;

    }
}
