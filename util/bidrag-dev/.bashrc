# Forutsetter installajon av bidrag-cli før bruk. Merk at FSS applikasjoner ikke er støtte per nå selv om de ligger inne som aliaser.

alias login='f() { gcloud auth login --update-adc && nais auth login --nais };f'
alias bidrag-cli='function _bcli(){node /Users/Simen.Naper.Hoston/Documents/Dev/bidrag-cli/dist/main.js "$@"};_bcli'

alias po="kubectl get po"
alias dev="kubectx dev-gcp"
alias dev-fss="kubectx dev-fss"
alias prod="kubectx prod-gcp"
alias prod-fss="kubectx prod-fss"

# =============================================================================
# Helper Functions Documentation
# =============================================================================
#
# database() - Connect to a PostgreSQL database via proxy
#   Usage: database <app-alias> [environment] [-p <port>] [-h]
#
#   Arguments:
#     app-alias     : Application alias (required)
#     environment   : Environment suffix (optional, e.g., 'dev', 'staging')
#                     If provided: uses dev-gcp context
#                     If omitted: uses prod-gcp context
#     -p <port>     : Custom local port number (optional, 1-65535)
#                     Must be specified with -p flag
#     -h            : Display help for this function
#
#   Examples:
#     database myapp              # Connect to myapp in prod-gcp
#     database myapp dev          # Connect to myapp-dev in dev-gcp
#     database myapp prod -p 5555 # Connect to myapp-prod in prod-gcp on port 5555
#     database myapp -p 3000      # Connect to myapp in prod-gcp on port 3000
#     database -h                 # Show help
#
# printenv() - Print environment variables from a pod
#   Usage: printenv <app-alias> [environment] [-h]
#
#   Arguments:
#     app-alias     : Application alias (required)
#     environment   : Environment suffix (optional)
#                     If provided: uses dev-gcp context
#                     If omitted: uses prod-gcp context
#     -h            : Display help for this function
#
#   Examples:
#     printenv myapp       # Print env vars from myapp in prod-gcp
#     printenv myapp dev   # Print env vars from myapp-dev in dev-gcp
#     printenv -h          # Show help
#
# token() - Generate authentication token for an application
#   Usage: token <app-alias> [environment] [-h]
#
#   Arguments:
#     app-alias     : Application alias (required)
#     environment   : Environment suffix (optional)
#                     If provided: uses dev-gcp context
#                     If omitted: uses prod-gcp context
#     -h            : Display help for this function
#
#   Examples:
#     token myapp       # Generate token for myapp in prod-gcp
#     token myapp dev   # Generate token for myapp-dev in dev-gcp
#     token -h          # Show help
#
# =============================================================================

database() {
    # Check for help flag first
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        cat << 'EOF'
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📦 database - Connect to PostgreSQL database via proxy
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

USAGE:
  database <app-alias> [environment] [-p <port>]

ARGUMENTS:
  app-alias       Application alias (required)
  environment     Environment suffix (optional, e.g., 'dev', 'staging')
                  • If provided: uses dev-gcp context
                  • If omitted: uses prod-gcp context
  -p <port>       Custom local port number (1-65535)

EXAMPLES:
  database myapp              # Connect to myapp in prod-gcp
  database myapp dev          # Connect to myapp-dev in dev-gcp
  database myapp -p 5555      # Connect to myapp in prod-gcp on port 5555
  database myapp dev -p 3000  # Connect to myapp-dev in dev-gcp on port 3000

EOF
        return 0
    fi

    local app=$(alias "$1" 2>/dev/null | cut -d'=' -f2)
    local port_option=""
    local team="bidrag"
    local environment=""
    local default_environment="prod-gcp"
    local default_reason="Prodoppfølging"


    # Parse arguments to handle both positional and flag-based port
    shift  # Remove first argument (app-alias)

    while [ $# -gt 0 ]; do
        case "$1" in
            -p)
                if [ -z "$2" ]; then
                    echo "Error: -p flag requires a port number" >&2
                    return 1
                fi
                if ! [[ "$2" =~ ^[0-9]+$ ]] || [ "$2" -le 0 ] || [ "$2" -gt 65535 ]; then
                    echo "Error: Port must be a number between 1 and 65535" >&2
                    return 1
                fi
                port_option="-p$2"
                shift 2
                ;;
            -h|--help)
                database -h
                return 0
                ;;
            *)
                # Assume it's the environment argument
                environment="$1"
                shift
                ;;
        esac
    done

    # Switch context based on environment
    if [ -n "$environment" ]; then
        kubectx dev-gcp
        app="$app-$environment"
        default_environment="dev-gcp"
        default_reason="Test i miljø"
    else
        kubectx prod-gcp
    fi

    # shellcheck disable=SC2086  # Intentional word splitting for optional single-word flag
    nais postgres proxy --environment "$default_environment" --reason "$default_reason" $port_option "$app"
}

printenv() {
    # Check for help flag first
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        cat << 'EOF'
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 printenv - Print environment variables from a pod
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

USAGE:
  printenv <app-alias> [environment]

ARGUMENTS:
  app-alias       Application alias (required)
  environment     Environment suffix (optional)
                  • If provided: uses dev-gcp context
                  • If omitted: uses prod-gcp context

EXAMPLES:
  printenv myapp       # Print env vars from myapp in prod-gcp
  printenv myapp dev   # Print env vars from myapp-dev in dev-gcp

EOF
        return 0
    fi

    local app=$(alias "$1" 2>/dev/null | cut -d'=' -f2)
    if [ -n "$2" ]; then
      kubectx dev-gcp
    else
      kubectx prod-gcp
    fi

    if [ -n "$2" ]; then
        app="$app-$2"
    fi

    kubectl exec --tty deployment/"$app" -- printenv
}

token() {
    # Check for help flag first
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        cat << 'EOF'
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔐 token - Generate authentication token for an application
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

USAGE:
  token <app-alias> [environment]

ARGUMENTS:
  app-alias       Application alias (required)
  environment     Environment suffix (optional)
                  • If provided: uses dev-gcp context
                  • If omitted: uses prod-gcp context

EXAMPLES:
  token myapp       # Generate token for myapp in prod-gcp
  token myapp dev   # Generate token for myapp-dev in dev-gcp

EOF
        return 0
    fi

    local app=$(alias "$1" 2>/dev/null | cut -d'=' -f2)
    if [ -n "$2" ]; then
      kubectx dev-gcp
    else
      kubectx prod-gcp
    fi

    if [ -n "$2" ]; then
        app="$app-$2"
    fi

    bidrag-cli token "$app"
}

# =============================================================================
# Application Aliases for bidrag apps
# =============================================================================

alias ba='bidrag-admin'
alias bar='bidrag-aktoerregister'
alias baf='bidrag-arbeidsflyt'
alias baj='bidrag-automatisk-jobb'
alias bbm="bidrag-bbm"
alias beh='bidrag-behandling'
alias bbh='bidrag-belopshistorikk'
alias bbka='bidrag-bidragskalkulator-api'
alias bbkv='bidrag-bidragskalkulator-v2'
alias bdb='bidrag-dokument-bestilling'
alias bdf='bidrag-dokument-forsendelse'
alias bdj="bidrag-dokument-journalpost"
alias bdp='bidrag-dokument-produksjon'
alias bdm='bidrag-dokumentmal'
alias bes='bidrag-elin-stub'
alias bg='bidrag-grunnlag'
alias bkm='bidrag-kafka-manager'
alias bk='bidrag-kodeverk'
alias bmc='bidrag-maskinporten-client'
alias bpg='bidrag-pdfgen'
alias bp="bidrag-person"
alias bph='bidrag-person-hendelse'
alias br='bidrag-regnskap'
alias bra='bidrag-reisekostnad-api'
alias bru='bidrag-reisekostnad-ui'
alias brk='bidrag-reskontro'
alias bs="bidrag-sak"
alias bsh='bidrag-samhandler'
alias bst='bidrag-statistikk'
alias btc='bidrag-tilgangskontroll'
alias busf='bidrag-ui-static-files'
alias bve='bidrag-vedtak'

# Optional: Add help function to display documentation
help_functions() {
    cat << 'EOF'
╔═══════════════════════════════════════════════════════════════════════════╗
║                    Helper Functions Quick Reference                       ║
╚═══════════════════════════════════════════════════════════════════════════╝

📦 database <app> [env] [-p <port>] [-h]
   Connect to PostgreSQL database via proxy

   Examples:
   • database myapp              → prod-gcp
   • database myapp dev          → dev-gcp (myapp-dev)
   • database myapp -p 5555      → prod-gcp on port 5555
   • database myapp dev -p 3000  → dev-gcp on port 3000
   • database -h                 → Show detailed help

🔍 printenv <app> [env] [-h]
   Print environment variables from pod

   Examples:
   • printenv myapp       → prod-gcp
   • printenv myapp dev   → dev-gcp (myapp-dev)
   • printenv -h          → Show detailed help

🔐 token <app> [env] [-h]
   Generate authentication token

   Examples:
   • token myapp       → prod-gcp
   • token myapp dev   → dev-gcp (myapp-dev)
   • token -h          → Show detailed help

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TIP: Use -h flag with any function to see detailed help
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

EOF
}
