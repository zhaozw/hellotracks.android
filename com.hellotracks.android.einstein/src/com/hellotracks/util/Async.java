package com.hellotracks.util;


import com.hellotracks.Logger;

import android.app.Activity;
import de.greenrobot.event.util.AsyncExecutor;
import de.greenrobot.event.util.AsyncExecutor.RunnableEx;

public class Async {
    
    public static abstract class Task<T> {
        
        public Task(Activity activity) {
            asyncAndPost(activity, new RunAsync<T>() {

                public T async() {
                   return Task.this.async();
                }
                
            }, new RunPost<T>() {

                public void post(T t) {
                    Task.this.post(t);
                }
                
            });
        }
        
        public abstract T async();
        
        public abstract void post(T result);
    }

    private static interface RunAsync<T> {
        public T async();
    }

    private static interface RunPost<T> {
        public void post(T t);
    }

    private static <T> void asyncAndPost(final Activity activity, final RunAsync<T> doIt, final RunPost<T> postIt) {
        AsyncExecutor.create().execute(new RunnableEx() {

            public void run() throws Exception {
                final T t = doIt.async();
                activity.runOnUiThread(new Runnable() {

                    public void run() {
                        try {
                            postIt.post(t);
                        } catch (Exception exc) {
                            Logger.w(exc);
                        }
                    }

                });
            }
        });

    }
}
