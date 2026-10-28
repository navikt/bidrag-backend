#!/bin/bash

# bidrag-cli installation script
# This script builds the CLI and adds an alias to your shell configuration

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_PATH="$PROJECT_DIR/dist/main.js"

echo "Installing bidrag-cli..."

# Install dependencies
echo "Running npm install..."
npm install

# Build the project
echo "Building the project..."
npm run build

# Function to add alias to shell config
add_alias() {
    local config_file="$1"
    local alias_line="alias bidrag-cli='function _bcli(){node $DIST_PATH \"\$@\"};_bcli'"

    # Check if alias already exists
    if grep -q "^alias bidrag-cli=" "$config_file" 2>/dev/null; then
        echo "Alias 'bidrag-cli' already exists in $config_file"
        return 0
    fi

    # Add alias to config file
    echo "" >> "$config_file"
    echo "# bidrag-cli alias" >> "$config_file"
    echo "$alias_line" >> "$config_file"
    echo "Added alias to $config_file"
}

add_fish_function() {
    local escaped_dist_path
    escaped_dist_path=$("$SHELL" --no-config -c 'string escape --style=script -- $argv[1]' "$DIST_PATH")

    if grep -Eq '^[[:space:]]*function[[:space:]]+bidrag-cli([[:space:]]|$)' "$SHELL_CONFIG" 2>/dev/null; then
        echo "Function 'bidrag-cli' already exists in $SHELL_CONFIG"
        return 0
    fi

    echo "" >> "$SHELL_CONFIG"
    echo "# bidrag-cli function" >> "$SHELL_CONFIG"
    printf 'function bidrag-cli\n    node %s $argv\nend\n' "$escaped_dist_path" >> "$SHELL_CONFIG"
    echo "Added function to $SHELL_CONFIG"
}

# Detect shell and configure bidrag-cli
SHELL_CONFIG=""
if [[ "$SHELL" == *"zsh"* ]]; then
    SHELL_CONFIG="$HOME/.zshrc"
elif [[ "$SHELL" == *"bash"* ]]; then
    SHELL_CONFIG="$HOME/.bashrc"
elif [[ "$SHELL" == *"fish"* ]]; then
    SHELL_CONFIG="${XDG_CONFIG_HOME:-$HOME/.config}/fish/config.fish"
else
    echo "Unsupported shell: $SHELL"
    echo "Please manually add the alias to your shell configuration file:"
    echo "alias bidrag-cli='function _bcli(){node $DIST_PATH \"\$@\"};_bcli'"
    exit 1
fi

if [[ "$SHELL" == *"fish"* ]]; then
    mkdir -p "$(dirname "$SHELL_CONFIG")"
    touch "$SHELL_CONFIG"
    add_fish_function
elif [[ -f "$SHELL_CONFIG" ]]; then
    add_alias "$SHELL_CONFIG"
else
    echo "Shell config file $SHELL_CONFIG not found. Creating it..."
    touch "$SHELL_CONFIG"
    add_alias "$SHELL_CONFIG"
fi

echo ""
echo "Installation complete!"
echo "Please restart your terminal or run 'source $SHELL_CONFIG' to use bidrag-cli."
echo "You can now use 'bidrag-cli' command directly."
