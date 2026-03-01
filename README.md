# Projeto_Banco_de_Dados_Distribuidos

Este projeto consiste em um Middleware para Banco de Dados Distribuído Homogêneo e Autônomo. O sistema orquestra a comunicação entre múltiplos nós de banco de dados (MySQL), garantindo a consistência dos dados através do protocolo Two-Phase Commit (2PC) e mantendo a alta disponibilidade com o Algoritmo de Eleição de Bully, além de garantir a sincronização por CDC baseado em Logs.

---

## Requisitos do Projeto Satisfeitos

O middleware foi desenvolvido para atender aos seguintes critérios técnicos:

1.  **Comunicação via Sockets:** Toda a troca de informações entre os nós ocorre através de Sockets TCP (porta `5000`).
2.  **Protocolo Customizado:** Implementação de um protocolo proprietário para mensagens estruturadas (`QUERY`, `PREPARE`, `COMMIT`, `HEARTBEAT`, `ELECTION`).
3.  **Configuração Dinâmica:** Possibilidade de configurar os nós através das variáveis de ambiente (script `run-node-no-docker.sh`).
4.  **DDM Homogêneo Autônomo:** Todos os nós executam a mesma lógica e possuem autonomia para processar requisições.
5.  **Replicação Total:** Qualquer alteração (`INSERT`, `UPDATE`, `DELETE`) efetuada em um nó é replicada síncronamente em todos os outros nós.
6.  **Coordenação e Alta Disponibilidade:** O sistema elege um coordenador inicial. Caso ele falhe, o **Algoritmo de Bully** entra em ação para eleger um novo líder.
7.  **Comunicação Unicast:** As mensagens são enviadas diretamente para os IPs da rede, garantindo entrega confiável via TCP.
8.  **Propriedades ACID:** Garantia de atomicidade e consistência através do protocolo **Two-Phase Commit (2PC)**.
9.  **Monitoramento (Heartbeat):** Todos os nós informam periodicamente ao coordenador que estão ativos.
10. **Integridade via Checksum:** Utilização de **MD5** para verificar a integridade de cada mensagem transmitida.
11. **Balanceamento de Carga:** As requisições de leitura podem ser distribuídas entre qualquer nó da rede.
12. **Transparência e Logs:** Cada nó loga no console as queries requisitadas e o conteúdo transmitido.

---

## Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Banco de Dados:** MySQL 8.0
* **Interface:** Java Swing (DDBClient)

---

## Arquitetura do Sistema

O sistema opera em uma rede local onde cada nó tem um Middleware Java e uma instância do MySQL portátil.

---

## Como Executar

### MySQL Portátil

Adicionaremos uma versão portável do mysql com um script.

1. Em cada computador da rede, use o script `setup-banco-no-docker.sh` presente no diretório `scripts/`.
2. Dê permissão de execução ao script:

```
chmod +x scripts/setup-banco-no-docker.sh
```

1. Execute o script passando o **número do Nó** como parâmetro (1, 2 ou 3).
    - No Computador 1:
        
        `./scripts/setup_banco.sh 1`
        
    - No Computador 2:
        
        `./scripts/setup_banco.sh 2`
        
    - No Computador 3:
        
        `./scripts/setup_banco.sh 3`

### Como aceder ao MySQL rodando:

Para testar se o banco está a responder, aceda à pasta criada e conecte-se pelo terminal usando a senha `root`:

```
cd mysql-local
./bin/mysql -S /tmp/mysql-local.sock -u root -p
#Senha: root
```

Para manipular o banco de dados:

**Opção 1: Selecionar o banco primeiro** 

```
USE ddb_distributed;
SELECT * FROM produtos;
```

**Opção 2: Referenciar o banco direto na query**

```
SELECT * FROM ddb_distributed.produtos;
```

### Como parar o banco de dados e limpar processos:

Quando terminar os testes e quiser encerrar o MySQL sem deixar processos órfãos ocupando memória no laboratório, execute:

```
pkill -u $USER -f mysqld
```

*Se quiser recomeçar do zero e limpar tudo, basta excluir a pasta:* `mysql-local/` 

OU

dê permissão `chmod +x script/stop-db-no-docker` e utilize o script `stop-db-no-docker.sh`

Para garantir que realmente não sobrou nada rodando escondido em segundo plano, verifique com:

```
ps -u $USER -f | grep mysqld
```

Se o retorno mostrar apenas a linha do próprio comando `grep`, todos os processos do MySQL portátil foram encerrados com sucesso.

Às vezes, quando o banco é "morto" (kill), ele deixa um arquivo "sujo" em `/tmp/mysql-local.sock`. Se o banco não subir de jeito nenhum, apague esse arquivo antes de tentar ligar.

## Midlleware

### 1. Compilar o Projeto

No terminal, dentro da pasta raiz do seu projeto, execute o comando para compilar o código. O Linux usa os dois pontos `:` para separar os caminhos no `classpath` em vez do ponto e vírgula `;` do Windows:

```
mkdir -p out
javac -cp "lib/*" -d out src/br/com/ddb/common/*.java src/br/com/ddb/middleware/*.java src/br/com/ddb/client/*.java
```

### 2. Alterar o Script de Execução do Nó (`run-node-no-docker.sh`)

Em vez de definir as variáveis à mão sempre que for testar use o ficheiro chamado `run-node-no-docker.sh` na raiz do projeto:

```
nano run-node-no-docker.sh
```

Agora você precisará alterar os IPs para os IPs IPv4 reais dos computadores:

```
#!/bin/bash

# 1. Defina o IP DESTA máquina física
export MY_IP="AQUI" 

# 2. Defina os IPs de TODOS os nós da rede separados por vírgula
export ALL_NODES="AQUI, AQUI, AQUI" 

# 3. URL do banco apontando para o MySQL local que configurámos na porta 3307
export DB_URL="jdbc:mysql://localhost:3307/ddb_distributed?allowMultiQueries=true"

echo "=> Iniciando Nó DDB no IP $MY_IP..."
echo "=> Conectando ao MySQL em $DB_URL"

# Executa o Middleware
java -cp "out:lib/*" br.com.ddb.middleware.DDBNode
```

Execute o Script do midlleware `run-node-no-docker.sh`. Adicione o IP da máquina que vai receber a solicitação e a porta `5000`.

## Comandos de Testes Sugeridos

1. Inserção : INSERT INTO produtos (nome, quantidade, preco) VALUES ('Notebook', 10, 3500.00)

2. Atualização : UPDATE produtos SET preco = 3200.00 WHERE nome = 'Notebook'

3. Remoção : DELETE FROM produtos WHERE nome = 'Notebook'

4. Consulta : SELECT * FROM produtos

5. Eleição : execute o script ``stop-db-no-docker.sh` (o Nó 2 deve detectar a falha e se tornar o novo coordenador)