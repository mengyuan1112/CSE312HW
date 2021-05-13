// Establish a WebSocket connection with the server
socket = new WebSocket('ws://' + window.location.host + '/websocket');

// Call the addMessage function whenever data is received from the server over the WebSocket
socket.onmessage = addMessage;

// Allow users to send messages by pressing enter instead of clicking the Send button
document.addEventListener("keypress", function (event) {
    if (event.code === "Enter") {
        sendMessage();
    }
});

// Read the name/comment the user is sending to chat and send it to the server over the WebSocket as a JSON string
// Called whenever the user clicks the Send button or pressed enter
function sendMessage() {
    const chatName = document.getElementById("chat-name").value;
    const chatBox = document.getElementById("chat-comment");
    const comment = chatBox.value;
    chatBox.value = "";
    chatBox.focus();
    if(comment !== "") {
        socket.send(JSON.stringify({'username': chatName, 'comment': comment}));
    }
}

// Called when the server sends a new message over the WebSocket and renders that message so the user can read it
function addMessage(message) {
    const chatMessage = JSON.parse(message.data);
    let chat = document.getElementById('chat');
    chat.innerHTML += "<b>" + chatMessage['username'] + "</b>: " + chatMessage["comment"] + "<br/>";
}


function hello() {
    alert("if continue loading page for submit, please click submit again. With unexpected issue, restart container will be solve");
}
function useless() {
    document.getElementById("useless").innerHTML = "I have told you, I am useless.... It's waste of your time";

}


function generateTitle() {

    const urlSearch = new URLSearchParams(window.location.search);
    const name = urlSearch.get("name");
    const picture = urlSearch.get("images");
    // console.log(name)
    document.getElementById("welcome").innerText = "Welcome " + name
    // var pic = document.createElement('img')
    var picSrc = picture.split(" ");

    for(i = 0; i < picSrc.length; i++) {
        var pic = document.createElement('img')
        pic.src = "/image/" + picSrc[i]
        document.getElementById('body').appendChild(pic)
    }

}

