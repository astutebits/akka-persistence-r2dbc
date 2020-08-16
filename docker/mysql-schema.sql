DROP TABLE IF EXISTS event;
CREATE TABLE IF NOT EXISTS event
(
    id             SERIAL,
    persistence_id VARCHAR(255)          NOT NULL,
    sequence_nr    BIGINT UNSIGNED       NOT NULL,
    timestamp      BIGINT                NOT NULL,
    payload        BLOB                  NOT NULL,
    manifest       VARCHAR(255)          NOT NULL,
    writer_uuid    VARCHAR(255)          NOT NULL,
    ser_id         INTEGER               NOT NULL,
    ser_manifest   VARCHAR(255)          NOT NULL,
    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (persistence_id, sequence_nr)
);

DROP TABLE IF EXISTS tag;
CREATE TABLE IF NOT EXISTS tag
(
    id       SERIAL,
    event_id BIGINT UNSIGNED NOT NULL,
    tag      VARCHAR(255)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY fk_event_id (event_id)
);

CREATE TABLE IF NOT EXISTS snapshot
(
    `persistence_id` VARCHAR(255)    NOT NULL,
    `sequence_nr`    BIGINT UNSIGNED NOT NULL,
    `instant`        BIGINT UNSIGNED NOT NULL,
    `snapshot`       BLOB            NOT NULL,
    PRIMARY KEY (persistence_id, sequence_nr)
);
