package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Résultat du calcul d'une aide à partir d'une rubrique et du nombre de
 * membres actifs (bénéficiaire inclus) :
 *   PAR_PERSONNE : part = montantReference        ; total = part × N
 *   FORFAITAIRE  : total = montantReference        ; part = total ÷ N
 */
public record SimulationAideResponse(

        UUID rubriqueId,
        String libelle,
        Aide.TypeAide typeAide,
        RubriqueAide.ModeCalcul modeCalcul,
        BigDecimal montantReference,
        int nbMembresActifs,
        BigDecimal partParMembre,
        BigDecimal total,
        boolean prefinancable
) {}
