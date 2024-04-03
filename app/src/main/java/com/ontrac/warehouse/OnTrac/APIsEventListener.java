package com.ontrac.warehouse.OnTrac;

import java.util.List;

public interface APIsEventListener {
    void RequestCompleted(int type, int responseCode, String responseBody, Object data) throws Exception;
    //void CallCompleted(int type, Object data, ClientConnectionError error);
    void CallCompleted(int type, Object data, List<ClientConnectionError> errors);
    //void Error(ClientConnectionError type, String message, StackTraceElement[] stackTraceElements);

    class ClientConnectionError {
        public ErrorType Type;
        public String Message;
        public StackTraceElement[] StackTraceElements;

        public ClientConnectionError(ErrorType errorType, String message, StackTraceElement[] stackTraceElements){
            this.Type = errorType;
            this.Message = message;
            this.StackTraceElements = stackTraceElements;
        }

        public enum ErrorType {
            Unspecified,
            UnknownHost,
            SocketTimeout
        }
    }
}


