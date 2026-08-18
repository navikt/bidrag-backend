alter table lest_av_bruker
    add column if not exists endringslogg_endring_id bigint REFERENCES endringslogg_endring (id);
alter table endringslogg
    add column if not exists endringstype text[] NOT NULL DEFAULT '{}'::text[];
alter table endringslogg
    add column if not exists innhold text;