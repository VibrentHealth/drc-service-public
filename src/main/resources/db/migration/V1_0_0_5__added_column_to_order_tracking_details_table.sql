-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Dec 03, 2021             Added By  : Ritesh Khaire
-- JIRA ID       : AC-106490               Comments  : Added new column to DRC order details table
-- --------------------------------------------------------------------------------------------------------------------

ALTER TABLE `order_tracking_details`
    DROP COLUMN is_drc_request_sent;
ALTER TABLE `order_tracking_details`
	ADD COLUMN `last_message_status` VARCHAR(50) NULL DEFAULT NULL;