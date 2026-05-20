export interface AbsenceResponse {
  id: string;
  membreId: string;
  membreNom: string;
  membrePrenom: string;
  membreMatricule: string;
  tontineId: string;
  dateReunion: string;
  justifiee: boolean;
  motif: string | null;
  createdAt: string;
}

export interface EnregistrerAbsenceRequest {
  membreId: string;
  dateReunion: string;
  justifiee: boolean;
  motif?: string;
}

export interface AppelPresenceRequest {
  tontineId: string;
  dateReunion: string;
  membresPresentsIds: string[];
}

export interface AppelPresenceResult {
  totalMembres: number;
  presentsCount: number;
  absentsCount: number;
  sanctionsGenerees: number;
  absencesCreees: AbsenceResponse[];
}
