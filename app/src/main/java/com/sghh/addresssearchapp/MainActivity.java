package com.sghh.addresssearchapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private TextView textView;

    private ExecutorService executorService;

    @UiThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mainスレッドを戻り値として取得
        Looper mainLooper = Looper.getMainLooper();
        // スレッド間の通信を行ってくれるオブジェクト
        Handler handler = HandlerCompat.createAsync(mainLooper);

        editText = findViewById(R.id.editPostalCodeTextNumber);
        textView = findViewById(R.id.responseText);
        Button button = findViewById(R.id.submitButton);

        button.setOnClickListener(view -> {
            // editTextが7桁ならtrue
            if (editText.length() == 7) {
                // BackgroundTaskをインスタンス化
                BackgroundTask backgroundTask = new BackgroundTask(handler);
                // シングルスレッドを作成
                executorService = Executors.newSingleThreadExecutor();
                // BackgroundTaskの処理をシングルスレッドで実行
                executorService.submit(backgroundTask);
            } else {
                // 7桁まで入力してくださいのエラートースト表示
                Toast.makeText(getApplicationContext(), R.string.inputWarning, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 非同期処理クラス
    private class BackgroundTask implements Runnable {
        // Handlerオブジェクト
        private final Handler _handler;

        // コンストラクト
        public BackgroundTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            // editText欄にある数値を取得
            Editable getText = editText.getText();
            // getTextを文字列に変換し格納
            String postalCode = getText.toString();

            // 郵便番号検索APIから取得したJSON文字列を格納する変数
            String addressJSON = getAddress(postalCode);
            // addressJSONを住所の形に加工した文字列を格納する変数
            String processingJSON = getJSONProcessing(addressJSON);
            // YahooAPIから取得したJSON文字列を格納する変数
            String yahooJSON = getYahooAPI(processingJSON);
            // yahooJSONの座標を取得する変数
            String coordinatesJSON = getYahooJSONProcessing(yahooJSON);
            // yahooJSONで取得した座標でgoogleMAPを表示
            googleMap(coordinatesJSON);

            // UiInfoTaskをインスタンス化
//            UiInfoTask uiInfoTask = new UiInfoTask(coordinatesJSON);

            // Handlerオブジェクトを生成した元スレッドで画面描画の処理を行わせる
//            _handler.post(uiInfoTask);
        }
    }

    // 非同期処理クラスのデータをUIスレッドに反映するクラス
    private class UiInfoTask implements Runnable {
        // 取得した住所情報のJSON文字列
        private final String _result;

        // コンストラクタ
        public UiInfoTask(String result) {
            _result = result;
        }

        @UiThread
        @Override
        public void run() {
            textView.setText(_result);
        }
    }

    // 郵便番号検索APIから住所を取得するメソッド
    private String getAddress(String urlSt) {
        // リクエストURL
        String requestURL = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=" + urlSt;

        // 郵便番号検索APIから取得したJSON文字列を格納する
        String result = null;
        try {
            // URLオブジェクトを生成
            URL url = new URL(requestURL);
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // データ取得に使っても良い時間を設定
            urlConnection.setReadTimeout(5000);
            // 接続に使っても良い時間を設定
            urlConnection.setConnectTimeout(5000);

            // リクエストメソッド
            urlConnection.setRequestMethod("GET");

            // 接続
            urlConnection.connect();

            // レスポンスデータを取得
            try (InputStream inputStream = urlConnection.getInputStream()) {
                // レスポンスデータであるInputStreamオブジェクトを文字列に変換
                result = isString(inputStream);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // HttpURLConnectionオブジェクトを開放
            urlConnection.disconnect();

        }
        catch (SocketException e) {
            Log.d("SocketException", "通信タイムアウト", e);
        }
        catch (IOException e) {
            Log.d("IOException", "通信失敗", e);
            e.printStackTrace();
        }

        return result;
    }

    // 郵便番号検索APIから取得したJSONデータを加工するメソッド
    private String getJSONProcessing(String _result) {
        // 住所
        String address = "";
        try {
            // JSONObjectオブジェクトを_resultを引数に生成
            JSONObject jsonObject = new JSONObject(_result);
            // 配列データをgetJSONArray()で取得
            JSONArray arrayJSON = jsonObject.getJSONArray("results");
            // 配列データを取り出すためgetJSONObject()で1番目のデータを取得
            JSONObject addressJSON = arrayJSON.getJSONObject(0);

            // 都道府県名を取得
            String prefectureName = addressJSON.getString("address1");
            // 市区町村名を取得
            String cityName = addressJSON.getString("address2");
            // 町名を取得
            String townName = addressJSON.getString("address3");

            // 住所を定義
            address = prefectureName + cityName + townName;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return address;
    }

    // YahooAPIから緯度経度を取得するメソッド
    private String getYahooAPI(String address) {
        // apiKey
        String apiKey = "dj00aiZpPTJibE1aWXQyU2ZleCZzPWNvbnN1bWVyc2VjcmV0Jng9ODY-";
        // リクエストURL
        String requestURL = "https://map.yahooapis.jp/geocode/V1/geoCoder?output=json&recursive=true&appid=" + apiKey + "&query=" + address;
        // 郵便番号検索APIから取得したJSON文字列を格納する
        String result = null;

        try {
            // URLオブジェクトを生成
            URL url = new URL(requestURL);
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // データ取得に使っても良い時間を設定
            urlConnection.setReadTimeout(3000);
            // 接続に使っても良い時間を設定
            urlConnection.setConnectTimeout(3000);

            // リクエストメソッド
            urlConnection.setRequestMethod("GET");

            // 接続
            urlConnection.connect();

            // レスポンスデータを取得
            try (InputStream inputStream = urlConnection.getInputStream()) {
                // レスポンスデータであるInputStreamオブジェクトを文字列に変換
                result = isString(inputStream);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // HttpURLConnectionオブジェクトを開放
            urlConnection.disconnect();

        }
        catch (SocketException e) {
            Log.d("SocketException", "通信タイムアウト", e);
        }
        catch (IOException e) {
            Log.d("IOException", "通信失敗", e);
            e.printStackTrace();
        }

        return result;
    }

    // YahooAPIから取得したJSONデータを加工するメソッド
    private String getYahooJSONProcessing(String _result) {
        // 座標
        String coordinates = null;

        try {
            // JSONObjectオブジェクトを_resultを引数に生成
            JSONObject jsonObject = new JSONObject(_result);
            // 配列データをgetJSONArray()で取得
            JSONArray arrayJSON = jsonObject.getJSONArray("Feature");
            // 配列データを取り出すためgetJSONObject()で1番目のデータを取得
            JSONObject addressJSON = arrayJSON.getJSONObject(0);

            // Geometryを取得
            JSONObject geometryJSON = addressJSON.getJSONObject("Geometry");
            // 座標を取得
            coordinates = geometryJSON.getString("Coordinates");

            // 座標を緯度経度の順番に並び替え
            // (,)の文字列位置を取得
            int position = coordinates.indexOf(",");
            // 緯度を取得
            String lat = coordinates.substring(position);
            // latから(,)を削除
            lat = lat.replace(",", "");
            // 経度を取得
            String lon = coordinates.substring(0, position);
            // 座標を再作成
            coordinates = lat + "," + lon;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    // Google Mapを起動するIntentを投げるメソッド
    private void googleMap(String coordinates) {
        // Create a Uri from an intent string. Use the result to create an Intent.
        Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + coordinates);

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

        // Attempt to start an activity that can handle the Intent
        startActivity(mapIntent);


    }

    // InputStreamオブジェクトを文字列に変換するメソッド
    private String isString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] b = new char[1024];
        int line;
        while (0 <= (line = reader.read(b))) {
            sb.append(b, 0, line);
        }
        return sb.toString();
    }
}