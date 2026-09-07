package com.example.crackcs.exception;

public class EvaluationTimeoutException extends IllegalStateException {
    public EvaluationTimeoutException() { super("evaluation timeout"); }
}
