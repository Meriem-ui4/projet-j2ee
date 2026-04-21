package com.memorygame.service;

import com.memorygame.model.Player;
import com.memorygame.repository.PlayerRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service metier pour la gestion des comptes joueurs.
 *
 * Securite :
 *   - Les mots de passe sont hasches avec BCrypt (facteur 12).
 *   - BCrypt ne permet pas de retrouver le mot de passe original :
 *     la verification re-hashe et compare les empreintes.
 *   - L'email est valide par expression reguliere avant insertion.
 */
@Service
@Transactional
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    private static final int    USERNAME_MIN     = 3;
    private static final int    USERNAME_MAX     = 20;
    private static final int    PASSWORD_MIN     = 6;
    private static final String USERNAME_PATTERN = "[a-zA-Z0-9_]+";
    private static final String EMAIL_PATTERN    = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * Inscrit un nouveau joueur apres validation de tous les champs.
     * Verifie l'unicite du nom d'utilisateur et de l'email.
     *
     * @param username Nom d'utilisateur (3 a 20 caracteres alphanumeriques)
     * @param email    Adresse email valide et unique
     * @param password Mot de passe (minimum 6 caracteres)
     * @return null si l'inscription reussit, ou un message d'erreur
     */
    public String register(String username, String email, String password) {

        if (username == null || username.isBlank())
            return "Le nom d'utilisateur est obligatoire.";
        username = username.trim();
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX)
            return "Le nom d'utilisateur doit contenir entre " + USERNAME_MIN + " et " + USERNAME_MAX + " caracteres.";
        if (!username.matches(USERNAME_PATTERN))
            return "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et _.";

        if (email == null || email.isBlank())
            return "L'adresse email est obligatoire.";
        email = email.trim().toLowerCase();
        if (!email.matches(EMAIL_PATTERN))
            return "L'adresse email n'est pas valide.";

        if (password == null || password.length() < PASSWORD_MIN)
            return "Le mot de passe doit contenir au moins " + PASSWORD_MIN + " caracteres.";

        if (playerRepository.findByUsername(username).isPresent())
            return "Ce nom d'utilisateur est deja pris.";

        if (playerRepository.findByEmail(email).isPresent())
            return "Cette adresse email est deja utilisee.";

        Player player = new Player();
        player.setUsername(username);
        player.setEmail(email);
        player.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        playerRepository.save(player);
        return null;
    }

    /**
     * Authentifie un joueur par nom d'utilisateur ou email.
     * Cherche d'abord par username, puis par email si non trouve.
     * Verifie le mot de passe avec BCrypt.
     *
     * @param usernameOrEmail Nom d'utilisateur ou adresse email
     * @param password        Mot de passe en clair a verifier
     * @return Optional contenant le joueur si authentifie, vide sinon
     */
    public Optional<Player> login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) return Optional.empty();

        String input = usernameOrEmail.trim();

        Optional<Player> found = playerRepository.findByUsername(input);
        if (found.isEmpty()) {
            found = playerRepository.findByEmail(input.toLowerCase());
        }

        return found.filter(p -> BCrypt.checkpw(password, p.getPassword()));
    }

    /**
     * Met a jour le meilleur score et incremente le compteur de parties.
     * Le meilleur score n'est mis a jour que si le nouveau score est superieur.
     *
     * @param playerId Identifiant du joueur
     * @param newScore Score obtenu lors de la partie terminee
     */
    public void updateBestScore(Long playerId, int newScore) {
        playerRepository.findById(playerId).ifPresent(player -> {
            if (newScore > player.getBestScore()) {
                player.setBestScore(newScore);
            }
            player.setTotalGames(player.getTotalGames() + 1);
            playerRepository.update(player);
        });
    }

    /**
     * Retourne les N meilleurs joueurs classes par score decroissant.
     * @param limit Nombre maximum de joueurs a retourner
     */
    public List<Player> getLeaderboard(int limit) {
        return playerRepository.findTopScores(limit);
    }

    /**
     * Recherche un joueur par son identifiant.
     * @param id Identifiant du joueur
     * @return Optional contenant le joueur, ou vide si inexistant
     */
    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }
}
