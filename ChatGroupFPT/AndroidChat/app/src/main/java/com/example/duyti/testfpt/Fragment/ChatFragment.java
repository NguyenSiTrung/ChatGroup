package com.example.duyti.testfpt.Fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.duyti.testfpt.Activity.LoginActivity;
import com.example.duyti.testfpt.Adapter.MessageAdapter;
import com.example.duyti.testfpt.Config.ChatApplication;
import com.example.duyti.testfpt.Model.Message;
import com.example.duyti.testfpt.R;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by duyti on 8/25/2016.
 */
public class ChatFragment extends Fragment implements View.OnClickListener, MessageAdapter.OnClickLinkListener {

    private static final String EVENT_USER_JOINED = "user joined";
    private static final String EVENT_GET_AVATAR = "get avatar";
    private static final String EVENT_GET_AVATAR_OWN = "get avatar own";
    private static final String EVENT_NEW_MESSAGE = "new message";
    private static final String EVENT_RECEIVE_AVATAR = "receiver avatar";
    private static final String EVENT_BROADCAST_AVATAR = "broadcast avatar";

    public static final String DB_NAME="chatapp";
    public static final String HOST="10.0.3.2";
    public static final int PORT = 27017;
    public static final String MONGO_COLLECTION_ADDMESSAGE= "addMessage";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MESSAGE = "data";
    private static final String KEY_TIME = "time";
    private static final int REQUEST_SEND_FILE = 106;
    private static final String PATH_EXTERNAL_STORAGE = Environment.getExternalStorageDirectory().getPath();
    private static final String EVENT_RECEIVE_FILE = "receivePhotos";
    private static final String EVENT_SEND_FILE_SUCCESS = "file sent";
    private static final String EVENT_START_RECEIVE_FILE = "receivePhoto";

    private String mUsername;

    private RecyclerView rcMessage;
    private EditText etMessage;
    private ImageView ivAddFile, ivSend;
    private Socket mSocket;

    private MessageAdapter messageAdapter;
    private List<Message> messages = new ArrayList<>();
    private Map<String, Bitmap> avatars = new HashMap<>();


    
    private Emitter.Listener onSendFileSuccess = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String fileName = "", path = "", time = "";
                    String userNameSend="";
                    try {
                        fileName = data.getString("name");
                        path = data.getString("path");
                        time = data.getString("time");
                        userNameSend = data.getString("usernameSend");
                    } catch (JSONException e) {
                        return;
                    }
                    addFile(userNameSend, fileName, path, Message.TYPE_MESSAGE_FILE, time);
                }
            });
        }
    };

    private Emitter.Listener onReceivePhoto = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Download start!", Toast.LENGTH_SHORT).show();
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String dataFile = data.getString("fileData");
                        String fileName = data.getString("name");
                        File file = new File(PATH_EXTERNAL_STORAGE + "/" + fileName);
                        Log.i("PATH FILE", PATH_EXTERNAL_STORAGE + "/" + fileName);
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        byte[] decodedString = Base64.decode(dataFile, Base64.DEFAULT);

                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(decodedString);
                        fileOutputStream.close();
                        Toast.makeText(getContext(), "Download success!", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String userName = data.getString("username");
                        Toast.makeText(getContext(), userName+" joined", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onGetAvatar = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String userName = data.getString("username");
                        String avatar = data.getString("avatar");
                        byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                        Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        avatars.put(userName, avatarBitmap);
                        Log.i("Avatar", avatars.size() + "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onGetAvatarOwn = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String userName = data.getString("username");
                        String avatar = data.getString("avatar");
                        byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                        Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        avatars.put(userName, avatarBitmap);
                        Log.i("Avatar own", userName+"");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String msg = data.getString("message");
                        String time = data.getString("timeMess");
                        String user = data.getString("username");
                        addMessage(user, msg, Message.TYPE_MESSAGE_OTHER, time);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };
    private Emitter.Listener onBroadcastAvatar = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSocket.emit(EVENT_GET_AVATAR);
                }
            });
        }
    };
    private byte[] buffOutput = null;
    private String filename = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = getActivity().getIntent().getStringExtra(LoginActivity.KEY_USERNAME);
        //init adapter
        messageAdapter = new MessageAdapter(messages, avatars);
        initSocket();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    private void initSocket() {
        ChatApplication app = (ChatApplication) getActivity().getApplication();
        mSocket = app.getSocket();

        mSocket.on(EVENT_USER_JOINED, onUserJoined);
        mSocket.on(EVENT_GET_AVATAR, onGetAvatar);
        mSocket.on(EVENT_GET_AVATAR_OWN, onGetAvatarOwn);
        mSocket.on(EVENT_NEW_MESSAGE, onNewMessage);
        mSocket.on(EVENT_BROADCAST_AVATAR, onBroadcastAvatar);
        mSocket.on(EVENT_RECEIVE_FILE, onReceivePhoto);
        mSocket.on(EVENT_SEND_FILE_SUCCESS, onSendFileSuccess);
        mSocket.connect();
        mSocket.emit(EVENT_GET_AVATAR);
        mSocket.emit(EVENT_RECEIVE_AVATAR);

        MyAsyncTask myAsyncTask = new MyAsyncTask();
        myAsyncTask.execute();

    }

    private void initView(View view) {

        rcMessage = (RecyclerView) view.findViewById(R.id.messages);
        rcMessage.setLayoutManager(new LinearLayoutManager(getActivity()));
        rcMessage.setAdapter(messageAdapter);
        etMessage = (EditText)  view.findViewById(R.id.message_input);
        ivSend = (ImageView)  view.findViewById(R.id.send_button);
        ivAddFile = (ImageView)  view.findViewById(R.id.add_file_button);


        ivAddFile.setOnClickListener(this);
        ivSend.setOnClickListener(this);
        messageAdapter.setOnClickLinkListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_file_button:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_SEND_FILE);
                break;
            case R.id.send_button:
                attemptSend();
                break;
            default:
                break;
        }
    }

    private void addFile(String username, String fileName, String path, int type, String time) {
        Message msg = new Message();
        msg.setmUsername(username);
        msg.setmTime(time);
        msg.setmPath(path);
        msg.setmMessage(fileName);
        msg.setmType(type);
        messages.add(msg);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addMessage( String user, String msg, int type, String time) {
        Message message = new Message();
        message.setmUsername(user);
        message.setmMessage(msg);
        message.setmTime(time);
        message.setmType(type);
        messages.add(message);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        rcMessage.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void attemptSend() {
        if (!mSocket.connected()) return;

        String message = etMessage.getText().toString();

        if (TextUtils.isEmpty(message)&& buffOutput == null) {
            etMessage.requestFocus();
            return;
        }
        if(buffOutput!=null){
            //addMessage(mUsername, message +"\n"+ filename, Message.TYPE_MESSAGE, getDate()[1]);
            if(message.equals("")==false) {
                addMessage(mUsername, message, Message.TYPE_MESSAGE, getDate()[1]);
                mSocket.emit(EVENT_NEW_MESSAGE, getDate()[0], message);
            }
            addMessage(mUsername, filename, Message.TYPE_MESSAGE, getDate()[1]);
            mSocket.emit("sendPhoto", buffOutput, filename, getDate()[1]);
            buffOutput = null;
        }
        else {
            addMessage(mUsername, message, Message.TYPE_MESSAGE, getDate()[1]);
            mSocket.emit(EVENT_NEW_MESSAGE, getDate()[0], message);
        }

        //addMessage(mUsername, filename, Message.TYPE_MESSAGE, getDate()[1]);
        //mSocket.emit(EVENT_NEW_MESSAGE, getDate()[0], message);
        etMessage.setText("");

    }

    private String[] getDate() {
        String time[] = new String[2];
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm-d/M/y");
        SimpleDateFormat dateFormatDisplay = new SimpleDateFormat("hh:mm");
        time[0] = dateFormat.format(new Date(System.currentTimeMillis()));
        time[1] = dateFormatDisplay.format(new Date(System.currentTimeMillis()));
        return time;
    }

    @Override
    public void onClickLink(String fileName, String path) {
        mSocket.emit(EVENT_START_RECEIVE_FILE, path, fileName);
    }

    private class MyAsyncTask extends AsyncTask<Void, Message, Boolean> {
        MongoClient mongoClient;
        List<String> dbName;
        DBCollection dbCollection;
        DBCursor cursor;
        @Override
        protected Boolean doInBackground(Void... params) {
            dbName = mongoClient.getDatabaseNames();

            DB db = mongoClient.getDB(DB_NAME);

            dbCollection = db.getCollection(MONGO_COLLECTION_ADDMESSAGE);
            cursor = dbCollection.find();
            SimpleDateFormat dateToday = new SimpleDateFormat("d/M/y");
            String date = dateToday.format(new Date(System.currentTimeMillis()));
            while (cursor.hasNext()){
                cursor.next();
                String username = cursor.curr().get(KEY_USERNAME).toString();
                if(!avatars.containsKey(username)){
                    mSocket.emit(EVENT_GET_AVATAR, username);
                }
                String message = cursor.curr().get(KEY_MESSAGE).toString();
                String time = cursor.curr().get(KEY_TIME).toString();
                if(time.substring(time.indexOf('-')+1,time.length()).equals(date)){
                    time=time.substring(0,time.indexOf('-'));
                }

                if (username.equals(mUsername)) {
                    Message message1 = new Message();
                    message1.setmUsername(username);
                    message1.setmMessage(message);
                    message1.setmTime(time);
                    message1.setmType(Message.TYPE_MESSAGE);
                    publishProgress(message1);
                } else {
                    Message message1 = new Message();
                    message1.setmUsername(username);
                    message1.setmMessage(message);
                    message1.setmTime(time);
                    message1.setmType(Message.TYPE_MESSAGE_OTHER);
                    publishProgress(message1);
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            try{
                mongoClient = new MongoClient(HOST,PORT);
            }
            catch (Exception e) {
                Log.e("Log err", e.toString());
            }
        }

        @Override
        protected void onProgressUpdate(Message... values) {
            super.onProgressUpdate(values);
            addMessage(values[0].getmUsername(),values[0].getmMessage(), values[0].getmType(),values[0].getmTime());
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean){
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_SEND_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    String fileName = getFileName(uri);
                    Toast.makeText(getContext(), fileName, Toast.LENGTH_SHORT).show();
                    try {
                        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                        byte[] buffer = new byte[1024];
                        int bytesRead = inputStream.read(buffer);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        try {
                            while (bytesRead!= -1) {
                                output.write(buffer, 0, bytesRead);
                                bytesRead = inputStream.read(buffer);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        buffOutput = output.toByteArray();
                        filename = fileName;

                        //addMessage(mUsername, filename, Message.TYPE_MESSAGE, getDate()[1]);
                        //mSocket.emit("sendPhoto", buffOutput, filename, getDate()[1]);
                        Toast.makeText(getContext(), "Choose file done!", Toast.LENGTH_SHORT).show();
                        output.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
