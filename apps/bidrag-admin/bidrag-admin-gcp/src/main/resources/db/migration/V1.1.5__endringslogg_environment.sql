alter table endringslogg
    add column if not exists aktiv_for_miljø text[] NOT NULL DEFAULT '{}'::text[];