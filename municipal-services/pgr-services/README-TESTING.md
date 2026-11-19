# pgr-services — testing

```bash
mvn test              # unit (*Test.java)
mvn verify            # + integration (*IT.java) + JaCoCo gate on validator package
mvn verify -Powasp    # dependency scan (after adding owasp profile from vendor-management)
```

Post-deploy: `core-smoke-tests` with `SERVICE=pgr-services`.
