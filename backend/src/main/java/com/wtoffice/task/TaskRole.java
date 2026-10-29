package com.wtoffice.task;

public enum TaskRole {
    PLANNING("기획"),
    DEVELOPMENT("개발"),
    REVIEW("검토");

    private final String label;

    TaskRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TaskRole fromLabel(String label) {
        for (TaskRole role : values()) {
            if (role.label.equals(label)) {
                return role;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 담당 역할입니다.");
    }
}
