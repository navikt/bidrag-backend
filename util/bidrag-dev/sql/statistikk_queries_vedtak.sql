select substring(referanse, 9, 41) as inntektstype, innhold ->> 'manueltRegistrert' as "Manuelt registrert", count(*) as Antall
from grunnlag g
where innhold ->> 'valgt' = 'true'
group by inntektstype, "Manuelt registrert"
order by Antall desc, inntektstype
;

--- Inntekter manuell vs offentlig
select innhold ->> 'inntektsrapportering' as inntektstype,
       innhold ->> 'manueltRegistrert'    as "Manuelt registrert",
       count(distinct vedtaksid)          as Antall
from grunnlag g
where innhold ->> 'valgt' = 'true'
  and type = 'INNTEKT_RAPPORTERING_PERIODE'
group by inntektstype, "Manuelt registrert"
order by Antall desc, inntektstype
;

--- Inntekter manuell vs offentlig 2
select innhold ->> 'inntektsrapportering' as inntektstype,
       count(distinct vedtaksid)          as Antall,
       CASE
           WHEN innhold ->> 'manueltRegistrert' = 'true' THEN 'Manuelt'
           WHEN innhold ->> 'manueltRegistrert' = 'false' THEN 'Offentlig'
           END                            as kilde
from grunnlag g
where innhold ->> 'valgt' = 'true'
  and type = 'INNTEKT_RAPPORTERING_PERIODE'

group by inntektstype, kilde
order by Antall desc, inntektstype
;

--- Inntekter manuell vs offentlig graf
select innhold ->> 'inntektsrapportering' as inntektstype,
       count(distinct g.vedtaksid)        as Antall,
       v.opprettet_tidspunkt::date          AS day,
       CASE
           WHEN innhold ->> 'manueltRegistrert' = 'true' THEN 'Manuelt'
           WHEN innhold ->> 'manueltRegistrert' = 'false' THEN 'Offentlig'
           END                            as kilde
from grunnlag g
         inner join public.vedtak v on v.vedtaksid = g.vedtaksid
where innhold ->> 'valgt' = 'true'
  and g.type = 'INNTEKT_RAPPORTERING_PERIODE'

group by inntektstype, kilde, opprettet_tidspunkt
order by Antall desc, inntektstype
;


-- Antall vedtak fattet gjennom ny løsning
select count(*)
from vedtak
         inner join public.behandlingsreferanse b on vedtak.vedtaksid = b.vedtaksid
where b.kilde = 'BEHANDLING_ID';

-- Antall forskudd vedtak fattet gjennom bisys
select count(*)
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-03-30';

-- Antall forskudd vedtak fattet gjennom bisys
select count(distinct s.vedtaksid) as Antall,
       opprettet_tidspunkt::date   AS day
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where kildeapplikasjon = 'bisys'
  and enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-03-30'
group by day
order by day;

-- Antall forskudd vedtak fattet gjennom ny løsning
select count(*)                  as Antall,
       opprettet_tidspunkt::date AS day
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where kildeapplikasjon = 'bidrag-behandling'
  and enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-04-30'
group by day
order by day;

WITH vedtak_ny_løsning AS (select count(distinct s.vedtaksid) as Antall,
                                  opprettet_tidspunkt::date   AS day
                           from vedtak
                                    inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
                           where kildeapplikasjon = 'bidrag-behandling'
                             and enhetsnummer != '9999'
                             and s.type = 'FORSKUDD'
                             and opprettet_tidspunkt::date >= '2024-04-30'
                           group by day),
     vedtak_gammel_løsning AS (select count(distinct s.vedtaksid) as Antall,
                                      opprettet_tidspunkt::date   AS day
                               from vedtak
                                        inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
                               where kildeapplikasjon = 'bisys'
                                 and enhetsnummer != '9999'
                                 and s.type = 'FORSKUDD'
                                 and opprettet_tidspunkt::date >= '2024-04-30'
                               group by day)
SELECT vedtak_ny_løsning.Antall     AS "Antall vedtak fattet ny",
       vedtak_gammel_løsning.Antall AS "Antall vedtak fattet",
       vedtak_ny_løsning.day        AS dager_ny,
       vedtak_gammel_løsning.day    AS dager_gammel
FROM vedtak_ny_løsning,
     vedtak_gammel_løsning
where vedtak_ny_løsning.day = vedtak_gammel_løsning.day
group by dager_ny, dager_gammel, "Antall vedtak fattet ny", "Antall vedtak fattet"
order by dager_ny, dager_gammel;



select count(distinct s.vedtaksid) as Antall,
       enhetsnummer                as enhet
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where kildeapplikasjon = 'bidrag-behandling'
  and enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-04-30'
group by enhetsnummer;

select count(distinct s.vedtaksid) as Antall,
       enhetsnummer                as enhet
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where kildeapplikasjon = 'bidrag-behandling'
  and enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-06-01'
group by enhetsnummer
order by Antall desc;


select *
from vedtak
         inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
where kildeapplikasjon = 'bidrag-behandling'
  and enhetsnummer != '9999'
  and s.type = 'FORSKUDD'
  and opprettet_tidspunkt::date >= '2024-04-30';


WITH vedtak_ny_løsning AS (select count(distinct s.vedtaksid) as Antall,
                                  enhetsnummer                as enhet
                           from vedtak
                                    inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
                           where kildeapplikasjon = 'bidrag-behandling'
                             and enhetsnummer != '9999'
                             and s.type = 'FORSKUDD'
                             and opprettet_tidspunkt::date >= '2024-04-30'
                           group by enhetsnummer),
     vedtak_gammel_løsning AS (select count(distinct s.vedtaksid) as Antall,
                                      enhetsnummer                as enhet
                               from vedtak
                                        inner join public.stønadsendring s on vedtak.vedtaksid = s.vedtaksid
                               where kildeapplikasjon = 'bisys'
                                 and enhetsnummer != '9999'
                                 and s.type = 'FORSKUDD'
                                 and opprettet_tidspunkt::date >= '2024-04-30'
                               group by enhetsnummer
                               order by Antall desc)
SELECT vedtak_ny_løsning.Antall     AS "Antall vedtak fattet ny",
       vedtak_gammel_løsning.Antall AS "Antall vedtak fattet",
       vedtak_ny_løsning.enhet      AS enhet_ny,
       vedtak_gammel_løsning.enhet  AS enhet_gammel
FROM vedtak_ny_løsning,
     vedtak_gammel_løsning
where vedtak_ny_løsning.enhet = vedtak_gammel_løsning.enhet
group by enhet_ny, enhet_gammel, "Antall vedtak fattet ny", "Antall vedtak fattet";