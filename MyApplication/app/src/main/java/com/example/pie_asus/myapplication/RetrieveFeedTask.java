package com.example.pie_asus.myapplication;

import android.os.AsyncTask;

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
class RetrieveFeedTask extends AsyncTask<Void, Void, Void>
{

    OkHttpClient client = new OkHttpClient();
    private Exception exception;

    String run(String url) throws IOException
    {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        RetrieveFeedTask example = new RetrieveFeedTask();
        String response = null;
        try
        {
            response = example.run("https://shop.jbhifi.co.nz/support.aspx?q=2tb+hard+drive+seagate");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println(response);
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
                            }
                            else if (name.equalsIgnoreCase("p"))
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
                                System.out.println(
                                        (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " "));
                            }
                            else if (isPrice)
                            {
                                if (priceCounter == 0)
                                {
                                    System.out.println(
                                            (new String(ch, start, length)).trim().replaceAll("[\\t\\n\\r\\s]+", " "));
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

        return null;
    }
}