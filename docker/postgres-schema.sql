DROP TABLE IF EXISTS public.journal_event;
CREATE TABLE IF NOT EXISTS public.journal_event (
  index BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_nr BIGINT NOT NULL,
  event BYTEA NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  PRIMARY KEY(persistence_id, sequence_nr)
);
CREATE UNIQUE INDEX journal_ordering_idx ON public.journal_event(index);

DROP TABLE IF EXISTS public.tag;
CREATE TABLE IF NOT EXISTS public.tag (
 index bigserial,
 event_index BIGINT NOT NULL,
 tag VARCHAR(255) NOT NULL,
 PRIMARY KEY (index)
);
CREATE UNIQUE INDEX tag_journal_event_index_idx ON public.tag(event_index);

DROP TABLE IF EXISTS public.snapshot;
CREATE TABLE IF NOT EXISTS public.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  timestamp BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  CONSTRAINT pk_snapshot_persistence_id_sequence_number PRIMARY KEY(persistence_id, sequence_number)
);