package com.example.pie_asus.pricecompare;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by PIE-ASUS on 24/03/2016.
 * Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
 * http://stackoverflow.com/questions/17634643/how-to-add-a-hyperlink-into-a-preference-screen-preferenceactivity
 * http://developer.android.com/reference/android/os/AsyncTask.html
 * http://stackoverflow.com/questions/1239026/how-to-create-a-file-in-android
 * http://stackoverflow.com/questions/19788294/how-does-evaluatejavascript-work
 * http://stackoverflow.com/questions/8200945/how-to-get-html-content-from-a-webview#8201246
 */

class pbtechRetrieveFeedTask extends AsyncTask<Void, Void, String>
{
    private OkHttpClient client = new OkHttpClient();
    private Exception exception;

    private Context context;

    private String searchResults = "", searchInput = "", searchResultsForParsing = "";

    private Preference preference;

    // http://stackoverflow.com/questions/10996479/how-to-update-a-textview-of-an-activity-from-another-classssss
    @SuppressLint("JavascriptInterface")
    public pbtechRetrieveFeedTask(Preference preference, String searchInput)
    {
        this.preference = preference;
        this.searchInput = searchInput.replaceAll("\\s+", "+");
    }

    public pbtechRetrieveFeedTask()
    {

    }

    String run(String url) throws IOException
    {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    protected String doInBackground(Void... params)
    {
        pbtechRetrieveFeedTask example = new pbtechRetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("http://www.pbtech.co.nz/index.php?sf=" + this.searchInput + "&p=search&o=price&d=a");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        // http://stackoverflow.com/questions/32102166/standardcharsets-utf-8-on-lower-api-lower-than-19
        InputStream stream = new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));

        try
        {
            SAXParserImpl.newInstance(null).parse(stream,
                    // http://stackoverflow.com/questions/24113529/how-to-get-elements-value-from-xml-using-sax-parser-in-startelement
                    new DefaultHandler()
                    {
                        private boolean isName = false;
                        private boolean isPrice = false;
                        private boolean isTD = false;
                        private int priceCounter = 0;
                        private String price = "";

                        public void startElement(String uri, String localName, String name, Attributes a)
                        {
                            if (name.equalsIgnoreCase("td"))
                            {
                                if ("explist_name".equals(a.getValue("class")))
                                {
                                    isTD = true;
                                }
                            }
                            else if (name.equalsIgnoreCase("a"))
                            {
                                if (isTD)
                                {
                                    isTD = false;
                                    searchResultsForParsing = searchResultsForParsing + "http://www.pbtech.co.nz/" + a.getValue("href") + "$";
                                }
                            }
                            else if (name.equalsIgnoreCase("span"))
                            {
                                if ("explist_name_link".equals(a.getValue("class")))
                                {
                                    isName = true;
                                }
                            }
                            else if (name.equalsIgnoreCase("div"))
                            {
                                if ("explist_dollars".equals(a.getValue("class")))
                                {
                                    priceCounter++;
                                    //System.out.println(priceCounter + " start");
                                    isPrice = true;
                                }
                            }
                        }

                        public void characters(char[] ch, int start, int length)
                        {
                            if (isName)
                            {
                                String productName = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                searchResultsForParsing += productName + " ";
                            }
                            else if (isPrice)
                            {
                                if (priceCounter >= 2)
                                {
                                    //System.out.println( priceCounter + " " + (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " "));
                                    price = price + (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ") + ".";
                                }
                            }
                        }

                        public void endElement(String uri, String localName, String qName)
                        {
                            if (isName)
                            {
                                isName = false;
                            }
                            else if (isPrice)
                            {
                                //System.out.println(priceCounter + " end");
                                isPrice = false;
                                if (priceCounter >= 2)
                                {
                                    price = price.substring(0, price.length() - 1);
                                    searchResultsForParsing += price + "$";
                                    priceCounter = 0;
                                    price = "";
                                }
                            }
                        }
                    });
        }
        catch (SAXException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return searchResultsForParsing;
    }

    protected void onPostExecute(String result)
    {
        //Log.println(Log.ERROR, "log", "******" + result + "++++++++");
        PreferenceManager preferenceManager = preference.getPreferenceManager();
        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_pbtech_search_results");
        preferenceCategory.removeAll();

        String[] resultsArray = result.split("\\$");

        ArrayList<String> nameResultsArray = new ArrayList<String>();
        ArrayList<String> priceResultsArray = new ArrayList<String>();
        ArrayList<String> urlResultsArray = new ArrayList<String>();

        for (int i = 0; i < resultsArray.length; i++)
        {
            if (i % 3 == 0)
            {
                urlResultsArray.add(resultsArray[i]);
            }
            if (i % 3 == 1)
            {
                nameResultsArray.add(resultsArray[i]);
            }
            else if (i % 3 == 2)
            {
                priceResultsArray.add(resultsArray[i]);
            }
        }

        for (int i = 0; i < priceResultsArray.size(); i++)
        {
            Preference resultPreference = new Preference(preference.getContext());
            resultPreference.setKey("pref_name");
            resultPreference.setTitle(nameResultsArray.get(i));
            resultPreference.setSummary(priceResultsArray.get(i));
            Intent redirect = new Intent();
            redirect.setData(Uri.parse(urlResultsArray.get(i)));
            redirect.setAction("android.intent.action.VIEW");
            resultPreference.setIntent(redirect);
            preferenceCategory.addPreference(resultPreference);
            //Log.println(Log.ERROR,"log","******"+result+"++++++++");
        }

    }




}