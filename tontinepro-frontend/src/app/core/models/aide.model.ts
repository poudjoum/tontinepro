export type TypeAide = 'DECES' | 'MALADIE' | 'ACCIDENT' | 'MARIAGE'
                     | 'NAISSANCE' | 'SCOLARITE' | 'CALAMITE' | 'AUTRE';
export type StatutAide = 'PROPOSEE' | 'SOUMISE' | 'VALIDEE' | 'REJETEE' | 'PAYEE' | 'REFUSEE';

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

export type PorteeLimiteAide = 'VIE' | 'SESSION' | 'ANNEE';

export const PORTEE_LIMITE_LABELS: Record<PorteeLimiteAide, string> = {
  VIE:     'À vie (une seule fois)',
  SESSION: 'Par session',
  ANNEE:   'Par année',
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
  limiteParBeneficiaire: number | null;
  porteeLimite: PorteeLimiteAide;
  variantes: string | null;
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
  limiteParBeneficiaire?: number | null;
  porteeLimite?: PorteeLimiteAide;
  variantes?: string | null;
}

export interface SimulationAideResponse {
  rubriqueId: string;
  libelle: string;
  typeAide: TypeAide;
  modeCalcul: ModeCalculAide;
  montantReference: number;
  nbMembresActifs: number;
  partParMembre: number;
  total: number;
  prefinancable: boolean;
}

export interface LigneContributionAide {
  contributionId: string;
  membreId: string;
  membreNom: string;
  membreMatricule: string;
  montant: number;
  statut: 'A_PAYER' | 'PAYEE';
  datePaiement: string | null;
  estBeneficiaire: boolean;
}

export interface AideSuiviResponse {
  aideId: string;
  rubriqueLibelle: string | null;
  beneficiaireNom: string;
  beneficiaireMatricule: string;
  statut: StatutAide;
  prefinance: boolean;
  montantTotal: number;
  partParMembre: number;
  nbMembresBase: number;
  totalAttendu: number;
  totalCollecte: number;
  nbPayes: number;
  nbTotal: number;
  soldeFonds: number;
  contributions: LigneContributionAide[];
}

export interface AideResponse {
  id: string;
  typeAide: TypeAide;
  rubriqueId: string | null;
  rubriqueLibelle: string | null;
  variante: string | null;
  montantDemande: number;
  montantAccorde: number | null;
  modeCalcul: ModeCalculAide | null;
  nbMembresBase: number | null;
  partParMembre: number | null;
  prefinance: boolean;
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
