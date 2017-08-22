package com.example.babajidemustapha.lagosjavagurus;

import android.graphics.Bitmap;

/**
 * Created by Babajide Mustapha on 8/4/2017.
 */

public class User {
    String username;
    String gitUrl;
    String avi;
    String file;
    public User(String username, String gitUrl, String avi, String file){
        this.username = username;
        this.gitUrl = gitUrl;
        this.avi = avi;
        this.file = file;
    }
}
