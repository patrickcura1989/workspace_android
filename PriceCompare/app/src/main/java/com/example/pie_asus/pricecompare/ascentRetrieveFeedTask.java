package com.example.pie_asus.pricecompare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
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

class ascentRetrieveFeedTask extends AsyncTask<Void, Void, String>
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
    public ascentRetrieveFeedTask(Preference preference, String searchInput)
    {
        this.preference = preference;
        this.searchInput = searchInput.replaceAll("\\s+", "+");
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

    public ascentRetrieveFeedTask()
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
        ascentRetrieveFeedTask example = new ascentRetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("http://www.ascent.co.nz/search.aspx?T1=" + this.searchInput );
            /*
            // for testing
            String toast = response;
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.html");
            file.createNewFile();
            //write the bytes in file
            if (file.exists())
            {
                OutputStream fo = new FileOutputStream(file);
                fo.write(toast.getBytes());
                fo.close();
            }
            */
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
                        private String priceFull = "", productNameFull = "";

                        public void startElement(String uri, String localName, String name, Attributes a)
                        {
                            if (name.equalsIgnoreCase("td"))
                            {
                                if ("bodytxt1".equals(a.getValue("class")) )
                                {
                                    isTD = true;
                                }
                                else if ("right".equals(a.getValue("align")) && "50".equals(a.getValue("width")))
                                {
                                    isPrice = true;
                                }
                                //System.out.println(a.getValue("align")+"");
                                //System.out.println(a.getValue("style")+"");
                            }
                            else if(name.equalsIgnoreCase("a"))
                            {
                                if(isTD)
                                {
                                    searchResultsForParsing += "http://www.ascent.co.nz/" + a.getValue("href") + "\n";
                                    urlResultsArray.add("http://www.ascent.co.nz/" + a.getValue("href"));
                                    isTD = false;
                                    isName = true;
                                }
                            }
                        }

                        public void characters(char[] ch, int start, int length)
                        {
                            if (isName)
                            {
                                String productName = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                //System.out.println(productName);
                                productNameFull+=productName;
                            }
                            else if (isPrice)
                            {
                                String price = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                //System.out.println(productName);
                                priceFull+=price;
                            }
                        }

                        public void endElement(String uri, String localName, String qName)
                        {
                            if (isName)
                            {
                                isName = false;
                                searchResultsForParsing += productNameFull + "\n";
                                nameResultsArray.add(productNameFull);
                                productNameFull = "";
                            }
                            else if (isPrice)
                            {
                                isPrice = false;
                                searchResultsForParsing += priceFull + "\n";
                                priceResultsArray.add(priceFull);
                                priceFull = "";
                            }
                            else if (isTD)
                            {
                                isTD = false;
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
        //System.out.println(result);
        /*
        // For Testing Purposes
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "parse.txt");
        try
        {
            file.createNewFile();
            //write the bytes in file
            if (file.exists())
            {
                OutputStream fo = new FileOutputStream(file);
                fo.write(result.getBytes());
                fo.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        */

        PreferenceManager preferenceManager = preference.getPreferenceManager();
        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceManager.findPreference("pref_key_ascent_search_results");
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