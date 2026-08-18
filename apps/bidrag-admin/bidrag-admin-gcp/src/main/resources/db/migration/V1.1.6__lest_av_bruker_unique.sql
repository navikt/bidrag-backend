CREATE UNIQUE INDEX IF NOT EXISTS idx_endringslogg_endring_person_unique
    ON lest_av_bruker (endringslogg_endring_id, person_id);