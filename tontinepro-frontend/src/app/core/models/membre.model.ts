export interface MembreResponse {
  id: string;
  matricule: string;
  nom: string;
  prenom: string;
  dateAdhesion: string;
  statut: 'ACTIF' | 'SUSPENDU' | 'RETIRE';
  tontineId: string;
  tontineNom: string;
  userId: string;
  userEmail: string;
  createdAt: string;
}
