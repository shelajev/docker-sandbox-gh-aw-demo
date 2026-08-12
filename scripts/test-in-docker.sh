#!/usr/bin/env bash
set -euo pipefail

docker run --rm \
  --add-host=host.testcontainers.internal:host-gateway \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.testcontainers.internal \
  -v "$PWD:/workspace" \
  -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e \
  mvn --batch-mode "$@" test
