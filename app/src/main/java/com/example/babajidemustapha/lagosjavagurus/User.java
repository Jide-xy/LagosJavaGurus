package com.example.babajidemustapha.lagosjavagurus;

/**
 * Created by Babajide Mustapha on 8/4/2017.
 */

public class User {
    private String username;
    private String gitUrl;
    private String avi;
    private String file;
    public User(String username, String gitUrl, String avi, String file){
        this.username = username;
        this.gitUrl = gitUrl;
        this.avi = avi;
        this.file = file;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getAvi() {
        return avi;
    }

    public String getFile() {
        return file;
    }

    public String getUsername() {
        return username;
    }
}
