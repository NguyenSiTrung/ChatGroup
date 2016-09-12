package com.example.duyti.testfpt.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.duyti.testfpt.Config.ChatApplication;
import com.example.duyti.testfpt.R;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String SOCKET_LOGIN = "login";
    private static final String EVENT_LOGIN = "login";

    public static final String KEY_USERNAME = "KEY_USERNAME";

    private Button btnLogin;
    private TextView register;
    private EditText etUserName, etPassword;
    private Socket mSocket;
    private ProgressDialog pDialog;

    private Emitter.Listener onLoginListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String result = args[0].toString();
                    if(result.equals("false")){
                        //Toast.makeText(LoginActivity.this, "Sai username hoặc password", Toast.LENGTH_SHORT).show();
                        showDialog();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideDialog();
                                Toast.makeText(LoginActivity.this, "Nhap sai password!", Toast.LENGTH_SHORT).show();
                            }
                        }, 1000);
                    }else{
                        Toast.makeText(LoginActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(KEY_USERNAME, etUserName.getText().toString());
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideDialog();

                            }
                        }, 1000);
                        //hideDialog();
                        //startActivity(intent);
                        startActivity(intent);

                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ChatApplication app = (ChatApplication) getApplication();
        mSocket = app.getSocket();

        mSocket.on(SOCKET_LOGIN, onLoginListener);
        mSocket.connect();
        initView();
    }

    private void initView() {
        btnLogin = (Button) findViewById(R.id.btn_login);
        register = (TextView) findViewById(R.id.register);
        etUserName = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);

        btnLogin.setOnClickListener(this);
        register.setOnClickListener(this);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_login:
                mSocket.emit(EVENT_LOGIN, etUserName.getText().toString(), etPassword.getText().toString());
                showDialog();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (pDialog.isShowing()) {
                            pDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Nhap sai email!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
                break;
            case R.id.register:
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private void showDialog() {
        pDialog.setMessage("Logging in ...");
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
