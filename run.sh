#!/bin/bash

#
# App Message Push application starter
# @author   Boram Kim
# @since    2020-10-07
#
# List of environment variables declared in dockers
#  - ACTIVE_PROFILE: String boot active profile
#  - APP_NAME: Application name
#  - API_SERVER_URL: Api server url

if [ "${APP_NAME}" = "" ]; then
    echo "Application name variable must be defined."
    exit 1
fi

if [ "${ACTIVE_PROFILE}" = "" ]; then
    echo "Active profile environment variable must be defined."
    exit 1
fi

if [ "${API_SERVER_URL}" = "" ]; then
    echo "Api server url must be defined."
    exit 1
fi

echo "Set application environments"
echo "  > app name: ${APP_NAME}"
echo "  > active profile: ${ACTIVE_PROFILE}"
echo "  > Api server url: ${API_SERVER_URL}"
echo "  > keystore file: ${KEYSTORE_FILE_LOCATION}"


java -server \
    -Dspring.profiles.active="${ACTIVE_PROFILE}" \
    -Dprg_name="${CONTAINER_JAR_NAME}" \
    -Dspring.application.name="${APP_NAME}" \
    -Dspring.datasource.username="${DATABASE_ACCOUNT}" \
    -Dspring.datasource.password="${DATABASE_PASSWORD}" \
    -Dsecurity.shop-account.encrypt.key-path="${KEYSTORE_FILE_LOCATION}" \
    -Dsecurity.shop-account.encrypt.password="${KEYSTORE_PASSWORD}" \
    -Dinternal.client.url="${API_SERVER_URL}" \
    -jar "${CONTAINER_JAR_NAME}"
