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

// This file should ONLY be loaded from frame.html, and is not used directly by webpages.
// iframes that need WebSocket support should load websocket-iframe.js.

// iframes can create new messages by calling new WebSocketMessage()

// we define WEBSOCKET_CORE as a var, because consts cannot be accessed by iframes
var WEBSOCKET_CORE = function () {
    // If WEBSOCKET_CORE is already defined, we don't want to overwrite it.
    if (WEBSOCKET_CORE) return WEBSOCKET_CORE;

    if (window != window.top) {
        throw new Error('websocket-core.js must only be included in the main window, not an iframe');
    }

    var globalState = {
        webSocket: null, // If this is null, then the WebSocket is CLOSED. If this is not null, it may be CONNECTING, OPEN, or CLOSING.
        connectionListeners: new Set(),
        disconnectionListeners: new Set(),
        namespaceMap: new Map(), // All namespaces in this map are subscribed to by at least one instance of WebSocketManager
        logMessages: false, // Log all incoming and outgoing messages by running WEBSOCKET_CORE.enableLogging('messages') from the parent console (not an iframe)
        logDebug: false // Enable debug logging by running WEBSOCKET_CORE.enableLogging('debug') from the parent console (not an iframe)
    };

    const SYSTEM_NAMESPACE = 'system';
    const SUBSCRIBE_TO_NAMESPACE_MESSAGE_TYPE = 'subscribeToNamespace';
    const UNSUBSCRIBE_FROM_NAMESPACE_MESSAGE_TYPE = 'unsubscribeFromNamespace';

    function WebSocketMessage(namespace, type, payload) {
        this.namespace = namespace;
        this.type = type;
        this.payload = payload ? payload : "";
    }


    //----------------------------------------------------------------------------------------------
    // WebSocketManager constructor
    //----------------------------------------------------------------------------------------------

    // There is typically one instance per iframe, created and used through websocket-iframe.js
    // There should be no reason that instances of WebSocketManager have to be tied to iframes.
    // You should be able to create a new instance for any self-contained piece of UI, not just an iframe.

    // Rules for manually-created WebSocketManager instances:
    //      1. Be careful. This use-case was considered, but not tested. Contact Noah for assistance.
    //      2. Call the finish() method when the UI section has been removed from the DOM.
    //      3. Make SURE that you don't register any of the same handler instances on two different
    //         instances of WebSocketManager. Doing this will break things when finish() is called
    //         on one of the instances.
    function WebSocketManager() {

        //----------------------------------------------------------------------------------------------
        // Private state variables
        //----------------------------------------------------------------------------------------------
        var localNamespaceMap = new Map(); // All namespaces in this map are subscribed to by this WebSocketManager instance
        var connectionListenersToDispose = new Set();
        var disconnectionListenersToDispose = new Set();

        /**
         * Send an instance of WebSocketMessage.
         *
         * Will throw an Error if the WebSocket is not open, the namespace has not been subscribed to,
         * or the message is addressed to the system namespace.
         *
         * params:
         *      message: an instance of WebSocketMessage to be sent
         */
        this.sendMessage = function (message) {
            if (!localNamespaceMap.has(message.namespace)) {
                throw new Error('Subscribe to ' + message.namespace + ' before sending a message to it');
            }

            if (message.namespace === SYSTEM_NAMESPACE) {
                throw new Error('You cannot send messages to the system namespace');
            }

            internalSendMessage(message);
        };

        /**
         * Register a connection listener and a disconnection listener.
         *
         * The connection listener will be called (with no parameters) to inform you that the
         * WebSocket connection is established and it is safe to start sending messages.
         *
         * The disconnection listener will be called (with no parameters) to notify you that the
         * WebSocket has been disconnected and that you should stop sending messages. You should not
         * assume that this will be called in the event that a connection is not established in the
         * first place, so assume that you're disconnected until the connection listener has been
         * called.
         *
         * The listeners will be unregistered automatically when the iframe is unloaded or the
         * finish() method is called.
         *
         * Be careful with what you call from the connection listener. If you're sending messages, be
         * sure that it makes sense to send them every time the connection is re-established.
         *
         * This method is idempotent.
         *
         * params:
         *      connectionListener:     A function that takes no parameters
         *      disconnectionListener:  A function that takes no parameters
         */
        this.registerConnectionStateListeners = function (connectionListener, disconnectionListener) {
            if (isWebSocketConnected()) {
                connectionListener();
            }
            connectionListenersToDispose.add(connectionListener);
            disconnectionListenersToDispose.add(disconnectionListener);
            globalState.connectionListeners.add(connectionListener);
            globalState.disconnectionListeners.add(disconnectionListener);
        };

        /**
         * Subscribe to a namespace so that we can send and receive messages within it.
         *
         * This method is idempotent.
         *
         * params:
         *      namespace: A string specify the namespace to subscribe to
         */
        this.subscribeToNamespace = function (namespace) {
            if (localNamespaceMap.has(namespace)) return; // If the namespace is in the local map, we've already subscribed.

            localNamespaceMap.set(namespace, new Namespace());

            if (globalState.namespaceMap.has(namespace)) {
                var globalNamespaceObject = globalState.namespaceMap.get(namespace);
                globalNamespaceObject.subscriberCount++;
            } else {
                globalState.namespaceMap.set(namespace, new Namespace());
                if (isWebSocketConnected()) { // In the alternative case (we're not connected), a subscription request will be sent in onopen instead of here
                    var subscriptionRequest = new WebSocketMessage(SYSTEM_NAMESPACE, SUBSCRIBE_TO_NAMESPACE_MESSAGE_TYPE, namespace);
                    internalSendMessage(subscriptionRequest);
                }
            }
        };

        /**
         * Register a handler for all messages received from a particular namespace.
         *
         *
         * The handler will be unregistered automatically when the iframe is unloaded or the
         * finish() method is called.
         *
         * This method is idempotent.
         *
         * params:
         *      namespace:  A string specifying the namespace to register a handler for
         *      handler:    A function accepting one parameter, which will be called for every
         *                  WebSocketMessage that is received from the specified namespace
         */
        this.registerNamespaceHandler = function (namespace, handler) {
            if (!localNamespaceMap.has(namespace)) {
                throw new Error('Subscribe to ' + namespace + ' before registering a handler for it');
            }
            localNamespaceMap.get(namespace).handlers.add(handler);
            globalState.namespaceMap.get(namespace).handlers.add(handler);
        }

        /**
         * Register a handler for all messages from a specific namespace with a specific type.
         *
         * The handler will be unregistered automatically when the iframe is unloaded or the
         * finish() method is called.
         *
         * This method is idempotent.
         *
         * params:
         *      namespace:  A string specifying the namespace to process messages from
         *      type:       A string specifying the message type to be handled
         *      handler:    A function accepting one parameter, which will be called for every
         *                  WebSocketMessage that matches the specified namespace and type
         */
        this.registerTypeHandler = function (namespace, type, handler) {
            if (!localNamespaceMap.has(namespace)) {
                throw new Error('Subscribe to ' + namespace + ' before registering a handler for one of its types');
            }
            var localNamespaceObject = localNamespaceMap.get(namespace);
            var globalNamespaceObject = globalState.namespaceMap.get(namespace);

            // Make sure that both the local and global `typeHandlersMap`s have an entry corresponding to our type
            if (!localNamespaceObject.typeHandlersMap.has(type)) {
                localNamespaceObject.typeHandlersMap.set(type, new Set());
                if (!globalNamespaceObject.typeHandlersMap.has(type)) {
                    globalNamespaceObject.typeHandlersMap.set(type, new Set());
                }
            }

            localNamespaceObject.typeHandlersMap.get(type).add(handler);
            globalNamespaceObject.typeHandlersMap.get(type).add(handler);
        }

        // We're done with this instance of WebSocketManager, so "subtract" the local state from the global state
        this.finish = function () {
            localNamespaceMap.forEach(function (localNamespaceObject, namespaceName, map) {
                let globalNamespaceObject = globalState.namespaceMap.get(namespaceName);

                localNamespaceObject.handlers.forEach( function (handler){
                    globalNamespaceObject.handlers.delete(handler);
                });

                localNamespaceObject.typeHandlersMap.forEach( function (handlers, type) {
                    handlers.forEach( function (handler) {
                        globalNamespaceObject.typeHandlersMap.get(type).delete(handler);
                    });
                });

                if (--globalNamespaceObject.subscriberCount < 1) {
                    internalSendMessage(new WebSocketMessage(SYSTEM_NAMESPACE, UNSUBSCRIBE_FROM_NAMESPACE_MESSAGE_TYPE, namespaceName));
                    globalState.namespaceMap.delete(namespaceName);
                }
            });

            connectionListenersToDispose.forEach( function (connectionListener) {
                globalState.connectionListeners.delete(connectionListener);
            });


            disconnectionListenersToDispose.forEach( function (disconnectionListener) {
                globalState.disconnectionListeners.delete(disconnectionListener);
            });

            localNamespaceMap = new Map();
            connectionListenersToDispose = new Set();
            disconnectionListenersToDispose = new Set();

            if (globalState.logDebug) {
                console.log("globalState:");
                console.log(globalState);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // End of WebSocketManager
    //----------------------------------------------------------------------------------------------


    function Namespace() {
        this.handlers = new Set();
        this.typeHandlersMap = new Map();
        this.subscriberCount = 1;
    }

    function isWebSocketConnected() {
        if (!globalState.webSocket) return false;
        return globalState.webSocket.readyState === globalState.webSocket.OPEN;
    }

    function enableLogging(val) {
        if (val === 'messages') globalState.logMessages = true;
        else if (val === 'debug') globalState.logDebug = true;
    }

    function disableLogging(val) {
        if (val === 'messages') globalState.logMessages = false;
        else if (val === 'debug') globalState.logDebug = false;
    }

    function onFramePingSuccess() {
        var namespaceIterator = globalState.namespaceMap.values();
        var result = namespaceIterator.next();
        while (!result.done) {
            if (result.value.subscriberCount > 0) {
                openWebSocket();
                break;
            }
            result = namespaceIterator.next();
        }
    };

    function onClose(closeEvent) {
        console.log("WebSocket has closed");
        globalState.webSocket = null;
        globalState.disconnectionListeners.forEach( function (listener) {
            listener();
        });
    }

    function onFramePingFailure() {
        // If webSocket is not null, then we haven't yet realized that the WebSocket is closed.
        if (globalState.webSocket) {
            onClose();
        }
    }

    function internalSendMessage(message) {
        if (! message instanceof WebSocketMessage) {
            throw new Error('Only instances of WebSocketMessage can be sent');
        }
        if (!isWebSocketConnected()) {
            throw new Error('Cannot send message, the WebSocket connection is not open');
        }

        if (globalState.logMessages) {
            console.log("Sending message:");
            console.log(message);
        }

        var rawMessage = {
            namespace: message.namespace,
            type: message.type,
            encodedPayload: btoa(message.payload)
        }

        globalState.webSocket.send(JSON.stringify(rawMessage));
        return true;
    }

    // Attempt to open the WebSocket. This method is idempotent.
    function openWebSocket() {
        // If globalState.webSocket exists and is either connecting or open, exit now to preserve idempotency.
        if (globalState.webSocket && (globalState.webSocket.readyState == 0 || globalState.webSocket.readyState == 1 )) return;

        globalState.webSocket = new WebSocket("ws://" + window.location.hostname + ':' + (parseInt(window.location.port) + 1));

        globalState.webSocket.onopen = function () {
            globalState.namespaceMap.forEach( function (value, key, map) {
                var subscriptionRequest = new WebSocketMessage(SYSTEM_NAMESPACE, SUBSCRIBE_TO_NAMESPACE_MESSAGE_TYPE, key);
                internalSendMessage(subscriptionRequest);
            });

            globalState.connectionListeners.forEach( function (listener) {
                listener();
            });
        };

        globalState.webSocket.onmessage = function (messageEvent) {
            var json = messageEvent.data;
            var rawMessage = JSON.parse(json);
            var payload = rawMessage.encodedPayload ? atob(rawMessage.encodedPayload) : ""
            var message = new WebSocketMessage(rawMessage.namespace, rawMessage.type, payload);

            if (globalState.logMessages) {
                console.log("Received message:");
                console.log(message);
            }

            if (globalState.namespaceMap.has(message.namespace)) {
                let namespaceObject = globalState.namespaceMap.get(message.namespace);
                namespaceObject.handlers.forEach( function (handler) {
                    handler(message);
                });

                if (namespaceObject.typeHandlersMap.has(message.type)) {
                    namespaceObject.typeHandlersMap.get(message.type).forEach( function (handler){
                        handler(message);
                    });
                }
            }
        };

        globalState.webSocket.onclose = onClose;

        globalState.webSocket.onerror = function (errorEvent) {
            console.error("Websocket error:", errorEvent);
        };
    }

    console.log("Log all incoming and outgoing WebSocket messages by running WEBSOCKET_CORE.enableLogging('messages') from the console");
    console.log("Enable WebSocket debug logging by running WEBSOCKET_CORE.enableLogging('debug') from the console");
    return {
        WebSocketMessage: WebSocketMessage,
        WebSocketManager: WebSocketManager,
        enableLogging: enableLogging,
        disableLogging: disableLogging,
        openWebSocket: openWebSocket,
        onFramePingSuccess: onFramePingSuccess,
        onFramePingFailure: onFramePingFailure
    }
}();
