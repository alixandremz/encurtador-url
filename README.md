# encurtador-url

API REST de encurtador de URL feita para **estudar Spring Boot e Spring Web na prática**.
O objetivo não é ser um produto, e sim um projeto onde cada conceito novo (REST, JPA,
validação, cache, banco real, containers) entra de forma incremental e explicada.

Exemplo de uso: `POST /links` com uma URL longa devolve um `slug` curto. Quem abrir
`GET /{slug}` é redirecionado para a URL original, e o acesso é contado. Links podem
ter data de expiração.

---

## O que estou estudando aqui

| Área | Conceitos praticados |
|------|----------------------|
| Spring Web (REST) | `@RestController`, `@GetMapping`/`@PostMapping`, `@PathVariable`, `@RequestBody`, `ResponseEntity`, códigos HTTP (201, 302, 400, 404, 410) |
| Spring Data JPA | entidade `@Entity`, `JpaRepository`, query derivada (`findBySlug`), `existsBySlug`, `@Query` com `@Modifying` (update atômico) |
| Bean Validation | `@NotBlank`, `@Size`, `@Pattern`, `@Future`, `@Valid`, mensagens de erro por campo |
| Tratamento de erros | `@RestControllerAdvice`, `@ExceptionHandler`, exceções de domínio (`LinkNaoEncontradoException`, `LinkExpiradoException`) |
| DTOs | `record` como request/response, factory method (`LinkResponse.de`), não expor a entidade direto |
| Regras de negócio | geração de slug em Base62, colisão de slug, expiração de link, contador de cliques |
| Cache | Spring Cache (`@Cacheable`), `CacheManager` em memória vs Redis, efeito de proxy do Spring |
| Banco de dados | H2 em memória (dev) e PostgreSQL (perfil docker), `ddl-auto`, pool Hikari |
| Docker | Dockerfile multi-stage (build com JDK, runtime com JRE, usuário sem privilégios), `docker-compose` com app + banco + cache |
| Testes | `@SpringBootTest`, `MockMvc`, `jsonPath`, testes de caminho felizes e de erro |

---

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Spring Cache, Spring Data Redis
- H2 (desenvolvimento) / PostgreSQL (docker)
- Redis (cache no perfil docker)
- Maven (via Maven Wrapper `./mvnw`, não precisa ter Maven instalado)

---

## Arquitetura

Organização em camadas, cada uma com uma responsabilidade:

```
HTTP  ──►  web (Controller)  ──►  service (regras de negócio)  ──►  repository (JPA)
                 │                         │                              │
                 │                         ▼                              ▼
          ErroResposta /             LinkResolvedor                 Link (entidade)
          RestExceptionHandler       (@Cacheable)                   tabela "links"
```

- **web** — recebe a requisição HTTP, valida o corpo (`@Valid`) e devolve JSON/redirect.
  Não tem regra de negócio. Também concentra o tratamento de erros.
- **service** — regras de negócio: gerar slug, checar expiração, contar clique.
- **repository** — acesso ao banco via Spring Data JPA.
- **model** — entidade `Link` mapeada na tabela `links`.
- **dto** — objetos de entrada/saída (`LinkRequest`, `LinkResponse`), separados da entidade.

### Estrutura de pastas

```
src/main/java/com/alixandremz/encurtador/
├── EncurtadorUrlApplication.java   # main + @EnableCaching
├── model/
│   └── Link.java                   # entidade (tabela links)
├── repository/
│   └── LinkRepository.java         # JpaRepository + update atômico de cliques
├── service/
│   ├── LinkService.java            # regras de negócio
│   ├── LinkResolvedor.java         # busca cacheada (@Cacheable)
│   ├── LinkResolvido.java          # objeto cacheado (url + expiraEm)
│   ├── LinkNaoEncontradoException.java
│   └── LinkExpiradoException.java
├── dto/
│   ├── LinkRequest.java
│   └── LinkResponse.java
└── web/
    ├── LinkController.java         # endpoints
    ├── ErroResposta.java           # formato padrão de erro
    └── RestExceptionHandler.java   # @RestControllerAdvice
```

---

## Endpoints

| Método | Caminho | Descrição | Sucesso | Erros |
|--------|---------|-----------|---------|-------|
| POST | `/links` | cria um link curto | `201 Created` | `400` validação |
| GET | `/{slug}` | redireciona e conta o clique | `302 Found` | `404` não existe, `410` expirado |
| GET | `/links/{slug}` | estatísticas do link | `200 OK` | `404` |

### POST /links

Cria o link. `expiraEm` é opcional (se omitido, o link nunca expira).

```bash
curl -X POST http://localhost:8080/links \
  -H 'Content-Type: application/json' \
  -d '{"urlOriginal":"https://spring.io/guides","expiraEm":"2030-01-01T00:00:00Z"}'
```

Resposta (`201 Created`):

```json
{
  "slug": "aB3xY9z",
  "urlOriginal": "https://spring.io/guides",
  "urlCurta": "http://localhost:8080/aB3xY9z",
  "cliques": 0,
  "expiraEm": "2030-01-01T00:00:00Z",
  "criadoEm": "2026-10-14T17:40:00Z"
}
```

Erro de validação (`400 Bad Request`):

```json
{
  "erro": "dados inválidos",
  "campos": { "urlOriginal": "a url deve começar com http:// ou https://" }
}
```

### GET /{slug}

Redireciona (302) para a URL original e incrementa o contador de cliques.

```bash
curl -i http://localhost:8080/aB3xY9z
# HTTP/1.1 302 Found
# Location: https://spring.io/guides
```

### GET /links/{slug}

Retorna os dados do link, incluindo quantos cliques ele já teve.

```bash
curl http://localhost:8080/links/aB3xY9z
```

---

## Decisões e detalhes técnicos

Coisas que valem a pena entender no código:

- **Slug em Base62 com retry.** O slug é um texto aleatório de 7 caracteres usando
  `0-9A-Za-z` (62 possibilidades). Isso dá ~3,5 trilhões de combinações. `SecureRandom`
  gera o valor e o service repete a geração enquanto `existsBySlug` indicar colisão.
  Foi escolhido em vez de usar o `id` do banco para não expor a sequência de criação.
- **Contagem de cliques atômica.** Em vez de ler e gravar (`cliques = cliques + 1` em
  Java), o repositório executa `update Link l set l.cliques = l.cliques + 1 where slug = :slug`.
  Assim dois cliques simultâneos não se sobrescrevem (evita o problema de *lost update*).
- **Expiração checada mesmo com cache.** O `LinkResolvedor` cacheia a URL **e** a data de
  expiração. A checagem de expiração roda no `LinkService` depois de ler o cache, então um
  link expirado deixa de funcionar mesmo estando em cache. Link expirado devolve `410 Gone`
  (o recurso existiu, mas não está mais disponível).
- **`LinkResolvedor` é um bean separado.** `@Cacheable` do Spring só entra em ação quando a
  chamada passa pelo *proxy* gerado pelo Spring. Se o método fosse chamado de dentro da
  própria classe (`this.resolver(...)`), o cache seria ignorado. Por isso a resolução foi
  para uma classe própria.
- **Perfis do Spring mudam a infraestrutura sem mudar o código.**
  - Perfil padrão (dev): H2 em memória + cache em memória (`spring.cache.type=simple`).
    Roda sem instalar nada.
  - Perfil `docker`: PostgreSQL + Redis. Mesmo código, só muda a configuração.
- **Tratamento de erro centralizado.** As exceções de negócio viram respostas HTTP no
  `RestExceptionHandler`, no mesmo formato (`{ "erro": ..., "campos": {...} }`).
- **Dockerfile multi-stage.** O *build* acontece numa imagem JDK com o Maven Wrapper e a
  imagem final usa só o JRE, rodando com um usuário sem privilégios (`appuser`).

---

## Como rodar

### Opção 1 — local, sem instalar banco (mais rápido)

Usa H2 em memória e cache em memória. Os dados somem ao parar a aplicação.

```bash
cd encurtador-url
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Opção 2 — stack completo com Docker (Postgres + Redis + app)

Precisa do Docker com o plugin Compose.

```bash
cd encurtador-url
docker compose up --build
```

Sobe:
- `app` → a API em `http://localhost:8080` (perfil `docker`)
- `db` → PostgreSQL em `localhost:5432`
- `redis` → Redis em `localhost:6379`

Os dados do Postgres ficam no volume `pgdata`, então sobrevivem a `docker compose down`
(use `docker compose down -v` para apagar tudo).

---

## Testes

```bash
./mvnw test
```

Os testes rodam sempre com H2 e cache em memória (não precisam de Postgres nem Redis).
Cobrem:

- criação com `201`, geração de slug e montagem da `urlCurta`;
- redirecionamento (`302`) e `Location` correto;
- contagem de cliques após dois acessos;
- slug inexistente → `404`;
- URL inválida / em branco → `400` com o campo de erro;
- data de expiração no passado → `400`;
- link expirado → `410`.

---

## Próximos passos (ideias para continuar estudando)

- Migrations com **Flyway** em vez de `ddl-auto=update`.
- Autenticação com **Spring Security + JWT** (só quem criou pode ver as estatísticas).
- **Rate limiting** por IP com um `Filter`/`Interceptor` do Spring Web.
- Customizar um slug (ex.: `/links` aceitar um `slug` desejado, com validação).
- Deletar/expirar links em lote com um job agendado (`@Scheduled`).
- Publicar métricas com **Actuator + Micrometer** e observar o cache (hit/miss).
