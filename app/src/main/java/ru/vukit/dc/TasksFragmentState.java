package ru.vukit.dc;

import androidx.annotation.Keep;

import java.util.HashMap;

@Keep
class TasksFragmentState {

    final HashMap<String, String> tasksStatus = new HashMap<>();

    private static TasksFragmentState INSTANCE = null;

    private TasksFragmentState() {
    }

    public static synchronized TasksFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TasksFragmentState();
        }
        return (INSTANCE);
    }

    void setTaskStatus(String taskId, String status) {
        tasksStatus.put(taskId, status);
    }

}
