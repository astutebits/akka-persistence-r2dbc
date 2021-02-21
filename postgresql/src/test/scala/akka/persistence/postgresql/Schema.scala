/*
 * Copyright 2020-2021 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.postgresql

object Schema {

  val SQL =
    """
      |CREATE TABLE IF NOT EXISTS public.event
      |(
      |    id             BIGSERIAL UNIQUE,
      |    persistence_id VARCHAR(255)          NOT NULL,
      |    sequence_nr    BIGINT                NOT NULL,
      |    timestamp      BIGINT                NOT NULL,
      |    payload        BYTEA                 NOT NULL,
      |    manifest       VARCHAR(255)          NOT NULL,
      |    writer_uuid    VARCHAR(255)          NOT NULL,
      |    ser_id         INTEGER               NOT NULL,
      |    ser_manifest   VARCHAR(255)          NOT NULL,
      |    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
      |    PRIMARY KEY (persistence_id, sequence_nr)
      |);
      |
      |CREATE TABLE IF NOT EXISTS public.tag
      |(
      |    id       BIGSERIAL,
      |    event_id BIGINT       NOT NULL UNIQUE,
      |    tag      VARCHAR(255) NOT NULL,
      |    PRIMARY KEY (id)
      |);
      |
      |CREATE TABLE IF NOT EXISTS public.snapshot
      |(
      |    persistence_id VARCHAR(255) NOT NULL,
      |    sequence_nr    BIGINT       NOT NULL,
      |    instant        BIGINT       NOT NULL,
      |    snapshot       BYTEA        NOT NULL,
      |    CONSTRAINT pk_snapshot_persistence_id_sequence_number PRIMARY KEY (persistence_id, sequence_nr)
      |);
      |""".stripMargin

}
