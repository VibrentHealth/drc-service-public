-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Feb 17, 2022             Added By  : Vishnu Kotu
-- JIRA ID       : AC-109441                Comments  : Added table to store the Account update and program update info data that are successfully send to DRC
-- --------------------------------------------------------------------------------------------------------------------
 -- DRC send data details
CREATE TABLE IF NOT EXISTS `drc`.`drc_synced_status` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `vibrent_id` BIGINT(20) NOT NULL,
    `type` varchar(64) NOT NULL,
    `data` JSON DEFAULT NULL,
	`created_on` BIGINT(20) NULL DEFAULT NULL,
	`updated_on` BIGINT(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `drc_synced_status_unique_vibrent_id_type` (`vibrent_id`,`type`)
);