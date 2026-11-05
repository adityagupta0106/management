create database sp_file_mgmt;

create schema schm_sp;
CREATE TABLE IF NOT EXISTS schm_sp.upload_session
(
    upload_id character varying COLLATE pg_catalog."default" NOT NULL,
    user_id integer NOT NULL,
    reference_id character varying COLLATE pg_catalog."default" NOT NULL,
    category character varying COLLATE pg_catalog."default" NOT NULL,
    max_file_size bigint,
    min_file_size bigint,
    max_chunks integer,
    status character varying COLLATE pg_catalog."default" NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    source_service character varying COLLATE pg_catalog."default",
    reference_type character varying COLLATE pg_catalog."default",
    allowed_mime_types text COLLATE pg_catalog."default",
    chunked_upload boolean,
    document_id character varying COLLATE pg_catalog."default",
    functionality character varying COLLATE pg_catalog."default",
    tenant_id character varying COLLATE pg_catalog."default",
    CONSTRAINT upload_session_pkey PRIMARY KEY (upload_id)
);



CREATE TABLE IF NOT EXISTS schm_sp.document_metadata
(
    upload_id character varying COLLATE pg_catalog."default" NOT NULL,
    document_id character varying COLLATE pg_catalog."default",
    user_id integer NOT NULL,
    reference_type character varying COLLATE pg_catalog."default" NOT NULL,
    reference_id character varying COLLATE pg_catalog."default" NOT NULL,
    original_file_name text COLLATE pg_catalog."default",
    category character varying COLLATE pg_catalog."default" NOT NULL,
    bucket_name character varying COLLATE pg_catalog."default" NOT NULL,
    object_key text COLLATE pg_catalog."default" NOT NULL,
    thumbnail_key text COLLATE pg_catalog."default",
    file_name text COLLATE pg_catalog."default",
    mime_type character varying COLLATE pg_catalog."default",
    size_bytes bigint,
    file_hash character varying COLLATE pg_catalog."default",
    status character varying COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    tenant_id character varying COLLATE pg_catalog."default",
    CONSTRAINT document_metadata_pkey PRIMARY KEY (upload_id)
);

CREATE INDEX IF NOT EXISTS upload_session_idx   ON schm_sp.upload_session USING btree(upload_id,status,tenant_id);
CREATE INDEX IF NOT EXISTS document_metadata_idx   ON schm_sp.document_metadata USING btree(upload_id,object_key,thumbnail_key,file_hash,tenant_id,status);


--------------
alter table schm_sp.upload_session add column updated_at timestamp with time zone;
alter table schm_sp.upload_session add column previous_upload_id character varying;
alter table schm_sp.upload_session add column public_upload_ref character varying;
alter table schm_sp.upload_session add column replaced_by_upload_id character varying;
ALTER TABLE schm_sp.upload_session ALTER COLUMN user_id TYPE BIGINT;


ALTER TABLE schm_sp.document_metadata ALTER COLUMN user_id TYPE BIGINT;

ALTER TABLE schm_sp.upload_session ALTER COLUMN user_id TYPE BIGINT;