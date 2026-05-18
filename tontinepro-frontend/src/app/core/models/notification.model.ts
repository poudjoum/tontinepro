export type NotificationType =
  | 'AIDE_SOUMISE' | 'AIDE_VALIDEE' | 'AIDE_REJETEE' | 'AIDE_PAYEE'
  | 'PRET_DEMANDE' | 'PRET_APPROUVE' | 'PRET_REJETE' | 'PRET_REMBOURSE' | 'PRET_SOLDE'
  | 'COTISATION_PAYEE' | 'COTISATION_EN_RETARD'
  | 'EPARGNE_DEPOT' | 'EPARGNE_RETRAIT' | 'EPARGNE_INTERETS'
  | 'BIENVENUE';

export interface NotificationResponse {
  id: string;
  type: NotificationType;
  titre: string;
  message: string;
  lu: boolean;
  referenceId?: string;
  referenceType?: string;
  createdAt: string;
}
