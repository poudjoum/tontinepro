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
  /** Date limite de collecte (3 séances après l'activation). */
  dateEcheanceRecouvrement: string | null;
  /** Échéance dépassée alors que des parts restent dues. */
  recouvrementEnRetard: boolean;
  contributions: LigneContributionAide[];
}

export interface MembreRowCollecte {
  membreId: string;
  nomPrenom: string;
  matricule: string;
}

export interface CelluleCollecte {
  membreId: string;
  contributionId: string | null;
  montant: number | null;
  paye: boolean;
  estBeneficiaire: boolean;
}

export interface AideColonneCollecte {
  aideId: string;
  libelle: string;
  variante: string | null;
  beneficiaireId: string;
  beneficiaireNom: string;
  statut: StatutAide;
  prefinance: boolean;
  objectif: number;
  collecte: number;
  dateEcheanceRecouvrement: string | null;
  enRetard: boolean;
  cellules: CelluleCollecte[];
}

export interface CollecteAidesResponse {
  membres: MembreRowCollecte[];
  aides: AideColonneCollecte[];
  totalObjectif: number;
  totalCollecte: number;
}

/** Compte rendu de la suppression d'une aide (effets annulés sur le fonds). */
export interface SuppressionAideResponse {
  aideId: string;
  libelle: string;
  beneficiaireNom: string;
  nbContributionsSupprimees: number;
  montantCollecteAnnule: number;
  montantDecaisseRendu: number;
  soldeFondsApres: number | null;
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
  /** Date limite de recouvrement, fixée à l'activation (null avant). */
  dateEcheanceRecouvrement: string | null;
  createdAt: string;
  updatedAt: string;
}
