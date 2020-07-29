package org.infinispan.tutorial.embedded;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.infinispan.Cache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpenWeatherMapService extends CachingWeatherService {
   final private static String OWM_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
   private DocumentBuilder db;
   private final String apiKey;

   public OpenWeatherMapService(String apiKey, Cache<String, LocationWeather> cache) {
      super(cache);
      this.apiKey = apiKey;
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
         db = dbf.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
      }
      setProxy(System.getenv("HTTP_PROXY"));
   }
   private void setProxy(String urlStr) {
     // urlStr =  http://user:password@host:port
     try {
       URL url = new URL(urlStr);

       System.setProperty("proxySet", "true");
       System.setProperty("proxyHost", url.getHost());
       System.setProperty("proxyPort", "" + url.getPort());

       String userInfo[] = url.getUserInfo().split(":", 0);
       if(userInfo.length >= 2) {
          Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userInfo[0], userInfo[1].toCharArray());
                }
           });
        }
     } catch (MalformedURLException e) {
          e.printStackTrace();
     } catch (NullPointerException e) {
          e.printStackTrace();
     }
       
   }

   private Document fetchData(String location) {
      HttpURLConnection conn = null;
      try {
         String query = String.format("%s?q=%s&mode=xml&units=metric&APPID=%s", OWM_BASE_URL,
               URLEncoder.encode(location.replaceAll(" ", ""), "UTF-8"), apiKey);
         URL url = new URL(query);
         conn = (HttpURLConnection) url.openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty("Accept", "application/xml");
         if (conn.getResponseCode() != 200) {
            throw new Exception(location + " HTTP " + conn.getResponseCode());
         }
         return db.parse(conn.getInputStream());
      } catch (Exception e) {
         System.out.println("==========================");
         System.out.println(e);
         System.out.println("==========================");
         return null;
      } finally {
         if (conn != null) {
            conn.disconnect();
         }
      }
   }

   @Override
   protected LocationWeather fetchWeather(String location) {

      Document dom = fetchData(location);
      if (dom == null) {
         throw new RuntimeException("Unable to reach or get response from open weather service with given OWMAPIKEY : please try with valid OWMAPIKEY or use Random Weather Service");
      }
      Element current = (Element) dom.getElementsByTagName("current").item(0);
      Element temperature = (Element) current.getElementsByTagName("temperature").item(0);
      Element weather = (Element) current.getElementsByTagName("weather").item(0);
      String[] split = location.split(",");
      return new LocationWeather(
            Float.parseFloat(temperature.getAttribute("value")),
            weather.getAttribute("value"),
            split[1].trim());
   }

}
