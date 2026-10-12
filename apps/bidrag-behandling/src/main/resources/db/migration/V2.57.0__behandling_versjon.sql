-- Versjon for optimistisk låsing av behandling
alter table behandling add column if not exists versjon bigint not null default 0;
