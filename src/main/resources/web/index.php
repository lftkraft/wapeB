<?php
session_start();
header('Content-Type: text/html; charset=utf-8');

// --- CONFIGURATION ---
$api_ip = "94.156.37.198";        // Minecraft server IP
$api_port = 25571;               // Plugin config web-api port
$api_key = "trpzw345uwe49txfgbOhdG_DFGDVCXAer432";   // Plugin config api-key
// ----------------------

$api_url = "http://$api_ip:$api_port/api";

function callAPI($endpoint, $params = [], $method = "GET") {
    global $api_url, $api_key, $api_ip, $api_port;
    $url = $api_url . $endpoint;
    $queryString = http_build_query($params);

    if ($method == "GET" && !empty($params)) {
        $url .= "?" . $queryString;
    }

    $ch = curl_init();
    if ($method == "POST") {
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_URL, $url . "?" . $queryString);
    } else {
        curl_setopt($ch, CURLOPT_URL, $url);
    }

    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-API-Key: $api_key",
        "Accept: application/json"
    ]);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);

    $response = curl_exec($ch);
    $curlError = curl_error($ch);
    $curlErrno = curl_errno($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($response === false || $httpCode === 0) {
        $msg = "Sikertelen csatlakozás a Minecraft szerverhez ($api_ip:$api_port). ";
        if ($curlErrno === 28) {
            $msg .= "Időtúllépés! A port ($api_port) esetleg le van tiltva a tűzfalon.";
        } elseif ($curlErrno === 7) {
            $msg .= "Kapcsolat elutasítva. Ellenőrizd, hogy a Web-API aktív-e a porton ($api_port)!";
        } elseif (!empty($curlError)) {
            $msg .= "cURL Hiba (#$curlErrno): $curlError";
        } else {
            $msg .= "A szerver nem érhető el.";
        }
        return ['code' => 0, 'data' => ['error' => $msg], 'error' => $msg];
    }

    $data = json_decode($response, true);
    if ($data === null && !empty($response)) {
        return ['code' => $httpCode, 'data' => ['error' => 'Érvénytelen válasz a szervertől: ' . substr(strip_tags($response), 0, 100)], 'error' => 'Érvénytelen válasz.'];
    }

    return ['code' => $httpCode, 'data' => $data, 'error' => ($httpCode != 200 ? ($data['error'] ?? "HTTP hibakód: $httpCode") : null)];
}

if (isset($_GET['proxy'])) {
    if (!isset($_SESSION['user_uuid'])) {
        header('Content-Type: application/json');
        echo json_encode(['error' => 'Nincs hitelesítve']);
        exit;
    }
    $res = callAPI($_GET['proxy'], array_diff_key($_GET, ['proxy' => '']), $_SERVER['REQUEST_METHOD']);
    header('Content-Type: application/json');
    http_response_code($res['code'] > 0 ? $res['code'] : 502);
    echo json_encode($res['data'] ?? ['error' => $res['error'] ?? 'Hiba a kérés feldolgozása során']);
    exit;
}

if (isset($_GET['logout'])) { session_destroy(); header("Location: index.php"); exit; }

$error = "";
$step = isset($_SESSION['pending_player']) ? 2 : 1;
if(isset($_GET['mode']) && $_GET['mode'] == 'password') $step = 3;

if ($_SERVER['REQUEST_METHOD'] == 'POST' && !isset($_GET['proxy'])) {
    if (isset($_POST['login_username']) && $step == 1) {
        $res = callAPI("/login/request", ["player" => $_POST['login_username']]);
        if ($res['code'] == 200) {
            $_SESSION['pending_player'] = $_POST['login_username'];
            header("Location: index.php");
            exit;
        } else {
            $error = $res['data']['error'] ?? ($res['error'] ?? "Sikertelen bejelentkezési kód kérés!");
        }
    } elseif (isset($_POST['login_code']) && $step == 2) {
        $res = callAPI("/login/verify", ["player" => $_SESSION['pending_player'], "code" => $_POST['login_code']]);
        if ($res['code'] == 200) {
            $_SESSION['user_uuid'] = $res['data']['uuid'];
            unset($_SESSION['pending_player']);
            header("Location: index.php");
            exit;
        } else {
            $error = $res['data']['error'] ?? "Érvénytelen vagy lejárt ellenőrző kód!";
        }
    } elseif (isset($_POST['login_password']) && $step == 3) {
        $res = callAPI("/login/password", ["player" => $_POST['target_player'], "password" => $_POST['login_password']]);
        if ($res['code'] == 200) {
            $_SESSION['user_uuid'] = $res['data']['uuid'];
            header("Location: index.php");
            exit;
        } else {
            $error = $res['data']['error'] ?? "Hibás felhasználónév vagy jelszó!";
        }
    }
}

$user_info = null;
if (isset($_SESSION['user_uuid'])) {
    $res = callAPI("/user/info", ["uuid" => $_SESSION['user_uuid']]);
    if ($res['code'] == 200) $user_info = $res['data'];
    else { session_destroy(); header("Location: index.php"); exit; }
}
?>
<!DOCTYPE html>
<html lang="hu" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>COOLNETWORK Admin Panel</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css" rel="stylesheet">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;600;800&display=swap');

        :root {
            --accent-pink: #FF00D9;
            --accent-purple: #9D00FF;
            --accent-cyan: #00F0FF;
            --bg-dark: #07070a;
            --card-dark: rgba(18, 18, 26, 0.7);
        }

        body {
            background-color: var(--bg-dark);
            color: #f0f0f5;
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-image: 
                radial-gradient(circle at 15% 15%, rgba(255, 0, 217, 0.07) 0%, transparent 40%),
                radial-gradient(circle at 85% 85%, rgba(157, 0, 255, 0.07) 0%, transparent 40%),
                radial-gradient(circle at 50% 50%, rgba(0, 240, 255, 0.03) 0%, transparent 60%);
            background-attachment: fixed;
        }

        .mono { font-family: 'JetBrains Mono', monospace; }

        /* Custom Modern Scrollbars */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        ::-webkit-scrollbar-track {
            background: rgba(10, 10, 16, 0.6);
            border-radius: 10px;
        }
        ::-webkit-scrollbar-thumb {
            background: linear-gradient(180deg, rgba(255, 0, 217, 0.4) 0%, rgba(112, 0, 255, 0.4) 100%);
            border-radius: 10px;
            border: 2px solid rgba(10, 10, 16, 0.6);
        }
        ::-webkit-scrollbar-thumb:hover {
            background: linear-gradient(180deg, rgba(255, 0, 217, 0.7) 0%, rgba(112, 0, 255, 0.7) 100%);
        }
        * {
            scrollbar-width: thin;
            scrollbar-color: rgba(255, 0, 217, 0.4) rgba(10, 10, 16, 0.6);
        }

        /* Glassmorphism Styles */
        .glass-panel {
            background: rgba(15, 15, 23, 0.75);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
        }

        .glass-card {
            background: rgba(22, 22, 34, 0.5);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.06);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .glass-card:hover {
            border-color: rgba(255, 0, 217, 0.3);
            box-shadow: 0 10px 30px rgba(255, 0, 217, 0.1);
            transform: translateY(-2px);
        }

        .input-cyber {
            background: rgba(10, 10, 16, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.08);
            color: #ffffff;
            transition: all 0.25s ease;
        }

        .input-cyber:focus {
            border-color: var(--accent-pink);
            box-shadow: 0 0 20px rgba(255, 0, 217, 0.25);
            outline: none;
        }

        .btn-neon {
            background: linear-gradient(135deg, #FF00D9 0%, #7000FF 100%);
            color: #ffffff;
            font-weight: 700;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 4px 25px rgba(255, 0, 217, 0.35);
        }

        .btn-neon:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 35px rgba(255, 0, 217, 0.55);
            filter: brightness(1.1);
        }

        /* Titlebar Navigation Tabs */
        .nav-tab {
            color: #8f8fa8;
            background: transparent;
            transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
            border: 1px solid transparent;
        }

        .nav-tab:hover {
            color: #ffffff;
            background: rgba(255, 255, 255, 0.05);
        }

        .nav-tab.active {
            color: #ffffff;
            background: linear-gradient(135deg, rgba(255, 0, 217, 0.25) 0%, rgba(112, 0, 255, 0.25) 100%);
            border-color: rgba(255, 0, 217, 0.4);
            box-shadow: 0 4px 20px rgba(255, 0, 217, 0.25);
        }

        .swal2-popup {
            background: #0d0d14 !important;
            border: 1px solid rgba(255, 0, 217, 0.3) !important;
            border-radius: 24px !important;
            color: #fff !important;
            backdrop-filter: blur(20px) !important;
        }

        .tag-pill {
            padding: 4px 12px;
            border-radius: 9999px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.05em;
            text-transform: uppercase;
        }

        .badge-ban { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); }
        .badge-mute { background: rgba(234, 179, 8, 0.15); color: #facc15; border: 1px solid rgba(234, 179, 8, 0.3); }
        .badge-warn { background: rgba(59, 130, 246, 0.15); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.3); }
        .badge-kick { background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.3); }

        .filter-btn {
            padding: 8px 16px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 700;
            color: #8f8fa8;
            transition: all 0.2s ease;
        }

        .filter-btn:hover { color: #fff; background: rgba(255, 255, 255, 0.05); }
        .filter-btn.active { color: #fff; background: rgba(255, 0, 217, 0.2); border: 1px solid rgba(255, 0, 217, 0.3); }

        .preset-pill {
            padding: 6px 14px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 700;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            color: #d1d5db;
            transition: all 0.2s ease;
        }
        .preset-pill:hover {
            background: rgba(255, 0, 217, 0.2);
            border-color: rgba(255, 0, 217, 0.4);
            color: #fff;
        }

        .pulse-online {
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
            animation: pulse-green 2s infinite;
        }

        @keyframes pulse-green {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(34, 197, 94, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
        }
    </style>
</head>
<body class="min-h-screen antialiased flex flex-col">

    <?php if (!$user_info): ?>
        <!-- LOGIN SECTION -->
        <div class="min-h-screen flex items-center justify-center p-6 relative">
            <div class="glass-panel p-10 rounded-3xl w-full max-w-md animate__animated animate__fadeInDown relative z-10">
                <div class="text-center mb-8">
                    <div class="w-20 h-20 bg-gradient-to-tr from-[#FF00D9] via-[#9D00FF] to-[#00F0FF] p-[2px] rounded-3xl mx-auto mb-6 shadow-2xl shadow-[#FF00D9]/30">
                        <div class="w-full h-full bg-[#0a0a10] rounded-[22px] flex items-center justify-center">
                            <i data-lucide="shield-alert" class="text-[#FF00D9] w-10 h-10"></i>
                        </div>
                    </div>
                    <h1 class="text-4xl font-extrabold tracking-tight text-white mb-1">COOL<span class="text-transparent bg-clip-text bg-gradient-to-r from-[#FF00D9] to-[#00F0FF]">NETWORK</span></h1>
                    <p class="text-xs font-semibold uppercase tracking-[0.25em] text-gray-400">Admin Control Portál</p>
                </div>

                <form method="POST" class="space-y-6">
                    <?php if ($error): ?>
                        <div class="p-4 bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-semibold rounded-2xl text-center leading-relaxed animate__animated animate__shakeX">
                            <i data-lucide="alert-circle" class="w-4 h-4 inline mr-1"></i> <?= htmlspecialchars($error) ?>
                        </div>
                    <?php endif; ?>

                    <?php if ($step == 1): ?>
                        <div class="space-y-2">
                            <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider ml-1">Minecraft Felhasználónév</label>
                            <div class="relative">
                                <i data-lucide="user" class="w-5 h-5 absolute left-4 top-4 text-gray-500"></i>
                                <input type="text" name="login_username" required placeholder="pl. Administrator" class="input-cyber w-full pl-12 pr-4 py-3.5 rounded-2xl text-sm">
                            </div>
                        </div>
                        <div class="flex gap-3 pt-2">
                            <button type="submit" class="btn-neon flex-grow py-3.5 rounded-2xl text-xs uppercase tracking-wider">Bejelentkezési Kód Kérése</button>
                            <a href="?mode=password" class="p-3.5 bg-white/5 hover:bg-white/10 rounded-2xl border border-white/10 transition-all text-gray-400 hover:text-white flex items-center justify-center" title="Bejelentkezés jelszóval">
                                <i data-lucide="key" class="w-5 h-5"></i>
                            </a>
                        </div>
                    <?php elseif ($step == 2): ?>
                        <div class="space-y-3 text-center">
                            <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Ellenőrző PIN Kód (Játékban ellenőrizd)</label>
                            <input type="text" name="login_code" required maxlength="6" autofocus placeholder="000000" class="input-cyber w-full py-4 text-center text-3xl font-black tracking-[0.3em] text-[#FF00D9] rounded-2xl mono">
                            <p class="text-xs text-gray-500">Futtasd a <span class="text-white font-mono bg-white/5 px-2 py-1 rounded">/wapeb login</span> parancsot a Minecraftban, ha a kód lejárt</p>
                        </div>
                        <button type="submit" class="btn-neon w-full py-3.5 rounded-2xl text-xs uppercase tracking-wider mt-2">Ellenőrzés és Bejelentkezés</button>
                        <div class="flex justify-between items-center px-1 text-xs">
                            <a href="index.php" class="text-gray-500 hover:text-white font-medium transition-all">Vissza</a>
                            <a href="?mode=password" class="text-gray-500 hover:text-white font-medium transition-all">Jelszó Használata</a>
                        </div>
                    <?php elseif ($step == 3): ?>
                        <div class="space-y-4">
                            <div>
                                <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider ml-1">Felhasználónév</label>
                                <input type="text" name="target_player" required placeholder="Felhasználónév" class="input-cyber w-full p-3.5 rounded-2xl text-sm">
                            </div>
                            <div>
                                <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider ml-1">Jelszó</label>
                                <input type="password" name="login_password" required placeholder="........" class="input-cyber w-full p-3.5 rounded-2xl text-sm">
                            </div>
                        </div>
                        <button type="submit" class="btn-neon w-full py-3.5 rounded-2xl text-xs uppercase tracking-wider">Hitelesítés</button>
                        <div class="text-center pt-2">
                            <a href="index.php" class="text-xs text-gray-500 hover:text-white font-medium transition-all">Vissza a PIN Kódhoz</a>
                        </div>
                    <?php endif; ?>
                </form>
            </div>
        </div>
    <?php else: ?>
        <!-- TOP TITLEBAR HEADER -->
        <header class="sticky top-0 z-40 w-full glass-panel px-6 py-4 flex items-center justify-between shadow-2xl">
            <!-- BRAND & LOGO -->
            <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-2xl bg-gradient-to-tr from-[#FF00D9] to-[#7000FF] flex items-center justify-center shadow-lg shadow-[#FF00D9]/30">
                    <i data-lucide="shield" class="text-white w-5 h-5"></i>
                </div>
                <div>
                    <h2 class="text-xl font-black tracking-tight text-white leading-none">COOL<span class="text-transparent bg-clip-text bg-gradient-to-r from-[#FF00D9] to-[#00F0FF]">NETWORK</span></h2>
                    <p class="text-[9px] font-bold uppercase tracking-widest text-gray-400 mt-1">Adminisztrációs Vezérlőpult</p>
                </div>
            </div>

            <!-- TITLEBAR NAVIGATION TABS (DESKTOP) -->
            <nav class="hidden md:flex items-center gap-2 bg-black/40 p-1.5 rounded-2xl border border-white/5">
                <button onclick="showPage('dashboard')" id="nav-dashboard" class="nav-tab active flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider">
                    <i data-lucide="layout-dashboard" class="w-4 h-4"></i> Áttekintés
                </button>
                <button onclick="showPage('players')" id="nav-players" class="nav-tab flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider">
                    <i data-lucide="users" class="w-4 h-4"></i> Játékoskezelő
                </button>
                <button onclick="showPage('active-punishments')" id="nav-active-punishments" class="nav-tab flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider">
                    <i data-lucide="gavel" class="w-4 h-4"></i> Aktív Szankciók
                </button>
                <button onclick="showPage('staff-history')" id="nav-staff-history" class="nav-tab flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider">
                    <i data-lucide="shield-check" class="w-4 h-4"></i> Staff Előzmények
                </button>
                <button onclick="showPage('lockdown')" id="nav-lockdown" class="nav-tab flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider">
                    <i data-lucide="lock" class="w-4 h-4"></i> Szerver Zárolás
                </button>
            </nav>

            <!-- RIGHT: USER PROFILE -->
            <div class="flex items-center gap-4">
                <div class="flex items-center gap-3 pl-3">
                    <img src="https://mc-heads.net/avatar/<?= $_SESSION['user_uuid'] ?>/40" class="w-9 h-9 rounded-xl bg-black/40 border border-white/10" alt="">
                    <div class="hidden sm:block">
                        <p class="text-xs font-extrabold text-white leading-tight"><?= htmlspecialchars($user_info['name']) ?></p>
                        <span class="text-[9px] font-black uppercase tracking-widest text-[#FF00D9]"><?= htmlspecialchars($user_info['rank']) ?></span>
                    </div>
                    <button onclick="openPasswordModal()" class="p-2 hover:bg-white/10 rounded-xl transition-all text-gray-400 hover:text-white" title="Jelszó Módosítása">
                        <i data-lucide="settings" class="w-4 h-4"></i>
                    </button>
                    <a href="?logout" class="p-2 hover:bg-red-500/20 text-gray-400 hover:text-red-400 rounded-xl transition-all" title="Kijelentkezés">
                        <i data-lucide="log-out" class="w-4 h-4"></i>
                    </a>
                </div>
            </div>
        </header>

        <!-- MOBILE NAVIGATION ROW -->
        <div class="md:hidden glass-panel border-b border-white/5 px-4 py-3 flex items-center justify-around overflow-x-auto gap-2">
            <button onclick="showPage('dashboard')" id="mob-nav-dashboard" class="nav-tab active px-3 py-2 rounded-xl text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 whitespace-nowrap">
                <i data-lucide="layout-dashboard" class="w-3.5 h-3.5"></i> Áttekintés
            </button>
            <button onclick="showPage('players')" id="mob-nav-players" class="nav-tab px-3 py-2 rounded-xl text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 whitespace-nowrap">
                <i data-lucide="users" class="w-3.5 h-3.5"></i> Játékosok
            </button>
            <button onclick="showPage('active-punishments')" id="mob-nav-active-punishments" class="nav-tab px-3 py-2 rounded-xl text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 whitespace-nowrap">
                <i data-lucide="gavel" class="w-3.5 h-3.5"></i> Aktív
            </button>
            <button onclick="showPage('staff-history')" id="mob-nav-staff-history" class="nav-tab px-3 py-2 rounded-xl text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 whitespace-nowrap">
                <i data-lucide="shield-check" class="w-3.5 h-3.5"></i> Staff
            </button>
            <button onclick="showPage('lockdown')" id="mob-nav-lockdown" class="nav-tab px-3 py-2 rounded-xl text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 whitespace-nowrap">
                <i data-lucide="lock" class="w-3.5 h-3.5"></i> Zárolás
            </button>
        </div>

        <!-- MAIN CONTENT DISPLAY CONTAINER -->
        <main class="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 space-y-8">
            <!-- PAGE SUBHEADER -->
            <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-white/5">
                <div>
                    <h1 class="text-3xl font-black text-white tracking-tight" id="header-title">Áttekintő Vezérlőpult</h1>
                    <p class="text-xs text-gray-400 mt-1">Valós idejű Minecraft szerver moderációs statisztikák és vezérlés</p>
                </div>
            </div>

            <!-- PAGE 1: DASHBOARD OVERVIEW -->
            <div id="page-dashboard" class="page animate__animated animate__fadeIn space-y-8">
                <!-- STAT CARDS GRID -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                    <div class="glass-card p-6 rounded-3xl relative overflow-hidden">
                        <div class="absolute -right-4 -bottom-4 opacity-10 text-[#FF00D9]"><i data-lucide="activity" class="w-32 h-32"></i></div>
                        <p class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Összes Szankció</p>
                        <h3 id="stat-total" class="text-4xl font-extrabold text-white">--</h3>
                        <div class="mt-4 text-[11px] font-semibold text-[#FF00D9] flex items-center gap-1"><i data-lucide="trending-up" class="w-3.5 h-3.5"></i> Mindenkori rekord</div>
                    </div>
                    <div class="glass-card p-6 rounded-3xl relative overflow-hidden">
                        <div class="absolute -right-4 -bottom-4 opacity-10 text-red-500"><i data-lucide="ban" class="w-32 h-32"></i></div>
                        <p class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Kiosztott Ban-ok</p>
                        <h3 id="stat-bans" class="text-4xl font-extrabold text-white">--</h3>
                        <div class="mt-4 text-[11px] font-semibold text-red-400 flex items-center gap-1"><i data-lucide="shield-x" class="w-3.5 h-3.5"></i> Örök és ideiglenes kitiltások</div>
                    </div>
                    <div class="glass-card p-6 rounded-3xl relative overflow-hidden">
                        <div class="absolute -right-4 -bottom-4 opacity-10 text-yellow-500"><i data-lucide="mic-off" class="w-32 h-32"></i></div>
                        <p class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Kiosztott Némítások</p>
                        <h3 id="stat-mutes" class="text-4xl font-extrabold text-white">--</h3>
                        <div class="mt-4 text-[11px] font-semibold text-yellow-400 flex items-center gap-1"><i data-lucide="message-square-off" class="w-3.5 h-3.5"></i> Csevegési korlátozások</div>
                    </div>
                    <div class="glass-card p-6 rounded-3xl relative overflow-hidden">
                        <div class="absolute -right-4 -bottom-4 opacity-10 text-blue-500"><i data-lucide="alert-triangle" class="w-32 h-32"></i></div>
                        <p class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Kiosztott Figyelmeztetések</p>
                        <h3 id="stat-warnings" class="text-4xl font-extrabold text-white">--</h3>
                        <div class="mt-4 text-[11px] font-semibold text-blue-400 flex items-center gap-1"><i data-lucide="info" class="w-3.5 h-3.5"></i> Szabályszegési figyelmeztetések</div>
                    </div>
                </div>

                <!-- ACTIVITY CHART -->
                <div class="glass-card p-8 rounded-3xl">
                    <div class="flex items-center justify-between mb-6">
                        <div>
                            <h3 class="text-lg font-extrabold text-white">Moderációs Aktivitás</h3>
                            <p class="text-xs text-gray-400">Kiosztott szankciók óránként az elmúlt 24 órában</p>
                        </div>
                    </div>
                    <div class="h-80 w-full">
                        <canvas id="statsChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- PAGE 2: ACTIVE PUNISHMENTS -->
            <div id="page-active-punishments" class="page hidden animate__animated animate__fadeIn space-y-6">
                <div class="flex flex-wrap gap-4 items-center justify-between">
                    <!-- FILTER BUTTONS -->
                    <div class="flex bg-white/5 p-1.5 rounded-2xl border border-white/5 gap-1">
                        <button onclick="filterActive('ALL')" class="filter-btn active" id="f-all">Összes Aktív</button>
                        <button onclick="filterActive('BAN')" class="filter-btn" id="f-ban">Csak Ban-ok</button>
                        <button onclick="filterActive('MUTE')" class="filter-btn" id="f-mute">Csak Némítások</button>
                        <button onclick="filterActive('WARN')" class="filter-btn" id="f-warn">Csak Warn-ok</button>
                    </div>

                    <!-- SEARCH BAR -->
                    <div class="relative flex-grow max-w-sm">
                        <i data-lucide="search" class="w-4 h-4 absolute left-4 top-3.5 text-gray-500"></i>
                        <input type="text" id="active-search" oninput="renderActiveTable()" placeholder="Aktív játékos keresése..." class="input-cyber w-full pl-11 pr-4 py-2.5 rounded-2xl text-xs font-semibold">
                    </div>
                </div>

                <!-- TABLE PANEL -->
                <div class="glass-card rounded-3xl overflow-hidden">
                    <table class="w-full text-left border-collapse">
                        <thead class="bg-white/5 text-[11px] uppercase font-bold text-gray-400 tracking-wider">
                            <tr>
                                <th class="p-5">Játékos</th>
                                <th class="p-5">Típus</th>
                                <th class="p-5">Indok</th>
                                <th class="p-5">Adminisztrátor</th>
                                <th class="p-5">Dátum és Idő</th>
                                <th class="p-5 text-right">Visszavonás</th>
                            </tr>
                        </thead>
                        <tbody id="active-table-body" class="divide-y divide-white/5 text-sm"></tbody>
                    </table>
                </div>
            </div>

            <!-- PAGE 3: PLAYER MANAGER (CENTERED SEARCH AT VERY TOP) -->
            <div id="page-players" class="page hidden animate__animated animate__fadeIn space-y-8">
                <!-- CENTERED SEARCH BOX AT THE VERY TOP -->
                <div class="max-w-2xl mx-auto glass-card p-4 rounded-3xl flex gap-3 shadow-2xl">
                    <div class="relative flex-grow">
                        <i data-lucide="user-search" class="w-5 h-5 absolute left-4 top-4 text-gray-500"></i>
                        <input type="text" id="player-search-input" placeholder="Írd be a játékos nevét..." onkeypress="if(event.key==='Enter') searchPlayer()" class="input-cyber w-full pl-12 pr-4 py-3.5 rounded-2xl text-sm font-semibold">
                    </div>
                    <button onclick="searchPlayer()" class="btn-neon px-8 rounded-2xl text-xs uppercase tracking-wider flex items-center gap-2">
                        <i data-lucide="search" class="w-4 h-4"></i> Profil Megtekintése
                    </button>
                </div>

                <!-- ONLINE PLAYERS CONTAINER (Hides when a player profile is searched) -->
                <div id="online-players-container" class="space-y-4">
                    <div class="flex items-center justify-between">
                        <h3 class="text-xs font-black uppercase tracking-wider text-gray-400 flex items-center gap-2">
                            <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 pulse-online"></span> Aktív Közösségi Játékosok
                        </h3>
                        <span class="text-xs text-gray-500 font-medium">Kattints bármelyik játékosra a profil megtekintéséhez</span>
                    </div>
                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4" id="online-players-grid"></div>
                </div>

                <!-- PROFILE VIEW RESULT -->
                <div id="player-profile-view" class="hidden animate__animated animate__fadeInUp space-y-6">
                    <div class="flex items-center justify-between">
                        <button onclick="showOnlinePlayersGrid()" class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-400 hover:text-white transition-all flex items-center gap-2">
                            <i data-lucide="arrow-left" class="w-4 h-4"></i> Vissza az aktív játékosokhoz
                        </button>
                    </div>

                    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
                        <!-- PLAYER CARD -->
                        <div class="glass-card p-8 rounded-3xl flex flex-col items-center text-center sticky top-24 border-l-4 border-[#FF00D9]">
                            <img id="p-avatar" src="" class="w-32 h-32 rounded-3xl mb-6 shadow-2xl bg-black/40 border border-white/10" alt="Player Skin">
                            <h3 id="p-name" class="text-2xl font-black text-white mb-1">--</h3>
                            <span id="p-rank" class="text-xs font-bold text-[#FF00D9] uppercase tracking-widest bg-[#FF00D9]/10 px-3 py-1 rounded-full border border-[#FF00D9]/30 mb-6">--</span>
                            
                            <!-- ACTION BUTTONS -->
                            <div class="w-full grid grid-cols-2 gap-3 mt-4">
                                <button onclick="openPunishModal('ban')" class="py-3.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 rounded-2xl font-bold text-xs uppercase transition-all flex items-center justify-center gap-1.5"><i data-lucide="ban" class="w-4 h-4"></i> BAN</button>
                                <button onclick="openPunishModal('mute')" class="py-3.5 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 border border-yellow-500/20 rounded-2xl font-bold text-xs uppercase transition-all flex items-center justify-center gap-1.5"><i data-lucide="mic-off" class="w-4 h-4"></i> NÉMÍTÁS</button>
                                <button onclick="openPunishModal('warn')" class="py-3.5 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/20 rounded-2xl font-bold text-xs uppercase transition-all flex items-center justify-center gap-1.5"><i data-lucide="alert-triangle" class="w-4 h-4"></i> WARN</button>
                                <button onclick="openPunishModal('kick')" class="py-3.5 bg-purple-500/10 hover:bg-purple-500/20 text-purple-400 border border-purple-500/20 rounded-2xl font-bold text-xs uppercase transition-all flex items-center justify-center gap-1.5"><i data-lucide="user-x" class="w-4 h-4"></i> KIDOBÁS</button>
                            </div>
                        </div>

                        <!-- HISTORY TIMELINE -->
                        <div class="lg:col-span-2 glass-card p-8 rounded-3xl">
                            <h4 class="font-extrabold text-white text-lg mb-6 flex items-center gap-2">
                                <i data-lucide="history" class="w-5 h-5 text-[#FF00D9]"></i> Szankciós Előzmények Idővonala
                            </h4>
                            <div id="p-history" class="space-y-4 max-h-[600px] overflow-y-auto pr-2"></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- PAGE 4: STAFF HISTORY PANEL (WITH HU PAGINATION) -->
            <div id="page-staff-history" class="page hidden animate__animated animate__fadeIn space-y-8">
                <!-- SEARCH STAFF MEMBER -->
                <div class="flex flex-col sm:flex-row gap-4 items-center justify-between">
                    <div class="max-w-xl w-full glass-card p-4 rounded-3xl flex gap-3">
                        <div class="relative flex-grow">
                            <i data-lucide="shield-search" class="w-5 h-5 absolute left-4 top-4 text-gray-500"></i>
                            <input type="text" id="staff-search-input" placeholder="Staff tag nevének keresése..." onkeypress="if(event.key==='Enter') searchStaffHistory()" class="input-cyber w-full pl-12 pr-4 py-3.5 rounded-2xl text-sm font-semibold">
                        </div>
                        <button onclick="searchStaffHistory()" class="btn-neon px-8 rounded-2xl text-xs uppercase tracking-wider">Keresés</button>
                    </div>
                    <button onclick="loadMyStaffHistory()" class="px-5 py-4 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl text-xs font-bold text-white flex items-center gap-2 transition-all whitespace-nowrap">
                        <i data-lucide="user-check" class="w-4 h-4 text-[#FF00D9]"></i> Saját Staff Előzményeim
                    </button>
                </div>

                <!-- STAFF METRICS CARD & TIMELINE -->
                <div id="staff-history-view" class="space-y-8">
                    <div class="glass-card p-8 rounded-3xl flex flex-col md:flex-row items-center justify-between gap-6 border-l-4 border-[#00F0FF]">
                        <div class="flex items-center gap-6">
                            <img id="st-avatar" src="https://mc-heads.net/avatar/<?= $_SESSION['user_uuid'] ?>/80" class="w-20 h-20 rounded-2xl bg-black/40 border border-white/10 shadow-xl" alt="Staff Head">
                            <div>
                                <div class="flex items-center gap-2 mb-1">
                                    <h3 id="st-name" class="text-2xl font-black text-white">--</h3>
                                    <span id="st-rank" class="text-xs font-bold text-[#FF00D9] uppercase tracking-widest bg-[#FF00D9]/10 px-3 py-1 rounded-full border border-[#FF00D9]/30">STAFF TAG</span>
                                </div>
                                <p class="text-xs text-gray-400">Moderátori Szankciós és Audit Előzmények</p>
                            </div>
                        </div>

                        <!-- METRICS MINI STATS -->
                        <div class="flex gap-3 sm:gap-4">
                            <div class="bg-black/30 p-4 rounded-2xl border border-white/5 text-center min-w-[85px]">
                                <p class="text-[10px] font-bold text-gray-400 uppercase">Összes</p>
                                <h4 id="st-stat-total" class="text-2xl font-extrabold text-white">0</h4>
                            </div>
                            <div class="bg-black/30 p-4 rounded-2xl border border-white/5 text-center min-w-[85px]">
                                <p class="text-[10px] font-bold text-red-400 uppercase">Ban-ok</p>
                                <h4 id="st-stat-bans" class="text-2xl font-extrabold text-red-400">0</h4>
                            </div>
                            <div class="bg-black/30 p-4 rounded-2xl border border-white/5 text-center min-w-[85px]">
                                <p class="text-[10px] font-bold text-yellow-400 uppercase">Némítások</p>
                                <h4 id="st-stat-mutes" class="text-2xl font-extrabold text-yellow-400">0</h4>
                            </div>
                            <div class="bg-black/30 p-4 rounded-2xl border border-white/5 text-center min-w-[85px]">
                                <p class="text-[10px] font-bold text-blue-400 uppercase">Warn-ok</p>
                                <h4 id="st-stat-warns" class="text-2xl font-extrabold text-blue-400">0</h4>
                            </div>
                        </div>
                    </div>

                    <!-- ISSUED PUNISHMENTS TABLE WITH PAGINATION -->
                    <div class="glass-card rounded-3xl overflow-hidden">
                        <div class="p-6 border-b border-white/5 flex items-center justify-between">
                            <h4 class="font-extrabold text-white text-lg flex items-center gap-2">
                                <i data-lucide="shield-check" class="w-5 h-5 text-[#00F0FF]"></i> Kiosztott Szankciók Naplója
                            </h4>
                            <span id="staff-pagination-info" class="text-xs font-bold text-gray-400">1. oldal / 1</span>
                        </div>
                        <table class="w-full text-left border-collapse">
                            <thead class="bg-white/5 text-[11px] uppercase font-bold text-gray-400 tracking-wider">
                                <tr>
                                    <th class="p-5">Játékos</th>
                                    <th class="p-5">Típus</th>
                                    <th class="p-5">Indok</th>
                                    <th class="p-5">Dátum és Idő</th>
                                    <th class="p-5 text-right">Státusz</th>
                                </tr>
                            </thead>
                            <tbody id="staff-table-body" class="divide-y divide-white/5 text-sm"></tbody>
                        </table>
                        <!-- PAGINATION CONTROLS -->
                        <div class="p-4 bg-white/5 border-t border-white/5 flex items-center justify-between">
                            <button id="staff-prev-btn" onclick="changeStaffPage(-1)" class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 disabled:opacity-30 disabled:cursor-not-allowed transition-all flex items-center gap-1">
                                <i data-lucide="chevron-left" class="w-4 h-4"></i> Előző
                            </button>
                            <span id="staff-page-indicator" class="text-xs font-semibold text-gray-400">0 bejegyzés megjelenítve</span>
                            <button id="staff-next-btn" onclick="changeStaffPage(1)" class="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-xs font-bold text-gray-300 disabled:opacity-30 disabled:cursor-not-allowed transition-all flex items-center gap-1">
                                Következő <i data-lucide="chevron-right" class="w-4 h-4"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- PAGE 5: LOCKDOWN CONTROLLER -->
            <div id="page-lockdown" class="page hidden animate__animated animate__fadeIn max-w-4xl mx-auto space-y-8">
                <div class="glass-card p-8 rounded-3xl border-l-4 border-[#FF00D9] relative overflow-hidden space-y-8">
                    <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
                        <div>
                            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold uppercase tracking-wider mb-2">
                                <i data-lucide="shield-alert" class="w-4 h-4"></i> Biztonsági Protokoll Felülbírálás
                            </div>
                            <h3 class="text-2xl font-black text-white">Szerver Karbantartási Zárolás (Lockdown)</h3>
                            <p class="text-xs text-gray-400 mt-1">Nem-staff kapcsolatok korlátozása és a szerver izolálása vészhelyzet esetén</p>
                        </div>
                        
                        <!-- DYNAMIC STATUS BADGE -->
                        <div id="lockdown-status-badge" class="px-5 py-3 rounded-2xl border text-center transition-all flex items-center gap-2 font-black text-xs uppercase tracking-wider">
                            <span id="lockdown-status-dot" class="w-2.5 h-2.5 rounded-full inline-block"></span>
                            <span id="lockdown-status-text">--</span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 pt-4 border-t border-white/5">
                        <!-- TOGGLE SWITCH CARD -->
                        <div class="glass-card p-6 rounded-2xl flex flex-col justify-between items-start">
                            <div>
                                <h4 class="text-sm font-extrabold text-white">Zárolási Állapot</h4>
                                <p class="text-xs text-gray-500 mt-1">Fehérlista alapú kidobás engedélyezése</p>
                            </div>
                            <label class="relative inline-flex items-center cursor-pointer mt-6">
                                <input type="checkbox" id="lockdown-toggle" class="sr-only peer" onchange="toggleLockdown()">
                                <div class="w-16 h-9 bg-white/10 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[4px] after:left-[4px] after:bg-gray-400 after:rounded-full after:h-7 after:w-7 after:transition-all peer-checked:bg-[#FF00D9] peer-checked:after:bg-white shadow-inner"></div>
                            </label>
                        </div>

                        <!-- QUICK PRESET TEMPLATES -->
                        <div class="md:col-span-2 glass-card p-6 rounded-2xl space-y-4">
                            <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Gyors Sablonok</label>
                            <div class="flex flex-wrap gap-2">
                                <button type="button" onclick="setLockdownPreset('Tervezett karbantartás folyamatban. Kérjük tájékozódj a Discord szerverünkön!')" class="px-3.5 py-2.5 bg-white/5 hover:bg-[#FF00D9]/20 hover:border-[#FF00D9]/40 border border-white/10 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5">
                                    <i data-lucide="wrench" class="w-3.5 h-3.5 text-[#FF00D9]"></i> Karbantartás
                                </button>
                                <button type="button" onclick="setLockdownPreset('A szerver jelenleg Anti-Bot védelmi módban van. Próbáld újra később!')" class="px-3.5 py-2.5 bg-white/5 hover:bg-[#FF00D9]/20 hover:border-[#FF00D9]/40 border border-white/10 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5">
                                    <i data-lucide="shield-check" class="w-3.5 h-3.5 text-[#00F0FF]"></i> Anti-Bot Mód
                                </button>
                                <button type="button" onclick="setLockdownPreset('DDoS védelmi pajzs aktív. A csatlakozások ideiglenesen korlátozva.')" class="px-3.5 py-2.5 bg-white/5 hover:bg-[#FF00D9]/20 hover:border-[#FF00D9]/40 border border-white/10 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5">
                                    <i data-lucide="zap" class="w-3.5 h-3.5 text-yellow-400"></i> DDoS Védelem
                                </button>
                                <button type="button" onclick="setLockdownPreset('Vészhelyzeti biztonsági zárolás az Vezetőség által.')" class="px-3.5 py-2.5 bg-white/5 hover:bg-[#FF00D9]/20 hover:border-[#FF00D9]/40 border border-white/10 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5">
                                    <i data-lucide="slash" class="w-3.5 h-3.5 text-red-400"></i> Vészhelyzeti Zárolás
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="space-y-3">
                        <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Kidobó Képernyő Indok Üzenete</label>
                        <textarea id="lockdown-reason" class="input-cyber w-full p-4 rounded-2xl h-32 text-sm font-medium" placeholder="Adja meg az egyedi zárolási üzenetet..."></textarea>
                        <button onclick="updateLockdownReason()" class="btn-neon w-full py-4 rounded-2xl text-xs uppercase tracking-wider flex items-center justify-center gap-2">
                            <i data-lucide="save" class="w-4 h-4"></i> Zárolási Indok Mentése és Alkalmazása
                        </button>
                    </div>
                </div>
            </div>
        </main>

        <!-- EXECUTE PUNISHMENT MODAL -->
        <div id="punish-modal" class="fixed inset-0 z-50 hidden flex items-center justify-center p-6 bg-black/80 backdrop-blur-md">
            <div class="glass-panel w-full max-w-lg p-8 rounded-3xl animate__animated animate__zoomIn border border-[#FF00D9]/30 shadow-2xl shadow-[#FF00D9]/20 space-y-6">
                <!-- MODAL HEADER -->
                <div class="flex justify-between items-center border-b border-white/10 pb-4">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-2xl bg-[#FF00D9]/20 border border-[#FF00D9]/40 flex items-center justify-center">
                            <i data-lucide="gavel" class="w-5 h-5 text-[#FF00D9]"></i>
                        </div>
                        <div>
                            <h3 id="modal-title" class="text-lg font-black uppercase text-white tracking-tight">--</h3>
                            <p id="modal-subtitle" class="text-xs text-gray-400">Szankciós paraméterek beállítása</p>
                        </div>
                    </div>
                    <button onclick="closePunishModal()" class="text-gray-500 hover:text-white p-2 hover:bg-white/10 rounded-xl transition-all">
                        <i data-lucide="x" class="w-5 h-5"></i>
                    </button>
                </div>

                <!-- FORM INPUTS -->
                <div class="space-y-5">
                    <!-- DURATION WITH QUICK PRESETS -->
                    <div id="duration-container" class="space-y-2">
                        <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Időtartam</label>
                        <input type="text" id="m-duration" class="input-cyber w-full p-3.5 rounded-2xl text-sm" placeholder="pl. 1h, 7d, 30d, vagy -1 az örökös szankcióhoz">
                        <div class="flex flex-wrap gap-1.5 pt-1">
                            <button type="button" onclick="setDurationPreset('1h')" class="preset-pill">1 Óra</button>
                            <button type="button" onclick="setDurationPreset('1d')" class="preset-pill">1 Nap</button>
                            <button type="button" onclick="setDurationPreset('7d')" class="preset-pill">7 Nap</button>
                            <button type="button" onclick="setDurationPreset('30d')" class="preset-pill">30 Nap</button>
                            <button type="button" onclick="setDurationPreset('-1')" class="preset-pill text-[#FF00D9] font-black">Örökös (-1)</button>
                        </div>
                    </div>

                    <!-- REASON INPUT -->
                    <div class="space-y-1.5">
                        <label class="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Indok</label>
                        <input type="text" id="m-reason" class="input-cyber w-full p-3.5 rounded-2xl text-sm" placeholder="Szankció indoka...">
                    </div>

                    <!-- SILENT MODE TOGGLE -->
                    <div class="flex items-center justify-between bg-white/5 p-4 rounded-2xl border border-white/5">
                        <div>
                            <p class="text-xs font-bold text-white">Néma Üzenet (-s)</p>
                            <p class="text-[10px] text-gray-500">Publikus globális üzenet elrejtése</p>
                        </div>
                        <input type="checkbox" id="m-silent" class="w-5 h-5 accent-[#FF00D9] rounded">
                    </div>
                </div>

                <!-- MODAL ACTIONS -->
                <div class="grid grid-cols-2 gap-3 pt-2">
                    <button onclick="closePunishModal()" class="py-3.5 bg-white/5 hover:bg-white/10 rounded-2xl text-xs font-bold text-gray-400 hover:text-white transition-all border border-white/10">MÉGSE</button>
                    <button onclick="executePunishment()" class="btn-neon py-3.5 rounded-2xl text-xs uppercase tracking-wider">MEGERŐSÍTÉS ÉS KIOSZTÁS</button>
                </div>
            </div>
        </div>

        <script>
            lucide.createIcons();
            let chart = null, currentTarget = null, currentType = null, activePunishments = [], currentFilter = 'ALL';
            let currentStaffPage = 1, staffPageSize = 10, currentStaffPuns = [];

            function isPunishmentActive(p) {
                if (!p) return false;
                if (p.active === false) return false;
                if (p.type && p.type.toUpperCase().includes('KICK')) return false;
                if (p.duration !== undefined && p.duration !== null && p.duration > 0 && p.duration !== -1) {
                    const expireTime = Number(p.date) + Number(p.duration);
                    if (expireTime <= Date.now()) {
                        return false;
                    }
                }
                return true;
            }

            async function apiFetch(endpoint, params = {}, method = 'GET') {
                const query = new URLSearchParams({proxy: endpoint, ...params}).toString();
                const res = await fetch('index.php?' + query, { method: method });
                const text = await res.text();
                try {
                    return JSON.parse(text);
                } catch(e) {
                    console.error("Failed to parse JSON: " + text);
                    return {error: "Invalid JSON response"};
                }
            }

            function showPage(pageId) {
                document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
                const targetPage = document.getElementById('page-' + pageId);
                if (targetPage) targetPage.classList.remove('hidden');
                
                // Update Desktop & Mobile nav active tabs
                document.querySelectorAll('.nav-tab').forEach(i => i.classList.remove('active'));
                const dtNav = document.getElementById('nav-' + pageId);
                const mbNav = document.getElementById('mob-nav-' + pageId);
                if (dtNav) dtNav.classList.add('active');
                if (mbNav) mbNav.classList.add('active');
                
                const titles = {
                    'dashboard': 'Áttekintő Vezérlőpult',
                    'players': 'Játékoskezelő',
                    'active-punishments': 'Aktív Szankciók',
                    'staff-history': 'Staff Előzmények',
                    'lockdown': 'Szerver Karbantartási Zárolás'
                };
                document.getElementById('header-title').innerText = titles[pageId] || 'Vezérlőpult';

                if(pageId === 'dashboard') loadStats();
                if(pageId === 'players') showOnlinePlayersGrid();
                if(pageId === 'staff-history') loadMyStaffHistory();
                if(pageId === 'lockdown') loadLockdown();
                if(pageId === 'active-punishments') loadActivePunishments();
            }

            async function renderOnlinePlayers() {
                const grid = document.getElementById('online-players-grid');
                if (!grid) return;
                grid.innerHTML = '<p class="text-xs text-gray-500 py-4 col-span-5 text-center font-medium">Aktív játékosok betöltése...</p>';
                
                const activeData = await apiFetch('/punish/active');
                const myName = "<?= htmlspecialchars($user_info['name']) ?>";
                
                let playerMap = new Map();
                playerMap.set(myName.toLowerCase(), { name: myName, rank: "<?= htmlspecialchars($user_info['rank']) ?>", uuid: "<?= $_SESSION['user_uuid'] ?>" });

                if (Array.isArray(activeData)) {
                    activeData.forEach(p => {
                        if (isPunishmentActive(p)) {
                            if (p.target && !playerMap.has(p.target.toLowerCase())) {
                                playerMap.set(p.target.toLowerCase(), { name: p.target, rank: 'JÁTÉKOS', uuid: p.uuid || p.target });
                            }
                            if (p.executor && !playerMap.has(p.executor.toLowerCase())) {
                                playerMap.set(p.executor.toLowerCase(), { name: p.executor, rank: 'STAFF', uuid: p.executor });
                            }
                        }
                    });
                }

                const realPlayers = Array.from(playerMap.values()).slice(0, 10);
                grid.innerHTML = '';

                realPlayers.forEach(p => {
                    grid.innerHTML += `
                        <div onclick="inspectPlayer('${p.name}')" class="glass-card p-5 rounded-2xl flex flex-col items-center text-center cursor-pointer hover:border-[#FF00D9]/50 transition-all group">
                            <div class="relative mb-3">
                                <img src="https://mc-heads.net/avatar/${p.name}/48" class="w-12 h-12 rounded-xl bg-black/40 border border-white/10 group-hover:scale-105 transition-all" alt="${p.name}">
                                <span class="w-3 h-3 rounded-full bg-emerald-400 border-2 border-[#0d0d14] absolute -bottom-1 -right-1"></span>
                            </div>
                            <h4 class="text-xs font-black text-white group-hover:text-[#FF00D9] transition-all truncate w-full">${p.name}</h4>
                            <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest mt-0.5">${p.rank}</span>
                        </div>`;
                });
            }

            function showOnlinePlayersGrid() {
                renderOnlinePlayers();
                document.getElementById('online-players-container').classList.remove('hidden');
                document.getElementById('player-profile-view').classList.add('hidden');
            }

            function inspectPlayer(name) {
                document.getElementById('player-search-input').value = name;
                searchPlayer();
            }

            async function loadStats() {
                const stats = await apiFetch('/stats');
                if (!stats || stats.error) return;
                document.getElementById('stat-total').innerText = stats.bans + stats.mutes + stats.warnings;
                document.getElementById('stat-bans').innerText = stats.bans;
                document.getElementById('stat-mutes').innerText = stats.mutes;
                document.getElementById('stat-warnings').innerText = stats.warnings;
                
                const ctx = document.getElementById('statsChart').getContext('2d');
                if(chart) chart.destroy();
                
                const labels = Array.from({length: 24}, (_, i) => (23-i) + " órája");
                
                let gradient = ctx.createLinearGradient(0, 0, 0, 300);
                gradient.addColorStop(0, 'rgba(255, 0, 217, 0.35)');
                gradient.addColorStop(1, 'rgba(255, 0, 217, 0.0)');

                chart = new Chart(ctx, { 
                    type: 'line', 
                    data: { 
                        labels: labels, 
                        datasets: [{ 
                            data: stats.graph, 
                            borderColor: '#FF00D9', 
                            backgroundColor: gradient, 
                            fill: true, 
                            tension: 0.4, 
                            borderWidth: 3, 
                            pointBackgroundColor: '#00F0FF',
                            pointBorderColor: '#ffffff',
                            pointRadius: 4,
                            pointHoverRadius: 7
                        }] 
                    }, 
                    options: { 
                        responsive: true, 
                        maintainAspectRatio: false, 
                        plugins: { legend: { display: false } }, 
                        scales: { 
                            x: { grid: { display: false }, ticks: { color: '#6b7280', font: { family: 'Plus Jakarta Sans', size: 11 } } }, 
                            y: { grid: { color: 'rgba(255,255,255,0.04)' }, ticks: { color: '#6b7280', stepSize: 1, font: { family: 'Plus Jakarta Sans', size: 11 } } } 
                        } 
                    } 
                });
            }

            async function loadActivePunishments() { 
                activePunishments = await apiFetch('/punish/active'); 
                renderActiveTable(); 
            }

            function filterActive(type) { 
                currentFilter = type; 
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active')); 
                document.getElementById('f-' + type.toLowerCase()).classList.add('active'); 
                renderActiveTable(); 
            }

            function renderActiveTable() {
                const body = document.getElementById('active-table-body'), search = document.getElementById('active-search').value.toLowerCase();
                body.innerHTML = '';
                
                const filtered = activePunishments.filter(p => 
                    isPunishmentActive(p) && 
                    (currentFilter === 'ALL' || p.type.includes(currentFilter)) && 
                    p.target.toLowerCase().includes(search)
                );

                if (filtered.length === 0) { 
                    body.innerHTML = '<tr><td colspan="6" class="p-10 text-center text-gray-500 font-medium">Nincs aktív visszavonható szankció.</td></tr>'; 
                    return; 
                }
                filtered.forEach(p => {
                    let badgeClass = "badge-ban";
                    if (p.type.includes("MUTE")) badgeClass = "badge-mute";
                    else if (p.type.includes("WARN")) badgeClass = "badge-warn";
                    else if (p.type.includes("KICK")) badgeClass = "badge-kick";

                    body.innerHTML += `
                        <tr class="hover:bg-white/5 transition-all">
                            <td class="p-5 flex items-center gap-3 font-semibold text-white">
                                <img src="https://mc-heads.net/avatar/${p.uuid}/28" class="w-7 h-7 rounded-lg border border-white/10" alt="">
                                <span>${p.target}</span>
                            </td>
                            <td class="p-5"><span class="tag-pill ${badgeClass}">${p.type}</span></td>
                            <td class="p-5 text-gray-300 font-medium">${p.reason}</td>
                            <td class="p-5 font-bold text-gray-200">${p.executor}</td>
                            <td class="p-5 text-gray-400 text-xs mono">${new Date(p.date).toLocaleString()}</td>
                            <td class="p-5 text-right">
                                <button onclick="removePunish(${p.id}, true)" class="p-2 hover:bg-red-500/20 text-red-400 rounded-xl transition-all" title="Szankció Visszavonása">
                                    <i data-lucide="unlock" class="w-4 h-4"></i>
                                </button>
                            </td>
                        </tr>`;
                });
                lucide.createIcons();
            }

            async function openPasswordModal() {
                const { value: pass } = await Swal.fire({ 
                    title: 'Jelszó Módosítása', 
                    input: 'password', 
                    inputPlaceholder: 'Írd be az új jelszót...', 
                    showCancelButton: true, 
                    confirmButtonText: 'Mentés',
                    cancelButtonText: 'Mégse',
                    confirmButtonColor: '#FF00D9'
                });
                if (pass) {
                    const res = await apiFetch('/user/set-password', { uuid: '<?= $_SESSION['user_uuid'] ?>', password: pass }, 'POST');
                    if (res.success) Swal.fire({ icon: 'success', title: 'Jelszó sikeresen elmentve!', timer: 1500, showConfirmButton: false });
                    else Swal.fire({ icon: 'error', title: 'Hiba', text: res.error });
                }
            }

            async function searchPlayer() {
                const name = document.getElementById('player-search-input').value.trim();
                if (!name) return;
                const data = await apiFetch('/player/profile', { player: name });
                if (data.error) return Swal.fire({ icon: 'error', title: 'Hiba', text: 'Játékos nem található!' });
                currentTarget = data;
                
                // Hide online players grid when profile is loaded
                document.getElementById('online-players-container').classList.add('hidden');
                
                document.getElementById('p-name').innerText = data.name; 
                document.getElementById('p-rank').innerText = data.rank; 
                document.getElementById('p-avatar').src = `https://mc-heads.net/avatar/${data.uuid}/128`;
                
                const historyBox = document.getElementById('p-history'); 
                historyBox.innerHTML = '';
                if (!data.history || data.history.length === 0) {
                    historyBox.innerHTML = '<p class="text-center text-gray-500 py-6 font-medium">Tiszta előzmény! Ehhez a játékoshoz nem található szankció.</p>';
                } else {
                    data.history.reverse().forEach(p => { 
                        let badgeClass = "badge-ban";
                        if (p.type.includes("MUTE")) badgeClass = "badge-mute";
                        else if (p.type.includes("WARN")) badgeClass = "badge-warn";

                        const active = isPunishmentActive(p);

                        historyBox.innerHTML += `
                            <div class="glass-card p-5 rounded-2xl flex items-center justify-between border-l-4 ${active ? 'border-[#FF00D9]' : 'border-white/10'}">
                                <div>
                                    <div class="flex items-center gap-2 mb-1">
                                        <span class="tag-pill ${badgeClass}">${p.type}</span>
                                        ${active ? '<span class="text-[10px] font-black uppercase text-[#FF00D9] tracking-wider">[AKTÍV]</span>' : '<span class="text-[10px] font-bold text-gray-500 uppercase">[LEJÁRT]</span>'}
                                    </div>
                                    <p class="text-sm font-bold text-white mb-1">${p.reason}</p>
                                    <p class="text-xs text-gray-400 mono">ID: #${p.id} | ${new Date(p.date).toLocaleString()} | Staff: ${p.executor}</p>
                                </div>
                                ${active ? `<button onclick="removePunish(${p.id})" class="p-2 hover:bg-red-500/20 text-red-400 rounded-xl transition-all" title="Visszavonás"><i data-lucide="unlock" class="w-4 h-4"></i></button>` : ''}
                            </div>`; 
                    });
                }
                lucide.createIcons(); 
                document.getElementById('player-profile-view').classList.remove('hidden');
            }

            async function searchStaffHistory() {
                const name = document.getElementById('staff-search-input').value.trim();
                if (name) loadStaffHistory(name);
            }

            function loadMyStaffHistory() {
                const myName = "<?= htmlspecialchars($user_info['name']) ?>";
                document.getElementById('staff-search-input').value = myName;
                loadStaffHistory(myName);
            }

            async function loadStaffHistory(staffName) {
                if (!staffName) return;
                
                const allActive = await apiFetch('/punish/active');
                
                document.getElementById('st-name').innerText = staffName;
                document.getElementById('st-avatar').src = `https://mc-heads.net/avatar/${staffName}/80`;
                
                currentStaffPuns = [];
                if (Array.isArray(allActive)) {
                    currentStaffPuns = allActive.filter(p => isPunishmentActive(p) && p.executor && p.executor.toLowerCase() === staffName.toLowerCase());
                }

                let bans = 0, mutes = 0, warns = 0;
                currentStaffPuns.forEach(p => {
                    if (p.type.includes("BAN")) bans++;
                    else if (p.type.includes("MUTE")) mutes++;
                    else if (p.type.includes("WARN")) warns++;
                });
                
                document.getElementById('st-stat-total').innerText = currentStaffPuns.length;
                document.getElementById('st-stat-bans').innerText = bans;
                document.getElementById('st-stat-mutes').innerText = mutes;
                document.getElementById('st-stat-warns').innerText = warns;

                currentStaffPage = 1;
                renderStaffTablePage();
            }

            function changeStaffPage(delta) {
                const totalPages = Math.max(1, Math.ceil(currentStaffPuns.length / staffPageSize));
                currentStaffPage += delta;
                if (currentStaffPage < 1) currentStaffPage = 1;
                if (currentStaffPage > totalPages) currentStaffPage = totalPages;
                renderStaffTablePage();
            }

            function renderStaffTablePage() {
                const body = document.getElementById('staff-table-body');
                body.innerHTML = '';
                
                const totalEntries = currentStaffPuns.length;
                const totalPages = Math.max(1, Math.ceil(totalEntries / staffPageSize));
                
                if (totalEntries === 0) {
                    body.innerHTML = `<tr><td colspan="5" class="p-10 text-center text-gray-500 font-medium">Nincs mentett aktív szankció ehhez a staff taghoz.</td></tr>`;
                    document.getElementById('staff-pagination-info').innerText = '1. oldal / 1';
                    document.getElementById('staff-page-indicator').innerText = '0 bejegyzés megjelenítve';
                    document.getElementById('staff-prev-btn').disabled = true;
                    document.getElementById('staff-next-btn').disabled = true;
                    return;
                }

                const startIndex = (currentStaffPage - 1) * staffPageSize;
                const endIndex = Math.min(startIndex + staffPageSize, totalEntries);
                const pageItems = currentStaffPuns.slice(startIndex, endIndex);

                pageItems.forEach(p => {
                    let badgeClass = "badge-ban";
                    if (p.type.includes("MUTE")) badgeClass = "badge-mute";
                    else if (p.type.includes("WARN")) badgeClass = "badge-warn";
                    else if (p.type.includes("KICK")) badgeClass = "badge-kick";

                    body.innerHTML += `
                        <tr class="hover:bg-white/5 transition-all">
                            <td class="p-5 flex items-center gap-3 font-semibold text-white">
                                <img src="https://mc-heads.net/avatar/${p.target}/28" class="w-7 h-7 rounded-lg border border-white/10" alt="">
                                <span>${p.target}</span>
                            </td>
                            <td class="p-5"><span class="tag-pill ${badgeClass}">${p.type}</span></td>
                            <td class="p-5 text-gray-300 font-medium">${p.reason}</td>
                            <td class="p-5 text-gray-400 text-xs mono">${new Date(p.date).toLocaleString()}</td>
                            <td class="p-5 text-right">
                                <span class="text-[10px] font-black uppercase text-[#FF00D9] tracking-wider">[AKTÍV]</span>
                            </td>
                        </tr>`;
                });

                document.getElementById('staff-pagination-info').innerText = `${currentStaffPage}. oldal / ${totalPages}`;
                document.getElementById('staff-page-indicator').innerText = `Megjelenítve: ${startIndex + 1}-${endIndex} / ${totalEntries} bejegyzés`;
                document.getElementById('staff-prev-btn').disabled = (currentStaffPage === 1);
                document.getElementById('staff-next-btn').disabled = (currentStaffPage === totalPages);
                
                lucide.createIcons();
            }

            function openPunishModal(type) { 
                currentType = type; 
                document.getElementById('modal-title').innerText = type.toUpperCase() + ' KIOSZTÁSA'; 
                document.getElementById('modal-subtitle').innerText = 'Célpont: ' + (currentTarget ? currentTarget.name : 'Ismeretlen'); 
                document.getElementById('duration-container').style.display = (type == 'ban' || type == 'mute') ? 'block' : 'none'; 
                document.getElementById('punish-modal').classList.remove('hidden'); 
            }
            
            function closePunishModal() { 
                document.getElementById('punish-modal').classList.add('hidden'); 
            }

            function setDurationPreset(val) {
                document.getElementById('m-duration').value = val;
            }

            async function executePunishment() {
                const data = await apiFetch('/punish/execute', { 
                    admin_uuid: '<?= $_SESSION['user_uuid'] ?>', 
                    target: currentTarget.name, 
                    type: currentType, 
                    reason: document.getElementById('m-reason').value, 
                    duration: document.getElementById('m-duration').value, 
                    silent: document.getElementById('m-silent').checked 
                }, 'POST');
                if (data.success) { 
                    Swal.fire({ icon: 'success', title: 'Szankció sikeresen kiosztva!', timer: 1500, showConfirmButton: false }); 
                    closePunishModal(); 
                    searchPlayer(); 
                }
            }

            async function removePunish(id, reloadActive = false) {
                const res = await Swal.fire({ 
                    title: 'Szankció Visszavonása?', 
                    text: "Biztosan vissza szeretnéd vonni ezt a szankciót?", 
                    icon: 'warning', 
                    showCancelButton: true, 
                    confirmButtonText: 'Igen, Visszavonás', 
                    cancelButtonText: 'Mégse',
                    confirmButtonColor: '#FF00D9'
                });
                if (res.isConfirmed) {
                    const data = await apiFetch('/punish/remove', { id: id, admin_uuid: '<?= $_SESSION['user_uuid'] ?>' }, 'POST');
                    if (data.success) { 
                        Swal.fire({ icon: 'success', title: 'Sikeresen visszavonva!', timer: 1500, showConfirmButton: false }); 
                        if (reloadActive) loadActivePunishments(); 
                        else searchPlayer(); 
                    }
                }
            }

            async function toggleLockdown() { 
                const action = document.getElementById('lockdown-toggle').checked ? 'on' : 'off'; 
                await apiFetch('/lockdown', { action: action }, 'POST'); 
                loadLockdown();
            }

            async function updateLockdownReason() { 
                await apiFetch('/lockdown', { action: 'on', reason: document.getElementById('lockdown-reason').value }, 'POST'); 
                Swal.fire({ icon: 'success', title: 'Indok sikeresen elmentve!', timer: 1500, showConfirmButton: false }); 
                loadLockdown();
            }

            function setLockdownPreset(reasonText) {
                document.getElementById('lockdown-reason').value = reasonText;
            }

            async function loadLockdown() { 
                const data = await apiFetch('/lockdown'); 
                const toggle = document.getElementById('lockdown-toggle');
                const statusBadge = document.getElementById('lockdown-status-badge');
                const statusDot = document.getElementById('lockdown-status-dot');
                const statusText = document.getElementById('lockdown-status-text');
                
                toggle.checked = data.enabled; 
                document.getElementById('lockdown-reason').value = data.reason || ''; 
                
                if (data.enabled) {
                    statusBadge.className = "px-5 py-3 rounded-2xl border bg-red-500/20 border-red-500/50 text-red-400 shadow-lg shadow-red-500/20 animate-pulse flex items-center gap-2 font-black text-xs uppercase tracking-wider";
                    statusDot.className = "w-2.5 h-2.5 rounded-full bg-red-500 animate-ping inline-block";
                    statusText.innerText = "ZÁROLÁS AKTÍV";
                } else {
                    statusBadge.className = "px-5 py-3 rounded-2xl border bg-emerald-500/10 border-emerald-500/30 text-emerald-400 flex items-center gap-2 font-black text-xs uppercase tracking-wider";
                    statusDot.className = "w-2.5 h-2.5 rounded-full bg-emerald-400 pulse-online inline-block";
                    statusText.innerText = "SZERVEREM ÜZEMEL";
                }
            }

            window.onload = loadStats;
        </script>
    <?php endif; ?>
</body>
</html>