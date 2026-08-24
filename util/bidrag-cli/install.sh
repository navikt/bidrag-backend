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

# Detect shell and add alias
SHELL_CONFIG=""
if [[ "$SHELL" == *"zsh"* ]]; then
    SHELL_CONFIG="$HOME/.zshrc"
elif [[ "$SHELL" == *"bash"* ]]; then
    SHELL_CONFIG="$HOME/.bashrc"
else
    echo "Unsupported shell: $SHELL"
    echo "Please manually add the alias to your shell configuration file:"
    echo "alias bidrag-cli='function _bcli(){node $DIST_PATH \"\$@\"};_bcli'"
    exit 1
fi

if [[ -f "$SHELL_CONFIG" ]]; then
    add_alias "$SHELL_CONFIG"
else
    echo "Shell config file $SHELL_CONFIG not found. Creating it..."
    touch "$SHELL_CONFIG"
    add_alias "$SHELL_CONFIG"
fi

echo ""
echo "Installation complete!"
echo "Please restart your terminal or run 'source $SHELL_CONFIG' to use the alias."
echo "You can now use 'bidrag-cli' command directly."