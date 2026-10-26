export type PeriodeCotisation = 'HEBDOMADAIRE' | 'MENSUEL' | 'BIMENSUEL' | 'TRIMESTRIEL' | 'SEMESTRIEL' | 'ANNUEL';
export type ModeContributionAide = 'AUCUN' | 'MENSUEL' | 'A_LA_BENEFICIATION';
export type ModeTontine = 'CLASSIQUE' | 'A_LOT';
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
  mode: ModeTontine;
  montantLot: number | null;
  moisClotureAdhesions: number;
  typeAcces: 'OUVERTE' | 'RESTREINTE';
  visible: boolean;
  descriptionAcces: string | null;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
  /** Fonction du compte dans cette tontine (liste « mes tontines » uniquement). */
  maFonction?: string | null;
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
  mode?: ModeTontine;
  montantLot?: number;
  moisClotureAdhesions?: number;
}

/**
 * Part d'un co-bénéficiaire d'un tour à lot. Avant versement, retenue et montant
 * remis sont les montants prévus ; après, ceux enregistrés.
 */
export interface PartLotMembre {
  partId: string; membreId: string; nom: string; prenom: string;
  montantMensuel: number; partCagnotte: number | null;
  verse: boolean; annulee: boolean; dateVersement: string | null;
  montantRetenue: number | null; montantVerse: number | null;
}

/** Aperçu de la composition des lots avant figeage. */
export interface PartProposee {
  membreId: string; nom: string; prenom: string; montant: number; partCagnotte: number;
}
export interface CompositionLotsResponse {
  montantLot: number;
  nombreTours: number;
  cagnotte: number;
  complete: boolean;
  lots: Array<{ parts: PartProposee[] }>;
  restes: PartProposee[];
}

export interface SessionLotResponse {
  sessionId: string;
  figee: boolean;
  dateFigeage: string | null;
  montantLot: number | null;
  cagnotte: number | null;
  nombreTours: number | null;
  tresorerieLots: number;
  adhesions: Array<{
    membreId: string; nom: string; prenom: string; matricule: string; montantMensuel: number;
    abandonnee: boolean;
  }>;
  lots: Array<{
    ordreBeneficiaireId: string; ordre: number; dateTour: string | null; beneficie: boolean;
    montantRecu: number | null; nombreVerses: number;
    membres: PartLotMembre[];
  }>;
  abandons: AbandonLot[];
  tresorerie: SuiviTresorerieLot;
  /** Membres encore présents dans la session (avec ou sans mise) : candidats à un départ. */
  membresSession: MembreSessionLot[];
  /** Tour de remboursement des avoirs gelés, s'il est programmé. */
  restitution: TourRemboursementLot | null;
}

/**
 * Tour de remboursement : les cotisants cochés par le bureau financent la restitution des
 * avoirs gelés. ecart = encaissé − avoirs (négatif : avancé par la trésorerie).
 */
export interface TourRemboursementLot {
  dateTour: string; clos: boolean; dateCloture: string | null;
  cotisants: CotisantRemboursementLot[];
  totalAttendu: number; totalEncaisse: number; avoirsARestituer: number; ecart: number;
}

export interface CotisantRemboursementLot {
  cotisantId: string; membreId: string; nom: string; prenom: string;
  montant: number; paye: boolean; datePaiement: string | null;
}

export interface MembreSessionLot {
  membreId: string; nom: string; prenom: string; montantMensuel: number | null;
  dejaRecu: number; toursRecus: number; toursAVenir: number;
}

/** Montants saisis par le bureau au départ d'un membre ; null = calculé. */
export interface SaisieDepart { moisCotises: number | null; totalCotise: number | null; totalRecu: number | null; }

export type StatutAbandonLot = 'AVOIR_GELE' | 'AVOIR_RESTITUE' | 'DETTE' | 'DETTE_SOLDEE' | 'SOLDE_NUL';

/** Départ d'un membre : solde = cotisé − reçu (positif = avoir gelé, négatif = dette). */
export interface AbandonLot {
  abandonId: string; membreId: string; nom: string; prenom: string;
  dateAbandon: string; motif: string | null; moisCotises: number;
  totalCotise: number; totalRecu: number; solde: number;
  statut: StatutAbandonLot; dateRestitution: string | null;
  montantRembourse: number; resteARembourser: number;
  saisieManuelle: boolean;
}

/** Trésorerie de la session ; solde négatif = ce que la trésorerie avance. */
export interface SuiviTresorerieLot {
  misesEncaissees: number; remboursementsDettes: number; cotisationsRemboursement: number;
  partsVersees: number; avoirsRestitues: number; solde: number;
  avoirsGelesARestituer: number; dettesARecouvrer: number;
}
