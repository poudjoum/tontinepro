package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.MembreImportResponse;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MembreImportService {

    private final UserRepository          userRepository;
    private final MembreRepository        membreRepository;
    private final TontineRepository       tontineRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PasswordEncoder         passwordEncoder;
    private final EmailService            emailService;

    @Value("${app.frontend-url:https://tontinepro.tontinepro.uk}")
    private String frontendUrl;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final SecureRandom RANDOM = new SecureRandom();
    // Sans caractères ambigus (0/O, 1/l/I) pour un mot de passe lisible
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    @Transactional
    public MembreImportResponse importer(UUID tontineId, MultipartFile fichier, String requesterEmail) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));

        verifierAcces(tontineId, requesterEmail);

        List<LigneMembre> lignes = lire(fichier);

        List<String> avertissements = new ArrayList<>();
        List<MembreImportResponse.MembreCree> nouveaux = new ArrayList<>();
        List<Runnable> mailsAEnvoyer = new ArrayList<>();
        int crees = 0, rattaches = 0, ignores = 0;

        for (LigneMembre l : lignes) {
            String contexte = "Ligne " + l.numero();

            String telephone = isBlank(l.telephone()) ? null : l.telephone().trim();
            boolean aEmail = !isBlank(l.email());

            // Nom + prénom obligatoires, et AU MOINS un identifiant (email OU téléphone).
            if (isBlank(l.nom()) || isBlank(l.prenom())) {
                avertissements.add(contexte + " : nom et prénom sont obligatoires — ignorée");
                ignores++;
                continue;
            }
            if (!aEmail && telephone == null) {
                avertissements.add(contexte + " : un email ou un téléphone est obligatoire — ignorée");
                ignores++;
                continue;
            }
            if (aEmail && !EMAIL.matcher(l.email().trim()).matches()) {
                avertissements.add(contexte + " : email invalide (" + l.email() + ") — ignorée");
                ignores++;
                continue;
            }

            // Email réel (si fourni) ou email technique dérivé du téléphone (jamais affiché).
            String emailReel  = aEmail ? l.email().trim().toLowerCase() : null;
            String emailLogin = aEmail ? emailReel : emailSynthetique(telephone);
            String identifiant = aEmail ? emailReel : telephone;

            // Recherche d'un compte existant : email d'abord (si fourni), puis téléphone
            Optional<User> existant = aEmail ? userRepository.findByEmail(emailReel) : Optional.empty();
            if (existant.isEmpty() && telephone != null) {
                existant = userRepository.findByTelephone(telephone);
            }

            if (existant.isPresent()) {
                User user = existant.get();
                if (membreRepository.existsByUserIdAndTontineId(user.getId(), tontineId)) {
                    avertissements.add(contexte + " : " + identifiant + " est déjà membre de cette tontine — ignorée");
                    ignores++;
                    continue;
                }
                creerProfilMembre(user, tontine, l);
                rattaches++;
                avertissements.add(contexte + " : compte existant rattaché (" + identifiant + ") — aucun mot de passe envoyé");
                continue;
            }

            // Nouveau compte avec mot de passe temporaire
            String motDePasseTemporaire = genererMotDePasse();
            User user = userRepository.save(User.builder()
                    .email(emailLogin)
                    .telephone(telephone)
                    .hashedPassword(passwordEncoder.encode(motDePasseTemporaire))
                    .role(User.Role.MEMBRE)
                    .mustChangePassword(true)
                    .build());

            Membre membre = creerProfilMembre(user, tontine, l);

            if (aEmail) {
                // L'envoi est différé après le commit : si la transaction échoue plus loin,
                // aucun identifiant n'est envoyé pour un compte qui sera annulé par le rollback.
                String prenom = l.prenom(), nom = l.nom(), nomTontine = tontine.getNom();
                mailsAEnvoyer.add(() ->
                        envoyerMailBienvenue(emailReel, prenom, nom, nomTontine, motDePasseTemporaire));
            }

            crees++;
            // Sans email : on renvoie le mot de passe temporaire à l'admin (pas d'envoi possible).
            nouveaux.add(new MembreImportResponse.MembreCree(
                    l.nom(), l.prenom(), emailReel, telephone, membre.getMatricule(),
                    aEmail ? null : motDePasseTemporaire));
        }

        envoyerApresCommit(mailsAEnvoyer);

        return new MembreImportResponse(
                lignes.size(), crees, rattaches, ignores, nouveaux, avertissements);
    }

    /**
     * N'envoie les mails de bienvenue qu'une fois la transaction validée, pour ne jamais
     * communiquer d'identifiants pour des comptes annulés par un rollback. En l'absence de
     * transaction active, l'envoi est immédiat.
     */
    private void envoyerApresCommit(List<Runnable> mails) {
        if (mails.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mails.forEach(Runnable::run);
                }
            });
        } else {
            mails.forEach(Runnable::run);
        }
    }

    /** Génère le modèle d'import pré-rempli avec le nom de la tontine. */
    @Transactional(readOnly = true)
    public String nomTontine(UUID tontineId) {
        return tontineRepository.findById(tontineId)
                .map(Tontine::getNom)
                .orElse(null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void verifierAcces(UUID tontineId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (requester.getRole() == User.Role.SUPER_ADMIN) return;

        boolean membreDeLaTontine = membreRepository
                .findByUserEmailAndTontineId(requesterEmail, tontineId)
                .filter(m -> m.getStatut() == Membre.Statut.ACTIF)
                .isPresent();
        if (!membreDeLaTontine) {
            throw new AccessDeniedException("Vous n'appartenez pas à cette tontine");
        }
    }

    private Membre creerProfilMembre(User user, Tontine tontine, LigneMembre l) {
        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(l.nom().trim())
                .prenom(l.prenom().trim())
                .matricule(genererMatricule())
                .build());
        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());
        return membre;
    }

    private void envoyerMailBienvenue(String email, String prenom, String nom,
                                      String nomTontine, String motDePasseTemporaire) {
        String corps = """
                Bonjour %s %s,

                Un compte a été créé pour vous sur TontinePro pour la tontine « %s ».

                Voici vos identifiants de connexion :
                  • Identifiant : %s
                  • Mot de passe temporaire : %s

                Pour des raisons de sécurité, vous devrez OBLIGATOIREMENT changer ce mot de passe
                lors de votre première connexion.

                Connectez-vous dès maintenant : %s

                À bientôt,
                L'équipe TontinePro
                """.formatted(prenom, nom, nomTontine, email, motDePasseTemporaire, frontendUrl);

        emailService.envoyer(email, "Vos identifiants de connexion", corps);
    }

    private List<LigneMembre> lire(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni");
        }
        try (InputStream is = fichier.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheet("Membres");
            if (sheet == null) sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Le fichier ne contient aucune feuille");
            }

            int headerRow = trouverLigneEntete(sheet);
            if (headerRow < 0) {
                throw new IllegalArgumentException(
                        "En-têtes introuvables. Utilisez le modèle (colonnes : Nom, Prénom, Email, Téléphone).");
            }
            int[] cols = mapperColonnes(sheet.getRow(headerRow));

            List<LigneMembre> lignes = new ArrayList<>();
            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String nom       = cellule(row, cols[0]);
                String prenom    = cellule(row, cols[1]);
                String email     = cellule(row, cols[2]);
                String telephone = cellule(row, cols[3]);
                if (isBlank(nom) && isBlank(prenom) && isBlank(email) && isBlank(telephone)) {
                    continue; // ligne entièrement vide
                }
                lignes.add(new LigneMembre(r + 1, nom, prenom, email, telephone));
            }
            return lignes;

        } catch (IOException e) {
            throw new IllegalArgumentException("Fichier illisible. Format Excel (.xlsx) attendu.");
        }
    }

    /** Trouve la ligne d'en-tête (celle contenant à la fois « nom » et « email »). */
    private int trouverLigneEntete(Sheet sheet) {
        for (int r = sheet.getFirstRowNum(); r <= Math.min(sheet.getLastRowNum(), 15); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            boolean hasNom = false, hasEmail = false;
            for (Cell c : row) {
                String v = normaliser(FORMATTER.formatCellValue(c));
                if (v.startsWith("nom")) hasNom = true;
                if (v.startsWith("email") || v.startsWith("mail")) hasEmail = true;
            }
            if (hasNom && hasEmail) return r;
        }
        return -1;
    }

    /** Associe chaque champ logique à un index de colonne d'après l'en-tête. */
    private int[] mapperColonnes(Row header) {
        int nom = -1, prenom = -1, email = -1, telephone = -1;
        for (Cell c : header) {
            String v = normaliser(FORMATTER.formatCellValue(c));
            int idx = c.getColumnIndex();
            if (nom < 0 && v.startsWith("nom")) nom = idx;
            else if (prenom < 0 && (v.startsWith("prenom"))) prenom = idx;
            else if (email < 0 && (v.startsWith("email") || v.startsWith("mail"))) email = idx;
            else if (telephone < 0 && (v.startsWith("telephone") || v.startsWith("tel"))) telephone = idx;
        }
        return new int[]{nom, prenom, email, telephone};
    }

    private String cellule(Row row, int col) {
        if (col < 0) return null;
        Cell c = row.getCell(col);
        if (c == null) return null;
        String v = FORMATTER.formatCellValue(c).trim();
        return v.isEmpty() ? null : v;
    }

    /** Normalise un libellé d'en-tête : minuscules, sans accents, sans symboles. */
    private String normaliser(String s) {
        if (s == null) return "";
        String sansAccents = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase().replaceAll("[^a-z]", "").trim();
    }

    /**
     * Email technique pour un membre sans adresse mail, dérivé de son téléphone.
     * Jamais affiché à l'utilisateur : il se connecte avec son numéro de téléphone.
     * Le téléphone étant unique, l'email dérivé l'est aussi.
     */
    private String emailSynthetique(String telephone) {
        String digits = telephone.replaceAll("[^0-9]", "");
        return "tel-" + digits + "@membre.tontinepro.local";
    }

    private String genererMotDePasse() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String genererMatricule() {
        return membreRepository.genererMatriculeUnique();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record LigneMembre(int numero, String nom, String prenom, String email, String telephone) {}
}
