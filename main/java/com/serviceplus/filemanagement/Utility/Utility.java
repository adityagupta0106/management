package com.serviceplus.filemanagement.Utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.ulid.UlidCreator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.model.UserSessionObject;
import com.serviceplus.filemanagement.storage.ObjectStorage;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static com.serviceplus.filemanagement.Utility.ApplicationConstants.*;
import static com.serviceplus.filemanagement.Utility.SnowflakeIdGenerator.createUniqueId;
import static java.util.Objects.isNull;

@Component
public class Utility {

    @Autowired
    private ObjectStorage objectStorageInjector;

    @Value("${aesAuthKey}")
    private String aesAuthKey;

    @Value("${backend.gateway.public.domain}")
    private String gatewayPublicDomainConfigured;

    @Value("${upload.session.chunk.split.mb}")
    private Integer uploadSessionChunkSplitMBConfigured;

    private static String AES_AUTH_KEY;
    private static String GATEWAY_PUBLIC_DOMAIN;
    private static Integer CHUNK_SIZE_CONFIGURED;

    private static ObjectStorage objectStorage;

    @PostConstruct
    public void init() {
        objectStorage = objectStorageInjector;
        AES_AUTH_KEY = this.aesAuthKey;
        GATEWAY_PUBLIC_DOMAIN = this.gatewayPublicDomainConfigured;
        CHUNK_SIZE_CONFIGURED = this.uploadSessionChunkSplitMBConfigured;
    }

    public static String getBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String getRedisChunkKey(String uploadId, int chunkIndex) {
        return TEMP_CHUNK_UPLOAD_PREFIX + "/" + uploadId + "/chunk-" + chunkIndex;
    }

    public static String convertToJsonFromList(List<String> list) {
        try {
            return new ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mime types");
        }
    }

    public static String generateUploadId() {
        return UPLOAD_ID_PREFIX.concat(createUniqueId());
    }

    public static UserSessionObject getUserSessionDetails(HttpServletRequest request) {
        String header = request.getHeader("USER-DETAILS");

        if (isEmpty(header))
            return new UserSessionObject();

        return (UserSessionObject) stringToEntity(header, UserSessionObject.class);
    }

    public static Object stringToEntity(String data, Class<?> classs) {
        return new Gson().fromJson(data, classs);
    }

    public static String entityToString(Object data) {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create().toJson(data);
    }

    public static void ensureBucketExists(String bucket) {
        if (!objectStorage.bucketExists(bucket)) {
            objectStorage.createBucket(bucket);
        }
    }

    public static String createBucket(String tenantId, String service, String functionality) {
        return tenantId.concat("-").concat(service).concat("-").concat(functionality);
    }

    public static String generateFileSignature(File file) {

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int read;

            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] hashBytes = digest.digest();

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new SPRuntimeError("Hash generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static String getExtensionFromMime(String mimeType) {

        if (mimeType == null) return "";

        try {
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType type = allTypes.forName(mimeType);

            String ext = type.getExtension();

            return ext != null ? ext : "";

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String AESEncrypt(String content, String authKey) {
        try {
            authKey = isNull(authKey) ? AES_AUTH_KEY : authKey;
            SecretKeySpec skeySpec = new SecretKeySpec(authKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] encrypted = cipher.doFinal(content.getBytes("UTF-8"));
            String finalString = org.apache.commons.codec.binary.Base64.encodeBase64String(encrypted);

            return finalString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String AESDecrypt(String content, String authKey) {
        try {
            authKey = isNull(authKey) ? AES_AUTH_KEY : authKey;
            SecretKeySpec skeySpec = new SecretKeySpec(authKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            byte[] original = cipher.doFinal(org.apache.commons.codec.binary.Base64.decodeBase64(content));

            return new String(original);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String resolveBaseUrl(HttpServletRequest request) {
        return GATEWAY_PUBLIC_DOMAIN + "/spgateway/object";
    }

    public static String getHost(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.HOST);
    }

    public static Long getChunkSizeBytes() {
        return CHUNK_SIZE_CONFIGURED * 1024L * 1024L;
    }

    public static String resolveMimeType(File tempFile) {

        String mimeType;

        try (InputStream is = new FileInputStream(tempFile)) {
            mimeType = detectMimeType(is);

        } catch (IOException e) {
            throw new SPRuntimeError("We could not determine the file type. Please upload a valid file [1].", HttpStatus.BAD_REQUEST);
        }

        if (mimeType == null || mimeType.isBlank()) {
            throw new SPRuntimeError("We could not determine the file type. Please upload a valid file [2].", HttpStatus.BAD_REQUEST);
        }

        // Handle JSON files that Tika detects as text/plain
        if (MediaType.TEXT_PLAIN_VALUE.equalsIgnoreCase(mimeType)) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(tempFile);

                if (jsonNode != null) {
                    return MediaType.APPLICATION_JSON_VALUE;
                }
            } catch (Exception ignored) {

            }
        }

        // Preserve existing Office document detection
        if ("application/x-tika-ooxml".equalsIgnoreCase(mimeType)) {

            mimeType = detectOfficeOpenXmlType(tempFile);

            if (mimeType == null) {
                throw new SPRuntimeError(
                        "We could not determine the Office document type.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        return mimeType;
    }

    public static String detectMimeType(InputStream inputStream) {

        try {
            Tika tika = new Tika();
            return tika.detect(inputStream);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String detectOfficeOpenXmlType(File file) {

        try (OPCPackage packageFile = OPCPackage.open(file)) {

            boolean hasWord = false;
            boolean hasExcel = false;
            boolean hasPowerPoint = false;

            for (PackagePart part : packageFile.getParts()) {

                String contentType = part.getContentType();

                if (contentType == null) {
                    continue;
                }

                if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml")) {
                    hasWord = true;
                }

                if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml")) {
                    hasExcel = true;
                }

                if (contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml")) {
                    hasPowerPoint = true;
                }
            }

            if (hasExcel) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }

            if (hasWord) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }

            if (hasPowerPoint) {
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generatePublicUploadRef() {

        return UPLOAD_REF_PREFIX
                + UlidCreator.getMonotonicUlid()
                .toString();
    }
}
