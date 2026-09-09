#!/bin/sh
set -e  # fail fast

# -------------------------------
# Safe branch detection
# -------------------------------
if [ -d ".git" ]; then
  BRANCH="$(git branch --show-current)"
else
  BRANCH="${BRANCH_NAME:-no-git}"
fi

echo "Main Branch: $BRANCH"

INTERNALS="micro-ui-internals"

# -------------------------------
# Safe file copy
# -------------------------------
if [ -f "$INTERNALS/example/src/UICustomizations.js" ]; then
  mkdir -p src/Customisations
  cp "$INTERNALS/example/src/UICustomizations.js" src/Customisations/
  echo "UICustomizations copied"
else
  echo "UICustomizations file not found, skipping"
fi

# -------------------------------
# Internals build
# -------------------------------
cd $INTERNALS || exit 1

if [ -d ".git" ]; then
  echo "Branch: $(git branch --show-current)"
  echo "Last commit: $(git log -1 --pretty=%B)"
fi

echo "Installing internal packages"

yarn install --ignore-engines

echo "Building internals"
yarn build

echo "Internals build finished"

cd ..