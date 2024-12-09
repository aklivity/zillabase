const WebSocketService = {
    ws: null,
    messageHandlers: [],

    connect(onOpenCallback) {
        if (this.ws) {
            if (onOpenCallback) {
                onOpenCallback();
            }
            return;
        };

        this.ws = new WebSocket('ws://localhost:8080');

        this.ws.onopen = () => {
            console.log('WebSocket connection established.');
            if (onOpenCallback) {
                onOpenCallback();
            }
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.messageHandlers.forEach((handler) => handler(data));
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        this.ws.onclose = () => {
            console.log('WebSocket connection closed.');
        };
    },

    sendMessage(message, type) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({
                query: message,
                type
            }));
        } else {
            console.error('WebSocket is not open');
        }
    },

    addMessageHandler(handler) {
        this.messageHandlers.push(handler);
    },

    removeMessageHandler(handler) {
        this.messageHandlers = this.messageHandlers.filter((h) => h !== handler);
    },

    removeAll() {
        this.messageHandlers = [];
    }
};

export default WebSocketService;
