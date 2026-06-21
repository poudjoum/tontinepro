export interface MembreCree {
  nom: string;
  prenom: string;
  email: string;
  matricule: string;
}

export interface MembreImportResponse {
  totalLignes: number;
  crees: number;
  rattaches: number;
  ignores: number;
  nouveaux: MembreCree[];
  avertissements: string[];
}
