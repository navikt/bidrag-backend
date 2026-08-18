alter table grunnlagspakke add column if not exists kildeapplikasjon text not null default '';
alter table grunnlagspakke add column if not exists opprettet_av_navn text;