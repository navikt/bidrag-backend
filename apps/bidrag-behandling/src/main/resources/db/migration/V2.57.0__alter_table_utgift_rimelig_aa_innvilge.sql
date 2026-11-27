alter table utgift
    add column if not exists rimelig_aa_innvilge boolean not null default false;
alter table utgift
    add column if not exists rimelig_aa_innvilge_begrunnelse text;
