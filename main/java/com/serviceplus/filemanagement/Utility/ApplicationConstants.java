package com.serviceplus.filemanagement.Utility;

public class ApplicationConstants {

    public final static String TEMP_CHUNK_UPLOAD_PREFIX = "TEMP_CHUNK";
    public final static String TEMP_BUCKET = "temp-files";
    public final static Integer CHUNK_EXPIRATION_REDIS_TTL_HOURS = 168;
    public final static String DB_SCHEMA = "schm_sp";
    public final static String UPLOAD_ID_PREFIX = "UP";
    public static final String UPLOAD_STATUS_INITIATED = "INITIATED";
    public static final String UPLOAD_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String UPLOAD_STATUS_UPLOADED = "UPLOADED";
    public static final String UPLOAD_STATUS_FAILED = "FAILED";
    public static final String UPLOAD_STATUS_TEMPORARY = "TEMPORARY";
    public static final String UPLOAD_STATUS_COMPLETED = "COMPLETED";
    public static final String UPLOAD_STATUS_CANCELLED_BY_USER = "CANCELLED_BY_USER";
    public static final String UPLOAD_STATUS_DELETE_PENDING = "DELETE_PENDING";
    public static final String UPLOAD_STATUS_DELETED = "DELETED";
    public static final String UPLOAD_STATUS_REPLACED = "REPLACED";
    public static final String UPLOAD_REF_PREFIX = "fd_";
}
