# Source this file from Fish to load the bidrag development helpers.

set -g __BIDRAG_DEV_DIR (path dirname (realpath (status filename)))
set -g __BIDRAG_CLI_PATH (path resolve "$__BIDRAG_DEV_DIR/../bidrag-cli/dist/main.js")

function login
    gcloud auth login --update-adc
    and nais auth login --nais
end

function bidrag-cli
    node "$__BIDRAG_CLI_PATH" $argv
end

alias po 'kubectl get po'
alias dev 'kubectx dev-gcp'
alias dev-fss 'kubectx dev-fss'
alias prod 'kubectx prod-gcp'
alias prod-fss 'kubectx prod-fss'

function __bidrag_app_name
    switch "$argv[1]"
        case ba
            echo bidrag-admin
        case bar
            echo bidrag-aktoerregister
        case baf
            echo bidrag-arbeidsflyt
        case baj
            echo bidrag-automatisk-jobb
        case bbm
            echo bidrag-bbm
        case beh
            echo bidrag-behandling
        case bbh
            echo bidrag-belopshistorikk
        case bbka
            echo bidrag-bidragskalkulator-api
        case bbkv
            echo bidrag-bidragskalkulator-v2
        case bdb
            echo bidrag-dokument-bestilling
        case bdf
            echo bidrag-dokument-forsendelse
        case bdj
            echo bidrag-dokument-journalpost
        case bdp
            echo bidrag-dokument-produksjon
        case bdm
            echo bidrag-dokumentmal
        case bes
            echo bidrag-elin-stub
        case bg
            echo bidrag-grunnlag
        case bkm
            echo bidrag-kafka-manager
        case bk
            echo bidrag-kodeverk
        case bmc
            echo bidrag-maskinporten-client
        case bpg
            echo bidrag-pdfgen
        case bp
            echo bidrag-person
        case bph
            echo bidrag-person-hendelse
        case br
            echo bidrag-regnskap
        case bra
            echo bidrag-reisekostnad-api
        case bru
            echo bidrag-reisekostnad-ui
        case brk
            echo bidrag-reskontro
        case bs
            echo bidrag-sak
        case bsh
            echo bidrag-samhandler
        case bst
            echo bidrag-statistikk
        case btc
            echo bidrag-tilgangskontroll
        case busf
            echo bidrag-ui-static-files
        case bve
            echo bidrag-vedtak
        case '*'
            return 1
    end
end

function database
    if test "$argv[1]" = -h; or test "$argv[1]" = --help
        printf '%s\n' \
            'Usage: database <app-alias> [environment] [-p <port>]' \
            'Connect to a PostgreSQL database through a Nais proxy.' \
            'With an environment, use dev-gcp; otherwise use prod-gcp.'
        return 0
    end

    if test (count $argv) -lt 1
        echo 'Usage: database <app-alias> [environment] [-p <port>]' >&2
        return 1
    end

    set -l app (__bidrag_app_name "$argv[1]")
    if test (count $app) -eq 0
        echo "Unknown application alias: $argv[1]" >&2
        return 1
    end

    set -l environment
    set -l port_option
    set -l i 2
    while test $i -le (count $argv)
        switch "$argv[$i]"
            case -p
                set -l port_index (math $i + 1)
                if test $port_index -gt (count $argv)
                    echo 'Error: -p requires a port number.' >&2
                    return 1
                end
                set -l port "$argv[$port_index]"
                if not string match -rq '^[0-9]{1,5}$' -- "$port"; or test "$port" -lt 1; or test "$port" -gt 65535
                    echo 'Error: Port must be a number between 1 and 65535.' >&2
                    return 1
                end
                set port_option "-p$port"
                set i (math $i + 2)
            case -h --help
                database -h
                return 0
            case '*'
                set environment "$argv[$i]"
                set i (math $i + 1)
        end
    end

    set -l target_environment prod-gcp
    set -l reason 'Prodoppfølging'
    if test -n "$environment"
        kubectx dev-gcp
        set app "$app-$environment"
        set target_environment dev-gcp
        set reason 'Test i miljø'
    else
        kubectx prod-gcp
    end

    set -l proxy_args postgres proxy --environment "$target_environment" --reason "$reason"
    if test -n "$port_option"
        set -a proxy_args "$port_option"
    end
    set -a proxy_args "$app"
    nais $proxy_args
end

function printenv
    if test "$argv[1]" = -h; or test "$argv[1]" = --help
        printf '%s\n' \
            'Usage: printenv <app-alias> [environment]' \
            'Print environment variables from an application pod.' \
            'With an environment, use dev-gcp; otherwise use prod-gcp.'
        return 0
    end

    if test (count $argv) -lt 1
        echo 'Usage: printenv <app-alias> [environment]' >&2
        return 1
    end

    set -l app (__bidrag_app_name "$argv[1]")
    if test (count $app) -eq 0
        echo "Unknown application alias: $argv[1]" >&2
        return 1
    end

    if test -n "$argv[2]"
        kubectx dev-gcp
        set app "$app-$argv[2]"
    else
        kubectx prod-gcp
    end

    kubectl exec --namespace bidrag --tty "deployment/$app" -- printenv
end

function token
    if test "$argv[1]" = -h; or test "$argv[1]" = --help
        printf '%s\n' \
            'Usage: token <app-alias> [environment]' \
            'Generate an authentication token for an application.' \
            'With an environment, use dev-gcp; otherwise use prod-gcp.'
        return 0
    end

    if test (count $argv) -lt 1
        echo 'Usage: token <app-alias> [environment]' >&2
        return 1
    end

    set -l app (__bidrag_app_name "$argv[1]")
    if test (count $app) -eq 0
        echo "Unknown application alias: $argv[1]" >&2
        return 1
    end

    if test -n "$argv[2]"
        kubectx dev-gcp
        set app "$app-$argv[2]"
    else
        kubectx prod-gcp
    end

    bidrag-cli token "$app"
end

alias ba 'bidrag-admin'
alias bar 'bidrag-aktoerregister'
alias baf 'bidrag-arbeidsflyt'
alias baj 'bidrag-automatisk-jobb'
alias bbm 'bidrag-bbm'
alias beh 'bidrag-behandling'
alias bbh 'bidrag-belopshistorikk'
alias bbka 'bidrag-bidragskalkulator-api'
alias bbkv 'bidrag-bidragskalkulator-v2'
alias bdb 'bidrag-dokument-bestilling'
alias bdf 'bidrag-dokument-forsendelse'
alias bdj 'bidrag-dokument-journalpost'
alias bdp 'bidrag-dokument-produksjon'
alias bdm 'bidrag-dokumentmal'
alias bes 'bidrag-elin-stub'
alias bg 'bidrag-grunnlag'
alias bkm 'bidrag-kafka-manager'
alias bk 'bidrag-kodeverk'
alias bmc 'bidrag-maskinporten-client'
alias bpg 'bidrag-pdfgen'
alias bp 'bidrag-person'
alias bph 'bidrag-person-hendelse'
alias br 'bidrag-regnskap'
alias bra 'bidrag-reisekostnad-api'
alias bru 'bidrag-reisekostnad-ui'
alias brk 'bidrag-reskontro'
alias bs 'bidrag-sak'
alias bsh 'bidrag-samhandler'
alias bst 'bidrag-statistikk'
alias btc 'bidrag-tilgangskontroll'
alias busf 'bidrag-ui-static-files'
alias bve 'bidrag-vedtak'

function help_functions
    printf '%s\n' \
        'database <app> [env] [-p <port>] [-h]  Connect to a PostgreSQL database.' \
        'printenv <app> [env] [-h]             Print environment variables from a pod.' \
        'token <app> [env] [-h]                Generate an application token.' \
        'Use -h with a function for details.'
end
