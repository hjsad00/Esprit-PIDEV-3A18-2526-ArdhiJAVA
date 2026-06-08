-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : dim. 03 mai 2026 à 00:52
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `ardhi`
--

-- --------------------------------------------------------

--
-- Structure de la table `abonnement`
--

CREATE TABLE `abonnement` (
  `id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `prix` float NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `statut` varchar(20) DEFAULT 'ACTIF',
  `user_id` int(11) NOT NULL,
  `offre_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `abonnement`
--

INSERT INTO `abonnement` (`id`, `type`, `prix`, `date_debut`, `date_fin`, `statut`, `user_id`, `offre_id`) VALUES
(1, 'VIP', 99.98, '2026-03-01', '2026-04-01', 'ACTIF', 2, 6),
(2, 'VIP', 99.98, '2026-03-01', '2026-04-01', 'ANNULE', 13, 6),
(3, 'VIP', 99.98, '2026-03-02', '2026-04-02', 'ANNULE', 13, 6),
(4, 'VIP', 49.99, '2026-04-19', '2026-05-19', 'ACTIF', 13, 6);

-- --------------------------------------------------------

--
-- Structure de la table `ai_suggestions`
--

CREATE TABLE `ai_suggestions` (
  `id` int(11) NOT NULL,
  `parcelle_id` int(11) NOT NULL,
  `culture_principale` varchar(100) DEFAULT NULL,
  `alternatives` longtext DEFAULT NULL,
  `justification` text DEFAULT NULL,
  `meteo` longtext DEFAULT NULL,
  `saison` varchar(50) DEFAULT NULL,
  `accepted` tinyint(1) DEFAULT 0,
  `created_at` datetime DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `ai_suggestions`
--

INSERT INTO `ai_suggestions` (`id`, `parcelle_id`, `culture_principale`, `alternatives`, `justification`, `meteo`, `saison`, `accepted`, `created_at`, `updated_at`) VALUES
(1, 16, 'Tomate', '[\"Melon\",\"Poivron\",\"Past\\u00e8que\"]', 'El Mehiri (Dakhla) est une zone d\'excellence pour la tomate. Le climat printanier à 24°C est l\'optimum thermique pour la croissance, et le sol sableux permet un contrôle total de la nutrition via la fertigation.', '{\"temperature\":24,\"humidity\":50,\"description\":\"ciel d\\u00e9gag\\u00e9\"}', 'printemps', 0, '2026-04-19 16:01:10', '2026-04-19 15:01:10'),
(2, 16, 'CULTURE OK: Tomate', '[\"Melon\",\"Past\\u00e8que\",\"Poivron\"]', 'La température de 24°C est l\'optimum thermique pour la tomate. Le sol sableux d\'El Mehiri permet un excellent drainage et un réchauffement rapide des racines en printemps.', '{\"temperature\":24,\"humidity\":50,\"description\":\"ciel d\\u00e9gag\\u00e9\"}', 'printemps', 0, '2026-04-19 16:01:35', '2026-04-19 15:01:35'),
(3, 18, 'Melon', '[\"Past\\u00e8que\",\"Tomate\",\"Poivron\"]', 'Le climat chaud de Targuellache et le sol argilo-limoneux favorisent une excellente concentration en sucre. La température actuelle est optimale pour la croissance rapide au printemps.', '{\"temperature\":27.4,\"humidity\":42,\"description\":\"partiellement nuageux\"}', 'printemps', 0, '2026-04-21 16:36:20', '2026-04-21 15:36:20');

-- --------------------------------------------------------

--
-- Structure de la table `alerte_technicien`
--

CREATE TABLE `alerte_technicien` (
  `id` int(11) NOT NULL,
  `id_materiel` int(11) NOT NULL,
  `agriculteur_id` int(11) NOT NULL,
  `description` longtext NOT NULL,
  `date_signalement` datetime NOT NULL,
  `statut` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `alerte_technicien`
--

INSERT INTO `alerte_technicien` (`id`, `id_materiel`, `agriculteur_id`, `description`, `date_signalement`, `statut`) VALUES
(1, 31, 25, 'Hjkiyt', '2026-04-18 03:00:06', 'lu'),
(2, 32, 25, 'Fuite de huile', '2026-04-18 03:17:51', 'lu'),
(3, 32, 25, 'Usuejdiie', '2026-04-18 03:23:58', 'lu'),
(4, 32, 25, 'Usuejdiie', '2026-04-18 03:24:03', 'lu'),
(5, 32, 25, 'Usuejdiie', '2026-04-18 03:24:05', 'lu'),
(6, 33, 25, 'Shsbabsnjd', '2026-04-18 10:25:41', 'lu'),
(7, 33, 25, '7+7', '2026-04-18 10:26:15', 'lu'),
(8, 33, 25, '7+7', '2026-04-18 10:26:16', 'lu'),
(9, 13, 13, 'Bruit moteur', '2026-04-20 15:11:22', 'lu'),
(10, 18, 13, 'Fuite', '2026-04-21 14:23:15', 'lu'),
(11, 20, 13, 'Hhhxxbxndndn', '2026-04-21 17:10:53', 'lu'),
(12, 20, 13, 'Fuite de huile', '2026-04-21 17:11:05', 'lu'),
(13, 20, 13, 'Fuite de huile', '2026-04-21 17:11:07', 'lu');

-- --------------------------------------------------------

--
-- Structure de la table `avis`
--

CREATE TABLE `avis` (
  `idAvis` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `id_produit` int(11) NOT NULL,
  `note` int(11) NOT NULL CHECK (`note` >= 1 and `note` <= 5),
  `commentaire` text DEFAULT NULL,
  `dateAvis` timestamp NOT NULL DEFAULT current_timestamp(),
  `isVerifiedBuyer` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `avis`
--

INSERT INTO `avis` (`idAvis`, `id_user`, `id_produit`, `note`, `commentaire`, `dateAvis`, `isVerifiedBuyer`) VALUES
(1, 2, 9, 1, 'produit null', '2026-02-10 15:34:07', 0),
(2, 2, 9, 5, 'tayaraaaaaa', '2026-02-10 15:34:21', 0),
(3, 2, 9, 5, 'haja wow', '2026-02-10 15:34:30', 0),
(4, 2, 9, 5, 'bravo pour ce produit', '2026-02-10 15:34:44', 0),
(5, 2, 9, 1, 'banane masar raw', '2026-02-10 15:35:25', 0),
(6, 2, 9, 1, 'ghali yesser', '2026-02-10 15:35:32', 0),
(7, 2, 9, 4, 'jnjknknjknknk', '2026-02-10 15:38:57', 0),
(8, 2, 9, 5, 'phoghdogd', '2026-02-10 15:45:13', 0),
(9, 2, 9, 5, 'jknjknkjn', '2026-02-10 15:45:26', 0),
(10, 2, 9, 4, 'onkjn', '2026-02-10 15:45:43', 0),
(15, 3, 9, 5, '', '2026-02-18 17:50:26', 0),
(16, 3, 9, 5, 'nhebek', '2026-02-18 17:50:34', 0),
(22, 2, 3, 5, 'test', '2026-02-24 21:10:29', 0),
(25, 2, 65, 5, '', '2026-02-27 22:47:06', 0),
(29, 4, 75, 5, '', '2026-03-02 08:13:31', 0),
(30, 4, 70, 1, '', '2026-03-02 08:13:36', 0),
(31, 13, 65, 5, 'bravo', '2026-04-13 21:24:38', 0),
(32, 13, 65, 5, NULL, '2026-04-13 21:24:54', 0),
(33, 13, 82, 5, 'super', '2026-04-13 21:26:39', 1),
(34, 13, 94, 1, 'null', '2026-04-13 21:27:12', 0),
(35, 13, 100, 4, NULL, '2026-04-13 22:13:48', 0),
(36, 2, 98, 1, 'null', '2026-04-13 22:22:09', 0),
(37, 2, 98, 5, '<3', '2026-04-13 22:24:16', 0),
(38, 2, 98, 5, NULL, '2026-04-13 22:26:46', 0),
(39, 2, 98, 1, 'test', '2026-04-13 22:27:33', 0),
(40, 2, 98, 5, NULL, '2026-04-13 22:28:30', 0),
(41, 2, 100, 5, NULL, '2026-04-13 22:30:15', 0),
(42, 13, 95, 1, 'faux', '2026-04-13 22:35:28', 0),
(43, 2, 101, 1, 'khayeb', '2026-04-15 23:08:32', 0),
(44, 13, 99, 1, NULL, '2026-04-17 20:10:00', 0),
(45, 23, 106, 5, NULL, '2026-04-21 16:31:22', 0);

-- --------------------------------------------------------

--
-- Structure de la table `badge`
--

CREATE TABLE `badge` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `condition_type` enum('DIAGNOSTIC','POINTS','HEALTHY_PLANTS','SOLUTION') DEFAULT 'DIAGNOSTIC',
  `threshold` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `badge`
--

INSERT INTO `badge` (`id`, `name`, `description`, `icon`, `condition_type`, `threshold`) VALUES
(1, 'Bienvenue', 'Premier diagnostic réalisé', '🌱', 'DIAGNOSTIC', 1),
(2, 'Explorateur', '10 diagnostics réalisés', '🔍', 'DIAGNOSTIC', 10),
(3, 'Expert', '50 diagnostics réalisés', '🎓', 'DIAGNOSTIC', 50),
(4, 'Fidèle', '100 points accumulés', '⭐', 'POINTS', 100),
(5, 'Légendaire', '1000 points accumulés', '👑', 'POINTS', 1000),
(6, 'Main Verte', '5 plantes saines diagnostiquées', '🌿', 'HEALTHY_PLANTS', 5),
(7, 'Premier Secours', 'Première solution acceptée', '🚑', 'SOLUTION', 1),
(8, 'Guru', '5 solutions acceptées', '🧘', 'SOLUTION', 5),
(9, 'Sage du Village', '10 solutions acceptées', '🦉', 'SOLUTION', 10);

-- --------------------------------------------------------

--
-- Structure de la table `chatbot_conversation`
--

CREATE TABLE `chatbot_conversation` (
  `id_conversation` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `message_utilisateur` text NOT NULL,
  `reponse_chatbot` text NOT NULL,
  `intention` varchar(100) DEFAULT NULL,
  `contexte` longtext DEFAULT NULL,
  `satisfaction` int(11) DEFAULT NULL,
  `date_message` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `commande`
--

CREATE TABLE `commande` (
  `idCommande` int(11) NOT NULL,
  `dateCommande` date NOT NULL,
  `etat` enum('en_attente','en_cours','livree','annulee') NOT NULL,
  `id_user` int(11) NOT NULL,
  `total` decimal(12,2) NOT NULL DEFAULT 0.00,
  `id_coupon` int(11) DEFAULT NULL,
  `frais_livraison` float DEFAULT 0,
  `mode_livraison` enum('RECUPERATION','LIVRAISON') DEFAULT 'RECUPERATION',
  `payee_par_points` tinyint(1) DEFAULT 0,
  `montantRemise` double NOT NULL DEFAULT 0,
  `qr_code_token` varchar(255) DEFAULT NULL,
  `qr_code_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `commande`
--

INSERT INTO `commande` (`idCommande`, `dateCommande`, `etat`, `id_user`, `total`, `id_coupon`, `frais_livraison`, `mode_livraison`, `payee_par_points`, `montantRemise`, `qr_code_token`, `qr_code_path`) VALUES
(72, '2026-02-06', 'annulee', 4, 1.22, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(73, '2026-02-06', 'annulee', 4, 1.22, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(74, '2026-02-06', 'en_cours', 4, 1.22, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(75, '2026-02-06', 'en_cours', 2, 1.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(76, '2026-02-06', 'annulee', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(83, '2026-02-07', 'annulee', 2, 16.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(85, '2026-02-07', 'en_cours', 2, 15.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(86, '2026-02-07', 'en_attente', 2, 1.80, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(87, '2026-02-07', 'en_cours', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(88, '2026-02-07', 'annulee', 2, 223.08, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(89, '2026-02-08', 'en_attente', 2, 4.30, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(90, '2026-02-08', 'en_attente', 2, 6.50, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(91, '2026-02-10', 'annulee', 4, 2.44, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(92, '2026-02-13', 'en_cours', 2, 13.52, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(93, '2026-02-13', 'en_cours', 2, 20.28, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(94, '2026-02-13', 'en_cours', 2, 60.84, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(95, '2026-02-13', 'en_cours', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(96, '2026-02-13', 'annulee', 4, 1.35, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(97, '2026-02-13', 'en_cours', 4, 1.35, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(98, '2026-02-13', 'annulee', 2, 15.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(99, '2026-02-13', 'annulee', 2, 15.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(100, '2026-02-13', 'en_cours', 6, 98.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(101, '2026-02-13', 'en_cours', 2, 5.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(102, '2026-02-13', 'en_attente', 2, 3.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(103, '2026-02-13', 'en_attente', 2, 1.30, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(104, '2026-02-13', 'en_cours', 2, 19.28, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(105, '2026-02-13', 'en_cours', 2, 14.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(106, '2026-02-14', 'en_cours', 2, 22.66, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(107, '2026-02-14', 'en_attente', 2, 6.50, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(108, '2026-02-15', 'en_cours', 2, 14.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(110, '2026-02-15', 'en_attente', 2, 1.19, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(111, '2026-02-16', 'en_attente', 2, 6.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(112, '2026-02-16', 'en_cours', 2, 14.30, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(113, '2026-02-16', 'en_attente', 2, 6.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(114, '2026-02-16', 'en_cours', 2, 14.30, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(115, '2026-02-23', 'en_attente', 3, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(118, '2026-02-24', 'en_attente', 2, 9.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(119, '2026-02-24', 'en_attente', 2, 22.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(120, '2026-02-24', 'annulee', 2, 50.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(121, '2026-02-24', 'en_attente', 4, 19.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(122, '2026-02-25', 'en_attente', 2, 1.77, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(123, '2026-02-25', 'en_cours', 2, 50.55, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(124, '2026-02-25', 'en_attente', 2, 1.80, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(125, '2026-02-25', 'en_attente', 2, 1.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(126, '2026-02-25', 'annulee', 2, 2.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(127, '2026-02-25', 'en_attente', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(128, '2026-02-25', 'en_attente', 2, 15.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(129, '2026-02-27', 'annulee', 2, 13.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(130, '2026-02-27', 'en_attente', 2, 341.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(131, '2026-02-27', 'en_attente', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(132, '2026-02-27', 'en_attente', 2, 13.50, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(133, '2026-02-27', 'en_cours', 2, 22.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(134, '2026-02-27', 'en_attente', 2, 3.38, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(135, '2026-02-27', 'en_cours', 2, 3387.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(136, '2026-02-27', 'en_attente', 4, 19.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(137, '2026-02-27', 'en_attente', 4, 17.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(138, '2026-02-27', 'en_attente', 4, 19.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(139, '2026-02-27', 'en_cours', 2, 22.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(140, '2026-02-27', 'annulee', 2, 22.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(141, '2026-02-27', 'en_cours', 2, 10.38, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(142, '2026-02-27', 'annulee', 2, 10.38, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(143, '2026-02-27', 'annulee', 2, 13.76, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(144, '2026-02-27', 'annulee', 2, 10.38, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(145, '2026-02-27', 'annulee', 4, 10.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(146, '2026-02-27', 'annulee', 2, 37.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(147, '2026-02-27', 'annulee', 4, 10.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(148, '2026-02-27', 'annulee', 2, 30.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(149, '2026-02-27', 'annulee', 2, 30.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(150, '2026-03-01', 'en_attente', 2, 13.50, NULL, 7, 'LIVRAISON', 1, 0, NULL, NULL),
(151, '2026-03-02', 'en_attente', 4, 15.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(152, '2026-03-02', 'livree', 13, 8.90, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(153, '2026-03-02', 'en_attente', 13, 22.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(154, '2026-03-02', 'livree', 13, 9.50, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(155, '2026-03-02', 'en_attente', 13, 22.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(156, '2026-03-02', 'annulee', 13, 2.50, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(157, '2026-04-05', 'livree', 2, 88.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(158, '2026-04-05', 'en_attente', 2, 107.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(159, '2026-04-05', 'en_attente', 2, 207.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(160, '2026-04-05', 'annulee', 2, 95.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(161, '2026-04-05', 'annulee', 2, 88.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(162, '2026-04-05', 'livree', 13, 32.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(163, '2026-04-05', 'en_attente', 13, 57.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(164, '2026-04-05', 'annulee', 2, 22200.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(165, '2026-04-05', 'annulee', 2, 100000.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(166, '2026-04-05', 'en_attente', 2, 87.00, 6, 7, 'LIVRAISON', 0, 20, NULL, NULL),
(167, '2026-04-05', 'en_attente', 2, 47.00, 6, 7, 'LIVRAISON', 0, 10, NULL, NULL),
(168, '2026-04-05', 'en_attente', 2, 86.67, 13, 0, 'RECUPERATION', 0, 13.333, NULL, NULL),
(169, '2026-04-05', 'en_attente', 2, 43.33, 13, 0, 'RECUPERATION', 0, 6.667, NULL, NULL),
(170, '2026-04-06', 'en_attente', 2, 1.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(171, '2026-04-06', 'en_attente', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(172, '2026-04-06', 'en_attente', 2, 15.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(173, '2026-04-06', 'annulee', 2, 15.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(174, '2026-04-06', 'annulee', 2, 15.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(175, '2026-04-06', 'en_attente', 2, 122.00, NULL, 7, 'LIVRAISON', 1, 0, NULL, NULL),
(176, '2026-04-06', 'en_attente', 2, 100.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(177, '2026-04-06', 'en_attente', 2, 100.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(178, '2026-04-06', 'en_attente', 2, 6.50, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(179, '2026-04-06', 'en_attente', 2, 200.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(180, '2026-04-06', 'annulee', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(181, '2026-04-06', 'annulee', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(182, '2026-04-07', 'en_attente', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(183, '2026-04-07', 'en_attente', 2, 15.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(184, '2026-04-07', 'annulee', 23, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(185, '2026-04-07', 'annulee', 23, 2.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(186, '2026-04-07', 'livree', 23, 2.00, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(187, '2026-04-09', 'annulee', 13, 264.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(188, '2026-04-11', 'en_cours', 13, 76.30, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(189, '2026-04-11', 'annulee', 13, 85.90, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(190, '2026-04-11', 'en_cours', 13, 113.60, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(191, '2026-04-11', 'en_attente', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(192, '2026-04-11', 'en_cours', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(193, '2026-04-11', 'en_attente', 2, 79.20, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(194, '2026-04-11', 'en_attente', 2, 86.20, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(195, '2026-04-11', 'en_attente', 2, 100.25, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(196, '2026-04-11', 'en_attente', 2, 335.15, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(197, '2026-04-11', 'en_attente', 2, 18.35, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(198, '2026-04-11', 'en_attente', 2, 46.98, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(199, '2026-04-12', 'en_attente', 13, 55.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(200, '2026-04-12', 'en_attente', 13, 62.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(201, '2026-04-12', 'en_cours', 13, 106.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(202, '2026-04-12', 'annulee', 13, 99.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(203, '2026-04-13', 'en_cours', 13, 99.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(204, '2026-04-13', 'annulee', 13, 106.00, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(205, '2026-04-13', 'en_attente', 2, 10.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(206, '2026-04-13', 'annulee', 2, 10.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(207, '2026-04-13', 'en_attente', 2, 55.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(208, '2026-04-16', 'en_attente', 2, 6.50, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(209, '2026-04-16', 'en_cours', 2, 55.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(210, '2026-04-16', 'en_attente', 2, 55.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(211, '2026-04-17', 'en_attente', 13, 19.99, NULL, 0, 'RECUPERATION', 1, 0, NULL, NULL),
(212, '2026-04-17', 'en_attente', 13, 26.99, NULL, 7, 'LIVRAISON', 1, 0, NULL, NULL),
(213, '2026-04-17', 'en_attente', 13, 81.99, NULL, 7, 'LIVRAISON', 0, 0, NULL, NULL),
(214, '2026-04-18', 'en_cours', 2, 151555.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(215, '2026-04-18', 'en_cours', 23, 1500.00, NULL, 0, 'RECUPERATION', 0, 0, NULL, NULL),
(216, '2026-04-18', 'en_attente', 13, 79.20, NULL, 0, 'RECUPERATION', 0, 0, '1ba38571dc8ddbd084c04a3b9c832140', 'uploads/qrcodes/marketplace/order-1ba38571dc8ddbd084c04a3b9c832140.svg'),
(217, '2026-04-18', 'en_cours', 13, 74.99, NULL, 0, 'RECUPERATION', 0, 0, 'e09073d7214ca2ce67569415f1d942eb', NULL),
(218, '2026-04-18', 'en_attente', 3, 128.25, NULL, 0, 'RECUPERATION', 0, 0, '4ff81652abd9ab37ee3b42965ac9a093', NULL),
(219, '2026-04-18', 'en_attente', 3, 1980.00, NULL, 0, 'RECUPERATION', 0, 0, 'cd3e2a8136aaf509ac9684e24d0b9dd3', NULL),
(220, '2026-04-18', 'annulee', 13, 9795.10, NULL, 0, 'RECUPERATION', 0, 0, '0947891a513fb1c406578258b328be73', NULL),
(221, '2026-04-20', 'en_attente', 13, 86.20, NULL, 7, 'LIVRAISON', 0, 0, 'ad5cc86c93b3e1da553dcf00313ab418', NULL),
(222, '2026-04-20', 'annulee', 13, 63.00, NULL, 7, 'LIVRAISON', 0, 0, '193dc2500542e6a43d2712731bb6ad90', NULL),
(223, '2026-04-21', 'en_attente', 13, 55.00, NULL, 0, 'RECUPERATION', 0, 0, 'aa591df54143ab6617e5ff2c425832f1', NULL),
(224, '2026-04-21', 'en_attente', 23, 75.00, NULL, 0, 'RECUPERATION', 0, 0, 'd031c5cb94a83100112f85a5c66e3d6f', NULL),
(225, '2026-04-21', 'en_attente', 23, 10.80, NULL, 0, 'RECUPERATION', 0, 0, 'b83f9fc759a1ae6517bfaa0cef37e2fa', NULL),
(226, '2026-04-21', 'en_attente', 23, 150000.00, NULL, 0, 'RECUPERATION', 0, 0, '2e136ae7da625d1b1b61919d40770d0f', NULL),
(227, '2026-05-02', 'en_attente', 2, 2.50, NULL, 0, 'RECUPERATION', 1, 0, '7e5a242263e206dd3830e5a161e659de', NULL),
(228, '2026-05-02', 'en_attente', 2, 1.00, NULL, 0, 'RECUPERATION', 1, 0, '290770ddb0faa9fff6cdd2859f17517e', NULL),
(229, '2026-05-02', 'en_attente', 2, 55.00, NULL, 0, 'RECUPERATION', 1, 0, '8e0d322a909005c9d4751dd2a0fbadfa', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `community_analytics_daily`
--

CREATE TABLE `community_analytics_daily` (
  `id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `date` date NOT NULL,
  `views` int(11) NOT NULL DEFAULT 0,
  `read_time` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `community_analytics_daily`
--

INSERT INTO `community_analytics_daily` (`id`, `post_id`, `date`, `views`, `read_time`) VALUES
(1, 3, '2026-04-19', 8, 45),
(2, 4, '2026-04-19', 2, 29),
(3, 1, '2026-04-19', 6, 117),
(4, 1, '2026-04-21', 4, 171);

-- --------------------------------------------------------

--
-- Structure de la table `community_comments`
--

CREATE TABLE `community_comments` (
  `id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `likes` int(11) DEFAULT 0,
  `dislikes` int(11) DEFAULT 0,
  `is_solution` tinyint(1) DEFAULT 0,
  `parent_comment_id` int(11) DEFAULT NULL,
  `total_read_time` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `community_comments`
--

INSERT INTO `community_comments` (`id`, `post_id`, `user_id`, `content`, `created_at`, `likes`, `dislikes`, `is_solution`, `parent_comment_id`, `total_read_time`) VALUES
(4, 2, 13, 'hello', '2026-03-01 22:22:14', 0, 0, 1, NULL, 0),
(5, 2, 13, 'hi', '2026-03-01 22:22:22', 0, 0, 0, 4, 0),
(6, 2, 13, 'salut', '2026-03-01 22:22:29', 0, 0, 0, NULL, 0),
(7, 2, 13, 'hi', '2026-03-01 22:22:35', 0, 0, 0, 5, 0),
(8, 2, 2, 'dskl,flsdf', '2026-04-06 23:56:03', 0, 0, 0, NULL, 0),
(9, 3, 13, 'BIEN', '2026-04-19 12:50:53', 0, 0, 0, NULL, 23),
(10, 3, 13, 'HAHAHAH', '2026-04-19 12:50:58', 0, 0, 0, 9, 23),
(11, 3, 13, ':heart:  :heart:  :heart:  :heart:  :heart:  :heart:  :heart:  :heart:  :heart:', '2026-04-19 12:51:04', 0, 0, 0, NULL, 22),
(14, 1, 13, '@USER', '2026-04-19 12:57:31', 0, 0, 0, NULL, 185),
(15, 1, 2, ':heart:', '2026-04-21 14:24:24', 0, 0, 0, NULL, 132);

-- --------------------------------------------------------

--
-- Structure de la table `community_likes`
--

CREATE TABLE `community_likes` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `post_id` int(11) DEFAULT NULL,
  `comment_id` int(11) DEFAULT NULL,
  `vote_type` enum('LIKE','DISLIKE') NOT NULL DEFAULT 'LIKE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `community_likes`
--

INSERT INTO `community_likes` (`id`, `user_id`, `post_id`, `comment_id`, `vote_type`, `created_at`) VALUES
(2, 13, 2, NULL, 'DISLIKE', '2026-03-01 22:21:17'),
(3, 13, 3, NULL, 'LIKE', '2026-04-19 12:50:24'),
(5, 13, 1, NULL, 'LIKE', '2026-04-19 12:56:48');

-- --------------------------------------------------------

--
-- Structure de la table `community_posts`
--

CREATE TABLE `community_posts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `likes` int(11) DEFAULT 0,
  `dislikes` int(11) DEFAULT 0,
  `is_resolved` tinyint(1) DEFAULT 0,
  `solution_comment_id` int(11) DEFAULT NULL,
  `views` int(11) DEFAULT 0,
  `feed_impressions` int(11) DEFAULT 0,
  `total_read_time` int(11) DEFAULT 0,
  `total_feed_dwell_time` int(11) DEFAULT 0,
  `completed_reads` int(11) DEFAULT 0,
  `media_clicks` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `community_posts`
--

INSERT INTO `community_posts` (`id`, `user_id`, `title`, `description`, `image_url`, `created_at`, `likes`, `dislikes`, `is_resolved`, `solution_comment_id`, `views`, `feed_impressions`, `total_read_time`, `total_feed_dwell_time`, `completed_reads`, `media_clicks`) VALUES
(1, 2, 'bdfomjkbdfmoj', 'ôjfdmknglkn', NULL, '2026-02-16 09:25:31', 2, 0, 0, NULL, 10, 5, 288, 5, 10, 0),
(2, 13, 'Aide pour BANANIER', 'L\'IA a détecté : MALADIE NON_VISIBLE (Confiance: 100.0%).\nJe ne suis pas sûr de ce résultat. Qu\'en pensez-vous ?', 'https://i.ibb.co/qM2gb4SS/0f949095291c.jpg', '2026-03-01 22:15:45', 0, 1, 1, 4, 0, 8, 0, 14, 0, 0),
(3, 13, 'Aide pour TOMAT', 'L\'IA a détecté : Bactériose (Confiance: 87%).\r\nJe ne suis pas sûr de ce résultat. Qu\'en pensez-vous ?', 'https://i.ibb.co/1JwxQw5C/c8d3d290db7e.png', '2026-04-19 12:50:18', 1, 0, 0, NULL, 8, 19, 45, 80, 8, 0),
(4, 13, 'AY HAJA', 'AY HAJAAY HAJAAY HAJAAY HAJA\r\nhttps://www.youtube.com/watch?v=GWhtsuVnctE', NULL, '2026-04-19 12:52:17', 0, 0, 0, NULL, 2, 16, 29, 29, 2, 0);

-- --------------------------------------------------------

--
-- Structure de la table `community_reports`
--

CREATE TABLE `community_reports` (
  `id` int(11) NOT NULL,
  `reporter_id` int(11) NOT NULL,
  `post_id` int(11) DEFAULT NULL,
  `comment_id` int(11) DEFAULT NULL,
  `reason` varchar(255) NOT NULL,
  `is_resolved` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `community_reports`
--

INSERT INTO `community_reports` (`id`, `reporter_id`, `post_id`, `comment_id`, `reason`, `is_resolved`, `created_at`) VALUES
(1, 13, 1, NULL, '+18', 0, '2026-04-19 13:56:38');

-- --------------------------------------------------------

--
-- Structure de la table `coupon`
--

CREATE TABLE `coupon` (
  `idCoupon` int(11) NOT NULL,
  `code` varchar(50) NOT NULL,
  `typeReduction` enum('POURCENTAGE','MONTANT_FIXE') NOT NULL,
  `valeur` decimal(10,2) NOT NULL,
  `dateDebut` date DEFAULT NULL,
  `dateFin` date DEFAULT NULL,
  `utilisationMax` int(11) DEFAULT 1,
  `utilisationActuelle` int(11) DEFAULT 0,
  `actif` tinyint(1) DEFAULT 1,
  `montantMin` decimal(10,2) DEFAULT 0.00,
  `limiteParUser` int(11) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `coupon`
--

INSERT INTO `coupon` (`idCoupon`, `code`, `typeReduction`, `valeur`, `dateDebut`, `dateFin`, `utilisationMax`, `utilisationActuelle`, `actif`, `montantMin`, `limiteParUser`) VALUES
(1, 'saif', 'MONTANT_FIXE', 10.00, '2026-02-13', '2027-02-13', 10, 2, 0, 11.00, 2),
(2, 'SAIF2', 'POURCENTAGE', 90.00, '2026-02-13', '2026-02-14', 10, 1, 0, 1.00, 1),
(3, 'saif3', 'POURCENTAGE', 100.00, '2026-01-13', '2026-01-15', 100, 0, 1, 0.00, 10),
(4, 'test', 'MONTANT_FIXE', 1.00, '2026-02-13', '2028-01-13', 10000, 9, 0, 0.00, 1000),
(6, 'TEST2', 'POURCENTAGE', 20.00, '2026-02-14', '2029-07-17', 100, 10, 1, 50.00, 100),
(13, '1', 'POURCENTAGE', 20.00, '2026-02-01', '2026-08-29', 1000, 2, 1, 0.00, 100),
(19, 'KNKDL', 'POURCENTAGE', 55.00, '2005-05-05', '2006-05-05', 0, 0, 1, 0.00, 1),
(20, 'TEST1', 'POURCENTAGE', 10.00, '2000-01-01', '2026-05-05', 100, 0, 0, 1.00, 1);

-- --------------------------------------------------------

--
-- Structure de la table `coupon_utilisation`
--

CREATE TABLE `coupon_utilisation` (
  `id` int(11) NOT NULL,
  `id_coupon` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `nombreUtilisation` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `coupon_utilisation`
--

INSERT INTO `coupon_utilisation` (`id`, `id_coupon`, `id_user`, `nombreUtilisation`) VALUES
(1, 1, 2, 2),
(2, 2, 2, 1),
(3, 4, 2, 8),
(4, 6, 1, 9),
(5, 13, 2, 2),
(6, 4, 4, 1),
(7, 6, 2, 1);

-- --------------------------------------------------------

--
-- Structure de la table `credit_dossier`
--

CREATE TABLE `credit_dossier` (
  `id` int(11) NOT NULL,
  `duree_annees` int(11) NOT NULL,
  `montant_pret_max` decimal(12,2) NOT NULL,
  `capacite_remboursement` decimal(10,2) NOT NULL,
  `score_risque` decimal(5,2) NOT NULL,
  `niveau_risque` varchar(50) NOT NULL,
  `score_rentabilite` decimal(10,2) NOT NULL,
  `score_stabilite_climat` decimal(5,2) NOT NULL,
  `score_diversification` decimal(5,2) NOT NULL,
  `score_historique` decimal(5,2) NOT NULL,
  `recommandations` longtext DEFAULT NULL,
  `statut` varchar(50) NOT NULL DEFAULT 'draft',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `parcelle_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `credit_dossier`
--

INSERT INTO `credit_dossier` (`id`, `duree_annees`, `montant_pret_max`, `capacite_remboursement`, `score_risque`, `niveau_risque`, `score_rentabilite`, `score_stabilite_climat`, `score_diversification`, `score_historique`, `recommandations`, `statut`, `created_at`, `updated_at`, `parcelle_id`) VALUES
(2, 6, 18000.00, 3000.00, 5.00, 'modere', 5.00, 5.00, 5.00, 5.00, '⚠️ RISQUE MODÉRÉ: À surveiller étroitement.\n📊 Améliorer la rentabilité : considérez l\'optimisation des coûts de production.', 'draft', '2026-04-07 16:29:51', '2026-04-07 16:29:51', 15),
(3, 4, 12000.00, 3000.00, 5.00, 'modere', 5.00, 5.00, 5.00, 5.00, '⚠️ RISQUE MODÉRÉ: À surveiller étroitement.\n📊 Améliorer la rentabilité : considérez l\'optimisation des coûts de production.', 'draft', '2026-04-19 16:03:59', '2026-04-19 16:03:59', 16),
(4, 5, 15000.00, 3000.00, 3.74, 'eleve', 5.00, 4.00, 0.20, 5.00, 'pdf.credit.reco_high_risk\npdf.credit.reco_improve_profitability', 'draft', '2026-04-21 16:32:44', '2026-04-21 16:32:44', 18);

-- --------------------------------------------------------

--
-- Structure de la table `culture`
--

CREATE TABLE `culture` (
  `id` int(11) NOT NULL,
  `nom_culture` varchar(255) NOT NULL,
  `type_culture` varchar(255) DEFAULT NULL,
  `saison` varchar(100) DEFAULT NULL,
  `date_plantation` date DEFAULT NULL,
  `date_recolte_prevue` date DEFAULT NULL,
  `etat_culture` varchar(50) DEFAULT 'en_croissance',
  `parcelle_id` int(11) NOT NULL,
  `surface_utilisee` double NOT NULL DEFAULT 0,
  `rendement_estime` double DEFAULT 0,
  `production_estimee` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `culture`
--

INSERT INTO `culture` (`id`, `nom_culture`, `type_culture`, `saison`, `date_plantation`, `date_recolte_prevue`, `etat_culture`, `parcelle_id`, `surface_utilisee`, `rendement_estime`, `production_estimee`, `created_at`, `updated_at`) VALUES
(18, 'Fèves', 'Légumineuse', 'Hiver', '2025-12-01', '2026-04-15', 'en_croissance', 2, 3, 1.8, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(19, 'ble', 'ble', 'printemps', '2024-01-15', '2026-11-01', 'en_croissance', 3, 30, 11.2, 336, '2026-04-06 23:40:27', '2026-04-07 11:49:06'),
(20, 'Blé tendre', 'Céréale', 'Hiver', '2025-10-20', '2026-06-10', 'en_croissance', 3, 20, 3, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(22, 'Tomate', 'Légume', 'Printemps', '2026-03-01', '2026-07-15', 'en_croissance', 4, 3, 8, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(23, 'Piment', 'Légume', 'Printemps', '2026-03-10', '2026-08-01', 'en_croissance', 4, 2, 5, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(24, 'Orge', 'Céréale', 'Hiver', '2025-11-20', '2026-05-20', 'en_croissance', 5, 2, 2, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(25, 'Petit pois', 'Légumineuse', 'Hiver', '2025-12-10', '2026-04-01', 'en_croissance', 5, 1.5, 3, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(26, 'Agrumes', 'Arbre', 'Annuelle', '2024-03-01', '2026-12-01', 'en_croissance', 6, 5, 3.5, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(27, 'Fraise', 'Légume', 'Printemps', '2026-02-15', '2026-05-30', 'en_croissance', 6, 3, 6, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(28, 'Pomme de terre', 'Légume', 'Automne', '2025-09-01', '2025-12-15', 'récoltée', 7, 2, 6, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(29, 'Olivier', 'Arbre', 'Annuelle', '2023-02-01', '2026-11-15', 'en_croissance', 8, 7, 1.5, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(30, 'Amandier', 'Arbre', 'Annuelle', '2023-06-01', '2026-09-01', 'en_croissance', 8, 3, 0.8, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(31, 'Blé dur', 'Céréale', 'Hiver', '2025-11-10', '2026-05-25', 'en_croissance', 9, 2.5, 2.5, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(32, 'Fève', 'Légumineuse', 'Hiver', '2025-12-05', '2026-04-10', 'en_croissance', 9, 2, 1.8, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(33, 'Blé tendre', 'Céréale', 'Hiver', '2025-10-25', '2026-06-05', 'en_croissance', 10, 4, 3, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(34, 'Colza', 'Oléagineuse', 'Hiver', '2025-11-01', '2026-06-20', 'en_croissance', 10, 2, 1.2, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(35, 'Pastèque', 'Légume', 'Été', '2025-05-01', '2025-08-15', 'récoltée', 11, 1.5, 10, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(36, 'Oignon', 'Maraîcher', 'Printemps', '2026-03-01', '2026-06-29', 'en_croissance', 12, 10, 30, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(37, 'Tomate', 'Maraîcher', 'Printemps', '2026-03-02', '2026-06-30', 'en_croissance', 13, 5.1, 40, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(38, 'Oignon', 'Maraîcher', 'Printemps', '2026-03-02', '2026-06-30', 'en_croissance', 13, 4.9, 30, NULL, '2026-04-06 23:40:27', '2026-04-06 23:40:27'),
(39, 'ble', 'ble', 'printemps', '2026-04-18', '2026-04-29', 'active', 15, 45, 4500, 202500, '2026-04-07 15:24:09', '2026-04-07 15:24:09'),
(40, 'Tomate', 'Tomate', 'printemps', '2026-02-22', '2026-03-03', 'active', 16, 5, 666, 3330, '2026-04-19 15:02:03', '2026-04-19 15:02:03');

-- --------------------------------------------------------

--
-- Structure de la table `detailscommande`
--

CREATE TABLE `detailscommande` (
  `idDetails` int(11) NOT NULL,
  `id_commande` int(11) NOT NULL,
  `id_produit` int(11) NOT NULL,
  `quantite` int(11) NOT NULL DEFAULT 1,
  `prixUnitaire` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `detailscommande`
--

INSERT INTO `detailscommande` (`idDetails`, `id_commande`, `id_produit`, `quantite`, `prixUnitaire`) VALUES
(116, 83, 9, 1, 15),
(119, 85, 9, 1, 15),
(123, 89, 1, 1, 2.5),
(125, 90, 6, 1, 6.5),
(133, 98, 9, 1, 15),
(134, 99, 9, 1, 15),
(135, 100, 9, 6, 15),
(137, 101, 9, 1, 15),
(138, 102, 6, 2, 6.5),
(139, 103, 6, 2, 6.5),
(141, 105, 9, 1, 15),
(143, 107, 6, 1, 6.5),
(144, 108, 9, 1, 15),
(146, 110, 3, 1, 1.2),
(147, 111, 6, 1, 6.5),
(148, 112, 9, 1, 15),
(149, 113, 6, 1, 6.5),
(150, 114, 9, 1, 15),
(159, 123, 9, 1, 15),
(167, 128, 9, 1, 15),
(172, 132, 6, 1, 6.5),
(173, 133, 9, 1, 15),
(179, 139, 9, 1, 15),
(180, 140, 9, 1, 15),
(186, 146, 9, 2, 15),
(188, 148, 9, 2, 15),
(189, 149, 9, 2, 15),
(190, 150, 6, 1, 6.5),
(193, 152, 70, 1, 1.9),
(194, 153, 9, 1, 15),
(195, 154, 72, 1, 2.5),
(196, 155, 9, 1, 15),
(197, 156, 72, 1, 2.5),
(198, 157, 93, 1, 88),
(199, 158, 65, 1, 100),
(201, 160, 93, 1, 88),
(202, 161, 93, 1, 88),
(203, 162, 82, 10, 2.5),
(205, 164, 65, 222, 100),
(206, 165, 65, 1000, 100),
(207, 166, 65, 1, 100),
(209, 168, 65, 1, 100),
(211, 170, 3, 1, 1.2),
(212, 171, 93, 1, 79.2),
(213, 172, 9, 1, 15),
(214, 173, 9, 1, 15),
(215, 174, 9, 1, 15),
(216, 175, 9, 1, 15),
(217, 175, 65, 1, 100),
(218, 176, 65, 1, 100),
(219, 177, 65, 1, 100),
(220, 178, 6, 1, 6.5),
(221, 179, 65, 2, 100),
(222, 180, 93, 1, 79.2),
(223, 181, 93, 1, 79.2),
(224, 182, 93, 1, 79.2),
(225, 183, 9, 1, 15),
(226, 184, 93, 1, 79.2),
(227, 185, 96, 1, 2),
(228, 186, 96, 1, 2),
(229, 187, 94, 4, 66),
(230, 188, 82, 1, 2.5),
(231, 188, 83, 1, 0.8),
(232, 188, 94, 1, 66),
(233, 189, 76, 1, 2.4),
(234, 189, 77, 1, 1.5),
(235, 189, 78, 1, 1.7),
(236, 189, 79, 1, 2),
(237, 189, 80, 1, 1.2),
(238, 189, 81, 1, 0.8),
(239, 189, 82, 1, 2.5),
(240, 189, 83, 1, 0.8),
(241, 189, 94, 1, 66),
(242, 190, 83, 142, 0.8),
(243, 191, 93, 1, 79.2),
(244, 192, 93, 1, 79.2),
(245, 193, 93, 1, 79.2),
(246, 194, 93, 1, 79.2),
(247, 195, 93, 1, 79.2),
(248, 195, 97, 3, 1.35),
(249, 195, 98, 1, 10),
(250, 196, 93, 4, 79.2),
(251, 196, 97, 1, 1.35),
(252, 196, 98, 1, 10),
(253, 197, 97, 1, 1.35),
(254, 197, 98, 1, 10),
(255, 198, 99, 2, 19.99),
(256, 199, 100, 1, 55),
(257, 200, 100, 1, 55),
(258, 201, 95, 1, 99),
(259, 202, 95, 1, 99),
(260, 203, 95, 1, 99),
(261, 204, 95, 1, 99),
(262, 205, 98, 1, 10),
(263, 206, 98, 1, 10),
(264, 207, 100, 1, 55),
(265, 208, 6, 1, 6.5),
(266, 209, 101, 1, 55),
(267, 210, 101, 1, 55),
(268, 211, 99, 1, 19.99),
(269, 212, 99, 1, 19.99),
(270, 213, 99, 1, 19.99),
(271, 213, 100, 1, 55),
(272, 214, 102, 1, 151555),
(273, 215, 103, 1, 1500),
(274, 216, 95, 1, 79.2),
(275, 217, 99, 1, 19.99),
(276, 217, 100, 1, 55),
(277, 218, 97, 95, 1.35),
(278, 219, 98, 198, 10),
(279, 220, 99, 490, 19.99),
(280, 221, 95, 1, 79.2),
(281, 222, 99, 1, 1),
(282, 222, 100, 1, 55),
(283, 223, 100, 1, 55),
(284, 224, 9, 5, 15),
(285, 225, 72, 5, 2),
(286, 225, 81, 1, 0.8),
(287, 226, 106, 1, 150000),
(288, 227, 1, 1, 2.5),
(289, 228, 99, 1, 1),
(290, 229, 101, 1, 55);

-- --------------------------------------------------------

--
-- Structure de la table `diagnostic`
--

CREATE TABLE `diagnostic` (
  `id` int(11) NOT NULL,
  `date_scan` datetime DEFAULT current_timestamp(),
  `image_scannee` varchar(255) DEFAULT NULL,
  `resultat_ia` varchar(255) DEFAULT NULL,
  `confiance` double DEFAULT 0,
  `user_id` int(11) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `location_label` varchar(255) DEFAULT NULL,
  `severity` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `diagnostic`
--

INSERT INTO `diagnostic` (`id`, `date_scan`, `image_scannee`, `resultat_ia`, `confiance`, `user_id`, `latitude`, `longitude`, `location_label`, `severity`) VALUES
(2, '2026-02-08 12:48:00', 'C:\\Users\\mejsa\\Pictures\\Screenshots\\Capture d\'écran 2026-02-07 104934.png', 'Plante - ERREUR GOOGLE (403) : {  \"error\": {    \"code\": ...', 50, 2, 88, NULL, NULL, NULL),
(3, '2026-02-13 19:23:06', 'https://i.ibb.co/35yGxSd6/7c227ba5ab8f.jpg', 'Coriandrum sativum - aucune maladie visible', 100, 2, 36.3935, 10.6226, 'Hammamet, Nabeul Governorate, Tunisia', 'LOW'),
(4, '2026-02-16 10:24:38', 'https://i.ibb.co/84XGN3fY/e3650614e789.jpg', 'POMME - TACHE_FOLIEE_PAR_BACTERIE', 80, 2, 36.8244, 10.1763, 'Tunis, Tunis Governorate, Tunisia', 'MEDIUM'),
(5, '2026-02-28 15:05:27', 'https://i.ibb.co/MDWy22hm/f55356895ec1.jpg', 'Non identifiée - Inconnue', 0, 2, 36.8178, 10.1656, 'Tunis, Tunis Governorate, Tunisia', 'LOW'),
(6, '2026-02-28 15:05:49', 'https://i.ibb.co/GvNbRRfV/705917b72e24.jpg', 'POMME - TACHE_MOISI', 80, 2, 36.8178, 10.1656, 'Tunis, Tunis Governorate, Tunisia', 'MEDIUM'),
(7, '2026-03-01 23:15:14', 'https://i.ibb.co/qM2gb4SS/0f949095291c.jpg', 'BANANIER - MALADIE NON_VISIBLE', 100, 13, 36.7496, 10.2126, 'Ben Arous, Ben Arous Governorate, Tunisia', 'LOW'),
(8, '2026-03-01 23:16:21', 'https://i.ibb.co/qM2gb4SS/0f949095291c.jpg', 'BANANIER - POURRITURE_STOLONIERE', 80, 13, 36.7496, 10.2126, 'Ben Arous, Ben Arous Governorate, Tunisia', 'MEDIUM'),
(9, '2026-03-01 23:17:19', 'https://i.ibb.co/qM2gb4SS/0f949095291c.jpg', 'BANANIER - PAS_DE_MALADIE_VISIBLE', 100, 13, 36.7496, 10.2126, 'Ben Arous, Ben Arous Governorate, Tunisia', 'LOW'),
(10, '2026-03-02 10:15:42', 'https://i.ibb.co/H51H8N4/a8d6f7a43817.webp', 'TOMATE - Alternaria solani (maladie Alternaire ou alternariose)', 80, 13, 36.8244, 10.1763, 'Tunis, Tunis Governorate, Tunisia', 'MEDIUM'),
(11, '2026-04-07 00:54:28', 'https://i.ibb.co/MxVq8rM5/5407bee58fae.jpg', 'BANANIER - ANTHRACNOSE', 60, 2, 35.7582, 10.7138, 'Sahline, Monastir Governorate, Tunisia', 'MEDIUM'),
(12, '2026-04-19 13:49:10', 'https://i.ibb.co/1JwxQw5C/c8d3d290db7e.png', 'Tomat - Bactériose', 87, 13, 36.8061, 10.0931, 'Manouba, Manouba, Tunisia', 'MEDIUM');

-- --------------------------------------------------------

--
-- Structure de la table `diag_notification`
--

CREATE TABLE `diag_notification` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `message` longtext NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `related_entity_id` int(11) DEFAULT NULL,
  `related_entity_type` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `diag_notification`
--

INSERT INTO `diag_notification` (`id`, `user_id`, `type`, `message`, `is_read`, `created_at`, `related_entity_id`, `related_entity_type`) VALUES
(1, 2, 'LIKE', 'mej a aimé votre commentaire.', 0, '2026-04-19 13:56:46', 1, 'COMMENT'),
(2, 2, 'LIKE', 'mej a aimé votre publication.', 0, '2026-04-19 13:56:48', 1, 'POST'),
(3, 2, 'COMMENT_REPLY', 'mej a répondu à votre commentaire.', 0, '2026-04-19 13:56:57', 13, 'COMMENT'),
(4, 2, 'COMMENT', 'mej a commenté votre publication.', 1, '2026-04-19 13:57:31', 14, 'COMMENT'),
(5, 2, 'MENTION', 'mej vous a mentionné dans la communauté.', 1, '2026-04-19 13:57:31', 14, 'COMMENT'),
(6, 13, 'REVIEW', 'Un agronome a répondu à votre demande de suivi.', 1, '2026-04-19 14:07:08', 1, 'TREATMENT');

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `employe`
--

CREATE TABLE `employe` (
  `id_employe` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `poste` varchar(100) DEFAULT NULL,
  `telephone` varchar(20) DEFAULT NULL,
  `actif` tinyint(4) NOT NULL DEFAULT 1,
  `id_agriculteur` int(11) DEFAULT NULL,
  `qr_code_unique` varchar(50) DEFAULT NULL,
  `photo_path` varchar(500) DEFAULT NULL,
  `salaire_journalier` decimal(10,3) DEFAULT 45.000 COMMENT 'Salaire journalier en TND (base SMAG Tunisie 2024 ≈ 40-50 TND/j)',
  `type_contrat` varchar(20) DEFAULT 'CDI' COMMENT 'CDI, CDD, Saisonnier, Journalier',
  `date_embauche` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `employe`
--

INSERT INTO `employe` (`id_employe`, `nom`, `prenom`, `email`, `poste`, `telephone`, `actif`, `id_agriculteur`, `qr_code_unique`, `photo_path`, `salaire_journalier`, `type_contrat`, `date_embauche`) VALUES
(41, 'saif', 'mejri', 'mejsaif@gmail.com', 'ing', '12345648', 0, 2, 'EMP_41_CAB320', NULL, 95.000, 'CDI', NULL),
(42, 'saif', 'saifoj', 'sss@gmail.com', 'ing', '12345678', 0, 2, 'EMP_42_A8D304', NULL, 95.000, 'CDI', NULL),
(43, 'Ben Salah', 'Ahmed', 'ahmed.bensalah@ardhi.tn', 'Technicien', '71234567', 0, 2, 'EMP_S2_A1B2C3', NULL, 55.000, 'CDI', NULL),
(44, 'Trabelsi', 'Sonia', 'sonia.trabelsi@ardhi.tn', 'Agronome', '72345678', 0, 2, 'EMP_S2_D4E5F6', NULL, 80.000, 'CDI', NULL),
(45, 'Chaabane', 'Mohamed', 'mohamed.chaabane@ardhi.tn', 'Ouvrier agricole', '73456789', 0, 2, 'EMP_S2_G7H8I9', NULL, 42.000, 'Saisonnier', NULL),
(46, 'Mansouri', 'Fatma', 'fatma.mansouri@ardhi.tn', 'Superviseure', '74567890', 1, 2, 'EMP_S2_J1K2L3', NULL, 70.000, 'CDI', NULL),
(47, 'Aloui', 'Karim', 'yasminebenattia17@gmail.com', 'Ingénieur', '75678901', 1, 2, 'EMP_S2_M4N5O6', '/uploads/employes/EMP_47_1775555364.png', 95.021, 'CDI', NULL),
(48, 'Belhaj', 'Amira', 'amira.belhaj@ardhi.tn', 'Ouvrière', '76789012', 0, 2, 'EMP_S2_P7Q8R9', NULL, 42.000, 'Saisonnier', NULL),
(49, 'Hammami', 'Yassine', 'yassine.hammami@ardhi.tn', 'Technicien maint.', '77890123', 1, 2, 'EMP_S2_S1T2U3', NULL, 55.000, 'CDI', NULL),
(50, 'Zouari', 'Nadia', 'nadia.zouari@ardhi.tn', 'Resp. récolte', '78901234', 0, 2, 'EMP_S2_V4W5X6', NULL, 45.000, 'Saisonnier', NULL),
(51, 'Aloui', 'pij', 'saifadmin@gmail.com', 'jnskjnfkn', NULL, 0, 4, 'EMP_51_0CE0D4', NULL, 45.000, 'CDI', NULL),
(53, 'mdgfjiogj', 'pojidfojiogldfj', 'ali@gmail.com', '$pdfpioghokdfe', '12345678', 0, 13, 'EMP_53_17DC80', NULL, 45.000, 'CDI', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `employe_competence`
--

CREATE TABLE `employe_competence` (
  `id` int(11) NOT NULL,
  `id_employe` int(11) NOT NULL,
  `id_competence` int(11) NOT NULL,
  `niveau` int(11) NOT NULL DEFAULT 1,
  `annees_experience` decimal(3,1) DEFAULT 0.0,
  `derniere_utilisation` date DEFAULT NULL,
  `certification` varchar(200) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `date_ajout` datetime DEFAULT current_timestamp(),
  `date_modification` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `evenement`
--

CREATE TABLE `evenement` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `lieu` varchar(255) DEFAULT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `type` varchar(50) NOT NULL,
  `nombre_places_max` int(11) NOT NULL DEFAULT 50,
  `organisateur` varchar(255) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `statut` varchar(50) NOT NULL DEFAULT 'A_VENIR',
  `date_creation` datetime DEFAULT current_timestamp(),
  `id_createur` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `evenement`
--

INSERT INTO `evenement` (`id`, `titre`, `description`, `lieu`, `date_debut`, `date_fin`, `type`, `nombre_places_max`, `organisateur`, `image_url`, `statut`, `date_creation`, `id_createur`) VALUES
(2, 'FDHBHJH', 'jhgbf', 'dfhbhj', '2026-02-11', '2026-02-12', 'FOIRE', 50, 'fshb', 'uploads/evenements/event_1771243672812.png', 'TERMINE', '2026-02-16 13:07:56', 1),
(3, 'fhjbh', 'dfbhhhjbhj', 'dfhhbn', '2026-02-13', '2026-02-14', 'FORMATION', 50, 'dnhkj', '', 'TERMINE', '2026-02-16 13:12:01', 2),
(5, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:39', 1),
(6, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:39', 1),
(7, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:39', 1),
(8, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:39', 1),
(9, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:39', 1),
(10, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(11, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(12, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(13, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(14, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(15, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(16, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique pour optimiser les rendements', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(17, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie avec exposants internationaux', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:43:46', 1),
(18, 'Atelier Permaculture', 'Introduction pratique à la permaculture en climat méditerranéen', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:43:46', 1),
(19, 'Conférence Innovation Agricole', 'Les nouvelles technologies au service de agriculture tunisienne', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:43:46', 1),
(20, 'Formation Élevage Biologique', 'Techniques élevage respectueuses de environnement', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:43:46', 1),
(21, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(22, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(23, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(24, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(25, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(26, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(27, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique pour optimiser les rendements', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(28, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie avec exposants internationaux', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:44:29', 1),
(29, 'Atelier Permaculture', 'Introduction pratique à la permaculture en climat méditerranéen', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:44:29', 1),
(30, 'Conférence Innovation Agricole', 'Les nouvelles technologies au service de agriculture tunisienne', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:44:29', 1),
(31, 'Formation Élevage Biologique', 'Techniques élevage respectueuses de environnement', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:44:29', 1),
(32, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(33, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(34, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(35, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(36, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(37, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(38, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(39, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:21', 1),
(40, 'Atelier Permaculture', 'Introduction pratique à la permaculture', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:21', 1),
(41, 'Conférence Innovation Agricole', 'Les nouvelles technologies', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:21', 1),
(42, 'Formation Élevage Biologique', 'Techniques élevage bio', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:21', 1),
(43, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(44, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(45, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(46, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(47, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(48, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(49, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(50, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 16:57:42', 1),
(51, 'Atelier Permaculture', 'Introduction pratique à la permaculture', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:42', 1),
(52, 'Conférence Innovation Agricole', 'Les nouvelles technologies', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:42', 1),
(53, 'Formation Élevage Biologique', 'Techniques élevage bio', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 16:57:42', 1),
(54, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(55, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(56, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(57, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(58, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(59, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(60, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(61, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:11:53', 1),
(62, 'Atelier Permaculture', 'Introduction pratique à la permaculture', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:11:53', 1),
(63, 'Conférence Innovation Agricole', 'Les nouvelles technologies', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:11:53', 1),
(64, 'Formation Élevage Biologique', 'Techniques élevage bio', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:11:53', 1),
(65, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(66, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(67, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(68, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(69, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(70, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(71, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(72, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:22', 1),
(73, 'Atelier Permaculture', 'Introduction pratique à la permaculture', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:22', 1),
(74, 'Conférence Innovation Agricole', 'Les nouvelles technologies', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:22', 1),
(75, 'Formation Élevage Biologique', 'Techniques élevage bio', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:22', 1),
(76, 'Formation Agriculture Bio 2024', 'Formation sur les techniques bio', 'Tunis', '2024-03-15', '2024-03-15', 'FORMATION', 50, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(77, 'Foire Agricole Printemps', 'Grande foire agricole annuelle', 'Ariana', '2024-04-20', '2024-04-21', 'FOIRE', 100, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(78, 'Atelier Irrigation Moderne', 'Techniques irrigation goutte à goutte', 'Sousse', '2024-05-10', '2024-05-10', 'ATELIER', 30, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(79, 'Conférence Agriculture Durable', 'Conférence sur la durabilité', 'Sfax', '2024-06-15', '2024-06-15', 'CONFERENCE', 80, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(80, 'Formation Compostage', 'Apprendre le compostage', 'Tunis', '2024-07-05', '2024-07-05', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(81, 'Foire Équipement Agricole', 'Exposition matériel agricole', 'Ariana', '2024-08-10', '2024-08-11', 'FOIRE', 120, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(82, 'Formation Agriculture Bio Avancée', 'Techniques avancées en agriculture biologique', 'Tunis', '2026-03-20', '2026-03-20', 'FORMATION', 45, 'Ministère Agriculture', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(83, 'Grande Foire Agricole 2026', 'La plus grande foire agricole de Tunisie', 'Ariana', '2026-04-15', '2026-04-16', 'FOIRE', 150, 'CNEA', 'uploads/default.jpg', 'TERMINE', '2026-03-01 22:12:30', 1),
(84, 'Atelier Permaculture', 'Introduction pratique à la permaculture', 'Sousse', '2026-05-05', '2026-05-05', 'ATELIER', 35, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:30', 1),
(85, 'Conférence Innovation Agricole', 'Les nouvelles technologies', 'Sfax', '2026-06-10', '2026-06-10', 'CONFERENCE', 100, 'Ministère Agriculture', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:30', 1),
(86, 'Formation Élevage Biologique', 'Techniques élevage bio', 'Tunis', '2026-07-15', '2026-07-15', 'FORMATION', 40, 'CNEA', 'uploads/default.jpg', 'A_VENIR', '2026-03-01 22:12:30', 1),
(87, 'foire agricole', '\"Découvrez les dernières innovations et techniques agricoles à l\'atelier pratique \"Foires agricoles\" setenant à Ariana ! Ce rendez-vous exceptionnel vous offrira l\'occasion de mettre en avant les derniers outils et méthodes pour améliorer la productivité et la rentabilité de vos cultures. Grâce à l\'approche pratique et applicable, vous développerez des compétences concrets pour optimiser vos pratiques agricoles et relever les défis du secteur. Rejoignez-nous pour rester au fil de l\'eau de la modernité agricole et prendre votre place dans l\'évolution de l\'agriculture tunisienne.\"', 'ariana', '2026-03-08', '2026-03-08', 'ATELIER', 50, 'saif', 'uploads/evenements/unsplash_atelier_1772399710800.jpg', 'TERMINE', '2026-03-01 22:15:27', 13),
(88, 'foire agricole du printemps', 'Découvrez la Foire Agricole du Printemps à Tunis, lieu de rencontre incontournable pour les agriculteurs tunisiens ambitieux ! Cette exposition professionnelle exceptionnelle vous offre la possibilité de découvrir les dernières avancées en matière d\'innovation agricole, de technologie et de pratiques durables, tout en renforçant vos compétences et votre savoir-faire. Grâce à une large gamme d\'expositions, de démonstrations et d\'opportunités de networking, vous profiterez de l\'expérience inédite du marché agricole tunisien, vous permettant d\'optimiser vos activités et d\'accroître votre productivité. Rejoignez-nous pour accéder à des solutions pratiques et concrètes pour améliorer votre entreprise et votre succès !', 'tunis', '2026-03-09', '2026-03-09', 'FOIRE', 50, 'yass', 'uploads/evenements/unsplash_foire_1772444603736.jpg', 'TERMINE', '2026-03-02 10:44:46', 13),
(89, 'mjdgifj', 'kfmdkgk', 'kkl,nlkn,l', '0006-05-05', '2025-05-05', 'FORMATION', 50, 'm,l,qs', '/uploads/evenements/event_69d446c0534985.67393127.jpg', 'TERMINE', '2026-04-07 01:50:24', 1),
(91, 'ojsfl,gslk,', 'k,nfdlkg,ndfklg,', 'ds,flnds', '2026-04-07', '2026-04-08', 'FOIRE', 1000000000, 'daif', NULL, 'TERMINE', '2026-04-07 01:54:44', 2),
(92, 'TESTTTVAL', 'fdkjnjkhsdbj', 'sdjbj', '2026-04-01', '2026-04-02', 'FORMATION', 50, 'ezsbhb', NULL, 'TERMINE', '2026-04-07 15:25:51', 23),
(93, 'VALIDATION', 'SNBNHB', 'SDBN', '2026-04-02', '2026-04-04', 'FORMATION', 50, 'DSCDRV', '/uploads/evenements/event_69d509542edf92.34795850.png', 'TERMINE', '2026-04-07 15:40:36', 1),
(94, 'foire agri 2026', 'Découvrez la Foire Agri 2026 à Montplaisir, un espace de rencontre incontournable pour les agriculteurs tunisiens. Cette exposition professionnelle présentera les dernières innovations et technologies agricoles pour améliorer les conditions de production et augmenter les rendements. Vous pourrez faire des trouvailles et échanger avec les acteurs clés de l\'industrie, vous permettant de mettre en place des stratégies pratiques et applicables pour relever les défis de la production agricole. Rejoignez-nous à la Foire Agri 2026 pour prendre votre place au cœur de ce dynamisme et de cette évolution.', 'Montplaisir, Khereiddine Pacha, Délégation Cité El Khadra, Tunis, Gouvernorat Tunis, 1073, Tunisie', '2026-04-20', '2026-04-21', 'FOIRE', 50, 'saif mej', '/uploads/evenements/unsplash_foire_1776601000.jpg', 'EN_COURS', '2026-04-19 14:16:56', 13),
(95, 'TESTAPI', 'Retrouvez-nous au cœur de l\'innovation agricole au TESTAPI de Baguette & Baguette, où les agriculteurs tunisiens rencontreront les dernières technologies et avantages pour améliorer leur production, leur efficacité et leur rentabilité. Cette foire agricole offrira aux professionnels de l\'agriculture des solutions concrètes pour relever les défis de la productivité, de l\'efficacité des ressources et de la qualité des produits. Venez découvrir et expérimenter les dernières tendances et innovations pratiques, et profiterez d\'une plateforme unique pour échanger avec les experts et faire des affaires. Au TESTAPI, innover et progresser seront les mots d\'ordre !', 'Baguette & Baguette, Avenue de Paris, Habib Thameur, Délégation Bab Bhar, Tunis, Gouvernorat Tunis, 1000, Tunisie', '2026-04-24', '2026-04-26', 'FOIRE', 100, 'saif USER', '/uploads/evenements/unsplash_foire_1776778105.jpg', 'A_VENIR', '2026-04-21 15:29:29', 2),
(96, 'Foire agricole 2026', 'Profitez pleinement du rendez-vous agricole incontournable en Tunisie, la Foire agricole 2026 à Hôtel El Hana International. Cette plateforme unique réunit les acteurs clés de l\'industrie agricole pour partager les dernières avancées, échanger des meilleures pratiques et se lancer dans des partenariats durables. Au coeur de la Tunisie agricole, cette exposition présente des opportunités concrettes d\'amélioration des rendements, de réduction des coûts et d\'adoption de méthodes innovantes, contribuant ainsi au développement durable de l\'agriculture tunisienne.', 'Hôtel El Hana International, 49, Avenue Habib Bourguiba, Mongi Slim, Délégation Bab Bhar, Tunis, Gouvernorat Tunis, 1000, Tunisie', '2026-04-15', '2026-04-25', 'FOIRE', 100, 'saif mej', '/uploads/evenements/unsplash_foire_1776783451.jpg', 'EN_COURS', '2026-04-21 16:59:09', 13);

-- --------------------------------------------------------

--
-- Structure de la table `evenement_favoris`
--

CREATE TABLE `evenement_favoris` (
  `id` int(11) NOT NULL,
  `id_evenement` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `date_ajout` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `evenement_favoris`
--

INSERT INTO `evenement_favoris` (`id`, `id_evenement`, `id_utilisateur`, `date_ajout`) VALUES
(5, 87, 13, '2026-04-07 00:52:54'),
(6, 92, 23, '2026-04-07 14:26:42'),
(7, 94, 14, '2026-04-19 13:33:42');

-- --------------------------------------------------------

--
-- Structure de la table `farm_health_report`
--

CREATE TABLE `farm_health_report` (
  `id` int(11) NOT NULL,
  `scan_id` int(11) NOT NULL,
  `health_score` int(11) DEFAULT NULL,
  `biodiversity_score` int(11) DEFAULT NULL,
  `llava_analysis` text DEFAULT NULL,
  `generated_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `farm_health_report`
--

INSERT INTO `farm_health_report` (`id`, `scan_id`, `health_score`, `biodiversity_score`, `llava_analysis`, `generated_at`) VALUES
(1, 2, 64, 35, 'Il semble qu\'il y ait eu un malentendu. L\'image que vous avez fournie ne correspond pas au contexte que vous avez décrit, car elle montre des tomates et non des olives. Cependant, je vais essayer de vous fournir une analyse basée sur les informations fournies et en supposant que l\'image pourrait représenter un problème générique dans les cultures.\n\nÉtant donné que l\'image ne correspond pas au contexte (olives au stade germination), je vais quand même essayer de fournir une réponse basée sur les instructions et en considérant les risques potentiels pour les olives ou les cultures en général.\n\nDISEASE_RISK|MENACE DE MALADIE|MEDIUM|Les signes de maladie sont souvent visibles sur les feuilles, les tiges ou les fruits. Cependant, sans image appropriée des olives, il est difficile de préciser.\nPEST_OUTBREAK_RISK|RAVAGEURS|LOW|Les ravageurs peuvent être présents mais sans spécificité sur les olives, difficile d\'évaluer.\nNUTRIENT_DEFICIENCY|CARENCE NUTRITIVE|LOW|Les carences nutritives peuvent affecter la croissance mais l\'image ne fournit pas d\'information.\nLOW_POLLINATION|PROBLÈME DE POLLINISATION|LOW|Pas d\'info directe.\nSOIL_DEGRADATION|DÉGRADATION DU SOL|LOW|Pas visible sur l\'image.\n\nMais puisque l\'image fournie ne correspond pas au stade de germination des olives et qu\'il n\'y a pas d\'éléments suffisants pour fournir une analyse précise :\n\nAUCUN_RISQUE', '2026-03-01 22:23:00');

-- --------------------------------------------------------

--
-- Structure de la table `farm_health_scan`
--

CREATE TABLE `farm_health_scan` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `crop_type` varchar(100) NOT NULL,
  `planting_date` date NOT NULL,
  `growth_stage` varchar(50) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `concerns` text DEFAULT NULL,
  `photo_crops` varchar(500) DEFAULT NULL,
  `photo_soil` varchar(500) DEFAULT NULL,
  `photo_edges` varchar(500) DEFAULT NULL,
  `photo_insects` varchar(500) DEFAULT NULL,
  `photo_spacing` varchar(500) DEFAULT NULL,
  `photo_overview` varchar(500) DEFAULT NULL,
  `scan_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PENDING','PROCESSING','COMPLETED','FAILED') DEFAULT 'PENDING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `farm_health_scan`
--

INSERT INTO `farm_health_scan` (`id`, `user_id`, `crop_type`, `planting_date`, `growth_stage`, `latitude`, `longitude`, `concerns`, `photo_crops`, `photo_soil`, `photo_edges`, `photo_insects`, `photo_spacing`, `photo_overview`, `scan_date`, `status`) VALUES
(1, 13, 'Olives', '2026-03-06', 'Germination', NULL, NULL, 'je ne suis pas sure', 'https://i.ibb.co/xt2LjT8m/222d1cd37f9b.jpg', NULL, NULL, NULL, NULL, NULL, '2026-03-01 22:23:56', 'COMPLETED'),
(2, 1, 'dsflns', '2026-05-05', 'ksdflsdn', 88, 88, 'sdlm,fklsd,l', 'lsndlfnsd', 'ljnkfjnsjkn', 'knkdjfnkfdjsn', 'lnjfdnjksfn', 'mkskd,,flksd,', 'lklknfngfjfsn', '2026-04-07 00:01:00', 'PENDING');

-- --------------------------------------------------------

--
-- Structure de la table `irrigation_request`
--

CREATE TABLE `irrigation_request` (
  `id` int(11) NOT NULL,
  `date` date NOT NULL,
  `temperature_moyenne` decimal(10,2) NOT NULL,
  `temperature_max` decimal(10,2) NOT NULL,
  `temperature_min` decimal(10,2) NOT NULL,
  `precipitations` decimal(10,2) NOT NULL,
  `humidite` decimal(5,2) NOT NULL,
  `kc` decimal(5,2) NOT NULL,
  `volume_litres` decimal(15,2) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `parcelle_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `maintenance`
--

CREATE TABLE `maintenance` (
  `id_maintenance` int(11) NOT NULL,
  `materiel_id` int(11) NOT NULL,
  `description` text DEFAULT NULL,
  `date_maintenance` date NOT NULL,
  `google_calendar_event_id` varchar(255) DEFAULT NULL,
  `statut_maintenance` varchar(50) DEFAULT 'planifiee',
  `date_planifiee` date DEFAULT NULL,
  `date_realisee` date DEFAULT NULL,
  `type_maintenance` varchar(50) DEFAULT 'preventive',
  `decision_admin` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `maintenance`
--

INSERT INTO `maintenance` (`id_maintenance`, `materiel_id`, `description`, `date_maintenance`, `google_calendar_event_id`, `statut_maintenance`, `date_planifiee`, `date_realisee`, `type_maintenance`, `decision_admin`) VALUES
(1, 3, 'Maintenance préventive du matériel.\nVérification générale, graissage, contrôle des niveaux.', '2026-03-28', NULL, 'planifiee', NULL, NULL, 'preventive', NULL),
(2, 4, '[Préventive] Maintenance préventive du matériel.\nVérification générale, graissage, contrôle des niveaux.', '2026-04-01', NULL, 'planifiee', NULL, NULL, 'preventive', NULL),
(3, 5, '[Préventive] Maintenance préventive du matériel.\nVérification générale, graissage, contrôle des niveaux.', '2026-04-01', NULL, 'planifiee', NULL, NULL, 'preventive', NULL),
(4, 6, '[Préventive] Maintenance préventive du matériel.\nVérification générale, graissage, contrôle des niveaux.', '2026-04-02', NULL, 'planifiee', NULL, NULL, 'preventive', NULL),
(5, 6, '[Préventive] Maintenance préventive du matériel.\nVérification générale, graissage, contrôle des niveaux.', '2026-04-02', NULL, 'planifiee', NULL, NULL, 'preventive', NULL),
(6, 8, 'testtttt', '2026-04-07', 'tnj8eh39nmb42poo9aohu344ms', 'planifiee', NULL, NULL, 'corrective', NULL),
(7, 9, 'materiel en panne', '2026-05-11', '03t2l7rrqccrejq77dr9i8khms', 'en_cours', NULL, NULL, 'urgente', 'planification_demandee'),
(8, 9, 'materiel en panne', '2026-04-27', '9eo8ihf9k4fkg8v31h5a0ta8to', 'en_attente', NULL, NULL, 'urgente', 'urgent_accepte'),
(9, 9, 'materiel en panne', '2026-04-27', 'e0fk98ocmgam308jf5f1b26l0g', 'planifiee', NULL, NULL, 'urgente', NULL),
(10, 8, NULL, '2026-05-25', '26gjvnm8jnj7nkld70btbp3dqo', 'planifiee', NULL, NULL, 'corrective', NULL),
(11, 4, 'fkldsfnflsdnlfs,dflsd', '2026-04-19', NULL, 'en_cours', NULL, NULL, 'urgente', 'planification_demandee'),
(12, 3, 'l,sdlf,sldfs', '2026-04-19', NULL, 'en_cours', NULL, NULL, 'urgente', 'planification_demandee'),
(13, 13, 'jnknknnjhbnljhnjh', '2026-04-20', NULL, 'terminee', NULL, '2026-04-20', 'urgente', 'planification_demandee'),
(14, 11, 'uhnou\'iogji', '2026-04-30', 'tjsv8e50dba0jrokm4ro4nlie8', 'planifiee', NULL, NULL, 'corrective', NULL),
(15, 2, 'hgilyhj', '2026-04-30', '24lq6t9cgo6ec5u2ajfl3fcsos', 'planifiee', NULL, NULL, 'corrective', NULL),
(16, 2, 'hbkmkbhkbh', '2026-04-21', NULL, 'en_attente', NULL, NULL, 'urgente', 'urgent_accepte'),
(17, 14, 'fuite de huile et bruit bizarre', '2026-04-21', NULL, 'en_attente', NULL, NULL, 'urgente', 'urgent_accepte'),
(18, 15, 'gvj;vjk;hybhujk;kbj:jnk', '2026-04-21', NULL, 'en_cours', NULL, NULL, 'urgente', 'planification_demandee'),
(19, 16, 'd,gf,klmh,dgkl,hlfkg,hlkfgn', '2026-04-21', NULL, 'en_cours', NULL, NULL, 'urgente', 'planification_demandee'),
(20, 17, 'gmsifhgljdfjhlkge', '2026-04-21', NULL, 'en_attente', NULL, NULL, 'urgente', 'urgent_accepte'),
(21, 19, 'aaaaaaaaaaaaaaaaaaaaaaaaaaa', '2026-04-21', NULL, 'en_attente', NULL, NULL, 'urgente', 'urgent_accepte'),
(22, 18, 'aaaaaaaaaaaaaaaaaaaaaaaaaaa', '2026-04-21', NULL, 'en_attente', NULL, NULL, 'urgente', NULL),
(23, 12, NULL, '2026-04-29', '2qcj0qrn12shcr3b0h4dc1evls', 'planifiee', NULL, NULL, 'urgente', NULL),
(24, 17, 'ytyfytftuguyguygyug', '2026-04-21', '9fucds3qte12m3mh05gpcggo4o', 'planifiee', NULL, NULL, 'corrective', NULL),
(25, 20, 'hvbsuoyrgboqeryu', '2026-04-21', NULL, 'terminee', NULL, '2026-04-21', 'urgente', 'urgent_accepte');

-- --------------------------------------------------------

--
-- Structure de la table `materiel`
--

CREATE TABLE `materiel` (
  `id_materiel` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL,
  `etat` varchar(50) NOT NULL,
  `user_id` int(11) NOT NULL,
  `date_achat` date DEFAULT NULL,
  `date_prochaine_maintenance` date DEFAULT NULL,
  `google_calendar_event_id` varchar(255) DEFAULT NULL,
  `derniere_maintenance` date DEFAULT NULL,
  `frequence_maintenance_mois` int(11) DEFAULT 12,
  `image` varchar(255) DEFAULT NULL,
  `statut` varchar(50) NOT NULL DEFAULT 'en_service',
  `qr_code_token` varchar(255) DEFAULT NULL,
  `qr_code_path` varchar(255) DEFAULT NULL,
  `prochaine_maintenance_alerte` varchar(255) DEFAULT NULL,
  `heures_utilisation` int(11) NOT NULL DEFAULT 0,
  `seuil_maintenance_heures` int(11) NOT NULL DEFAULT 500,
  `derniere_maintenance_heures` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `materiel`
--

INSERT INTO `materiel` (`id_materiel`, `nom`, `type`, `etat`, `user_id`, `date_achat`, `date_prochaine_maintenance`, `google_calendar_event_id`, `derniere_maintenance`, `frequence_maintenance_mois`, `image`, `statut`, `qr_code_token`, `qr_code_path`, `prochaine_maintenance_alerte`, `heures_utilisation`, `seuil_maintenance_heures`, `derniere_maintenance_heures`) VALUES
(1, 'mjsfmosdjlifjiljsild', 'Moissonneuse', 'Neuf', 2, '2026-02-09', NULL, NULL, NULL, 12, NULL, 'en_service', NULL, NULL, NULL, 0, 500, 0),
(2, 'ôjsfgdipjgip', 'Moissonneuse', 'En maintenance', 2, '2023-02-10', NULL, NULL, NULL, 12, NULL, 'en_maintenance', 'a327eeba45042696cd571bf7fd16b768', 'uploads/qrcodes/qr-a327eeba45042696cd571bf7fd16b768.svg', NULL, 100, 500, 0),
(3, 'pfojsjgdfl', 'Moissonneuse', 'En panne', 13, '2024-02-22', '2026-03-28', 'al691refp1ameq6tdjln10rnpo', NULL, 12, NULL, 'attente_planification', '7ad05cfbfecc0d8aa754229f98788f9b', 'uploads/qrcodes/qr-7ad05cfbfecc0d8aa754229f98788f9b.svg', NULL, 0, 500, 0),
(4, 'traktour', 'Tracteur', 'En panne', 13, '2023-03-09', '2026-04-01', '03gusprmiir115u8mhhgn6tja0', NULL, 12, NULL, 'attente_planification', 'a16566169da360e098a81921437ad810', 'uploads/qrcodes/qr-a16566169da360e098a81921437ad810.svg', NULL, 0, 500, 0),
(5, 'semoir', 'Semoir', 'Moyen', 13, '2022-03-17', '2026-04-01', '0omkebus5lbrdu7t8pfps62eg0', NULL, 12, NULL, 'en_service', '482a355ce22c16660306e3c2349db5e2', 'uploads/qrcodes/qr-482a355ce22c16660306e3c2349db5e2.svg', NULL, 0, 500, 0),
(6, 'semoir', 'Semoir', 'Moyen', 13, '2024-03-01', '2026-04-02', '56f9ik615kjf1h22p9767a0etc', NULL, 12, NULL, 'en_vente', 'c3bde88045345568636935ae5abe0a9c', 'uploads/qrcodes/qr-c3bde88045345568636935ae5abe0a9c.svg', NULL, 0, 500, 0),
(8, 'traktour', 'Tracteur', 'Neuf', 23, '2024-02-25', '2024-08-25', NULL, NULL, 12, 'OIP-69d4c6bf3fdf6.webp', 'en_service', NULL, NULL, NULL, 0, 500, 0),
(9, 'semoirr', 'Semoir', 'En maintenance', 23, '2026-04-01', '2026-10-01', NULL, NULL, 12, 'rim-69d501b9b249e.webp', 'en_maintenance', NULL, NULL, NULL, 0, 500, 0),
(10, 'traktourrrrrrr', 'Tracteur', 'Neuf', 13, '2026-04-17', '2026-10-17', NULL, NULL, 12, 'rim-69e367578613f.webp', 'en_vente', '4f547acf539e9bca1eb88ebd2cd222e8', 'uploads/qrcodes/qr-4f547acf539e9bca1eb88ebd2cd222e8.svg', NULL, 0, 500, 0),
(11, 'semoir', 'Semoir', 'Neuf', 13, '2026-04-17', '2026-10-17', NULL, NULL, 12, 'stripe-69e36af97a388.png', 'vendu', 'ef1d106ae73bd0bbb957a5193f9952cc', 'uploads/qrcodes/qr-ef1d106ae73bd0bbb957a5193f9952cc.svg', NULL, 10, 200, 0),
(12, 'camion', 'Autre', 'Neuf', 13, '2026-04-18', '2026-10-18', NULL, NULL, 12, NULL, 'en_vente', 'deff80eef5c49792fdbd9b88e449ceaf', 'uploads/qrcodes/qr-deff80eef5c49792fdbd9b88e449ceaf.svg', NULL, 0, 500, 0),
(13, 'traktourR', 'Tracteur', 'En panne', 13, '2026-04-02', '2026-10-20', NULL, '2026-04-20', 12, 'ai-prediction-69e621790d909.webp', 'attente_planification', '27e2f82b970f9bf0f4b26bc6759b2137', 'uploads/qrcodes/qr-27e2f82b970f9bf0f4b26bc6759b2137.svg', NULL, 56, 500, 0),
(14, 'trakteuuuur', 'Tracteur', 'En maintenance', 13, '2026-04-02', '2026-10-02', NULL, NULL, 12, 'rim-69e76816d835d.webp', 'en_maintenance', '820d833f469ae581f9f3c03f58b5450a', 'uploads/qrcodes/qr-820d833f469ae581f9f3c03f58b5450a.svg', NULL, 200, 500, 0),
(15, 'ooooo', 'Moissonneuse', 'En panne', 13, '2026-04-02', '2026-10-02', NULL, NULL, 12, 'OIP-69e768db094c8.webp', 'attente_planification', '92c053e974167e87e27b14fd7922a5eb', 'uploads/qrcodes/qr-92c053e974167e87e27b14fd7922a5eb.svg', NULL, 100, 300, 0),
(16, 'saif', 'Semoir', 'En panne', 13, '2026-03-20', '2026-09-20', NULL, NULL, 12, NULL, 'attente_planification', '4b608240a5c182bda85fb64d74ef04a1', 'uploads/qrcodes/qr-4b608240a5c182bda85fb64d74ef04a1.svg', NULL, 0, 200, 0),
(17, 'kkôk', 'Moissonneuse', 'En maintenance', 13, '2026-04-20', '2026-10-20', NULL, NULL, 12, NULL, 'en_maintenance', '091cd3ed490f59d63d0e20c658ae2c85', 'uploads/qrcodes/qr-091cd3ed490f59d63d0e20c658ae2c85.svg', NULL, 0, 300, 0),
(18, '222', 'Pulvérisateur', 'En panne', 13, NULL, '2026-10-21', NULL, NULL, 12, NULL, 'panne_signalee', 'dff48903487769df5e070d830867c910', 'uploads/qrcodes/qr-dff48903487769df5e070d830867c910.svg', NULL, 0, 400, 0),
(19, '333', 'Tracteur', 'En maintenance', 13, '2026-04-10', '2026-10-10', NULL, NULL, 12, NULL, 'en_maintenance', '50031e43abd46912ee899cdc9b44604f', 'uploads/qrcodes/qr-50031e43abd46912ee899cdc9b44604f.svg', NULL, 0, 500, 0),
(20, 'trakteeur test', 'Semoir', 'En maintenance', 13, '2026-04-01', '2026-10-21', NULL, '2026-04-21', 12, 'rim-69e79349d7007.webp', 'en_maintenance', 'a78069a4ef2750218e64b6d5174f6c23', 'uploads/qrcodes/qr-a78069a4ef2750218e64b6d5174f6c23.svg', NULL, 98, 200, 0);

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `moderation_audit`
--

CREATE TABLE `moderation_audit` (
  `id` int(11) NOT NULL,
  `moderator_id` int(11) NOT NULL,
  `target_user_id` int(11) NOT NULL,
  `action` varchar(50) NOT NULL COMMENT 'MUTE, BAN, UNBAN, UNMUTE, DELETE_POST, DELETE_COMMENT, GRANT_MOD, REVOKE_MOD',
  `reason` varchar(500) DEFAULT NULL,
  `related_post_id` int(11) DEFAULT NULL,
  `related_comment_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `moderation_audit`
--

INSERT INTO `moderation_audit` (`id`, `moderator_id`, `target_user_id`, `action`, `reason`, `related_post_id`, `related_comment_id`, `created_at`) VALUES
(1, 2, 2, 'MUTE', 'kkk', NULL, NULL, '2026-04-19 14:02:49');

-- --------------------------------------------------------

--
-- Structure de la table `notification`
--

CREATE TABLE `notification` (
  `id_notification` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `priorite` varchar(20) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `message` text NOT NULL,
  `id_agriculteur` int(11) NOT NULL,
  `id_tache` int(11) DEFAULT NULL,
  `id_employe` int(11) DEFAULT NULL,
  `lue` tinyint(1) DEFAULT 0,
  `archivee` tinyint(1) DEFAULT 0,
  `date_creation` datetime DEFAULT current_timestamp(),
  `date_lecture` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `notification`
--

INSERT INTO `notification` (`id_notification`, `type`, `priorite`, `titre`, `message`, `id_agriculteur`, `id_tache`, `id_employe`, `lue`, `archivee`, `date_creation`, `date_lecture`) VALUES
(27, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plan de fertilisation printanier', 'Date limite : 05/04/2026 | Retard : 2 jour(s)', 2, 58, NULL, 0, 0, '2026-04-07 11:47:52', NULL),
(28, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : test', 'Date limite : 06/04/2026 | Retard : 1 jour(s)', 2, 71, 47, 0, 0, '2026-04-07 16:12:49', NULL),
(29, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plantation oliviers parcelle Nord', 'Date limite : 15/04/2026 | Retard : 4 jour(s)', 13, 56, NULL, 0, 0, '2026-04-19 15:49:06', NULL),
(30, 'TACHE_BLOQUEE', 'WARNING', '🔒 Tâche bloquée : Plantation oliviers parcelle Nord', 'Tâche en cours sans modification depuis plus de 2 jours. Un suivi est recommandé.', 13, 56, NULL, 0, 0, '2026-04-19 15:49:06', NULL),
(31, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plantation oliviers parcelle Nord', 'Date limite : 15/04/2026 | Retard : 5 jour(s)', 13, 56, NULL, 0, 0, '2026-04-20 01:03:42', NULL),
(32, 'TACHE_BLOQUEE', 'WARNING', '🔒 Tâche bloquée : Plantation oliviers parcelle Nord', 'Tâche en cours sans modification depuis plus de 2 jours. Un suivi est recommandé.', 13, 56, NULL, 0, 0, '2026-04-20 01:03:42', NULL),
(33, 'GEN_IDEAL', 'INFO', 'notification.meteo.general_good', '✨ Conditions idéales pour tout type de travaux et plantations.', 13, NULL, NULL, 0, 0, '2026-04-20 01:03:43', NULL),
(34, 'GEN_GOOD', 'INFO', 'notification.meteo.general_good', '✅ يوم مناسب للأنشطة الزراعية.', 13, NULL, NULL, 0, 0, '2026-04-20 14:49:23', NULL),
(35, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plantation oliviers parcelle Nord', 'Date limite : 15/04/2026 | Retard : 6 jour(s)', 13, 56, NULL, 0, 0, '2026-04-21 13:24:24', NULL),
(36, 'TACHE_BLOQUEE', 'WARNING', '🔒 Tâche bloquée : Plantation oliviers parcelle Nord', 'Tâche en cours sans modification depuis plus de 2 jours. Un suivi est recommandé.', 13, 56, NULL, 0, 0, '2026-04-21 13:24:24', NULL),
(37, 'GEN_HEAT', 'WARNING', '⚠️ Prudence recommandée en raison de la météo.', '☀️ Forte chaleur : privilégiez une irrigation tôt le matin ou soir.', 13, NULL, NULL, 0, 0, '2026-04-21 13:24:25', NULL),
(42, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Contrôle qualité huile olive lot 12', 'Date limite : 10/04/2026 | Retard : 11 jour(s)', 2, 69, NULL, 0, 0, '2026-04-21 13:34:45', NULL),
(47, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Coordination livraison coopérative Sfax', 'Date limite : 08/04/2026 | Retard : 13 jour(s)', 2, 68, NULL, 0, 0, '2026-04-21 13:34:45', NULL),
(51, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plan de fertilisation printanier', 'Date limite : 05/04/2026 | Retard : 16 jour(s)', 2, 58, NULL, 0, 0, '2026-04-21 13:34:45', NULL),
(52, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : test', 'Date limite : 06/04/2026 | Retard : 15 jour(s)', 2, 71, 47, 0, 0, '2026-04-21 13:34:45', NULL),
(53, 'GEN_HEAT', 'WARNING', '⚠️ Prudence recommandée en raison de la météo.', '☀️ Forte chaleur : privilégiez une irrigation tôt le matin ou soir.', 2, NULL, NULL, 0, 0, '2026-04-21 13:34:47', NULL),
(54, 'GEN_WIND_OK', 'INFO', '✅ Conditions idéales pour vos activités agricoles.', '🚜 Vent calme : parfait pour les traitements et la pulvérisation.', 2, NULL, NULL, 0, 0, '2026-04-21 16:42:48', NULL),
(55, 'METEO_POSITIVE', 'INFO', '✅ Recommandé : Contrôle qualité huile olive lot 12', '✅ Excellent moment pour la récolte « Contrôle qualité huile olive lot 12 » : 20°C, ciel dégagé — qualité optimale.', 2, 69, 46, 0, 0, '2026-04-25 11:58:13', NULL),
(56, 'METEO_POSITIVE', 'INFO', '✅ Recommandé : Coordination livraison coopérative Sfax', '✅ Excellent moment pour la récolte « Coordination livraison coopérative Sfax » : 20°C, ciel dégagé — qualité optimale.', 2, 68, 49, 0, 0, '2026-04-25 11:58:13', NULL),
(57, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : test', 'Date limite : 06/04/2026 | Retard : 19 jour(s)', 2, 71, 47, 1, 0, '2026-04-25 11:58:33', '2026-04-25 11:58:36'),
(58, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Contrôle qualité huile olive lot 12', 'Date limite : 10/04/2026 | Retard : 15 jour(s)', 2, 69, 46, 1, 0, '2026-04-25 11:58:33', '2026-04-25 11:58:37'),
(59, 'tache_bloquee', 'WARNING', '⚠️ Tâche bloquée : Contrôle qualité huile olive lot 12', 'Statut : En cours | Inactive depuis 4 jour(s)', 2, 69, 46, 1, 0, '2026-04-25 11:58:33', '2026-04-25 11:58:40'),
(60, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Coordination livraison coopérative Sfax', 'Date limite : 08/04/2026 | Retard : 17 jour(s)', 2, 68, 49, 0, 0, '2026-04-25 11:58:33', NULL),
(61, 'tache_bloquee', 'WARNING', '⚠️ Tâche bloquée : Coordination livraison coopérative Sfax', 'Statut : En cours | Inactive depuis 4 jour(s)', 2, 68, 49, 1, 0, '2026-04-25 11:58:33', '2026-04-25 11:58:42'),
(62, 'TACHE_RETARD', 'CRITICAL', '⏰ Tâche en retard : Plan de fertilisation printanier', 'Date limite : 05/04/2026 | Retard : 20 jour(s)', 2, 58, 47, 0, 0, '2026-04-25 11:58:33', NULL),
(63, 'tache_bloquee', 'WARNING', '⚠️ Tâche bloquée : Plan de fertilisation printanier', 'Statut : En cours | Inactive depuis 4 jour(s)', 2, 58, 47, 0, 0, '2026-04-25 11:58:33', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `notificationmaintenance`
--

CREATE TABLE `notificationmaintenance` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `materiel_id` int(11) DEFAULT NULL,
  `nouveau_statut` varchar(50) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `titre` varchar(255) DEFAULT NULL,
  `message` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `notificationmaintenance`
--

INSERT INTO `notificationmaintenance` (`id`, `user_id`, `materiel_id`, `nouveau_statut`, `is_read`, `created_at`, `titre`, `message`) VALUES
(1, 25, NULL, 'en_cours', 1, '2026-04-12 18:55:01', NULL, NULL),
(2, 1, NULL, 'reclamation_soumise', 0, '2026-04-12 19:59:19', NULL, NULL),
(3, 26, NULL, 'reclamation_soumise', 0, '2026-04-12 19:59:19', NULL, NULL),
(4, 1, NULL, 'reclamation_soumise', 0, '2026-04-12 20:03:43', NULL, NULL),
(5, 26, NULL, 'reclamation_soumise', 0, '2026-04-12 20:03:43', NULL, NULL),
(6, 1, NULL, 'reclamation_soumise', 0, '2026-04-12 20:14:27', NULL, NULL),
(7, 26, NULL, 'reclamation_soumise', 0, '2026-04-12 20:14:27', NULL, NULL),
(8, 13, 3, 'en_service', 0, '2026-04-15 22:22:19', 'Machine en maintenance', 'Le matériel pfojsjgdfl est désormais marqué en maintenance.'),
(9, 13, 4, 'en_service', 0, '2026-04-15 22:22:20', 'Machine en maintenance', 'Le matériel traktour est désormais marqué en maintenance.'),
(10, 13, 5, 'en_service', 1, '2026-04-15 22:22:20', 'Machine en maintenance', 'Le matériel semoir est désormais marqué en maintenance.'),
(11, 13, 6, 'en_service', 0, '2026-04-15 22:22:20', 'Machine en maintenance', 'Le matériel semoir est désormais marqué en maintenance.'),
(12, 25, NULL, 'en_service', 1, '2026-04-15 22:22:20', 'Machine en maintenance', 'Le matériel tracteur est désormais marqué en maintenance.'),
(13, 25, NULL, 'en_service', 1, '2026-04-15 22:22:20', 'Machine en maintenance', 'Le matériel semoir est désormais marqué en maintenance.'),
(14, 25, NULL, 'en_service', 1, '2026-04-15 22:25:56', 'Machine en maintenance', 'Le matériel charrue est désormais marqué en maintenance.'),
(15, 25, NULL, 'en_cours', 1, '2026-04-15 22:50:44', NULL, NULL),
(16, 25, NULL, 'en_service', 1, '2026-04-16 02:01:22', 'Machine en maintenance', 'Le matériel tracteur rim est désormais marqué en maintenance.'),
(17, 25, NULL, 'en_service', 1, '2026-04-16 02:36:36', 'Machine en maintenance', 'Le matériel charrue est désormais marqué en maintenance.'),
(18, 25, NULL, 'en_service', 1, '2026-04-16 19:43:19', 'Machine en maintenance', 'Le matériel traktout est désormais marqué en maintenance.'),
(19, 25, NULL, 'en_cours', 1, '2026-04-16 20:04:58', 'Mise à jour : traktout (En_cours)', 'URGENT : Veuillez apporter votre matériel à l\'atelier immédiatement pour intervention.'),
(20, 25, NULL, 'en_cours', 1, '2026-04-16 20:06:35', 'Mise à jour : traktout (En_cours)', 'URGENT : Veuillez apporter votre matériel à l\'atelier immédiatement pour intervention.'),
(21, 25, NULL, 'en_attente', 1, '2026-04-16 20:23:02', 'Mise à jour : traktourRim (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(22, 25, NULL, 'en_attente', 1, '2026-04-17 20:49:45', 'Mise à jour : traktourRim (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(23, 25, NULL, 'en_attente', 0, '2026-04-17 21:20:24', 'Mise à jour : traaaktouur (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(24, 25, NULL, 'en_attente', 0, '2026-04-17 21:21:02', 'Mise à jour : traaaktouur (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(25, 25, NULL, 'en_attente', 0, '2026-04-17 21:36:05', 'Mise à jour : charrue (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(26, 25, NULL, 'en_cours', 0, '2026-04-17 21:36:23', 'Mise à jour : charrue (En_cours)', 'Action requise : Veuillez planifier un créneau pour votre maintenance via le calendrier.'),
(27, 25, NULL, 'en_attente', 1, '2026-04-17 21:36:38', 'Mise à jour : charrue (En_attente)', 'le responsable a approuvé votre demande de maintenance en urgence veuillez apporter votre matériel dès que possible'),
(28, 25, 28, 'en_attente', 1, '2026-04-17 22:48:05', 'Urgence Acceptée : ljkk', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine ljkk, veuillez apporter votre matériel dès que possible'),
(29, 25, 29, 'en_attente', 1, '2026-04-17 23:33:22', 'Urgence Acceptée : fcvcefvzevf', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine fcvcefvzevf, veuillez apporter votre matériel dès que possible'),
(30, 25, 30, 'en_cours', 1, '2026-04-18 00:02:11', 'Planification demandée : grbrgrb', 'Votre demande de maintenance pour votre machine grbrgrb a été reçue, veuillez planifier une intervention via la page de maintenance'),
(31, 25, 31, 'en_attente', 1, '2026-04-18 00:06:08', 'Urgence Acceptée : l, l lk,lk', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine l, l lk,lk, veuillez apporter votre matériel dès que possible'),
(32, 25, 32, 'en_attente', 0, '2026-04-18 03:39:54', 'Urgence Acceptée : ifjqziofl', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine ifjqziofl, veuillez apporter votre matériel dès que possible'),
(33, 25, NULL, 'reclamation_resolue', 0, '2026-04-18 10:23:33', NULL, NULL),
(34, 25, 33, 'en_attente', 0, '2026-04-18 10:24:00', 'Urgence Acceptée : ljl,l,kl,lk,lk,', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine ljl,l,kl,lk,lk,, veuillez apporter votre matériel dès que possible'),
(35, 1, 6, 'reclamation_soumise', 0, '2026-04-19 16:12:12', NULL, NULL),
(36, 13, 4, 'en_cours', 0, '2026-04-19 16:18:56', 'Planification demandée : traktour', 'Votre demande de maintenance pour votre machine traktour a été reçue, veuillez planifier une intervention via la page de maintenance'),
(37, 13, 3, 'en_cours', 1, '2026-04-19 16:20:57', 'Planification demandée : pfojsjgdfl', 'Votre demande de maintenance pour votre machine pfojsjgdfl a été reçue, veuillez planifier une intervention via la page de maintenance'),
(38, 1, 12, 'reclamation_soumise', 0, '2026-04-19 17:18:26', NULL, NULL),
(39, 1, 13, 'reclamation_soumise', 0, '2026-04-20 15:12:24', NULL, NULL),
(40, 23, 9, 'en_cours', 0, '2026-04-20 15:20:31', 'Planification demandée : semoirr', 'Votre demande de maintenance pour votre machine semoirr a été reçue, veuillez planifier une intervention via la page de maintenance'),
(41, 13, 13, 'en_cours', 0, '2026-04-20 15:20:59', 'Planification demandée : traktourR', 'Votre demande de maintenance pour votre machine traktourR a été reçue, veuillez planifier une intervention via la page de maintenance'),
(42, 23, 9, 'en_attente', 0, '2026-04-20 15:21:09', 'Urgence Acceptée : semoirr', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine semoirr, veuillez apporter votre matériel dès que possible'),
(43, 13, 13, 'verifie', 0, '2026-04-20 15:27:16', 'Mise à jour : traktourR (Verifie)', 'L\'intervention a été effectuée et est en cours de vérification par nos techniciens.'),
(44, 13, 13, 'terminee', 0, '2026-04-20 15:27:25', 'Mise à jour : traktourR (Terminee)', 'Maintenance terminée avec succès. Votre matériel est de nouveau opérationnel et prêt à l\'emploi.'),
(45, 23, 9, 'en_attente', 0, '2026-04-20 15:27:28', 'Mise à jour : semoirr (En_attente)', 'Une demande de plannification est envoyer a l\'agriculteur'),
(46, 13, 13, 'reclamation_en_attente', 0, '2026-04-20 15:45:03', NULL, NULL),
(47, 13, 13, 'reclamation_en_cours', 0, '2026-04-20 15:45:13', NULL, NULL),
(48, 2, 2, 'en_attente', 0, '2026-04-21 14:00:54', 'Urgence Acceptée : ôjsfgdipjgip', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine ôjsfgdipjgip, veuillez apporter votre matériel dès que possible'),
(49, 13, 14, 'en_attente', 0, '2026-04-21 14:07:27', 'Urgence Acceptée : trakteuuuur', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine trakteuuuur, veuillez apporter votre matériel dès que possible'),
(50, 13, 15, 'en_cours', 0, '2026-04-21 14:09:36', 'Planification demandée : ooooo', 'Votre demande de maintenance pour votre machine ooooo a été reçue, veuillez planifier une intervention via la page de maintenance'),
(51, 13, 16, 'en_cours', 0, '2026-04-21 14:12:21', 'Planification demandée : saif', 'Votre demande de maintenance pour votre machine saif a été reçue, veuillez planifier une intervention via la page de maintenance'),
(52, 13, 17, 'en_attente', 0, '2026-04-21 14:19:06', 'Urgence Acceptée : kkôk', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine kkôk, veuillez apporter votre matériel dès que possible'),
(53, 13, 19, 'en_attente', 0, '2026-04-21 14:20:45', 'Urgence Acceptée : 333', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine 333, veuillez apporter votre matériel dès que possible'),
(54, 13, 20, 'en_attente', 0, '2026-04-21 17:13:38', 'Urgence Acceptée : trakteeur test', 'Le responsable a accepté votre demande de maintenance urgente pour votre machine trakteeur test, veuillez apporter votre matériel dès que possible'),
(55, 1, 20, 'reclamation_soumise', 0, '2026-04-21 17:15:17', NULL, NULL),
(56, 13, 20, 'reclamation_en_cours', 0, '2026-04-21 17:15:46', NULL, NULL),
(57, 13, 20, 'terminee', 0, '2026-04-21 17:17:46', 'Mise à jour : trakteeur test (Terminee)', 'Maintenance terminée avec succès. Votre matériel est de nouveau opérationnel et prêt à l\'emploi.');

-- --------------------------------------------------------

--
-- Structure de la table `notification_config`
--

CREATE TABLE `notification_config` (
  `id_config` int(11) NOT NULL,
  `id_agriculteur` int(11) NOT NULL,
  `seuil_taches_employe` int(11) DEFAULT 10,
  `seuil_jours_inactivite` int(11) DEFAULT 30,
  `activer_notifications` tinyint(1) DEFAULT 1,
  `activer_sons` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `notif_market`
--

CREATE TABLE `notif_market` (
  `id_notif` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `id_produit` int(11) DEFAULT NULL,
  `id_commande` int(11) DEFAULT NULL,
  `lue` tinyint(1) DEFAULT 0,
  `date_creation` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `notif_market`
--

INSERT INTO `notif_market` (`id_notif`, `id_user`, `type`, `titre`, `message`, `id_produit`, `id_commande`, `lue`, `date_creation`) VALUES
(3, 2, 'ACHAT', '🛒 Nouvelle commande reçue', 'agri saif vient de passer une commande (#151) pour un total de 15,00 DT.', NULL, 151, 1, '2026-03-01 23:23:01'),
(4, 2, 'AVIS', '⭐ Nouvel avis sur votre produit', 'agri saif a laissé un avis ⭐⭐⭐⭐⭐ sur \"Ananas\".', 75, NULL, 1, '2026-03-02 08:13:31'),
(5, 2, 'AVIS', '⭐ Nouvel avis sur votre produit', 'agri saif a laissé un avis ⭐ sur \"Avoine\".', 70, NULL, 1, '2026-03-02 08:13:36'),
(6, 2, 'ACHAT', '🛒 Nouvelle commande reçue', 'mej saif vient de passer une commande (#152) pour un total de 8,90 DT.', 70, 152, 1, '2026-03-02 08:33:29'),
(7, 4, 'ACHAT', '🛒 Nouvelle commande reçue', 'mej saif vient de passer une commande (#153) pour un total de 22,00 DT.', 9, 153, 0, '2026-03-02 08:33:29'),
(8, 2, 'ACHAT', '🛒 Nouvelle commande reçue', 'mej saif vient de passer une commande (#154) pour un total de 9,50 DT.', 72, 154, 1, '2026-03-02 10:04:33'),
(9, 4, 'ACHAT', '🛒 Nouvelle commande reçue', 'mej saif vient de passer une commande (#155) pour un total de 22,00 DT.', 9, 155, 0, '2026-03-02 10:04:33'),
(11, 13, 'avis', 'Nouvel avis sur votre produit', 'USER saif a laisse un avis (5/5) sur \"Pommes\".', 98, NULL, 1, '2026-04-13 22:24:16'),
(12, 13, 'avis', 'Nouvel avis sur votre produit', 'USER saif a laisse un avis (5/5) sur \"Pommes\".', 98, NULL, 1, '2026-04-13 22:26:46'),
(13, 13, 'avis', 'Nouvel avis sur votre produit', 'USER saif a laisse un avis (1/5) sur \"Pommes\".', 98, NULL, 1, '2026-04-13 22:27:33'),
(14, 13, 'avis', 'Nouvel avis sur votre produit', 'USER saif a laisse un avis (5/5) sur \"Pommes\".', 98, NULL, 1, '2026-04-13 22:28:30'),
(15, 23, 'avis', 'Nouvel avis sur votre produit', 'USER saif a laisse un avis (5/5) sur \"Pistaches\".', 100, NULL, 0, '2026-04-13 22:30:15'),
(16, 13, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#206) pour un total de 10.00 DT.', 98, 206, 1, '2026-04-13 22:32:57'),
(17, 23, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#207) pour un total de 55.00 DT.', 100, 207, 0, '2026-04-13 22:32:59'),
(21, 2, 'achat', 'Mise a jour de votre commande', 'Votre commande #181 est maintenant: Annulee (Total: 79.20 DT).', NULL, 181, 0, '2026-04-14 16:37:27'),
(22, 2, 'achat', 'Mise a jour de votre commande', 'Votre commande #180 est maintenant: Annulee (Total: 79.20 DT).', NULL, 180, 0, '2026-04-14 16:37:40'),
(24, 1, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#208) pour un total de 6.50 DT.', 6, 208, 0, '2026-04-15 23:09:06'),
(25, 13, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#209) pour un total de 55.00 DT.', 101, 209, 1, '2026-04-15 23:09:11'),
(26, 2, 'achat', 'Mise a jour de votre commande', 'Votre commande #209 est maintenant: En cours (Total: 55.00 DT).', NULL, 209, 0, '2026-04-15 23:10:24'),
(27, 13, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#210) pour un total de 55.00 DT.', 101, 210, 1, '2026-04-15 23:58:47'),
(28, 23, 'avis', 'Nouvel avis sur votre produit', 'mej saif a laisse un avis (1/5) sur \"Banane\".', 99, NULL, 0, '2026-04-17 20:10:00'),
(29, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#211) pour un total de 19.99 DT.', 99, 211, 0, '2026-04-17 20:45:31'),
(30, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#212) pour un total de 26.99 DT.', 99, 212, 0, '2026-04-17 21:03:59'),
(31, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#213) pour un total de 81.99 DT.', 99, 213, 0, '2026-04-17 21:11:17'),
(32, 13, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#214) pour un total de 151555.00 DT.', 102, 214, 1, '2026-04-18 12:16:29'),
(33, 2, 'achat', 'Mise a jour de votre commande', 'Votre commande #214 est maintenant: En cours (Total: 151555.00 DT).', NULL, 214, 0, '2026-04-18 12:17:05'),
(34, 13, 'achat', 'Nouvelle commande recue', 'istic saif vient de passer une commande (#215) pour un total de 1500.00 DT.', 103, 215, 1, '2026-04-18 12:31:15'),
(35, 23, 'achat', 'Mise a jour de votre commande', 'Votre commande #215 est maintenant: En cours (Total: 1500.00 DT).', NULL, 215, 0, '2026-04-18 12:31:45'),
(36, 2, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#216) pour un total de 79.20 DT.', 95, 216, 0, '2026-04-18 20:41:27'),
(37, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#217) pour un total de 74.99 DT.', 99, 217, 0, '2026-04-18 20:41:33'),
(38, 13, 'achat', 'Nouvelle commande recue', 'saif saif vient de passer une commande (#218) pour un total de 128.25 DT.', 97, 218, 1, '2026-04-18 22:01:06'),
(39, 13, 'achat', 'Nouvelle commande recue', 'saif saif vient de passer une commande (#219) pour un total de 1980.00 DT.', 98, 219, 1, '2026-04-18 22:02:38'),
(40, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#220) pour un total de 9795.10 DT.', 99, 220, 0, '2026-04-18 22:04:40'),
(41, 13, 'achat', 'Mise a jour de votre commande', 'Votre commande #220 est maintenant: Annulee (Total: 9795.10 DT).', NULL, 220, 1, '2026-04-18 22:12:59'),
(42, 2, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#221) pour un total de 86.20 DT.', 95, 221, 0, '2026-04-20 22:00:37'),
(43, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#222) pour un total de 63.00 DT.', 99, 222, 0, '2026-04-20 22:00:43'),
(44, 13, 'achat', 'Mise a jour de votre commande', 'Votre commande #222 est maintenant: Annulee (Total: 63.00 DT).', NULL, 222, 0, '2026-04-20 22:03:03'),
(45, 13, 'achat', 'Mise a jour de votre commande', 'Votre commande #217 est maintenant: En cours (Total: 74.99 DT).', NULL, 217, 0, '2026-04-20 22:03:20'),
(46, 23, 'achat', 'Nouvelle commande recue', 'mej saif vient de passer une commande (#223) pour un total de 55.00 DT.', 100, 223, 0, '2026-04-21 11:17:34'),
(47, 4, 'achat', 'Nouvelle commande recue', 'istic saif vient de passer une commande (#224) pour un total de 75.00 DT.', 9, 224, 0, '2026-04-21 16:29:23'),
(48, 2, 'achat', 'Nouvelle commande recue', 'istic saif vient de passer une commande (#225) pour un total de 10.80 DT.', 72, 225, 0, '2026-04-21 16:29:28'),
(49, 13, 'avis', 'Nouvel avis sur votre produit', 'istic saif a laisse un avis (5/5) sur \"camion\".', 106, NULL, 0, '2026-04-21 16:31:22'),
(50, 13, 'achat', 'Nouvelle commande recue', 'istic saif vient de passer une commande (#226) pour un total de 150000.00 DT.', 106, 226, 0, '2026-04-21 16:35:00'),
(51, 1, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#227) pour un total de 2.50 DT.', 1, 227, 0, '2026-05-02 16:39:30'),
(52, 23, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#228) pour un total de 1.00 DT.', 99, 228, 0, '2026-05-02 16:39:34'),
(53, 13, 'achat', 'Nouvelle commande recue', 'USER saif vient de passer une commande (#229) pour un total de 55.00 DT.', 101, 229, 0, '2026-05-02 16:39:35');

-- --------------------------------------------------------

--
-- Structure de la table `offre`
--

CREATE TABLE `offre` (
  `id` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `prix_mensuel` float NOT NULL,
  `avantages` text DEFAULT NULL,
  `diagnostics_par_heure` int(11) DEFAULT 3,
  `acces_traitement` tinyint(1) DEFAULT 0,
  `acces_plan_traitement` tinyint(1) DEFAULT 0,
  `couleur_primaire` varchar(20) DEFAULT NULL,
  `couleur_secondaire` varchar(20) DEFAULT NULL,
  `est_active` tinyint(1) DEFAULT 1,
  `est_recommandee` tinyint(1) DEFAULT 0,
  `date_creation` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `offre`
--

INSERT INTO `offre` (`id`, `nom`, `description`, `prix_mensuel`, `avantages`, `diagnostics_par_heure`, `acces_traitement`, `acces_plan_traitement`, `couleur_primaire`, `couleur_secondaire`, `est_active`, `est_recommandee`, `date_creation`) VALUES
(4, 'Express', 'Pour une analyse rapide et ponctuelle', 4.99, '5 diagnostics/heure\r\nAccès standard\r\nSupport par email', 5, 0, 0, '#95a5a6', '#7f8c8d', 1, 0, '2026-02-10 14:11:21'),
(5, 'Premium', 'L\'offre idéale pour les passionnés', 19.99, '20 diagnostics/heure|Accès aux traitements détaillés|Support prioritaire|Pas de publicités', 20, 1, 1, '#1abc9c', '#16a085', 1, 1, '2026-02-10 14:11:21'),
(6, 'VIP', 'Solution complète pour les experts', 49.99, 'Diagnostics illimités|Accès prioritaire aux nouvelles fonctionnalités|Consultation agronome dédiée|API Access', -1, 1, 1, '#34495e', '#2c3e50', 1, 1, '2026-02-10 14:11:21');

-- --------------------------------------------------------

--
-- Structure de la table `panier`
--

CREATE TABLE `panier` (
  `idPanier` int(11) NOT NULL,
  `dateCreation` date DEFAULT NULL,
  `totalMontant` decimal(12,2) NOT NULL DEFAULT 0.00,
  `totalProduits` int(11) DEFAULT 0,
  `id_user` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `panier`
--

INSERT INTO `panier` (`idPanier`, `dateCreation`, `totalMontant`, `totalProduits`, `id_user`) VALUES
(23, '2026-04-16', 0.00, 0, 2),
(31, '2026-04-18', 0.00, 0, 3),
(33, '2026-04-21', 0.00, 0, 13),
(34, '2026-04-21', 0.00, 0, 23);

-- --------------------------------------------------------

--
-- Structure de la table `panier_produits`
--

CREATE TABLE `panier_produits` (
  `id_panier` int(11) NOT NULL,
  `id_produit` int(11) NOT NULL,
  `quantite` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `parcelle`
--

CREATE TABLE `parcelle` (
  `id` int(11) NOT NULL,
  `surface` double NOT NULL,
  `localisation` varchar(255) DEFAULT NULL,
  `type_sol` varchar(255) DEFAULT NULL,
  `systeme_irrigation` varchar(255) DEFAULT NULL,
  `statut` varchar(50) DEFAULT 'active',
  `agriculteur_id` int(11) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `polygon_geojson` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '(DC2Type:json)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `parcelle`
--

INSERT INTO `parcelle` (`id`, `surface`, `localisation`, `type_sol`, `systeme_irrigation`, `statut`, `agriculteur_id`, `latitude`, `longitude`, `created_at`, `updated_at`, `polygon_geojson`) VALUES
(2, 88, 'mrj', 'argileux', 'goutte_a_goutte', 'active', 2, NULL, NULL, '2026-04-06 23:39:34', '2026-04-07 11:47:59', NULL),
(3, 65, 'Borj Cedria, Délégation Hammam Chott, Gouvernorat Ben Arous, 2084, Tunisie', 'Argileux', 'f^dojkgildljfglk', 'active', 2, 36.706963, 10.407143, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(4, 5, 'Tunis - La Marsa', 'Argileux', 'Goutte à goutte', 'active', 3, 36.8781, 10.3247, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(5, 3.5, 'Bizerte - Utique', 'Sablonneux', 'Aspersion', 'active', 3, 37.06, 9.76, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(6, 8, 'Nabeul - Hammamet', 'Limoneux', 'Gravitaire', 'active', 3, 36.4, 10.6167, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(7, 2, 'Sousse - Centre', 'Calcaire', 'Goutte à goutte', 'repos', 2, 35.8256, 10.6369, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(8, 10, 'Sfax - El Hencha', 'Argileux', 'Aspersion', 'active', 3, 34.7333, 10.7667, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(9, 4.5, 'Manouba - Tebourba', 'Limoneux', 'Gravitaire', 'active', 3, 36.8333, 9.8333, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(10, 6, 'Béja - Nefza', 'Sablonneux', 'Goutte à goutte', 'active', 3, 36.9667, 9.0667, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(11, 1.5, 'Kairouan - Sbikha', 'Argileux', 'Aspersion', 'repos', 3, 35.6833, 10.1, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(12, 10, 'El Mourouj (5), Délégation El Mourouj, Gouvernorat Ben Arous, 2074, Tunisie', 'Argilo-limoneux', 'Goutte-à-goutte', 'active', 2, 36.708064, 10.206299, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(13, 10, 'Targuellache, Délégation Mateur, Gouvernorat Bizerte, Tunisie', 'Argilo-limoneux', 'goutte', 'active', 13, 36.936721, 9.788818, '2026-04-06 23:39:34', '2026-04-06 23:39:34', NULL),
(15, 90, 'ghazela', 'argileux', 'goutte_a_goutte', 'active', 2, NULL, NULL, '2026-04-07 15:22:10', '2026-04-07 15:22:26', NULL),
(16, 10, 'El Mehiri', 'Sableux', 'Goutte-à-goutte', 'active', 13, 36.7579, 10.2293, '2026-04-19 15:00:44', '2026-04-19 15:00:44', NULL),
(17, 13004.66, 'Sidi Said', 'Argilo-limoneux', 'Goutte-à-goutte', 'active', 2, 36.1872, 9.6438, '2026-04-21 14:08:12', '2026-04-21 14:08:43', '{\"type\":\"Feature\",\"properties\":[],\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[9.48907,36.08906],[9.791011,36.16227],[9.602984,36.223227],[9.692194,36.274172],[9.48907,36.08906]]]}}'),
(18, 0.9, 'Targuellache', 'Argilo-limoneux', 'Goutte-à-goutte', 'active', 13, 36.9691, 9.7502, '2026-04-21 15:26:28', '2026-04-21 15:27:03', '{\"type\":\"Feature\",\"properties\":[],\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[9.747993,36.969027],[9.752496,36.970432],[9.74885,36.972044],[9.751509,36.964844],[9.747993,36.969027]]]}}');

-- --------------------------------------------------------

--
-- Structure de la table `participation`
--

CREATE TABLE `participation` (
  `id` int(11) NOT NULL,
  `id_evenement` int(11) NOT NULL,
  `id_utilisateur` int(11) NOT NULL,
  `date_inscription` datetime DEFAULT current_timestamp(),
  `statut` varchar(50) NOT NULL DEFAULT 'CONFIRME',
  `commentaire` text DEFAULT NULL,
  `nombre_personnes` int(11) DEFAULT 1,
  `note` int(11) DEFAULT 0 CHECK (`note` >= 0 and `note` <= 5),
  `avis` text DEFAULT NULL,
  `attestation_envoyee` tinyint(1) DEFAULT 0,
  `qr_code_token` varchar(64) DEFAULT NULL,
  `rappel_j3_envoye` tinyint(1) NOT NULL DEFAULT 0,
  `rappel_j1_envoye` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `participation`
--

INSERT INTO `participation` (`id`, `id_evenement`, `id_utilisateur`, `date_inscription`, `statut`, `commentaire`, `nombre_personnes`, `note`, `avis`, `attestation_envoyee`, `qr_code_token`, `rappel_j3_envoye`, `rappel_j1_envoye`) VALUES
(2, 3, 2, '2026-02-16 13:14:24', 'CONFIRME', '', 1, 4, '', 0, NULL, 0, 0),
(5, 10, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(6, 21, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(7, 32, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(8, 43, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(9, 54, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(10, 65, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(11, 76, 14, '2024-03-01 10:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(19, 5, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(20, 11, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(21, 22, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(22, 33, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(23, 44, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(24, 55, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(25, 66, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(26, 77, 14, '2024-04-10 14:00:00', 'ANNULE', 'Problème personnel', 1, 0, NULL, 0, NULL, 0, 0),
(34, 6, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(35, 12, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(36, 23, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(37, 34, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(38, 45, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(39, 56, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(40, 67, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(41, 78, 14, '2024-05-05 09:00:00', 'ANNULE', 'Changement de programme', 1, 0, NULL, 0, NULL, 0, 0),
(49, 7, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(50, 13, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(51, 24, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(52, 35, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(53, 46, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(54, 57, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(55, 68, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(56, 79, 14, '2024-06-01 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(64, 8, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(65, 14, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(66, 25, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(67, 36, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(68, 47, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(69, 58, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(70, 69, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(71, 80, 14, '2024-07-01 15:00:00', 'ANNULE', 'Trop loin', 1, 0, NULL, 0, NULL, 0, 0),
(79, 9, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(80, 15, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(81, 26, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(82, 37, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(83, 48, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(84, 59, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(85, 70, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(86, 81, 14, '2024-08-01 10:00:00', 'ANNULE', 'Date non convenable', 1, 0, NULL, 0, NULL, 0, 0),
(95, 10, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(96, 21, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(97, 32, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(98, 43, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(99, 54, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(100, 65, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(101, 76, 15, '2024-03-10 10:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(109, 5, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(110, 11, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(111, 22, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(112, 33, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(113, 44, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(114, 55, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(115, 66, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(116, 77, 15, '2024-04-15 14:00:00', 'PRESENT', 'Très intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(124, 6, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(125, 12, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(126, 23, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(127, 34, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(128, 45, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(129, 56, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(130, 67, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(131, 78, 15, '2024-05-08 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(139, 7, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(140, 13, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(141, 24, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(142, 35, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(143, 46, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(144, 57, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(145, 68, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(146, 79, 15, '2024-06-10 11:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(154, 8, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(155, 14, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(156, 25, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(157, 36, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(158, 47, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(159, 58, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(160, 69, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(161, 80, 15, '2024-07-02 15:00:00', 'PRESENT', 'Bon contenu', 1, 0, NULL, 1, NULL, 0, 0),
(169, 9, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(170, 15, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(171, 26, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(172, 37, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(173, 48, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(174, 59, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(175, 70, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(176, 81, 15, '2024-08-05 10:00:00', 'ANNULE', 'Empêchement', 1, 0, NULL, 0, NULL, 0, 0),
(184, 5, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(185, 11, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(186, 22, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(187, 33, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(188, 44, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(189, 55, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(190, 66, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(191, 77, 16, '2024-04-18 10:00:00', 'PRESENT', 'Intéressant', 1, 0, NULL, 1, NULL, 0, 0),
(199, 6, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(200, 12, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(201, 23, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(202, 34, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(203, 45, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(204, 56, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(205, 67, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(206, 78, 16, '2024-05-09 14:00:00', 'PRESENT', 'Bon atelier', 1, 0, NULL, 1, NULL, 0, 0),
(214, 7, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(215, 13, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(216, 24, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(217, 35, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(218, 46, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(219, 57, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(220, 68, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(221, 79, 16, '2024-06-12 09:00:00', 'CONFIRME', NULL, 1, 0, NULL, 0, NULL, 0, 0),
(229, 8, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(230, 14, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(231, 25, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(232, 36, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(233, 47, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(234, 58, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(235, 69, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(236, 80, 16, '2024-07-03 11:00:00', 'ANNULE', 'Imprévu', 1, 0, NULL, 0, NULL, 0, 0),
(244, 9, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(245, 15, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(246, 26, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(247, 37, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(248, 48, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(249, 59, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(250, 70, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(251, 81, 16, '2024-08-08 15:00:00', 'PRESENT', 'Très bien', 1, 0, NULL, 1, NULL, 0, 0),
(260, 10, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(261, 21, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(262, 32, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(263, 43, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(264, 54, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(265, 65, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(266, 76, 16, '2024-03-14 10:00:00', 'PRESENT', 'Utile', 1, 0, NULL, 1, NULL, 0, 0),
(274, 6, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(275, 12, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(276, 23, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(277, 34, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(278, 45, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(279, 56, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(280, 67, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(281, 78, 13, '2024-05-09 10:00:00', 'PRESENT', 'Excellent', 1, 0, NULL, 1, NULL, 0, 0),
(289, 7, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(290, 13, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(291, 24, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(292, 35, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(293, 46, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(294, 57, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(295, 68, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(296, 79, 13, '2024-06-14 14:00:00', 'PRESENT', 'Très instructif', 1, 0, NULL, 1, NULL, 0, 0),
(304, 8, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(305, 14, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(306, 25, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(307, 36, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(308, 47, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(309, 58, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(310, 69, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(311, 80, 13, '2024-07-04 09:00:00', 'PRESENT', 'Bravo', 1, 0, NULL, 1, NULL, 0, 0),
(319, 9, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(320, 15, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(321, 26, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(322, 37, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(323, 48, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(324, 59, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(325, 70, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(326, 81, 13, '2024-08-09 11:00:00', 'PRESENT', 'Super', 1, 0, NULL, 1, NULL, 0, 0),
(334, 5, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(335, 11, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(336, 22, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(337, 33, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(338, 44, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(339, 55, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(340, 66, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(341, 77, 13, '2024-04-19 15:00:00', 'PRESENT', 'Top', 1, 0, NULL, 1, NULL, 0, 0),
(349, 16, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(350, 27, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(351, 38, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(352, 49, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(353, 60, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(354, 71, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(355, 82, 13, '2026-03-01 10:00:00', 'CONFIRME', 'Hâte', 1, 0, NULL, 0, NULL, 0, 0),
(356, 87, 13, '2026-03-01 22:16:16', 'PRESENT', '', 1, 5, 'hello wo', 1, '8ca462a0dc234e209ad3108a8dc52ea0', 0, 0),
(357, 88, 13, '2026-03-02 10:45:14', 'PRESENT', '', 1, 0, NULL, 1, '7717138d807243e7ab89413913b967b7', 0, 0),
(358, 17, 13, '2026-04-07 01:55:09', 'PRESENT', NULL, 1, 0, NULL, 1, '2f5a301642869388854380b5dd76b1e6', 0, 0),
(359, 28, 23, '2026-04-07 15:32:05', 'CONFIRME', NULL, 1, 0, NULL, 0, '113325f51d58c1056d38272d36bad528', 0, 0),
(360, 92, 3, '2026-04-07 15:34:30', 'PRESENT', NULL, 1, 1, 'HGDS', 1, '8e8ecbcf589d4029ab408d4a6a79e615', 0, 0),
(361, 93, 23, '2026-04-07 15:40:54', 'PRESENT', NULL, 1, 0, NULL, 1, 'b362e68a9832db0f2c8671c80eac4147', 0, 0),
(362, 94, 14, '2026-04-19 14:33:44', 'CONFIRME', NULL, 1, 0, NULL, 0, 'b8c99b9cf351ca93aa68edb3c247499c', 0, 0),
(363, 95, 13, '2026-04-21 15:31:22', 'PRESENT', NULL, 1, 0, NULL, 0, 'f985326e7cdb298dea6c09c215a76720', 0, 0);

-- --------------------------------------------------------

--
-- Structure de la table `prevention_plan`
--

CREATE TABLE `prevention_plan` (
  `id` int(11) NOT NULL,
  `report_id` int(11) NOT NULL,
  `vulnerability_id` int(11) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `problem_summary` text DEFAULT NULL,
  `steps` text NOT NULL,
  `timeline_days` int(11) DEFAULT NULL,
  `estimated_cost` float DEFAULT NULL,
  `expected_outcome` text DEFAULT NULL,
  `impact_level` enum('HIGH','MEDIUM','LOW') DEFAULT NULL,
  `status` enum('ACTIVE','COMPLETED','ABANDONED') DEFAULT 'ACTIVE',
  `start_date` date DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `prevention_task`
--

CREATE TABLE `prevention_task` (
  `id` int(11) NOT NULL,
  `prevention_plan_id` int(11) NOT NULL,
  `day_offset` int(11) NOT NULL,
  `task_description` varchar(255) NOT NULL,
  `status` enum('PENDING','COMPLETED','MISSED') DEFAULT 'PENDING',
  `proof_photo_url` varchar(500) DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `produits`
--

CREATE TABLE `produits` (
  `idProduit` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `prix` float NOT NULL,
  `quantiteStock` int(11) NOT NULL,
  `categorie` varchar(100) DEFAULT NULL,
  `id_user` int(11) NOT NULL,
  `uniteMesure` enum('Kg','L','Piece') NOT NULL,
  `image` varchar(255) DEFAULT NULL,
  `remise` float DEFAULT 0,
  `typeRemise` varchar(20) DEFAULT NULL,
  `visible` tinyint(1) DEFAULT 1,
  `visible_admin` tinyint(1) DEFAULT 1,
  `materiel_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `produits`
--

INSERT INTO `produits` (`idProduit`, `nom`, `description`, `prix`, `quantiteStock`, `categorie`, `id_user`, `uniteMesure`, `image`, `remise`, `typeRemise`, `visible`, `visible_admin`, `materiel_id`) VALUES
(1, 'Tomates', 'Tomates fraîches cultivées localement', 2.5, 98, 'Légumes', 1, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(3, 'Maïs', 'Maïs jaune utilisé pour alimentation', 1.2, 198, 'Céréales', 1, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(4, 'Riz', 'Riz local de qualité supérieure', 4.5, 150, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(6, 'felfell', 'felfel 7arr', 6.5, 111, 'Légumes', 1, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(9, 'banane', 'banane maser', 15, 174, 'Céréales', 4, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(14, 'louz', 'louz jdid', 22, 22, 'Fruits', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(25, 'ma3dnous', 'pifsjlndfkjnsdfkbsdkfbsd', 1, 1000, 'Légumes', 2, 'Piece', 'C:\\Users\\mejsa\\ArdhiImages\\9ce8ba12-fa56-4ebc-bae0-450f292e3fce_CI1E8kcWsAAbN4r.jpg', 0, NULL, 1, 1, NULL),
(46, 'USD', 'USD', 1, 100000000, 'Produits laitiers', 2, 'Piece', NULL, 0, NULL, 0, 1, NULL),
(47, 'tomates', 'tomates  bio 100%', 2.5, 100, 'Légumes', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(65, 'pomme', '', 100, 1215, 'Fruits', 4, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(66, 'Riz blanc', 'Riz blanc naturel', 1.5, 400, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(67, 'Maïs grain', 'Maïs sec', 1.4, 350, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(68, 'Blé dur', 'Blé pour farine', 1.3, 300, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(69, 'Mil', 'Mil local', 1.6, 280, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(70, 'Avoine', 'Avoine grain', 1.9, 199, 'Céréales', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(71, 'Pomme', 'Pomme rouge fraiche', 3, 150, 'Fruits', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(72, 'Banane', 'Banane locale', 2.5, 174, 'Fruits', 2, 'Kg', NULL, 20, 'POURCENTAGE', 1, 1, NULL),
(73, 'Orange', 'Orange juteuse', 2.8, 170, 'Fruits', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(74, 'Mangue', 'Mangue sucrée', 3.5, 120, 'Fruits', 2, 'Piece', NULL, 0, NULL, 1, 1, NULL),
(75, 'Ananas', 'Ananas frais', 3.7, 90, 'Fruits', 2, 'Piece', NULL, 1, 'MONTANT_FIXE', 1, 1, NULL),
(76, 'Tomate', 'Tomate fraiche', 2.4, 220, 'Légumes', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(77, 'Oignon', 'Oignon blanc', 1.5, 250, 'Légumes', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(78, 'Carotte', 'Carotte croquante', 1.7, 180, 'Légumes', 2, 'Kg', NULL, 0, NULL, 1, 1, NULL),
(79, 'Chou vert', 'Chou vert frais', 2, 120, 'Légumes', 2, 'Piece', NULL, 0, NULL, 1, 1, NULL),
(80, 'Lait frais', 'Lait de vache pasteurisé', 1.2, 8, 'Produits laitiers', 2, 'L', NULL, 0, NULL, 1, 1, NULL),
(81, 'Yaourt nature', 'Yaourt frais naturel', 0.8, 249, 'Produits laitiers', 2, 'Piece', NULL, 0, NULL, 1, 1, NULL),
(82, 'Fromage blanc', 'Fromage blanc crémeux', 2.5, 119, 'Produits laitiers', 2, 'Kg', NULL, 76.98, 'POURCENTAGE', 1, 1, NULL),
(83, 'Beurre naturel', 'Beurre doux', 2.8, 7, 'Produits laitiers', 2, 'Kg', NULL, 20.1, 'POURCENTAGE', 1, 1, NULL),
(93, 'pfjkl', 'ojdfkl,', 88, 77, 'Fruits', 13, 'Kg', NULL, 10, 'POURCENTAGE', 1, 1, NULL),
(94, 'test', 'test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test ', 66, 11, 'Légumes', 2, 'Kg', 'imageee-69d3a3c6c6dea.jpg', 60, 'FIXE', 1, 1, NULL),
(95, 'dkfdkls', 'dmksdlkf,', 99, 8, 'Fruits', 2, 'Kg', NULL, 20, 'POURCENTAGE', 1, 1, NULL),
(96, 'Beurre naturel', 'midsjflgijsdlij,', 22, 221, 'test', 2, 'Kg', 'tomate-69d50b0eb53c1.png', 20, 'FIXE', 1, 0, NULL),
(97, 'Lait cru de vache', 'Découvrez notre lait cru de vache, issu d\'une production locale et sélectionné avec soin pour garantir une qualité premium. Profitez de ses saveurs authentiques et de ses bienfaits nutritionnels, avec une traçabilité totale de notre élevage à votre assiet', 1.35, 0, 'Produits laitiers', 13, 'L', 'lait-69dabf2eca213.jpg', 0, NULL, 1, 1, NULL),
(98, 'Pommes', 'Découvrez nos pommes fraîches et croquantes, sélectionnées avec soin pour leur saveur exceptionnelle et leur qualité premium. Originaires de nos vergers locaux, ces pommes sont parfaites pour une consommation directe ou pour ajouter une touche de fraîcheu', 10, 0, 'Fruits', 13, 'Kg', 'images-69dabf55ab539.jpg', 0, NULL, 1, 1, NULL),
(99, 'Banane', 'Découvrez nos bananes fraîches, sélectionnées avec soin pour leur saveur sucrée et leur texture crémeuse. Origine locale, qualité premium pour un goût incomparable.', 11, 498, 'Fruits', 23, 'Piece', NULL, 10, 'FIXE', 1, 1, NULL),
(100, 'Pistaches', 'Découvrez nos pistaches fraîches et savoureuses, sélectionnées avec soin pour vous offrir une expérience gustative exceptionnelle. Originaires de nos champs locaux, ces fruits secs sont riches en nutriments et parfaits pour une collation saine. Profitez d', 55, 549, 'Fruits', 23, 'Kg', 'imageee-69dac081b11d2.jpg', 0, NULL, 1, 1, NULL),
(101, 'Banane', 'Découvrez nos bananes fraîches et savoureuses, sélectionnées avec soin pour leur qualité premium. Originaires de producteurs locaux, ces bananes sont parfaites pour une pause gourmande ou un ajout délicieux à vos smoothies et desserts. Profitez de leur ri', 55, 552, 'Fruits', 13, 'Kg', 'bana-69e00c5194efe.jpg', 0, NULL, 0, 1, NULL),
(102, 'traktourrrrrrr', 'a7laa nes', 151555, 0, 'Outillage', 13, 'Piece', 'materiel-10-69e36782e6b00.webp', 0, NULL, 1, 1, 10),
(103, 'semoir', 'ojvfipjviolfdjsiolj', 1500, 0, 'Outillage', 13, 'Piece', 'materiel-11-69e36b187cb1c.png', 0, NULL, 1, 1, 11),
(106, 'camion', 'h,jhj,,jhj,h,jhj,h15000', 150000, 0, 'Autre', 13, 'Piece', NULL, 0, NULL, 1, 1, 12);

-- --------------------------------------------------------

--
-- Structure de la table `reclamation`
--

CREATE TABLE `reclamation` (
  `idReclamation` int(11) NOT NULL,
  `description` text NOT NULL,
  `type` varchar(50) NOT NULL,
  `statut` varchar(50) DEFAULT 'EN_ATTENTE',
  `date_reclamation` datetime DEFAULT current_timestamp(),
  `nom_produit` varchar(100) DEFAULT NULL,
  `id_produit` int(11) NOT NULL,
  `id_user` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reclamation`
--

INSERT INTO `reclamation` (`idReclamation`, `description`, `type`, `statut`, `date_reclamation`, `nom_produit`, `id_produit`, `id_user`) VALUES
(1, 'ljlfndkfjdsnkfs', 'PRODUIT_AVARIE', 'EN_ATTENTE', '2026-01-31 12:48:59', 'Pommes', 2, 1),
(5, 'kngdflngdfln', 'PRODUIT_AVARIE', 'EN_ATTENTE', '2026-01-31 18:43:37', 'Lait frais', 5, 2),
(6, 'kfkldnflsdnfsdl', 'QUANTITE_INCORRECTE', 'EN_ATTENTE', '2026-01-31 18:44:09', 'Maïs', 3, 2),
(7, 'fama hajaa testt ijgndfjkgndfjgnsngjlnfama hajaa testt ijgndfjkgndfjgnsngjlnfama hajaa testt ijgndfjkgndfjgnsngjlnfama hajaa testt ijgndfjkgndfjgnsngjlnfama hajaa testt ijgndfjkgndfjgnsngjlnfama hajaa testt ijgndfjkgndfjgnsngjln', 'PRIX_NON_CONFORME', 'EN_ATTENTE', '2026-01-31 19:05:29', 'Riz', 4, 2),
(8, 'mkkfsklfndslfnsdlfnsd', 'RETARD_LIVRAISON', 'EN_COURS', '2026-01-31 19:14:47', 'Maïs', 3, 2),
(9, 'rim rim', 'QUANTITE_INCORRECTE', 'RESOLUE', '2026-02-02 15:58:20', 'tofe7', 2, 2),
(10, 'jlnfksjndfkjdnfjksdf', 'PRIX_NON_CONFORME', 'EN_ATTENTE', '2026-02-07 10:35:29', 'banane', 9, 2),
(11, 'ôjgpdifjgodfjgidjfksaif ijareb', 'PRIX_NON_CONFORME', 'EN_ATTENTE', '2026-02-08 20:01:09', 'felfell', 6, 4),
(12, 'fomsdmfsd,lkfjnds', 'PRIX_NON_CONFORME', 'RESOLUE', '2026-02-08 21:26:14', 'banane', 9, 2),
(15, ':klj:knk:n;,', 'PRODUIT_AVARIE', 'REJETEE', '2026-02-16 13:22:50', 'felfell', 6, 3),
(18, 'lngfjngsnfkfgjs', 'PRIX_NON_CONFORME', 'EN_ATTENTE', '2026-02-27 22:50:36', 'appel', 63, 2),
(19, 'SUJET : PGJFDLJGDFJGKLDFNL\n\ndkjnjnfksndjkfndskjnfkjsndfjksdnjlfnsjldnfkjsdnfdjkzjnjfnsdjkn', 'AUTRE', 'REJETEE', '2026-04-06 11:15:31', 'pomme', 65, 2),
(20, 'kfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdbkfjdslfdslkfjklsdnfjoldshfjkjbdshkfbhjsdb', 'PRODUIT_NON_CONFORME', 'EN_COURS', '2026-04-06 11:15:58', 'banane', 9, 2),
(21, 'amknfdlff,dslkfnsdlnfsdklfsd', 'QUANTITE_INCORRECTE', 'EN_ATTENTE', '2026-04-07 01:10:15', 'pomme', 65, 2),
(22, 'SUJET : PIJGFLJNF\n\nsdfsdmokfmsd', 'QUALITE_RECOLTE', 'REJETEE', '2026-04-07 15:54:05', 'Beurre naturel', 96, 23),
(23, 'SUJET : OGDKFKMG,DF\n\nol,gml,dmg,kfdslkg', 'RETARD_LIVRAISON', 'EN_ATTENTE', '2026-04-16 00:20:20', 'Banane', 101, 2);

-- --------------------------------------------------------

--
-- Structure de la table `reclamation_materiel`
--

CREATE TABLE `reclamation_materiel` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `materiel_id` int(11) DEFAULT NULL,
  `sujet` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `urgence` varchar(20) NOT NULL DEFAULT 'normale' COMMENT 'normale ou urgente',
  `statut` varchar(30) NOT NULL DEFAULT 'en_attente' COMMENT 'en_attente, en_cours, resolue',
  `commentaire_admin` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL COMMENT '(DC2Type:datetime_immutable)',
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `reclamation_materiel`
--

INSERT INTO `reclamation_materiel` (`id`, `user_id`, `materiel_id`, `sujet`, `description`, `urgence`, `statut`, `commentaire_admin`, `created_at`, `updated_at`) VALUES
(4, 13, 6, 'Retard de maintenance - semoir', 'gjljfjnjgjkdfngnjlfdngjkdngjkdn', 'normale', 'en_attente', NULL, '2026-04-19 16:12:12', NULL),
(5, 13, 12, 'Retard de maintenance - camion', 'ofjdsjfsdfsdfsfsd', 'normale', 'en_attente', NULL, '2026-04-19 17:18:26', NULL),
(6, 13, 13, 'Retard de maintenance - traktourR', 'mon materiel est en retard', 'urgente', 'en_cours', 'jnkjnkjnkjn', '2026-04-20 15:12:24', '2026-04-20 15:45:13'),
(7, 13, 20, 'Retard de maintenance - trakteeur test', 'yfgrfyugoagfo', 'urgente', 'en_cours', 'gjmtgzueog', '2026-04-21 17:15:17', '2026-04-21 17:15:46');

-- --------------------------------------------------------

--
-- Structure de la table `review`
--

CREATE TABLE `review` (
  `id` int(11) NOT NULL,
  `diagnostic_id` int(11) NOT NULL,
  `treatment_plan_id` int(11) DEFAULT NULL,
  `expert_id` int(11) DEFAULT NULL,
  `review_type` enum('DIAGNOSIS','PROGRESS','PREVENTION') NOT NULL,
  `status` enum('PENDING','IN_PROGRESS','COMPLETED') DEFAULT 'PENDING',
  `photo_url` varchar(500) DEFAULT NULL,
  `ai_analysis` text DEFAULT NULL,
  `expert_notes` text DEFAULT NULL,
  `expert_verdict` enum('CONTINUE','HEALED','WORSENED') DEFAULT NULL,
  `expert_disease_name` varchar(255) DEFAULT NULL,
  `farmer_response` enum('ACCEPTED','REJECTED','ACKNOWLEDGED') DEFAULT NULL,
  `ai_proposed_plan` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `prevention_plan_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `review`
--

INSERT INTO `review` (`id`, `diagnostic_id`, `treatment_plan_id`, `expert_id`, `review_type`, `status`, `photo_url`, `ai_analysis`, `expert_notes`, `expert_verdict`, `expert_disease_name`, `farmer_response`, `ai_proposed_plan`, `created_at`, `updated_at`, `prevention_plan_id`) VALUES
(1, 4, NULL, NULL, 'DIAGNOSIS', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-16 09:24:45', '2026-02-16 09:24:45', NULL),
(2, 9, 1, 13, 'PROGRESS', 'COMPLETED', 'https://i.ibb.co/tp1HzCMC/ae9d24244268.jpg', NULL, 'lll', 'HEALED', NULL, NULL, NULL, '2026-03-01 22:19:22', '2026-04-19 13:07:08', NULL),
(3, 10, NULL, NULL, 'DIAGNOSIS', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-02 09:15:49', '2026-03-02 09:15:49', NULL),
(4, 12, NULL, NULL, 'DIAGNOSIS', 'PENDING', 'https://i.ibb.co/1JwxQw5C/c8d3d290db7e.png', 'Tomat - Bactériose', NULL, NULL, NULL, NULL, NULL, '2026-04-19 12:49:27', '2026-04-19 12:49:27', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `review_comments`
--

CREATE TABLE `review_comments` (
  `id` int(11) NOT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `participation_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `parent_comment_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `review_comments`
--

INSERT INTO `review_comments` (`id`, `content`, `created_at`, `participation_id`, `user_id`, `parent_comment_id`) VALUES
(1, 'fk', '2026-04-19 14:20:48', 356, 13, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `roi_analyses`
--

CREATE TABLE `roi_analyses` (
  `id` int(11) NOT NULL,
  `culture` varchar(100) NOT NULL,
  `roi` decimal(10,2) NOT NULL,
  `marge` decimal(10,2) NOT NULL,
  `revenu` decimal(10,2) NOT NULL,
  `cout_total` decimal(10,2) NOT NULL,
  `niveau` varchar(50) NOT NULL,
  `risque` varchar(50) NOT NULL,
  `conseils` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`conseils`)),
  `alternative` varchar(200) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `parcelle_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `tache`
--

CREATE TABLE `tache` (
  `id_tache` int(11) NOT NULL,
  `titre` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `statut` varchar(50) DEFAULT 'En attente',
  `date_debut` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `id_employe` int(11) DEFAULT NULL,
  `id_agriculteur` int(11) DEFAULT NULL,
  `priorite` int(11) DEFAULT NULL COMMENT '1=Basse, 2=Moyenne, 3=Haute, 4=Critique',
  `categorie` varchar(100) DEFAULT NULL COMMENT 'Plantation, Récolte, Irrigation, etc.',
  `type_tache` varchar(50) DEFAULT 'AUTRE',
  `google_calendar_event_id` varchar(255) DEFAULT NULL,
  `date_modification` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `google_event_id` varchar(255) DEFAULT NULL,
  `budget_prevu` decimal(10,3) DEFAULT NULL COMMENT 'Budget prévisionnel en TND',
  `cout_materiel` decimal(10,3) DEFAULT 0.000 COMMENT 'Coût des matériaux/consommables en TND'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `tache`
--

INSERT INTO `tache` (`id_tache`, `titre`, `description`, `statut`, `date_debut`, `date_fin`, `id_employe`, `id_agriculteur`, `priorite`, `categorie`, `type_tache`, `google_calendar_event_id`, `date_modification`, `google_event_id`, `budget_prevu`, `cout_materiel`) VALUES
(56, 'Plantation oliviers parcelle Nord', 'Planter 200 oliviers variété Chemlali. Espacement 6m x 6m.', 'En cours', '2026-04-01', '2026-04-15', NULL, 13, 3, 'Plantation', 'AUTRE', NULL, '2026-04-07 14:19:21', NULL, NULL, 0.000),
(57, 'Analyse du sol zone A', 'Prélever des échantillons sur 5 points. Envoyer au laboratoire.', 'Validé', '2026-04-10', '2026-04-12', 45, 2, 2, 'Fertilisation', 'AUTRE', NULL, '2026-04-21 13:33:19', NULL, NULL, 0.000),
(58, 'Plan de fertilisation printanier', 'Établir le calendrier de fertilisation pour les cultures de printemps.', 'En cours', '2026-03-20', '2026-04-05', 47, 2, 3, 'Fertilisation', 'AUTRE', NULL, '2026-04-21 13:30:40', NULL, 1200.000, 150.000),
(59, 'Diagnostic ravageurs verger Est', 'Inspecter les 3 hectares du verger Est. Identifier les ravageurs.', 'Validé', '2026-04-08', '2026-04-09', 48, 2, 4, 'Autre', 'AUTRE', NULL, '2026-04-21 13:33:02', NULL, NULL, 0.000),
(60, 'Récolte tomates serre 2', 'Récolter les tomates mûres. Trier et stocker en chambre froide.', 'Validé', '2026-04-03', '2026-04-07', 44, 2, 3, 'Récolte', 'AUTRE', NULL, '2026-04-21 13:31:34', NULL, 800.000, 50.000),
(62, 'Supervision équipe récolte semaine 15', 'Coordonner les 6 ouvriers. Contrôler la qualité. Rapport de rendement.', 'Validé', '2026-04-07', '2026-04-11', 46, 2, 3, 'Récolte', 'AUTRE', NULL, '2026-04-21 13:32:09', NULL, NULL, 0.000),
(64, 'Installation irrigation goutte-à-goutte', 'Installer 500 mètres de tuyaux. Connecter au système de pompage.', 'Validé', '2026-04-01', '2026-04-20', 42, 2, 4, 'Irrigation', 'AUTRE', NULL, '2026-04-21 13:31:13', NULL, 3500.000, 2200.000),
(65, 'Maintenance pompe principale station Nord', 'Vidange et remplacement du filtre. Vérifier joints et raccords.', 'Validé', '2026-04-09', '2026-04-20', 48, 2, 4, 'Maintenance', 'AUTRE', NULL, '2026-04-21 13:34:17', NULL, 400.000, 180.000),
(66, 'Révision tracteur Massey Ferguson', 'Vidange moteur, filtre à huile, filtre à air. Vérifier niveaux.', 'Validé', '2026-04-05', '2026-04-06', 50, 2, 3, 'Maintenance', 'AUTRE', NULL, '2026-04-21 13:31:48', NULL, 300.000, 120.000),
(68, 'Coordination livraison coopérative Sfax', 'Organiser transport 2 tonnes de dattes. Préparer bons de livraison.', 'En cours', '2026-04-06', '2026-04-08', 49, 2, 3, 'Récolte', 'AUTRE', NULL, '2026-04-21 13:31:57', NULL, 600.000, 0.000),
(69, 'Contrôle qualité huile olive lot 12', 'Analyser acidité, polyphénols, couleur. Valider avant mise en bouteille.', 'En cours', '2026-04-10', '2026-04-10', 46, 2, 4, 'Récolte', 'AUTRE', NULL, '2026-04-21 13:33:31', NULL, 250.000, 80.000),
(70, 'Formation sécurité au travail', 'Session formation sécurité. Présence obligatoire. Durée demi-journée.', 'Validé', '2026-03-25', '2026-03-25', 47, 2, 1, 'Administratif', 'AUTRE', NULL, '2026-04-21 13:30:56', NULL, NULL, 0.000),
(71, 'test', 'tech', 'En attente', '2002-04-08', '2026-04-06', 47, 2, 2, 'Administratif', 'AUTRE', NULL, '2026-04-07 15:12:45', NULL, NULL, 0.000);

-- --------------------------------------------------------

--
-- Structure de la table `tache_competence_requise`
--

CREATE TABLE `tache_competence_requise` (
  `id` int(11) NOT NULL,
  `id_tache` int(11) NOT NULL,
  `id_competence` int(11) NOT NULL,
  `niveau_requis` int(11) NOT NULL DEFAULT 2,
  `importance` int(11) DEFAULT 5,
  `date_ajout` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `traitement`
--

CREATE TABLE `traitement` (
  `id` int(11) NOT NULL,
  `diagnostic_id` int(11) NOT NULL,
  `solution_nom` varchar(255) NOT NULL,
  `description_detaillee` text DEFAULT NULL,
  `type_traitement` enum('FONGICIDE','HERBICIDE','INSECTICIDE','BACTERICIDE','NEMATICIDE','VIRUCIDE','NUTRIMENT','REGULATEUR_CROISSANCE','AUTRE') DEFAULT 'AUTRE',
  `duree_recommandee` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `traitement`
--

INSERT INTO `traitement` (`id`, `diagnostic_id`, `solution_nom`, `description_detaillee`, `type_traitement`, `duree_recommandee`) VALUES
(1, 3, '-', '-', 'NUTRIMENT', NULL),
(3, 6, 'DIFENOCONAZOLE (SOMOVA)', '2-3 traitements à 0,5-1L/ha, début floraison, prélevée et réitéré 2-3 fois avec un intervalle de 10-14 jours, évitez les heures chaudes', 'FONGICIDE', NULL),
(4, 7, 'FOLIOREL', 'APPLICATION : 1L/100L EAU, 2-3 APPLICATIONS, INTERVALLE 7 JOURS, ÉVITER LE VENT', 'FONGICIDE', NULL),
(5, 8, 'SHIELD 300 SC (produit fongicide disponible en Tunisie)', 'Appliquer  SHIELD 300 SC à raison de  100-150 ml/100L d\'eau,  en prévention,  à partir de la floraison et répéter toutes les 10-15 jours', 'FONGICIDE', NULL),
(6, 9, 'NON_APPLICABLE', 'AUCUN_TRAITEMENT_NECESSAIRE', 'AUTRE', NULL),
(7, 10, 'Dithane M 45', 'Appliquer 2 à 3 traitements à raison de 200g/100L d\'eau tous les 7 à 10 jours, commencer au stade début floraison. Éviter les traitements en période de forte chaleur ou de vent.', 'FONGICIDE', NULL),
(8, 11, 'DITHANE M45', '2g/L eau, 2 applications espacées de 7-10 jours, évitez les heures ensoleillées', 'FONGICIDE', NULL),
(9, 12, 'CUPROXAT', '1L/100L eau à la pré-levée et 1L/100L eau tous les 10 jours', 'FONGICIDE', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `treatment_plan`
--

CREATE TABLE `treatment_plan` (
  `id` int(11) NOT NULL,
  `diagnostic_id` int(11) NOT NULL,
  `start_date` datetime DEFAULT NULL,
  `status` enum('ACTIVE','COMPLETED','ABANDONED') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `treatment_plan`
--

INSERT INTO `treatment_plan` (`id`, `diagnostic_id`, `start_date`, `status`) VALUES
(1, 9, '2026-03-01 23:17:20', 'COMPLETED'),
(2, 10, '2026-03-02 10:15:52', 'ACTIVE'),
(3, 12, '2026-04-19 13:49:32', 'ACTIVE');

-- --------------------------------------------------------

--
-- Structure de la table `treatment_task`
--

CREATE TABLE `treatment_task` (
  `id` int(11) NOT NULL,
  `treatment_plan_id` int(11) NOT NULL,
  `day_offset` int(11) NOT NULL,
  `task_description` varchar(255) NOT NULL,
  `status` enum('PENDING','COMPLETED','MISSED') DEFAULT 'PENDING',
  `tech_x` double DEFAULT 0,
  `tech_y` double DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `treatment_task`
--

INSERT INTO `treatment_task` (`id`, `treatment_plan_id`, `day_offset`, `task_description`, `status`, `tech_x`, `tech_y`) VALUES
(1, 1, 1, 'Isoler la plante pour éviter la propagation de la maladie', 'COMPLETED', 0, 0),
(2, 1, 2, 'Appliquer un traitement préventif à base de cuivre pour renforcer la résistance de la plante', 'COMPLETED', 0, 0),
(3, 1, 3, 'Fertiliser la plante avec un engrais équilibré pour favoriser sa croissance et son développement', 'COMPLETED', 0, 0),
(4, 1, 4, 'Effectuer un arrosage profond mais éviter l\'excès d\'eau pour ne pas favoriser les maladies', 'COMPLETED', 0, 0),
(5, 1, 5, 'Inspecter les feuilles et les tiges pour détecter les premiers signes de maladie et les psi nécessairezzz', 'PENDING', 0, 0),
(7, 1, 7, 'Appliquer un traitement à base de soufre pour prévenir les maladies fongiques', 'PENDING', 0, 0),
(8, 1, 8, 'Vérifier l\'état général de la plante et enlever toute partie morte ou endommagée', 'PENDING', 0, 0),
(9, 1, 9, 'Réappliquer un traitement fongicide ciblé si des signes de maladie persistent', 'PENDING', 0, 0),
(10, 1, 10, 'Effectuer un dernier contrôle pour s\'assurer que la plante est en bonne santé et prête pour une nouvelle période de croissance', 'PENDING', 0, 0),
(11, 2, 1, 'Isoler la plante et couper les feuilles infectées pour prévenir la propagation de la maladie', 'PENDING', 0, 0),
(12, 2, 2, 'Nettoyer les outils et les zones environnantes pour éviter la contamination', 'PENDING', 0, 0),
(13, 2, 3, 'Appliquer un traitement fongicide ciblé contre Alternaria solani sur les parties saines de la plante', 'PENDING', 0, 0),
(14, 2, 4, 'Augmenter la ventilation et la circulation de l\'air autour de la plante pour réduire l\'humidité', 'PENDING', 0, 0),
(15, 2, 5, 'Réduire l\'arrosage et éviter de mouiller les feuilles pour empêcher le développement de la maladie', 'PENDING', 0, 0),
(16, 2, 6, 'Appliquer un engrais équilibré pour renforcer la santé générale de la plante et ses défenses naturelles', 'PENDING', 0, 0),
(17, 2, 7, 'Vérifier l\'état visuel de la plante pour détecter toute apparition de nouveaux symptômes', 'PENDING', 0, 0),
(19, 2, 9, 'Surveiller les conditions météorologiques et prendre des mesures pour protéger la plante des intempéries', 'PENDING', 0, 0),
(21, 3, 1, 'Isoler la plante et couper les feuilles infectées pour éviter la propagation de la bactériose.', 'PENDING', 0, 0),
(22, 3, 2, 'Appliquer un traitement antibactérien ciblé sur la plante pour lutter contre la bactériose.', 'PENDING', 0, 0),
(23, 3, 3, 'Augmenter la circulation d\'air autour des plantes pour réduire l\'humidité et prévenir la propagation de la maladie.', 'PENDING', 0, 0),
(24, 3, 4, 'Fertiliser la plante avec un engrais équilibré pour renforcer son système immunitaire et favoriser la croissance.', 'PENDING', 0, 0),
(25, 3, 5, 'Surveiller les niveaux d\'eau et ajuster l\'arrosage pour éviter l\'excès d\'humidité qui peut favoriser la bactériose.', 'PENDING', 0, 0),
(26, 3, 6, 'Appliquer un traitement à base de cuivre pour renforcer la résistance de la plante aux maladies bactériennes.', 'PENDING', 0, 0),
(27, 3, 7, 'Vérifier l\'état visuel de la plante pour détecter tout signe de propagation ou d\'amélioration de la bactériose.', 'PENDING', 0, 0),
(28, 3, 8, 'Répéter l\'application du traitement antibactérien ciblé pour consolider les effets et prévenir tout rebond de la maladie.', 'PENDING', 0, 0),
(29, 3, 9, 'Entretenir le sol en ajoutant des micro-organismes bénéfiques pour améliorer la santé globale de la plante et du sol.', 'PENDING', 0, 0),
(30, 3, 10, 'Rééquilibrer le pH du sol si nécessaire pour créer un environnement défavorable à la bactériose et favoriser la bonne santé de la plante.', 'PENDING', 0, 0);

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `id` int(11) NOT NULL,
  `email` varchar(180) NOT NULL,
  `role` enum('ADMIN','AGRICULTEUR','CLIENT','AGRONOME') NOT NULL DEFAULT 'AGRICULTEUR',
  `password` varchar(255) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) NOT NULL,
  `points` int(11) DEFAULT 0,
  `level` int(11) DEFAULT 1,
  `two_factor_enabled` tinyint(1) DEFAULT 0,
  `two_factor_code` varchar(10) DEFAULT NULL,
  `two_factor_expires_at` datetime DEFAULT NULL,
  `fingerprint_signature` mediumtext DEFAULT NULL,
  `points_fidelite` double DEFAULT 0,
  `face_signature` longtext DEFAULT NULL,
  `reset_password_code` varchar(10) DEFAULT NULL,
  `reset_password_expires_at` datetime DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `google_access_token` longtext DEFAULT NULL,
  `google_refresh_token` longtext DEFAULT NULL,
  `is_moderator` tinyint(1) NOT NULL DEFAULT 0,
  `muted_until` datetime DEFAULT NULL,
  `is_banned` tinyint(1) NOT NULL DEFAULT 0,
  `avatar` varchar(255) DEFAULT NULL,
  `bio` longtext DEFAULT NULL,
  `banner` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`id`, `email`, `role`, `password`, `nom`, `prenom`, `points`, `level`, `two_factor_enabled`, `two_factor_code`, `two_factor_expires_at`, `fingerprint_signature`, `points_fidelite`, `face_signature`, `reset_password_code`, `reset_password_expires_at`, `phone`, `location`, `google_access_token`, `google_refresh_token`, `is_moderator`, `muted_until`, `is_banned`, `avatar`, `bio`, `banner`) VALUES
(1, 'saifadmin@gmail.com', 'ADMIN', '$2y$13$3RGnQuGOS.HbJghWTaACauEv.945DQx1aavVF5t/Xk16myZHH8chy', 'saif', 'admin', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(2, 'saifuser@gmail.com', 'AGRICULTEUR', '$2y$13$.uXow5WxnO4QX5abQuq/xOf6WuexxegvKz1D4D1XH2E5QJf1P/Vri', 'saif', 'USER', 235, 1, 0, NULL, NULL, NULL, 15201.394999779, NULL, NULL, NULL, NULL, 'ben arous', 'fake_access_token', 'fake_refresh_token', 1, '2026-04-20 14:02:49', 0, NULL, NULL, NULL),
(3, 'saif@gmail.com', 'CLIENT', '$2y$13$1A40D3jAnzcf.0PcEyded.d4B4HxQMfXwGOT3.mM65ASP23huLl8.', 'saif', 'saif', 0, 1, 0, NULL, NULL, NULL, 210.825, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(4, 'sieeef@gmail.com', 'AGRICULTEUR', '$2y$13$lXyDYJnY5.6Qf.3Uyh9ixuIOq5fh8Q4Wm3n2pU1yVwHVjEGJkNHcC', 'saif', 'agri', 0, 1, 0, NULL, NULL, NULL, 999965.2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(6, 'hajsalemadel10@gmail.com', 'AGRICULTEUR', '$2a$10$tDbs8jSSpNLvt/RtMJ5/7erpqV1Y3VhpWATDNXojtitN/M0hqcB3q', 'adel ', 'hajsalem', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(7, 'khaled.guedria@esprit.tn', 'CLIENT', '$2a$10$KjcjzwmIPQx2GxXf1b9iUOFyPoLXFzG6oYhBO2xBBCCs8CQOJ5zl2', 'Khaled', 'Guedria', 0, 1, 1, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(9, 'khaled.guedria@esprit2.tn', 'CLIENT', '$2y$13$CfsS4bDP5OeZ9Hy4VkkcLOoEkDvY02YZedDxmjte.56MTqNq9I5tG', 'Khaled', 'Guedria', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(12, 'a@gmail.com', 'CLIENT', '$2a$10$a64BFViv4SFAzz.CePLaA.f8tKZL8Rv3Uz.y8RF/O8ZZfu9x.3m3W', 'saif', 'saif', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL);
INSERT INTO `user` (`id`, `email`, `role`, `password`, `nom`, `prenom`, `points`, `level`, `two_factor_enabled`, `two_factor_code`, `two_factor_expires_at`, `fingerprint_signature`, `points_fidelite`, `face_signature`, `reset_password_code`, `reset_password_expires_at`, `phone`, `location`, `google_access_token`, `google_refresh_token`, `is_moderator`, `muted_until`, `is_banned`, `avatar`, `bio`, `banner`) VALUES
(13, 'mejsaif@gmail.com', 'AGRICULTEUR', '$2y$13$5Ppy1dYZkeOIXVYJlVZH8.GbImmcSjIvTaoyxaWtRkjy6vLD4xiwe', 'saif', 'mej', 440, 1, 0, NULL, NULL, NULL, 48.1879999619, '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', NULL, NULL, '+21690722197', 'tunis', 'fake_access_token', 'fake_refresh_token', 0, NULL, 0, 'https://i.ibb.co/SwdYH65k/a4047e557c86.jpg', NULL, 'https://i.ibb.co/mVvtS5Xs/96dd3601e3bd.png');
INSERT INTO `user` (`id`, `email`, `role`, `password`, `nom`, `prenom`, `points`, `level`, `two_factor_enabled`, `two_factor_code`, `two_factor_expires_at`, `fingerprint_signature`, `points_fidelite`, `face_signature`, `reset_password_code`, `reset_password_expires_at`, `phone`, `location`, `google_access_token`, `google_refresh_token`, `is_moderator`, `muted_until`, `is_banned`, `avatar`, `bio`, `banner`) VALUES
(14, 'yassminerahmouni44@gmail.com', 'CLIENT', '$2y$13$UNGUH9m2tU0kaTFJU0IYMuJBQgsxAXcK9eTWB11DAqf72tupvMG2O', 'Urgent', 'Participant', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, '20123456', 'Tunis', NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(15, 'yassminerahmouni9@gmail.com', 'CLIENT', '12345678', 'Important', 'Participant', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, '20234567', 'Ariana', NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(16, 'yassminerahmouni051@gmail.com', 'CLIENT', '12345678', 'Modere', 'Participant', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, '20345678', 'Sousse', NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(22, 'test2@test.com', 'AGRONOME', '$2y$13$wEERxtEMf15N7ixQx/7Ub.Iu.upHUezIHq/sWJblHrC.ait7skaBu', 'Test', 'User', 0, 1, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL),
(23, 'saifeddine.mejri4938@istic.ucar.tn', 'AGRICULTEUR', '$2y$13$ykfy9/X4EF6toQMcrq2Z4uzjc7hoMzLqSjb.PReXhc0YURwfeoYee', 'saif', 'istic', 0, 1, 0, NULL, NULL, NULL, 15156.58, NULL, NULL, NULL, '12345678', 'ben arous', 'fake_access_token', 'fake_refresh_token', 0, NULL, 0, NULL, NULL, NULL),
(24, 'hadjsalemadel@gmail.com', 'AGRONOME', '$2y$13$YpAlKEEJYvDP0HiS.xxTGO3JoEGAZwwWVHAtgmtNgA0XycAV/yKoq', 'Adel', 'HJSS', 0, 1, 1, '6450', '2026-04-18 13:40:26', NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 0, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `user_badge`
--

CREATE TABLE `user_badge` (
  `user_id` int(11) NOT NULL,
  `badge_id` int(11) NOT NULL,
  `acquired_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_badge`
--

INSERT INTO `user_badge` (`user_id`, `badge_id`, `acquired_at`) VALUES
(2, 1, '2026-02-13 18:23:06'),
(2, 4, '2026-02-16 09:24:38'),
(13, 1, '2026-03-01 22:15:14'),
(13, 4, '2026-03-01 22:16:21'),
(13, 7, '2026-03-01 22:22:40');

-- --------------------------------------------------------

--
-- Structure de la table `user_blocks`
--

CREATE TABLE `user_blocks` (
  `user_source` int(11) NOT NULL,
  `user_target` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Structure de la table `vulnerability`
--

CREATE TABLE `vulnerability` (
  `id` int(11) NOT NULL,
  `report_id` int(11) NOT NULL,
  `type` enum('PEST_OUTBREAK_RISK','DISEASE_RISK','NUTRIENT_DEFICIENCY','LOW_POLLINATION','SOIL_DEGRADATION') NOT NULL,
  `severity` enum('CRITICAL','MEDIUM','LOW') NOT NULL,
  `threat` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `risk_score` float DEFAULT NULL,
  `timeframe_days` int(11) DEFAULT NULL,
  `estimated_yield_loss_percent` int(11) DEFAULT NULL,
  `estimated_cost_if_occurs` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `vulnerability`
--

INSERT INTO `vulnerability` (`id`, `report_id`, `type`, `severity`, `threat`, `description`, `risk_score`, `timeframe_days`, `estimated_yield_loss_percent`, `estimated_cost_if_occurs`) VALUES
(1, 1, 'DISEASE_RISK', 'MEDIUM', 'MENACE DE MALADIE', 'Les signes de maladie sont souvent visibles sur les feuilles, les tiges ou les fruits. Cependant, sans image appropriée des olives, il est difficile de préciser.', 0.65, 21, 15, 750),
(3, 1, 'NUTRIENT_DEFICIENCY', 'LOW', 'CARENCE NUTRITIVE', 'Les carences nutritives peuvent affecter la croissance mais l\'image ne fournit pas d\'information.', 0.2, 45, 5, 200),
(4, 1, 'LOW_POLLINATION', 'LOW', 'PROBLÈME DE POLLINISATION', 'Pas d\'info directe.', 0.2, 45, 5, 200),
(5, 1, 'SOIL_DEGRADATION', 'LOW', 'DÉGRADATION DU SOL', 'Pas visible sur l\'image.', 0.2, 59, 5, 200);

-- --------------------------------------------------------

--
-- Structure de la table `wishlist`
--

CREATE TABLE `wishlist` (
  `idWishlist` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `id_produit` int(11) NOT NULL,
  `dateAjout` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `wishlist`
--

INSERT INTO `wishlist` (`idWishlist`, `id_user`, `id_produit`, `dateAjout`) VALUES
(13, 2, 9, '2026-03-01 23:46:46'),
(14, 13, 75, '2026-03-02 09:20:42'),
(15, 13, 70, '2026-03-02 09:20:44'),
(17, 13, 72, '2026-03-02 11:05:43'),
(20, 2, 6, '2026-04-03 21:25:29'),
(21, 13, 94, '2026-04-06 14:17:03'),
(22, 13, 82, '2026-04-06 14:17:05'),
(23, 13, 81, '2026-04-06 14:17:10'),
(24, 2, 65, '2026-04-07 15:53:00'),
(25, 13, 80, '2026-04-11 23:02:20'),
(26, 13, 83, '2026-04-11 23:10:12'),
(27, 2, 100, '2026-04-13 23:30:07'),
(28, 13, 99, '2026-04-14 17:29:49'),
(29, 13, 95, '2026-04-16 00:11:46'),
(30, 23, 95, '2026-04-21 17:31:43');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `abonnement`
--
ALTER TABLE `abonnement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_user_abonnement` (`user_id`),
  ADD KEY `fk_abonnement_offre` (`offre_id`);

--
-- Index pour la table `ai_suggestions`
--
ALTER TABLE `ai_suggestions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_AI_PARCELLE` (`parcelle_id`);

--
-- Index pour la table `alerte_technicien`
--
ALTER TABLE `alerte_technicien`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_BE2DDF8Cid_materiel` (`id_materiel`),
  ADD KEY `IDX_BE2DDF8Cagriculteur_id` (`agriculteur_id`);

--
-- Index pour la table `avis`
--
ALTER TABLE `avis`
  ADD PRIMARY KEY (`idAvis`),
  ADD KEY `idx_avis_idProduit` (`id_produit`),
  ADD KEY `idx_avis_idUser` (`id_user`);

--
-- Index pour la table `badge`
--
ALTER TABLE `badge`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `chatbot_conversation`
--
ALTER TABLE `chatbot_conversation`
  ADD PRIMARY KEY (`id_conversation`),
  ADD KEY `idx_utilisateur` (`id_utilisateur`),
  ADD KEY `idx_intention` (`intention`),
  ADD KEY `idx_date` (`date_message`);

--
-- Index pour la table `commande`
--
ALTER TABLE `commande`
  ADD PRIMARY KEY (`idCommande`),
  ADD UNIQUE KEY `UNIQ_QR_TOKEN_COMMANDE` (`qr_code_token`),
  ADD KEY `idCoupon` (`id_coupon`),
  ADD KEY `idx_commande_idUser` (`id_user`);

--
-- Index pour la table `community_analytics_daily`
--
ALTER TABLE `community_analytics_daily`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_9F2CCBDE4B89032C` (`post_id`);

--
-- Index pour la table `community_comments`
--
ALTER TABLE `community_comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `post_id` (`post_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `fk_parent_comment` (`parent_comment_id`);

--
-- Index pour la table `community_likes`
--
ALTER TABLE `community_likes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_post_like` (`user_id`,`post_id`),
  ADD UNIQUE KEY `unique_comment_like` (`user_id`,`comment_id`),
  ADD KEY `post_id` (`post_id`),
  ADD KEY `comment_id` (`comment_id`);

--
-- Index pour la table `community_posts`
--
ALTER TABLE `community_posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `community_reports`
--
ALTER TABLE `community_reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_reporter` (`reporter_id`),
  ADD KEY `IDX_post` (`post_id`),
  ADD KEY `IDX_comment` (`comment_id`);

--
-- Index pour la table `coupon`
--
ALTER TABLE `coupon`
  ADD PRIMARY KEY (`idCoupon`),
  ADD UNIQUE KEY `code` (`code`),
  ADD KEY `idx_coupon_actif` (`actif`),
  ADD KEY `idx_coupon_code` (`code`);

--
-- Index pour la table `coupon_utilisation`
--
ALTER TABLE `coupon_utilisation`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idCoupon` (`id_coupon`,`id_user`),
  ADD KEY `idUser` (`id_user`);

--
-- Index pour la table `credit_dossier`
--
ALTER TABLE `credit_dossier`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_7CC87184433ED66` (`parcelle_id`);

--
-- Index pour la table `culture`
--
ALTER TABLE `culture`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_culture_parcelle` (`parcelle_id`);

--
-- Index pour la table `detailscommande`
--
ALTER TABLE `detailscommande`
  ADD PRIMARY KEY (`idDetails`),
  ADD KEY `idx_details_idCommande` (`id_commande`),
  ADD KEY `idx_details_idProduit` (`id_produit`);

--
-- Index pour la table `diagnostic`
--
ALTER TABLE `diagnostic`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_diagnostic_user` (`user_id`);

--
-- Index pour la table `diag_notification`
--
ALTER TABLE `diag_notification`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_DIAG_NOTIF_USER` (`user_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `employe`
--
ALTER TABLE `employe`
  ADD PRIMARY KEY (`id_employe`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `qr_code_unique` (`qr_code_unique`);

--
-- Index pour la table `employe_competence`
--
ALTER TABLE `employe_competence`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_employe_competence` (`id_employe`,`id_competence`),
  ADD KEY `id_competence` (`id_competence`),
  ADD KEY `idx_niveau` (`niveau`),
  ADD KEY `idx_experience` (`annees_experience`);

--
-- Index pour la table `evenement`
--
ALTER TABLE `evenement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_evenement_date_debut` (`date_debut`),
  ADD KEY `idx_evenement_type` (`type`),
  ADD KEY `idx_evenement_statut` (`statut`),
  ADD KEY `idx_evenement_createur` (`id_createur`);

--
-- Index pour la table `evenement_favoris`
--
ALTER TABLE `evenement_favoris`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_favori` (`id_evenement`,`id_utilisateur`),
  ADD KEY `idx_favoris_user` (`id_utilisateur`),
  ADD KEY `idx_favoris_event` (`id_evenement`);

--
-- Index pour la table `farm_health_report`
--
ALTER TABLE `farm_health_report`
  ADD PRIMARY KEY (`id`),
  ADD KEY `scan_id` (`scan_id`);

--
-- Index pour la table `farm_health_scan`
--
ALTER TABLE `farm_health_scan`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `irrigation_request`
--
ALTER TABLE `irrigation_request`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_CC105F64433ED66` (`parcelle_id`);

--
-- Index pour la table `maintenance`
--
ALTER TABLE `maintenance`
  ADD PRIMARY KEY (`id_maintenance`),
  ADD KEY `idx_materiel_id` (`materiel_id`),
  ADD KEY `idx_statut` (`statut_maintenance`),
  ADD KEY `idx_date_planifiee` (`date_planifiee`);

--
-- Index pour la table `materiel`
--
ALTER TABLE `materiel`
  ADD PRIMARY KEY (`id_materiel`),
  ADD UNIQUE KEY `qr_code_token` (`qr_code_token`),
  ADD KEY `fk_materiel_user` (`user_id`),
  ADD KEY `idx_materiel_user` (`user_id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `moderation_audit`
--
ALTER TABLE `moderation_audit`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_moderator` (`moderator_id`),
  ADD KEY `IDX_target` (`target_user_id`);

--
-- Index pour la table `notification`
--
ALTER TABLE `notification`
  ADD PRIMARY KEY (`id_notification`),
  ADD KEY `id_tache` (`id_tache`),
  ADD KEY `id_employe` (`id_employe`),
  ADD KEY `idx_agriculteur` (`id_agriculteur`),
  ADD KEY `idx_type` (`type`),
  ADD KEY `idx_lue` (`lue`),
  ADD KEY `idx_date` (`date_creation`);

--
-- Index pour la table `notificationmaintenance`
--
ALTER TABLE `notificationmaintenance`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_F4B0E85FA76ED395` (`user_id`),
  ADD KEY `IDX_F4B0E85F16880AAF` (`materiel_id`);

--
-- Index pour la table `notification_config`
--
ALTER TABLE `notification_config`
  ADD PRIMARY KEY (`id_config`),
  ADD UNIQUE KEY `id_agriculteur` (`id_agriculteur`);

--
-- Index pour la table `notif_market`
--
ALTER TABLE `notif_market`
  ADD PRIMARY KEY (`id_notif`),
  ADD KEY `id_user` (`id_user`),
  ADD KEY `id_produit` (`id_produit`),
  ADD KEY `id_commande` (`id_commande`);

--
-- Index pour la table `offre`
--
ALTER TABLE `offre`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `panier`
--
ALTER TABLE `panier`
  ADD PRIMARY KEY (`idPanier`),
  ADD KEY `idx_panier_idUser` (`id_user`);

--
-- Index pour la table `panier_produits`
--
ALTER TABLE `panier_produits`
  ADD PRIMARY KEY (`id_panier`,`id_produit`),
  ADD KEY `idProduit` (`id_produit`);

--
-- Index pour la table `parcelle`
--
ALTER TABLE `parcelle`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_parcelle_agriculteur` (`agriculteur_id`);

--
-- Index pour la table `participation`
--
ALTER TABLE `participation`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_participation` (`id_evenement`,`id_utilisateur`),
  ADD UNIQUE KEY `unique_qr_token` (`qr_code_token`),
  ADD KEY `idx_participation_evenement` (`id_evenement`),
  ADD KEY `idx_participation_utilisateur` (`id_utilisateur`),
  ADD KEY `idx_participation_statut` (`statut`);

--
-- Index pour la table `prevention_plan`
--
ALTER TABLE `prevention_plan`
  ADD PRIMARY KEY (`id`),
  ADD KEY `report_id` (`report_id`),
  ADD KEY `vulnerability_id` (`vulnerability_id`);

--
-- Index pour la table `prevention_task`
--
ALTER TABLE `prevention_task`
  ADD PRIMARY KEY (`id`),
  ADD KEY `prevention_plan_id` (`prevention_plan_id`);

--
-- Index pour la table `produits`
--
ALTER TABLE `produits`
  ADD PRIMARY KEY (`idProduit`),
  ADD KEY `idx_produit_idUser` (`id_user`),
  ADD KEY `idx_produit_categorie` (`categorie`),
  ADD KEY `FK_BE2DDF8C16880210` (`materiel_id`);

--
-- Index pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`idReclamation`),
  ADD KEY `idx_reclamation_idProduit` (`id_produit`),
  ADD KEY `idx_reclamation_idUser` (`id_user`);

--
-- Index pour la table `reclamation_materiel`
--
ALTER TABLE `reclamation_materiel`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_RECL_USER` (`user_id`),
  ADD KEY `IDX_RECL_MATERIEL` (`materiel_id`);

--
-- Index pour la table `review`
--
ALTER TABLE `review`
  ADD PRIMARY KEY (`id`),
  ADD KEY `diagnostic_id` (`diagnostic_id`),
  ADD KEY `treatment_plan_id` (`treatment_plan_id`),
  ADD KEY `expert_id` (`expert_id`);

--
-- Index pour la table `review_comments`
--
ALTER TABLE `review_comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_participation` (`participation_id`),
  ADD KEY `IDX_user` (`user_id`),
  ADD KEY `IDX_parent` (`parent_comment_id`);

--
-- Index pour la table `roi_analyses`
--
ALTER TABLE `roi_analyses`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_39354DB24433ED66` (`parcelle_id`);

--
-- Index pour la table `tache`
--
ALTER TABLE `tache`
  ADD PRIMARY KEY (`id_tache`);

--
-- Index pour la table `tache_competence_requise`
--
ALTER TABLE `tache_competence_requise`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_tache_competence` (`id_tache`,`id_competence`),
  ADD KEY `id_competence` (`id_competence`),
  ADD KEY `idx_niveau` (`niveau_requis`),
  ADD KEY `idx_importance` (`importance`);

--
-- Index pour la table `traitement`
--
ALTER TABLE `traitement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_2A356D27224CCA91` (`diagnostic_id`);

--
-- Index pour la table `treatment_plan`
--
ALTER TABLE `treatment_plan`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_1E99976C224CCA91` (`diagnostic_id`);

--
-- Index pour la table `treatment_task`
--
ALTER TABLE `treatment_task`
  ADD PRIMARY KEY (`id`),
  ADD KEY `treatment_plan_id` (`treatment_plan_id`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UNIQ_IDENTIFIER_EMAIL` (`email`);

--
-- Index pour la table `user_badge`
--
ALTER TABLE `user_badge`
  ADD PRIMARY KEY (`user_id`,`badge_id`),
  ADD KEY `badge_id` (`badge_id`);

--
-- Index pour la table `user_blocks`
--
ALTER TABLE `user_blocks`
  ADD PRIMARY KEY (`user_source`,`user_target`),
  ADD KEY `IDX_USER_BLOCKS_SOURCE` (`user_source`),
  ADD KEY `IDX_USER_BLOCKS_TARGET` (`user_target`);

--
-- Index pour la table `vulnerability`
--
ALTER TABLE `vulnerability`
  ADD PRIMARY KEY (`id`),
  ADD KEY `report_id` (`report_id`);

--
-- Index pour la table `wishlist`
--
ALTER TABLE `wishlist`
  ADD PRIMARY KEY (`idWishlist`),
  ADD UNIQUE KEY `uq_wishlist_user_produit` (`id_user`,`id_produit`),
  ADD KEY `fk_wish_produit` (`id_produit`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `abonnement`
--
ALTER TABLE `abonnement`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `ai_suggestions`
--
ALTER TABLE `ai_suggestions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `alerte_technicien`
--
ALTER TABLE `alerte_technicien`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `avis`
--
ALTER TABLE `avis`
  MODIFY `idAvis` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=46;

--
-- AUTO_INCREMENT pour la table `badge`
--
ALTER TABLE `badge`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `chatbot_conversation`
--
ALTER TABLE `chatbot_conversation`
  MODIFY `id_conversation` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `commande`
--
ALTER TABLE `commande`
  MODIFY `idCommande` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=230;

--
-- AUTO_INCREMENT pour la table `community_analytics_daily`
--
ALTER TABLE `community_analytics_daily`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `community_comments`
--
ALTER TABLE `community_comments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT pour la table `community_likes`
--
ALTER TABLE `community_likes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `community_posts`
--
ALTER TABLE `community_posts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `community_reports`
--
ALTER TABLE `community_reports`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `coupon`
--
ALTER TABLE `coupon`
  MODIFY `idCoupon` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT pour la table `coupon_utilisation`
--
ALTER TABLE `coupon_utilisation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `credit_dossier`
--
ALTER TABLE `credit_dossier`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `culture`
--
ALTER TABLE `culture`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=41;

--
-- AUTO_INCREMENT pour la table `detailscommande`
--
ALTER TABLE `detailscommande`
  MODIFY `idDetails` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=291;

--
-- AUTO_INCREMENT pour la table `diagnostic`
--
ALTER TABLE `diagnostic`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `diag_notification`
--
ALTER TABLE `diag_notification`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `employe`
--
ALTER TABLE `employe`
  MODIFY `id_employe` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=54;

--
-- AUTO_INCREMENT pour la table `employe_competence`
--
ALTER TABLE `employe_competence`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `evenement`
--
ALTER TABLE `evenement`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=97;

--
-- AUTO_INCREMENT pour la table `evenement_favoris`
--
ALTER TABLE `evenement_favoris`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `farm_health_report`
--
ALTER TABLE `farm_health_report`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `farm_health_scan`
--
ALTER TABLE `farm_health_scan`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `irrigation_request`
--
ALTER TABLE `irrigation_request`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `maintenance`
--
ALTER TABLE `maintenance`
  MODIFY `id_maintenance` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=26;

--
-- AUTO_INCREMENT pour la table `materiel`
--
ALTER TABLE `materiel`
  MODIFY `id_materiel` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `moderation_audit`
--
ALTER TABLE `moderation_audit`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `notification`
--
ALTER TABLE `notification`
  MODIFY `id_notification` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;

--
-- AUTO_INCREMENT pour la table `notificationmaintenance`
--
ALTER TABLE `notificationmaintenance`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=58;

--
-- AUTO_INCREMENT pour la table `notification_config`
--
ALTER TABLE `notification_config`
  MODIFY `id_config` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `notif_market`
--
ALTER TABLE `notif_market`
  MODIFY `id_notif` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=54;

--
-- AUTO_INCREMENT pour la table `offre`
--
ALTER TABLE `offre`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `panier`
--
ALTER TABLE `panier`
  MODIFY `idPanier` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=35;

--
-- AUTO_INCREMENT pour la table `parcelle`
--
ALTER TABLE `parcelle`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT pour la table `participation`
--
ALTER TABLE `participation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=364;

--
-- AUTO_INCREMENT pour la table `prevention_plan`
--
ALTER TABLE `prevention_plan`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `prevention_task`
--
ALTER TABLE `prevention_task`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `produits`
--
ALTER TABLE `produits`
  MODIFY `idProduit` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=107;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `idReclamation` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT pour la table `reclamation_materiel`
--
ALTER TABLE `reclamation_materiel`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `review`
--
ALTER TABLE `review`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `review_comments`
--
ALTER TABLE `review_comments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `roi_analyses`
--
ALTER TABLE `roi_analyses`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `tache`
--
ALTER TABLE `tache`
  MODIFY `id_tache` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=72;

--
-- AUTO_INCREMENT pour la table `tache_competence_requise`
--
ALTER TABLE `tache_competence_requise`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `traitement`
--
ALTER TABLE `traitement`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `treatment_plan`
--
ALTER TABLE `treatment_plan`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `treatment_task`
--
ALTER TABLE `treatment_task`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT pour la table `vulnerability`
--
ALTER TABLE `vulnerability`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `wishlist`
--
ALTER TABLE `wishlist`
  MODIFY `idWishlist` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `abonnement`
--
ALTER TABLE `abonnement`
  ADD CONSTRAINT `fk_abonnement_offre` FOREIGN KEY (`offre_id`) REFERENCES `offre` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_user_abonnement` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ai_suggestions`
--
ALTER TABLE `ai_suggestions`
  ADD CONSTRAINT `FK_AI_PARCELLE` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `avis`
--
ALTER TABLE `avis`
  ADD CONSTRAINT `avis_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `avis_ibfk_2` FOREIGN KEY (`id_produit`) REFERENCES `produits` (`idProduit`) ON DELETE CASCADE;

--
-- Contraintes pour la table `chatbot_conversation`
--
ALTER TABLE `chatbot_conversation`
  ADD CONSTRAINT `chatbot_conversation_ibfk_1` FOREIGN KEY (`id_utilisateur`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `commande`
--
ALTER TABLE `commande`
  ADD CONSTRAINT `commande_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `commande_ibfk_2` FOREIGN KEY (`id_coupon`) REFERENCES `coupon` (`idCoupon`);

--
-- Contraintes pour la table `community_analytics_daily`
--
ALTER TABLE `community_analytics_daily`
  ADD CONSTRAINT `FK_9F2CCBDE4B89032C` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`);

--
-- Contraintes pour la table `community_comments`
--
ALTER TABLE `community_comments`
  ADD CONSTRAINT `community_comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `community_comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_parent_comment` FOREIGN KEY (`parent_comment_id`) REFERENCES `community_comments` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `community_likes`
--
ALTER TABLE `community_likes`
  ADD CONSTRAINT `community_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `community_likes_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `community_likes_ibfk_3` FOREIGN KEY (`comment_id`) REFERENCES `community_comments` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `community_posts`
--
ALTER TABLE `community_posts`
  ADD CONSTRAINT `community_posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `community_reports`
--
ALTER TABLE `community_reports`
  ADD CONSTRAINT `FK_report_comment` FOREIGN KEY (`comment_id`) REFERENCES `community_comments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_report_post` FOREIGN KEY (`post_id`) REFERENCES `community_posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `coupon_utilisation`
--
ALTER TABLE `coupon_utilisation`
  ADD CONSTRAINT `coupon_utilisation_ibfk_1` FOREIGN KEY (`id_coupon`) REFERENCES `coupon` (`idCoupon`),
  ADD CONSTRAINT `coupon_utilisation_ibfk_2` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `credit_dossier`
--
ALTER TABLE `credit_dossier`
  ADD CONSTRAINT `FK_7CC87184433ED66` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `culture`
--
ALTER TABLE `culture`
  ADD CONSTRAINT `culture_ibfk_1` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `detailscommande`
--
ALTER TABLE `detailscommande`
  ADD CONSTRAINT `detailscommande_ibfk_1` FOREIGN KEY (`id_commande`) REFERENCES `commande` (`idCommande`) ON DELETE CASCADE,
  ADD CONSTRAINT `detailscommande_ibfk_2` FOREIGN KEY (`id_produit`) REFERENCES `produits` (`idProduit`);

--
-- Contraintes pour la table `diagnostic`
--
ALTER TABLE `diagnostic`
  ADD CONSTRAINT `fk_diagnostic_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `diag_notification`
--
ALTER TABLE `diag_notification`
  ADD CONSTRAINT `FK_DIAG_NOTIF_USER` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `employe_competence`
--
ALTER TABLE `employe_competence`
  ADD CONSTRAINT `employe_competence_ibfk_1` FOREIGN KEY (`id_employe`) REFERENCES `employe` (`id_employe`) ON DELETE CASCADE;

--
-- Contraintes pour la table `evenement`
--
ALTER TABLE `evenement`
  ADD CONSTRAINT `evenement_ibfk_1` FOREIGN KEY (`id_createur`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `evenement_favoris`
--
ALTER TABLE `evenement_favoris`
  ADD CONSTRAINT `evenement_favoris_ibfk_1` FOREIGN KEY (`id_evenement`) REFERENCES `evenement` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `evenement_favoris_ibfk_2` FOREIGN KEY (`id_utilisateur`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `farm_health_report`
--
ALTER TABLE `farm_health_report`
  ADD CONSTRAINT `farm_health_report_ibfk_1` FOREIGN KEY (`scan_id`) REFERENCES `farm_health_scan` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `farm_health_scan`
--
ALTER TABLE `farm_health_scan`
  ADD CONSTRAINT `farm_health_scan_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `irrigation_request`
--
ALTER TABLE `irrigation_request`
  ADD CONSTRAINT `FK_CC105F64433ED66` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `maintenance`
--
ALTER TABLE `maintenance`
  ADD CONSTRAINT `fk_maintenance_materiel` FOREIGN KEY (`materiel_id`) REFERENCES `materiel` (`id_materiel`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `materiel`
--
ALTER TABLE `materiel`
  ADD CONSTRAINT `fk_materiel_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `moderation_audit`
--
ALTER TABLE `moderation_audit`
  ADD CONSTRAINT `FK_audit_moderator` FOREIGN KEY (`moderator_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `FK_audit_target` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`id`);

--
-- Contraintes pour la table `notification`
--
ALTER TABLE `notification`
  ADD CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`id_agriculteur`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `notification_ibfk_2` FOREIGN KEY (`id_tache`) REFERENCES `tache` (`id_tache`) ON DELETE CASCADE,
  ADD CONSTRAINT `notification_ibfk_3` FOREIGN KEY (`id_employe`) REFERENCES `employe` (`id_employe`) ON DELETE CASCADE;

--
-- Contraintes pour la table `notification_config`
--
ALTER TABLE `notification_config`
  ADD CONSTRAINT `notification_config_ibfk_1` FOREIGN KEY (`id_agriculteur`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `notif_market`
--
ALTER TABLE `notif_market`
  ADD CONSTRAINT `notif_market_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `notif_market_ibfk_2` FOREIGN KEY (`id_produit`) REFERENCES `produits` (`idProduit`) ON DELETE SET NULL,
  ADD CONSTRAINT `notif_market_ibfk_3` FOREIGN KEY (`id_commande`) REFERENCES `commande` (`idCommande`) ON DELETE SET NULL;

--
-- Contraintes pour la table `panier`
--
ALTER TABLE `panier`
  ADD CONSTRAINT `fk_panier_user` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `panier_produits`
--
ALTER TABLE `panier_produits`
  ADD CONSTRAINT `panier_produits_ibfk_1` FOREIGN KEY (`id_panier`) REFERENCES `panier` (`idPanier`),
  ADD CONSTRAINT `panier_produits_ibfk_2` FOREIGN KEY (`id_produit`) REFERENCES `produits` (`idProduit`);

--
-- Contraintes pour la table `parcelle`
--
ALTER TABLE `parcelle`
  ADD CONSTRAINT `parcelle_ibfk_1` FOREIGN KEY (`agriculteur_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `participation`
--
ALTER TABLE `participation`
  ADD CONSTRAINT `participation_ibfk_1` FOREIGN KEY (`id_evenement`) REFERENCES `evenement` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `participation_ibfk_2` FOREIGN KEY (`id_utilisateur`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `prevention_plan`
--
ALTER TABLE `prevention_plan`
  ADD CONSTRAINT `prevention_plan_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `farm_health_report` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `prevention_plan_ibfk_2` FOREIGN KEY (`vulnerability_id`) REFERENCES `vulnerability` (`id`);

--
-- Contraintes pour la table `prevention_task`
--
ALTER TABLE `prevention_task`
  ADD CONSTRAINT `prevention_task_ibfk_1` FOREIGN KEY (`prevention_plan_id`) REFERENCES `prevention_plan` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `produits`
--
ALTER TABLE `produits`
  ADD CONSTRAINT `FK_BE2DDF8C16880210` FOREIGN KEY (`materiel_id`) REFERENCES `materiel` (`id_materiel`),
  ADD CONSTRAINT `fk_produit_user` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `review`
--
ALTER TABLE `review`
  ADD CONSTRAINT `review_ibfk_1` FOREIGN KEY (`diagnostic_id`) REFERENCES `diagnostic` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `review_ibfk_2` FOREIGN KEY (`treatment_plan_id`) REFERENCES `treatment_plan` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `review_ibfk_3` FOREIGN KEY (`expert_id`) REFERENCES `user` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `review_comments`
--
ALTER TABLE `review_comments`
  ADD CONSTRAINT `FK_rc_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `review_comments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_rc_participation` FOREIGN KEY (`participation_id`) REFERENCES `participation` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_rc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `roi_analyses`
--
ALTER TABLE `roi_analyses`
  ADD CONSTRAINT `FK_39354DB24433ED66` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `tache_competence_requise`
--
ALTER TABLE `tache_competence_requise`
  ADD CONSTRAINT `tache_competence_requise_ibfk_1` FOREIGN KEY (`id_tache`) REFERENCES `tache` (`id_tache`) ON DELETE CASCADE;

--
-- Contraintes pour la table `traitement`
--
ALTER TABLE `traitement`
  ADD CONSTRAINT `FK_2A356D27224CCA91` FOREIGN KEY (`diagnostic_id`) REFERENCES `diagnostic` (`id`);

--
-- Contraintes pour la table `treatment_plan`
--
ALTER TABLE `treatment_plan`
  ADD CONSTRAINT `FK_1E99976C224CCA91` FOREIGN KEY (`diagnostic_id`) REFERENCES `diagnostic` (`id`);

--
-- Contraintes pour la table `treatment_task`
--
ALTER TABLE `treatment_task`
  ADD CONSTRAINT `treatment_task_ibfk_1` FOREIGN KEY (`treatment_plan_id`) REFERENCES `treatment_plan` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `user_badge`
--
ALTER TABLE `user_badge`
  ADD CONSTRAINT `user_badge_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `user_badge_ibfk_2` FOREIGN KEY (`badge_id`) REFERENCES `badge` (`id`);

--
-- Contraintes pour la table `user_blocks`
--
ALTER TABLE `user_blocks`
  ADD CONSTRAINT `FK_USER_BLOCKS_SOURCE` FOREIGN KEY (`user_source`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_USER_BLOCKS_TARGET` FOREIGN KEY (`user_target`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `vulnerability`
--
ALTER TABLE `vulnerability`
  ADD CONSTRAINT `vulnerability_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `farm_health_report` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `wishlist`
--
ALTER TABLE `wishlist`
  ADD CONSTRAINT `fk_wish_produit` FOREIGN KEY (`id_produit`) REFERENCES `produits` (`idProduit`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_wish_user` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
