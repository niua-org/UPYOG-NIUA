# ws-services — testing

```bash
mvn test
mvn verify
RUN_INTEGRATION_TESTS=true mvn verify
mvn verify -Powasp
```

Post-deploy smoke: `core-smoke-tests` with `SERVICE=ws-services`.
