package com.tontinepro.tontinepro_backend.api.historique.dto;

import java.util.List;

public record ImporterHistoriqueResponse(
        int beneficiairesImportes,
        int cotisationsImportees,
        int absencesImportees,
        int sanctionsImportees,
        int epargnesInitialisees,
        int pretsImportes,
        List<String> avertissements
) {}