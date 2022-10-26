package com.sghh.addresssearchapp;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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

        //
        Looper mainLooper = Looper.getMainLooper();
        // スレッド間の通信を行ってくれるオブジェクト
        Handler handler = HandlerCompat.createAsync(mainLooper);

        editText = findViewById(R.id.editPostalCodeTextNumber);
        textView = findViewById(R.id.responseText);
        Button button = findViewById(R.id.submitButton);

        button.setOnClickListener(view -> {
            // editTextが7桁ならtrue
            if (editText.length() == 7) {
                // ログ取得
                logThread("Main");

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
            // ログ取得
            logThread("BackgroundTask");

            // editText欄にある数値を取得
            Editable getText = editText.getText();
            // getTextを文字列に変換し格納
            String postalCode = getText.toString();

            // UiInfoTaskをインスタンス化&getAddressメソッドの実行
            UiInfoTask uiInfoTask = new UiInfoTask(getAddress(postalCode));
            // Handlerオブジェクトを生成した元スレッドで画面描画の処理を行わせる
            _handler.post(uiInfoTask);
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
            // ログ取得
            logThread("UiInfoTask");

            textView.setText(_result);
        }
    }

    // 住所を取得するメソッド
    private String getAddress(String urlSt) {
        // ログ取得
        logThread("getAddress");
        // ベースとなるURL
        String urlModel = "https://zipcloud.ibsnet.co.jp/api/search?zipcode=";

        // HTTP接続のレスポンスデータとして取得するInputStreamオブジェクトを宣言
        InputStream inputStream;
        // 郵便番号検索APIから取得したJSON文字列を格納する
        String result = null;
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
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

    // スレッドをログで確認するメソッド
    private static void logThread(String tag) {
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        Log.i(tag, String.format("Thread = %s(%d)", threadName, threadId));
    }
}