-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Nov 18, 2021             Added By  : Vishnu Kotu
-- JIRA ID       : AC-101832                Comments  : Added DRC order details table
-- --------------------------------------------------------------------------------------------------------------------
 -- DRC order tracking details table
CREATE TABLE IF NOT EXISTS `drc`.`order_tracking_details` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT(20) DEFAULT NULL,
  `participant_id` VARCHAR(64) DEFAULT NULL,
  `user_id` BIGINT(20) DEFAULT NULL,
  `identifier_type` VARCHAR(64) DEFAULT NULL,
  `identifier` VARCHAR(100) NOT NULL,
  `is_drc_request_sent` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique-identifier` (`identifier`),
  KEY `idx-oderid-identifiertype` (`order_id`,`identifier_type`)
);