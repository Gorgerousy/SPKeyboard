package com.example.pgn.spkeyboard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button randomBoard;
    private Button timeCatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        randomBoard = (Button) findViewById(R.id.random_board);
        timeCatcher = (Button) findViewById(R.id.time_board);

        randomBoard.setOnClickListener(this);
        timeCatcher.setOnClickListener(this);
    }

    //点击事件响应
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.random_board:
                Intent intent_random = new Intent(MainActivity.this, RandomKeyboard.class);
                startActivity(intent_random);
                break;
            case R.id.time_board:
                Intent intent_time = new Intent(MainActivity.this, TimeCatcherKeyboard.class);
                startActivity(intent_time);
                break;
        }
    }
}
