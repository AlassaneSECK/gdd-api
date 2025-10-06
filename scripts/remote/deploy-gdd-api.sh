#!/usr/bin/env bash
set -euo pipefail

# Deploys the packaged Spring Boot jar into the target directory and restarts the systemd service.
APP_DIR="${APP_DIR:-/opt/gdd-api/app}"
SERVICE_NAME="${SERVICE_NAME:-gdd-api}"
SERVICE_USER="${SERVICE_USER:-deployer}"
SERVICE_GROUP="${SERVICE_GROUP:-${SERVICE_USER}}"
JAR_SOURCE="${TMP_JAR_PATH:-/tmp/gdd-api.jar}"
JAR_TARGET="${JAR_TARGET:-${APP_DIR}/gdd-api.jar}"
SYSTEMCTL_BIN="${SYSTEMCTL_BIN:-$(command -v systemctl || echo /bin/systemctl)}"

if [[ ! -f "${JAR_SOURCE}" ]]; then
    echo "Artifact not found at ${JAR_SOURCE}" >&2
    exit 1
fi

sudo -n mkdir -p "${APP_DIR}"
sudo -n chown "${SERVICE_USER}:${SERVICE_GROUP}" "${APP_DIR}"

if [[ -n "${SERVICE_NAME}" ]]; then
    sudo -n "${SYSTEMCTL_BIN}" stop "${SERVICE_NAME}" || true
fi

sudo -n install -m 640 "${JAR_SOURCE}" "${JAR_TARGET}"
sudo -n chown "${SERVICE_USER}:${SERVICE_GROUP}" "${JAR_TARGET}"
sudo -n rm -f "${JAR_SOURCE}"

if [[ -n "${SERVICE_NAME}" ]]; then
    sudo -n "${SYSTEMCTL_BIN}" start "${SERVICE_NAME}"
    sudo -n "${SYSTEMCTL_BIN}" status "${SERVICE_NAME}" --no-pager
fi
