package com.example.webscraping;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PS4ScrapingActivity extends AppCompatActivity {
    private ScheduledExecutorService scheduledExecutorService;

    private Future<?> logginatiScheduledFuture;
    private Future<?> fetchaListaGiochiScheduledFuture;
    private Future<?> checkaAcquistatoScheduledFuture;

    private RelativeLayout rl_layout;
    private ProgressBar pb_progressBar;
    private TextView txt_msg;
    private WebView wv_login;
    private WebView wv_fetchListaGiochi;
    private WebView wv_checkAcquistato;
    private FloatingActionButton fab_Top;
    private FloatingActionButton fab_Up;
    private FloatingActionButton fab_Down;
    private FloatingActionButton fab_Bottom;
    private ScrollView sv_view;

    private AtomicBoolean staLoggandosi;
    private AtomicBoolean evitaLoginIndesiderate;
    private AtomicBoolean emailScritta;
    private AtomicBoolean passwordScritta;

    private AtomicBoolean staFetchandoListaGiochi;
    private AtomicBoolean evitaFetchIndesiderati;

    private AtomicBoolean staCheckandoAcquistato;
    private AtomicBoolean evitaCheckIndesiderati;

    private AtomicBoolean isLoggato;

    private boolean isRicercaOffline = false;


    private CountDownLatch countDownLogin;
    private CountDownLatch countDownFetch;
    private CountDownLatch countDownCheckAcquistato;

    private int totPagine;
    private int totGiochiPerPagina;
    private int totGiochiDaControllare;

    private AtomicInteger cntPaginaProcessata;
    private AtomicInteger cntGiocoProcessato;

    private final int scrollRange = 1024;
    //private final int scrollResiduo = (int)(500 * getResources().getDisplayMetrics().density);
    private int scrollAttuale;

    private Logginati logginati;
    private FetchaListaGiochiOnline fetchaListaGiochiOnline;
    private FetchaListaGiochiOffline fetchaListaGiochiOffline;
    private CheckaAcquistato checkaAcquistato;
    private CheckaAcquistatoOffline checkaAcquistatoOffline;

    private Gson gson;

    private List<OggettoJson> listaGiochiDaFetchare;
    private List<OggettoJson> listaGiochiGratisTrovati;
    private List<OggettoJson> listaGiochiGratisNonAcquistati;
    private OggettoJson giocoInFetching;

    private OkHttpClient okHttpClient;
    private Request request;
    private Response response;
    private String html;
    private Document document;


    private static final int CONSTRAINT_LAYOUT_ID = R.layout.activity_ps4;
    private static final int WEB_VIEW_LOGIN_ID = R.id.wv_login;
    private static final int WEB_VIEW_FETCH_LISTA_GIOCHI_ID = R.id.wv_fetchListaGiochi;
    private static final int WEB_VIEW_CHECK_ACQUISTATO_ID = R.id.wv_checkAcquistato;

    private static final int TENTATIVI_LOGIN = 10;
    private static final int TENTATIVI_FETCH_LISTA_GIOCHI = 5;
    private static final int TENTATIVI_CHECK_ACQUISTATO = 5;

    private static final int INTERVALLO_TENTATIVO_LOGIN = 1000 / 1;
    private static final int INTERVALLO_TENTATIVO_FETCH_LISTA = 1000 / 1000;
    private static final int INTERVALLO_TENTATIVO_CHECK_ACQUISTATO = 1000 / 100;
    private static final int INTERVALLO_THREAD = 1000 / 1000;

    private static final int COLORE_INFO = Color.rgb(0, 255, 255);
    private static final int COLORE_ERRORE = Color.rgb(255, 0, 0);

    private static final String GIOCHI_OFFLINE_JSON_NAME = "prezzi-offline-ps4";
    private static final String PLAYSTATION_ACCOUNT_URL = "https://my.account.sony.com";
    private static final String PLAYSTATION_COM_URL = "https://www.playstation.com";
    private static final String PLAYSTATION_STORE_URL = "https://store.playstation.com";
    private static final String PLAYSTATION_MAP_URL = PLAYSTATION_COM_URL + "/it-it/site-map/";
    private static final String PLAYSTATION_4_GAMES_URL = PLAYSTATION_STORE_URL + "/it-it/category/44d8bb20-653e-431e-8ad0-c0a365f68d2f/";


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(CONSTRAINT_LAYOUT_ID);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        rl_layout = findViewById(R.id.rl_layout);
        pb_progressBar = findViewById(R.id.pb_progressBar);
        txt_msg = findViewById(R.id.txt_msg);
        fab_Top = findViewById(R.id.fab_Top);
        fab_Up = findViewById(R.id.fab_Up);
        fab_Down = findViewById(R.id.fab_Down);
        fab_Bottom = findViewById(R.id.fab_Bottom);
        sv_view = findViewById(R.id.sv_view);

        gson = new Gson();

        staLoggandosi = new AtomicBoolean(false);
        evitaLoginIndesiderate = new AtomicBoolean(false);
        emailScritta = new AtomicBoolean(false);
        passwordScritta = new AtomicBoolean(false);

        staFetchandoListaGiochi = new AtomicBoolean(false);
        evitaFetchIndesiderati = new AtomicBoolean(false);

        staCheckandoAcquistato = new AtomicBoolean(false);
        evitaCheckIndesiderati = new AtomicBoolean(false);

        isLoggato = new AtomicBoolean(false);

        cntPaginaProcessata = new AtomicInteger(0);
        cntGiocoProcessato = new AtomicInteger(0);

        scrollAttuale = 0;

        System.setProperty("net.dns1", "8.8.8.8");
        System.setProperty("net.dns2", "8.8.4.4");

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        logginati = new Logginati();
        isLoggato.set(false);

        isRicercaOffline = IsRicercaOffline();
        if (isRicercaOffline) {
            fetchaListaGiochiOffline = new FetchaListaGiochiOffline();
            checkaAcquistatoOffline = new CheckaAcquistatoOffline();
        }
        else {
            fetchaListaGiochiOnline = new FetchaListaGiochiOnline();
            checkaAcquistato = new CheckaAcquistato();
        }

        ResetLogin();
        ResetFetchListaGiochi();
        ResetCheckaAcquistato();

        CaricaFinestraLogin();
        CaricaFinestraListaGiochi();
        CaricaFinestraCheckAcquisto();

        listaGiochiDaFetchare = new ArrayList<OggettoJson>();
        listaGiochiGratisTrovati = new ArrayList<OggettoJson>();
        listaGiochiGratisNonAcquistati = new ArrayList<OggettoJson>();

        fab_Top.setOnClickListener(v -> {
            wv_login.scrollTo(0, 0);
            wv_login.post(() -> {
                scrollAttuale = 0;
            });
        });
        fab_Up.setOnClickListener(v -> {
            scrollAttuale -= scrollRange;
            if (0 <= scrollAttuale) {
                wv_login.post(() -> {
                    wv_login.scrollTo(0, scrollAttuale);
                });
            }
            else {
                scrollAttuale = 0;
                wv_login.post(() -> {
                    wv_login.scrollTo(0, scrollAttuale);
                });
            }
        });
        fab_Down.setOnClickListener(v -> {
            scrollAttuale += scrollRange;
            if((int) (wv_login.getContentHeight() * wv_login.getScaleY() - wv_login.getHeight()) >= scrollAttuale) {
                wv_login.post(() -> {
                    wv_login.scrollTo(0, scrollAttuale);
                });
            }
            else {
                scrollAttuale = (int) (wv_login.getContentHeight() * wv_login.getScaleY() - wv_login.getHeight());
                wv_login.post(() -> {
                    wv_login.scrollTo(0, scrollAttuale);
                });
            }
        });
        fab_Bottom.setOnClickListener(v -> {
            wv_login.scrollTo(0, wv_login.getContentHeight());;
            wv_login.post(() -> {
                scrollAttuale = wv_login.getScrollY();
            });
        });

        wv_login.loadUrl(PLAYSTATION_MAP_URL);
    }


    @SuppressLint("SdCardPath")
    private void InizializzaListaGiochi() {
        runOnUiThread(() -> {
            try {
                showMessages("Caricamento database in corso...", false);

                if (isRicercaOffline) {
                    showMessages("Lettura database in corso...", false);

                    listaGiochiDaFetchare = gson.fromJson(getJson(GIOCHI_OFFLINE_JSON_NAME), new TypeToken<List<OggettoJson>>() {}.getType());
                    totGiochiDaControllare = listaGiochiDaFetchare.size();

                    showMessages("Inizio processo in corso...", false);

                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                } else {
                    totPagine = GetTotalePagine();
                    totGiochiPerPagina = GetTotaleElementi();

                    showMessages("Creazione database in corso...", false);

                    listaGiochiDaFetchare = CreaListaOggettiJson(totPagine, totGiochiPerPagina);
                    totGiochiDaControllare = listaGiochiDaFetchare.size();

                    showMessages("Inizio processo in corso...", false);

                    SfogliaPagineOnline();
                }
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void CaricaFinestraLogin() {
        wv_login = findViewById(WEB_VIEW_LOGIN_ID);
        SettaProprietàComuniWebView(wv_login);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void CaricaFinestraListaGiochi() {
        wv_fetchListaGiochi = findViewById(WEB_VIEW_FETCH_LISTA_GIOCHI_ID);
        SettaProprietàComuniWebView(wv_fetchListaGiochi);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void CaricaFinestraCheckAcquisto() {
        wv_checkAcquistato = findViewById(WEB_VIEW_CHECK_ACQUISTATO_ID);
        SettaProprietàComuniWebView(wv_checkAcquistato);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void SettaProprietàComuniWebView(WebView webView) {
//        ClearWebView(webView, false);
        webView.setWebViewClient(new WebViewClientComune());

        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
//        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(PS4ScrapingActivity.this));
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
//        webSettings.setAllowContentAccess(true);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setBlockNetworkImage(false);
//        webSettings.setSafeBrowsingEnabled(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.setAcceptCookie(true);

        String cookies = cookieManager.getCookie(String.valueOf(webView.getUrl()));
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", cookies);

        cookieManager.flush();
    }


    private class WebViewClientComune extends WebViewClient {
        @Override
        public synchronized void onPageFinished(WebView webView, String url) {
            try {
                super.onPageFinished(webView, url);
                if (webView.getId() == WEB_VIEW_LOGIN_ID && !staLoggandosi.get() && !evitaLoginIndesiderate.get()) {
                    staLoggandosi.set(true);

                    logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                } else if (webView.getId() == WEB_VIEW_FETCH_LISTA_GIOCHI_ID && !staFetchandoListaGiochi.get() && !evitaFetchIndesiderati.get()) {
                    staFetchandoListaGiochi.set(true);

                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                } else if (webView.getId() == WEB_VIEW_CHECK_ACQUISTATO_ID && !staCheckandoAcquistato.get() && !evitaCheckIndesiderati.get()) {
                    staCheckandoAcquistato.set(true);

                    if (isRicercaOffline) {
                        checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistatoOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                    }
                    else {
                        checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                    }
                }
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
            if (webView.getId() == WEB_VIEW_LOGIN_ID && !staLoggandosi.get()) {
                evitaLoginIndesiderate.set(false);
            } else if (webView.getId() == WEB_VIEW_FETCH_LISTA_GIOCHI_ID && !staFetchandoListaGiochi.get()) {
                evitaFetchIndesiderati.set(false);
            } else if (webView.getId() == WEB_VIEW_CHECK_ACQUISTATO_ID && !staCheckandoAcquistato.get()) {
                evitaCheckIndesiderati.set(false);
            }
            webView.loadUrl(String.valueOf(webResourceRequest.getUrl()));
            return true;
        }

        @Override
        public synchronized void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public synchronized void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
            super.onReceivedError(webView, webResourceRequest, webResourceError);
            if (
                    !webResourceError.getDescription().equals("net::ERR_TIMED_OUT") &&
                            !webResourceError.getDescription().equals("net::ERR_SOCKET_NOT_CONNECTED") &&
                            !webResourceError.getDescription().equals("net::ERR_FAILED") &&
                            !webResourceError.getDescription().equals("net::ERR_NAME_NOT_RESOLVED") &&
                            !webResourceError.getDescription().equals("net::ERR_CONNECTION_RESET")
            ) {
                showMessages(String.valueOf(webResourceError.getDescription()), true);
            }
        }

        @Override
        public synchronized void onReceivedHttpError(WebView webView, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
            super.onReceivedHttpError(webView, webResourceRequest, webResourceResponse);
            //showMessages(String.valueOf("Errore Fatale! " + webResourceResponse.getStatusCode()), true);
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        @Override
        public synchronized void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            super.onReceivedSslError(webView, sslErrorHandler, sslError);
            sslErrorHandler.proceed();
        }
    }

    private final class Logginati implements Runnable {
        @Override
        public synchronized void run() {
            runOnUiThread(() -> {
                try {
                    if (!isLoggato.get() && countDownLogin.getCount() > 0) {
                        String currentUrl = wv_login.getUrl();

                        if (currentUrl != null) {
                            if (currentUrl.contains(PLAYSTATION_ACCOUNT_URL)) {
                                if (currentUrl.contains("#/signin/input/password")) {
                                    evitaLoginIndesiderate.set(true);

                                    wv_login.evaluateJavascript(
                                            "document.getElementById('signin-password-input-password').value",
                                            password -> {
                                                runOnUiThread(() -> {
                                                    try {
                                                        evitaLoginIndesiderate.set(false);

                                                        updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                        countDownLogin.countDown();

                                                        if (password.equals("null")) {
                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                        } else if (password.equals("\"Tenacious.1990!_\"")) {
                                                            passwordScritta.set(true);

                                                            countDownLogin = new CountDownLatch(0);

                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                                                        }
                                                        else {
                                                            evitaLoginIndesiderate.set(true);

                                                            wv_login.evaluateJavascript("document.getElementById('signin-password-input-password').focus();", null);
                                                            wv_login.evaluateJavascript("setTimeout(function() { document.getElementById('signin-password-input-password').value = 'Tenacious.1990!_'; }, 100);", null);

                                                            evitaLoginIndesiderate.set(false);

                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS); //submit
                                                        }
                                                    } catch (Exception e) {
                                                        showMessages(e.getMessage(), true);
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                            }
                                    );
                                } else if (currentUrl.contains("sonyacct/signin") || currentUrl.contains("#/signin/input/id")) {
                                    evitaLoginIndesiderate.set(true);

                                    wv_login.evaluateJavascript(
                                            "document.getElementById('signin-entrance-input-signinId').value",
                                            email -> {
                                                runOnUiThread(() -> {
                                                    try {
                                                        evitaLoginIndesiderate.set(false);

                                                        updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                        countDownLogin.countDown();

                                                        if (email.equals("null")) {
                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                        } else if (email.equals("\"ferrari.90@hotmail.it_\"")) {
                                                            emailScritta.set(true);

                                                            countDownLogin = new CountDownLatch(0);

                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, 0, TimeUnit.MILLISECONDS);
                                                        }
                                                        else {
                                                            evitaLoginIndesiderate.set(true);

                                                            wv_login.evaluateJavascript("document.getElementById('signin-entrance-input-signinId').focus();", null);
                                                            wv_login.evaluateJavascript("document.getElementById('signin-entrance-input-signinId').value = 'ferrari.90@hotmail.it_';", null);

                                                            evitaLoginIndesiderate.set(false);

                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS); //submit
                                                        }
                                                    } catch (Exception e) {
                                                        showMessages(e.getMessage(), true);
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                            }
                                    );
                                }
                            } else {
                                emailScritta.set(false);
                                passwordScritta.set(false);

                                evitaLoginIndesiderate.set(true);

                                wv_login.evaluateJavascript(
                                        "document.querySelector('[data-qa=\"web-toolbar#profile-container\"]');",
                                        divLoggato -> {
                                            runOnUiThread(() -> {
                                                try {
                                                    evitaLoginIndesiderate.set(false);

                                                    updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                    isLoggato.set(!divLoggato.equals("null"));

                                                    if (!isLoggato.get() && countDownLogin.getCount() > 0) {
                                                        countDownLogin.countDown();
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                    } else {
                                                        countDownLogin = new CountDownLatch(0);
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                    }
                                                } catch (Exception e) {
                                                    showMessages(e.getMessage(), true);
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        }
                                );

//                                wv_login.evaluateJavascript(
//                                        "document.getElementById('__NEXT_DATA__').innerHTML",
//                                        nextData -> {
//                                            runOnUiThread(() -> {
//                                                try {
//                                                    evitaLoginIndesiderate.set(false);
//
//                                                    updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);
//
//                                                    if (nextData.equals("null")) {
//                                                        countDownLogin.countDown();
//                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
//                                                    }
//                                                    else {
//                                                        boolean isLoggedIn = JsonParser.parseString(nextData).getAsJsonObject()
//                                                                .getAsJsonObject("props")
//                                                                .getAsJsonObject("pageProps")
//                                                                .get("isLoggedIn")
//                                                                .getAsBoolean();
//
//                                                        isLoggato.set(isLoggedIn);
//
//                                                        if (!isLoggato.get() && countDownLogin.getCount() > 0) {
//                                                            countDownLogin.countDown();
//                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
//                                                        } else {
//                                                            countDownLogin = new CountDownLatch(0);
//                                                            logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, 0, TimeUnit.MILLISECONDS); //submit
//                                                        }
//                                                    }
//                                                } catch (Exception e) {
//                                                    showMessages(e.getMessage(), true);
//                                                    throw new RuntimeException(e);
//                                                }
//                                            });
//                                        }
//                                );
                            }
                        }
                        else {
                            showMessages("Ma che cazzo Alfio!", true);
                            throw new Exception("Ma che cazzo Alfio!");
                        }
                    }
                    else {
                        hideProgressBar();

                        if (isLoggato.get()) {
                            showMessages("Login effettuato!", false);

                            //DistruggiWebView(wv_login);
                            wv_login.setVisibility(View.GONE);

                            InizializzaListaGiochi();
                        } else {
                            showMessages("Si prega di fare il Log-in.", false);

                            ResetLogin();
                        }
                    }
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private final class FetchaListaGiochiOffline implements Runnable {
        @Override
        public synchronized void run() {
            runOnUiThread(() -> {
                try {
                    if (cntGiocoProcessato.get() < totGiochiDaControllare) {
                        giocoInFetching = listaGiochiDaFetchare.get(cntGiocoProcessato.get());

                        cntGiocoProcessato.getAndIncrement();

                        updateProgress(cntGiocoProcessato.get() * 100 / totGiochiDaControllare);

                        showMessage(String.format("Check acquisto %1$s (Gioco %2$s di %3$s)", giocoInFetching.Descrizione, cntGiocoProcessato.get(), listaGiochiDaFetchare.size()), false);

                        if (!giocoInFetching.IsAcquistato) {
                            evitaCheckIndesiderati.set(false);

                            wv_checkAcquistato.loadUrl(giocoInFetching.UrlGioco);
                        } else {
                            DisegnaOggetto();

                            fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                        }
                    } else {
                        txt_msg.setTextColor(Color.rgb(255, 255, 255));
                        txt_msg.setText(String.format("Ecco la lista dei giochi gratis.\n(Trovati %1$s - Da aggiungere %2$s)", cntGiocoProcessato.get(), listaGiochiGratisNonAcquistati.size()));

                        hideProgressBar();

                        showToast("Ecco la lista dei giochi gratis.");
                    }
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private final class FetchaListaGiochiOnline implements Runnable {
        @Override
        public synchronized void run() {
            runOnUiThread(() -> {
                try {
                    if (giocoInFetching == null && countDownFetch.getCount() > 0) {
                        giocoInFetching = listaGiochiDaFetchare.get(cntGiocoProcessato.get());

                        evitaFetchIndesiderati.set(true);

                        wv_fetchListaGiochi.evaluateJavascript(
                                "document.querySelector(\"" + giocoInFetching.TxtDescrizioneSelector + "\").innerText",
                                descrizione -> {
                                    runOnUiThread(() -> {
                                        try {
                                            evitaFetchIndesiderati.set(false);

                                            if (descrizione.equals("null")) {
                                                countDownFetch.countDown();
                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_TENTATIVO_FETCH_LISTA, TimeUnit.MILLISECONDS);
                                            } else {
                                                giocoInFetching.Descrizione = descrizione.replace("\"", "");

                                                countDownFetch = new CountDownLatch(TENTATIVI_FETCH_LISTA_GIOCHI);
                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                            }
                                        } catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                        );
                    } else if (giocoInFetching.Descrizione != null && giocoInFetching.Prezzo == null) {
                        evitaFetchIndesiderati.set(true);

                        wv_fetchListaGiochi.evaluateJavascript(
                                "document.querySelector(\"" + giocoInFetching.TxtPrezzoSelector + "\").innerText",
                                prezzo -> {
                                    runOnUiThread(() -> {
                                        try {
                                            evitaFetchIndesiderati.set(false);

                                            if (prezzo.equals("null") && countDownFetch.getCount() > 0) {
                                                countDownFetch.countDown();
                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_TENTATIVO_FETCH_LISTA, TimeUnit.MILLISECONDS); //submit
                                            } else {

                                                giocoInFetching.Prezzo = prezzo.replace("\"", "");

                                                if (Arrays.asList(new String[]{"€0,00", "Inclusi", "Gratis", "Acquistato", "Nella raccolta", "Versione di prova del gioco"}).contains(giocoInFetching.Prezzo)) {
                                                    if (!document.location().equals(giocoInFetching.UrlPaginaRicerca)) {
                                                        SetDocument(giocoInFetching.UrlPaginaRicerca);
                                                    }

                                                    giocoInFetching.UrlGioco = PLAYSTATION_STORE_URL + document.select(giocoInFetching.AnchorGiocoSelector).first().attr("href");
                                                    giocoInFetching.SrcAnteprima = document.select(giocoInFetching.ImgAnteprimaSelector).first().attr("src");
                                                    giocoInFetching.IsGratis = true;

                                                    if (Arrays.asList(new String[]{"Acquistato", "Nella raccolta"}).contains(giocoInFetching.Prezzo)) {
                                                        giocoInFetching.IsAcquistato = true;

                                                        if (isRicercaOffline || !giocoInFetching.IsDisegnato) DisegnaOggetto();

                                                        listaGiochiGratisTrovati.add(giocoInFetching);

                                                        fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                    } else {
                                                        evitaCheckIndesiderati.set(false);

                                                        wv_checkAcquistato.loadUrl(giocoInFetching.UrlGioco);
                                                    }
                                                }
                                                else {
                                                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                }
                                            }
                                        } catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }

                        );
                    }
                    else {
                        cntGiocoProcessato.getAndIncrement();

                        updateProgress(cntGiocoProcessato.get() * 100 / totGiochiDaControllare);

                        ResetFetchListaGiochi();

                        showMessage(String.format("Ricerca a pagina %1d di %2...\n(Trovati %3d - Nuovi %4d)", giocoInFetching.PaginaRicerca, totPagine, listaGiochiGratisTrovati.size(), listaGiochiGratisNonAcquistati.size()), false);

                        evitaFetchIndesiderati.set(true);

                        if (giocoInFetching.Descrizione == null || giocoInFetching.ElementoPagina == (totGiochiPerPagina - 1)) {
                            giocoInFetching = null;

                            cntPaginaProcessata.incrementAndGet();

                            SfogliaPagineOnline();
                        } else {
                            giocoInFetching = null;

                            fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                        }
                    }
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private final class CheckaAcquistatoOffline implements Runnable {
        @Override
        public synchronized void run() {
            runOnUiThread(() -> {
                try {
                    evitaCheckIndesiderati.set(true);

                    wv_checkAcquistato.evaluateJavascript(
                            "(function() {" +
                                    "   var isAcquistato = false;" +
                                    "   var spans = document.querySelectorAll('" +
                                    "       span[data-qa=\"mfeCtaMain#offer0#finalPrice\"]," +
                                    "       span[data-qa=\"mfeCtaMain#offer1#finalPrice\"]," +
                                    "       span[data-qa=\"mfeCtaMain#offer2#finalPrice\"]," +
                                    "       span[data-qa=\"mfeUpsell#productEdition0#ctaWithPrice#offer0#finalPrice\"]," +
                                    "       span[data-qa=\"mfeUpsell#productEdition1#ctaWithPrice#offer0#finalPrice\"]," +
                                    "       span[data-qa=\"mfeUpsell#productEdition2#ctaWithPrice#offer0#finalPrice\"]," +
                                    "       span[data-qa=\"mfeUpsell#productEdition3#ctaWithPrice#offer0#finalPrice\"]," +
                                    "       span[data-qa=\"mfeUpsell#productEdition4#ctaWithPrice#offer0#finalPrice\"]" +
                                    "   ');" +
                                    "   var prezzi = [];" +
                                    "   for (var cntSpan = 0; cntSpan < spans.length - 1; cntSpan++) {" +
                                    "       if (" +
                                    "           spans[cntSpan].innerText == \"Acquistato\" ||" +
                                    "           spans[cntSpan].innerText == \"Nella raccolta\" ||" +
                                    "           spans[cntSpan].innerText == \"€0,00\" ||" +
                                    "           spans[cntSpan].innerText == \"Inclusi\" ||" +
                                    "           spans[cntSpan].innerText == \"Gratis\" ||" +
                                    "           spans[cntSpan].innerText == \"Versione di prova del gioco"+
                                    "       ) {"  +
                                    "           prezzi.push(spans[cntSpan].innerText);" +
                                    "       }" +
                                    "   }" +
                                    "   return prezzi.join(';');" +
                                    "})()",
                            prezzi -> {
                                runOnUiThread(() -> {
                                    try {
                                        evitaCheckIndesiderati.set(false);

                                        if (prezzi.equals("null") && countDownCheckAcquistato.getCount() > 0) {
                                            countDownCheckAcquistato.countDown();
                                            checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                                        } else {
                                            String[] listaPrezzi = prezzi.split(";");

                                            boolean isAcquistato = false;
                                            boolean isGratis = false;

                                            for (String prezzo : listaPrezzi) {
                                                if (prezzo.equals("Acquistato") || prezzo.equals("Nella raccolta")) {
                                                    isAcquistato = true;
                                                }
                                                if (prezzo.equals("€0,00") || prezzo.equals("Inclusi") || prezzo.equals("Gratis") || prezzo.equals("Versione di prova del gioco") || giocoInFetching.IsAcquistato) {
                                                    isGratis = true;
                                                }
                                            }

                                            giocoInFetching.IsAcquistato = isAcquistato;
                                            giocoInFetching.IsGratis = isGratis;

                                            if (giocoInFetching.IsAcquistato) {
                                                listaGiochiGratisNonAcquistati.add(giocoInFetching);
                                            }

                                            if (!giocoInFetching.IsDisegnato) {
                                                DisegnaOggetto();
                                            }

                                            ResetCheckaAcquistato();

                                            evitaCheckIndesiderati.set(true);

                                            countDownCheckAcquistato = new CountDownLatch(0);
                                            fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                                        }
                                    } catch (Exception e) {
                                        showMessages(e.getMessage(), true);
                                        throw new RuntimeException(e);
                                    }
                                });
                            }
                    );
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private final class CheckaAcquistato implements Runnable {
        @Override
        public synchronized void run() {
            runOnUiThread(() -> {
                try {
                    if (giocoInFetching != null && (isRicercaOffline || !giocoInFetching.IsAcquistato)) {
                        evitaCheckIndesiderati.set(true);

                        wv_checkAcquistato.evaluateJavascript(
                                "(function() {" +
                                        "   var isAcquistato = false;" +
                                        "   var spans = document.querySelectorAll('" +
                                        "       span[data-qa=\"mfeCtaMain#offer0#finalPrice\"]," +
                                        "       span[data-qa=\"mfeCtaMain#offer1#finalPrice\"]," +
                                        "       span[data-qa=\"mfeCtaMain#offer2#finalPrice\"]," +
                                        "       span[data-qa=\"mfeUpsell#productEdition0#ctaWithPrice#offer0#finalPrice\"]," +
                                        "       span[data-qa=\"mfeUpsell#productEdition1#ctaWithPrice#offer0#finalPrice\"]," +
                                        "       span[data-qa=\"mfeUpsell#productEdition2#ctaWithPrice#offer0#finalPrice\"]," +
                                        "       span[data-qa=\"mfeUpsell#productEdition3#ctaWithPrice#offer0#finalPrice\"]," +
                                        "       span[data-qa=\"mfeUpsell#productEdition4#ctaWithPrice#offer0#finalPrice\"]" +
                                        "   ');" +
                                        "   for (var cntSpan = 0; cntSpan < spans.length - 1; cntSpan++) {" +
                                        "       if (spans[cntSpan].innerText == \"Acquistato\" || spans[cntSpan].innerText == \"Nella raccolta\") {"  +
                                        "           isAcquistato = true;" +
                                        "           break;" +
                                        "       }" +
                                        "   }" +
                                        "   return isAcquistato;" +
                                        "})()",
                                isAcquistato -> {
                                    runOnUiThread(() -> {
                                        try {
                                            evitaCheckIndesiderati.set(false);

                                            if (isAcquistato.equals("null") && countDownCheckAcquistato.getCount() > 0) {
                                                countDownCheckAcquistato.countDown();
                                                checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                                            } else {
                                                giocoInFetching.IsAcquistato = isAcquistato.equals("true");

                                                if (!giocoInFetching.IsAcquistato) {
                                                    listaGiochiGratisNonAcquistati.add(giocoInFetching);
                                                }

                                                if (!giocoInFetching.IsDisegnato) {
                                                    DisegnaOggetto();
                                                }

                                                ResetCheckaAcquistato();

                                                listaGiochiGratisTrovati.add(giocoInFetching);

                                                evitaCheckIndesiderati.set(true);

                                                countDownCheckAcquistato = new CountDownLatch(0);
                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                            }
                                        } catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                        );
                    }
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private synchronized void SfogliaPagineOnline() {
        runOnUiThread(() -> {
            try {
                if (cntPaginaProcessata.get() < totPagine) {
                    evitaFetchIndesiderati.set(false);

                    wv_fetchListaGiochi.loadUrl(listaGiochiDaFetchare.get(cntGiocoProcessato.get()).UrlPaginaRicerca);
                } else {
                    hideProgressBar();

                    showMessage(String.format("Ricerca Completata!\n(Trovati %1d - Nuovi %2d - Ultimo a pag. %2d - # %3d)", listaGiochiGratisTrovati.size(), cntPaginaProcessata.get(), cntGiocoProcessato.get()), false);

                    saveJson(GIOCHI_OFFLINE_JSON_NAME, listaGiochiGratisTrovati);

                    ClearWebView(wv_fetchListaGiochi, false);
                    DistruggiWebView(wv_checkAcquistato);

                    fetchaListaGiochiScheduledFuture.cancel(true);
                    checkaAcquistatoScheduledFuture.cancel(true);
                }
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }
        });
    }


    private synchronized void ResetLogin() {
        staLoggandosi.set(false);
        countDownLogin = new CountDownLatch(TENTATIVI_LOGIN);
        if (logginatiScheduledFuture != null) {
            logginatiScheduledFuture.cancel(true);
        }
    }

    private synchronized void ResetFetchListaGiochi() {
        staFetchandoListaGiochi.set(false);
        countDownFetch = new CountDownLatch(TENTATIVI_FETCH_LISTA_GIOCHI);
        if (fetchaListaGiochiScheduledFuture != null) {
            fetchaListaGiochiScheduledFuture.cancel(true);
        }
    }

    private synchronized void ResetCheckaAcquistato() {
        staCheckandoAcquistato.set(false);
        countDownCheckAcquistato = new CountDownLatch(TENTATIVI_CHECK_ACQUISTATO);
        if (checkaAcquistatoScheduledFuture != null) {
            checkaAcquistatoScheduledFuture.cancel(true);
        }
    }

    private synchronized void SetDocument(String url) {
        try {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)
                    .build();
            request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = okHttpClient.newCall(request).execute();
            html = response.body().string();
            document = Jsoup.parse(html, url);
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    private synchronized Document GetDocument() {
        try {
            if (document == null) {
                SetDocument(PLAYSTATION_4_GAMES_URL + giocoInFetching.PaginaRicerca);
            }
            return document;
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    private synchronized void DisegnaOggetto() {
        runOnUiThread(() -> {
            try {
                TextView txtTitoloGioco = new TextView(PS4ScrapingActivity.this);
                RelativeLayout.LayoutParams titoloGiocoRelativeLayoutParams =
                        new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                titoloGiocoRelativeLayoutParams.leftMargin = 50;
                txtTitoloGioco.setLayoutParams(titoloGiocoRelativeLayoutParams);
                txtTitoloGioco.setGravity(Gravity.CENTER);
                txtTitoloGioco.setTextColor(Color.rgb(255, 255, 255));
                txtTitoloGioco.setText(String.format("%1$s (%2$s)", giocoInFetching.Descrizione, giocoInFetching.Prezzo));
                rl_layout.addView(txtTitoloGioco);

                Button bottoneGioco = new Button(PS4ScrapingActivity.this);
                RelativeLayout.LayoutParams bottoneGiocoRelativeLayoutParams =
                        new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                bottoneGiocoRelativeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                bottoneGiocoRelativeLayoutParams.leftMargin = 410;
                bottoneGioco.setLayoutParams(bottoneGiocoRelativeLayoutParams);
                String bottoneUrlGioco = giocoInFetching.UrlGioco;
                bottoneGioco.setOnClickListener(v -> {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(bottoneUrlGioco)));
                });
                if (giocoInFetching.IsAcquistato) {
                    bottoneGioco.setText(String.format("Scaricalo a pagina %s", giocoInFetching.PaginaRicerca));
                    bottoneGioco.setTextColor(Color.rgb(255, 255, 255));
                    bottoneGioco.setBackgroundColor(Color.rgb(69, 69, 69));
                } else {
                    bottoneGioco.setText(String.format("Aggiungilo a pagina %s", giocoInFetching.PaginaRicerca));
                    bottoneGioco.setTextColor(Color.rgb(0, 0, 0));
                    bottoneGioco.setBackgroundColor(Color.rgb(69, 255, 255));
                }
                bottoneGioco.setPadding(30, 0, 30,0);
                rl_layout.addView(bottoneGioco);

                if(giocoInFetching.SrcAnteprima != null) {
                    InputStream inputStreamAnteprimaGioco = new URL(giocoInFetching.SrcAnteprima.replace("54&thumb=true", String.valueOf(256))).openStream();
                    Bitmap bitmapAnteprimaGioco = BitmapFactory.decodeStream(inputStreamAnteprimaGioco);
                    ImageView imageView = new ImageView(PS4ScrapingActivity.this);
                    imageView.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    );
                    imageView.setImageBitmap(bitmapAnteprimaGioco);
                    imageView.setOnClickListener(v -> {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(bottoneUrlGioco)));
                    });
                    rl_layout.addView(imageView);
                }
                else {
                    boolean chiappa = true;
                }

                Space spazio2 = new Space(PS4ScrapingActivity.this);
                spazio2.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                30
                        )
                );
                rl_layout.addView(spazio2);

                RelativeLayout.LayoutParams txtTimeStampRelativeLayoutParams =
                        new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                        );
                txtTimeStampRelativeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                txtTimeStampRelativeLayoutParams.leftMargin = 1024;
                TextView txtTimestamp = new TextView(PS4ScrapingActivity.this);
                txtTimestamp.setLayoutParams(txtTimeStampRelativeLayoutParams);
                txtTimestamp.setGravity(Gravity.CENTER);
                txtTimestamp.setTextColor(Color.rgb(255, 255, 255));
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String formattedDate = simpleDateFormat.format(new Date());
                txtTimestamp.setText(formattedDate);
                txtTimestamp.setTextSize(10);
                txtTimestamp.setTextColor(Color.YELLOW);
                rl_layout.addView(txtTimestamp);

                Space spazio3 = new Space(PS4ScrapingActivity.this);
                spazio3.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                30
                        )
                );
                rl_layout.addView(spazio3);

                giocoInFetching.IsDisegnato = true;
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }

        });
    }

    private synchronized void DistruggiWebView(WebView webView) {
        rl_layout.removeView(webView);
        //ClearWebView(webView, false);
        webView.destroy();
        webView = null;
    }

    private synchronized void ClearWebView(WebView webView, boolean clearCache) {
        webView.clearHistory();
        webView.clearFocus();
        webView.clearFormData();
        webView.clearCache(clearCache);
    }

    private synchronized boolean IsRicercaOffline() {
        try {
            File jsonFile = new File("/data/data/com.example.webscraping/files/" + GIOCHI_OFFLINE_JSON_NAME + ".json");
            return jsonFile.exists();
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    private synchronized int GetTotalePagine() {
        try {
            if (document == null || !document.location().equals(PLAYSTATION_4_GAMES_URL)) {
                SetDocument(PLAYSTATION_4_GAMES_URL);
            }

            return Integer.parseInt(document.select("button.psw-page-button").last().text().trim());
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    private synchronized int GetTotaleElementi() {
        try {
            if (document == null || !document.location().equals(PLAYSTATION_4_GAMES_URL)) {
                SetDocument(PLAYSTATION_4_GAMES_URL);
            }

            return document.select("li[class^=psw-l-w-1]").size();
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    public synchronized List<OggettoJson> CreaListaOggettiJson(int totPagine, int totElementi) {
        List<OggettoJson> ListaOggetti = new ArrayList<>();

        for (int page = 1; page <= totPagine; page++) {
            for (int element = 0; element < totElementi; element++) {
                OggettoJson oggettoJson = new OggettoJson();

                oggettoJson.TxtDescrizioneSelector = String.format("span[data-qa='ems-sdk-grid#productTile%s#product-name']", element);
                oggettoJson.TxtPrezzoSelector = String.format("span[data-qa='ems-sdk-grid#productTile%s#price#display-price']", element);
                oggettoJson.ImgAnteprimaSelector = String.format("img[data-qa='ems-sdk-grid#productTile%s#game-art#image#preview']", element);
                oggettoJson.AnchorGiocoSelector = String.format("div[data-qa='ems-sdk-grid#productTile%s']>a", element);
                oggettoJson.UrlPaginaRicerca = PLAYSTATION_4_GAMES_URL + page;
                oggettoJson.PaginaRicerca = page;
                oggettoJson.ElementoPagina = element;
                oggettoJson.IsAcquistato = false;
                oggettoJson.IsDisegnato = false;
                oggettoJson.IsGratis = false;

                ListaOggetti.add(oggettoJson);
            }
        }

        return ListaOggetti;
    }

    public synchronized String getJson(String jsonName) {
        String listaGiochiJsonFullName = String.format("%s.json", jsonName);
        FileInputStream jsonInputStream;
        InputStreamReader jsonInputStreamReader;
        BufferedReader jsonBufferedReader;

        try {
            jsonInputStream = openFileInput(listaGiochiJsonFullName);
            jsonInputStreamReader = new InputStreamReader(jsonInputStream);
            jsonBufferedReader = new BufferedReader(jsonInputStreamReader);
            String jsonRow;
            StringBuilder jsonData = new StringBuilder();
            while ((jsonRow = jsonBufferedReader.readLine()) != null) {
                jsonData.append(jsonRow);
            }
            jsonBufferedReader.close();
            jsonInputStreamReader.close();
            jsonInputStream.close();
            return jsonData.toString();
        } catch (FileNotFoundException fnfe) {
            return "";
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    public synchronized void saveJson(String jsonName, List<OggettoJson> ListaOggetti) {
        String listaGiochiJsonFullName = String.format("%s.json", jsonName);
        FileOutputStream jsonOutputStream;
        OutputStreamWriter jsonOutputStreamWriter;

        try {
            jsonOutputStream = openFileOutput(listaGiochiJsonFullName, MODE_PRIVATE);
            jsonOutputStreamWriter = new OutputStreamWriter(jsonOutputStream);
            jsonOutputStreamWriter.write(gson.toJson(ListaOggetti));
            jsonOutputStreamWriter.flush();
            jsonOutputStream.close();
            jsonOutputStreamWriter.close();
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }


    private synchronized void updateProgress(int progress) {
        runOnUiThread(() -> {
            pb_progressBar.setVisibility(View.VISIBLE);
            pb_progressBar.setProgress(progress);
        });
    }

    private synchronized void hideProgressBar() {
        runOnUiThread(() -> pb_progressBar.setVisibility(ProgressBar.GONE));
    }

    private synchronized void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(
                        PS4ScrapingActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private synchronized void showMessage(String message, boolean isErrore) {
        runOnUiThread(() -> {
            txt_msg.setTextColor(isErrore ? COLORE_ERRORE : COLORE_INFO);
            txt_msg.setText(message);
        });
    }

    private synchronized void showMessages(String message, boolean isErrore) {
        showToast(message);
        showMessage(message, isErrore);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
    }
}