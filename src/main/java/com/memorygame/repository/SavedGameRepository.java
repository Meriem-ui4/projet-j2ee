package com.memorygame.repository;

import com.memorygame.model.SavedGame;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository Hibernate pour l'acces aux donnees de l'entite SavedGame.
 * Fournit les operations de sauvegarde, chargement et suppression
 * des parties en cours.
 */
@Repository
@Transactional
public class SavedGameRepository {

    @Autowired
    private SessionFactory sessionFactory;

    /** Persiste ou met a jour une partie sauvegardee. */
    public void save(SavedGame game) {
        sessionFactory.getCurrentSession().saveOrUpdate(game);
    }

    /**
     * Recherche une sauvegarde par son identifiant.
     * @param id Identifiant de la sauvegarde
     * @return Optional contenant la sauvegarde, ou vide si inexistante
     */
    public Optional<SavedGame> findById(Long id) {
        SavedGame g = sessionFactory.getCurrentSession().get(SavedGame.class, id);
        return Optional.ofNullable(g);
    }

    /**
     * Retourne toutes les sauvegardes d'un joueur, triees par date decroissante.
     * @param playerId Identifiant du joueur
     */
    public List<SavedGame> findByPlayerId(Long playerId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                    "FROM SavedGame WHERE player.id = :pid ORDER BY savedAt DESC",
                    SavedGame.class)
                .setParameter("pid", playerId)
                .list();
    }

    /**
     * Retourne la derniere partie non terminee d'un joueur (pour la reprise).
     * @param playerId Identifiant du joueur
     * @return Optional contenant la sauvegarde, ou vide si aucune
     */
    public Optional<SavedGame> findLatestByPlayerId(Long playerId) {
        List<SavedGame> result = sessionFactory.getCurrentSession()
                .createQuery(
                    "FROM SavedGame WHERE player.id = :pid AND completed = false ORDER BY savedAt DESC",
                    SavedGame.class)
                .setParameter("pid", playerId)
                .setMaxResults(1)
                .list();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Supprime une sauvegarde par son identifiant.
     * @param id Identifiant de la sauvegarde a supprimer
     */
    public void delete(Long id) {
        SavedGame g = sessionFactory.getCurrentSession().get(SavedGame.class, id);
        if (g != null) sessionFactory.getCurrentSession().delete(g);
    }
}
