const express = require('express');
const WebSocket = require('ws');
const { Client } = require('pg');

const app = express();
const port = 8080;

const client = new Client({
    user: 'postgres',
    host: 'localhost',
    database: 'zillabase',
    password: 'Shaikh',
    port: 5432,
});

client.connect();

const wss = new WebSocket.Server({ noServer: true });

wss.on('connection', (ws) => {
    console.log('New WebSocket connection');

    ws.on('message', async (message) => {
        const { query, type } = JSON.parse(message);

        try {
            const res = await client.query(query);
            ws.send(JSON.stringify({
                data: res.rows,
                type
            }));
        } catch (error) {
            ws.send(JSON.stringify({ error: 'Query failed' }));
        }
    });
});

app.server = app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});

app.server.on('upgrade', (request, socket, head) => {
    wss.handleUpgrade(request, socket, head, (ws) => {
        wss.emit('connection', ws, request);
    });
});
