-- Drop the existing UNIQUE constraint
ALTER TABLE stønad DROP CONSTRAINT IF EXISTS stønad_type_skyldner_kravhaver_sak_key;

-- Add a new UNIQUE constraint with desired column order
ALTER TABLE stønad
    ADD CONSTRAINT stønad_type_sak_skyldner_kravhaver_key
        UNIQUE (type, sak, skyldner, kravhaver);
