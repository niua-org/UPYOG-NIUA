# community-hall-booking — testing

```bash
mvn test                    # unit; excludes *IT.java
mvn verify                  # runs *IT.java (Testcontainers Postgres/Redis)
```

Integration tests require Docker. Post-deploy smoke: `SERVICE=community-hall-booking`.
