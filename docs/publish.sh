#!/usr/bin/env bash

# Build and publish documentation to github pages
# Prerequisites:
# - Valid ssh key to access: https://help.github.com/en/github/authenticating-to-github/connecting-to-github-with-ssh
# - export GITHUB_GIT_USER_NAME
# - export GITHUB_GIT_USER_EMAIL

set -euf -o pipefail

if [ "$(git status -s)" ]
then
    echo "The working directory is dirty. Please commit pending changes"
    exit 1;
fi

DOCS_DIR=$(dirname "$0")
cd "${DOCS_DIR}/.."

docker build --tag android/docs/local docs

docker run --rm \
        --volume "$(pwd)":/app \
        --volume "$HOME/.ssh":/home/user/.ssh \
        --volume "${SSH_AUTH_SOCK:-/dev/null}":/tmp/ssh_auth_sock \
        --env "LOCAL_USER_ID=$(id -u)" \
        --env "GITHUB_GIT_USER_NAME=${GITHUB_GIT_USER_NAME}" \
        --env "GITHUB_GIT_USER_EMAIL=${GITHUB_GIT_USER_EMAIL}" \
        -w="/app" \
        android/docs/local:latest \
        sh -c "docs/_publish_internal.sh"
