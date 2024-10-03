import { boot } from 'quasar/wrappers'
import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8080' })
const streamingUrl = 'http://localhost:8080';

export default boot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
  app.config.globalProperties.streamingUrl = streamingUrl;
})

export { axios, api, streamingUrl }
