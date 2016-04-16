package com.example.pie_asus.myapplication;
/* References:
 * http://stackoverflow.com/questions/2586301/set-inputtype-for-an-edittext
 */
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "PriceCompare";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout myLayout = (LinearLayout) findViewById(R.id.linearLayout);

        TextView tv_login = new TextView(this);
        tv_login.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv_login.setTextAppearance(this, android.R.style.TextAppearance_Large);
        tv_login.setText(getString(R.string.tv_login));
        myLayout.addView(tv_login);

        LinearLayout linearLayout1 = new LinearLayout(this);
        linearLayout1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tv_username = new TextView(this);
        tv_username.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv_username.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        tv_username.setText(getString(R.string.tv_username));
        linearLayout1.addView(tv_username);

        EditText et_username = new EditText(this);
        et_username.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        et_username.setEms(10);
        linearLayout1.addView(et_username);

        myLayout.addView(linearLayout1);

        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tv_password = new TextView(this);
        tv_password.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv_password.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        tv_password.setText(getString(R.string.tv_password));
        linearLayout2.addView(tv_password);

        EditText et_password = new EditText(this);
        et_password.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        et_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        et_username.setEms(10);
        linearLayout2.addView(et_password);

        myLayout.addView(linearLayout2);

        Button btn_submit = new Button(this);
        btn_submit.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btn_submit.setText("Submit");

        myLayout.addView(btn_submit);

        Log.v(TAG, "onCreate");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }
}
