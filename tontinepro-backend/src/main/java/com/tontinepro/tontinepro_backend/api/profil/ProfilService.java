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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final MembreRepository membreRepository;
    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;

    @Transactional
    public MembreResponse mettreAJourProfil(String email, UUID tontineId, UpdateProfilRequest request) {
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

        // Nom et prénom désignent la personne : on les reporte sur chacun de ses
        // profils, sinon elle porterait un nom différent d'une tontine à l'autre.
        List<Membre> profils = membreRepository.findAllByUserEmail(email);
        if (profils.isEmpty()) {
            throw new IllegalArgumentException("Aucun profil membre associé à ce compte");
        }
        for (Membre m : profils) {
            if (request.nom() != null && !request.nom().isBlank()) {
                m.setNom(request.nom().trim());
            }
            if (request.prenom() != null && !request.prenom().isBlank()) {
                m.setPrenom(request.prenom().trim());
            }
        }
        membreRepository.saveAll(profils);

        Membre affiche = profils.stream()
                .filter(m -> m.getTontine().getId().equals(tontineId))
                .findFirst()
                .orElse(profils.get(0));
        return MembreResponse.from(affiche);
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
