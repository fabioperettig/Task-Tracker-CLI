package tasktracker;


import tasktracker.model.Task;
import tasktracker.model.TaskStatus;

public class Main {

    public static void main(String[] args) {

        Task task1 = new Task(1, "Study Java");

        System.out.println(task1.getId());
        System.out.println(task1.getDescription());
        System.out.println(task1.getStatus().getValue());
        System.out.println(task1.getCreatedAt());
        System.out.println(task1.getUpdatedAt());

        task1.updateDescription("Study Java Classes");
        task1.updateStatus(TaskStatus.IN_PROGRESS);

        System.out.println(task1.getStatus().getValue());
        System.out.println(task1.getDescription());
        System.out.println(task1.getUpdatedAt());

    }
}


