package com.tontinepro.tontinepro_backend.api.membre.dto;

import java.util.List;

/**
 * Bilan d'un import de membres par fichier Excel.
 *
 * @param totalLignes  nombre de lignes de données lues dans le fichier
 * @param crees        nouveaux comptes créés (mail de bienvenue + mot de passe temporaire envoyé)
 * @param rattaches    comptes existants rattachés à la tontine (aucun mail envoyé)
 * @param ignores      lignes ignorées (déjà membre de la tontine, ou en erreur)
 * @param nouveaux     détail des comptes créés (pour affichage)
 * @param avertissements messages par ligne (erreurs de validation, doublons, etc.)
 */
public record MembreImportResponse(
        int totalLignes,
        int crees,
        int rattaches,
        int ignores,
        List<MembreCree> nouveaux,
        List<String> avertissements
) {
    /**
     * @param email                 email réel, ou null si le membre n'en a pas
     * @param telephone             numéro de téléphone (identifiant de connexion si pas d'email)
     * @param motDePasseTemporaire  renseigné uniquement pour les membres SANS email
     *                              (le mot de passe ne pouvant pas être envoyé par mail,
     *                              l'admin doit le communiquer lui-même) ; null sinon
     */
    public record MembreCree(
            String nom,
            String prenom,
            String email,
            String telephone,
            String matricule,
            String motDePasseTemporaire
    ) {}
}
