export type PeriodeCotisation = 'HEBDOMADAIRE' | 'MENSUEL' | 'BIMENSUEL' | 'TRIMESTRIEL' | 'SEMESTRIEL' | 'ANNUEL';
export type ModeContributionAide = 'AUCUN' | 'MENSUEL' | 'A_LA_BENEFICIATION';
export type TypeReglePeriodicite =
  | 'HEBDOMADAIRE'
  | 'MENSUEL_JOUR_FIXE'
  | 'MENSUEL_DERNIER_SAMEDI'
  | 'MENSUEL_DERNIER_DIMANCHE'
  | 'MENSUEL_PREMIER_DIMANCHE_APRES'
  | 'MENSUEL_DIMANCHE_SUR_OU_APRES'
  | 'TRIMESTRIEL_JOUR_FIXE'
  | 'SEMESTRIEL_JOUR_FIXE'
  | 'ANNUEL_JOUR_FIXE'
  | 'DATE_MANUELLE';

export interface TontineResponse {
  id: string;
  nom: string;
  description: string;
  montantCotisationMin: number;
  montantCotisationMax: number | null;
  montantConsensuel: number | null;
  jourReference: number | null;
  typeReglePeriodicite: TypeReglePeriodicite;
  dateProchaineTontine: string | null;
  tauxInteretPret: number;
  tauxInteretEpargne: number;
  modeContributionAide: ModeContributionAide;
  montantCotisationAide: number | null;
  montantAmende: number;
  montantPenaliteRetard: number;
  montantRetardReunionT1: number;
  montantRetardReunionT2: number;
  montantRetardReunionT3: number;
  montantEchecTontineAvant: number;
  montantEchecTontineApres: number;
  montantReverseBeneficiaire: number;
  montantTroubleBagarre: number;
  montantTroubleEngueulade: number;
  montantTroubleInsulte: number;
  montantFondAideAnnuelMembre: number | null;
  typeAcces: 'OUVERTE' | 'RESTREINTE';
  visible: boolean;
  descriptionAcces: string | null;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateTontineConfigRequest {
  nom?: string;
  description?: string;
  montantCotisationMin?: number;
  montantCotisationMax?: number;
  montantConsensuel?: number;
  jourReference?: number;
  typeReglePeriodicite?: TypeReglePeriodicite;
  dateProchaineTontine?: string;
  tauxInteretPret?: number;
  tauxInteretEpargne?: number;
  actif?: boolean;
  modeContributionAide?: ModeContributionAide;
  montantCotisationAide?: number;
  montantAmende?: number;
  montantPenaliteRetard?: number;
  montantRetardReunionT1?: number;
  montantRetardReunionT2?: number;
  montantRetardReunionT3?: number;
  montantEchecTontineAvant?: number;
  montantEchecTontineApres?: number;
  montantReverseBeneficiaire?: number;
  montantTroubleBagarre?: number;
  montantTroubleEngueulade?: number;
  montantTroubleInsulte?: number;
  montantFondAideAnnuelMembre?: number;
}
