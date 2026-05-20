package com.tontinepro.tontinepro_backend.api.profil;

import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.profil.dto.ChangerMotDePasseRequest;
import com.tontinepro.tontinepro_backend.api.profil.dto.UpdateProfilRequest;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final MembreRepository membreRepository;
    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;

    @Transactional
    public MembreResponse mettreAJourProfil(String email, UpdateProfilRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Mettre à jour le téléphone sur le compte utilisateur
        if (request.telephone() != null && !request.telephone().isBlank()) {
            String tel = request.telephone().trim();
            // Vérifier unicité uniquement si différent de l'actuel
            if (!tel.equals(user.getTelephone())) {
                // Pas de contrainte d'unicité à vérifier ici (géré en base)
                user.setTelephone(tel);
            }
        }
        userRepository.save(user);

        // Mettre à jour nom/prénom sur le profil membre (premier profil actif)
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));

        if (request.nom() != null && !request.nom().isBlank()) {
            membre.setNom(request.nom().trim());
        }
        if (request.prenom() != null && !request.prenom().isBlank()) {
            membre.setPrenom(request.prenom().trim());
        }

        return MembreResponse.from(membreRepository.save(membre));
    }

    @Transactional
    public void changerMotDePasse(String email, ChangerMotDePasseRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.ancienMotDePasse(), user.getHashedPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }

        if (request.ancienMotDePasse().equals(request.nouveauMotDePasse())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setHashedPassword(passwordEncoder.encode(request.nouveauMotDePasse()));
        userRepository.save(user);
    }
}
