-- Antall vedtak fattet
select *
from behandling
where vedtaksid is not null and behandler_enhet = '4820'
order by vedtakstidspunkt desc;

select *
from behandling
where vedtaksid is not null and vedtakstidspunkt >= now() - interval '48 hours'
order by vedtakstidspunkt desc;

select *
from behandling
where deleted is false and behandler_enhet = '4812'
order by opprettet_tidspunkt desc;

select i.ta_med from behandling inner join public.inntekt i on behandling.id = i.behandling_id and i.ta_med = true where vedtakstidspunkt is not null;

--- Statistikk avslag
WITH total AS (
    SELECT COUNT(*) as count
    FROM behandling
    WHERE vedtaksid IS NOT NULL and avslag is not null
),
     avslag_counts AS (
         SELECT avslag, COUNT(*) as count
         FROM behandling
         WHERE vedtaksid IS NOT NULL and avslag is not null
         GROUP BY avslag
     )
SELECT avslag as Avslag, (ceil(avslag_counts.count / (total.count::float) * 100)) as Andel
FROM avslag_counts, total order by Andel desc;

--- Statistikk årsak
WITH total AS (
    SELECT COUNT(*) as count
    FROM behandling
    WHERE vedtaksid IS NOT NULL and aarsak is not null
),
     aarsak_counts AS (
         SELECT aarsak, COUNT(*) as count
         FROM behandling
         WHERE vedtaksid IS NOT NULL and aarsak is not null
         GROUP BY aarsak
     )
SELECT aarsak as Årsak, (ceil(aarsak_counts.count / (total.count::float) * 100)) as Andel
FROM aarsak_counts, total order by Andel desc;

--- Statistikk inntekter
WITH total_behandlinger AS (
    SELECT COUNT(*) as count
    FROM behandling
    WHERE vedtaksid IS NOT NULL
),
     inntekstrapportering_counts AS (
         SELECT i.inntektsrapportering, COUNT(DISTINCT behandling_id) as count
         FROM inntekt i
                  INNER JOIN behandling b ON b.id = i.behandling_id
         WHERE b.vedtaksid IS NOT NULL and i.ta_med = true
         GROUP BY i.inntektsrapportering
     )
SELECT
    inntekstrapportering_counts.inntektsrapportering as type,
    inntekstrapportering_counts.count as antall,
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) || '%' AS "Andel",
    total_behandlinger.count AS "Av totalt vedtak",
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) AS "AndelNumeric"

FROM
    inntekstrapportering_counts, total_behandlinger
order by "AndelNumeric" desc;

--- Statistikk inntekter kilde
WITH total_behandlinger AS (
    SELECT COUNT(*) as count
    FROM behandling
    WHERE vedtaksid IS NOT NULL
),
     inntekstrapportering_counts AS (
         SELECT i.inntektsrapportering, COUNT(DISTINCT behandling_id) as count, kilde
         FROM inntekt i
                  INNER JOIN behandling b ON b.id = i.behandling_id
         WHERE b.vedtaksid IS NOT NULL and i.ta_med = true
         GROUP BY i.inntektsrapportering, kilde
     )
SELECT
    inntekstrapportering_counts.inntektsrapportering as type,
    inntekstrapportering_counts.count as antall,
    CASE
        WHEN inntekstrapportering_counts.kilde = 'OFFENTLIG' THEN 'Offentlig'
        WHEN inntekstrapportering_counts.kilde = 'MANUELL' THEN 'Saksbehandler'
        ELSE inntekstrapportering_counts.kilde
        END as kilde,
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) || '%' AS "Andel",
    total_behandlinger.count AS "Av totalt vedtak",
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) AS "AndelNumeric"
FROM
    inntekstrapportering_counts, total_behandlinger
order by "AndelNumeric" desc;


--- Statistikk inntekter ikke tatt med
WITH total_behandlinger AS (
    SELECT COUNT(*) as count
    FROM behandling
    WHERE vedtaksid IS NOT NULL
),
     inntekstrapportering_counts AS (
         SELECT i.inntektsrapportering, COUNT(DISTINCT behandling_id) as count
         FROM inntekt i
                  INNER JOIN behandling b ON b.id = i.behandling_id
         WHERE b.vedtaksid IS NOT NULL and i.ta_med = false and i.belop > 0
         GROUP BY i.inntektsrapportering
     )
SELECT
    inntekstrapportering_counts.inntektsrapportering as type,
    inntekstrapportering_counts.count as antall,
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) || '%' AS "Andel",
    total_behandlinger.count AS "Av totalt vedtak",
    round((inntekstrapportering_counts.count::numeric / total_behandlinger.count::numeric) * 100, 2) AS "AndelNumeric"

FROM
    inntekstrapportering_counts, total_behandlinger
order by "AndelNumeric" desc;

select * from behandling inner join public.inntekt i on behandling.id = i.behandling_id where vedtakstidspunkt is not null and i.inntektsrapportering = 'KAPITALINNTEKT_EGNE_OPPLYSNINGER' and i.ta_med = true;

--- Statistikk inntekter offentlig vs manuell
WITH total AS (SELECT count(*) as count
               from inntekt i
                        inner join behandling b
                                   on b.id = i.behandling_id and b.vedtaksid is not null and i.ta_med = true),
     manuelle AS (SELECT count(*) as count
                  FROM inntekt i
                           inner join behandling b on b.id = i.behandling_id and b.vedtaksid is not null
                      and i.ta_med = true and i.kilde = 'MANUELL'),
     offentlige AS (SELECT count(*) as count
                    FROM inntekt i
                             inner join behandling b on b.id = i.behandling_id and b.vedtaksid is not null
                        and i.ta_med = true and i.kilde = 'OFFENTLIG'),
    si AS (SELECT count(*) as count
    FROM inntekt i
    inner join behandling b on b.id = i.behandling_id and b.vedtaksid is not null
    and i.ta_med = true and i.inntektsrapportering = 'SAKSBEHANDLER_BEREGNET_INNTEKT')
SELECT manuelle.count                                       as manuelle,
       offentlige.count                                     as offentlige,
       si.count                                     as saksbehandlers_beregnet_inntekt,
       total.count                                          as totalt,
       floor(manuelle.count / (total.count)::float * 100)   as andel_manuelle,
       floor(offentlige.count / (total.count)::float * 100) as andel_offentlige,
       floor(si.count / (total.count)::float * 100) as andel_saksbehandlers_inntekt
FROM total,
     manuelle,
     offentlige,
     si
GROUP BY total.count, manuelle.count, offentlige.count, si.count;



---- OTHERS

select * from inntekt inner join public.behandling b on b.id = inntekt.behandling_id where kilde = 'MANUELL' and ta_med = true and inntektsrapportering = 'UTVIDET_BARNETRYGD' and vedtakstidspunkt is not null;

select *
from behandling
where vedtaksid is not null
  and vedtak_fattet_av != 'J141208'
order by vedtakstidspunkt desc;

WITH behandlinger_med_saksbehandler_beregnet AS (
    SELECT behandling_id
    FROM inntekt
    WHERE inntektsrapportering = 'SAKSBEHANDLER_BEREGNET_INNTEKT' AND ta_med = true
    GROUP BY behandling_id
),
     total_behandlinger_med_vedtak AS (
         SELECT COUNT(DISTINCT id) as count
         FROM behandling
         WHERE vedtakstidspunkt IS NOT NULL
     )
SELECT
    COUNT(*) AS behandlinger_med_saksbehandler_beregnet,
    total_behandlinger_med_vedtak.count AS totalt,
    (COUNT(*)::float / total_behandlinger_med_vedtak.count::float) * 100 AS andel_behandlinger_med_saksbehandler_beregnet
FROM
    behandlinger_med_saksbehandler_beregnet, total_behandlinger_med_vedtak
GROUP BY total_behandlinger_med_vedtak.count;

SELECT
    public.behandling.id,
    vedtak_fattet_av,
    opprettet_av,
    aarsak,
    avslag,
    opprettet_tidspunkt,
    vedtakstidspunkt,
    behandler_enhet,
    AGE(vedtakstidspunkt, opprettet_tidspunkt) AS time_difference,
    i.inntektsrapportering
FROM
    behandling
inner join public.inntekt i on behandling.id = i.behandling_id
WHERE
    i.ta_med = true and
    vedtakstidspunkt IS NOT NULL and AGE(vedtakstidspunkt, opprettet_tidspunkt) < interval '1 day';

--- Gjennomsnittlig saksbehandlingstid
SELECT
    make_interval(secs => AVG(EXTRACT(EPOCH FROM (vedtakstidspunkt - opprettet_tidspunkt))))
FROM
    behandling
WHERE
  vedtak_fattet_av != 'J141208' and
    vedtakstidspunkt IS NOT NULL and AGE(vedtakstidspunkt, opprettet_tidspunkt) < interval '1 day';