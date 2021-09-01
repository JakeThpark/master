FROM openjdk:11.0.6-slim
USER root

ARG JAR_FILE
ARG KEYSTORE_FILE

ARG DATABASE_ACCOUNT
ARG DATABASE_PASSWORD
ARG KEYSTORE_PASSWORD

ENV DATABASE_ACCOUNT $DATABASE_ACCOUNT
ENV DATABASE_PASSWORD $DATABASE_PASSWORD
ENV KEYSTORE_PASSWORD $KEYSTORE_PASSWORD

ENV CONTAINER_JAR_NAME app.jar
ENV KEYSTORE_FILE_LOCATION /resources/keystore.p12

COPY ${JAR_FILE} ${CONTAINER_JAR_NAME}
COPY ${KEYSTORE_FILE} ${KEYSTORE_FILE_LOCATION}

COPY ./sellist-category.json sellist-category.json
COPY ./category-feelway.json category-feelway.json
COPY ./category-mustit.json category-mustit.json
COPY ./category-reebonz.json category-reebonz.json

COPY ./run.sh run.sh

ENV TZ Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime

ENTRYPOINT /bin/sh run.sh
