package com.tontinepro.tontinepro_backend.api.absence.dto;

import java.util.List;

public record AppelPresenceResult(
        int totalMembres,
        int presentsCount,
        int absentsCount,
        int sanctionsGenerees,
        List<AbsenceResponse> absencesCreees
) {}
