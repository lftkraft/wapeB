<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

function getExternalIP() {
    $services = [
        'https://api.ipify.org',
        'https://ifconfig.me/ip',
        'https://icanhazip.com',
        'https://checkip.amazonaws.com'
    ];

    foreach ($services as $service) {
        if (function_exists('curl_init')) {
            $ch = curl_init($service);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_TIMEOUT, 5);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            $ip = curl_exec($ch);
            curl_close($ch);
            if ($ip && filter_var(trim($ip), FILTER_VALIDATE_IP)) {
                return trim($ip);
            }
        }
    }

    if (isset($_SERVER['SERVER_ADDR'])) {
        return $_SERVER['SERVER_ADDR'];
    }

    return "Nem sikerült lekérni (nézd meg a cPanel/tárhely panelen)";
}

$ip = getExternalIP();
echo "<h2>A webtárhelyed kimenő IP címe: <span style='color: green;'>$ip</span></h2>";
if (isset($_SERVER['SERVER_ADDR'])) {
    echo "<p>Belső szerver IP: " . $_SERVER['SERVER_ADDR'] . "</p>";
}
