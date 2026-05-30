export type TypeParticipation = 'TONTINE' | 'AIDE_SOCIALE';
export type ModePaiementAide  = 'MENSUEL' | 'EN_UNE_FOIS';

export interface MembreResponse {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  dateAdhesion: string;
  statut:   'ACTIF' | 'SUSPENDU' | 'RETIRE';
  fonction: 'PRESIDENT' | 'SECRETAIRE' | 'TRESORIER' | 'CENSEUR' | 'MEMBRE_ORDINAIRE';
  typeParticipation: TypeParticipation;
  modePaiementAide: ModePaiementAide | null;
  tontineId: string;
  tontineNom: string;
  userId: string;
  userEmail: string;
  createdAt: string;
}
