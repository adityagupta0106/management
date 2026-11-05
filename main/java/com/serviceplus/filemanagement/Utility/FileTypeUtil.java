package com.serviceplus.filemanagement.Utility;

import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.HttpStatus;

import java.util.*;

public class FileTypeUtil {
    //Allowed is from tenant configuration
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".zip", ".pdf", ".doc", ".docx",
            ".ppt", ".pptx",
            ".tar", ".gz", ".tgz", ".rar",
            ".xls", ".xlsx",
            ".rtf", ".txt", ".xml",
            ".png", ".jpeg", ".jpg", ".gif",
            ".kml", ".kmz",
            ".dwg", ".dxf" , ".json"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".dll", ".jar", ".war", ".ear",
            ".class", ".java", ".jsp", ".jsf", ".properties", ".bat", ".sh"
    );

    private static final Set<String> RAW_EXTENSIONS = new HashSet<>(Arrays.asList(
            "3fr","ari","arw","bay","braw","crw","cr2","cr3","cap","data","dcs","dcr","dng",
            "drf","eip","erf","fff","gpr","iiq","k25","kdc","mdc","mef","mos","mrw","nef",
            "nrw","obm","orf","pef","ptx","pxn","r3d","raf","raw","rwl","rw2","rwz","sr2",
            "srf","srw","tif","x3f","avif","tiff","psd"
    ));

    private static final Set<String> BLOCKED_MIME_TYPES = Set.of(
            "application/x-msdownload",     // .exe
            "application/x-dosexec",        // .exe alt
            "application/java-archive",     // .jar
            "application/x-sh",             // .sh
            "application/x-bat",            // .bat
            "application/octet-stream",     // generic executable risk
            "text/x-java-source",           // .java
            "application/x-class",          // .class
            "application/x-msdos-program"   // .exe alt
    );

    public static String resolveExtension(String mimeType, String originalFileName) {

        String ext = "";

        try {
            if (mimeType != null) {
                MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
                MimeType type = allTypes.forName(mimeType);
                ext = type.getExtension();
            }
        } catch (Exception ignored) {}

        if ((ext == null || ext.isBlank()) && originalFileName != null) {
            originalFileName = sanitizeFileName(originalFileName);

            if (isSuspiciousFileName(originalFileName)) {
                throw new SPRuntimeError("Suspicious filename", HttpStatus.NOT_ACCEPTABLE);
            }

            ext = extractExtension(originalFileName);
        }

        if (ext == null || ext.isBlank()) {
            throw new SPRuntimeError("Cannot determine file extension",HttpStatus.FAILED_DEPENDENCY);
        }

        ext = ext.toLowerCase();

        if (BLOCKED_EXTENSIONS.contains(ext)) {
            throw new SPRuntimeError("Blocked file type: " + ext,HttpStatus.BAD_REQUEST);
        }

        if (!ALLOWED_EXTENSIONS.contains(ext) && !RAW_EXTENSIONS.contains(ext.replace(".", ""))) {
            throw new SPRuntimeError("Unsupported file type: " + ext,HttpStatus.BAD_REQUEST);
        }

        return ext;
    }

    public static String sanitizeFileName(String fileName) {

        if (fileName == null) return null;

        fileName = fileName.replace("\\", "/");
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static String extractExtension(String fileName) {

        int lastDot = fileName.lastIndexOf(".");
        if (lastDot == -1) return "";

        return fileName.substring(lastDot).toLowerCase();
    }

    public static boolean isSuspiciousFileName(String fileName) {

        int dotCount = fileName.length() - fileName.replace(".", "").length();

        if (dotCount > 1) {
            String lower = fileName.toLowerCase();

            return lower.contains(".exe.") ||
                    lower.contains(".dll.") ||
                    lower.contains(".jar.") ||
                    lower.contains(".bat.") ||
                    lower.contains(".sh.");
        }

        return false;
    }

    public static List<String> sanitizeAllowedMime(List<String> requestedMime) {

        if (requestedMime == null || requestedMime.isEmpty()) {
            return List.of(); // means "no restriction from client"
        }

        List<String> result = new ArrayList<>();

        for (String mime : requestedMime) {

            if (mime == null || mime.isBlank()) continue;

            mime = mime.trim().toLowerCase();

            if (BLOCKED_MIME_TYPES.contains(mime)) {
                throw new SPRuntimeError("Blocked MIME type: " + mime,HttpStatus.BAD_REQUEST);
            }

            result.add(mime);
        }

        return result;
    }
}
