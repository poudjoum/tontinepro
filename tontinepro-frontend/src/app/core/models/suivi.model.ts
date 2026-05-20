export interface CotisationSuiviItem {
  id: string;
  tontineNom: string;
  montant: number;
  statut: 'EN_ATTENTE' | 'PAYEE' | 'EN_RETARD';
  datePaiement: string | null;
}

export interface ContributionSuiviItem {
  id: string;
  tontineNom: string;
  montant: number;
  statut: 'A_PAYER' | 'PAYEE';
  datePaiement: string | null;
}

export interface SanctionSuiviItem {
  id: string;
  tontineNom: string;
  typeSanction: string;
  montant: number;
  motif: string | null;
  payee: boolean;
  date: string;
}

export interface EcheanceSuiviItem {
  id: string;
  pretId: string;
  tontineNom: string;
  numeroEcheance: number;
  montantTotal: number;
  statut: 'A_PAYER' | 'PAYEE' | 'EN_RETARD';
  dateEcheance: string;
  datePaiement: string | null;
}

export interface MouvementEpargneSuiviItem {
  id: string;
  tontineNom: string;
  typeMouvement: 'DEPOT' | 'RETRAIT' | 'INTERET';
  montant: number;
  soldeApres: number;
  date: string;
}

export interface SuiviMois {
  mois: number;
  annee: number;
  cotisations: CotisationSuiviItem[];
  contributions: ContributionSuiviItem[];
  sanctions: SanctionSuiviItem[];
  echeancesPret: EcheanceSuiviItem[];
  mouvementsEpargne: MouvementEpargneSuiviItem[];
}
