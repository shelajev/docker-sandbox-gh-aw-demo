# Docker Sandbox + GitHub Agentic Workflows demo

This repository demonstrates an agent running exploratory integration tests inside a
KVM-backed Docker Sandbox microVM. The agent gets a private Docker daemon, so a Maven
container can use Testcontainers to start a real PostgreSQL dependency without exposing
the runner host's Docker socket to agent-controlled code.

```text
GitHub-hosted Ubuntu runner
└── Docker Sandbox microVM
    ├── GitHub Agentic Workflows agent
    └── private Docker daemon
        ├── Maven 3.9.9 / Java 21 test container
        └── PostgreSQL 16.6 Testcontainers container
```

The seeded implementation has one deliberate defect. `REQUIREMENTS.md` says email
addresses are case-insensitive, but the implementation stores them unchanged and relies
on PostgreSQL's case-sensitive unique constraint. The existing Testcontainers test covers
an exact duplicate only. The workflow asks the agent to add the missing case-variation
test, observe its failure, make the smallest source-only correction, rerun the suite, and
open a draft pull request through a constrained safe output.

## Run the seed test

Docker is the only local prerequisite:

```bash
./scripts/test-in-docker.sh
```

The Maven image is pinned to `maven:3.9.9-eclipse-temurin-21` and its multi-platform
digest; the test pins PostgreSQL to `postgres:16.6-alpine3.21` and its digest.

## Runner requirements

The demo defaults to a GitHub-hosted `ubuntu-24.04` runner. Docker Sandbox needs working
KVM, access to `/dev/kvm`, passwordless sudo, Docker Engine, and an apt-based Linux
distribution. When the runner is itself a virtual machine, nested virtualization must be
available.

GitHub documents nested virtualization on hosted runners as technically possible but
officially unsupported, so availability may change with the runner image. The manual
`Hosted runner KVM probe` workflow checks the relevant capabilities before running the
full demonstration:

```bash
uname -m
grep -Eoc '(vmx|svm)' /proc/cpuinfo
lsmod | grep '^kvm'
test -c /dev/kvm && test -w /dev/kvm && echo '/dev/kvm is usable'
sudo -n true && echo 'passwordless sudo works'
sudo apt-get update && sudo apt-get install -y cpu-checker
sudo kvm-ok
```

If the hosted runner does not expose KVM, use an Ubuntu 24.04 self-hosted runner that meets
the same requirements, update `runs-on` in `sandbox-explorer.md`, and recompile the
workflow. Do not preinstall `docker-sbx`; the compiled workflow owns its installation and
preflight.

## Repository configuration

Add Actions secrets `DOCKER_USERNAME` and `DOCKER_PAT`. The Docker Hub PAT must be able to
pull `docker/sandbox-templates:shell-docker`. For the Copilot engine, enable an organization
Copilot subscription with `copilot-requests: write`, or configure the supported token path
for your account. In Actions settings, allow GitHub Actions to create pull requests.

Compile and commit both the editable Markdown workflow and generated lock file:

```bash
gh extension install github/gh-aw
gh aw compile sandbox-explorer
git add .github/workflows/sandbox-explorer.md \
  .github/workflows/sandbox-explorer.lock.yml
git commit -m "Add Docker Sandbox exploratory testing workflow"
git push
```

Never edit the generated `.lock.yml` file manually. Start and watch the demo with:

```bash
gh aw run sandbox-explorer
gh run watch
```

## What proves the demo

A successful recorded run should show the generated KVM check, Docker Sandbox install and
authentication, the create/exec/remove preflight, `docker info` and the Alpine kernel from
inside the microVM, PostgreSQL starting through Testcontainers, the new test failing before
the correction and the full suite passing afterward. The safe-output job must open a draft
PR whose patch touches only `src/**`, and the teardown must remove the sandbox without
printing Docker or AI credentials.

The key integration is compiler configuration, not a separate Marketplace Action:

```yaml
sandbox:
  agent:
    id: awf
    runtime: docker-sbx
    sudo: true
```

Templates and kits make the sandbox environment reusable; network policy limits egress;
Docker organization governance can centrally enforce network and filesystem policy. Those
controls complement gh-aw's read-only agent permissions and separately executed safe output.
