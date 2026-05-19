export type TypeSanction = 'RETARD_COTISATION' | 'ABSENCE_REUNION' | 'AUTRE';

export interface SanctionResponse {
  id: string;
  membreId: string;
  membreNom: string;
  membrePrenom: string;
  membreMatricule: string;
  tontineId: string;
  typeSanction: TypeSanction;
  montant: number;
  motif: string | null;
  payee: boolean;
  referenceId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreerSanctionRequest {
  membreId: string;
  typeSanction: TypeSanction;
  montant: number;
  motif?: string;
}
