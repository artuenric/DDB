#!/bin/bash
cd "$(dirname "$0")/.."

cd mysql-local
echo "=> Iniciando MySQL Portátil..."
LIB_DIR=$(dirname $(find $(pwd)/libaio_local -name "libaio.so.1" | head -n 1))
LD_LIBRARY_PATH=$LIB_DIR ./bin/mysqld --defaults-file=./my.cnf &
echo "=> Banco de dados subindo em segundo plano."
