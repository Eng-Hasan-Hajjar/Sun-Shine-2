package com.example.tasniema.sunshine;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;



public class SunshineHome extends AppCompatActivity {

    ArrayAdapter mForecastAdapter;
    String forecastJsonStr, in, country, temperature;
    URL url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sunshine_home);
/*
        String[] forecastArray = {
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/40",
                "Weds - Cloudy - 72/63",
                "Thurs - Ast - 75/65",
                "Fri - Heavy Rain - 65/56",
                "Sat - Sunny - 80/68"
        };
        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray));
*/


        mForecastAdapter = new ArrayAdapter<String>
                (this, R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,new ArrayList<String>());
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute("94043");
        ListView listView = (ListView)findViewById(R.id.listview_forecast);
       // listView.setTextColor(Color.RED);
        listView.setAdapter(mForecastAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh)
        {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94043");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            try {
                //URL url = new URL(createURL(params[0]).toString());
                // Log.v(LOG_TAG, "Built URI" + createURL(params[0]).toString());

                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=776dedb6f686cf859ad2b7d4f4743bee");
                forecastJsonStr = makeHTTPRequest(url);
                Log.v(LOG_TAG, "Forecast Jason String:" + forecastJsonStr);
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting to parse it.
                return null;
            }

            try {
                return getWeatherDateFromJson(forecastJsonStr, 7);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
    }

                @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            if(strings != null)
            {
                mForecastAdapter.clear();
                for (String dayForecastStr:strings)
                {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }


        private String makeHTTPRequest(URL url) throws IOException {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=776dedb6f686cf859ad2b7d4f4743bee");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                forecastJsonStr = readFromStream(inputStream);
            } catch (IOException e) {
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    reader.close();
                }
            }
            return forecastJsonStr;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            } else {
                InputStreamReader inputStreamReader =
                        new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    output.append(line + "\n");
                }

                if (output.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
            }

            return output.toString();
        }

        private String[] getWeatherDateFromJson(String forecastJsonStr, int numDays)   throws JSONException {
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;

                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }

        private String formatHighLows(double high, double low) {

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "/" + roundedLow;

            return highLowStr;
        }

        private String getReadableDateString(long time) {
            SimpleDateFormat shortendDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortendDateFormat.format(time);
        }
    }
}
