#!/bin/bash

if [[ $# -ne 1 ]] && [[ $# -ne 4 ]]; then
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
  exit 1
fi

echo $@

command="./Client/gradlew run --console=plain -p './Client/' --args=$@"

echo "Rodando ${command}"
eval "${command}"

exit 0
