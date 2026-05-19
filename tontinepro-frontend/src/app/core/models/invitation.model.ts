export interface InvitationResponse {
  id: string;
  tontineId: string;
  tontineNom: string;
  token: string;
  utilise: boolean;
  expiresAt: string;
  createdAt: string;
}

export interface StatutInvitationResponse {
  valide: boolean;
  tontineId: string | null;
  tontineNom: string | null;
  tontineDescription: string | null;
  expiresAt: string | null;
}

export interface GenererInvitationRequest {
  tontineId: string;
  dureeHeures?: number;
}

export interface RejoindreRequest {
  email: string;
  password: string;
  nom: string;
  prenom: string;
  telephone?: string;
}
