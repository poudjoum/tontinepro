export type StatutSession = 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export interface OrdreBeneficiaireResponse {
  id: string;
  membreId: string;
  membreNom: string;
  membrePrenom: string;
  membreMatricule: string;
  ordre: number;
  dateBenefice: string | null;
  montantRecu: number | null;
  beneficie: boolean;
}

export interface SessionResponse {
  id: string;
  tontineId: string;
  numero: number;
  dateDebut: string;
  dateFin: string | null;
  dateProchaineTontine: string | null;
  statut: StatutSession;
  nombreMembres: number;
  potReserve: number;
  beneficiaires: OrdreBeneficiaireResponse[];
}

export interface CreerSessionRequest {
  tontineId: string;
  dateDebut: string;
  ordreMembreIds?: string[];
}

export interface MiseAJourDateRequest {
  dateProchaineTontine: string;
}

export interface ValiderBeneficeRequest {
  montantRecu: number;
}

export interface ReordonnerBeneficiairesRequest {
  ordreMembreIds: string[];
}
