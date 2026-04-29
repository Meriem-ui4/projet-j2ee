/* Initialisation des etoiles de fond */
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

/* MINI-JEU LABYRINTHE */
const MAZE_ROWS    = 7;
const MAZE_COLS    = 7;
const CELL_PX      = 40;
const MAZE_SECONDS = 10;
const MAZE_BONUS   = 10;

let mazeGrid     = [];
let mazePlayer   = { r: 0, c: 0 };
let mazeActive   = false;
let mazeInterval = null;
 
function mazeShuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}
 
function generateMaze() {
  mazeGrid = Array.from({ length: MAZE_ROWS }, () => Array(MAZE_COLS).fill(true));
  const visited = Array.from({ length: MAZE_ROWS }, () => Array(MAZE_COLS).fill(false));
  function dfs(r, c) {
    visited[r][c]  = true;
    mazeGrid[r][c] = false;
    for (const [dr, dc] of mazeShuffle([[-2,0],[2,0],[0,-2],[0,2]])) {
      const nr = r + dr, nc = c + dc;
      if (nr >= 0 && nr < MAZE_ROWS && nc >= 0 && nc < MAZE_COLS && !visited[nr][nc]) {
        mazeGrid[r + dr/2][c + dc/2] = false;
        dfs(nr, nc);
      }
    }
  }
  dfs(0, 0);
  mazeGrid[0][0]                     = false;
  mazeGrid[MAZE_ROWS-1][MAZE_COLS-1] = false;
}
 
function drawMaze() {
  const canvas = document.getElementById('mazeCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  for (let r = 0; r < MAZE_ROWS; r++) {
    for (let c = 0; c < MAZE_COLS; c++) {
      const x = c * CELL_PX, y = r * CELL_PX;
      ctx.fillStyle = mazeGrid[r][c] ? '#07102a' : '#1a2f5e';
      ctx.fillRect(x, y, CELL_PX, CELL_PX);
      if (!mazeGrid[r][c]) {
        ctx.strokeStyle = '#ffffff0a';
        ctx.strokeRect(x, y, CELL_PX, CELL_PX);
      }
      if (r === MAZE_ROWS-1 && c === MAZE_COLS-1) {
        ctx.font = `${CELL_PX * 0.65}px serif`;
        ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
        ctx.fillText('🏁', x + CELL_PX/2, y + CELL_PX/2 + 1);
      }
    }
  }
  const px = mazePlayer.c * CELL_PX + CELL_PX/2;
  const py = mazePlayer.r * CELL_PX + CELL_PX/2;
  ctx.beginPath();
  ctx.arc(px, py, CELL_PX * 0.28, 0, Math.PI * 2);
  ctx.fillStyle = '#c9a84c'; ctx.shadowColor = '#c9a84c'; ctx.shadowBlur = 12;
  ctx.fill(); ctx.shadowBlur = 0;
}
 
function closeMaze(won, gameInstance) {
  clearInterval(mazeInterval);
  mazeActive = false;
  const modal = document.getElementById('mazeModal');
  if (modal) modal.style.display = 'none';
  if (won) {
    showToast(`🎉 Bravo ! +${MAZE_BONUS} secondes bonus !`, "success");
    if (gameInstance) gameInstance.addTime(MAZE_BONUS);
  } else {
    showToast('😅 Dommage... La partie continue !', "error");
  }
  if (gameInstance) gameInstance.resumeTimer();
}
 
function openMaze(gameInstance) {
  if (mazeActive) return;
  mazeActive = true;
  mazePlayer = { r: 0, c: 0 };
  if (gameInstance) gameInstance.pauseTimer();
  generateMaze();
  const modal = document.getElementById('mazeModal');
  if (!modal) { if (gameInstance) gameInstance.resumeTimer(); mazeActive = false; return; }
  modal.style.display = 'flex';
  drawMaze();
  let t = MAZE_SECONDS;
  const timerEl = document.getElementById('mazeTimerDisplay');
  if (timerEl) timerEl.textContent = t;
  clearInterval(mazeInterval);
  mazeInterval = setInterval(() => {
    t--;
    if (timerEl) { timerEl.textContent = t; timerEl.style.color = t <= 3 ? '#ff1744' : '#f44336'; }
    if (t <= 0) { clearInterval(mazeInterval); closeMaze(false, gameInstance); }
  }, 1000);
}
 
window.mazeNotifyPair = function(gameInstance) {
  if (!mazeActive) openMaze(gameInstance);
};
 
/* Contrôles clavier du labyrinthe */
document.addEventListener('keydown', function(e) {
  if (!mazeActive) return;
  const moves = { ArrowUp:[-1,0], ArrowDown:[1,0], ArrowLeft:[0,-1], ArrowRight:[0,1] };
  if (!moves[e.key]) return;
  e.preventDefault();
  const [dr, dc] = moves[e.key];
  const nr = mazePlayer.r + dr, nc = mazePlayer.c + dc;
  if (nr >= 0 && nr < MAZE_ROWS && nc >= 0 && nc < MAZE_COLS && !mazeGrid[nr][nc]) {
    mazePlayer = { r: nr, c: nc };
    drawMaze();
    if (nr === MAZE_ROWS-1 && nc === MAZE_COLS-1) {
      clearInterval(mazeInterval);
      closeMaze(true, window.currentGame);
    }
  }
});

class MemoryGame {

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
    this._mazeActive = false;
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

  /* Restauration d'une partie sauvegardee */
  _restoreState() {
    const s = this.savedState;
    this.score    = s.score       || 0;
    this.moves    = s.moves       || 0;
    
    const elapsed = Math.min(s.timeElapsed || 0, this.timeLimit - 2);
    this.timeLeft = Math.max(2, this.timeLimit - elapsed);
    
    if (s.flipped && Array.isArray(s.flipped)) {
      this.matched    = s.flipped.slice();
      this.pairsFound = this.matched.filter(Boolean).length / 2;
    }
  }

  /* Rendu du plateau de jeu */
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
    this._initLamp();
  }

  /* Gestion du clic sur une carte */
  _onCardClick(index) {
    if (this.locked)               return;
    if (this._paused)              return;
    if (this._mazeActive)          return;
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

          /* Déclencher le labyrinthe toutes les 4 paires */
          if (this.pairsFound % 4 === 0 && this.pairsFound < this.pairsTotal) {
            this._startLampBlink();
          }

          if (this.pairsFound === this.pairsTotal) {
            this._onVictory();
          }
        }, 480);

      } else {
        /* Mauvaise paire */
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

  _flipCard(index, show) {
    this.flipped[index] = show;
    const el = this._getCardEl(index);
    el?.classList.toggle('flipped', show);
  }

  _markMatched(index) {
    this.matched[index] = true;
    const el = this._getCardEl(index);
    el?.classList.add('matched');
  }

  /* Recupere l'element DOM d'une carte par son index */
  _getCardEl(index) {
    return document.querySelector(`.card-wrap[data-index="${index}"]`);
  }

  /* Mise a jour du HUD */
  _updateHUD() {
    const set = (id, v) => { const e = document.getElementById(id); if (e) e.textContent = v; };
    set('hud-score', this.score);
    set('hud-moves', this.moves);
    set('hud-pairs', `${this.pairsFound}/${this.pairsTotal}`);
    set('hud-timer', this._fmtTime(this.timeLeft));

    const timerWrap = document.getElementById('hud-timer-wrap');
    timerWrap?.classList.toggle('warning', this.timeLeft <= 20 && this.timeLeft > 0);
  }

  /* Formate temps en chaine "m:ss" */
  _fmtTime(secs) {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  /* Demarrage du timer */
  _startTimer() {
    this._updateHUD();
    this.timerInterval = setInterval(() => {
      if (this._paused || this._mazeActive) return;
      this.timeLeft--;
      this._updateHUD();

      if (this.timeLeft === 10) {
        this._startLampBlink(10000);
      }

      if (this.timeLeft <= 0) {
        clearInterval(this.timerInterval);
        this._onTimeOut();
      }
    }, 1000);
  }

  /* Arrete le timer */
  stopTimer() {
    clearInterval(this.timerInterval);
    this.timerInterval = null;
  }

  /* API appelée par le labyrinthe */
  _initLamp() {
    const lamp = document.getElementById('mazeLamp');
    if (!lamp) return;

    lamp.style.display = 'flex';

    lamp.onclick = () => {
      window.mazeNotifyPair(this);
    };
  }
  _startLampBlink(duration = 10000) {
    const lamp = document.getElementById('mazeLamp');
    if (!lamp) return;

    lamp.style.animation = 'blink 1s infinite';

    clearTimeout(this._lampBlinkTimeout);

    this._lampBlinkTimeout = setTimeout(() => {
      lamp.style.animation = 'none';
    }, duration);
  }

  _hideLamp() {
    const lamp = document.getElementById('mazeLamp');
    if (!lamp) return;

    lamp.style.display = 'none';
    lamp.style.animation = 'none';
    lamp.onclick = null;
    
    if (this._lampBlinkTimeout) {
      clearTimeout(this._lampBlinkTimeout);
      this._lampBlinkTimeout = null;
    }
  }

  pauseTimer() {
    this._mazeActive = true;
    this.locked      = true;
  }
 
  resumeTimer() {
    this._mazeActive = false;
    this.locked      = false;
  }
 
  addTime(seconds) {
    this.timeLeft += seconds;
    this._updateHUD();
  }

  _endGame() {
    this.stopTimer();
    this._hideLamp();
    this.locked = true;
  }

  /* Gestion de la victoire */
  _onVictory() {
    this._endGame();
    const timeElapsed = this.timeLimit - this.timeLeft;

    const pairs      = this.pairsTotal;
    const base       = this.level * 1000;
    const extraMoves = Math.max(0, this.moves - pairs);
    const movePen    = extraMoves * 10;
    const timePen    = timeElapsed * 2;
    const timeBonus  = (this.timeLimit - timeElapsed) * 3;
    this.score = Math.max(0, base - movePen - timePen + timeBonus);
    this._updateHUD();

    /* Enregistrement du score */
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

  /* Gestion de la defaite */
  _onTimeOut() {
    this._endGame();
    document.querySelectorAll('.card-wrap:not(.matched)').forEach(el => {
      el.style.opacity = '0.35';
    });
    document.getElementById('modal-lose').classList.add('show');
  }

  /* Sauvegarde AJAX de la partie en cours */
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

  /* Basculement pause - reprise */
  togglePause() {
    this._paused = !this._paused;
    const btn = document.getElementById('btn-pause');

    if (this._paused) {
      this.locked = true;
      if (btn) btn.textContent = '▶️';
      document.querySelectorAll('.card-wrap.flipped:not(.matched)').forEach(el => {
        el.style.opacity = '0';
      });
    } else {
      this.locked = false;
      if (btn) btn.textContent = '⏸️';
      document.querySelectorAll('.card-wrap').forEach(el => {
        el.style.opacity = '1';
      });
    }
  }

  restart() {
      const url = new URL(window.location.href);
      window.location.href = url.pathname + url.search;
  }

  _bindButtons() {
    document.getElementById('btn-save')
      ?.addEventListener('click', () => this.save());
    document.getElementById('btn-pause')
      ?.addEventListener('click', () => this.togglePause());
    document.getElementById('btn-restart')
      ?.addEventListener('click', () => this.restart());
  }
}

/* Affichage des Notifications temporaires */
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

/* Supprimer la partie sauvegardée */
function deleteSave(url) {
  if (!confirm('Supprimer la partie sauvegardée ?')) return;

  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include'
  })
  .then(response => {
    if (!response.ok) throw new Error('HTTP ' + response.status);
    return response.json();
  })
  .then(data => {
    if (data.success) {
      const banner = document.getElementById('save-banner');
      if (banner) {
        banner.style.transition = 'opacity 0.4s, transform 0.4s';
        banner.style.opacity    = '0';
        banner.style.transform  = 'translateY(-10px)';
        setTimeout(() => banner.remove(), 420);
      }
    } else {
      alert('Erreur lors de la suppression.');
    }
  })
  .catch(err => {
    console.error('deleteSave error:', err);
    alert('Erreur : ' + err.message);
  });
}


/* Dropdown utilisateur */
function toggleUserDropdown(e) {
  if (e) e.stopPropagation();
  const menu = document.getElementById('userDropdownMenu');
  const btn  = document.getElementById('userDropdownBtn');
  const open = menu.classList.toggle('open');
  btn.classList.toggle('open', open);
}
document.addEventListener('click', () => {
  document.getElementById('userDropdownMenu')?.classList.remove('open');
  document.getElementById('userDropdownBtn')?.classList.remove('open');
});

/* Ouvrir modales */
function openChangePseudoModal() {
  document.getElementById('newPseudoInput').value = '';
  document.getElementById('pseudoError').style.display = 'none';
  document.getElementById('changePseudoModal').style.display = 'flex';
}
function openChangePasswordModal() {
  ['oldPasswordInput','newPasswordInput','confirmPasswordInput']
    .forEach(id => document.getElementById(id).value = '');
  document.getElementById('passwordError').style.display = 'none';
  document.getElementById('changePasswordModal').style.display = 'flex';
}

/* Changer le pseudo */
function submitChangePseudo(btn) {
  const url = btn.getAttribute('data-url');
  if (!url) {
    alert('URL non définie');
    return;
  }

  const newPseudo = document.getElementById('newPseudoInput').value.trim();
  const errEl = document.getElementById('pseudoError');
  errEl.style.display = 'none';

  if (newPseudo.length < 3 || newPseudo.length > 20) {
    errEl.textContent = 'Le pseudo doit contenir entre 3 et 20 caractères.';
    errEl.style.display = 'block';
    return;
  }

  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ username: newPseudo })
  })
  .then(r => r.json())
  .then(data => {
    if (data.success) {
      document.getElementById('changePseudoModal').style.display = 'none';
      showToast('Pseudo mis à jour !', 'success');
      setTimeout(() => location.reload(), 1200);
    } else {
      errEl.textContent = data.message || 'Erreur lors du changement.';
      errEl.style.display = 'block';
    }
  })
  .catch(() => {
    errEl.textContent = 'Erreur réseau.';
    errEl.style.display = 'block';
  });
}

function submitChangePassword(btn) {
  const oldPw  = document.getElementById('oldPasswordInput').value;
  const newPw  = document.getElementById('newPasswordInput').value;
  const conf   = document.getElementById('confirmPasswordInput').value;
  const errEl  = document.getElementById('passwordError');

  errEl.style.display = 'none';
  btn.disabled = true;
  btn.textContent = 'Traitement...';
  
  if (!oldPw) {
    return showError('Veuillez entrer votre mot de passe actuel.');
  }
  if (newPw.length < 6) {
    return showError('Le nouveau mot de passe doit contenir au moins 6 caractères.');
  }
  if (newPw !== conf) {
    return showError('Les mots de passe ne correspondent pas.');
  }
  if (oldPw === newPw) {
    return showError('Le nouveau mot de passe doit être différent de l\'ancien.');
  }

  fetch(`/auth/changepassword`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ oldPassword: oldPw, newPassword: newPw })
  })
  .then(r => r.json())
  .then(data => {
    if (data.success) {
      document.getElementById('changePasswordModal').style.display = 'none';
      showToast('Mot de passe mis à jour !', 'success');
    } else {
      showError(data.message || 'Erreur lors du changement.');
    }
  })
  .catch(() => {
    showError('Erreur réseau.');
  });

  function showError(msg) {
    errEl.textContent = msg;
    errEl.style.display = 'block';

    btn.disabled = false;
    btn.textContent = 'Confirmer ✦';
  }
}

function togglePassword(inputId, icon) {
  const input = document.getElementById(inputId);
  const openIcon = icon.querySelector('.eye-open');
  const closedIcon = icon.querySelector('.eye-closed');

  if (input.type === "password") {
    input.type = "text";
    openIcon.style.display = "none";
    closedIcon.style.display = "block";
  } else {
    input.type = "password";
    openIcon.style.display = "block";
    closedIcon.style.display = "none";
  }
}