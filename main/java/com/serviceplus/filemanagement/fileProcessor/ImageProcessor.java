package com.serviceplus.filemanagement.fileProcessor;

import com.serviceplus.filemanagement.storage.ObjectStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ImageProcessor implements FileProcessor {

    @Autowired
    private ObjectStorage objectStorage;

    @Override
    public String generateThumbnail(InputStream inputStream, String uploadId, String bucket) {

        try {
            BufferedImage original = ImageIO.read(inputStream);

            if (original == null) {
                return null;
            }

            BufferedImage thumb = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = thumb.createGraphics();
            g.drawImage(original, 0, 0, 150, 150, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(thumb, "jpg", out);

            String key = "thumbnail-img/" + uploadId + ".jpg";

            try (InputStream uploadStream = new ByteArrayInputStream(out.toByteArray())) {
                objectStorage.store(bucket, key, uploadStream);
            }

            return key;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getSupportedType() {
        return "image";
    }
}