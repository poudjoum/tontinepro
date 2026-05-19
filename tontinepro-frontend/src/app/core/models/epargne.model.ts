export type TypeMouvement = 'DEPOT' | 'RETRAIT' | 'INTERET';

export interface CompteEpargneResponse {
  id: string;
  membreId: string;
  membreMatricule: string;
  membreNom: string;
  membrePrenom: string;
  tontineId: string;
  tontineNom: string;
  solde: number;
  createdAt: string;
  updatedAt: string;
}

export interface MouvementEpargneResponse {
  id: string;
  compteId: string;
  typeMouvement: TypeMouvement;
  montant: number;
  soldeApres: number;
  reference: string | null;
  createdAt: string;
}
