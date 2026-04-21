package com.memorygame.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entite JPA representant une partie sauvegardee.
 * Mappee sur la table "saved_games" en base de donnees.
 * Le plateau et l'etat des cartes sont serialises en JSON (TEXT).
 */
@Entity
@Table(name = "saved_games")
public class SavedGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int level;

    /** Identifiant du theme : "theme1", "theme2" ou "theme3" */
    @Column(nullable = false, length = 20)
    private String theme = "theme1";

    @Column(nullable = false)
    private int score;

    @Column(name = "moves_count")
    private int movesCount;

    @Column(name = "time_elapsed")
    private int timeElapsed;

    /** Liste des chemins d'images du plateau serialisee en JSON */
    @Column(name = "board_state", columnDefinition = "TEXT")
    private String boardState;

    /** Liste des cartes trouvees (true/false) serialisee en JSON */
    @Column(name = "flipped_state", columnDefinition = "TEXT")
    private String flippedState;

    @Column(name = "saved_at")
    private LocalDateTime savedAt = LocalDateTime.now();

    @Column(name = "is_completed")
    private boolean completed = false;

    public SavedGame() {}

    public Long getId()               { return id; }
    public Player getPlayer()         { return player; }
    public int getLevel()             { return level; }
    public String getTheme()          { return theme; }
    public int getScore()             { return score; }
    public int getMovesCount()        { return movesCount; }
    public int getTimeElapsed()       { return timeElapsed; }
    public String getBoardState()     { return boardState; }
    public String getFlippedState()   { return flippedState; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public boolean isCompleted()      { return completed; }

    public void setId(Long id)                  { this.id = id; }
    public void setPlayer(Player player)        { this.player = player; }
    public void setLevel(int level)             { this.level = level; }
    public void setTheme(String theme)          { this.theme = theme; }
    public void setScore(int score)             { this.score = score; }
    public void setMovesCount(int v)            { this.movesCount = v; }
    public void setTimeElapsed(int v)           { this.timeElapsed = v; }
    public void setBoardState(String s)         { this.boardState = s; }
    public void setFlippedState(String s)       { this.flippedState = s; }
    public void setSavedAt(LocalDateTime t)     { this.savedAt = t; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
