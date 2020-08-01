DROP TABLE IF EXISTS public.event;
CREATE TABLE IF NOT EXISTS public.event
(
    id             BIGSERIAL,
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
CREATE UNIQUE INDEX journal_ordering_idx ON public.event (id);

DROP TABLE IF EXISTS public.tag;
CREATE TABLE IF NOT EXISTS public.tag
(
    id       BIGSERIAL,
    event_id BIGINT       NOT NULL,
    tag      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX tag_journal_event_id_idx ON public.tag (event_id);

DROP TABLE IF EXISTS public.snapshot;
CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    timestamp       BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    CONSTRAINT pk_snapshot_persistence_id_sequence_number PRIMARY KEY (persistence_id, sequence_number)
);