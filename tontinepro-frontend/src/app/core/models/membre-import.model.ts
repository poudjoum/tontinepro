export interface MembreCree {
  nom: string;
  prenom: string;
  /** Email réel, ou null si le membre se connecte par téléphone. */
  email: string | null;
  telephone: string | null;
  matricule: string;
  /** Renseigné uniquement pour les membres SANS email (à communiquer par l'admin). */
  motDePasseTemporaire: string | null;
}

export interface MembreImportResponse {
  totalLignes: number;
  crees: number;
  rattaches: number;
  ignores: number;
  nouveaux: MembreCree[];
  avertissements: string[];
}
