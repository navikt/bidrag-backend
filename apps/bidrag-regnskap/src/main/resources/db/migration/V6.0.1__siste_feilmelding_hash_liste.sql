ALTER TABLE oppdrag
    DROP COLUMN siste_feilmelding_hash;

ALTER TABLE oppdrag
    ADD COLUMN siste_feilmelding_hash INT[];
