package com.memorygame.controller;

import com.memorygame.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Controleur Spring MVC pour l'authentification.
 * Gere les operations de connexion, inscription et deconnexion.
 * Un token CSRF est genere pour chaque formulaire afin de prevenir
 * les attaques de type Cross-Site Request Forgery.
 */
@Controller
public class AuthController {

    @Autowired
    private PlayerService playerService;

    /**
     * Redirige la racine vers le menu si connecte, sinon vers la connexion.
     */
    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("playerId") != null) return "redirect:/game/menu";
        return "redirect:/login";
    }

    /**
     * Affiche la page de connexion avec un token CSRF.
     * Redirige vers le menu si le joueur est deja connecte.
     */
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("playerId") != null) return "redirect:/game/menu";
        String csrf = UUID.randomUUID().toString();
        session.setAttribute("csrfLogin", csrf);
        model.addAttribute("csrf", csrf);
        return "auth/login";
    }

    /**
     * Traite le formulaire de connexion.
     * Valide le token CSRF, authentifie le joueur via PlayerService,
     * puis invalide l'ancienne session avant d'en creer une nouvelle.
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String csrf,
                          HttpSession session,
                          Model model) {

        String expectedCsrf = (String) session.getAttribute("csrfLogin");
        if (expectedCsrf == null || !expectedCsrf.equals(csrf)) {
            model.addAttribute("error", "Requete invalide. Veuillez reessayer.");
            return refreshLoginCsrf(session, model);
        }

        return playerService.login(username, password)
                .map(player -> {
                    session.invalidate();
                    return "redirect:/game/menu?login=" + player.getId() + "&user=" + player.getUsername();
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Identifiants incorrects. Veuillez reessayer.");
                    return refreshLoginCsrf(session, model);
                });
    }

    /** Regenere un token CSRF pour la page de connexion apres une erreur. */
    private String refreshLoginCsrf(HttpSession session, Model model) {
        String csrf = UUID.randomUUID().toString();
        session.setAttribute("csrfLogin", csrf);
        model.addAttribute("csrf", csrf);
        return "auth/login";
    }

    /**
     * Reinjecte les attributs de session apres l'invalidation post-login.
     * La redirection vers /game/menu passe l'id et le nom via parametres URL,
     * ce point d'entree les recupere et les stocke dans la nouvelle session.
     */
    @GetMapping("/game/menu")
    public String menuRedirect(@RequestParam(required = false) Long login,
                                @RequestParam(required = false) String user,
                                HttpSession session) {
        if (login != null && user != null && session.getAttribute("playerId") == null) {
            session.setAttribute("playerId", login);
            session.setAttribute("username", user);
        }
        return "forward:/game/menu-view";
    }

    /**
     * Affiche la page d'inscription avec un token CSRF.
     */
    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session.getAttribute("playerId") != null) return "redirect:/game/menu";
        String csrf = UUID.randomUUID().toString();
        session.setAttribute("csrfRegister", csrf);
        model.addAttribute("csrf", csrf);
        return "auth/register";
    }

    /**
     * Traite le formulaire d'inscription.
     * Valide le token CSRF et delegue la creation du compte a PlayerService.
     */
    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam(required = false) String csrf,
                              HttpSession session,
                              Model model) {

        String expectedCsrf = (String) session.getAttribute("csrfRegister");
        if (expectedCsrf == null || !expectedCsrf.equals(csrf)) {
            model.addAttribute("error", "Requete invalide. Veuillez reessayer.");
            return refreshRegisterCsrf(session, model);
        }

        String errorMsg = playerService.register(username, email, password);
        if (errorMsg != null) {
            model.addAttribute("error", errorMsg);
            return refreshRegisterCsrf(session, model);
        }

        return "redirect:/login?registered=true";
    }

    /** Regenere un token CSRF pour la page d'inscription apres une erreur. */
    private String refreshRegisterCsrf(HttpSession session, Model model) {
        String csrf = UUID.randomUUID().toString();
        session.setAttribute("csrfRegister", csrf);
        model.addAttribute("csrf", csrf);
        return "auth/register";
    }

    /**
     * Invalide la session HTTP et redirige vers la page de connexion.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
