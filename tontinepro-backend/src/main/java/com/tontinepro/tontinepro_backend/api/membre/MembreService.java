package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.CreateMembreRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.InscriptionDirecteRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreFonctionRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreStatutRequest;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final TontineRepository tontineRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MembreResponse create(CreateMembreRequest request) {
        if (membreRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("Cet utilisateur est déjà membre d'une tontine");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + request.userId()));

        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Impossible d'adhérer à une tontine inactive");
        }

        Membre membre = Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .dateAdhesion(request.dateAdhesion() != null ? request.dateAdhesion() : LocalDate.now())
                .fonction(request.fonction() != null ? request.fonction() : Membre.Fonction.MEMBRE_ORDINAIRE)
                .build();

        membre = membreRepository.save(membre);
        membre.setMatricule(generateMatricule(membre.getId()));
        membre = membreRepository.save(membre);

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return MembreResponse.from(membre);
    }

    @Transactional(readOnly = true)
    public MembreResponse getById(UUID id) {
        return membreRepository.findById(id)
                .map(MembreResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public MembreResponse getMe(String email) {
        return membreRepository.findByUserEmail(email)
                .map(MembreResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> list(UUID tontineId, Membre.Statut statut) {
        List<Membre> membres;

        if (tontineId != null && statut != null) {
            membres = membreRepository.findAllByTontineIdAndStatut(tontineId, statut);
        } else if (tontineId != null) {
            membres = membreRepository.findAllByTontineId(tontineId);
        } else {
            membres = membreRepository.findAll();
        }

        return membres.stream().map(MembreResponse::from).toList();
    }

    @Transactional
    public MembreResponse updateStatut(UUID id, UpdateMembreStatutRequest request) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));

        membre.setStatut(request.statut());
        return MembreResponse.from(membreRepository.save(membre));
    }

    @Transactional
    public MembreResponse updateFonction(UUID id, UpdateMembreFonctionRequest request) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));

        membre.setFonction(request.fonction());
        return MembreResponse.from(membreRepository.save(membre));
    }

    @Transactional
    public MembreResponse inscrireDirectement(InscriptionDirecteRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Un compte existe déjà avec l'email : " + request.email());
        }

        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Impossible d'inscrire dans une tontine inactive");
        }

        User user = User.builder()
                .email(request.email())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .telephone(request.telephone())
                .role(User.Role.MEMBRE)
                .build();
        user = userRepository.save(user);

        Membre membre = Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .fonction(request.fonction() != null ? request.fonction() : Membre.Fonction.MEMBRE_ORDINAIRE)
                .build();
        membre = membreRepository.save(membre);
        membre.setMatricule(generateMatricule(membre.getId()));
        membre = membreRepository.save(membre);

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return MembreResponse.from(membre);
    }

    private String generateMatricule(UUID membreId) {
        return "MBR-" + membreId.toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
