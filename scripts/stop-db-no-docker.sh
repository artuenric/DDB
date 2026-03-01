#!/bin/bash
cd "$(dirname "$0")/.."

echo "=> Encerrando MySQL Portátil..."

# Mata apenas os processos mysqld iniciados pelo seu usuário
pkill -u $USER -f mysqld

# Remove o arquivo de socket para garantir que o próximo 'start' seja limpo
if [ -f "/tmp/mysql-local.sock" ]; then
    rm /tmp/mysql-local.sock
    echo "=> Arquivo de socket removido."
fi

echo "=> Todos os processos do banco foram encerrados."
