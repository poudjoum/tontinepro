export type StatutSession = 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export interface OrdreBeneficiaireResponse {
  id: string;
  membreId: string;
  membreNom: string;
  membrePrenom: string;
  membreMatricule: string;
  membreStatut: 'ACTIF' | 'SUSPENDU' | 'RETIRE';
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
  cibleMembres: number | null;
  potReserve: number;
  beneficiaires: OrdreBeneficiaireResponse[];
}

export interface CreerSessionRequest {
  tontineId: string;
  dateDebut: string;
  cibleMembres?: number;
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

export interface MembreCotisationStatut {
  membreId: string;
  nom: string;
  prenom: string;
  matricule: string;
  statutCotisation: 'PAYEE' | 'EN_ATTENTE' | 'EN_RETARD' | 'ABSENTE';
  cotisationId: string | null;
}

export interface SessionCotisationsStatutResponse {
  sessionId: string;
  sessionNumero: number;
  mois: number;
  annee: number;
  totalMembres: number;
  nbPayes: number;
  nbEnAttente: number;
  nbEnRetard: number;
  nbAbsents: number;
  complete: boolean;
  montantCotisationDefaut: number | null;
  montantFondAideDefaut: number | null;
  modeContributionAide: string;
  membres: MembreCotisationStatut[];
}

export interface MonBeneficeResponse {
  ordreBeneficiaireId: string;
  sessionId: string;
  sessionNumero: number;
  tontineNom: string;
  ordre: number;
  totalMembres: number;
  dateBenefice: string | null;
  montantRecu: number | null;
}

export interface LigneRapport {
  membreId: string;
  nomPrenom: string;
  matricule: string;
  cotisation: number;
  fond: number;
  repas: number;
  sanctions: number;
  paye: boolean;
}

export interface RapportTourResponse {
  sessionId: string;
  sessionNumero: number;
  tontineNom: string;
  dateTour: string | null;
  mois: number;
  annee: number;
  beneficiaireId: string;
  beneficiaireNom: string;
  beneficiaireMatricule: string;
  lignes: LigneRapport[];
  totalCotisations: number;
  totalFond: number;
  totalRepas: number;
  totalSanctions: number;
  potBrut: number;
  fondAideObligation: number;
  fondAidePayeAnnee: number;
  detteFondAide: number;
  cagnotteBeneficiaire: number;
}

export interface MonTourResponse {
  sessionId: string;
  sessionNumero: number;
  ordre: number;
  totalMembres: number;
  dateBenefice: string | null;
  beneficie: boolean;
  tontineNom: string;
}

export interface LignePaiementMembre {
  membreId: string;
  matricule: string;
  nomPrenom: string;
  montantTontine: number;
  montantFondAide: number;
  total: number;
  paye: boolean;
}

export interface SessionBilanResponse {
  sessionId: string;
  sessionNumero: number;
  nbMembresPayes: number;
  potTontineBrut: number;
  fondsAideCollecte: number;
  beneficiaireId: string | null;
  beneficiaireNom: string | null;
  beneficiairePrenom: string | null;
  beneficiaireMatricule: string | null;
  fondAideAnnuelObligation: number;
  fondAidePayeCetteAnnee: number;
  dettesFondsAide: number;
  potBeneficiaireNet: number;
  lignes: LignePaiementMembre[];
}
