-- =====================================================================
-- V31 – Tontine à lot : un membre peut porter plusieurs tours
-- (mise de 60 000 pour un lot de 50 000 = un lot plein + 10 000 dans un
-- lot partagé). L'unicité (session, membre) faisait échouer le figeage.
-- =====================================================================
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        WHERE con.conrelid = 'ordre_beneficiaires'::regclass
          AND con.contype = 'u'
          AND (SELECT array_agg(att.attname::text ORDER BY att.attname)
               FROM unnest(con.conkey) k
               JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = k)
              = ARRAY['membre_id', 'session_id']
    LOOP
        EXECUTE format('ALTER TABLE ordre_beneficiaires DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;
