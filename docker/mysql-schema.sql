DROP TABLE IF EXISTS event;
CREATE TABLE IF NOT EXISTS event
(
    id             SERIAL,
    persistence_id VARCHAR(255)          NOT NULL,
    sequence_nr    BIGINT UNSIGNED       NOT NULL,
    payload        BLOB                  NOT NULL,
    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (persistence_id, sequence_nr)
);

DROP TABLE IF EXISTS tag;
CREATE TABLE IF NOT EXISTS tag
(
    id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, -- SERIAL?
    event_id BIGINT UNSIGNED NOT NULL,
    tag      VARCHAR(255)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY fk_event_id (event_id)
);

CREATE TABLE IF NOT EXISTS snapshot
(
    `persistence_id` VARCHAR(255)    NOT NULL,
    `seq_nr`         BIGINT UNSIGNED NOT NULL,
    `time`           BIGINT UNSIGNED NOT NULL,
    `snapshot`       BLOB            NOT NULL,
    PRIMARY KEY (persistence_id, seq_nr)
);
