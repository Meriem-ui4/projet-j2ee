

-- 1. Créer la base de données
CREATE DATABASE IF NOT EXISTS memorygame
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 2. Sélectionner la base
USE memorygame;

-- ───────────────────────────────────────────────────────────
-- NOTE : Les tables ci-dessous sont créées AUTOMATIQUEMENT
-- par Hibernate (hbm2ddl.auto=update) au premier démarrage.
-- Ce script sert uniquement de référence / vérification.
-- ───────────────────────────────────────────────────────────

-- Table des joueurs
CREATE TABLE IF NOT EXISTS players (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(20)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL COMMENT 'Hash BCrypt — jamais en clair',
    best_score  INT NOT NULL DEFAULT 0,
    total_games INT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table des parties sauvegardées
CREATE TABLE IF NOT EXISTS saved_games (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id     BIGINT NOT NULL,
    level         INT NOT NULL,
    theme         VARCHAR(20) NOT NULL DEFAULT 'theme1',
    score         INT NOT NULL DEFAULT 0,
    moves_count   INT NOT NULL DEFAULT 0,
    time_elapsed  INT NOT NULL DEFAULT 0,
    board_state   TEXT,
    flipped_state TEXT,
    saved_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_completed  TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table du classement (un enregistrement par partie terminée)
CREATE TABLE IF NOT EXISTS score_entries (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id    BIGINT NOT NULL,
    level        INT NOT NULL,
    theme        VARCHAR(20) NOT NULL DEFAULT 'theme1',
    score        INT NOT NULL DEFAULT 0,
    moves_count  INT NOT NULL DEFAULT 0,
    time_elapsed INT NOT NULL DEFAULT 0,
    played_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ───────────────────────────────────────────────────────────
-- Vérification : afficher les tables créées
-- ───────────────────────────────────────────────────────────
SHOW TABLES;
