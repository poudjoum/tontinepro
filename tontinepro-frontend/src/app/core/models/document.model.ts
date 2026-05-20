export type TypeDocument = 'CNI' | 'LETTRE_ENGAGEMENT' | 'PLAN_LOCALISATION' | 'RECONNAISSANCE_DETTE' | 'JUSTIFICATIF_ABSENCE' | 'AUTRE';

export interface DocumentResponse {
  id: string;
  membreId: string;
  membreNom: string;
  membrePrenom: string;
  membreMatricule: string;
  typeDocument: TypeDocument;
  nomFichier: string;
  tailleOctets: number;
  contentType: string;
  createdAt: string;
}
