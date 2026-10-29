package com.wtoffice.task;

public record UpdateTaskStatusRequest(String status, String resultSummary, String errorSummary) {
}
