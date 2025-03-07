import { showError } from './notification';

const WebSocketService = {
    ws: null,
    messageHandlers: null,

    connect(onOpenCallback) {
        if (onOpenCallback) {
            onOpenCallback();
        }

        this.ws = {};
    },

    async sendMessage(message, type) {
        console.log(message, type)
        try {
            const res = await window.zillabaseActions.executeQuery(message, type);
            this.messageHandlers({
                data: res.data,
                type
            });
        } catch (error) {
            console.log(type, error);
            showError('Failed to Run the Query')
        }
    },

    addMessageHandler(handler) {
        this.messageHandlers = handler;
    },

    removeAll() {
        this.messageHandlers = null;
    }
};

export default WebSocketService;
