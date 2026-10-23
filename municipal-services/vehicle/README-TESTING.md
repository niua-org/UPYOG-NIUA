# vehicle — testing

```bash
mvn test
mvn verify
RUN_INTEGRATION_TESTS=true mvn verify   # Testcontainers IT
mvn verify -Powasp
```

Post-deploy smoke: `core-smoke-tests` with `SERVICE=vehicle`.
