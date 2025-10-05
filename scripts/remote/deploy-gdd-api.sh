#!/usr/bin/env bash
set -euo pipefail

# Deploys the packaged Spring Boot jar into the target directory and restarts the systemd service.
APP_DIR="${APP_DIR:-/opt/gdd-api/app}"
SERVICE_NAME="${SERVICE_NAME:-gdd-api}"
SERVICE_USER="${SERVICE_USER:-deployer}"
SERVICE_GROUP="${SERVICE_GROUP:-${SERVICE_USER}}"
JAR_SOURCE="${TMP_JAR_PATH:-/tmp/gdd-api.jar}"
JAR_TARGET="${JAR_TARGET:-${APP_DIR}/gdd-api.jar}"

if [[ ! -f "${JAR_SOURCE}" ]]; then
    echo "Artifact not found at ${JAR_SOURCE}" >&2
    exit 1
fi

sudo mkdir -p "${APP_DIR}"
sudo chown "${SERVICE_USER}:${SERVICE_GROUP}" "${APP_DIR}"

if [[ -n "${SERVICE_NAME}" ]]; then
    sudo systemctl stop "${SERVICE_NAME}" || true
fi

sudo install -m 640 "${JAR_SOURCE}" "${JAR_TARGET}"
sudo chown "${SERVICE_USER}:${SERVICE_GROUP}" "${JAR_TARGET}"
sudo rm -f "${JAR_SOURCE}"

if [[ -n "${SERVICE_NAME}" ]]; then
    sudo systemctl start "${SERVICE_NAME}"
    sudo systemctl status "${SERVICE_NAME}" --no-pager
fi
