#!/bin/bash

function canexecute {
    EPATH=$(type -path $1)
    if [ -n "$EPATH" ] && [ -x "$EPATH" ]
    then
        return 0
    else
        return 1
    fi
}

if [[ -z $ACK_CMD ]]; then
    if canexecute ack-grep; then
        export ACK_CMD=ack-grep
    elif canexecute ack; then
        export ACK_CMD=ack
    else
        echo "Can't find ack-grep or ack command - please set ACK_CMD"
        exit 1
    fi
fi


