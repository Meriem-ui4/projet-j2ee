package com.memorygame.repository;

import com.memorygame.model.ScoreEntry;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository Hibernate pour l'acces aux donnees de l'entite ScoreEntry.
 * Fournit les operations d'insertion et de lecture du classement general.
 */
@Repository
@Transactional
public class ScoreEntryRepository {

    @Autowired
    private SessionFactory sessionFactory;

    /** Enregistre une nouvelle entree de score apres une partie gagnee. */
    public void save(ScoreEntry entry) {
        sessionFactory.getCurrentSession().save(entry);
    }

    /** Retourne toutes les entrees du classement triees par score decroissant. */
    public List<ScoreEntry> findAllOrderByScore() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM ScoreEntry ORDER BY score DESC", ScoreEntry.class)
                .list();
    }

    /** Retourne les N meilleurs scores du classement.
     * @param limit Nombre maximum de resultats
     */
    public List<ScoreEntry> findTopScores(int limit) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM ScoreEntry ORDER BY score DESC", ScoreEntry.class)
                .setMaxResults(limit)
                .list();
    }
}
