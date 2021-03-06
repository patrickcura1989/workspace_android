package com.example.pie_asus.pricecompare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

class tmRetrieveFeedTask extends AsyncTask<Void, Void, String>
{
    private OkHttpClient client = new OkHttpClient();
    private Exception exception;

    private Context context;

    private String searchResults = "", searchInput = "", searchResultsForParsing = "", htmlCode="";

    private Preference preference;

    ArrayList<String> nameResultsArray = new ArrayList<String>();
    ArrayList<String> priceResultsArray = new ArrayList<String>();
    ArrayList<String> urlResultsArray = new ArrayList<String>();

    // http://stackoverflow.com/questions/10996479/how-to-update-a-textview-of-an-activity-from-another-classssss
    public tmRetrieveFeedTask(Preference preference, String searchInput)
    {
        this.preference = preference;
        //this.searchInput = searchInput;
        this.searchInput = searchInput.replaceAll("[^\\w ]+", "");
        //this.searchInput = searchInput.replaceAll("\\s+", "+");
        /*
        try
        {
            this.searchInput = URLEncoder.encode(searchInput, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        */
    }

    public tmRetrieveFeedTask()
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
        tmRetrieveFeedTask example = new tmRetrieveFeedTask();
        String response = null;


        try
        {
            //System.out.println("http://www.trademe.co.nz/Browse/SearchResults.aspx?sort_order=price_asc&searchString=" + this.searchInput + "&type=Search&searchType=all&user_region=100&user_district=0&generalSearch_keypresses=11&generalSearch_suggested=0&generalSearch_suggestedCategory=&buy=buynow&v=List&pay=paynow");
            response = example.run("http://www.trademe.co.nz/Browse/SearchResults.aspx?sort_order=price_asc&searchString=" + this.searchInput + "&type=Search&searchType=all&user_region=100&user_district=0&generalSearch_keypresses=11&generalSearch_suggested=0&generalSearch_suggestedCategory=&buy=buynow&v=List&pay=paynow");
            //response = example.run("http://www.trademe.co.nz/Browse/SearchResults.aspx?sort_order=price_asc&searchString=" + URLEncoder.encode(this.searchInput, "UTF-8") + "&type=Search&searchType=all&user_region=100&user_district=0&generalSearch_keypresses=11&generalSearch_suggested=0&generalSearch_suggestedCategory=&buy=buynow&v=List&pay=paynow");
            /*
            // For Testing Purposes
            //System.out.println("----------------->"+Environment.getExternalStorageDirectory());
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "parse.html");

            file.createNewFile();
            //write the bytes in file
            if (file.exists())
            {
                OutputStream fo = new FileOutputStream(file);
                fo.write(response.getBytes());
                fo.close();
            }
            */
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (null != response) // used for no internet connection, no response is received if there is no internet connection
        {
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
                            //private boolean isLi = false;
                            private String priceFull = "", productNameFull = "";
                            private int priceCounter = -1;

                            public void startElement(String uri, String localName, String name, Attributes a)
                            {
                                if (name.equalsIgnoreCase("a"))
                                {
                                    if ("dotted".equals(a.getValue("class")))
                                    {
                                        searchResultsForParsing += "http://www.trademe.co.nz" + a.getValue("href") + "\n";
                                        urlResultsArray.add("http://www.trademe.co.nz" + a.getValue("href"));
                                        priceResultsArray.add("See Website");
                                        priceCounter++;
                                        isName = true;
                                        //System.out.println(searchResultsForParsing);
                                    }
                                }
                                else if (name.equalsIgnoreCase("div"))
                                {
                                    if ("listingBuyNowPrice".equals(a.getValue("class")))
                                    {
                                        isPrice = true;
                                        //Log.println(Log.ERROR,"log", name + " " + a.getValue("class"));
                                    }
                                }
                            /* // for testing purposes
                            else if (name.equalsIgnoreCase("li"))
                            {
                                if ("listingBuyNowCol".equals(a.getValue("class")))
                                {
                                    isLi = true;
                                    Log.println(Log.ERROR,"log","li listingBuyNowCol start");
                                }
                            }
                            */
                            }

                            public void characters(char[] ch, int start, int length)
                            {
                                if (isName)
                                {
                                    String productName = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                    //System.out.println(productName);
                                    productNameFull += productName;
                                }
                                else if (isPrice)
                                {
                                    String price = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                    //System.out.println(productName);
                                    priceFull += price;
                                }
                            /* // for testing purposes
                            else if (isLi)
                            {
                                Log.println(Log.ERROR,"log","li listingBuyNowCol ongoing");
                            }
                            */
                            }

                            public void endElement(String uri, String localName, String qName)
                            {
                                if (isName)
                                {
                                    isName = false;
                                    searchResultsForParsing += productNameFull + "\n";
                                    //System.out.println(searchResultsForParsing);
                                    nameResultsArray.add(productNameFull);
                                    productNameFull = "";
                                }
                                else if (isPrice)
                                {
                                    isPrice = false;
                                    searchResultsForParsing += priceFull + "\n";
                                    //System.out.println(searchResultsForParsing);
                                    priceResultsArray.set(priceCounter, priceFull);
                                    priceFull = "";
                                    //Log.println(Log.ERROR,"log","price end");
                                }
                            /* // for testing purposes
                            else if (isLi)
                            {
                                isLi = false;
                                Log.println(Log.ERROR,"log","li listingBuyNowCol end");
                            }
                            */
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
        }
        return searchResultsForParsing;
    }

    protected void onPostExecute(String result)
    {
        //System.out.println(result);
        PreferenceManager preferenceManager = preference.getPreferenceManager();
        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_tm_search_results");
        //preferenceCategory.removeAll();

        //Toast.makeText(preference.getContext(), "Here "+ priceResultsArray.size(), Toast.LENGTH_LONG).show();

        if(priceResultsArray.size() <=0)
        {
            //Toast.makeText(preference.getContext(), "Here", Toast.LENGTH_LONG).show();
            Preference resultPreference = new Preference(preference.getContext());
            resultPreference.setKey("pref_name");
            resultPreference.setTitle("No Results Found");
            //resultPreference.setSummary("$"+priceResultsArray.get(i));
            preferenceCategory.addPreference(resultPreference);
        }

        for (int i = 0; i < nameResultsArray.size(); i++)
        {
            TwoLinePreference resultPreference = new TwoLinePreference(preference.getContext());
            resultPreference.setKey("pref_name");
            resultPreference.setTitle(nameResultsArray.get(i));
            resultPreference.setSummary(priceResultsArray.get(i));
            Intent redirect = new Intent();
            redirect.setData(Uri.parse(urlResultsArray.get(i)));
            redirect.setAction("android.intent.action.VIEW");
            resultPreference.setIntent(redirect);
            preferenceCategory.addPreference(resultPreference);
        }
    }


}