# Zillabase Quickstart

A quickstart example to get started with Zillabase.

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

### Using the generated API Endpoints

The Zillabase quickstart is an HTTP Kafka proxy and exposes common entity CRUD endpoints with the entity data being stored on Kafka topics. Leveraging Kafka's cleanup.policy=compact feature.

### Endpoints

| Protocol | Method | Endpoint     | Topic  | Description                     |
|----------|--------|--------------|--------|---------------------------------|
| HTTP     | POST   | /events      | events | Create an event.                |
| HTTP     | PUT    | /events/{id} | events | Update an event by the key.     |
| HTTP     | DELETE | /events/{id} | events | Delete event by the key.        |
| HTTP     | GET    | /events      | events | Fetch all events.               |
| HTTP     | GET    | /events/{id} | events | Fetch event by the key.         |


##### Example:

##### Publish a valid record

```bash
curl -k -v -X POST http://localhost:8080/events -H 'Idempotency-Key: 1'  -H 'Content-Type: application/json' -d '{"id": "101","message": "Hello, World"}'
```

Output:

```text
> POST /events HTTP/1.1
...
> Content-Type: application/json
>
< HTTP/1.1 204 No Content
< Access-Control-Allow-Origin: *
<
* Connection #0 to host localhost left intact
```

##### Fetch a record

```bash
curl -k -v http://localhost:8080/events/1
```

Output:

```text
> GET /events/1 HTTP/1.1
...
< HTTP/1.1 200 OK
< Content-Length: 33
< Content-Type: application/json
< Etag: AQIAAg==
< Access-Control-Allow-Origin: *
< Access-Control-Expose-Headers: *
<
* Connection #0 to host localhost left intact
{"id": "101","message": "Hello, World"}%
```

#### Stop `zillabase` stack

```bash
zillabase stop
```
