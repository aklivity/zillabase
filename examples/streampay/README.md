# StreamPay App (streampay-app)

StreamPay Project

### Start zillabase
```bash
java -jar ../../cli/target/cli-develop-SNAPSHOT.jar start
```

## Install the dependencies
```bash
yarn
# or
npm install
```

### Start the app in development mode (hot-code reloading, error reporting, etc.)
```bash
quasar dev
```

### Login

Login with one of the users pre-created in `zillabase/config.yaml`

### Deposit initial balance

When you login the new user will be added into `dev.streampay_users` topic use id from that user to deposit initial balance;


```bash
./initial_balance.sh USER_ID
```

Replace `USER_ID` with one you got from topic as explained above.
