# GitHub Agentic Workflows + Docker Sandboxes sample

This is a deliberately small sample project demonstrating the `docker-sbx` agent runtime
in [GitHub Agentic Workflows](https://github.github.com/gh-aw/).

The workflow runs an AI coding agent inside a KVM-backed Docker Sandbox microVM. Unlike an
ordinary container, the sandbox has its own kernel boundary and a private Docker daemon.
That lets agent-controlled code build and run containers without giving the agent access
to the runner host's Docker socket.

In this sample, the agent runs Maven in a container and Maven uses Testcontainers to start
a real PostgreSQL database:

```text
GitHub Actions runner
└── Docker Sandbox microVM
    ├── GitHub Agentic Workflows agent
    └── private Docker daemon
        ├── Maven 3.9.9 / Java 21 test container
        └── PostgreSQL 16.6 Testcontainers container
```

## What the sample demonstrates

- Selecting Docker Sandboxes with three lines of agent-runtime configuration.
- Running broad agent-controlled shell commands behind a microVM boundary.
- Starting real integration-test dependencies with Testcontainers inside the sandbox.
- Restricting network access with a GitHub Agentic Workflows allowlist.
- Keeping the agent's GitHub token read-only while a separate safe-output job creates a
  constrained draft pull request.

The essential integration is in
[`.github/workflows/sandbox-explorer.md`](.github/workflows/sandbox-explorer.md):

```yaml
sandbox:
  agent:
    id: awf
    runtime: docker-sbx
    sudo: true
```

No separate Marketplace Action is needed. The `gh-aw` compiler generates the Docker
Sandbox installation, authentication, KVM checks, preflight, agent invocation, and cleanup
steps in `sandbox-explorer.lock.yml`.

## The sample task

The Java application registers users by email. Its documented requirement says email
addresses are case-insensitive, but the seeded implementation stores them unchanged and
relies on PostgreSQL's case-sensitive unique constraint. The existing integration test
covers an exact duplicate but deliberately omits case variants.

The agent is asked to:

1. Inspect the requirements and implementation.
2. Confirm that Docker is available inside the sandbox.
3. Run the existing PostgreSQL Testcontainers suite.
4. Add a regression test for `Alice@example.com` followed by `alice@example.com`.
5. Observe the failing test and make the smallest source-only correction.
6. Run the complete suite and create a draft pull request through a safe output.

The interesting part is not discovering a mysterious bug. It is safely giving an agent
enough freedom to build software, launch containers, test against PostgreSQL, edit code,
and propose a reviewed change.

## Repository map

| Path | Purpose |
| --- | --- |
| [`REQUIREMENTS.md`](REQUIREMENTS.md) | The invariant the agent must verify. |
| [`src/main`](src/main) | The intentionally incomplete implementation. |
| [`src/test`](src/test) | The seeded PostgreSQL Testcontainers test. |
| [`scripts/test-in-docker.sh`](scripts/test-in-docker.sh) | Runs Maven in Docker and passes it the sandbox's private Docker socket. |
| [`.github/workflows/sandbox-explorer.md`](.github/workflows/sandbox-explorer.md) | Human-authored agentic workflow and prompt. |
| [`.github/workflows/sandbox-explorer.lock.yml`](.github/workflows/sandbox-explorer.lock.yml) | Compiler-generated GitHub Actions workflow; do not edit manually. |

## Run the seed test locally

Docker is the only local prerequisite:

```bash
./scripts/test-in-docker.sh
```

The Maven and PostgreSQL images are pinned by version and digest for repeatable runs.

## Run the agentic workflow

The sample uses the Copilot engine and requires:

- `DOCKER_USERNAME` and `DOCKER_PAT` Actions secrets for pulling
  `docker/sandbox-templates:shell-docker`.
- Either an organization Copilot subscription with `copilot-requests: write` or a supported
  `COPILOT_GITHUB_TOKEN` secret.
- Repository Actions settings that permit GitHub Actions to create pull requests.

Install the compiler, regenerate the workflow, and commit both files:

```bash
gh extension install github/gh-aw
gh aw compile sandbox-explorer
git add .github/workflows/sandbox-explorer.md \
  .github/workflows/sandbox-explorer.lock.yml
git commit -m "Compile Docker Sandboxes sample workflow"
git push
```

Then start and watch the workflow:

```bash
gh aw run sandbox-explorer
gh run watch
```

## Runner requirements

The workflow currently uses a GitHub-hosted `ubuntu-24.04` runner. Docker Sandboxes needs
working KVM, access to `/dev/kvm`, passwordless sudo, Docker Engine, and an apt-based Linux
distribution. When the runner is itself a virtual machine, nested virtualization must be
available.

GitHub describes nested virtualization on hosted runners as technically possible but
officially unsupported, so availability can change. Run the manual `Hosted runner KVM
probe` workflow to check the current hosted image. If it fails, use a compatible Ubuntu
24.04 self-hosted runner, update `runs-on` in the Markdown workflow, and recompile it.

Do not preinstall `docker-sbx`; the compiled workflow owns installation and preflight.

## Expected result

A successful run shows:

- The generated KVM and Docker Sandbox preflight succeeding.
- `docker version`, `docker info`, and an Alpine container running inside the microVM.
- Testcontainers starting PostgreSQL through the sandbox's private Docker daemon.
- The new case-variation test failing against the seeded implementation.
- The full suite passing after the agent's correction.
- A separate safe-output job opening a draft PR whose patch touches only `src/**`.
- Docker Sandbox cleanup at the end of the agent job.

See the repository's [successful sample run](https://github.com/shelajev/docker-sandbox-gh-aw-demo/actions/runs/31590806747)
and its [draft pull request](https://github.com/shelajev/docker-sandbox-gh-aw-demo/pull/1).

This repository is an executable companion sample: the surrounding article can explain
Docker Sandbox templates, kits, network policy, and organization governance in more depth,
while the code here stays focused on the GitHub Agentic Workflows integration.
