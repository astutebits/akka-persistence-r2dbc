CREATE TABLE IF NOT EXISTS public.event
(
    id             BIGSERIAL UNIQUE,
    persistence_id VARCHAR(255)          NOT NULL,
    sequence_nr    BIGINT                NOT NULL,
    timestamp      BIGINT                NOT NULL,
    payload        BYTEA                 NOT NULL,
    manifest       VARCHAR(255)          NOT NULL,
    writer_uuid    VARCHAR(255)          NOT NULL,
    ser_id         INTEGER               NOT NULL,
    ser_manifest   VARCHAR(255)          NOT NULL,
    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (persistence_id, sequence_nr)
);

CREATE TABLE IF NOT EXISTS public.tag
(
    id       BIGSERIAL,
    event_id BIGINT       NOT NULL UNIQUE,
    tag      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id VARCHAR(255) NOT NULL,
    sequence_nr    BIGINT       NOT NULL,
    instant        BIGINT       NOT NULL,
    snapshot       BYTEA        NOT NULL,
    CONSTRAINT pk_snapshot_persistence_id_sequence_number PRIMARY KEY (persistence_id, sequence_nr)
);