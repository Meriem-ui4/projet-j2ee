package com.memorygame.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entite JPA representant un joueur inscrit.
 * Mappee sur la table "players" en base de donnees.
 * Le mot de passe est stocke sous forme hachee (BCrypt).
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** Empreinte BCrypt du mot de passe. Jamais stocke en clair. */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "best_score")
    private int bestScore = 0;

    @Column(name = "total_games")
    private int totalGames = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SavedGame> savedGames;

    public Player() {}

    public Long getId()                    { return id; }
    public String getUsername()            { return username; }
    public String getEmail()               { return email; }
    public String getPassword()            { return password; }
    public int getBestScore()              { return bestScore; }
    public int getTotalGames()             { return totalGames; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public List<SavedGame> getSavedGames() { return savedGames; }

    public void setId(Long id)                        { this.id = id; }
    public void setUsername(String username)          { this.username = username; }
    public void setEmail(String email)                { this.email = email; }
    public void setPassword(String password)          { this.password = password; }
    public void setBestScore(int bestScore)           { this.bestScore = bestScore; }
    public void setTotalGames(int totalGames)         { this.totalGames = totalGames; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSavedGames(List<SavedGame> sg)     { this.savedGames = sg; }
}
