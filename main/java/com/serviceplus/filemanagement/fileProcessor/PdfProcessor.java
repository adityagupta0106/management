package com.serviceplus.filemanagement.fileProcessor;

import com.serviceplus.filemanagement.storage.ObjectStorage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class PdfProcessor implements FileProcessor {

    @Autowired
    private ObjectStorage objectStorage;

    @Override
    public String generateThumbnail(InputStream inputStream, String uploadId, String bucket) {

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {

            PDFRenderer renderer = new PDFRenderer(doc);

            BufferedImage image = renderer.renderImageWithDPI(0, 100);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);

            String key = "thumbnail-pdf/" + uploadId + ".jpg";

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
        return "";
    }
}
