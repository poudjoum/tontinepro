export interface MembreDashboard {
  matricule:    string;
  nom:          string;
  prenom:       string;
  fonction:     string;
  statut:       string;
  tontineNom:   string;

  soldeEpargne: number;

  derniereCotisationMois:    number | null;
  derniereCotisationAnnee:   number | null;
  montantCotisation:         number; // correspond à montantCotisationMin
  statutDerniereCotisation:  string | null;

  pretActifId:            string | null;
  pretMontantPrincipal:   number | null;
  pretMontantTotal:       number | null;
  pretDureeMois:          number;
  pretEcheancesRestantes: number;

  notificationsNonLues: number;
}

export interface AdminDashboard {
  tontineId:  string;
  tontineNom: string;

  membresActifs: number;
  membresTotal:  number;

  cotisationsPayees:    number;
  cotisationsEnRetard:  number;
  cotisationsEnAttente: number;
  montantCollecte:      number;
  montantAttendu:       number;

  totalEpargne:   number;
  fondsAideSolde: number;

  pretsEnCours: number;
  encoursPrets: number;

  aidesEnAttente: number;
  pretsEnAttente: number;
}
