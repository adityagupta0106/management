package com.serviceplus.filemanagement.Utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceplus.filemanagement.ExceptionHandler.SPRuntimeError;
import com.serviceplus.filemanagement.model.persistenceEntity.UploadSession;
import com.serviceplus.filemanagement.model.UploadToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Service
public class FileUploadJWTService {

    @Value("${upload.jwt.private-key}")
    private String secretKey;

    @Value("${upload.jwt.token.max.expiry.minutes}")
    private Integer tokenMaxExpiry;

    public String generateUploadToken(UploadSession session) {
        Map<String, Object> claims = getStringObjectMap(session);

        Instant expiresAt = session.getExpiresAt();
        Instant now = Instant.now();

        Instant cutoff = now.plus(Duration.ofMinutes(tokenMaxExpiry));
        boolean usePropKey = isNull(expiresAt) || expiresAt.isAfter(cutoff);

        Instant finalExpiry = usePropKey
                ? cutoff
                : expiresAt;

        session.setExpiresAt(finalExpiry);

        return Jwts.builder()
                .setSubject(session.getUserId().toString())
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(finalExpiry))
                .signWith(getSignInKey())
                .compact();
    }

    @NotNull
    private static Map<String, Object> getStringObjectMap(UploadSession session) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("uploadId", session.getPublicUploadRef());
        claims.put("userId", session.getUserId());
        claims.put("sourceService", session.getSourceService());
        claims.put("referenceType", session.getReferenceType());
        claims.put("referenceId", session.getReferenceId());
        claims.put("category", session.getCategory());

        claims.put("maxFileSize", session.getMaxFileSize());
        claims.put("minFileSize", session.getMinFileSize());
        claims.put("maxChunks", session.getMaxChunks());
        claims.put("allowedMime", session.getAllowedMimeTypes());
        claims.put("chunkedUpload", session.getChunkedUpload());

        claims.put("documentId", session.getDocumentId());
        claims.put("functionality", session.getFunctionality());
        claims.put("tenantId", isNull(session.getTenantId()) ? "" : session.getTenantId());
        claims.put("persistentUploadId",session.getUploadId());
        claims.put("maxWidth",session.getMaxWidth());
        claims.put("minWidth",session.getMinWidth());
        claims.put("maxHeight",session.getMaxHeight());
        claims.put("minHeight",session.getMinHeight());
        return claims;
    }

    public UploadToken validateUploadToken(String token) {

        try {

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            UploadToken payload = new UploadToken();

            payload.setUserId(Long.parseLong(claims.getSubject()));

            payload.setUploadId(claims.get("uploadId", String.class));
            payload.setSourceService(claims.get("sourceService", String.class));
            payload.setReferenceType(claims.get("referenceType", String.class));
            payload.setReferenceId(claims.get("referenceId", String.class));
            payload.setCategory(claims.get("category", String.class));

            payload.setMaxFileSize(getLong(claims, "maxFileSize"));
            payload.setMinFileSize(getLong(claims, "minFileSize"));
            payload.setMaxChunks(getInt(claims, "maxChunks"));

            payload.setAllowedMime(parseMimeList(claims.get("allowedMime")));
            payload.setChunkedUpload(Boolean.TRUE.equals(claims.get("chunkedUpload")));

            payload.setDocumentId(claims.get("documentId", String.class));

            payload.setFunctionality(claims.get("functionality", String.class));
            payload.setTenantId(claims.get("tenantId", String.class));
            payload.setPersistentUploadId(claims.get("persistentUploadId", String.class));
            payload.setMaxHeight(claims.get("maxHeight",Integer.class));
            payload.setMinHeight(claims.get("minHeight",Integer.class));
            payload.setMaxWidth(claims.get("maxWidth",Integer.class));
            payload.setMinWidth(claims.get("minWidth",Integer.class));
            return payload;

        } catch (Exception e) {
            e.printStackTrace();
            throw new SPRuntimeError("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private long getLong(Claims claims, String key) {
        Object val = claims.get(key);
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof Long) return (Long) val;
        if (val instanceof String) return Long.parseLong((String) val);
        return 0L;
    }

    private int getInt(Claims claims, String key) {
        Object val = claims.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }

    private List<String> parseMimeList(Object value) {
        try {
            if (value instanceof List<?>) {
                return ((List<?>) value).stream()
                        .map(Object::toString)
                        .toList();
            }
            if (value instanceof String) {
                return new ObjectMapper().readValue((String) value, List.class);
            }
        } catch (Exception e) {
            return List.of();
        }
        return List.of();
    }
}
