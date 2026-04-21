package com.memorygame.controller;

import com.memorygame.model.SavedGame;
import com.memorygame.model.ScoreEntry;
import com.memorygame.service.GameService;
import com.memorygame.service.PlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Controleur Spring MVC pour le jeu memoire.
 * Gere les routes du menu, du demarrage de partie, de la reprise
 * d'une sauvegarde, des endpoints AJAX (sauvegarde et fin de partie)
 * et du classement.
 *
 * Toutes les routes protegees verifient que le joueur est connecte
 * avant d'executer l'action (methode isLoggedIn).
 */
@Controller
@RequestMapping("/game")
public class GameController {

    @Autowired private GameService   gameService;
    @Autowired private PlayerService playerService;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Retourne l'identifiant du joueur stocke en session. */
    private Long getPlayerId(HttpSession session) {
        return (Long) session.getAttribute("playerId");
    }

    /** Verifie si un joueur est connecte en session. */
    private boolean isLoggedIn(HttpSession session) {
        return getPlayerId(session) != null;
    }

    /**
     * Affiche le menu principal avec la selection du theme et du niveau.
     * Injecte les donnees du joueur, la presence d'une sauvegarde et le classement.
     */
    @GetMapping("/menu-view")
    public String menuView(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Long playerId = getPlayerId(session);
        playerService.findById(playerId).ifPresent(p -> model.addAttribute("player", p));
        model.addAttribute("leaderboard", gameService.getLeaderboard());
        model.addAttribute("hasSave",     gameService.loadGame(playerId).isPresent());
        model.addAttribute("themeNames",  GameService.THEME_NAMES);
        return "game/menu";
    }

    /**
     * Alias du menu principal (accessible via le lien Accueil du jeu).
     */
    @GetMapping("/accueil")
    public String accueil(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Long playerId = getPlayerId(session);
        playerService.findById(playerId).ifPresent(p -> model.addAttribute("player", p));
        model.addAttribute("leaderboard", gameService.getLeaderboard());
        model.addAttribute("hasSave",     gameService.loadGame(playerId).isPresent());
        model.addAttribute("themeNames",  GameService.THEME_NAMES);
        return "game/menu";
    }

    /**
     * Demarre une nouvelle partie pour le niveau et le theme demandes.
     * Genere un plateau melange via GameService et injecte toutes les
     * donnees necessaires a la vue play.html.
     *
     * @param level Niveau de 1 a 3 (valide par verification)
     * @param theme Identifiant du theme (theme1, theme2 ou theme3)
     */
    @GetMapping("/start/{level}")
    public String startGame(@PathVariable int level,
                             @RequestParam(defaultValue = "theme1") String theme,
                             HttpSession session,
                             Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";
        if (level < 1 || level > 3) return "redirect:/game/accueil";
        if (!List.of("theme1","theme2","theme3").contains(theme)) theme = "theme1";

        List<String>       board  = gameService.generateBoard(level, theme);
        Map<String, Object> config = gameService.getLevelConfig(level);

        model.addAttribute("board",        board);
        model.addAttribute("level",        level);
        model.addAttribute("theme",        theme);
        model.addAttribute("themeName",    GameService.THEME_NAMES.get(theme));
        model.addAttribute("config",       config);
        model.addAttribute("username",     session.getAttribute("username"));
        model.addAttribute("savedGame",    (Object) null);
        model.addAttribute("flippedState", (Object) null);
        return "game/play";
    }

    /**
     * Reprend la derniere partie sauvegardee du joueur connecte.
     * Deserialise le plateau et l'etat des cartes depuis la base de donnees.
     * Redirige vers le menu si aucune sauvegarde n'est trouvee.
     */
    @GetMapping("/resume")
    public String resumeGame(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";

        Optional<SavedGame> saved = gameService.loadGame(getPlayerId(session));
        if (saved.isEmpty()) return "redirect:/game/accueil";

        SavedGame sg = saved.get();
        try {
            List<String>  board   = mapper.readValue(sg.getBoardState(),   List.class);
            List<Boolean> flipped = mapper.readValue(sg.getFlippedState(), List.class);
            Map<String, Object> config = gameService.getLevelConfig(sg.getLevel());
            String theme = sg.getTheme() != null ? sg.getTheme() : "theme1";

            model.addAttribute("board",        board);
            model.addAttribute("level",        sg.getLevel());
            model.addAttribute("theme",        theme);
            model.addAttribute("themeName",    GameService.THEME_NAMES.get(theme));
            model.addAttribute("config",       config);
            model.addAttribute("username",     session.getAttribute("username"));
            model.addAttribute("savedGame",    sg);
            model.addAttribute("flippedState", flipped);
        } catch (Exception e) {
            return "redirect:/game/accueil";
        }
        return "game/play";
    }

    /**
     * Endpoint AJAX (POST /game/save) pour sauvegarder une partie en cours.
     * Appele par le bouton Sauvegarder en JavaScript (fetch API).
     * Retourne un objet JSON indiquant le succes ou l'echec.
     */
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveGame(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isLoggedIn(session))
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Non connecte"));

        try {
            Long   playerId    = getPlayerId(session);
            int    level       = (int)    body.get("level");
            String theme       = (String) body.getOrDefault("theme", "theme1");
            int    score       = (int)    body.get("score");
            int    moves       = (int)    body.get("moves");
            int    timeElapsed = (int)    body.get("timeElapsed");
            List<String>  board   = (List<String>)  body.get("board");
            List<Boolean> flipped = (List<Boolean>) body.get("flipped");

            gameService.saveGame(playerId, level, theme, score, moves, timeElapsed, board, flipped);
            return ResponseEntity.ok(Map.of("success", true, "message", "Partie sauvegardee !"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Erreur : " + e.getMessage()));
        }
    }

    /**
     * Endpoint AJAX (POST /game/complete) appele a la victoire.
     * Enregistre le score final dans le classement via GameService.
     * Retourne un objet JSON indiquant le succes ou l'echec.
     */
    @PostMapping("/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeGame(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        if (!isLoggedIn(session))
            return ResponseEntity.status(401).body(Map.of("success", false));

        try {
            int    score       = (int)    body.get("score");
            int    level       = (int)    body.get("level");
            String theme       = (String) body.getOrDefault("theme", "theme1");
            int    moves       = (int)    body.getOrDefault("moves", 0);
            int    timeElapsed = (int)    body.getOrDefault("timeElapsed", 0);

            gameService.completeGame(getPlayerId(session), level, theme, score, moves, timeElapsed);
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
    }

    /**
     * Affiche la page du classement general.
     * Accessible meme sans etre connecte.
     */
    @GetMapping("/leaderboard")
    public String leaderboard(HttpSession session, Model model) {
        model.addAttribute("scores",     gameService.getLeaderboard());
        model.addAttribute("themeNames", GameService.THEME_NAMES);
        model.addAttribute("isLoggedIn", isLoggedIn(session));
        return "game/leaderboard";
    }
}
