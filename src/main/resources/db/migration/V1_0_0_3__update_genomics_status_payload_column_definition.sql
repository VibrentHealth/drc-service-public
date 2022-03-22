-- --------------------------------------------------------------------------------------------------------------------
-- Date          : Aug 04, 2021            Added By  : Ritesh Khaire
-- JIRA ID       : AC-101014                Comments  : Added  script to updated genomics status payload column definition
-- --------------------------------------------------------------------------------------------------------------------
 -- script to update column definition
 ALTER TABLE participant_genomic_status_payload MODIFY raw_payload json;
 ALTER TABLE participant_genomic_status_batch MODIFY batch_payload json;