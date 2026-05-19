export type PeriodeCotisation = 'HEBDOMADAIRE' | 'MENSUEL' | 'BIMENSUEL' | 'TRIMESTRIEL' | 'SEMESTRIEL' | 'ANNUEL';
export type ModeContributionAide = 'AUCUN' | 'MENSUEL' | 'A_LA_BENEFICIATION';

export interface TontineResponse {
  id: string;
  nom: string;
  description: string;
  montantCotisation: number;
  jourCotisation: number;
  periodeCotisation: PeriodeCotisation;
  tauxInteretPret: number;
  tauxInteretEpargne: number;
  modeContributionAide: ModeContributionAide;
  montantCotisationAide: number | null;
  montantAmende: number;
  montantPenaliteRetard: number;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateTontineConfigRequest {
  nom?: string;
  description?: string;
  montantCotisation?: number;
  jourCotisation?: number;
  periodeCotisation?: PeriodeCotisation;
  tauxInteretPret?: number;
  tauxInteretEpargne?: number;
  actif?: boolean;
  modeContributionAide?: ModeContributionAide;
  montantCotisationAide?: number;
  montantAmende?: number;
  montantPenaliteRetard?: number;
}
