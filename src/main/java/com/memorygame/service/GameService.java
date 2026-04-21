package com.memorygame.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.model.Player;
import com.memorygame.model.SavedGame;
import com.memorygame.model.ScoreEntry;
import com.memorygame.repository.SavedGameRepository;
import com.memorygame.repository.ScoreEntryRepository;

/**
 * Service metier principal du jeu memoire.
 *
 * Themes disponibles :
 *   theme1 : Princesse  (images dans webapp/images/theme1/)
 *   theme2 : Animaux    (images dans webapp/images/theme2/)
 *   theme3 : Chiffres   (images dans webapp/images/theme3/)
 *
 * Nombre de paires par niveau :
 *   Niveau 1 (Facile)    : 8 paires,  temps limite 120 secondes
 *   Niveau 2 (Moyen)     : 10 paires, temps limite 90 secondes
 *   Niveau 3 (Difficile) : 12 paires, temps limite 60 secondes
 *
 * Formule de calcul du score :
 *   base      = niveau x 1000
 *   movePen   = max(0, coups - paires) x 10
 *   timePen   = tempsUtilise x 2
 *   timeBonus = (tempsLimite - tempsUtilise) x 3
 *   score     = max(0, base - movePen - timePen + timeBonus)
 */
@Service
@Transactional
public class GameService {

    @Autowired private SavedGameRepository savedGameRepository;
    @Autowired private ScoreEntryRepository scoreEntryRepository;
    @Autowired private PlayerService playerService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Noms des cartes par theme et par niveau.
     * Chaque entree contient les noms de fichiers sans extension.
     * Le chemin complet est construit dans generateBoard().
     */
    private static final Map<String, Map<Integer, List<String>>> THEME_CARDS;

    static {
        THEME_CARDS = new HashMap<>();

        Map<Integer, List<String>> t1 = new HashMap<>();
        t1.put(1, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8"));
        t1.put(2, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10"));
        t1.put(3, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10","card11","card12"));
        THEME_CARDS.put("theme1", t1);

        Map<Integer, List<String>> t2 = new HashMap<>();
        t2.put(1, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8"));
        t2.put(2, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10"));
        t2.put(3, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10","card11","card12"));
        THEME_CARDS.put("theme2", t2);

        Map<Integer, List<String>> t3 = new HashMap<>();
        t3.put(1, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8"));
        t3.put(2, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10"));
        t3.put(3, Arrays.asList("card1","card2","card3","card4","card5","card6","card7","card8","card9","card10","card11","card12"));
        THEME_CARDS.put("theme3", t3);
    }

    /** Noms affichables des themes, utilises dans les vues Thymeleaf */
    public static final Map<String, String> THEME_NAMES = Map.of(
        "theme1", "Princesse",
        "theme2", "Animaux",
        "theme3", "Chiffres"
    );

    /**
     * Retourne la configuration d'un niveau (dimensions, paires, temps limite).
     * @param level Niveau de 1 a 3
     * @return Map contenant rows, cols, pairs, timeLimit et name
     */
    public Map<String, Object> getLevelConfig(int level) {
        Map<String, Object> config = new HashMap<>();
        switch (level) {
            case 1 -> {
                config.put("rows",      4);
                config.put("cols",      4);
                config.put("pairs",     8);
                config.put("timeLimit", 120);
                config.put("name",      "Facile");
            }
            case 2 -> {
                config.put("rows",      4);
                config.put("cols",      5);
                config.put("pairs",     10);
                config.put("timeLimit", 90);
                config.put("name",      "Moyen");
            }
            case 3 -> {
                config.put("rows",      4);
                config.put("cols",      6);
                config.put("pairs",     12);
                config.put("timeLimit", 60);
                config.put("name",      "Difficile");
            }
            default -> throw new IllegalArgumentException("Niveau invalide : " + level);
        }
        return config;
    }

    /**
     * Genere un plateau de jeu melange.
     * Construit les chemins relatifs des images (ex: images/theme1/niveau1/card1.jpg),
     * duplique la liste pour former les paires, puis melange aleatoirement.
     *
     * @param level Niveau de difficulte (1, 2 ou 3)
     * @param theme Identifiant du theme (theme1, theme2 ou theme3)
     * @return Liste des chemins d'images dans un ordre aleatoire
     */
    public List<String> generateBoard(int level, String theme) {
        Map<Integer, List<String>> themeCards = THEME_CARDS.getOrDefault(theme, THEME_CARDS.get("theme1"));
        List<String> cards = themeCards.get(level);

        String basePath = "images/" + theme + "/niveau" + level + "/";
        List<String> imagePaths = new ArrayList<>();
        for (String card : cards) {
            imagePaths.add(basePath + card + ".jpg");
        }

        List<String> board = new ArrayList<>(imagePaths);
        board.addAll(imagePaths);
        Collections.shuffle(board);
        return board;
    }

    /**
     * Calcule le score final d'une partie terminee.
     *
     * @param level       Niveau joue (1, 2 ou 3)
     * @param moves       Nombre total de coups effectues
     * @param timeElapsed Temps utilise en secondes
     * @param timeLimit   Temps limite du niveau en secondes
     * @return Score calcule (minimum 0)
     */
    public int calculateScore(int level, int moves, int timeElapsed, int timeLimit) {
        int pairs      = (int) getLevelConfig(level).get("pairs");
        int base       = level * 1000;
        int extraMoves = Math.max(0, moves - pairs);
        int movePen    = extraMoves * 10;
        int timePen    = timeElapsed * 2;
        int timeBonus  = (timeLimit - timeElapsed) * 3;
        return Math.max(0, base - movePen - timePen + timeBonus);
    }

    /**
     * Sauvegarde l'etat courant d'une partie en base de donnees.
     * Si une sauvegarde precedente existe pour ce joueur, elle est remplacee.
     *
     * @param playerId    Identifiant du joueur
     * @param level       Niveau en cours
     * @param theme       Theme en cours
     * @param score       Score actuel
     * @param moves       Nombre de coups joues
     * @param timeElapsed Temps ecoule en secondes
     * @param board       Liste des chemins d'images du plateau
     * @param flipped     Liste des cartes deja trouvees (true = trouvee)
     * @return L'objet SavedGame persiste
     */
    public SavedGame saveGame(Long playerId, int level, String theme, int score, int moves,
                               int timeElapsed, List<String> board, List<Boolean> flipped) {
        try {
            Player player = playerService.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Joueur introuvable"));

            savedGameRepository.findLatestByPlayerId(playerId)
                    .ifPresent(old -> savedGameRepository.delete(old.getId()));

            SavedGame sg = new SavedGame();
            sg.setPlayer(player);
            sg.setLevel(level);
            sg.setTheme(theme != null ? theme : "theme1");
            sg.setScore(score);
            sg.setMovesCount(moves);
            sg.setTimeElapsed(timeElapsed);
            sg.setBoardState(mapper.writeValueAsString(board));
            sg.setFlippedState(mapper.writeValueAsString(flipped));
            sg.setSavedAt(LocalDateTime.now());
            sg.setCompleted(false);
            savedGameRepository.save(sg);
            return sg;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sauvegarde : " + e.getMessage(), e);
        }
    }

    /**
     * Charge la derniere partie non terminee d'un joueur.
     * @param playerId Identifiant du joueur
     * @return Optional contenant la sauvegarde, ou vide si aucune
     */
    public Optional<SavedGame> loadGame(Long playerId) {
        return savedGameRepository.findLatestByPlayerId(playerId);
    }

    /**
     * Enregistre la fin d'une partie gagnee.
     * Met a jour le meilleur score du joueur et ajoute une entree au classement.
     *
     * @param playerId   Identifiant du joueur
     * @param level      Niveau joue
     * @param theme      Theme joue
     * @param finalScore Score final calcule
     * @param moves      Nombre de coups
     * @param timeElapsed Temps utilise en secondes
     */
    public void completeGame(Long playerId, int level, String theme, int finalScore,
                              int moves, int timeElapsed) {
        playerService.updateBestScore(playerId, finalScore);

        playerService.findById(playerId).ifPresent(player -> {
            ScoreEntry entry = new ScoreEntry();
            entry.setPlayer(player);
            entry.setLevel(level);
            entry.setTheme(theme != null ? theme : "theme1");
            entry.setScore(finalScore);
            entry.setMovesCount(moves);
            entry.setTimeElapsed(timeElapsed);
            entry.setPlayedAt(LocalDateTime.now());
            scoreEntryRepository.save(entry);
        });
    }

    /**
     * Retourne tous les scores du classement tries par score decroissant.
     */
    public List<ScoreEntry> getLeaderboard() {
        return scoreEntryRepository.findAllOrderByScore();
    }
}
