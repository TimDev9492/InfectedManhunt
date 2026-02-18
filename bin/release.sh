#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd ${SCRIPT_DIR}/../
REPO_NAME=$(basename $(pwd))

check_prerequisites() {
  local required_commands=("printf" "sed" "git")

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

extract_yml_string() {
  local yml_file="$1"
  local key="$2"

  sed -nE "s/^${key}: '(.*)'/\1/p" "${yml_file}"
}

set_yml_string() {
  local yml_file="$1"
  local key="$2"
  local value="$3"

  sed -i "s/^${key}: '.*'/${key}: '${value}'/" "${yml_file}"
}

semver_ge() {
  local pre="$1"
  local fut="$2"

  IFS='.' read -r a1 a2 a3 <<< "$pre"
  IFS='.' read -r b1 b2 b3 <<< "$fut"

  if (( a1 != b1 )); then
    (( a1 <= b1 ))
  elif (( a2 != b2 )); then
    (( a2 <= b2 ))
  else
    (( a3 <= b3 ))
  fi
}

check_prerequisites

STARTING_BRANCH=$(git branch --show-current)

# only continue if there are no uncommitted changes
if [[ -n $(git status --porcelain) ]]; then
  echo "There are uncommitted changes, commit them before running this script."
  exit 1
fi

trap "git stash && git checkout ${STARTING_BRANCH}; exit 1" INT

PLUGIN_YML_FILE="src/main/resources/plugin.yml"

# Prompt user for project version bump
OLD_PROJECT_VERSION=$(extract_yml_string "${PLUGIN_YML_FILE}" "version")
while :; do
  prompt_default_value PROJECT_VERSION "Enter the version to compile for" \
      "${OLD_PROJECT_VERSION}"
    if echo "${PROJECT_VERSION}" | grep -qE '^([0-9]+\.){1,2}[0-9]+$'; then
      if semver_ge "${OLD_PROJECT_VERSION}" "${PROJECT_VERSION}"; then
        break;
      else
        echo "Cannot downgrade the project version from '${OLD_PROJECT_VERSION}' -> '${PROJECT_VERSION}'"
      fi
    else
      echo "Version '${PROJECT_VERSION}' invalid, must be semantic version!"
    fi
done

# Prompt user for minecraft version
while :; do
  prompt_default_value SPIGOT_COMPILE_VERSION "Enter the version to compile for" \
    "$(extract_yml_string "${PLUGIN_YML_FILE}" "compiled-version")"
  if echo "${SPIGOT_COMPILE_VERSION}" | grep -qE '^([0-9]+\.){1,2}[0-9]+$'; then
    break;
  else
    echo "Version '${SPIGOT_COMPILE_VERSION}' invalid, must be semantic version!"
  fi
done
prompt_default_value PLUGIN_YML_API_VERSION "Enter the minimum minecraft version this plugin works on" \
  "$(echo "${SPIGOT_COMPILE_VERSION}" | grep -oE '[0-9]+\.[0-9]+')"

BRANCH_NAME="release/${PROJECT_VERSION}+${SPIGOT_COMPILE_VERSION}"

# switch to new branch
git checkout -B "${BRANCH_NAME}"

# change the values in plugin.yml
set_yml_string "${PLUGIN_YML_FILE}" "version" "${PROJECT_VERSION}"
set_yml_string "${PLUGIN_YML_FILE}" "compiled-version" "${SPIGOT_COMPILE_VERSION}"
set_yml_string "${PLUGIN_YML_FILE}" "api-version" "${PLUGIN_YML_API_VERSION}"

echo "Building project version '${PROJECT_VERSION}'" \
     "for minecraft '${SPIGOT_COMPILE_VERSION}'" \
     "with api-version '${PLUGIN_YML_API_VERSION}'"
while :; do
  if ./gradlew build 1>/dev/null; then
    ./gradlew clean >/dev/null 2>&1
    break
  else
    read -p "Failed to compile project. Fix the errors and press enter to retry. "
  fi
done

git add .
git commit -m "chore: create project release version ${PROJECT_VERSION} for minecraft ${SPIGOT_COMPILE_VERSION}"
read -p "Success! Press enter to push the changes and create the release on the server or ^C to abort. "
git push -f origin "${BRANCH_NAME}"

echo ""
echo "Changes pushed to remote. Creating release..."
read -p "Press enter to switch back to the main branch or ^C to stay on the temporary release branch. "
git checkout $STARTING_BRANCH
