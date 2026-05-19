package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de calcul des dates de réunion selon les règles de périodicité avancées.
 */
@Service
public class PeriodiciteService {

    /**
     * Calcule la prochaine date de réunion à partir de 'depuis' (exclusive).
     */
    public LocalDate prochainDate(Tontine.TypeReglePeriodicite regle, Integer jourReference, LocalDate depuis) {
        return switch (regle) {
            case HEBDOMADAIRE -> prochainSamedi(depuis);
            case MENSUEL_JOUR_FIXE -> prochainJourFixe(depuis, jourReference != null ? jourReference : 1);
            case MENSUEL_DERNIER_SAMEDI -> prochainDernierJourSemaine(depuis, DayOfWeek.SATURDAY);
            case MENSUEL_DERNIER_DIMANCHE -> prochainDernierJourSemaine(depuis, DayOfWeek.SUNDAY);
            case MENSUEL_PREMIER_DIMANCHE_APRES -> prochainPremierDimancheApres(depuis, jourReference != null ? jourReference : 1);
            case MENSUEL_DIMANCHE_SUR_OU_APRES -> prochainDimancheSurOuApres(depuis, jourReference != null ? jourReference : 1);
            case TRIMESTRIEL_JOUR_FIXE -> prochainJourFixePeriode(depuis, jourReference != null ? jourReference : 1, 3);
            case SEMESTRIEL_JOUR_FIXE -> prochainJourFixePeriode(depuis, jourReference != null ? jourReference : 1, 6);
            case ANNUEL_JOUR_FIXE -> prochainJourFixePeriode(depuis, jourReference != null ? jourReference : 1, 12);
            case DATE_MANUELLE -> null; // Sera fourni manuellement
        };
    }

    /**
     * Calcule la date de fin d'une session.
     * dateFin = dateDebut + (nombreMembres - 1) × intervalle
     */
    public LocalDate calculerDateFin(Tontine tontine, LocalDate dateDebut, int nombreMembres) {
        if (nombreMembres <= 1) {
            return dateDebut;
        }
        LocalDate courant = dateDebut;
        for (int i = 1; i < nombreMembres; i++) {
            courant = prochainDate(tontine.getTypeReglePeriodicite(), tontine.getJourReference(), courant);
            if (courant == null) {
                return null;
            }
        }
        return courant;
    }

    /**
     * Calcule toutes les dates de bénéfice pour chaque position dans la rotation.
     * La première date est dateDebut, les suivantes sont calculées successivement.
     */
    public List<LocalDate> calculerDatesBenefice(Tontine tontine, LocalDate dateDebut, int nombreMembres) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate courant = dateDebut;
        dates.add(courant);
        for (int i = 1; i < nombreMembres; i++) {
            courant = prochainDate(tontine.getTypeReglePeriodicite(), tontine.getJourReference(), courant);
            if (courant == null) {
                dates.add(null);
            } else {
                dates.add(courant);
            }
        }
        return dates;
    }

    // ─────────────────────────────────────────────
    // Méthodes internes de calcul
    // ─────────────────────────────────────────────

    /** Prochain samedi (jour de réunion hebdomadaire) après 'depuis'. */
    private LocalDate prochainSamedi(LocalDate depuis) {
        LocalDate candidate = depuis.plusDays(1);
        while (candidate.getDayOfWeek() != DayOfWeek.SATURDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    /** Prochain jour fixe du mois (ex: le 5 de chaque mois). */
    private LocalDate prochainJourFixe(LocalDate depuis, int jour) {
        int jourSafe = Math.min(jour, 28);
        LocalDate candidate = depuis.withDayOfMonth(Math.min(jourSafe, depuis.lengthOfMonth()));
        if (!candidate.isAfter(depuis)) {
            candidate = depuis.plusMonths(1);
            candidate = candidate.withDayOfMonth(Math.min(jourSafe, candidate.lengthOfMonth()));
        }
        return candidate;
    }

    /** Dernier samedi ou dimanche du mois après 'depuis'. */
    private LocalDate prochainDernierJourSemaine(LocalDate depuis, DayOfWeek dow) {
        LocalDate candidate = depuis.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.previousOrSame(dow));
        if (!candidate.isAfter(depuis)) {
            LocalDate nextMonth = depuis.plusMonths(1);
            candidate = nextMonth.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.previousOrSame(dow));
        }
        return candidate;
    }

    /** Premier dimanche strictement après jourReference dans le mois. */
    private LocalDate prochainPremierDimancheApres(LocalDate depuis, int jourReference) {
        int jourSafe = Math.min(jourReference, 28);
        LocalDate mois = depuis.withDayOfMonth(1);
        for (int m = 0; m < 24; m++) {
            LocalDate ref = mois.withDayOfMonth(Math.min(jourSafe, mois.lengthOfMonth()));
            LocalDate candidate = ref.plusDays(1);
            while (candidate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                candidate = candidate.plusDays(1);
            }
            if (candidate.getMonth() != mois.getMonth()) {
                mois = mois.plusMonths(1);
                continue;
            }
            if (candidate.isAfter(depuis)) {
                return candidate;
            }
            mois = mois.plusMonths(1);
        }
        return null;
    }

    /** Premier dimanche >= jourReference dans le mois. */
    private LocalDate prochainDimancheSurOuApres(LocalDate depuis, int jourReference) {
        int jourSafe = Math.min(jourReference, 28);
        LocalDate mois = depuis.withDayOfMonth(1);
        for (int m = 0; m < 24; m++) {
            LocalDate ref = mois.withDayOfMonth(Math.min(jourSafe, mois.lengthOfMonth()));
            LocalDate candidate = ref;
            while (candidate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                candidate = candidate.plusDays(1);
            }
            if (candidate.getMonth() != mois.getMonth()) {
                mois = mois.plusMonths(1);
                continue;
            }
            if (candidate.isAfter(depuis)) {
                return candidate;
            }
            mois = mois.plusMonths(1);
        }
        return null;
    }

    /** Prochain jour fixe avec une périodicité en mois (3, 6 ou 12). */
    private LocalDate prochainJourFixePeriode(LocalDate depuis, int jour, int periodeMois) {
        int jourSafe = Math.min(jour, 28);
        LocalDate candidate = depuis.withDayOfMonth(Math.min(jourSafe, depuis.lengthOfMonth()));
        if (!candidate.isAfter(depuis)) {
            candidate = depuis.plusMonths(periodeMois);
            candidate = candidate.withDayOfMonth(Math.min(jourSafe, candidate.lengthOfMonth()));
        }
        return candidate;
    }
}
