package com.serviceplus.filemanagement.fileProcessor;

import com.serviceplus.filemanagement.storage.ObjectStorage;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ExcelProcessor implements FileProcessor {

    @Autowired
    private ObjectStorage objectStorage;

    @Override
    public String generateThumbnail(InputStream inputStream, String uploadId, String bucket) {

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StringBuilder preview = new StringBuilder();
            int rowCount = 0;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    preview.append(cell.toString()).append(" | ");
                }
                preview.append("\n");

                if (++rowCount > 10) break; // limit rows
            }

            BufferedImage image = textToImage(preview.toString());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);

            String key = "thumbnail-excel/" + uploadId + ".jpg";

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
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
