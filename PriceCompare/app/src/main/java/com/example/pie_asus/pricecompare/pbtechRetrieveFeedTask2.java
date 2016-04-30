package com.example.pie_asus.pricecompare;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.OkHttpClient;

/**
 * Created by PIE-ASUS on 24/03/2016.
 * Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
 * http://stackoverflow.com/questions/17634643/how-to-add-a-hyperlink-into-a-preference-screen-preferenceactivity
 * http://developer.android.com/reference/android/os/AsyncTask.html
 * http://stackoverflow.com/questions/1239026/how-to-create-a-file-in-android
 * http://stackoverflow.com/questions/19788294/how-does-evaluatejavascript-work
 * http://stackoverflow.com/questions/8200945/how-to-get-html-content-from-a-webview#8201246
 */

class pbtechRetrieveFeedTask2 extends AsyncTask<Void, Void, String>
{
    private OkHttpClient client = new OkHttpClient();
    private Exception exception;

    private Context context;

    private String searchResults = "", searchInput = "", searchResultsForParsing = "", htmlCode="";

    private Preference preference;


    // http://stackoverflow.com/questions/10996479/how-to-update-a-textview-of-an-activity-from-another-classssss
    @SuppressLint("JavascriptInterface")
    public pbtechRetrieveFeedTask2(Preference preference, String htmlCode)
    {
        this.preference = preference;
        this.htmlCode = htmlCode;
    }

    @Override
    protected String doInBackground(Void... params)
    {
        InputStream stream = new ByteArrayInputStream(htmlCode.getBytes(Charset.forName("UTF-8")));

        try
        {
            SAXParserImpl.newInstance(null).parse(stream,
                    // http://stackoverflow.com/questions/24113529/how-to-get-elements-value-from-xml-using-sax-parser-in-startelement
                    new DefaultHandler()
                    {
                        private boolean isName = false;
                        private boolean isPrice = false;
                        private boolean isPriceU = false;
                        private boolean isTD = false;
                        private int priceCounter = 0;
                        private String price = "";

                        public void startElement(String uri, String localName, String name, Attributes a)
                        {
                            if (name.equalsIgnoreCase("div"))
                            {
                                if ("explistm_pname".equals(a.getValue("class")))
                                {
                                    isName = true;
                                    isTD = true;
                                }
                            }
                            else if (name.equalsIgnoreCase("a"))
                            {
                                if (isTD)
                                {
                                    isTD = false;
                                    searchResultsForParsing = searchResultsForParsing + a.getValue("href") + "$";
                                    //System.out.println(a.getValue("href"));
                                }
                            }
                            else if (name.equalsIgnoreCase("span"))
                            {
                                if ("explistm_dollars".equals(a.getValue("class")))
                                {
                                    priceCounter++;
                                    //System.out.println(priceCounter + " start");
                                    isPrice = true;
                                }
                            }
                            else if (name.equalsIgnoreCase("td"))
                            {
                                if ("explistm_price".equals(a.getValue("class")))
                                {
                                    isPriceU = true;
                                }
                            }
                        }

                        public void characters(char[] ch, int start, int length)
                        {
                            if (isName)
                            {
                                String productName = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                System.out.println(productName);
                                searchResultsForParsing += productName ;
                            }
                            else if (isPrice)
                            {
                                if (priceCounter >= 2)
                                {
                                    System.out.println((new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " "));
                                    price = price + (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ") + ".";
                                }
                            }
                            else if (isPriceU)
                            {
                                String isPriceUS = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                if("Unavailable".equals(isPriceUS))
                                {
                                    searchResultsForParsing += "$" + isPriceUS + "$";
                                }
                                System.out.println("isPriceU: " + isPriceUS);
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
                            else if (isPriceU)
                            {
                                isPriceU = false;
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
        //Log.println(Log.ERROR, "log", "******RESULT" + result + "++++++++");
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
            else if (i % 3 == 1)
            {
                nameResultsArray.add(resultsArray[i]);
            }
            else if (i % 3 == 2)
            {
                priceResultsArray.add(resultsArray[i]);
            }
        }

        for (int i = 0; i < nameResultsArray.size(); i++)
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