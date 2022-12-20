# BULK PROCESSING

---

## Initial Setup
```bash
# 1. create database `bulk`
# 2. create tables with following command (see. flyway.local.conf)
$ mvn -Dflyway.configFiles=flyway.conf flyway:migrate
```

## Run Locally
```bash
# start the app
$ mvn spring-boot:run
```

Enjoy!!!
