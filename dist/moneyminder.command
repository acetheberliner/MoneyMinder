#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/MoneyMinder.jar"

# Prova JDK con JavaFX incluso (Liberica Full)
if java --add-modules javafx.controls,javafx.fxml -jar "$JAR"; then
  exit 0
fi

# Fallback: OpenJFX da Homebrew
JFX="$(brew --prefix openjfx 2>/dev/null)/libexec/openjfx/lib"
if [ -d "$JFX" ]; then
  exec java --module-path "$JFX" \
       --add-modules javafx.controls,javafx.fxml \
       -jar "$JAR"
fi

echo "⚠️ JavaFX non trovato. Installa:
  - brew tap bell-sw/liberica && brew install --cask liberica-jdk17-full
  oppure
  - brew install --cask temurin17 && brew install openjfx"
exit 1
