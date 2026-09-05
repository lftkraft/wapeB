<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header('Content-Type: text/html; charset=utf-8');

$ip = "94.156.37.198";
$port = 25571;
$apiKey = "trpzw345uwe49txfgbOhdG_DFGDVCXAer432";
$url = "http://$ip:$port/api/stats";

echo "<h2>wapeB API Connection Tester</h2>";
echo "<p><strong>Target Server:</strong> $ip:$port</p>";
echo "<p><strong>Test Endpoint:</strong> $url</p>";
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
    echo "<h3 style='color: green;'> CONNECTION SUCCESSFUL! (Response time: {$totalTime} ms)</h3>";
    echo "<p><strong>HTTP Code:</strong> $httpCode OK</p>";
    echo "<p><strong>Server Response:</strong></p>";
    echo "<pre style='background: #111; color: #0f0; padding: 15px; border-radius: 8px;'>" . htmlspecialchars($response) . "</pre>";
    echo "<p style='color: green; font-weight: bold;'>The dashboard can now successfully communicate with the wapeB plugin!</p>";
} else {
    echo "<h3 style='color: red;'> CONNECTION FAILED</h3>";
    echo "<p><strong>HTTP Code:</strong> $httpCode</p>";
    echo "<p><strong>cURL Error Code:</strong> $errno</p>";
    echo "<p><strong>cURL Error Message:</strong> " . ($error ? htmlspecialchars($error) : 'No error message') . "</p>";
    if ($errno == 28) {
        echo "<p style='color: orange;'><strong>Cause:</strong> Connection Timeout - the server firewall hasn't allowed the IP or port $port is closed.</p>";
    } elseif ($errno == 7) {
        echo "<p style='color: orange;'><strong>Cause:</strong> Connection Refused - nothing is listening on port $port on the target server.</p>";
    }
}
