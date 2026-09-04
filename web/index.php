<?php
session_start();
header('Content-Type: text/html; charset=utf-8');

// --- CONFIGURATION ---
$api_ip = "94.156.37.198";        // Minecraft szerver közvetlen IP (hu-1.balkercraft.eu)
$api_port = 25571;               // Plugin config port
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
        $msg = "Nem sikerült kapcsolódni a Minecraft szerverhez ($api_ip:$api_port). ";
        if ($curlErrno === 28) {
            $msg .= "Időtúllépés történt (Timeout). Valószínűleg a port ($api_port) nincs megnyitva vagy a tűzfal blokkolja!";
        } elseif ($curlErrno === 7) {
            $msg .= "A kapcsolat elutasítva (Connection refused). Ellenőrizd, hogy a szerver és a Web-API fut-e ezen a porton ($api_port)!";
        } elseif (!empty($curlError)) {
            $msg .= "cURL hiba (#$curlErrno): $curlError";
        } else {
            $msg .= "A szerver nem elérhető.";
        }
        return ['code' => 0, 'data' => ['error' => $msg], 'error' => $msg];
    }

    $data = json_decode($response, true);
    if ($data === null && !empty($response)) {
        return ['code' => $httpCode, 'data' => ['error' => 'Érvénytelen válasz a szervertől: ' . substr(strip_tags($response), 0, 100)], 'error' => 'Érvénytelen válasz a szervertől.'];
    }

    return ['code' => $httpCode, 'data' => $data, 'error' => ($httpCode != 200 ? ($data['error'] ?? "HTTP hiba kód: $httpCode") : null)];
}

if (isset($_GET['proxy'])) {
    if (!isset($_SESSION['user_uuid'])) {
        header('Content-Type: application/json');
        echo json_encode(['error' => 'Not authenticated']);
        exit;
    }
    $res = callAPI($_GET['proxy'], array_diff_key($_GET, ['proxy' => '']), $_SERVER['REQUEST_METHOD']);
    header('Content-Type: application/json');
    http_response_code($res['code'] > 0 ? $res['code'] : 502);
    echo json_encode($res['data'] ?? ['error' => $res['error'] ?? 'Hiba a kérés feldolgozásakor']);
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
            $error = $res['data']['error'] ?? ($res['error'] ?? "Hiba történt a kapcsolódáskor!");
        }
    } elseif (isset($_POST['login_code']) && $step == 2) {
        $res = callAPI("/login/verify", ["player" => $_SESSION['pending_player'], "code" => $_POST['login_code']]);
        if ($res['code'] == 200) {
            $_SESSION['user_uuid'] = $res['data']['uuid'];
            unset($_SESSION['pending_player']);
            header("Location: index.php");
            exit;
        } else {
            $error = $res['data']['error'] ?? "Helytelen vagy lejárt kód!";
        }
    } elseif (isset($_POST['login_password']) && $step == 3) {
        $res = callAPI("/login/password", ["player" => $_POST['target_player'], "password" => $_POST['login_password']]);
        if ($res['code'] == 200) {
            $_SESSION['user_uuid'] = $res['data']['uuid'];
            header("Location: index.php");
            exit;
        } else {
            $error = $res['data']['error'] ?? "Helytelen jelszó vagy felhasználónév!";
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
<html lang="hu">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>wapeB Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css" rel="stylesheet">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap');
        body { background: #070708; color: #e2e2e7; font-family: 'Plus Jakarta Sans', sans-serif; }
        .glass { background: rgba(255, 255, 255, 0.02); backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.05); border-radius: 24px; }
        .glass-card { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.06); border-radius: 20px; transition: all 0.3s ease; }
        .btn-gradient { background: linear-gradient(135deg, #FF00D9 0%, #B300FF 100%); transition: all 0.3s; }
        .input-dark { background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08); outline: none; }
        .sidebar-item { border-radius: 12px; transition: all 0.2s; color: #8a8a93; }
        .sidebar-item.active { background: rgba(255, 0, 217, 0.1); color: #FF00D9; }
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.1); border-radius: 10px; }
        .swal2-popup { background: #0f0f11 !important; border: 1px solid rgba(255,255,255,0.1) !important; border-radius: 24px !important; color: #fff !important; }
        .filter-btn { padding: 8px 16px; border-radius: 12px; font-size: 12px; font-weight: bold; transition: all 0.2s; border: 1px solid rgba(255,255,255,0.05); color: #8a8a93; }
        .filter-btn.active { background: #FF00D9; color: white; border-color: #FF00D9; }
    </style>
</head>
<body class="min-h-screen overflow-x-hidden">
    <?php if (!$user_info): ?>
        <div class="flex items-center justify-center h-screen">
            <div class="glass p-10 w-full max-w-md animate__animated animate__fadeInDown">
                <div class="text-center mb-10">
                    <div class="w-16 h-16 bg-gradient-to-tr from-[#FF00D9] to-[#B300FF] rounded-2xl mx-auto mb-6 flex items-center justify-center shadow-2xl rotate-12">
                        <i data-lucide="shield-check" class="text-white w-8 h-8"></i>
                    </div>
                    <h1 class="text-3xl font-black mb-1 text-white">wape<span class="text-[#FF00D9]">B</span></h1>
                    <p class="text-gray-500 text-[10px] font-bold uppercase tracking-widest">Admin Control Panel</p>
                </div>
                <form method="POST" class="space-y-6">
                    <?php if ($error): ?><div class="p-4 bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-bold rounded-xl text-center leading-relaxed"><?= htmlspecialchars($error) ?></div><?php endif; ?>
                    <?php if ($step == 1): ?>
                        <div class="space-y-2">
                            <label class="text-[10px] font-bold text-gray-500 uppercase tracking-widest ml-1">Játékosnév</label>
                            <input type="text" name="login_username" required placeholder="Pl. Azuyo" class="input-dark w-full p-4 rounded-xl text-white">
                        </div>
                        <div class="flex gap-3">
                            <button type="submit" class="btn-gradient flex-grow p-4 rounded-xl font-bold text-xs uppercase text-white shadow-xl shadow-[#FF00D9]/20">Kód kérése</button>
                            <a href="?mode=password" class="p-4 bg-white/5 rounded-xl hover:bg-white/10 transition-all border border-white/5"><i data-lucide="key" class="w-5 h-5 text-gray-400"></i></a>
                        </div>
                    <?php elseif ($step == 2): ?>
                        <div class="space-y-2 text-center">
                            <label class="text-[10px] font-bold text-gray-500 uppercase tracking-widest">6 jegyű kód</label>
                            <input type="text" name="login_code" required maxlength="6" autofocus placeholder="000000" class="input-dark w-full p-5 rounded-xl text-center text-3xl font-black tracking-[0.2em] text-[#FF00D9]">
                        </div>
                        <button type="submit" class="btn-gradient w-full p-4 rounded-xl font-bold text-xs uppercase text-white shadow-xl">Belépés</button>
                        <div class="flex justify-between items-center px-2">
                            <a href="index.php" class="text-[10px] text-gray-600 hover:text-white uppercase font-bold transition-all">Vissza</a>
                            <a href="?mode=password" class="text-[10px] text-gray-600 hover:text-white uppercase font-bold transition-all">Inkább jelszóval</a>
                        </div>
                    <?php elseif ($step == 3): ?>
                        <div class="space-y-4">
                            <div>
                                <label class="text-[10px] font-bold text-gray-500 uppercase tracking-widest ml-1">Játékosnév</label>
                                <input type="text" name="target_player" required placeholder="Játékosnév" class="input-dark w-full p-4 rounded-xl text-white">
                            </div>
                            <div>
                                <label class="text-[10px] font-bold text-gray-500 uppercase tracking-widest ml-1">Jelszó</label>
                                <input type="password" name="login_password" required placeholder="••••••••" class="input-dark w-full p-4 rounded-xl text-white">
                            </div>
                        </div>
                        <button type="submit" class="btn-gradient w-full p-4 rounded-xl font-bold text-xs uppercase text-white shadow-xl">Bejelentkezés</button>
                        <a href="index.php" class="block text-center text-[10px] text-gray-600 hover:text-white uppercase font-bold transition-all">Vissza a kódhoz</a>
                    <?php endif; ?>
                </form>
            </div>
        </div>
    <?php else: ?>
        <div class="flex h-screen overflow-hidden">
            <!-- Sidebar -->
            <div class="w-72 glass m-4 mr-0 p-6 flex flex-col">
                <div class="flex items-center gap-3 mb-10"><span class="text-xl font-black tracking-tighter uppercase">wape<span class="text-[#FF00D9]">B</span></span></div>
                <div class="space-y-2 flex-grow">
                    <button onclick="showPage('dashboard')" id="nav-dashboard" class="sidebar-item active w-full flex items-center gap-4 p-4 font-semibold text-sm"><i data-lucide="layout-dashboard" class="w-5 h-5"></i> Dashboard</button>
                    <button onclick="showPage('players')" id="nav-players" class="sidebar-item w-full flex items-center gap-4 p-4 font-semibold text-sm"><i data-lucide="users" class="w-5 h-5"></i> Játékosok</button>
                    <button onclick="showPage('active-punishments')" id="nav-active-punishments" class="sidebar-item w-full flex items-center gap-4 p-4 font-semibold text-sm"><i data-lucide="gavel" class="w-5 h-5"></i> Aktív Szankciók</button>
                    <button onclick="showPage('lockdown')" id="nav-lockdown" class="sidebar-item w-full flex items-center gap-4 p-4 font-semibold text-sm"><i data-lucide="lock" class="w-5 h-5"></i> Lockdown</button>
                </div>
                <div class="flex flex-col gap-3 mt-auto">
                    <div class="glass-card p-4 flex items-center gap-3 group">
                        <img src="https://mc-heads.net/avatar/<?= $_SESSION['user_uuid'] ?>/48" class="w-10 h-10 rounded-xl" alt="">
                        <div class="overflow-hidden flex-grow">
                            <p class="text-xs font-bold truncate"><?= htmlspecialchars($user_info['name']) ?></p>
                            <p class="text-[9px] uppercase tracking-widest text-[#FF00D9] font-bold"><?= htmlspecialchars($user_info['rank']) ?></p>
                        </div>
                        <button onclick="openPasswordModal()" class="p-2 hover:bg-white/10 rounded-lg transition-all opacity-0 group-hover:opacity-100 text-gray-400 hover:text-white"><i data-lucide="settings" class="w-4 h-4"></i></button>
                    </div>
                    <a href="?logout" class="p-4 rounded-xl bg-white/5 hover:bg-red-500/10 hover:text-red-500 transition-all text-xs font-bold flex items-center gap-3 justify-center"><i data-lucide="log-out" class="w-4 h-4"></i> Kijelentkezés</a>
                </div>
            </div>

            <!-- Content Area -->
            <div class="flex-grow p-8 overflow-y-auto">
                <div id="page-dashboard" class="page animate__animated animate__fadeIn">
                    <h2 class="text-3xl font-black mb-8">Statisztika</h2>
                    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10 text-center">
                        <div class="glass-card p-6 border-l-4 border-[#FF00D9]"><p class="text-[10px] font-bold text-gray-500 uppercase mb-2">Összesen</p><h3 id="stat-total" class="text-3xl font-black">--</h3></div>
                        <div class="glass-card p-6 border-l-4 border-red-500"><p class="text-[10px] font-bold text-gray-500 uppercase mb-2">Bans</p><h3 id="stat-bans" class="text-3xl font-black">--</h3></div>
                        <div class="glass-card p-6 border-l-4 border-yellow-500"><p class="text-[10px] font-bold text-gray-500 uppercase mb-2">Mutes</p><h3 id="stat-mutes" class="text-3xl font-black">--</h3></div>
                        <div class="glass-card p-6 border-l-4 border-blue-500"><p class="text-[10px] font-bold text-gray-500 uppercase mb-2">Warns</p><h3 id="stat-warnings" class="text-3xl font-black">--</h3></div>
                    </div>
                    <div class="glass-card p-8"><h3 class="font-bold mb-8">Aktivitás (Elmúlt 24 óra)</h3><div class="h-80 w-full"><canvas id="statsChart"></canvas></div></div>
                </div>

                <div id="page-active-punishments" class="page hidden animate__animated animate__fadeIn">
                    <h2 class="text-3xl font-black mb-8">Aktív Szankciók</h2>
                    <div class="flex flex-wrap gap-4 mb-8 items-center">
                        <div class="flex bg-white/5 p-1 rounded-xl">
                            <button onclick="filterActive('ALL')" class="filter-btn active" id="f-all">Összes</button>
                            <button onclick="filterActive('BAN')" class="filter-btn" id="f-ban">Kitiltások</button>
                            <button onclick="filterActive('MUTE')" class="filter-btn" id="f-mute">Némítások</button>
                            <button onclick="filterActive('WARN')" class="filter-btn" id="f-warn">Figyelmeztetések</button>
                        </div>
                        <input type="text" id="active-search" oninput="renderActiveTable()" placeholder="Keresés játékosra..." class="input-dark p-3 px-5 rounded-xl text-sm flex-grow max-w-xs">
                    </div>
                    <div class="glass-card overflow-hidden">
                        <table class="w-full text-left">
                            <thead class="bg-white/5 text-[10px] uppercase font-bold text-gray-500 tracking-widest"><tr><th class="p-5">Játékos</th><th class="p-5">Típus</th><th class="p-5">Indok</th><th class="p-5">Admin</th><th class="p-5">Dátum</th><th class="p-5 text-right">Művelet</th></tr></thead>
                            <tbody id="active-table-body" class="text-sm"></tbody>
                        </table>
                    </div>
                </div>

                <div id="page-players" class="page hidden animate__animated animate__fadeIn">
                    <h2 class="text-3xl font-black mb-8">Játékoskezelés</h2>
                    <div class="max-w-xl glass-card p-4 flex gap-3 mb-10"><input type="text" id="player-search-input" placeholder="Játékos neve..." class="input-dark flex-grow p-4 rounded-xl"><button onclick="searchPlayer()" class="btn-gradient px-8 rounded-xl font-bold text-xs uppercase">Keresés</button></div>
                    <div id="player-profile-view" class="hidden animate__animated animate__fadeInUp">
                        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
                            <div class="glass-card p-8 flex flex-col items-center sticky top-0">
                                <img id="p-avatar" src="" class="w-32 h-32 rounded-3xl mb-6 shadow-2xl bg-white/5" alt="">
                                <h3 id="p-name" class="text-2xl font-black mb-1">--</h3>
                                <p id="p-rank" class="text-xs font-bold text-[#FF00D9] uppercase tracking-widest mb-6">--</p>
                                <div class="w-full grid grid-cols-2 gap-3 mt-6">
                                    <button onclick="openPunishModal('ban')" class="p-4 bg-red-500/10 text-red-500 rounded-xl font-bold text-[10px] uppercase hover:bg-red-500/20 transition-all">BAN</button>
                                    <button onclick="openPunishModal('mute')" class="p-4 bg-yellow-500/10 text-yellow-500 rounded-xl font-bold text-[10px] uppercase hover:bg-yellow-500/20 transition-all">MUTE</button>
                                    <button onclick="openPunishModal('warn')" class="p-4 bg-blue-500/10 text-blue-500 rounded-xl font-bold text-[10px] uppercase hover:bg-blue-500/20 transition-all">WARN</button>
                                    <button onclick="openPunishModal('kick')" class="p-4 bg-white/10 text-white rounded-xl font-bold text-[10px] uppercase hover:bg-white/20 transition-all">KICK</button>
                                </div>
                            </div>
                            <div class="lg:col-span-2 glass-card p-8"><h4 class="font-bold mb-6 flex items-center gap-2"><i data-lucide="history" class="w-4 h-4"></i> Büntetési Előzmények</h4><div id="p-history" class="space-y-4 max-h-[600px] overflow-y-auto pr-4"></div></div>
                        </div>
                    </div>
                </div>

                <div id="page-lockdown" class="page hidden animate__animated animate__fadeIn">
                    <h2 class="text-3xl font-black mb-8">Lockdown Vezérlő</h2>
                    <div class="max-w-md glass-card p-10">
                        <div class="flex items-center justify-between mb-8"><h3 class="font-bold">Státusz</h3><label class="relative inline-flex items-center cursor-pointer"><input type="checkbox" id="lockdown-toggle" class="sr-only peer" onchange="toggleLockdown()"><div class="w-14 h-8 bg-white/10 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[4px] after:left-[4px] after:bg-gray-400 after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-[#FF00D9] peer-checked:after:bg-white"></div></label></div>
                        <div class="space-y-4"><label class="text-[10px] font-bold text-gray-500 uppercase tracking-widest">Indok</label><textarea id="lockdown-reason" class="input-dark w-full p-4 rounded-xl h-32" placeholder="Karbantartás..."></textarea><button onclick="updateLockdownReason()" class="w-full p-4 btn-gradient rounded-xl font-bold text-xs uppercase text-white shadow-xl">Mentés</button></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- PUNISH MODAL -->
        <div id="punish-modal" class="fixed inset-0 z-50 hidden flex items-center justify-center p-6 bg-black/80 backdrop-blur-md">
            <div class="glass w-full max-w-lg p-10 animate__animated animate__zoomIn">
                <div class="flex justify-between items-center mb-8"><h3 id="modal-title" class="text-2xl font-black uppercase text-[#FF00D9]">--</h3><button onclick="closePunishModal()" class="text-gray-500 hover:text-white"><i data-lucide="x"></i></button></div>
                <div class="space-y-6">
                    <div id="duration-container"><label class="text-[10px] font-bold text-gray-500 uppercase">Időtartam (pl. 1h, 7d, -1)</label><input type="text" id="m-duration" class="input-dark w-full p-4 rounded-xl mt-2 text-white"></div>
                    <div><label class="text-[10px] font-bold text-gray-500 uppercase">Indok</label><input type="text" id="m-reason" class="input-dark w-full p-4 rounded-xl mt-2 text-white" placeholder="Indok..."></div>
                    <div class="flex items-center justify-between bg-white/5 p-4 rounded-xl"><label class="text-sm font-bold">Silent Mode</label><input type="checkbox" id="m-silent" class="w-5 h-5 accent-[#FF00D9]"></div>
                    <button onclick="executePunishment()" class="btn-gradient w-full p-5 rounded-2xl font-bold text-white shadow-xl">VÉGREHAJTÁS</button>
                </div>
            </div>
        </div>

        <script>
            lucide.createIcons();
            let chart = null, currentTarget = null, currentType = null, activePunishments = [], currentFilter = 'ALL';

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
                document.getElementById('page-' + pageId).classList.remove('hidden');
                document.querySelectorAll('.sidebar-item').forEach(i => i.classList.remove('active'));
                document.getElementById('nav-' + pageId).classList.add('active');
                if(pageId === 'dashboard') loadStats();
                if(pageId === 'lockdown') loadLockdown();
                if(pageId === 'active-punishments') loadActivePunishments();
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
                const labels = Array.from({length: 24}, (_, i) => (23-i) + "ó");
                chart = new Chart(ctx, { type: 'line', data: { labels: labels, datasets: [{ data: stats.graph, borderColor: '#FF00D9', backgroundColor: 'rgba(255, 0, 217, 0.1)', fill: true, tension: 0.4, borderWidth: 3, pointRadius: 4 }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { display: false }, ticks: { color: '#555' } }, y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#555', stepSize: 1 } } } } });
            }

            async function loadActivePunishments() { activePunishments = await apiFetch('/punish/active'); renderActiveTable(); }
            function filterActive(type) { currentFilter = type; document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active')); document.getElementById('f-' + type.toLowerCase()).classList.add('active'); renderActiveTable(); }
            function renderActiveTable() {
                const body = document.getElementById('active-table-body'), search = document.getElementById('active-search').value.toLowerCase();
                body.innerHTML = '';
                const filtered = activePunishments.filter(p => (currentFilter === 'ALL' || p.type.includes(currentFilter)) && p.target.toLowerCase().includes(search));
                if (filtered.length === 0) { body.innerHTML = '<tr><td colspan="6" class="p-10 text-center text-gray-500">Nincs találat.</td></tr>'; return; }
                filtered.forEach(p => {
                    const badge = p.type.includes("BAN") ? "bg-red-500/10 text-red-500" : (p.type.includes("MUTE") ? "bg-yellow-500/10 text-yellow-500" : "bg-blue-500/10 text-blue-500");
                    body.innerHTML += `<tr class="border-b border-white/5 hover:bg-white/5 transition-all"><td class="p-5 flex items-center gap-3"><img src="https://mc-heads.net/avatar/${p.uuid}/24" class="w-6 h-6 rounded-md font-bold"><span>${p.target}</span></td><td class="p-5"><span class="px-3 py-1 rounded-full text-[10px] font-bold ${badge}">${p.type}</span></td><td class="p-5 text-gray-400 font-medium">${p.reason}</td><td class="p-5 font-bold">${p.executor}</td><td class="p-5 text-gray-500 text-xs">${new Date(p.date).toLocaleString()}</td><td class="p-5 text-right"><button onclick="removePunish(${p.id}, true)" class="p-2 hover:bg-red-500/20 text-red-500 rounded-lg transition-all"><i data-lucide="unlock" class="w-4 h-4"></i></button></td></tr>`;
                });
                lucide.createIcons();
            }

            async function openPasswordModal() {
                const { value: pass } = await Swal.fire({ title: 'Jelszó módosítása', input: 'password', inputPlaceholder: 'Új jelszó...', showCancelButton: true, confirmButtonText: 'Mentés' });
                if (pass) {
                    const res = await apiFetch('/user/set-password', { uuid: '<?= $_SESSION['user_uuid'] ?>', password: pass }, 'POST');
                    if (res.success) Swal.fire({ icon: 'success', title: 'Sikeresen mentve!', timer: 1500, showConfirmButton: false });
                    else Swal.fire({ icon: 'error', title: 'Hiba', text: res.error });
                }
            }

            async function searchPlayer() {
                const name = document.getElementById('player-search-input').value;
                if (!name) return;
                const data = await apiFetch('/player/profile', { player: name });
                if (data.error) return Swal.fire({ icon: 'error', title: 'Hiba', text: 'A játékos nem található!' });
                currentTarget = data;
                document.getElementById('p-name').innerText = data.name; document.getElementById('p-rank').innerText = data.rank; document.getElementById('p-avatar').src = `https://mc-heads.net/avatar/${data.uuid}/128`;
                const historyBox = document.getElementById('p-history'); historyBox.innerHTML = '';
                data.history.reverse().forEach(p => { historyBox.innerHTML += `<div class="glass-card p-5 flex items-center justify-between border-l-4 ${p.active ? 'border-[#FF00D9]' : 'border-white/10'}"><div><p class="text-[10px] font-bold uppercase ${p.active ? 'text-[#FF00D9]' : 'text-gray-500'}">${p.type}</p><p class="text-sm font-bold text-white mb-1">${p.reason}</p><p class="text-[9px] text-gray-500">ID: #${p.id} • ${new Date(p.date).toLocaleString()} • Admin: ${p.executor}</p></div>${p.active ? `<button onclick="removePunish(${p.id})" class="text-gray-600 hover:text-red-500 transition-all"><i data-lucide="unlock" class="w-4 h-4"></i></button>` : ''}</div>`; });
                lucide.createIcons(); document.getElementById('player-profile-view').classList.remove('hidden');
            }

            function openPunishModal(type) { currentType = type; document.getElementById('modal-title').innerText = type + ' végrehajtása'; document.getElementById('duration-container').style.display = (type == 'ban' || type == 'mute') ? 'block' : 'none'; document.getElementById('punish-modal').classList.remove('hidden'); }
            function closePunishModal() { document.getElementById('punish-modal').classList.add('hidden'); }

            async function executePunishment() {
                const data = await apiFetch('/punish/execute', { admin_uuid: '<?= $_SESSION['user_uuid'] ?>', target: currentTarget.name, type: currentType, reason: document.getElementById('m-reason').value, duration: document.getElementById('m-duration').value, silent: document.getElementById('m-silent').checked }, 'POST');
                if (data.success) { Swal.fire({ icon: 'success', title: 'Sikeres!', timer: 1500, showConfirmButton: false }); closePunishModal(); searchPlayer(); }
            }

            async function removePunish(id, reloadActive = false) {
                const res = await Swal.fire({ title: 'Feloldás?', text: "Biztosan feloldod?", icon: 'warning', showCancelButton: true, confirmButtonText: 'Igen', cancelButtonText: 'Mégse' });
                if (res.isConfirmed) {
                    const data = await apiFetch('/punish/remove', { id: id, admin_uuid: '<?= $_SESSION['user_uuid'] ?>' }, 'POST');
                    if (data.success) { Swal.fire({ icon: 'success', title: 'Feloldva!', timer: 1500, showConfirmButton: false }); if (reloadActive) loadActivePunishments(); else searchPlayer(); }
                }
            }

            async function toggleLockdown() { const action = document.getElementById('lockdown-toggle').checked ? 'on' : 'off'; await apiFetch('/lockdown', { action: action }, 'POST'); }
            async function updateLockdownReason() { await apiFetch('/lockdown', { action: 'on', reason: document.getElementById('lockdown-reason').value }, 'POST'); Swal.fire({ icon: 'success', title: 'Mentve!', timer: 1500, showConfirmButton: false }); }
            async function loadLockdown() { const data = await apiFetch('/lockdown'); document.getElementById('lockdown-toggle').checked = data.enabled; document.getElementById('lockdown-reason').value = data.reason; }

            window.onload = loadStats;
        </script>
    <?php endif; ?>
</body>
</html>