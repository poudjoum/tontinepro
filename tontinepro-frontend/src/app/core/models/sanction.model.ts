export type TypeSanction =
  | 'RETARD_COTISATION'
  | 'ABSENCE_REUNION'
  | 'RETARD_REUNION_T1'
  | 'RETARD_REUNION_T2'
  | 'RETARD_REUNION_T3'
  | 'ECHEC_TONTINE_AVANT'
  | 'ECHEC_TONTINE_APRES'
  | 'TROUBLE_BAGARRE'
  | 'TROUBLE_ENGUEULADE'
  | 'TROUBLE_INSULTE'
  | 'AUTRE';

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
