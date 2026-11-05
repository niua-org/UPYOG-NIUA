# Jenkins and smoke

Smoke tests were not executed against a live environment.

## What was added

`core-smoke-tests/Jenkinsfile` is a standalone job:

- Parameters: `ENVIRONMENT`, `SERVICE` (default `ALL`), `EXPECTED_VERSION`, plus `SMOKE_TENANT_ID` because `SmokeConfig` fails fast without it.
- Triggers: `H/30` with `SERVICE=ALL`.
- Timeout: 10 minutes.
- Stages: checkout, wait until `BASE_URL` accepts a connection (no invented health path), `mvn -Psmoke verify`, publish JUnit.
- Credentials: `smoke-base-url` (`BASE_URL`), `smoke-user-credentials` (`SMOKE_USER` / `SMOKE_PASSWORD`).
- On failure the job is failed. Notification is a log line. The existing core-services Jenkinsfile does not call mail, Slack, or any other notifier, so none was invented here.

`mvn -Psmoke verify` runs only `@Tag("smoke")`. Without `-Psmoke`, the module runs no tests.

## core-services/Jenkinsfile was not changed

`core-services/Jenkinsfile` is:

```groovy
library 'ci-libs'

buildPipeline(configFile: './build/build-config.yml')
```

There is no deploy stage, no `stages` block, and no post section. A post-deploy stage was not inserted, and `buildPipeline` was not wrapped, because that would invent a pipeline around the shared library.

Not added, for the same reason:

- `build job: 'core-smoke-tests', wait: true, propagate: true` with `SERVICE`, `ENVIRONMENT`, and `EXPECTED_VERSION`.
- Pre-deploy `mvn test`, `mvn verify`, and JaCoCo. Service `pom.xml` files were not edited, and this Jenkinsfile has nowhere to attach those goals.
- A non-blocking dependency scan.

When `ci-libs` `buildPipeline` grows a real deploy stage, the post-deploy call belongs after that stage:

```groovy
stage('post-deploy smoke') {
    steps {
        build job: 'core-smoke-tests', wait: true, propagate: true, parameters: [
            string(name: 'SERVICE', value: params.SERVICE ?: 'ALL'),
            string(name: 'ENVIRONMENT', value: params.ENVIRONMENT ?: 'dev'),
            string(name: 'EXPECTED_VERSION', value: params.EXPECTED_VERSION ?: '')
        ]
    }
}
```

`propagate: true` fails the caller when smoke fails. Notification should use whatever step `buildPipeline` already uses. None is visible in this repository.

## Dependency scan (not wired)

Keep the scan from failing the build. Either form is enough; both together are safe:

```groovy
stage('dependency-scan') {
    steps {
        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
            sh 'mvn -B org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=11'
        }
    }
}
```

`failBuildOnCVSS=11` cannot match a real CVSS score (the scale stops at 10). `catchError` keeps the build result successful if the plugin still exits non-zero. This stage is documentation only until `buildPipeline` is extended in `ci-libs`.

## Helm rollback (gap)

`core-services/Jenkinsfile` and `core-services/build/build-config.yml` do not invoke Helm. There is no automatic rollback for dev/UAT and no production approval input to add. Rollback stays outside this repo until the deploy library uses Helm.

## Parent Jenkinsfile

The repository root `Jenkinsfile` uses the same `library 'ci-libs'` / `buildPipeline` shape. It was not edited. The same gap applies there: no deploy stage in-repo to hang smoke, unit, JaCoCo, or the dependency scan on.
