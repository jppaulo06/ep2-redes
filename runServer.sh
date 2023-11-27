#!/bin/bash

function main () {
  [[ $1 = '--help' ]] && print_help && exit 0
  if [[ $# -gt 1 ]]; then
    print_help
    exit 1
  fi

  if [[ $# -eq 0 ]] then command="./Server/gradlew run --console=plain -p ./Server/"
  else command="./Server/gradlew run --console=plain -p ./Server/ --args=\"$@\""
  fi

  echo "Rodando ${command}"
  eval "${command}"
}


function print_help () {
  echo ''
  echo 'Utilização:'
  echo ''
  echo '    runServer [serverPort]'
  echo ''
  echo 'Quando a porta do servidor não é dada no argumento, o padrão é 3000.'
  echo ''
}

main $@
