package com.example.socialnetworkapp;

import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.Random;

public class AdapterRandomUsers {

    Context context;
    List<ModelUser> userList;

    //constructor
    public AdapterRandomUsers(Context context, List<ModelUser> userList) {
        Random rand = new Random();
        String ok="ok";
        this.context = context;
        this.userList = userList;
        int iValue = rand.nextInt(userList.size());
        final String hisUID = userList.get(iValue).getUid();
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("hisUid", hisUID);
        intent.putExtra("sw",ok);
        context.startActivity(intent);
    }
}
