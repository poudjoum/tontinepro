export type StatutDemande = 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE';
export type TypeDocumentTontine = 'REGLEMENT_INTERIEUR' | 'STATUTS' | 'AUTRE';
export type TypeAcces = 'OUVERTE' | 'RESTREINTE';

export interface DemandeResponse {
  id: string;
  tontineId: string;
  tontineNom: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  motivation: string | null;
  statut: StatutDemande;
  motifRejet: string | null;
  typeParticipation: 'TONTINE' | 'AIDE_SOCIALE';
  modePaiementAide: 'MENSUEL' | 'EN_UNE_FOIS' | null;
  createdAt: string;
  updatedAt: string;
}

export interface SoumettreDemandeRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  motivation?: string;
  typeParticipation?: 'TONTINE' | 'AIDE_SOCIALE';
  modePaiementAide?: 'MENSUEL' | 'EN_UNE_FOIS';
}

export interface DocumentTontineResponse {
  id: string;
  tontineId: string;
  typeDocument: TypeDocumentTontine;
  nomFichier: string;
  tailleOctets: number;
  contentType: string;
  createdAt: string;
}
