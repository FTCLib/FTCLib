/*
Copyright (c) 2018 Noah Andrews

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Noah Andrews nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

'use strict';

// Please load es5-shim.js and es6-shim.js (in that order) before loading this file.

// This file should ONLY be loaded from within an iframe.

/**
 *  WEBSOCKET_LIB.WebSocketMessage is a constructor. Use it like so:
 *      new WEBSOCKET_LIB.WebSocketMessage('namespace', 'type', 'payload');
 *
 *  All sent and received messages are created using the WebSocketMessage constructor.
 *
 *  WEBSOCKET_LIB.webSocketManager is an object that is your interface to the WebSocket.
 *
 *  The full documentation for webSocketManager's methods can be found in the WebSocketManager
 *  constructor, located in websocket-core.js. All methods intended for your use have javadoc-style
 *  comments above them.
 *
 *  The correct sequence for using the WebSocket connection in a piece of the web UI:
 *
 *      1. Subscribe to all namespaces that you will use to communicate
 *
 *      2. Register message handlers for entire namespaces and/or types within a namespace
 *
 *      3. Register connection listeners so that you can start and stop sending messages at the
 *         appropriate times and update your UI as necessary.
 *
 *  When the user navigates away from the current iframe, everything will be cleaned up automatically.
 *
 *  If the WebSocket becomes disconnected, a connection will attempt to be reestablished
 *  automatically for as long as there is at least one namespace handler registered.
 */


// The websocket-iframe file is tied to the concept of an iframe, but WebSocketManager is not. You
// could have panes of UI that don't have their own iframes, but that have their own instance of
// WebSocketManager. You'd just need to make sure to call finish() on it when closing the pane.

const WEBSOCKET_LIB = {
    WebSocketMessage: parent.WEBSOCKET_CORE.WebSocketMessage,
    webSocketManager: new parent.WEBSOCKET_CORE.WebSocketManager()
}

parent.WEBSOCKET_CORE.openWebSocket();

window.addEventListener('unload', function (event) {
    WEBSOCKET_LIB.webSocketManager.finish();
});

