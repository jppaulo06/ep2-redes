#!/bin/bash

set -e

function main () {
  [[ $1 = '--help' ]] && print_help && exit 0

  if [[ $# -ne 1 ]] && [[ $# -ne 4 ]]; then
    print_help
    exit 1
  fi

  command="./Client/gradlew run --console=plain -p './Client/' --args=\"$@\""

  echo "Rodando ${command}"
  eval "${command}"
}

function print_help () {
  echo ''
  echo 'Utilização:'
  echo ''
  echo '    runClient UDP|TCP'
  echo '    runClient UDP|TCP serverIP serverPort clientPort'
  echo ''
  echo 'No primeiro modo de execução, o serverIP, serverPort e clientPort são respectivamente'
  echo 'definidos para os seguintes valores:'
  echo '127.0.0.1, 3000 and 3001'
  echo ''
  echo 'Como melhor explicado no README:'
  echo '    serverIP - IP do servidor'
  echo '    serverPort - Porta em que o servidor está recebendo conexões TCP e UDP'
  echo '    clientPort - Porta em que o cliente receberá os desafios'
  echo ''
}

main $@
