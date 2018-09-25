package com.example.pgn.spkeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by PGN on 2018/9/13.
 */

public class TimeCatcherKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {

    /**
     * 数字键盘
     */
    private Keyboard keyboardNumber;

    /**
     * 字母键盘
     */
    private Keyboard keyboardLetter;

    /**
     * 对应的输入框，EditText是系统内部类
     */
    private EditText editText;

    /**
     * 用户当前点击的按键
     */
    private TextView keyName;

    /**
     * 记录用户按键的飞行起点
     */
    private String preKey;

    /**
     * 按键的按压时间
     */
    private TextView pressTime;

    /**
     * 按键的飞行时间
     */
    private TextView flyTime;

    /**
     * 输入框所在的根布局
     */
    private ViewGroup root;

    /**
     * 自定义软键盘所在的根布局
     */
    private ViewGroup keyBoardRoot;

    /**
     * 为防止自定义键盘覆盖输入框，根布局向上的移动高度
     */
    private int height = 0;

    /**
     * 是否发生键盘切换
     */
    private boolean changeLetter = false;
    /**
     * 是否为大写
     */
    private boolean isCapital = false;

    /**
     * 完成按钮
     */
    private TextView complete;

    private int[] arrays = new int[]{Keyboard.KEYCODE_SHIFT, Keyboard.KEYCODE_MODE_CHANGE,
            Keyboard.KEYCODE_CANCEL, Keyboard.KEYCODE_DONE, Keyboard.KEYCODE_DELETE,
            Keyboard.KEYCODE_ALT, 32};

    //该数组存放所有的功能键
    private List<Integer> functionLists = new ArrayList<>();

    //分别建立储存按压和释放按键的数组
    private List<KeyTime> pressList = new ArrayList<>();
    private List<KeyTime> releaseList = new ArrayList<>();

    //分别建立大小写字母和数字的Key值数组
    private List<Keyboard.Key> numberKeyList;
    private List<Keyboard.Key> letterSmallKeyList;
    private List<Keyboard.Key> letterBigKeyList;

    public TimeCatcherKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEditView();
    }

    public TimeCatcherKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initEditView();
    }

    /**
     * 初始化数字和字母键盘
     */
    private void initEditView() {
        //将键盘布局与键盘对象进行绑定
        keyboardNumber = new Keyboard(getContext(), R.xml.keyboard_num);
        keyboardLetter = new Keyboard(getContext(), R.xml.keyboard_letter);

        //下面的步骤是为了在显示时不显示ASCII码，显示字符值
        //将各种键盘的Key加入到各自的数组中
        numberKeyList = keyboardNumber.getKeys();
        letterSmallKeyList = keyboardLetter.getKeys();

        //将大写键盘也加入数组中，这是为了后面记录按键时间的具体字符
        changeCapital(!isCapital);
        letterBigKeyList = keyboardLetter.getKeys();
        //将键盘再次变为小写字母
        changeCapital(!isCapital);

        //将功能键加入功能键数组中
        for (int i = 0; i < arrays.length; i++) {
            functionLists.add(arrays[i]);
        }
    }

    public void initialTextView(TextView keyName, TextView pressTime, TextView flyTime) {
        this.keyName = keyName;
        this.pressTime = pressTime;
        this.flyTime = flyTime;
        this.preKey = " ";
    }

    /**
     * 点击文本框触发该函数
     * 关联自定义键盘与输入框，以及输入框所在的根布局
     * 需要注意此方法需要在输入框的OnTouchListener中当MotionEvent为MotionEvent.ACTION_UP时调用，
     * 否则无法正确阻止系统软键盘的弹出
     *
     * @param et           当前点击的输入框
     * @param root         输入框所在的根布局
     * @param keyBoardRoot 软键盘所在的布局
     */
    public void setAttachToEditText(EditText et, ViewGroup root, ViewGroup keyBoardRoot) {

        this.editText = et;
        this.root = root;
        this.keyBoardRoot = keyBoardRoot;
        complete = keyBoardRoot.findViewById(R.id.complete);

        complete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
            }
        });
        editText.requestFocus();
        hideSystemSoftInput();   //隐藏系统键盘

        setKeyboardType(true);

        showKeyboard();
    }

    /**
     * 设置软键盘刚弹出的时候显示字母键盘还是数字键盘
     *
     * @param keyboard_num 是否显示数字键盘
     */
    public void setKeyboardType(boolean keyboard_num) {
        if (keyboard_num) {
            setKeyboard(keyboardNumber);      //设置键盘类型
            changeLetter = false;
        } else {
            setKeyboard(keyboardLetter);
            changeLetter = true;
        }

    }

    /**
     * 显示当前软键盘
     */
    private void showKeyboard() {

        //显示预览
        setPreviewEnabled(true);
        //设置键盘为可用的
        setEnabled(true);

        keyBoardRoot.setVisibility(VISIBLE);
        setVisibility(VISIBLE);

        showResize();

        //监听键盘活动
        setOnKeyboardActionListener(this);
    }

    /**
     * 判断是否需要预览Key
     *
     * @param primaryCode keyCode
     */
    private void canShowPreview(int primaryCode) {

        if (functionLists.contains(primaryCode)) {
            setPreviewEnabled(false);
        } else {
            setPreviewEnabled(true);
        }
    }

    /**
     * 新建立一个储存键盘按键及其按压时间的类
     */
    private class KeyTime {
        private int primaryCode;
        private long time;

        public KeyTime(int primaryCode, long time) {
            this.primaryCode = primaryCode;
            this.time = time;
        }

        public long getKeyTime() {
            return time;
        }

        public int getKeyValue() {
            return primaryCode;
        }
    }


    @Override
    public void onPress(int primaryCode) {

        Log.d("PGNkey", "onPress: 触发");

        //如果按压的不是功能键，将按压的时刻储存进数组
        /*if(primaryCode!=Keyboard.KEYCODE_DELETE && primaryCode!=Keyboard.KEYCODE_MODE_CHANGE
                && primaryCode != Keyboard.KEYCODE_DONE && primaryCode!= Keyboard.KEYCODE_SHIFT
                && primaryCode != 32)
          */
        if (primaryCode >= 48 && primaryCode <= 57 || primaryCode >= 65 && primaryCode <= 90
                || primaryCode >= 97 && primaryCode <= 122) {
            Log.d("PANtimetest", "onPress:按压的code值为： " + primaryCode);
            long startTime = System.nanoTime();
            KeyTime key_press_time = new KeyTime(primaryCode, startTime);
            pressList.add(key_press_time);

            String key_name = showLabel(primaryCode);

            //更新并查看按键的飞行时间
            keyName.setText("" + key_name);
            flyTime.setText("按键" + preKey + "-->" + key_name + "的飞行时间：" + getKeyFlyTime() + "ns");


            Log.d("PANtimetest", "按键" + preKey + "-->" + key_name + "的飞行时间为：" + getKeyFlyTime());

            //最后更新按键的飞行起点
            preKey = key_name;
        }
        //点击一个按键时，通过判断其是否在列表中来决定是否预览按键
        canShowPreview(primaryCode);
    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        Log.d("PGNkey", "onKey:触发 ");

        //获取文本框中已输入的字符，并且以指针方式传递，即改变内容原内容也会改变
        Editable editable = editText.getText();

        //获取焦点光标的所在位置
        int start = editText.getSelectionStart();

        switch (primaryCode) {

            case Keyboard.KEYCODE_DELETE://删除
                if (editable != null && editable.length() > 0 && start > 0) {
                    editable.delete(start - 1, start);
                }
                break;
            case Keyboard.KEYCODE_MODE_CHANGE://字母键盘与数字键盘切换
                changeKeyBoard(!changeLetter);
                Log.d("PGN键盘切换", "键盘切换: " + changeLetter);
                break;
            case Keyboard.KEYCODE_DONE://完成
                //setVisibility(GONE);
                setVisibility(View.GONE);
                keyBoardRoot.setVisibility(GONE);
                break;
            case Keyboard.KEYCODE_SHIFT://大小写切换
                changeCapital(!isCapital);
                setKeyboard(keyboardLetter);
                break;
            default:
                editable.insert(start, Character.toString((char) primaryCode));


                //当按键为数字或者字母时，将按键释放时刻进行储存
                if (primaryCode != 32) {
                    long endTime = System.nanoTime();
                    KeyTime key_release_time = new KeyTime(primaryCode, endTime);
                    releaseList.add(key_release_time);

                    //获取并保存按压时间
                    long press_time = getKeyPressTime();

                    pressTime.setText("" + press_time + "ns");

                    String key_name = showLabel(primaryCode);


                    Log.d("PANtimetest", "onKey: 显示按压时间成功！");

                    Log.d("PANtimetest", "按键--" + showLabel(primaryCode) + "--的按压时间为：" + press_time);


                }

                break;
        }

    }


    /**
     * 得到最后一次按键的按压时间
     */
    private long getKeyPressTime() {
        int pressListLength = pressList.size();
        int releaseListLength = releaseList.size();

        Log.d("PANtimetest", "getKeyPressTime: 第二行成功");

        if (pressListLength != releaseListLength) {
            Log.d("PANtimetest", "getKeyPressTime: 进入判断成功");

            Log.e("PANtimetest", "getKeyPressTime: 前后时间储存个数不一致，失败！");
            pressList.clear();
            Log.d("PANtimetest", "getKeyPressTime: 判断第二条成功");
            releaseList.clear();
            Log.d("PANtimetest", "getKeyPressTime: 判断结束成功");
            return 100;
        }
        long release_time = releaseList.get(pressListLength - 1).getKeyTime();
        long press_time = pressList.get(releaseListLength - 1).getKeyTime();

        return release_time - press_time;
    }

    /**
     * 得到每一次按键中间的飞行时间
     */
    private long getKeyFlyTime() {
        int pressListLength = pressList.size();
        int releaseListLength = releaseList.size();

        if (releaseListLength <= 0) {
            Log.d("PANtimetest", "getKeyFlyTime: 时间储存列表为空,无飞行时间记录。");
            return 0;
        } else if ((pressListLength - 1) != releaseListLength) {
            Log.e("PANtimetest", "getKeyFlyTime: 时间储存列表个数匹配失败，无法计算飞行时间！");
            pressList.clear();
            releaseList.clear();
            return 0;
        }

        //飞行时间用本次的按压时刻减去前一次的释放时刻
        long press_time = pressList.get(pressListLength - 1).getKeyTime();
        long release_time = releaseList.get(releaseListLength - 1).getKeyTime();

        return press_time - release_time;
    }

    /**
     * 显示键盘按键的Code显示按键的Label
     */
    @NonNull
    private String showLabel(int code) {
        Log.d("PANtimetest", "showLabel: 此时的code值为：" + code);

        //判断键盘类型，false表示数字键盘，true表示字母键盘
        if (false == changeLetter) {
            for (int i = 0; i < numberKeyList.size(); i++) {
                if (numberKeyList.get(i).codes[0] == code)
                    return numberKeyList.get(i).label.toString();
            }
        }
        //确定为字母键盘后，判断字母大小写，false表示小写字母，true表示大写字母
        else if (true == changeLetter && false == isCapital) {
            for (int i = 0; i < letterSmallKeyList.size(); i++) {
                if (letterSmallKeyList.get(i).codes[0] == code)
                    return letterSmallKeyList.get(i).label.toString();
            }
        } else if (true == changeLetter && true == isCapital) {
            for (int i = 0; i < letterBigKeyList.size(); i++) {
                if (letterBigKeyList.get(i).codes[0] == code)
                    return letterBigKeyList.get(i).label.toString();
            }
        }
        Log.e("PANtimetest", "showLabel: Code匹配出错!");
        return "Code匹配出错!";
    }


    /**
     * 讲按键时间储存到文件中去
     *
     * @param file 文件名
     * @param text 需要存储的内容
     */
    /*
    public void saveTime(String file, String text) {
        Context context;
        FileOutputStream out = null;
        BufferedWriter writer = null;
        context = getContext();

        Log.d("PGNsave", "saveTime: 开始储存了~~");

        try{
            out = openFileOutput("pgn", Context.MODE_APPEND);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(text);
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (writer != null)
                {
                    writer.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
*/

    /**切换键盘大小写*/
    private void changeCapital(boolean b) {

        isCapital = b;
        List<Keyboard.Key> lists = keyboardLetter.getKeys();

        for (Keyboard.Key key: lists) {
            if (key.label != null && isKey(key.label.toString()))
            {
                if (isCapital)
                {
                    key.label = key.label.toString().toUpperCase();
                    key.codes[0] = key.codes[0] - 32;
                }
                else
                {
                    key.label = key.label.toString().toLowerCase();
                    key.codes[0] = key.codes[0] + 32;
                }
            }
            else if (key.label != null && key.label.toString().equals("小写"))
            {
                key.label = "大写";
            }
            else if (key.label != null && key.label.toString().equals("大写"))
            {
                key.label = "小写";
            }
        }
    }

    /** * 判断此key是否正确，且存在
     *  @param key
     * @return */
    private boolean isKey(String key) {
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        if (lowercase.indexOf(key.toLowerCase()) > -1) {
            return true;
        }
        return false;
    }
    /**判断key是否为字母键*/
    private boolean isLetter(Keyboard.Key key) {
        if(key.codes[0]<0)
            return false;
        else if(key.codes[0] >= 65 && key.codes[0]<= 90)
        {
            return true;
        }
        else if(key.codes[0]>=97 && key.codes[0]<=122)
            return true;
        return false;
    }

    /**判断key是否为数字键*/
    private boolean isNumber(Keyboard.Key key) {
        if(key.codes[0]<0)
            return false;
        else if(key.codes[0] >= 48 && key.codes[0]<= 57)
        {
            return true;
        }
        return false;
    }

    /**切换键盘类型
     * @param b true表示显示字母键盘
     *          false表示显示数字键盘*/
    private void changeKeyBoard(boolean b) {
        //此处将键盘的状态进行翻转改变
        changeLetter = b;
        if (changeLetter) {
            setKeyboard(keyboardLetter);
        } else {
            setKeyboard(keyboardNumber);
        }
    }


    /**根据输入框的底部坐标与自定义键盘的顶部坐标之间的差值height，
     * 判断自定义键盘是否覆盖住了输入框，如果覆盖则使输入框所在的根布局移动height*/
    private void showResize() {

        root.post(new Runnable() {
            @Override
            public void run() {

                int[] pos = new int[2];
                //获取编辑框在整个屏幕中的坐标
                editText.getLocationOnScreen(pos);
                //编辑框的Bottom坐标和键盘Top坐标的差
                height = (pos[1] + editText.getHeight()) -
                        (getScreenHeight(getContext()) - keyBoardRoot.getHeight());
                if (height > 0) {
                    root.scrollBy(0, height + dp2px(getContext(), 16));
                }
            }
        });
    }


    /**隐藏系统键盘*/
    private void hideSystemSoftInput() {

        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**隐藏键盘*/
    private void hideKeyBoard() {

        if (getVisibility() == VISIBLE) {
            keyBoardRoot.setVisibility(GONE);
            setVisibility(GONE);
            hideResize();
        }
    }

    /**自定义键盘隐藏时，判断输入框所在的根布局是否向上移动了height，如果移动了则需再移回来
     * 该函数在触发隐藏键盘时，才会被调用。*/
    private void hideResize() {
        if (height > 0) {
            root.scrollBy(0, -(height + dp2px(getContext(), 16)));
        }
    }

    /**获取手机屏幕高度*/
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    /**将px转换成dp*/
    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}

