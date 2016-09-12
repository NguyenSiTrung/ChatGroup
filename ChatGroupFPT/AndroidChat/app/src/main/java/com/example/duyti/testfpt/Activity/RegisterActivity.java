package com.example.duyti.testfpt.Activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.duyti.testfpt.Config.ChatApplication;
import com.example.duyti.testfpt.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RegisterActivity extends Activity implements View.OnClickListener {
    private static final String SOCKET_REGISTER = "register";
    private static final String EVENT_REGISTER = "register";
    private static final int REQUEST_CHOSE_AVATAR = 101;
    private static final String EVENT_SAVE_AVATAR = "save avatar";

    private Button btnRegister;
    private TextView back;
    private EditText etUserName, etPassword;
    private ImageView ivAvatar;

    private Socket mSocket;
    private String avatarBase64;


    private Emitter.Listener onRegisterListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = args[0].toString();
                    if (result.equals("false")) {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ChatApplication app = (ChatApplication) getApplication();
        mSocket = app.getSocket();

        mSocket.on(SOCKET_REGISTER, onRegisterListener);
        mSocket.connect();
        initView();
    }

    private void initView() {
        btnRegister = (Button) findViewById(R.id.btn_register);
        back = (TextView) findViewById(R.id.back);
        etUserName = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);
        ivAvatar = (ImageView) findViewById(R.id.avatar);

        btnRegister.setOnClickListener(this);
        back.setOnClickListener(this);
        ivAvatar.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_register:
                String username = etUserName.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (!username.isEmpty() && !password.isEmpty()) {
                    registerUser(username, password);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please enter your details!", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.back:
                finish();
                break;
            case R.id.avatar:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CHOSE_AVATAR);
                break;
            default:
                break;
        }
    }

    private void registerUser(String username, String password) {
        mSocket.emit(EVENT_REGISTER, username, password);
        String avatarName = username+".bin";
        mSocket.emit(EVENT_SAVE_AVATAR, avatarBase64, avatarName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHOSE_AVATAR:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
                        try {
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                output64.write(buffer, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        output64.close();

                        avatarBase64 = output.toString();
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
}
