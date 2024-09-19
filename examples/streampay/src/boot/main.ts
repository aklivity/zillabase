import {boot} from 'quasar/wrappers'
import { domain, clientId as client_id } from '../../auth_config.json';
import { createAuth0 } from '@auth0/auth0-vue';

// "async" is optional;
// more info on params: https://v2.quasar.dev/quasar-cli/boot-files
export default boot( ({ app}) => {
  app.use(
    createAuth0({
      domain,
      client_id,
      redirect_uri: `http://localhost:8080/#/main`,
      audience: 'https://localhost:9090/streampay',
      scope: 'read:users write:users read:payment-requests write:pay write:request read:activities read:balances'
    })
  )
})
