package com.example.pgn.spkeyboard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class RandomKeyboard extends AppCompatActivity implements View.OnTouchListener  {

    private EditText random_edit;
    private RandomKeyboardView keyBoardView;
    private LinearLayout root;
    private RelativeLayout keyboardRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_keyboard);

        random_edit = (EditText) findViewById(R.id.random_edit);

        //layout_root是文本框的一个根布局，也是父布局
        root = (LinearLayout) findViewById(R.id.layout_root);

        //这两个布局位于一个文件中，是键盘的根布局。
        keyBoardView = (RandomKeyboardView) findViewById(R.id.view_keyboard);
        keyboardRoot = (RelativeLayout) findViewById(R.id.mykeyboard_root);

        random_edit.setOnTouchListener(this);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            keyBoardView.setAttachToEditText((EditText) v, root, keyboardRoot);

            Log.d("PGN点击测试", "onTouch:点击函数触发！ ");
        }
        return true;
    }

}
