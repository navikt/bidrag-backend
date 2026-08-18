-- Add a new combined index
CREATE INDEX idx_engangsbeløp_2
    ON engangsbeløp (type, sak, referanse, skyldner, kravhaver)
    WHERE gjort_ugyldig_av_vedtaksid IS NULL;
