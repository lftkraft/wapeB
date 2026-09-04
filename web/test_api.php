<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header('Content-Type: text/html; charset=utf-8');

$ip = "94.156.37.198";
$port = 25571;
$apiKey = "trpzw345uwe49txfgbOhdG_DFGDVCXAer432";
$url = "http://$ip:$port/api/stats";

echo "<h2>wapeB API Kapcsolat Tesztelő</h2>";
echo "<p><strong>Cél szerver:</strong> $ip:$port</p>";
echo "<p><strong>Teszt végpont:</strong> $url</p>";
echo "<hr>";

$startTime = microtime(true);
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-API-Key: $apiKey"]);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
curl_setopt($ch, CURLOPT_TIMEOUT, 6);

$response = curl_exec($ch);
$error = curl_error($ch);
$errno = curl_errno($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$totalTime = round((microtime(true) - $startTime) * 1000, 2);

if ($response !== false && $httpCode === 200) {
    echo "<h3 style='color: green;'> SIKERES KAPCSOLAT! (Válaszidő: {$totalTime} ms)</h3>";
    echo "<p><strong>HTTP Kód:</strong> $httpCode OK</p>";
    echo "<p><strong>Szerver válasza:</strong></p>";
    echo "<pre style='background: #111; color: #0f0; padding: 15px; border-radius: 8px;'>" . htmlspecialchars($response) . "</pre>";
    echo "<p style='color: green; font-weight: bold;'>A weboldal mostantól tökéletesen működik, beléphetsz a wapeB felületre!</p>";
} else {
    echo "<h3 style='color: red;'> SIKERTELEN KAPCSOLAT</h3>";
    echo "<p><strong>HTTP Kód:</strong> $httpCode</p>";
    echo "<p><strong>cURL Hibakód:</strong> $errno</p>";
    echo "<p><strong>cURL Hibaüzenet:</strong> " . ($error ? htmlspecialchars($error) : 'Nincs hibaüzenet') . "</p>";
    if ($errno == 28) {
        echo "<p style='color: orange;'><strong>Ok:</strong> Időtúllépés (Timeout) - a szerver tűzfala még nem engedte át a 91.227.139.94 IP-címet, vagy a port még zárva van.</p>";
    } elseif ($errno == 7) {
        echo "<p style='color: orange;'><strong>Ok:</strong> Kapcsolat elutasítva (Connection refused) - a célgépen ezen a porton nem válaszol semmi.</p>";
    }
}
