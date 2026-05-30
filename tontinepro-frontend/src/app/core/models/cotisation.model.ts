export type StatutCotisation = 'EN_ATTENTE' | 'PAYEE' | 'EN_RETARD';

export interface CotisationResponse {
  id: string;
  mois: number;
  annee: number;
  montant: number;
  montantFondAide: number;
  montantRepas: number;
  montantTotal: number;
  statut: StatutCotisation;
  datePaiement: string | null;
  referencePaiement: string | null;
  membreId: string;
  membreMatricule: string;
  membreNom: string;
  membrePrenom: string;
  tontineId: string;
  tontineNom: string;
  createdAt: string;
  updatedAt: string;
}

export interface EnregistrerPaiementRequest {
  datePaiement?: string;
  referencePaiement?: string;
  montantTontine?: number;
  montantFondAide?: number;
}

export interface ModifierCotisationRequest {
  montant?: number | null;
  montantFondAide?: number | null;
  montantRepas?: number | null;
  referencePaiement?: string | null;
  statut?: StatutCotisation | null;
  datePaiement?: string | null;
}
