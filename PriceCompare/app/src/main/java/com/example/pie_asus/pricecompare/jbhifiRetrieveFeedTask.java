package com.example.pie_asus.pricecompare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by PIE-ASUS on 24/03/2016.
 * http://stackoverflow.com/questions/17634643/how-to-add-a-hyperlink-into-a-preference-screen-preferenceactivity
 * http://developer.android.com/reference/android/os/AsyncTask.html
 * http://stackoverflow.com/questions/3375166/android-drawable-images-from-url
 * http://stackoverflow.com/questions/9220039/android-preferencescreen-title-in-two-lines
 */

class jbhifiRetrieveFeedTask extends AsyncTask<Void, Void, String>
{
    private OkHttpClient client = new OkHttpClient();
    private Exception exception;

    private Context context;

    private String searchResults = "", searchInput = "", searchResultsForParsing = "";

    private Preference preference;

    ArrayList<Drawable> iconResultsArray = new ArrayList<Drawable>();

    // http://stackoverflow.com/questions/10996479/how-to-update-a-textview-of-an-activity-from-another-classssss
    public jbhifiRetrieveFeedTask(Preference preference, String searchInput)
    {
        this.preference = preference;
        this.searchInput = searchInput.replaceAll("\\s+", "+");
        try
        {
            this.searchInput = URLEncoder.encode(searchInput, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public jbhifiRetrieveFeedTask()
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
        jbhifiRetrieveFeedTask example = new jbhifiRetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("https://shop.jbhifi.co.nz/support.aspx?q=" + this.searchInput + "&source=all&sort=price_lh&plow=0&phigh=0&onsale=0&instock=0&len=10000");
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
                            private boolean isA = false;
                            private int priceCounter = 0;

                            public void startElement(String uri, String localName, String name, Attributes a)
                            {
                                if (name.equalsIgnoreCase("h2"))
                                {
                                    if ("title_list".equals(a.getValue("class")))
                                    {
                                        isName = true;
                                    }
                                }
                                else if (name.equalsIgnoreCase("p"))
                                {
                                    if ("price_list".equals(a.getValue("class")))
                                    {
                                        isPrice = true;
                                    }
                                }
                                else if (name.equalsIgnoreCase("a"))
                                {
                                    if ("image_list".equals(a.getValue("class")))
                                    {
                                        searchResultsForParsing = searchResultsForParsing + "https://shop.jbhifi.co.nz" + a.getValue("href") + "$";
                                        isA = true;
                                    }
                                }
                                else if (name.equalsIgnoreCase("img"))
                                {
                                    if(isA)
                                    {
                                        Drawable icon = null;
                                        try
                                        {
                                            icon = drawableFromUrl("https://shop.jbhifi.co.nz" + a.getValue("src"));
                                        }
                                        catch (IOException e)
                                        {
                                            e.printStackTrace();
                                        }
                                        iconResultsArray.add(icon);
                                        isA=false;
                                    }
                                }
                            }

                            public void characters(char[] ch, int start, int length)
                            {
                                if (isName)
                                {
                                    String content = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                    //System.out.println(content);
                                    searchResults = searchResults + content + "\n";
                                    searchResultsForParsing = searchResultsForParsing + content + " ";
                                }
                                else if (isPrice)
                                {
                                    if (priceCounter == 0)
                                    {
                                        String content = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                        //System.out.println(content);
                                        searchResults = searchResults + content + "\n";
                                        searchResultsForParsing = searchResultsForParsing + content + "$";
                                    }
                                    priceCounter++;
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
                                    isPrice = false;
                                    priceCounter = 0;
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
        PreferenceManager preferenceManager = preference.getPreferenceManager();
        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_jbhifi_search_results");
        //preferenceCategory.removeAll();

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

        for (int i = 0; i < priceResultsArray.size(); i++)
        {
            TwoLinePreference resultPreference = new TwoLinePreference(preference.getContext());
            resultPreference.setKey("pref_name");
            resultPreference.setTitle(nameResultsArray.get(i));
            resultPreference.setSummary("$"+priceResultsArray.get(i));
            Intent redirect = new Intent();
            redirect.setData(Uri.parse(urlResultsArray.get(i)));
            redirect.setAction("android.intent.action.VIEW");
            resultPreference.setIntent(redirect);

            resultPreference.setIcon(iconResultsArray.get(i));

            preferenceCategory.addPreference(resultPreference);
            //Log.println(Log.ERROR,"log","******"+result+"++++++++");
        }

    }


    public Drawable drawableFromUrl(String url) throws IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(x);
    }
}

