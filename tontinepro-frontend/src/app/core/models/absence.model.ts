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
