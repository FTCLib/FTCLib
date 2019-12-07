/*
 * Copyright (c) 2018 Craig MacFarlane
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Craig MacFarlane nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.robotcore.external;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.WifiMuteEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Infrastructure for a very simple generic state machine.  Part of a collection
 * of classes including State, Event, and StateTransition.  All this class does
 * is manage a directed graph and execute transitions on events.
 *
 * Create instances of a state machine by deriving from this class and defining
 * states, events, and transitions.  See usages of StateMachine for reference
 * implementations.
 */
public class StateMachine {

    private final static String TAG = "StateMachine";

    protected State currentState;
    protected HashMap<State, ArrayList<StateTransition>> stateGraph;
    protected ArrayList<Event> maskList;

    public StateMachine()
    {
        stateGraph = new HashMap<>();
        maskList = new ArrayList<>();
    }

    /**
     * start
     *
     * Define the start state of the state machine.  Should be called
     * from the start method of the feature's state machine.
     *
     * @param state The initial state.
     */
    protected void start(State state)
    {
        currentState = state;
    }

    /**
     * addTransition
     *
     * Adds a transition to the state machine.
     *
     * @param transition the transition to add.
     */
    public void addTransition(StateTransition transition)
    {
        ArrayList<StateTransition> edges = stateGraph.get(transition.from);
        if (edges == null) {
            edges = new ArrayList<>();
            edges.add(transition);
            stateGraph.put(transition.from, edges);
        } else {
            edges.add(transition);
        }
    }

    /**
     * consumeEvent
     *
     * Executes a state transition and returns the new state.
     * *
     * @param event The event to consume
     * @return The new state, or the current state if the current state does not have a
     *         matching event edge.
     */
    public State consumeEvent(Event event)
    {
        if (maskList.contains(event)) {
            RobotLog.ii(TAG, "Ignoring " + event.getName() + " masked");
            return currentState;
        }

        State newState = transition(event);
        if (newState != null) {
            RobotLog.ii(TAG, "Old State: " + currentState.toString() + ", Event: " + event.getName() + ", New State: " + newState.toString());
            currentState.onExit(event);
            currentState = newState;
            currentState.onEnter(event);
        }

        return currentState;
    }

    public void maskEvent(Event event)
    {
        if (!maskList.contains(event)) {
            RobotLog.ii(TAG, "Masking " + event.getName());
            maskList.add(event);
        }
    }

    public void unMaskEvent(Event event)
    {
        if (maskList.contains(event)) {
            RobotLog.ii(TAG, "Unmasking " + event.getName());
            maskList.remove(event);
        }
    }

    protected State transition(Event event)
    {
        ArrayList<StateTransition> edges = stateGraph.get(currentState);

        if (edges == null) {
            RobotLog.vv(TAG, "State with no transitions: " + currentState.toString());
            return null;
        }

        for (StateTransition edge: edges) {
            if (edge.event == event) {
                return edge.to;
            }
        }
        return null;
    }

    public String toString()
    {
        String str = "\n";

        for (HashMap.Entry<State, ArrayList<StateTransition>> entry : stateGraph.entrySet()) {
            State state = entry.getKey();
            ArrayList<StateTransition> edges = entry.getValue();
            str += state.toString() + "\n";
            for (StateTransition edge : edges) {
                str += "\t" + edge.toString() + "\n";
            }
        }

        return str;
    }

}
