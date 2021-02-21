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

package akka.persistence.mysql

object Schema {

  val SQL: String =
    """
      |CREATE TABLE IF NOT EXISTS event
      |(
      |    id             SERIAL,
      |    persistence_id VARCHAR(255)          NOT NULL,
      |    sequence_nr    BIGINT UNSIGNED       NOT NULL,
      |    timestamp      BIGINT                NOT NULL,
      |    payload        BLOB                  NOT NULL,
      |    manifest       VARCHAR(255)          NOT NULL,
      |    writer_uuid    VARCHAR(255)          NOT NULL,
      |    ser_id         INTEGER               NOT NULL,
      |    ser_manifest   VARCHAR(255)          NOT NULL,
      |    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
      |    PRIMARY KEY (persistence_id, sequence_nr)
      |);
      |
      |CREATE TABLE IF NOT EXISTS tag
      |(
      |    id       SERIAL,
      |    event_id BIGINT UNSIGNED NOT NULL,
      |    tag      VARCHAR(255)    NOT NULL,
      |    PRIMARY KEY (id),
      |    UNIQUE KEY fk_event_id (event_id)
      |);
      |
      |CREATE TABLE IF NOT EXISTS snapshot
      |(
      |    `persistence_id` VARCHAR(255)    NOT NULL,
      |    `sequence_nr`    BIGINT UNSIGNED NOT NULL,
      |    `instant`        BIGINT UNSIGNED NOT NULL,
      |    `snapshot`       BLOB            NOT NULL,
      |    PRIMARY KEY (persistence_id, sequence_nr)
      |);
      |""".stripMargin

}
