export type StatutPret = 'DEMANDE' | 'VALIDE' | 'REJETE' | 'EN_COURS' | 'SOLDE';
export type StatutEcheance = 'A_PAYER' | 'PAYEE' | 'EN_RETARD';

export interface PretResponse {
  id: string;
  membreId: string;
  membreMatricule: string;
  membreNom: string;
  membrePrenom: string;
  montantPrincipal: number;
  tauxInteret: number;
  dureeMois: number;
  montantTotal: number;
  totalInterets: number;
  statut: StatutPret;
  motifRejet: string | null;
  valideParId: string | null;
  valideParEmail: string | null;
  dateValidation: string | null;
  dateDecaissement: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EcheancePretResponse {
  id: string;
  pretId: string;
  numeroEcheance: number;
  dateEcheance: string;
  montantCapital: number;
  montantInteret: number;
  montantTotal: number;
  statut: StatutEcheance;
  datePaiement: string | null;
}

export interface SimulationPretResponse {
  montantPrincipal: number;
  tauxInteretAnnuel: number;
  dureeMois: number;
  mensualite: number;
  montantTotal: number;
  totalInterets: number;
  echeances: LigneAmortissement[];
}

export interface LigneAmortissement {
  numero: number;
  montantCapital: number;
  montantInteret: number;
  mensualite: number;
  soldeRestant: number;
}
