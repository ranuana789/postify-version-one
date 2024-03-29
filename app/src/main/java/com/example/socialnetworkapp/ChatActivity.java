package com.example.socialnetworkapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialnetworkapp.AdapterChat;
import com.example.socialnetworkapp.ModelChat;
import com.example.socialnetworkapp.ModelUser;
//import com.blogspot.atifsoftwares.firebaseapp.notifications.Data;
//import com.blogspot.atifsoftwares.firebaseapp.notifications.Sender;
//import com.blogspot.atifsoftwares.firebaseapp.notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    //views from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn, attachBtn;

    //firebase auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;

    //for checking if use has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.proifleIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);
        attachBtn = findViewById(R.id.attachBtn);


        //Layout (LinearLayout) for RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        /*On clicking user from users list we have passed that user's UID using intent
         * So get that uid here to get the profile picture, name and start chat with that
         * user*/
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");


        //firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");


        sendBtn.setOnClickListener( new View.OnClickListener(){
            public void onClick (View v){
                next_page(v);
            }
        });
    }

    private void next_page(View v) {

//            notify = true;
            //get text from edit text
            String message = messageEt.getText().toString().trim();
            //check if text is empty or not
            if (TextUtils.isEmpty(message)) {
                //text empty
                Toast.makeText(ChatActivity.this, "Cannot send the empty message...", Toast.LENGTH_SHORT).show();
            } else {
                //text not empty
                sendMessage(message);
            }
            //reset edittext after sending message
            messageEt.setText("");

            readMessages();
            seenMessage();
        
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                        chatList.add(chat);
                    }

                    //adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message) {
        /*"Chats" node will be created that will contain all chats
         * Whenever user sends message it will create new child in "Chats" node and that child will contain
         * the following key values
         * sender: UID of sender
         * receiver: UID if receiver
         * message: the actual message*/


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type", "text");
        databaseReference.child("Chats").push().setValue(hashMap);

        messageEt.setText("");

    }

    private void checkUserStatus() {
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail());
            //currently signed in user's
            myUid = user.getUid();
        } else {
            //user not signed in, go to main acitivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
//        checkOnlineStatus("online");
        super.onStart();
    }
    @Override
    protected void onPause()    {
        super.onPause();
        //get timestamp
//        String timestamp = String.valueOf(System.currentTimeMillis());
//        //set ofline with last seen time stamp
//        checkOnlineStatus(timestamp);
//        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }



    //search user to get that user's info
//    Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
//    //get user picture and name
//        userQuery.addValueEventListener(new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//            //check until required ifo is received
//            for (DataSnapshot ds : dataSnapshot.getChildren()) {
//                //get data
//                String name = "" + ds.child("name").getValue();
//                hisImage = "" + ds.child("image").getValue();
//                String typingStatus = "" + ds.child("typingTo").getValue();
//
//                //check typing status
//                if (typingStatus.equals(myUid)) {
//                    userStatusTv.setText("typing...");
//                } else {
//                    //get value of onlinestatus
//                    String onlineStatus = "" + ds.child("onlineStatus").getValue();
//                    if (onlineStatus.equals("online")) {
//                        userStatusTv.setText(onlineStatus);
//                    } else {
//                        //convert timestamp to proper time date
//                        //convert time stamp to dd/mm/yyyy hh:mm am/pm
//                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
//                        cal.setTimeInMillis(Long.parseLong(onlineStatus));
//                        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString();
//                        userStatusTv.setText("Last seen at: " + dateTime);
//
//                    }
//                }
//
//                //set data
//                if(sw.equals("ok"))
//                    nameTv.setText("익명");
//                else {
//                    nameTv.setText(name);
//                    try {
//                        //image received, set it to imageview in toolbar
//                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);
//                    } catch (Exception e) {
//                        //there is exception getting picture, set default picture
//                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError databaseError) {
//
//        }
//    });

    //click button to send message



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
//        //hide searchview, add post, as we dont need it here
//        menu.findItem(R.id.action_search).setVisible(false);
//        menu.findItem(R.id.action_add_post).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }



}