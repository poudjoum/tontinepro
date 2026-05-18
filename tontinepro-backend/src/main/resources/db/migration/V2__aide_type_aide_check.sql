-- V2 – Contrainte CHECK sur type_aide (enum TypeAide)
ALTER TABLE aides
    ADD CONSTRAINT chk_aides_type_aide
        CHECK (type_aide IN (
            'DECES',
            'MALADIE',
            'ACCIDENT',
            'MARIAGE',
            'NAISSANCE',
            'SCOLARITE',
            'CALAMITE',
            'AUTRE'
        ));