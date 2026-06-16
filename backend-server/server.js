const { WebSocketServer } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('AI Fit Tracker Synchronization Server\n');
});

const wss = new WebSocketServer({ server });

// In-memory states
const rooms = {}; // roomId -> Array of { ws, username, reps, state }
const roomMetadata = {}; // roomId -> { id, name, hostName, exerciseType, maxParticipants, participantCount }
const chats = {}; // coachId <-> traineeId chat history
let leaderboard = [
    { username: "@gym_bro", score: 1420 },
    { username: "@yoga_mind", score: 950 },
    { username: "@plank_master", score: 2100 },
    { username: "@fit_queen", score: 1850 },
    { username: "@iron_man", score: 1200 }
];

console.log(`Starting real-time server on port ${PORT}...`);

wss.on('connection', (ws) => {
    let currentUser = null;
    let currentRoomId = null;

    console.log('New client connected.');

    ws.on('message', (messageBuffer) => {
        try {
            const message = JSON.parse(messageBuffer.toString());
            console.log('Received action:', message.action, message);

            switch (message.action) {
                case 'join_room': {
                    const { roomId, username } = message;
                    currentUser = username;
                    currentRoomId = roomId;

                    if (!rooms[roomId]) {
                        rooms[roomId] = [];
                    }

                    // Remove if already exists in this room
                    rooms[roomId] = rooms[roomId].filter(u => u.username !== username);

                    // Add new user
                    rooms[roomId].push({ ws, username, reps: 0, state: "READY" });

                    console.log(`${username} joined room ${roomId}`);

                    // Update participant count
                    if (roomMetadata[roomId]) {
                        roomMetadata[roomId].participantCount = rooms[roomId].length;
                    } else {
                        roomMetadata[roomId] = {
                            id: roomId,
                            name: `Room ${roomId.slice(0, 4)}`,
                            hostName: username,
                            exerciseType: 'SQUAT',
                            maxParticipants: 4,
                            participantCount: rooms[roomId].length
                        };
                    }
                    broadcastRoomsList();

                    // Notify everyone in the room
                    broadcastToRoom(roomId, {
                        type: 'room_update',
                        users: rooms[roomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                    });
                    break;
                }

                case 'create_room': {
                    const { roomId, name, hostName, exerciseType, maxParticipants } = message;
                    roomMetadata[roomId] = {
                        id: roomId,
                        name: name,
                        hostName: hostName,
                        exerciseType: exerciseType,
                        maxParticipants: maxParticipants,
                        participantCount: 1
                    };
                    console.log(`Room created: ${name} (ID: ${roomId}) by ${hostName}`);
                    broadcastRoomsList();
                    break;
                }

                case 'get_rooms': {
                    sendRoomsList(ws);
                    break;
                }

                case 'rep_update': {
                    const { reps, state } = message;
                    if (currentRoomId && currentUser && rooms[currentRoomId]) {
                        const user = rooms[currentRoomId].find(u => u.username === currentUser);
                        if (user) {
                            user.reps = reps;
                            user.state = state;
                            console.log(`Rep update: ${currentUser} in room ${currentRoomId} now has ${reps} reps (${state})`);

                            broadcastToRoom(currentRoomId, {
                                type: 'room_update',
                                users: rooms[currentRoomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                            });
                        }
                    }
                    break;
                }

                case 'leave_room': {
                    handleLeaveRoom();
                    break;
                }

                case 'send_chat': {
                    const { sender, messageText, timestamp } = message;
                    console.log(`Chat from ${sender}: "${messageText}"`);

                    // Broadcast chat message to everyone connected (or simulate PT coach response if no other user is connected)
                    // For demo purposes, we broadcast it back to client, and if it's sent to Coach, we can trigger a real-time smart bot response!
                    ws.send(JSON.stringify({
                        type: 'chat_receive',
                        sender: sender,
                        messageText: messageText,
                        timestamp: timestamp
                    }));

                    // Simulated PT Coach auto-response
                    if (sender !== '@pt_coach') {
                        setTimeout(() => {
                            const botResponse = getSmartCoachResponse(messageText);
                            ws.send(JSON.stringify({
                                type: 'chat_receive',
                                sender: '@pt_coach',
                                messageText: botResponse,
                                timestamp: Date.now()
                            }));
                        }, 1500);
                    }
                    break;
                }

                case 'get_leaderboard': {
                    // Send leaderboard
                    ws.send(JSON.stringify({
                        type: 'leaderboard_data',
                        data: leaderboard.sort((a, b) => b.score - a.score)
                    }));
                    break;
                }

                case 'submit_score': {
                    const { username, score } = message;
                    console.log(`Score submitted: ${username} -> ${score}`);
                    
                    const existingUser = leaderboard.find(u => u.username === username);
                    if (existingUser) {
                        existingUser.score = Math.max(existingUser.score, score);
                    } else {
                        leaderboard.push({ username, score });
                    }

                    // Send updated leaderboard to everyone requesting
                    ws.send(JSON.stringify({
                        type: 'leaderboard_data',
                        data: leaderboard.sort((a, b) => b.score - a.score)
                    }));
                    break;
                }
            }
        } catch (err) {
            console.error('Error handling message:', err);
        }
    });

    ws.on('close', () => {
        console.log(`Client disconnected: ${currentUser}`);
        handleLeaveRoom();
    });

    function handleLeaveRoom() {
        if (currentRoomId && currentUser && rooms[currentRoomId]) {
            rooms[currentRoomId] = rooms[currentRoomId].filter(u => u.username !== currentUser);
            console.log(`${currentUser} left room ${currentRoomId}`);

            if (rooms[currentRoomId].length === 0) {
                delete rooms[currentRoomId];
                delete roomMetadata[currentRoomId];
            } else {
                if (roomMetadata[currentRoomId]) {
                    roomMetadata[currentRoomId].participantCount = rooms[currentRoomId].length;
                }
                broadcastToRoom(currentRoomId, {
                    type: 'room_update',
                    users: rooms[currentRoomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                });
            }
            broadcastRoomsList();
            currentRoomId = null;
        }
    }
});

function broadcastToRoom(roomId, data) {
    if (rooms[roomId]) {
        const payload = JSON.stringify(data);
        rooms[roomId].forEach(user => {
            if (user.ws.readyState === 1) { // OPEN
                user.ws.send(payload);
            }
        });
    }
}

function broadcastRoomsList() {
    const data = JSON.stringify({
        type: 'rooms_list',
        data: Object.values(roomMetadata)
    });
    wss.clients.forEach(client => {
        if (client.readyState === 1) { // OPEN
            client.send(data);
        }
    });
}

function sendRoomsList(ws) {
    if (ws.readyState === 1) { // OPEN
        ws.send(JSON.stringify({
            type: 'rooms_list',
            data: Object.values(roomMetadata)
        }));
    }
}

function getSmartCoachResponse(userMessage) {
    const msg = userMessage.toLowerCase();
    if (msg.includes('squat')) {
        return "Tư thế Squat của bạn hôm nay tốt lắm! Đùi đã đạt góc dưới 95 độ chưa? Nhớ đẩy hông ra sau và không để đầu gối vượt quá mũi chân nhé.";
    } else if (msg.includes('plank')) {
        return "Khi Plank hãy siết chặt cơ bụng và đùi. Lưng - hông - gót chân phải thẳng hàng. Nếu thấy mỏi hông hãy nghỉ 30s rồi tập tiếp.";
    } else if (msg.includes('mỏi') || msg.includes('đau')) {
        return "Đó là phản ứng bình thường khi các sợi cơ được kích hoạt! Hãy nghỉ ngơi đầy đủ, bổ sung protein (Whey/ức gà) và giãn cơ sau buổi tập.";
    } else if (msg.includes('chào') || msg.includes('hi') || msg.includes('hello')) {
        return "Chào bạn! Hôm nay kế hoạch tập luyện của bạn thế nào? Cần tôi tư vấn thêm về bài tập hay dinh dưỡng không?";
    } else {
        return "Chào bạn! Tôi đã nhận được phản hồi của bạn. Hãy giữ thói quen tập luyện đều đặn và tiếp tục phát huy nhé!";
    }
}

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Real-time synchronization server is running on http://localhost:${PORT}`);
});
