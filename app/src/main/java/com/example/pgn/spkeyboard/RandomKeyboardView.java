package com.example.pgn.spkeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by PGN on 2018/9/10.
 * 自定义键盘在布局文件中须确保在整体布局的底部，不要和输入框在相同的根布局内
 */

public class RandomKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {

    /**数字键盘*/
    private Keyboard keyboardNumber;

    /**字母键盘*/
    private Keyboard keyboardLetter;

    /**对应的输入框，EditText是系统内部类*/
    private EditText editText;

    /**输入框所在的根布局*/
    private ViewGroup root;

    /**自定义软键盘所在的根布局*/
    private ViewGroup keyBoardRoot;

    /**为防止自定义键盘覆盖输入框，根布局向上的移动高度*/
    private int height = 0;

    /**是否发生键盘切换*/
    private boolean changeLetter = false;
    /**是否为大写*/
    private boolean isCapital = false;

    /**完成按钮*/
    private TextView complete;

    private int[] arrays = new int[]{Keyboard.KEYCODE_SHIFT, Keyboard.KEYCODE_MODE_CHANGE,
            Keyboard.KEYCODE_CANCEL, Keyboard.KEYCODE_DONE, Keyboard.KEYCODE_DELETE,
            Keyboard.KEYCODE_ALT, 32};

    //该数组存放所有的功能键
    private List<Integer> functionLists = new ArrayList<>();

    public RandomKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEditView();
    }

    public RandomKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initEditView();
    }

    /**初始化数字和字母键盘*/
    private void initEditView() {
        //将键盘布局与键盘对象进行绑定
        keyboardNumber = new Keyboard(getContext(), R.xml.keyboard_num);
        keyboardLetter = new Keyboard(getContext(), R.xml.keyboard_letter);

        for (int i=0; i<arrays.length; i++) {
            functionLists.add(arrays[i]);
        }
    }

    /**
     * 点击文本框触发该函数
     * 关联自定义键盘与输入框，以及输入框所在的根布局
     * 需要注意此方法需要在输入框的OnTouchListener中当MotionEvent为MotionEvent.ACTION_UP时调用，
     * 否则无法正确阻止系统软键盘的弹出
     * @param et 当前点击的输入框
     * @param root 输入框所在的根布局
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
     * @param keyboard_num 是否显示数字键盘
     */
    public void setKeyboardType (boolean keyboard_num) {
        if (keyboard_num)
        {
            setKeyboard(keyboardNumber);      //设置键盘类型
            changeLetter = false;
        } else
        {
            setKeyboard(keyboardLetter);
            changeLetter = true;
        }

    }

    /**
     * 显示当前软键盘
     */
    private void showKeyboard()
    {

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
     * @param primaryCode keyCode
     */
    private void canShowPreview(int primaryCode) {

        if (functionLists.contains(primaryCode)) {
            setPreviewEnabled(false);
        } else {
            setPreviewEnabled(true);
        }
    }

    @Override
    public void onPress(int primaryCode) {
        Log.d("PGNkey", "onPress: 触发");

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
                    editable.delete(start-1, start);
                }
                break;
            case Keyboard.KEYCODE_MODE_CHANGE://字母键盘与数字键盘切换
                changeKeyBoard(!changeLetter);
                Log.d("PGN键盘切换", "键盘切换: "+changeLetter);
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
                editable.insert(start, Character.toString((char)primaryCode));
                randomKeyboardNumber();
                break;
        }

    }

    /**打乱键盘顺序*/
    private void randomKeyboardNumber()
    {

        Keyboard keyboard;
        keyboard=this.getKeyboard();


        List<Keyboard.Key> keyList = keyboard.getKeys();

        List<Keyboard.Key> newkeyList = new ArrayList<Keyboard.Key>();


        //changeLetter为true表示此时为字母键盘，false表示为数字键盘

        if(false==changeLetter)
        {
            for (int i = 0; i < keyList.size(); i++)
            {
                Log.d("潘冠男", "按键的CODE值: "+keyList.get(i).codes[0]);
                // Log.d("潘冠男", "按键的显示值: "+keyList.get(i).label.toString());

                //根据label属性选择Key
                //if ((keyList.get(i).label != null && keyList.get(i).codes[0] >= 48 && keyList.get(i).codes[0] <= 57 ))
                if ((keyList.get(i).label != null && isNumber(keyList.get(i)) ))
                {
                    newkeyList.add(keyList.get(i));   //将按键储存到新的List中
                    Log.d("潘冠男", "code值："+keyList.get(i).codes[0]+"已经成功计入");
                }
            }
        }
        else
        //遍历所有布局文件中的Key属性
        {
            for (int i = 0; i < keyList.size(); i++)
            {
                //根据label属性选择Key
                //if ((keyList.get(i).label != null && isKey(keyList.get(i).label.toString())))
                /**
                 * 经过潘冠男精密的测试，源代码中的isKey函数在使用时存在bug,在判断字母时不能准确的的识别字母
                 * 并且会将非字母codes也判断为字母，导致之前的诸多测试均出现问题
                 * 现改用直接判断按键的codes值，目前可以正确运行
                 */
                if ((keyList.get(i).label != null && isLetter(keyList.get(i))))
                {
                    newkeyList.add(keyList.get(i));   //将按键储存到新的List中
                    Log.d("潘冠男", "字母键盘code值："+keyList.get(i).codes[0]+"已经成功计入");
                }
            }
        }

        for(Keyboard.Key key : newkeyList)
            Log.d("潘冠男", "字母键盘扫描后："+key.codes[0]+"");


        // 数组长度,记录键盘按键的总个数
        int count = newkeyList.size();

        for(Keyboard.Key key : newkeyList)
            Log.d("潘冠男", key.codes[0]+"");

        // 结果集
        List<KeyModel> resultList = new ArrayList<KeyModel>();

        // 用一个LinkedList作为中介
        LinkedList<KeyModel> temp = new LinkedList<KeyModel>();

        Log.d("潘冠男", "randomKeyboardNumber: "+count);

        // 初始化temp
        //将0~9存入其中
        if(keyboard==keyboardNumber){
            for (int i = 0; i < count; i++) {
                temp.add(new KeyModel(48 + i, i + ""));
            }
        }

        if(keyboard==keyboardLetter)
        {
            if(true==isCapital)
            {
                for (int i = 0; i < count; i++)
                    temp.add(new KeyModel(65 + i, (char)(65+i)+""));
            }
            else
                for (int i = 0; i < count; i++)
                    temp.add(new KeyModel(97 + i, (char)(97+i)+""));
        }

        // 取数
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            //取0<=rand.nextInt(n)<n的随机数
            int num = rand.nextInt(count - i);
            resultList.add(new KeyModel(temp.get(num).getCode(),
                    temp.get(num).getLable()));
            temp.remove(num);
        }
        for (int i = 0; i < newkeyList.size(); i++) {
            newkeyList.get(i).label = resultList.get(i).getLable();
            newkeyList.get(i).codes[0] = resultList.get(i)
                    .getCode();
        }

        //changeLetter为true表示此时为字母键盘，false表示为数字键盘
        if(changeLetter)
            setKeyboard(keyboardLetter);
        else
            setKeyboard(keyboardNumber);
    }

    private class KeyModel {

        private int code;   //code是布局文件中每个字符的ASCII码
        private String lable;    //布局文件中每个按键所代表的字符值

        public KeyModel(int code, String lable) {
            this.code = code;
            this.lable = lable;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getLable() {
            return lable;
        }

        public void setLable(String lable) {
            this.lable = lable;
        }
    }


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
