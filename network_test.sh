#!/bin/bash
echo "=== ICMP 测试 ==="
ping -c 4 json.schemastore.org

echo "\n=== TCP 443 端口测试 ==="
telnet json.schemastore.org 443

echo "\n=== HTTPS 握手测试 ==="
openssl s_client -connect json.schemastore.org:443 -servername json.schemastore.org -showcerts
