/*
References:
http://stackoverflow.com/questions/17255383/how-do-i-programmatically-add-edittextpreferences-to-my-preferencefragment
http://stackoverflow.com/questions/12737724/how-to-use-setonpreferencechangelistener-for-quietlycoding-numberpicker
http://stackoverflow.com/questions/25544609/javascript-change-dropdown-list-value#25544845
http://stackoverflow.com/questions/8309796/want-to-load-desktop-version-in-my-webview-using-uastring#8310480
http://stackoverflow.com/questions/15874117/how-to-set-delay-in-android
Log.println(Log.ERROR,"log","hello");
http://stackoverflow.com/questions/9521232/how-to-catch-an-exception-if-the-internet-or-signal-is-down
http://stackoverflow.com/questions/26615889/how-to-change-the-launcher-logo-of-an-app-in-android-studio
 */

package com.example.pie_asus.pricecompare;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class MainActivity extends AppCompatPreferenceActivity
{

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            if (preference instanceof ListPreference)
            {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            else if (preference instanceof RingtonePreference)
            {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue))
                {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                }
                else
                {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null)
                    {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    }
                    else
                    {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }
            else
            {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    int id = 0;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || PerformSearchPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows Perform Search preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PerformSearchPreferenceFragment extends PreferenceFragment
    {
        public boolean isOnline()
        {
            ConnectivityManager cm = (ConnectivityManager) this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnectedOrConnecting())
            {
                return true;
            }
            return false;
        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_performsearch);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));

            final WebView pbtechWebview = new WebView(this.getContext());
            pbtechWebview.getSettings().setJavaScriptEnabled(true);

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(false);

            final WebView ascentWebview = new WebView(this.getContext());
            ascentWebview.getSettings().setJavaScriptEnabled(true);
            String ua = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
            ascentWebview.getSettings().setUserAgentString(ua);

            findPreference("pref_category_jb_results_key").setEnabled(false);
            findPreference("pref_category_pb_results_key").setEnabled(false);
            findPreference("pref_category_ascent_results_key").setEnabled(false);
            findPreference("pref_category_noel_results_key").setEnabled(false);
            findPreference("pref_category_hn_results_key").setEnabled(false);
            findPreference("pref_category_whs_results_key").setEnabled(false);
            findPreference("pref_category_wh_results_key").setEnabled(false);
            findPreference("pref_category_tm_results_key").setEnabled(false);

            findPreference("pref_category_jb_results_key").setSelectable(false);
            findPreference("pref_category_pb_results_key").setSelectable(false);
            findPreference("pref_category_ascent_results_key").setSelectable(false);
            findPreference("pref_category_noel_results_key").setSelectable(false);
            findPreference("pref_category_hn_results_key").setSelectable(false);
            findPreference("pref_category_whs_results_key").setSelectable(false);
            findPreference("pref_category_wh_results_key").setSelectable(false);
            findPreference("pref_category_tm_results_key").setSelectable(false);


            final Preference pref = findPreference("example_text");
            pref.setSummary("...");
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {

                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue)
                {
                    //Log.println(Log.ERROR,"log","hello");
                    preference.setSummary(newValue + "");

                    if (isOnline())
                    {

                        PreferenceManager preferenceManager = preference.getPreferenceManager();
                        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_harvey_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_jbhifi_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_noel_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_pbtech_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_ascent_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_stationery_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_tm_search_results");
                        preferenceCategory.removeAll();

                        preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_wh_search_results");
                        preferenceCategory.removeAll();

                        findPreference("pref_category_jb_results_key").setEnabled(true);
                        findPreference("pref_category_pb_results_key").setEnabled(true);
                        findPreference("pref_category_ascent_results_key").setEnabled(true);
                        findPreference("pref_category_noel_results_key").setEnabled(true);
                        findPreference("pref_category_hn_results_key").setEnabled(true);
                        findPreference("pref_category_whs_results_key").setEnabled(true);
                        findPreference("pref_category_wh_results_key").setEnabled(true);
                        findPreference("pref_category_tm_results_key").setEnabled(true);

                        findPreference("pref_category_jb_results_key").setSelectable(true);
                        findPreference("pref_category_pb_results_key").setSelectable(true);
                        findPreference("pref_category_ascent_results_key").setSelectable(true);
                        findPreference("pref_category_noel_results_key").setSelectable(true);
                        findPreference("pref_category_hn_results_key").setSelectable(true);
                        findPreference("pref_category_whs_results_key").setSelectable(true);
                        findPreference("pref_category_wh_results_key").setSelectable(true);
                        findPreference("pref_category_tm_results_key").setSelectable(true);

                        jbhifiRetrieveFeedTask jbhifiRFT = new jbhifiRetrieveFeedTask(preference, newValue + "");
                        jbhifiRFT.execute();

                        noelRetrieveFeedTask noelRFT = new noelRetrieveFeedTask(preference, newValue + "");
                        noelRFT.execute();

                        harveyRetrieveFeedTask harveyRFT = new harveyRetrieveFeedTask(preference, newValue + "");
                        harveyRFT.execute();

                        stationeryRetrieveFeedTask stationeryRFT = new stationeryRetrieveFeedTask(preference, newValue + "");
                        stationeryRFT.execute();

                        whRetrieveFeedTask whRFT = new whRetrieveFeedTask(preference, newValue + "");
                        whRFT.execute();

                        tmRetrieveFeedTask tmRFT = new tmRetrieveFeedTask(preference, newValue + "");
                        tmRFT.execute();

                        /*
                        ascentRetrieveFeedTask aRFT = new ascentRetrieveFeedTask(preference, newValue + "");
                        aRFT.execute();
                       */

                        pbtechWebview.setWebViewClient(new WebViewClient()
                        {
                            int counter = 0;

                            @Override
                            public void onPageFinished(WebView view, String url)
                            {
                                if (counter < 1)
                                {
                                    //Log.println(Log.ERROR, "log", "******" + url + "++++++++");
                                    pbtechWebview.evaluateJavascript(
                                            "var ddl1 = document.getElementById('cate_sort_by');\n" +
                                                    "var opts1 = ddl1.options.length;\n" +
                                                    "for (var i = 0; i < opts1; i++) {\n" +
                                                    "    if (ddl1.options[i].value == \"price|a\") {\n" +
                                                    "        ddl1.options[i].selected = true;\n" +
                                                    "        break;\n" +
                                                    "    }\n" +
                                                    "} \n" +
                                                    "\n" +
                                                    "var evt1 = document.createEvent(\"HTMLEvents\");\n" +
                                                    "evt1.initEvent(\"change\", false, true);\n" +
                                                    "ddl1.dispatchEvent(evt1);\n" +
                                                    "\n" +
                                                    "var ddl = document.getElementsByClassName('rec_num');\n" +
                                                    "var opts = ddl[0].options.length;\n" +
                                                    "for (var i=0; i<opts; i++){\n" +
                                                    "    ddl[0].options[i].value = 9000; \n" +
                                                    "}\n" +
                                                    "\n" +
                                                    "var evt = document.createEvent(\"HTMLEvents\");\n" +
                                                    "evt.initEvent(\"change\", false, true);\n" +
                                                    "ddl[0].dispatchEvent(evt);\n" +
                                                    "\n", new ValueCallback<String>()
                                            {
                                                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                                @Override
                                                public void onReceiveValue(String s)
                                                {
                                                }
                                            });
                                    pbtechWebview.evaluateJavascript(
                                            "'<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>';", new ValueCallback<String>()
                                            {
                                                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                                @Override
                                                public void onReceiveValue(String s)
                                                {
                                                    JsonReader reader = new JsonReader(new StringReader(s));

                                                    // Must set lenient to parse single values
                                                    reader.setLenient(true);

                                                    try
                                                    {
                                                        if (reader.peek() != JsonToken.NULL)
                                                        {
                                                            if (reader.peek() == JsonToken.STRING)
                                                            {
                                                                String msg = reader.nextString();
                                                                pbtechRetrieveFeedTask2 pbtechRFT = new pbtechRetrieveFeedTask2(pref, msg);
                                                                pbtechRFT.execute();
                                                                if (msg != null)
                                                                {
                                                                /* // for testing purposes
                                                                Toast.makeText(webview.getContext(), msg, Toast.LENGTH_LONG).show();
                                                                File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.html");
                                                                file.createNewFile();
                                                                //write the bytes in file
                                                                if (file.exists())
                                                                {
                                                                    OutputStream fo = new FileOutputStream(file);
                                                                    fo.write(msg.getBytes());
                                                                    fo.close();
                                                                }
                                                                */
                                                                }
                                                            }
                                                        }
                                                    }
                                                    catch (IOException e)
                                                    {
                                                        Log.e("TAG", "MainActivity: IOException", e);
                                                    } finally
                                                    {
                                                        try
                                                        {
                                                            reader.close();
                                                        }
                                                        catch (IOException e)
                                                        {
                                                            // NOOP
                                                        }
                                                    }
                                                }
                                            });
                                }
                                counter++;
                            }
                        });

                        String searchInput = (newValue + "").replaceAll("\\s+", "+");
                        String pbtechSearchInput = (newValue + "").replaceAll("\\s+", "+") + "+%27"; // workaround to pbtech issues with searches like: apple, iphone, macbook

                        try
                        {
                            searchInput = URLEncoder.encode((newValue + ""), "UTF-8");
                            pbtechSearchInput = searchInput + "+%27";
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            e.printStackTrace();
                        }

                        pbtechWebview.loadUrl("http://www.pbtech.co.nz/index.php?sf=" + pbtechSearchInput + "&p=search&o=price&d=a");


                        ascentWebview.setWebViewClient(new WebViewClient()
                        {
                            int counter = 0;

                            @Override
                            public void onPageFinished(WebView view, String url)
                            {
                                if (counter < 1)
                                {
                                    //Log.println(Log.ERROR, "log", "******" + url + "++++++++");
                                    ascentWebview.evaluateJavascript(
                                            "var ddl1 = document.getElementById('Content_ProductList_ctl01_ddlSort');\n" +
                                                    "var opts1 = ddl1.options.length;\n" +
                                                    "for (var i = 0; i < opts1; i++) {\n" +
                                                    "    if (ddl1.options[i].value == \"Price\") {\n" +
                                                    "        ddl1.options[i].selected = true;\n" +
                                                    "        break;\n" +
                                                    "    }\n" +
                                                    "} \n" +
                                                    "\n" +
                                                    "var evt1 = document.createEvent(\"HTMLEvents\");\n" +
                                                    "evt1.initEvent(\"change\", false, true);\n" +
                                                    "ddl1.dispatchEvent(evt1);" +
                                                    "\n", new ValueCallback<String>()
                                            {
                                                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                                @Override
                                                public void onReceiveValue(String s)
                                                {
                                                }
                                            });
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            // Do something after 5s = 5000ms
                                            ascentWebview.evaluateJavascript(
                                                    " $( 'body' ).html();", new ValueCallback<String>()
                                                    {
                                                        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                                        @Override
                                                        public void onReceiveValue(String s)
                                                        {
                                                            JsonReader reader = new JsonReader(new StringReader(s));

                                                            // Must set lenient to parse single values
                                                            reader.setLenient(true);

                                                            try
                                                            {
                                                                if (reader.peek() != JsonToken.NULL)
                                                                {
                                                                    if (reader.peek() == JsonToken.STRING)
                                                                    {
                                                                        String msg = reader.nextString();
                                                                        ascentRetrieveFeedTask2 ascentRFT = new ascentRetrieveFeedTask2(pref, msg);
                                                                        ascentRFT.execute();
                                                                        if (msg != null)
                                                                        {
                                                                            /*
                                                                            // for testing purposes
                                                                            Toast.makeText(ascentWebview.getContext(), msg, Toast.LENGTH_LONG).show();
                                                                            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.html");
                                                                            file.createNewFile();
                                                                            //write the bytes in file
                                                                            if (file.exists())
                                                                            {
                                                                                OutputStream fo = new FileOutputStream(file);
                                                                                fo.write(msg.getBytes());
                                                                                fo.close();
                                                                            }
                                                                            */
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            catch (IOException e)
                                                            {
                                                                Log.e("TAG", "MainActivity: IOException", e);
                                                            } finally
                                                            {
                                                                try
                                                                {
                                                                    reader.close();
                                                                }
                                                                catch (IOException e)
                                                                {
                                                                    // NOOP
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    }, 10000);

                                }
                                counter++;
                            }
                        });

                        ascentWebview.loadUrl("http://www.ascent.co.nz/search.aspx?T1=" + searchInput);

                    }
                    else
                    {
                        Toast.makeText(preference.getContext(), "No Internet Connection. Program will not work.", Toast.LENGTH_LONG).show();
                    }
                    return true;

                }

            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();

            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}