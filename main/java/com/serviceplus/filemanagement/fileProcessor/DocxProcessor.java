package com.serviceplus.filemanagement.fileProcessor;

import com.serviceplus.filemanagement.storage.ObjectStorage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class DocxProcessor implements FileProcessor {

    @Autowired
    private ObjectStorage objectStorage;

    @Override
    public String generateThumbnail(InputStream inputStream, String uploadId, String bucket) {

        try (XWPFDocument doc = new XWPFDocument(inputStream)) {

            StringBuilder previewText = new StringBuilder();

            for (XWPFParagraph p : doc.getParagraphs()) {
                previewText.append(p.getText()).append("\n");

                if (previewText.length() > 500) break;
            }

            BufferedImage image = textToImage(previewText.toString());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);

            String key = "thumbnail-doc/" + uploadId + ".jpg";

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
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
}
