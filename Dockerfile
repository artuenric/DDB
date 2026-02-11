FROM amazoncorretto:17

# Instalação de bibliotecas essenciais para o X11 e Swing/AWT
RUN yum install -y libXext libXrender libXtst libXi xorg-x11-server-Xvfb && yum clean all

WORKDIR /app

# Copia as bibliotecas, código e configurações
COPY lib/ ./lib/
COPY src/ ./src/
COPY config/ ./config/

RUN mkdir out

# Compilação do Middleware e do Cliente (caso precises de abrir a interface)
RUN javac -cp "lib/*" -d out src/br/com/ddb/common/*.java \
    src/br/com/ddb/middleware/*.java \
    src/br/com/ddb/client/*.java

# Comando para iniciar o Middleware (o Cliente pode ser chamado manualmente se necessário)
CMD ["java", "-cp", "out:lib/*", "br.com.ddb.middleware.DDBNode"]