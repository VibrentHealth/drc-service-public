-- --------------------------------------------------------------------------------------------------------------------
-- Date          : May 11, 2022             Added By  : Vishnu Kotu
-- JIRA ID       : AC-114297                Comments  : Added table to store the Account update and program update info sync retry table
-- --------------------------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `drc`.`update_info_sync_retry_entry` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `vibrent_id` BIGINT(20) NOT NULL,
    `type` varchar(64) NOT NULL,
    `payload` JSON DEFAULT NULL,
    `retry_count` BIGINT(20) NOT NULL,
    `error_details` TEXT NOT NULL,
	`created_on` BIGINT(20) NULL DEFAULT NULL,
	`updated_on` BIGINT(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
--  , KEY `drc_synced_status_unique_vibrent_id_type` (`vibrent_id`,`type`)
);