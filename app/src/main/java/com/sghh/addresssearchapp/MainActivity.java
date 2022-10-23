package com.sghh.addresssearchapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private TextView textView;
    private Button button;

    private final String urlModel = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=";

    private BackgroundTask backgroundTask = new BackgroundTask();
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editPostalCodeTextNumber);
        textView = findViewById(R.id.responseText);
        button = findViewById(R.id.submitButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // editTextが7桁ならtrue
                if (editText.length() == 7) {
                    // シングルスレッドを作成
                    executorService = Executors.newSingleThreadExecutor();
                    // BackgroundTaskの処理をシングルスレッドで実行
                    executorService.submit(backgroundTask);
                } else {
                    // 7桁まで入力してくださいのエラートースト表示
                    Toast.makeText(getApplicationContext(), R.string.inputWarning, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 非同期処理クラス
    private class BackgroundTask implements Runnable {
        @Override
        public void run() {
            Editable getText = editText.getText();
            String postalCode = getText.toString();
            getAddress(postalCode);
        }
    }

    // 住所を取得するメソッド
    private void getAddress(String urlSt) {
        // HTTP接続のレスポンスデータとして取得するInputStreamオブジェクトを宣言
        InputStream inputStream = null;
        // 郵便番号検索APIから取得したJSON文字列を格納する
        String result = "";
        try {
            // URLオブジェクトを生成
            URL url = new URL(urlModel + urlSt);
            // URLオブジェクトからHttpURLConnectionオブジェクトを取得
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // データ取得に使っても良い時間を設定
            urlConnection.setReadTimeout(1000);
            // 接続に使っても良い時間を設定
            urlConnection.setConnectTimeout(1000);

            // リクエストメソッド
            urlConnection.setRequestMethod("GET");

            // 接続
            urlConnection.connect();
            // レスポンスデータを取得
            inputStream = urlConnection.getInputStream();
            // レスポンスデータであるInputStreamオブジェクトを文字列に変換
            result = isString(inputStream);

            textView.setText(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // InputStreamオブジェクトを文字列に変換するメソッド
    private String isString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer sb = new StringBuffer();
        char[] b = new char[1024];
        int line;
        while (0 <= (line = reader.read(b))) {
            sb.append(b, 0, line);
        }
        return sb.toString();
    }
}