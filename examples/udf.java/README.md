# Zillabase UDF Java Example

This example demonstrates how to register external User Defined Functions (UDFs) in the UDF server(zillabase) and declare and use these UDFs.

#### Build external User Defined Functions (UDFs)

```bash
cd zillabase/functions/java/risingwave-java-udf-template/
./mvnw clean install
```

Output:

```text
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

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
latest: Pulling from aklivity/zilla
latest: Pulling from aklivity/zilla
3.2.3: Pulling from bitnami/kafka
latest: Pulling from risingwavelabs/risingwave
latest-release: Pulling from apicurio/apicurio-registry-mem
latest: Pulling from bitnami/keycloak
latest: Pulling from aklivity/zillabase/udf-server
```

#### Connect to psql endpoint expose through Admin service

```bash
psql -U root -d dev -h localhost -p 4567
```

#### Validate defined functions:

```bash
SELECT gcd(25, 15);
```

Output:

```text
 gcd
-----
   5
(1 row)
```

#### Stop `zillabase` stack

```bash
zillabase stop
```
