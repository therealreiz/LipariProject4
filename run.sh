#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
#  LipariBank Broken Project — Day 4
#  Build con Maven e avvio
# ─────────────────────────────────────────────────────────────────────────────

set -e

echo "==> Build Maven (fat-jar con H2 incluso)..."
mvn -q clean package -DskipTests

echo "==> Avvio applicazione..."
echo "─────────────────────────────────────────────────────────────────────────────"
java -jar target/liparibank-broken-day4-1.0-SNAPSHOT.jar
