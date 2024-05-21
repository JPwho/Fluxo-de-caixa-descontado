package com.example.avaliacaodeacoes.dto;

public class ResponseDTO<T> {
    private Integer code;
    private Status status;
    private String messagens;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessagens() {
        return messagens;
    }

    public void setMessagens(String messagens) {
        this.messagens = messagens;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public enum Status {
        SUCESSO,
        ERRO,
        ALERTA;
    }

    public enum Code {
        ERROR(1, "Erro!"),
        SUCCESS(0, "Sucesso!");

        private Integer code = 0;
        private String messageCode;

        Code(Integer code, String messageCode) {
            this.code = code;
            this.messageCode = messageCode;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessageCode() {
            return messageCode;
        }

        public void setMessageCode(String messageCode) {
            this.messageCode = messageCode;
        }
    }
}
