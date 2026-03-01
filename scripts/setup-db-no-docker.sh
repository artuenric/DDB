#!/bin/bash
cd "$(dirname "$0")/.." # Garante que o MySQL seja instalado na raiz

# Valida se o ID do nó foi passado
if [ -z "$1" ]; then
  echo "Erro: Informe o ID do Nó. Exemplo: ./setup_banco.sh 1"
  exit 1
fi

NODE_ID=$1
MYSQL_DIR="mysql-local"

echo "=> Iniciando configuração do MySQL Portátil para o Nó $NODE_ID..."

# 1. Download e extração do MySQL
if [ ! -d "$MYSQL_DIR" ]; then
    echo "=> Baixando MySQL 8.0..."
    wget -q --show-progress https://dev.mysql.com/get/Downloads/MySQL-8.0/mysql-8.0.36-linux-glibc2.28-x86_64.tar.xz
    echo "=> Extraindo MySQL (isso pode levar um minuto)..."
    tar -xf mysql-8.0.36-linux-glibc2.28-x86_64.tar.xz
    mv mysql-8.0.36-linux-glibc2.28-x86_64 $MYSQL_DIR
else
    echo "=> Diretório $MYSQL_DIR já existe. Ignorando download do MySQL."
fi

cd $MYSQL_DIR
mkdir -p data

# 2. Download e extração do libaio localmente
if [ ! -d "libaio_local" ]; then
    echo "=> Baixando e extraindo dependência (libaio)..."
    wget -q --show-progress http://mirrors.kernel.org/ubuntu/pool/main/liba/libaio/libaio1_0.3.112-13build1_amd64.deb
    dpkg -x libaio1_0.3.112-13build1_amd64.deb ./libaio_local
fi

# 3. Criação do ficheiro de configuração (my.cnf)
echo "=> Criando my.cnf na porta 3307..."
cat <<EOF > my.cnf
[mysqld]
basedir=./
datadir=./data
socket=/tmp/mysql-local.sock
port=3307
server-id=$NODE_ID
log-bin=mysql-bin
binlog-format=ROW
mysqlx=0
EOF

# 4. Inicialização do banco de dados
echo "=> Inicializando o banco de dados..."
LIB_DIR=$(dirname $(find $(pwd)/libaio_local -name "libaio.so.1" | head -n 1))
export LD_LIBRARY_PATH=$LIB_DIR

./bin/mysqld --defaults-file=./my.cnf --initialize-insecure

# 5. Início do serviço em segundo plano
echo "=> Iniciando MySQL em segundo plano..."
./bin/mysqld --defaults-file=./my.cnf &

echo "=> Aguardando o MySQL iniciar (10 segundos)..."
sleep 10

# 6. Injeção do Setup SQL
echo "=> Configurando tabelas e senha..."
./bin/mysql -S /tmp/mysql-local.sock -u root <<EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
CREATE DATABASE IF NOT EXISTS ddb_distributed;
USE ddb_distributed;
CREATE TABLE IF NOT EXISTS produtos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    quantidade INT NOT NULL,
    preco DECIMAL(10,2) NOT NULL,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO produtos (nome, quantidade, preco) VALUES ('Servidor Nó $NODE_ID', 1, 1500.00);
EOF

echo "=> SUCESSO! Banco de dados do Nó $NODE_ID configurado e rodando na porta 3307."