# Zillabase Petstore

This is an implementation of the common Petstore example where requests are proxied to Kafka. Zilla is implementing the REST endpoints defined in an AsyncAPI 3.x spec and proxying them onto Kafka topics defined in an AsyncAPI 3.x spec based on the operations defined in each spec.

Both HTTP AsyncAPI 3.x spec & Kafka AsyncAPI 3.x spec are generated automatically based on the Kafka Cluster metadata information.

#### Install `zillabase`

```bash
brew tap aklivity/tap

brew install zillabase
```

#### Start `zillabase` stack:

```bash
zillabase start
```

Output:

```text
3.2.3: Pulling from bitnami/kafka
latest: Pulling from risingwavelabs/risingwave
latest-release: Pulling from apicurio/apicurio-registry-mem
latest: Pulling from bitnami/keycloak
seed-kafka.yaml processed successfully!
Registered AsyncAPI spec: kafka-asyncapi
Registered AsyncAPI spec: http-asyncapi
Config Server is populated with zilla.yaml
```

### Using the Petstore APIs

The Zillabase Petstore is an HTTP Kafka proxy and exposes common entity CRUD endpoints with the entity data being stored on Kafka topics. Leveraging Kafka's cleanup.policy=compact feature.

### Endpoints

| Protocol | Method | Endpoint            | Topic         | Description            |
|----------|--------|---------------------|---------------|------------------------|
| HTTP     | POST   | /petstore_pets      | petstore_pets | Create an entry.       |
| HTTP     | PUT    | /petstore_pets/{id} | petstore_pets | Update pet by the key. |
| HTTP     | DELETE | /petstore_pets/{id} | petstore_pets | Delete pet by the key. |
| HTTP     | GET    | /petstore_pets      | petstore_pets | Fetch all pets.        |
| HTTP     | GET    | /petstore_pets/{id} | petstore_pets | Fetch pet by the key.  |

Similarly, endpoints are avaiable to manage customers using `/petstore_customers` & `/petstore_verified_customers`

##### Example:

##### Publish a valid record

```bash
curl -k -v -X POST http://localhost:8080/petstore_pets -H 'Idempotency-Key: 1'  -H 'Content-Type: application/json' -d '{"id": "123","breed": "Awesome Dog"}'
```

Output:

```text
> POST /petstore_pets HTTP/1.1
...
> Content-Type: application/json
> Content-Length: 35
>
< HTTP/1.1 204 No Content
< Access-Control-Allow-Origin: *
<
* Connection #0 to host localhost left intact
```

##### Fetch a record

```bash
curl -k -v http://localhost:8080/petstore_pets/1
```

Output:

```text
> GET /petstore_pets/1 HTTP/1.1
...
< HTTP/1.1 200 OK
< Content-Length: 33
< Content-Type: application/json
< Etag: AQIAAg==
< Access-Control-Allow-Origin: *
< Access-Control-Expose-Headers: *
<
* Connection #0 to host localhost left intact
{"id":"123","breed":"Common Dog"}%
```

#### Stop `zillabase` stack

```bash
zillabase stop
```
