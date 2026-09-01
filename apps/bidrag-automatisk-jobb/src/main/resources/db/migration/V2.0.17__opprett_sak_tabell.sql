CREATE TABLE IF NOT EXISTS sak
(
    id                  integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    saksnummer          text      NOT NULL UNIQUE,
    bidragspliktig      text,
    bidragsmottaker     text,
    opprettet_tidspunkt timestamp NOT NULL DEFAULT current_timestamp,
    endret_tidspunkt    timestamp NOT NULL DEFAULT current_timestamp
);

CREATE TABLE IF NOT EXISTS sak_barn
(
    id               integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    sak_id           integer   NOT NULL REFERENCES sak (id) ON DELETE CASCADE,
    kravhaver        text      NOT NULL,
    reell_mottaker   text,
    endret_tidspunkt timestamp NOT NULL DEFAULT current_timestamp,
    CONSTRAINT unik_sak_kravhaver UNIQUE (sak_id, kravhaver)
);

CREATE INDEX IF NOT EXISTS sak_bidragspliktig_index ON sak (bidragspliktig);
CREATE INDEX IF NOT EXISTS sak_barn_sak_id_index ON sak_barn (sak_id);

