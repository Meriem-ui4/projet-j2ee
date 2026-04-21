package com.memorygame.repository;

import com.memorygame.model.Player;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository Hibernate pour l'acces aux donnees de l'entite Player.
 * Fournit les operations CRUD et les requetes metier sur la table players.
 */
@Repository
@Transactional
public class PlayerRepository {

    @Autowired
    private SessionFactory sessionFactory;

    /** Persiste un nouveau joueur en base de donnees. */
    public void save(Player player) {
        sessionFactory.getCurrentSession().save(player);
    }

    /** Met a jour un joueur existant (meilleur score, total parties). */
    public void update(Player player) {
        sessionFactory.getCurrentSession().merge(player);
    }

    /**
     * Recherche un joueur par son nom d'utilisateur (insensible a la casse).
     * @param username Nom d'utilisateur a rechercher
     * @return Optional contenant le joueur, ou vide si non trouve
     */
    public Optional<Player> findByUsername(String username) {
        List<Player> result = sessionFactory.getCurrentSession()
                .createQuery("FROM Player WHERE LOWER(username) = LOWER(:u)", Player.class)
                .setParameter("u", username)
                .list();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Recherche un joueur par son adresse email (insensible a la casse).
     * @param email Adresse email a rechercher
     * @return Optional contenant le joueur, ou vide si non trouve
     */
    public Optional<Player> findByEmail(String email) {
        List<Player> result = sessionFactory.getCurrentSession()
                .createQuery("FROM Player WHERE LOWER(email) = LOWER(:e)", Player.class)
                .setParameter("e", email)
                .list();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Recherche un joueur par son identifiant numerique.
     * @param id Identifiant du joueur
     * @return Optional contenant le joueur, ou vide si inexistant
     */
    public Optional<Player> findById(Long id) {
        Player p = sessionFactory.getCurrentSession().get(Player.class, id);
        return Optional.ofNullable(p);
    }

    /**
     * Retourne les N meilleurs joueurs classes par meilleur score decroissant.
     * @param limit Nombre maximum de resultats
     */
    public List<Player> findTopScores(int limit) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Player ORDER BY bestScore DESC", Player.class)
                .setMaxResults(limit)
                .list();
    }
}
