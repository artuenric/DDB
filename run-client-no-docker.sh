#!/bin/bash

echo "=> Iniciando a Interface do Cliente DDB..."

# Executa a classe DDBClient usando o classpath correto para Linux
java -cp "out:lib/*" br.com.ddb.client.DDBClient
