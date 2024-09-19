import { boot } from 'quasar/wrappers'
import axios from 'axios'

const api = axios.create({ baseURL: 'https://localhost:9090' })
const streamingUrl = 'https://localhost:9090';

export default boot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
  app.config.globalProperties.streamingUrl = streamingUrl;
})

export { axios, api, streamingUrl }
