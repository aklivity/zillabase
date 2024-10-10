# Zillabase Streampay

This is an implementation of the Streampay App designed to transfer payments from one user to another.
Zilla is implementing the REST endpoints defined in an AsyncAPI 3.x spec and proxying them onto Kafka topics defined in an AsyncAPI 3.x spec based on the operations defined in each spec.

Both HTTP AsyncAPI 3.x spec & Kafka AsyncAPI 3.x spec are generated automatically based on the Kafka Cluster metadata information.

## Install `zillabase`

```bash
brew tap aklivity/tap

brew install zillabase
```

## Start `zillabase` stack

```bash
zillabase start
```

Output:

```text
latest: Pulling from aklivity/zillabase
...
seed.sql processed successfully!
...
Realm: zillabase created successfully.
...
Config Server is populated with zilla.yaml
```

## Using the Streampay APIs

The Zillabase Streampay is exposes common entity CRUD endpoints with the entity data being stored on Kafka topics if Kafka's cleanup.policy=compact otherwise it exposes only Read mode endpoints.

### Endpoints that is used to build the app

| Protocol | Method | Endpoint                           | Topic                          | Description                                  |
|----------|--------|------------------------------------|--------------------------------|----------------------------------------------|
| HTTP     | POST   | /streampay_users                   | dev.streampay_users            | Create an user.                              |
| HTTP     | PUT    | /streampay_users/{id}              | dev.streampay_users            | Update user by the key.                      |
| HTTP     | GET    | /streampay_users                   | dev.streampay_users            | Fetch all users.                             |
| HTTP     | GET    | /streampay_users/{id}              | dev.streampay_users            | Fetch user by the key.                       |
| HTTP     | GET    | /streampay_balances-stream         | dev.streampay_balances         | Stream latest user's balance.                |
| HTTP     | POST   | /streampay_commands                | dev.streampay_commands         | Post command such as payment request or pay. |
| HTTP     | GET    | /streampay_payment_requests        | dev.streampay_payment_requests | Fetch all payment requests.                  |
| HTTP     | GET    | /streampay_payment_requests/{id}   | dev.streampay_payment_requests | Fetch payment request by key.                |
| HTTP     | GET    | /streampay_payment_requests-stream | dev.streampay_payment_requests | Stream new available payment request.        |
| HTTP     | GET    | /streampay_activities-stream       | dev.streampay_activities       | Stream all the activities.                   |

## Setup StreamPay UI

### Install the dependencies

```bash
yarn
# or
npm install
```

### Start the app in development mode (hot-code reloading, error reporting, etc.)

```bash
quasar dev
```

Note: You can install quasar via `npm install -g @quasar/cli`, and you might need execute `rehash` to find `quasar` on the path afterwards.

### Login

Login with one of the users pre-created in `zillabase/config.yaml`

#### Stop `zillabase` stack

```bash
zillabase stop
```
