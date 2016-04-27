package com.example.pie_asus.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by PIE-ASUS on 24/03/2016.
 */
// http://developer.android.com/reference/android/os/AsyncTask.html
class RetrieveFeedTask extends AsyncTask<Void, Void, String>
{
    private OkHttpClient client = new OkHttpClient();
    private Exception exception;

    private Context context;

    private String searchResults = "", searchInput="";

    private TextView tvSearchResults;

    // http://stackoverflow.com/questions/10996479/how-to-update-a-textview-of-an-activity-from-another-classssss
    public RetrieveFeedTask(Context context, String searchInput)
    {
        this.context = context;
        this.tvSearchResults = (TextView)((Activity)(this.context)).findViewById(R.id.search_results);
        this.searchInput = searchInput.replaceAll("\\s+","+");
    }

    public RetrieveFeedTask()
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
        RetrieveFeedTask example = new RetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("https://shop.jbhifi.co.nz/support.aspx?q="+this.searchInput+"&source=all&sort=&plow=0&phigh=0&onsale=0&instock=0&len=10000");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //System.out.println(response);
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
                        private int priceCounter = 0;

                        public void startElement(String uri, String localName, String name, Attributes a)
                        {
                            if (name.equalsIgnoreCase("h2"))
                            {
                                if ("title_list".equals(a.getValue("class")))
                                {
                                    isName = true;
                                }
                            } else if (name.equalsIgnoreCase("p"))
                            {
                                if ("price_list".equals(a.getValue("class")))
                                {
                                    isPrice = true;
                                }
                            }
                        }

                        public void characters(char[] ch, int start, int length)
                        {
                            if (isName)
                            {
                                String content = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                System.out.println(content);
                                searchResults = searchResults + content + "\n";
                            } else if (isPrice)
                            {
                                if (priceCounter == 0)
                                {
                                    String content = (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " ");
                                    System.out.println(content);
                                    searchResults = searchResults + content + "\n";
                                }
                                priceCounter++;
                            }
                        }

                        public void endElement(String uri, String localName, String qName)
                        {
                            if (isName)
                            {
                                isName = false;
                            } else if (isPrice)
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

        return searchResults;
    }

    protected void onPostExecute(String result)
    {
        this.tvSearchResults.setText(result);
    }
}