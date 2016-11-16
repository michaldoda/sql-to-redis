package sql2redis.models;


public class ImportTaskModel {

    private String id;

    private String name;

    private String description;

    private String status;

    public ImportTaskModel(String name, String description, String status) {
        this.name = name;
        this.description = description;
        this.status = translateState(status);
    }
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = translateState(status);
    }

    public static String translateState(String status) {
        if (status.equals("TERMINATED")) {
            return "Done";
        } else if(status.equals("RUNNABLE")) {
            return "Running";
        } else if(status.equals("NEW")) {
            return "Waiting";
        } else if(status.equals("BLOCKED")) {
            return "Waiting";
        } else if(status.equals("WAITING")) {
            return "Waiting";
        } else if(status.equals("TIMED_WAITING")) {
            return "Waiting";
        } else if (status.equals("user_stop_action")) {
            return "Canceled";
        } else {
            return "";
        }
    }
}
