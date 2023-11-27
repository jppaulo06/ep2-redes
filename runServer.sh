#!/bin/bash

if [[ $# -gt 1 ]]; then
  echo ''
  echo 'Utilização:'
  echo ''
  echo '    server [serverPort]'
  echo ''
  echo 'Quando a porta do servidor não é dada no argumento, o padrão é 3000.'
  echo ''
  exit 1
fi

if [[ $# -eq 0 ]] then command="./Server/gradlew run --console=plain -p ./Server/"
else command="./Server/gradlew run --console=plain -p ./Server/ --args=\"$@\""
fi

echo "Rodando ${command}"
eval "${command}"

exit 0
