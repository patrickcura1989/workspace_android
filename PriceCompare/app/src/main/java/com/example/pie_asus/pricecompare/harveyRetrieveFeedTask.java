package com.example.pie_asus.pricecompare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

class harveyRetrieveFeedTask extends AsyncTask<Void, Void, String>
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
    public harveyRetrieveFeedTask(Preference preference, String searchInput)
    {
        this.preference = preference;
        this.searchInput = searchInput.replaceAll("\\s+", "+");
    }

    public harveyRetrieveFeedTask()
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
        harveyRetrieveFeedTask example = new harveyRetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("http://www.harveynorman.co.nz/index.php?subcats=Y&status=A&pshort=N&pfull=N&pname=Y&pkeywords=Y&search_performed=Y&q=" + this.searchInput + "&dispatch=products.search&items_per_page=9000&sort_by=price&sort_order=asc");
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
                            private String priceFull = "", productNameFull = "";

                            public void startElement(String uri, String localName, String name, Attributes a)
                            {
                                if (name.equalsIgnoreCase("a"))
                                {
                                    if ("product-title fn".equals(a.getValue("class")))
                                    {
                                        if(!"Harvey Norman PhotoCentre".equals(a.getValue("title")))
                                        { // for exceptions received after inputting garbage inputs

                                            searchResultsForParsing += a.getValue("href") + "\n";
                                            urlResultsArray.add(a.getValue("href"));

                                            searchResultsForParsing += a.getValue("title") + "\n";
                                            nameResultsArray.add(a.getValue("title"));
                                        }
                                    }
                                }
                                else if (name.equalsIgnoreCase("span"))
                                {
                                    String spanClass = a.getValue("id");

                                    if (spanClass != null && spanClass.matches("(.*)sec_discounted_price(.*)"))
                                    {
                                        isPrice = true;
                                    }
                                }
                            }

                            public void characters(char[] ch, int start, int length)
                            {
                                if (isPrice)
                                {
                                    String price = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                    //System.out.println(productName);
                                    priceFull += price;
                                }
                            }

                            public void endElement(String uri, String localName, String qName)
                            {
                                if (isPrice)
                                {
                                    isPrice = false;
                                    searchResultsForParsing += priceFull + "\n";

                                    priceResultsArray.add(priceFull);
                                    priceFull = "";
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
        }
        return searchResultsForParsing;
    }

    protected void onPostExecute(String result)
    {
        //System.out.println(result);
        PreferenceManager preferenceManager = preference.getPreferenceManager();
        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_harvey_search_results");
        preferenceCategory.removeAll();

        for (int i = 0; i < nameResultsArray.size(); i++)
        {
            Preference resultPreference = new Preference(preference.getContext());
            resultPreference.setKey("pref_name");
            resultPreference.setTitle(nameResultsArray.get(i));
            resultPreference.setSummary("$"+priceResultsArray.get(i));
            Intent redirect = new Intent();
            redirect.setData(Uri.parse(urlResultsArray.get(i)));
            redirect.setAction("android.intent.action.VIEW");
            resultPreference.setIntent(redirect);
            preferenceCategory.addPreference(resultPreference);
        }
    }


}