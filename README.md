# KeyFalls API

API REST para gestão de concursos públicos desenvolvida em **Java 21**, **Spring Boot 3.3.5**, **Spring Data JPA / Hibernate** e banco **H2 em memória**.

## Tecnologias
- Java 21
- Spring Boot 3.3.5
- Spring Data JPA / Hibernate
- Banco H2 em memória (`jdbc:h2:mem:keyfallsdb`)
- Spring Security (JWT)
- Swagger / OpenAPI 3

## Como executar
Entre na pasta do projeto e rode com o Maven:

```bash
cd keyfalls-api
mvn spring-boot:run
```

## Links locais
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **H2 Console:** `http://localhost:8080/h2-console`

## Testes
```bash
cd keyfalls-api
mvn test
```
