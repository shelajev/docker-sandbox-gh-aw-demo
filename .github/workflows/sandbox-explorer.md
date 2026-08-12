---
name: Docker Sandbox exploratory test

on:
  workflow_dispatch:

runs-on: ubuntu-24.04

permissions:
  contents: read
  copilot-requests: write

engine: copilot

network:
  allowed:
    - defaults
    - github
    - containers
    - java

sandbox:
  agent:
    id: awf
    runtime: docker-sbx
    sudo: true

tools:
  edit:
  bash: [":*"]

safe-outputs:
  create-pull-request:
    title-prefix: "[sandbox demo] "
    draft: true
    protected-files: blocked
    allowed-files:
      - "src/**"
---

Act as a bounded exploratory tester for this repository.

Your goal is to verify the documented email-registration invariants against
a real PostgreSQL database, add a regression test for any defect you find,
make the smallest appropriate correction, and propose the result as a draft
pull request.

First, record the execution environment in the workflow log:

- Run `uname -a`.
- Run `docker version` and `docker info`.
- Run `docker context show`.
- Run `docker run --rm alpine:3.21 uname -a`.

Then:

1. Read `REQUIREMENTS.md` and the relevant source and test files.
2. Run `./scripts/test-in-docker.sh` without changing anything.
3. Add a PostgreSQL Testcontainers test that checks registration of two
   addresses that differ only in letter case.
4. Run the focused test and explain the observed behavior.
5. If the implementation violates the documented invariant, make the
   smallest fix under `src/`.
6. Run the complete test suite again.
7. Create one draft pull request containing the regression test and fix.

Do not modify dependency manifests, workflow files, scripts, documentation,
or generated files. Do not weaken or delete existing tests. Include the
commands run and their results in the pull request description.
