let stompClient;
let channelId = '123'; // Substitua por seu ID de canal

function connect() {
    let socket = new SockJS('http://localhost:8080/chat');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/channel/' + channelId, function(messageOutput) {
            showMessageOutput(JSON.parse(messageOutput.body));
        });
    });
}

function disconnect() {
    if(stompClient != null) {
        stompClient.disconnect();
    }
    console.log("Disconnected");
}

function sendMessage() {
    let messageInput = document.querySelector('#message');
    let message = messageInput.value;
    stompClient.send("/app/chat/" + channelId, {}, JSON.stringify({'text': message}));
    messageInput.value = '';
}

function showMessageOutput(messageOutput) {
    let messageList = document.querySelector('#messageList');
    let messageElement = document.createElement('li');
    messageElement.innerText = messageOutput.text;
    messageList.appendChild(messageElement);
}

connect();