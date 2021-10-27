package com.cookandroid.graduation_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CovidInfoActivity extends AppCompatActivity {

    TextView defCnt;
    TextView isoClearCnt;
    TextView isollngCnt;
    TextView deathCnt;
    TextView incDec;
    TextView localOccCnt;
    TextView overFlowCnt;

    String sdefCnt;
    String sisoClearCnt;
    String sisollngCnt;
    String sdeathCnt;
    String sincDec;
    String slocalOccCnt;
    String soverFlowCnt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_covid_info);

        defCnt = findViewById(R.id.defCnt);
        isoClearCnt = findViewById(R.id.isoClearCnt);
        isollngCnt = findViewById(R.id.isollngCnt);
        deathCnt = findViewById(R.id.deathCnt);
        incDec = findViewById(R.id.incDec);
        localOccCnt = findViewById(R.id.localOccCnt);
        overFlowCnt = findViewById(R.id.overFlowCnt);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getXmlData();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        defCnt.setText(sdefCnt);
                        isoClearCnt.setText(sisoClearCnt);
                        isollngCnt.setText(sisollngCnt);
                        deathCnt.setText(sdeathCnt);
                        incDec.setText(sincDec);
                        localOccCnt.setText(slocalOccCnt);
                        overFlowCnt.setText(soverFlowCnt);
                    }
                });
            }
        }).start();
    }

    void getXmlData(){

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat time = new SimpleDateFormat("HH");
        String getTime = time.format(date);
        SimpleDateFormat today = new SimpleDateFormat("yyyyMMdd");
        String getToday = today.format(date);;

        if(Integer.parseInt(getTime) < 11){
            getToday = String.valueOf(Integer.parseInt(getToday) - 1);
        }

        String queryUrl="http://openapi.data.go.kr/openapi/service/rest/Covid19/" +
                "getCovid19SidoInfStateJson?serviceKey=FATOsFEwxZKqBOgWR6mHtxl%2Fe%2BTJ8IysGr%2FgK" +
                "PdqMvabMHkoLxFiUaTdUeaMUMeAhP3ARZWXjOSGVef8HdHo8A%3D%3D&pageNo=1&numOfRows=10&sta" +
                "rtCreateDt="+getToday+"&endCreateDt="+getToday;
        try{
            URL url= new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is= url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory= XmlPullParserFactory.newInstance();//xml파싱을 위한
            XmlPullParser xpp= factory.newPullParser();
            xpp.setInput( new InputStreamReader(is, "UTF-8") ); //inputstream 으로부터 xml 입력받기

            String tag;

            xpp.next();
            int eventType= xpp.getEventType();
            int i = 0;
            while( eventType != XmlPullParser.END_DOCUMENT ){
                switch( eventType ){
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag= xpp.getName();//테그 이름 얻어오기

                        if(tag.equals("item")) {
                            i = i+1;
                        }
                        else if(i==19 && tag.equals("defCnt")){
                            xpp.next();
                            sdefCnt = xpp.getText();
                        }
                        else if(i==19 && tag.equals("isolClearCnt")){
                            xpp.next();
                            sisoClearCnt = xpp.getText();
                        }
                        else if(i==19 && tag.equals("isolIngCnt")){
                            xpp.next();
                            sisollngCnt = xpp.getText();
                        }
                        else if(i==19 && tag.equals("deathCnt")){
                            xpp.next();
                            sdeathCnt = xpp.getText();
                        }
                        else if(i==19 && tag.equals("incDec")){
                            xpp.next();
                            sincDec = xpp.getText();
                        }
                        else if(i==19 && tag.equals("localOccCnt")){
                            xpp.next();
                            slocalOccCnt = xpp.getText();
                        }
                        else if(i==19 && tag.equals("overFlowCnt")){
                            xpp.next();
                            soverFlowCnt = xpp.getText();
                        }

                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType= xpp.next();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}