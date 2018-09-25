package com.example.pgn.spkeyboard;

import android.os.TestLooperManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TimeCatcherKeyboard extends AppCompatActivity implements View.OnTouchListener {

    public TextView keyName;
    public TextView pressTime;
    public TextView flyTime;

    private EditText timeEdit;

    private TimeCatcherKeyboardView keyBoardView;
    private LinearLayout root;
    private RelativeLayout keyboardRoot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_catcher_keyboard);

        keyName = (TextView) findViewById(R.id.key_name);
        pressTime = (TextView) findViewById(R.id.press_time);
        flyTime = (TextView) findViewById(R.id.fly_time);


        timeEdit = (EditText) findViewById(R.id.time_edit);


        root = (LinearLayout) findViewById(R.id.time_board_root);

        keyBoardView = (TimeCatcherKeyboardView) findViewById(R.id.view_keyboard);
        keyboardRoot = (RelativeLayout) findViewById(R.id.mykeyboard_root);

        timeEdit.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            keyBoardView.setAttachToEditText((EditText) v, root, keyboardRoot);
            keyBoardView.initialTextView(keyName,pressTime,flyTime);
        }
        return true;
    }
}
