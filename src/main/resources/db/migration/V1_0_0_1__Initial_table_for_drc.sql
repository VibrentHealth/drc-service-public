-- --------------------------------------------------------------------------------------------------------------------
-- Date          : July 02, 2021            Added By  : Ritesh Khaire
-- JIRA ID       : AC-98669                Comments  : Added Initial table to store drc payload
-- --------------------------------------------------------------------------------------------------------------------

CREATE SCHEMA IF NOT EXISTS drc;


CREATE TABLE `participant_genomic_status_payload` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`requested_timestamp` VARCHAR(64) NOT NULL,
	`next_timestamp` VARCHAR(64) NOT NULL,
	`raw_payload` TEXT NULL,
	`status` VARCHAR(64) NOT NULL,
	`created_on` BIGINT(20) NULL DEFAULT NULL,
	`updated_on` BIGINT(20) NULL DEFAULT NULL,
	PRIMARY KEY (`id`)
);

CREATE TABLE `participant_genomic_status_batch` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`batch_size` INT(11) NOT NULL,
	`batch_payload` TEXT NULL,
	`status` VARCHAR(64) NOT NULL,
	`participant_genomic_status_payload_id` BIGINT(20) NOT NULL,
	`retry_count` INT(11) NOT NULL,
	`created_on` BIGINT(20) NULL DEFAULT NULL,
	`updated_on` BIGINT(20) NULL DEFAULT NULL,
	 PRIMARY KEY (`id`),
	 CONSTRAINT `FK_participant_status_batch_participant_status_payload_id` FOREIGN KEY (`participant_genomic_status_payload_id`) REFERENCES `participant_genomic_status_payload` (`id`)
);
CREATE TABLE IF NOT EXISTS `drc`.`system_properties` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(45) NOT NULL,
	`value` VARCHAR(1024) NULL DEFAULT NULL,
	`created_on` BIGINT(20) NULL DEFAULT NULL,
	`updated_on` BIGINT(20) NULL DEFAULT NULL,
	`created_by_id` BIGINT(20) NULL DEFAULT NULL,
	`updated_by_id` BIGINT(20) NULL DEFAULT NULL,
	`text_value` TEXT NULL,
	 PRIMARY KEY (`id`),
	 UNIQUE INDEX `name` (`name`)
);
