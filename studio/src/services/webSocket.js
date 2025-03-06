import {showError} from "src/services/notification";

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
        try {
            const res = await window.zillabaseActions.executeQuery(message, type);
            this.messageHandlers({
                data: res.data,
                type
            });
        } catch (error) {
            showError(error.message);
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
