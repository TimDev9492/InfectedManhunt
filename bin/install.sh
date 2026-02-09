#!/bin/bash

INITIAL_GROUP_ID="me.author"
INITIAL_PACKAGE_NAME="minecraftPluginTemplate"
INITIAL_PROJECT_NAME="MinecraftPluginTemplate"

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd ${SCRIPT_DIR}/../
REPO_NAME=$(basename $(pwd))

TMP_DIR="./bin/tmp"
mkdir -p "${TMP_DIR}"

cleanup() {
  rm -rf "${TMP_DIR}"
}

trap cleanup EXIT

check_prerequisites() {
  local required_commands=("printf" "sed" "git" "find")

  # Accept more requirements passed as arguments
  if [ $# -gt 0 ]; then
    required_commands+=("$@")
  fi

  # Check if required commands are installed
  for cmd in "${required_commands[@]}"; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
      echo "[ERROR] $cmd is not installed." >&2
      exit 1
    fi
  done
}

prompt_default_value() {
  local assignment_var="$1"
  local prompt="$2"
  local default="$3"

  local input=''
  read -p "$prompt [$default]: " input
  read_status="$?"
  printf -v "$assignment_var" "%s" "${input:-$default}"
  return $read_status
}

package_to_path() {
  local package_uri="$1"
  echo "$package_uri" | sed 's/\./\//g'
}

check_prerequisites

# Prompt the user for input values
prompt_default_value PLUGIN_NAME "Enter the name for your plugin" "${REPO_NAME}"
prompt_default_value PACKAGE_NAME "Enter the name of your java package" "${PLUGIN_NAME,,}"
prompt_default_value GROUP_ID "Enter the java group id" "me.$(whoami)"

### Adjust plugin name, package and groud id ###
if [ "${PLUGIN_NAME}" != "${REPO_NAME}" ]; then
  cd ..
  [ -d "${PLUGIN_NAME}" ] && echo "Directory '${PLUGIN_NAME}' already exists. Exiting..." && exit 1
  mv "${REPO_NAME}" "${PLUGIN_NAME}"
  cd "${PLUGIN_NAME}"
fi

sed -i "s/^rootProject\.name = '${INITIAL_PROJECT_NAME}'/rootProject\.name = '${PLUGIN_NAME}'/" settings.gradle

# move MinecraftPluginTemplate.java to tmp dir
mv "src/main/java/$(package_to_path "${INITIAL_GROUP_ID}.${INITIAL_PACKAGE_NAME}")"/* "${TMP_DIR}"
# remove old group and package directories
rm -rf src/main/java/*
PACKAGE_DIR="src/main/java/$(package_to_path "${GROUP_ID}.${PACKAGE_NAME}")"
PLUGIN_JAVA_FILE="${PACKAGE_DIR}/${PLUGIN_NAME}.java"
# create new package directories
mkdir -p "${PACKAGE_DIR}"
# move main class to new location
mv "${TMP_DIR}"/* "${PACKAGE_DIR}"
# rename main class and package declarations
find "${PACKAGE_DIR}" -type f | while read -r file; do
  sed -i "s/${INITIAL_PROJECT_NAME}/${PLUGIN_NAME}/g" "${file}"
  sed -i "s/^import ${INITIAL_GROUP_ID}.${INITIAL_PACKAGE_NAME}/import ${GROUP_ID}.${PACKAGE_NAME}/" "${file}"
  sed -i "s/^package ${INITIAL_GROUP_ID}.${INITIAL_PACKAGE_NAME}/package ${GROUP_ID}.${PACKAGE_NAME}/" "${file}"
done
mv "${PACKAGE_DIR}/${INITIAL_PROJECT_NAME}.java" "${PLUGIN_JAVA_FILE}"

sed -i "s/^name: ${INITIAL_PROJECT_NAME}/name: ${PLUGIN_NAME}/" src/main/resources/plugin.yml
sed -i "s/^main: ${INITIAL_GROUP_ID}.${INITIAL_PACKAGE_NAME}.${INITIAL_PROJECT_NAME}/main: ${GROUP_ID}.${PACKAGE_NAME}.${PLUGIN_NAME}/" src/main/resources/plugin.yml

sed -i "s/^group = '${INITIAL_GROUP_ID}'/group = '${GROUP_ID}'/" build.gradle

while :; do
  prompt_default_value SPIGOT_API_VERSION "Enter the spigot API version for this project"
  sed -i "s/^compiled-version: '.*'$/compiled-version: '${SPIGOT_API_VERSION}'/" src/main/resources/plugin.yml

  if ./gradlew build 1>/dev/null; then
    ./gradlew clean >/dev/null 2>&1
    break
  else
    echo "Failed to compile project for spigot API version '${SPIGOT_API_VERSION}'"
  fi
done

# setup initial commit
prompt_default_value COMMIT_MESSAGE "Success! Enter a commit message for the automated setup modifications" "chore: setup project structure"
git add . && git commit -m "${COMMIT_MESSAGE}"

echo "Successfully installed plugin development environment for '${PLUGIN_NAME}'!"
