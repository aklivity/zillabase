import { boot } from 'quasar/wrappers'
import WebSocketService from 'src/services/webSocket'

export default boot(({ app }) => {
    app.config.globalProperties.$ws = WebSocketService
})