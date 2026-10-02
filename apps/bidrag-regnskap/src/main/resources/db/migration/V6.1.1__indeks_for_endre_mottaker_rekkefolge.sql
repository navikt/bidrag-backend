CREATE INDEX endre_mottaker_sak_opprettet_index
    ON endre_mottaker (saksnummer, opprettet_tidspunkt, id)
    WHERE godkjent_av_skatt_tidspunkt IS NULL;
