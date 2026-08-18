alter table lest_av_bruker add column if not exists lestetid_varighet_ms bigint;
alter table person add column if not exists enhet text;