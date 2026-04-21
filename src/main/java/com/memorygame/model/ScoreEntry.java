package com.memorygame.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entite JPA representant une entree du classement.
 * Mappee sur la table "score_entries" en base de donnees.
 * Une entree est creee a chaque partie gagnee.
 */
@Entity
@Table(name = "score_entries")
public class ScoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false, length = 20)
    private String theme;

    @Column(nullable = false)
    private int score;

    @Column(name = "moves_count")
    private int movesCount;

    @Column(name = "time_elapsed")
    private int timeElapsed;

    @Column(name = "played_at")
    private LocalDateTime playedAt = LocalDateTime.now();

    public ScoreEntry() {}

    public Long getId()                { return id; }
    public Player getPlayer()          { return player; }
    public int getLevel()              { return level; }
    public String getTheme()           { return theme; }
    public int getScore()              { return score; }
    public int getMovesCount()         { return movesCount; }
    public int getTimeElapsed()        { return timeElapsed; }
    public LocalDateTime getPlayedAt() { return playedAt; }

    public void setId(Long id)                 { this.id = id; }
    public void setPlayer(Player player)       { this.player = player; }
    public void setLevel(int level)            { this.level = level; }
    public void setTheme(String theme)         { this.theme = theme; }
    public void setScore(int score)            { this.score = score; }
    public void setMovesCount(int v)           { this.movesCount = v; }
    public void setTimeElapsed(int v)          { this.timeElapsed = v; }
    public void setPlayedAt(LocalDateTime t)   { this.playedAt = t; }
}
