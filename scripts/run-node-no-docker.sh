#!/bin/bash
cd "$(dirname "$0")/.." # Entra na raiz do projeto

# 1. Defina o IP DESTA máquina física
export MY_IP="10.113.50.66"

# 2. Defina os IPs de TODOS os nós da rede separados por vírgula
export ALL_NODES="10.113.50.66,10.113.16.198"

# 3. URL do banco apontando para o MySQL local que configurámos na porta 3307
export DB_URL="jdbc:mysql://localhost:3307/ddb_distributed?allowMultiQueries=true"

echo "=> Iniciando Nó DDB no IP $MY_IP..."
echo "=> Conectando ao MySQL em $DB_URL"

# Executa o Middleware
java -cp "out:lib/*" br.com.ddb.middleware.DDBNode
