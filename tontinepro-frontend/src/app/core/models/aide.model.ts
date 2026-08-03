export type TypeAide = 'DECES' | 'MALADIE' | 'ACCIDENT' | 'MARIAGE'
                     | 'NAISSANCE' | 'SCOLARITE' | 'CALAMITE' | 'AUTRE';
export type StatutAide = 'SOUMISE' | 'VALIDEE' | 'REJETEE' | 'PAYEE';

export const TYPE_AIDE_LABELS: Record<TypeAide, string> = {
  DECES:     'Décès',
  MALADIE:   'Maladie',
  ACCIDENT:  'Accident',
  MARIAGE:   'Mariage',
  NAISSANCE: 'Naissance',
  SCOLARITE: 'Scolarité',
  CALAMITE:  'Calamité',
  AUTRE:     'Autre',
};

export const TYPES_AIDE: TypeAide[] = [
  'DECES','MALADIE','ACCIDENT','MARIAGE','NAISSANCE','SCOLARITE','CALAMITE','AUTRE'
];

export type ModeCalculAide = 'PAR_PERSONNE' | 'FORFAITAIRE';

export const MODE_CALCUL_AIDE_LABELS: Record<ModeCalculAide, string> = {
  PAR_PERSONNE: 'Par personne (montant/tête)',
  FORFAITAIRE:  'Forfaitaire (enveloppe à répartir)',
};

export interface RubriqueAideResponse {
  id: string;
  tontineId: string;
  libelle: string;
  typeAide: TypeAide;
  modeCalcul: ModeCalculAide;
  montantReference: number;
  prefinancable: boolean;
  actif: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RubriqueAideRequest {
  libelle: string;
  typeAide?: TypeAide;
  modeCalcul: ModeCalculAide;
  montantReference: number;
  prefinancable?: boolean;
  actif?: boolean;
  description?: string | null;
}

export interface AideResponse {
  id: string;
  typeAide: TypeAide;
  montantDemande: number;
  montantAccorde: number | null;
  motif: string;
  statut: StatutAide;
  justificatifUrl: string | null;
  motifRejet: string | null;
  membreId: string;
  membreMatricule: string;
  membreNom: string;
  membrePrenom: string;
  valideParId: string | null;
  valideParEmail: string | null;
  dateValidation: string | null;
  createdAt: string;
  updatedAt: string;
}
