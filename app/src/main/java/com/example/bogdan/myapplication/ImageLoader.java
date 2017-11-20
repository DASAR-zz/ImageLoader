package com.example.bogdan.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private final Context ctx;

    private Object lock = new Object();

    private ExecutorService service = Executors.newFixedThreadPool(8);

    private HashMap<String, List<SoftReference<ImageView>>> subs = new HashMap<>();

    public ImageLoader(Context ctx) {
        this.ctx = ctx;
    }

    @Nullable
    private Bitmap load(String url) throws IOException {

        URL urll = new URL(url);

        HttpURLConnection urlConnection = (HttpURLConnection) urll.openConnection();

        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            return BitmapFactory.decodeStream(in);
        } finally {
            urlConnection.disconnect();
        }

    }

    public void load(final String url, final ImageView target) {

        synchronized (lock) {
            List<SoftReference<ImageView>> s = subs.get(url);

            if (s != null) {
                s.add(new SoftReference<>(target));
                // Wait until loaded
                return;
            } else {
                s = new ArrayList<>();

                s.add(new SoftReference<>(target));

                subs.put(url, s);
            }
        }

        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = load(url);

                    // FIXME: if image is NULL notify load failed

                    synchronized (lock) {
                        List<SoftReference<ImageView>> list = subs.get(url);

                        // Notify all subscribers
                        for (SoftReference<ImageView> s : list) {
                            final ImageView view = s.get();

                            // TODO: Scale bitmap to required size

                            if (view != null) {
                                view.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.setImageBitmap(bitmap);
                                    }
                                });
                            }
                        }

                        subs.remove(url);
                    }

                    // TODO: Put image into cache

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

}