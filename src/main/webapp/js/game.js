/*
 * Fichier  : game.js
 * Projet   : Memory Game
 * Role     : Logique complete du jeu cote client.
 *            Contient l'initialisation des etoiles de fond,
 *            la classe principale MemoryGame et la fonction
 *            d'affichage des notifications toast.
 */

/* -------------------------------------------------------------
   Initialisation des etoiles de fond
   Cree 130 elements .star avec des tailles, positions et
   durees d'animation aleatoires, puis les insere dans
   l'element .stars-layer present dans chaque page.
------------------------------------------------------------- */
(function initStars() {
  const layer = document.querySelector('.stars-layer');
  if (!layer) return;
  const fragment = document.createDocumentFragment();
  for (let i = 0; i < 130; i++) {
    const s = document.createElement('div');
    s.classList.add('star');
    const size = Math.random() * 2.5 + 0.5;
    s.style.cssText =
      `width:${size}px;height:${size}px;` +
      `top:${(Math.random()*100).toFixed(2)}%;` +
      `left:${(Math.random()*100).toFixed(2)}%;` +
      `--dur:${(Math.random()*4+2).toFixed(1)}s;` +
      `--delay:-${(Math.random()*6).toFixed(1)}s;`;
    fragment.appendChild(s);
  }
  layer.appendChild(fragment);
})();

/* =============================================================
   Classe MemoryGame
   Gere l'ensemble de la logique du jeu : affichage du plateau,
   retournement des cartes, detection des paires, timer,
   calcul du score, sauvegarde AJAX et gestion de la pause.
============================================================= */
class MemoryGame {

  /*
   * Constructeur
   * Initialise l'etat interne a partir des options fournies
   * par Thymeleaf (board, level, theme, timeLimit, savedState).
   * Si une sauvegarde existe, restaure l'etat precedent.
   */
  constructor(opts) {
    this.board      = opts.board;
    this.level      = opts.level;
    this.theme      = opts.theme || 'theme1';
    this.timeLimit  = opts.timeLimit;
    this.savedState = opts.savedState || null;

    this.score      = 0;
    this.moves      = 0;
    this.timeLeft   = this.timeLimit;
    this.timerInterval = null;
    this._paused    = false;
    this.locked     = false;

    this.flipped = new Array(this.board.length).fill(false);
    this.matched = new Array(this.board.length).fill(false);
    this.selected = [];

    this.pairsTotal = this.board.length / 2;
    this.pairsFound = 0;

    if (this.savedState) this._restoreState();

    this._render();
    this._bindButtons();
    this._startTimer();
  }

  /*
   * Restauration d'une partie sauvegardee
   * Recharge le score, le nombre de coups, le temps restant
   * et les cartes deja trouvees depuis l'objet savedState.
   */
  _restoreState() {
    const s = this.savedState;
    this.score    = s.score       || 0;
    this.moves    = s.moves       || 0;
    this.timeLeft = Math.max(0, this.timeLimit - (s.timeElapsed || 0));

    if (s.flipped && Array.isArray(s.flipped)) {
      this.matched    = s.flipped.slice();
      this.pairsFound = this.matched.filter(Boolean).length / 2;
    }
  }

  /*
   * Rendu du plateau de jeu
   * Cree dynamiquement les elements DOM pour chaque carte.
   * Les cartes deja trouvees (matched) sont immediatement
   * affichees en face avant.
   */
  _render() {
    const boardEl = document.getElementById('board');
    if (!boardEl) return;
    boardEl.innerHTML = '';
    boardEl.className = `board level-${this.level}`;

    this.board.forEach((imgSrc, i) => {
      const wrap = document.createElement('div');
      wrap.classList.add('card-wrap');
      wrap.dataset.index = i;
      wrap.style.animationDelay = `${(i * 0.04).toFixed(2)}s`;

      if (this.matched[i]) {
        wrap.classList.add('flipped', 'matched');
      }

      wrap.innerHTML = `
        <div class="card-inner">
          <div class="card-face card-back"></div>
          <div class="card-face card-front">
            <img src="${imgSrc}" alt="carte" loading="lazy" draggable="false"/>
          </div>
        </div>
      `;
      wrap.addEventListener('click', () => this._onCardClick(i));
      boardEl.appendChild(wrap);
    });

    this._updateHUD();
  }

  /*
   * Gestion du clic sur une carte
   * Verifie que le clic est valide (carte non deja trouvee,
   * non deja selectionnee, jeu non verrouille ni en pause).
   * Quand deux cartes sont selectionnees, compare les images.
   * Si elles sont identiques : paire trouvee.
   * Sinon : les cartes se retournent apres un delai.
   */
  _onCardClick(index) {
    if (this.locked)               return;
    if (this._paused)              return;
    if (this.matched[index])       return;
    if (this.selected.includes(index)) return;
    if (this.selected.length >= 2) return;

    this._flipCard(index, true);
    this.selected.push(index);

    if (this.selected.length === 2) {
      this.moves++;
      this._updateHUD();
      this.locked = true;

      const [a, b] = this.selected;

      if (this.board[a] === this.board[b]) {
        /* Paire correcte : marquer les deux cartes comme trouvees */
        setTimeout(() => {
          this._markMatched(a);
          this._markMatched(b);
          this.pairsFound++;
          this._updateHUD();
          this.selected = [];
          this.locked   = false;

          if (this.pairsFound === this.pairsTotal) {
            this._onVictory();
          }
        }, 480);

      } else {
        /* Mauvaise paire : animation d'erreur puis retournement */
        const elA = this._getCardEl(a);
        const elB = this._getCardEl(b);
        elA?.classList.add('error');
        elB?.classList.add('error');

        setTimeout(() => {
          this._flipCard(a, false);
          this._flipCard(b, false);
          elA?.classList.remove('error');
          elB?.classList.remove('error');
          this.selected = [];
          this.locked   = false;
        }, 950);
      }
    }
  }

  /* Retourne ou cache une carte en modifiant son etat et sa classe CSS */
  _flipCard(index, show) {
    this.flipped[index] = show;
    const el = this._getCardEl(index);
    el?.classList.toggle('flipped', show);
  }

  /* Marque une carte comme definitivement trouvee */
  _markMatched(index) {
    this.matched[index] = true;
    const el = this._getCardEl(index);
    el?.classList.add('matched');
  }

  /* Recupere l'element DOM d'une carte par son index */
  _getCardEl(index) {
    return document.querySelector(`.card-wrap[data-index="${index}"]`);
  }

  /*
   * Mise a jour du HUD
   * Actualise l'affichage du score, du nombre de coups,
   * du compteur de paires et du timer.
   * Active l'alerte visuelle si le temps restant est inferieur a 20s.
   */
  _updateHUD() {
    const set = (id, v) => { const e = document.getElementById(id); if (e) e.textContent = v; };
    set('hud-score', this.score);
    set('hud-moves', this.moves);
    set('hud-pairs', `${this.pairsFound}/${this.pairsTotal}`);
    set('hud-timer', this._fmtTime(this.timeLeft));

    const timerWrap = document.getElementById('hud-timer-wrap');
    timerWrap?.classList.toggle('warning', this.timeLeft <= 20 && this.timeLeft > 0);
  }

  /* Formate un nombre de secondes en chaine "m:ss" */
  _fmtTime(secs) {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  /*
   * Demarrage du timer
   * Decremente timeLeft chaque seconde.
   * Appelle _onTimeOut quand le temps atteint zero.
   */
  _startTimer() {
    this._updateHUD();
    this.timerInterval = setInterval(() => {
      if (this._paused) return;
      this.timeLeft--;
      this._updateHUD();
      if (this.timeLeft <= 0) {
        clearInterval(this.timerInterval);
        this._onTimeOut();
      }
    }, 1000);
  }

  /* Arrete le timer (appelee a la victoire ou depuis l'exterieur) */
  stopTimer() {
    clearInterval(this.timerInterval);
    this.timerInterval = null;
  }

  /*
   * Gestion de la victoire
   * Calcule le score final, l'envoie au serveur via AJAX,
   * puis affiche la fenetre modale de victoire.
   *
   * Formule du score :
   *   base      = niveau x 1000
   *   movePen   = max(0, coups - paires) x 10
   *   timePen   = tempsUtilise x 2
   *   timeBonus = tempsRestant x 3
   *   score     = max(0, base - movePen - timePen + timeBonus)
   */
  _onVictory() {
    this.stopTimer();
    const timeElapsed = this.timeLimit - this.timeLeft;

    const pairs      = this.pairsTotal;
    const base       = this.level * 1000;
    const extraMoves = Math.max(0, this.moves - pairs);
    const movePen    = extraMoves * 10;
    const timePen    = timeElapsed * 2;
    const timeBonus  = (this.timeLimit - timeElapsed) * 3;
    this.score = Math.max(0, base - movePen - timePen + timeBonus);
    this._updateHUD();

    /* Envoi du score au serveur pour enregistrement en base de donnees */
    fetch('/game/complete', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({
        score:       this.score,
        level:       this.level,
        theme:       this.theme,
        moves:       this.moves,
        timeElapsed: timeElapsed
      })
    }).catch(() => {});

    /* Affichage des resultats dans la fenetre modale */
    const winLevel = document.getElementById('win-level');
    const winTheme = document.getElementById('win-theme');
    if (winLevel) winLevel.textContent = `Niveau ${this.level}`;
    if (winTheme) winTheme.textContent = this._themeName();

    document.getElementById('win-score').textContent = this.score;
    document.getElementById('win-moves').textContent = `${this.moves} coup${this.moves > 1 ? 's' : ''}`;
    document.getElementById('win-time').textContent  = this._fmtTime(timeElapsed);
    document.getElementById('modal-win').classList.add('show');
  }

  /* Retourne le nom lisible du theme actif */
  _themeName() {
    const names = { theme1: 'Princesse', theme2: 'Animaux', theme3: 'Chiffres' };
    return names[this.theme] || this.theme;
  }

  /*
   * Gestion de la defaite (temps ecoule)
   * Grise les cartes non trouvees et affiche la fenetre modale de defaite.
   */
  _onTimeOut() {
    this.locked = true;
    document.querySelectorAll('.card-wrap:not(.matched)').forEach(el => {
      el.style.opacity = '0.35';
    });
    document.getElementById('modal-lose').classList.add('show');
  }

  /*
   * Sauvegarde AJAX de la partie en cours
   * Envoie l'etat complet (plateau, cartes trouvees, score, temps)
   * au serveur via POST /game/save.
   * Affiche un toast pour confirmer le resultat.
   */
  async save() {
    const timeElapsed = this.timeLimit - this.timeLeft;
    const payload = {
      level:       this.level,
      theme:       this.theme,
      score:       this.score,
      moves:       this.moves,
      timeElapsed: timeElapsed,
      board:       this.board,
      flipped:     this.matched
    };

    try {
      const res  = await fetch('/game/save', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(payload)
      });
      const data = await res.json();
      showToast(data.message || 'Sauvegarde !', data.success ? 'success' : 'error');
    } catch {
      showToast('Erreur reseau - sauvegarde echouee', 'error');
    }
  }

  /*
   * Basculement pause / reprise
   * En pause : verrouille le jeu et masque les cartes retournees.
   * En reprise : deverrouille et restaure la visibilite.
   */
  togglePause() {
    this._paused = !this._paused;
    const btn = document.getElementById('btn-pause');

    if (this._paused) {
      this.locked = true;
      if (btn) btn.textContent = 'Reprendre';
      document.querySelectorAll('.card-wrap.flipped:not(.matched)').forEach(el => {
        el.style.opacity = '0';
      });
    } else {
      this.locked = false;
      if (btn) btn.textContent = 'Pause';
      document.querySelectorAll('.card-wrap').forEach(el => {
        el.style.opacity = '1';
      });
    }
  }

  /* Attache les ecouteurs d'evenements aux boutons Sauvegarder et Pause */
  _bindButtons() {
    document.getElementById('btn-save')
      ?.addEventListener('click', () => this.save());
    document.getElementById('btn-pause')
      ?.addEventListener('click', () => this.togglePause());
  }
}

/* =============================================================
   Fonction showToast
   Affiche une notification temporaire en bas a droite de l'ecran.
   Le type peut etre 'success' (vert) ou 'error' (rouge).
   La notification disparait automatiquement apres 3,2 secondes.
============================================================= */
function showToast(message, type = 'success') {
  let toast = document.getElementById('global-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'global-toast';
    toast.className = 'toast';
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.className   = `toast ${type}`;
  void toast.offsetWidth;
  toast.classList.add('show');
  clearTimeout(toast._hideTimeout);
  toast._hideTimeout = setTimeout(() => toast.classList.remove('show'), 3200);
}
