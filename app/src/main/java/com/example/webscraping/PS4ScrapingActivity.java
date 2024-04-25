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
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PS4ScrapingActivity extends AppCompatActivity {
    private ScheduledExecutorService scheduledExecutorService;

    private Future<?> logginatiScheduledFuture;
    private Future<?> fetchaListaGiochiScheduledFuture;
    private Future<?> checkaAcquistatoScheduledFuture;

    private LinearLayout ll_linearLayout;
    private ProgressBar pb_progressBar;
    private TextView txt_msg;
    private WebView wv_login;
    private WebView wv_fetchListaGiochi;
    private WebView wv_checkAcquistato;

    private Boolean staLoggandosi;
    private Boolean staInserendoEmail;
    private Boolean staInserendoPassword;
    private Boolean staControllandoLogin;
    private Boolean isLoggato;

    private Boolean staFetchandoListaGiochi;
    private Boolean staLeggendoDescrizioneDaRicerca;
    private Boolean staLeggendoPrezzoDaRicerca;
    private Boolean staLeggendoPrezzoDaDettaglio;
    private Boolean isDescrizioneLetta;
    private Boolean isPrezzoLetto;
    private Boolean evitaFetchIndesiderate;

    private Boolean staCheckandoAcquistato;
    private Boolean staLeggendoPrezzoFinale;
    private Boolean evitaCheckIndesiderati;

    private Boolean isRicercaOffline;


    private CountDownLatch countDownLogin;
    private CountDownLatch countDownFetch;
    private CountDownLatch countDownCheckAcquistato;

    private int totPagine;
    private int totGiochiPerPagina;
    private int totGiochiDaControllare;
    private int cntPaginaProcessata;
    private int cntGiocoProcessato;

    private Logginati logginati;
    private FetchaListaGiochiOnline fetchaListaGiochiOnline;
    private FetchaListaGiochiOffline fetchaListaGiochiOffline;
    private CheckaAcquistato checkaAcquistato;

    private Gson gson;

    private List<OggettoJson> listaGiochiDaFetchare;
    private List<OggettoJson> listaGiochiTrovati;
    private OggettoJson giocoInFetching;

    private OkHttpClient okHttpClient;
    private Request request;
    private Response response;
    private String html;
    private Document document;


    private static final  int CONSTRAINT_LAYOUT_ID = R.layout.activity_ps4;
    private static final int WEB_VIEW_LOGIN_ID = R.id.wv_login;
    private static final int WEB_VIEW_FETCH_LISTA_GIOCHI_ID = R.id.wv_fetchListaGiochi;
    private static final int WEB_VIEW_CHECK_ACQUISTATO_ID = R.id.wv_checkAcquistato;

    private static final int TENTATIVI_LOGIN = 3;
    private static final int TENTATIVI_FETCH_LISTA_GIOCHI = 3;
    private static final int TENTATIVI_CHECK_ACQUISTATO = 3;

    private static final int INTERVALLO_TENTATIVO_LOGIN = 1000 * 3;
    private static final int INTERVALLO_TENTATIVO_FETCH_ELEMENTI = 1000 * 3;
    private static final int INTERVALLO_TENTATIVO_CHECK_ACQUISTATO = 1000 * 3;
    private static final int INTERVALLO_THREAD = 10;

    private static final int COLORE_INFO = Color.rgb(0, 255, 255);
    private static final int COLORE_ERRORE = Color.rgb(255, 0, 0);

    private static final String GIOCHI_OFFLINE_JSON_NAME = "prezzi-offline-ps4";
    private static final String GIOCHI_ONLINE_JSON_NAME = "prezzi-online-ps4";
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

        ll_linearLayout = findViewById(R.id.layout);
        pb_progressBar = findViewById(R.id.progressBar);
        txt_msg = findViewById(R.id.txt_msg);

        gson = new Gson();

        cntPaginaProcessata = 0;
        cntGiocoProcessato = 0;

        System.setProperty("net.dns1", "8.8.8.8");
        System.setProperty("net.dns2", "8.8.4.4");

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        logginati = new Logginati();
        fetchaListaGiochiOnline = new FetchaListaGiochiOnline();
        fetchaListaGiochiOffline = new FetchaListaGiochiOffline();
        checkaAcquistato = new CheckaAcquistato();

        ResetLogin();
        ResetFetchListaGiochi();
        ResetCheckAcquistato();

        CaricaFinestraLogin();
        CaricaFinestraListaGiochi();
        CaricaFinestraCheckAcquisto();

        listaGiochiTrovati = new ArrayList<OggettoJson>();

        isRicercaOffline = IsRicercaOffline();

        isLoggato = false;

        wv_login.loadUrl(PLAYSTATION_MAP_URL);
    }


    @SuppressLint("SdCardPath")
    private void InizializzaListaGiochi() {
        runOnUiThread(() -> {
            try {
                showMessages("Caricamento database in corso...", false);

                if (isRicercaOffline) {
                    listaGiochiDaFetchare = gson.fromJson(getJson(GIOCHI_OFFLINE_JSON_NAME), new TypeToken<List<OggettoJson>>() {}.getType());

                    totGiochiDaControllare = listaGiochiDaFetchare.size();

                    showMessages("Inizio processo in corso...", false);

                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                } else {
                    totPagine = GetTotalePagine();
                    totGiochiPerPagina = GetTotaleElementi();
                    listaGiochiDaFetchare = new ArrayList<>();

                    listaGiochiDaFetchare = gson.fromJson(getJson(GIOCHI_ONLINE_JSON_NAME), new TypeToken<List<OggettoJson>>() {}.getType());
                    if (listaGiochiDaFetchare == null) {
                        showMessages("Database vuoto! Creazione database in corso...", false);

                        listaGiochiDaFetchare = CreaListaOggettiJson(totPagine, totGiochiPerPagina);
                        saveJson(GIOCHI_ONLINE_JSON_NAME, listaGiochiDaFetchare);
                    }

                    totGiochiDaControllare = listaGiochiDaFetchare.size();

                    showMessages("Inizio processo in corso...", false);

                    SfogliaPaginaOnline();
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
        //ClearWebView(webView, false);
        webView.setWebViewClient(new WebViewClientComune());
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(PS4ScrapingActivity.this));
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setSafeBrowsingEnabled(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
    }


    private class WebViewClientComune extends WebViewClient {
        @Override
        public void onPageFinished(WebView webView, String url) {
            try {
                super.onPageFinished(webView, url);
                if (webView.getId() == WEB_VIEW_LOGIN_ID && !staLoggandosi) {
                    logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                } else if (webView.getId() == WEB_VIEW_FETCH_LISTA_GIOCHI_ID && !staFetchandoListaGiochi && !evitaFetchIndesiderate) {
                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                } else if (webView.getId() == WEB_VIEW_CHECK_ACQUISTATO_ID && !staCheckandoAcquistato && !evitaCheckIndesiderati) {
                    checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
            super.onReceivedError(webView, webResourceRequest, webResourceError);
            if (
                    !webResourceError.getDescription().equals("net::ERR_TIMED_OUT") &&
                            !webResourceError.getDescription().equals("net::ERR_SOCKET_NOT_CONNECTED") &&
                            !webResourceError.getDescription().equals("net::ERR_FAILED") &&
                            !webResourceError.getDescription().equals("net::ERR_NAME_NOT_RESOLVED")
            ) {
                showMessages(String.valueOf(webResourceError.getDescription()), true);
            }
        }

        @Override
        public void onReceivedHttpError(WebView webView, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
            super.onReceivedHttpError(webView, webResourceRequest, webResourceResponse);
            //showMessages(String.valueOf(webResourceResponse.getStatusCode()), true);
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            super.onReceivedSslError(webView, sslErrorHandler, sslError);
            sslErrorHandler.proceed();
        }
    }

    private final class Logginati implements Runnable {
        @Override
        public void run() {
            runOnUiThread(() -> {
                try {
                    staLoggandosi = true;
                    if (staInserendoEmail || staInserendoPassword || staControllandoLogin) {
                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                    } else if (!isLoggato && countDownLogin.getCount() > 0) {
                        String currentUrl = wv_login.getUrl();

                        if (currentUrl.contains(PLAYSTATION_ACCOUNT_URL)) {
                            if (currentUrl.contains("#/signin/input/password")) {
                                wv_login.evaluateJavascript(
                                        "document.getElementById('signin-password-input-password').value",
                                        password -> {
                                            runOnUiThread(() -> {
                                                try {
                                                    staInserendoPassword = false;
                                                    staLoggandosi = false;

                                                    updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                    if (!password.equals("null")) {
                                                        wv_login.evaluateJavascript("document.getElementById('signin-password-input-password').focus();", null);
                                                        wv_login.evaluateJavascript("document.getElementById('signin-password-input-password').value = 'Tenacious.1990!_';", null);

                                                        countDownLogin = new CountDownLatch(0);
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                    } else {
                                                        countDownLogin.countDown();
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                    }
                                                } catch (Exception e) {
                                                    showMessages(e.getMessage(), true);
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        }
                                );

                                staInserendoPassword = true;
                            } else if (currentUrl.contains("sonyacct/signin") || currentUrl.contains("#/signin/input/id")) {
                                wv_login.evaluateJavascript(
                                        "document.getElementById('signin-entrance-input-signinId').value",
                                        email -> {
                                            runOnUiThread(() -> {
                                                try {
                                                    staInserendoEmail = false;
                                                    staLoggandosi = false;

                                                    updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                    if (!email.equals("null")) {
                                                        wv_login.evaluateJavascript("document.getElementById('signin-entrance-input-signinId').focus();", null);
                                                        wv_login.evaluateJavascript("document.getElementById('signin-entrance-input-signinId').value = 'ferrari.90@hotmail.it_';", null);

                                                        countDownLogin = new CountDownLatch(0);
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                    } else {
                                                        countDownLogin.countDown();
                                                        logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                    }
                                                } catch (Exception e) {
                                                    showMessages(e.getMessage(), true);
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        }
                                );

                                staInserendoEmail = true;
                            }
                        } else {
                            wv_login.evaluateJavascript(
                                    "document.querySelector('[data-qa=\"web-toolbar#profile-container\"]');",
                                    divLoggato -> {
                                        runOnUiThread(() -> {
                                            try {
                                                staControllandoLogin = false;
                                                staLoggandosi = false;

                                                updateProgress((int) countDownLogin.getCount() * 100 / TENTATIVI_LOGIN);

                                                if (!divLoggato.equals("null")) {
                                                    countDownLogin = new CountDownLatch(0);

                                                    isLoggato = true;

                                                    logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                                } else {
                                                    countDownLogin.countDown();
                                                    logginatiScheduledFuture = scheduledExecutorService.schedule(logginati, INTERVALLO_TENTATIVO_LOGIN, TimeUnit.MILLISECONDS);
                                                }
                                            } catch (Exception e) {
                                                showMessages(e.getMessage(), true);
                                                throw new RuntimeException(e);
                                            }
                                        });
                                    }
                            );

                            staControllandoLogin = true;
                        }
                    } else {
                        hideProgressBar();

                        if (isLoggato) {
                            showMessages("Login effettuato!", false);

                            DistruggiWebView(wv_login);

                            InizializzaListaGiochi();
                        } else {
                            showMessages("Si prega di fare la login.", false);

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

    private void SfogliaPaginaOnline() {
        runOnUiThread(() -> {
            try {
                if (cntPaginaProcessata < totPagine) {
                    giocoInFetching = null;

                    ResetFetchListaGiochi();

                    wv_fetchListaGiochi.loadUrl(listaGiochiDaFetchare.get(cntGiocoProcessato).UrlPaginaRicerca);
                } else {
                    hideProgressBar();

                    showMessage(String.format("Ricerca Completata! (Trovati %1$s) - Ultimo gioco = Pag. %2$s / El. %3$s", listaGiochiTrovati.size(), cntPaginaProcessata, cntGiocoProcessato), false);

                    ClearWebView(wv_fetchListaGiochi, false);
                    DistruggiWebView(wv_checkAcquistato);

                    fetchaListaGiochiScheduledFuture.cancel(true);
                    checkaAcquistatoScheduledFuture.cancel(true);

                    saveJson(GIOCHI_OFFLINE_JSON_NAME, listaGiochiTrovati);
                }
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }
        });
    }

    private final class FetchaListaGiochiOffline implements Runnable {
        @Override
        public void run() {
            runOnUiThread(() -> {
                if (staLeggendoPrezzoDaDettaglio) {
                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                }
                else if (cntGiocoProcessato < totGiochiDaControllare) {
                    giocoInFetching = listaGiochiDaFetchare.get(cntGiocoProcessato);
                    cntGiocoProcessato++;
                    updateProgress(cntGiocoProcessato * 100 / totGiochiDaControllare);

                    showMessage(String.format("Check acquisto %1$s (Gioco %2$s di %3$s)", giocoInFetching.Descrizione, cntGiocoProcessato, listaGiochiDaFetchare.size()), false);

                    if (!giocoInFetching.IsAcquistato) {
                        staLeggendoPrezzoDaDettaglio = true;
                        evitaCheckIndesiderati = false;
                        wv_checkAcquistato.loadUrl(giocoInFetching.UrlGioco);

                        fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                    }
                    else {
                        DisegnaOggetto();

                        fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOffline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS);
                    }
                } else {
                    txt_msg.setTextColor(Color.rgb(255, 255, 255));
                    txt_msg.setText(String.format("Ecco la lista dei giochi gratis. (Trovati %s)", cntGiocoProcessato));

                    hideProgressBar();

                    showToast("Ecco la lista dei giochi gratis.");
                }
            });
        }
    }

    private final class FetchaListaGiochiOnline implements Runnable {
        @Override
        public void run() {
            runOnUiThread(() -> {
                try {
                    staFetchandoListaGiochi = true;
                    if (staLeggendoDescrizioneDaRicerca || staLeggendoPrezzoDaRicerca || staLeggendoPrezzoDaDettaglio || evitaFetchIndesiderate) {
                        fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_TENTATIVO_FETCH_ELEMENTI, TimeUnit.MILLISECONDS);
                    } else if (!isDescrizioneLetta && countDownFetch.getCount() > 0) {
                        if (giocoInFetching == null) {
                            giocoInFetching = listaGiochiDaFetchare.get(cntGiocoProcessato);
                        }

                        wv_fetchListaGiochi.evaluateJavascript(
                                "document.querySelector(\"" + giocoInFetching.TxtDescrizioneSelector + "\").innerText",
                                descrizione -> {
                                    runOnUiThread(() -> {
                                        try {
                                            staLeggendoDescrizioneDaRicerca = false;

                                            if (!descrizione.equals("null")) {
                                                giocoInFetching.Descrizione = descrizione.replace("\"", "");

                                                isDescrizioneLetta = true;

                                                countDownFetch = new CountDownLatch(TENTATIVI_FETCH_LISTA_GIOCHI);

                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                            } else {
                                                countDownFetch.countDown();
                                                fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_TENTATIVO_FETCH_ELEMENTI, TimeUnit.MILLISECONDS);
                                            }
                                        } catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                        );

                        staLeggendoDescrizioneDaRicerca = true;
                    } else if (isDescrizioneLetta && !isPrezzoLetto) {
                        wv_fetchListaGiochi.evaluateJavascript(
                                "document.querySelector(\"" + giocoInFetching.TxtPrezzoSelector + "\").innerText",
                                prezzo -> {
                                    runOnUiThread(() -> {
                                        try {
                                            if (!prezzo.equals("null")) {
                                                SettaProprietàComuniGioco(prezzo.replace("\"", ""));
                                            } else if (countDownFetch.getCount() > 0) {
                                                countDownFetch.countDown();
                                            }
                                            else {
                                                SettaProprietàComuniGioco("?");
                                            }

                                            staLeggendoPrezzoDaRicerca = false;
                                            staCheckandoAcquistato = false;

                                            updateProgress((int) countDownCheckAcquistato.getCount() * 100 / TENTATIVI_CHECK_ACQUISTATO);

                                            fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                        }
                                        catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                        );

                        staLeggendoPrezzoDaRicerca = true;
                    } else {
                        evitaFetchIndesiderate = true;

                        if (giocoInFetching.IsGratis) {
                            listaGiochiTrovati.add(giocoInFetching);
                        }

                        cntGiocoProcessato++;

                        showMessage(String.format("Ricerca a pagina %1$s... (Trovati %2$s)", giocoInFetching.PaginaRicerca, listaGiochiTrovati.size()), false);

                        updateProgress(cntGiocoProcessato * 100 / totGiochiDaControllare);
                        if (!isDescrizioneLetta || giocoInFetching.ElementoPagina == (totGiochiPerPagina - 1)) {
                            //ClearWebView(wv_fetchListaGiochi);

                            cntPaginaProcessata++;

                            SfogliaPaginaOnline();
                        } else {
                            giocoInFetching = null;

                            ResetFetchListaGiochi();

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

    private final class CheckaAcquistato implements Runnable {
        @Override
        public void run() {
            runOnUiThread(() -> {
                try {
                    staCheckandoAcquistato = true;
                    if (staLeggendoPrezzoFinale || evitaCheckIndesiderati) {
                        checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                    } else if (giocoInFetching != null && (isRicercaOffline || !giocoInFetching.IsAcquistato) && countDownCheckAcquistato.getCount() > 0) {
                        String jsCheckAcquistato =
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
                            "})()";
                        wv_checkAcquistato.evaluateJavascript(
                                jsCheckAcquistato,
                                isAcquistato -> {
                                    runOnUiThread(() -> {
                                        try {
                                            staLeggendoPrezzoFinale = false;

                                            updateProgress((int) countDownCheckAcquistato.getCount() * 100 / TENTATIVI_CHECK_ACQUISTATO);

                                            if (!isAcquistato.equals("null") && isAcquistato.equals("true")) {
                                                countDownCheckAcquistato = new CountDownLatch(0);
                                                giocoInFetching.IsAcquistato = true;
                                                checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                            } else if(countDownCheckAcquistato.getCount() > 0) {
                                                countDownCheckAcquistato.countDown();
                                                checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                                            }
                                            else {
                                                giocoInFetching.IsAcquistato = false;
                                                checkaAcquistatoScheduledFuture = scheduledExecutorService.schedule(checkaAcquistato, INTERVALLO_THREAD, TimeUnit.MILLISECONDS); //submit
                                            }
                                        } catch (Exception e) {
                                            showMessages(e.getMessage(), true);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                        );

                        staLeggendoPrezzoFinale = true;
                    } else {
                        staLeggendoPrezzoDaDettaglio = false;
                        evitaCheckIndesiderati = true;

                        if (isRicercaOffline || !giocoInFetching.IsDisegnato) DisegnaOggetto();

                        //ClearWebView(wv_checkAcquistato);

                        ResetCheckAcquistato();
                    }
                } catch (Exception e) {
                    showMessages(e.getMessage(), true);
                    throw new RuntimeException(e);
                }
            });
        }
    }


    private void ResetLogin() {
        countDownLogin = new CountDownLatch(TENTATIVI_LOGIN);
        staLoggandosi = false;
        staInserendoEmail = false;
        staInserendoPassword = false;
        staControllandoLogin = false;
        if (logginatiScheduledFuture != null) logginatiScheduledFuture.cancel(true);
    }

    private void ResetFetchListaGiochi() {
        countDownFetch = new CountDownLatch(TENTATIVI_FETCH_LISTA_GIOCHI);
        staFetchandoListaGiochi = false;
        staLeggendoDescrizioneDaRicerca = false;
        staLeggendoPrezzoDaRicerca = false;
        staLeggendoPrezzoDaDettaglio = false;
        isDescrizioneLetta = false;
        isPrezzoLetto = false;
        evitaFetchIndesiderate = false;
        if (fetchaListaGiochiScheduledFuture != null) fetchaListaGiochiScheduledFuture.cancel(true);
    }

    private void ResetCheckAcquistato() {
        countDownCheckAcquistato = new CountDownLatch(TENTATIVI_CHECK_ACQUISTATO);
        staCheckandoAcquistato = false;
        staLeggendoPrezzoFinale = false;
        if (checkaAcquistatoScheduledFuture != null) checkaAcquistatoScheduledFuture.cancel(true);
    }


    private void SetDocument(String url) {
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

    private Document GetDocument() {
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

    private void DisegnaOggetto() {
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
                ll_linearLayout.addView(txtTitoloGioco);

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
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(bottoneUrlGioco))
                    );
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
                ll_linearLayout.addView(bottoneGioco);

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
                ll_linearLayout.addView(imageView);

                Space spazio2 = new Space(PS4ScrapingActivity.this);
                spazio2.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                30
                        )
                );
                ll_linearLayout.addView(spazio2);

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
                ll_linearLayout.addView(txtTimestamp);

                Space spazio3 = new Space(PS4ScrapingActivity.this);
                spazio3.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                30
                        )
                );
                ll_linearLayout.addView(spazio3);

                giocoInFetching.IsDisegnato = true;
            } catch (Exception e) {
                showMessages(e.getMessage(), true);
                throw new RuntimeException(e);
            }

        });
    }

    private void SettaProprietàComuniGioco(String prezzo) {
        isPrezzoLetto = true;

        if (Arrays.asList(new String[]{"€0,00", "Inclusi", "Gratis", "Acquistato", "Nella raccolta", "Versione di prova del gioco", "?"}).contains(prezzo)) {
            giocoInFetching.Prezzo = prezzo.replace("\"", "");

            if (!document.location().equals(giocoInFetching.UrlPaginaRicerca)) {
                SetDocument(giocoInFetching.UrlPaginaRicerca);
            }

            giocoInFetching.UrlGioco = PLAYSTATION_STORE_URL + document.select(giocoInFetching.AnchorGiocoSelector).first().attr("href");
            giocoInFetching.SrcAnteprima = document.select(giocoInFetching.ImgAnteprimaSelector).first().attr("src");

            if (!prezzo.equals("?")) {
                giocoInFetching.IsGratis = true;

                if (Arrays.asList(new String[]{"Acquistato", "Nella raccolta"}).contains(giocoInFetching.Prezzo)) {
                    giocoInFetching.IsAcquistato = true;

                    if (isRicercaOffline || !giocoInFetching.IsDisegnato) DisegnaOggetto();
                } else {
                    staLeggendoPrezzoDaDettaglio = true;
                    evitaCheckIndesiderati = false;

                    wv_checkAcquistato.loadUrl(giocoInFetching.UrlGioco);

                    fetchaListaGiochiScheduledFuture = scheduledExecutorService.schedule(fetchaListaGiochiOnline, INTERVALLO_TENTATIVO_CHECK_ACQUISTATO, TimeUnit.MILLISECONDS);
                }
            }
        }
    }


    private void DistruggiWebView(WebView webView) {
        ll_linearLayout.removeView(webView);
        ClearWebView(webView, false);
        webView.destroy();
        webView = null;
    }

    private void ClearWebView(WebView webView, Boolean clearCache) {
        webView.clearHistory();
        webView.clearFocus();
        webView.clearFormData();
        webView.clearCache(clearCache);
    }

    private Boolean IsRicercaOffline() {
        try {
            File jsonFile = new File("/data/data/com.example.webscraping/files/" + GIOCHI_OFFLINE_JSON_NAME + ".json");
            return jsonFile.exists();
        } catch (Exception e) {
            showMessages(e.getMessage(), true);
            throw new RuntimeException(e);
        }
    }

    private Integer GetTotalePagine() {
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

    private Integer GetTotaleElementi() {
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

    public List<OggettoJson> CreaListaOggettiJson(int totPagine, int totElementi) {
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

    public String getJson(String jsonName) {
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

    public void saveJson(String jsonName, List<OggettoJson> ListaOggetti) {
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


    private void updateProgress(int progress) {
        runOnUiThread(() -> {
            pb_progressBar.setVisibility(View.VISIBLE);
            pb_progressBar.setProgress(progress);
        });
    }

    private void hideProgressBar() {
        runOnUiThread(() -> pb_progressBar.setVisibility(ProgressBar.GONE));
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(
                        PS4ScrapingActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void showMessage(String message, Boolean isErrore) {
        runOnUiThread(() -> {
            txt_msg.setTextColor(isErrore ? COLORE_ERRORE : COLORE_INFO);
            txt_msg.setText(message);
        });
    }

    private void showMessages(String message, Boolean isErrore) {
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