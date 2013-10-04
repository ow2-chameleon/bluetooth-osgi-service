/*
 * Copyright 2013 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ow2.chameleon.bluetooth.discovery;

import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This static class manages the Bluetooth action.
 * It ensures that only one bluetooth action is executed at a time.
 * <p/>
 * Bluetooth operation are submitted to this class which executed them when a free slot if available.
 */
public class BluetoothThreadManager {

    /**
     * Customization of the thread factory to avoid letting a uncaught exception
     * blowing up.
     * The exception is just logged.
     */
    private static final ThreadFactory m_factory = new ThreadFactory() {

        @Override
        public Thread newThread(final Runnable target) {
            final Thread thread = new Thread(target);
            LoggerFactory.getLogger(BluetoothThreadManager.class).debug("Creating new worker thread");
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LoggerFactory.getLogger(BluetoothThreadManager.class).error("Uncaught Exception thrown by " + target, e);
                }

            });
            return thread;
        }

    };

    /**
     * The thread pool executing the action.
     * the thread pool size is limited to 1.
     */
    private static ScheduledThreadPoolExecutor m_pool = new ScheduledThreadPoolExecutor(1, m_factory);

    /**
     * Schedules a periodic job such as the Device Inquiry
     *
     * @param runnable the job
     * @param period   the period
     */
    public static void scheduleJob(Runnable runnable, int period) {
        try {
            LoggerFactory.getLogger(BluetoothThreadManager.class).info("Submitting periodic task " + runnable);
            m_pool.scheduleWithFixedDelay(runnable, 0, period, TimeUnit.SECONDS);
            LoggerFactory.getLogger(BluetoothThreadManager.class).info(runnable
                    + " submitted - waiting queue " + m_pool.getQueue().size());
        } catch (RejectedExecutionException e) {
            LoggerFactory.getLogger(BluetoothThreadManager.class).error("Cannot submit task", e);
        }
    }

    /**
     * Submits a one-shot job that does not return a result such as a Service Inquiry.
     * the job will be executed when possible.
     *
     * @param runnable the job
     */
    public static void submit(Runnable runnable) {
        try {
            LoggerFactory.getLogger(BluetoothThreadManager.class).info("Submitting one-shot task " + runnable);
            m_pool.submit(runnable);
            LoggerFactory.getLogger(BluetoothThreadManager.class).info(runnable
                    + " submitted - waiting queue " + m_pool.getQueue().size());
        } catch (RejectedExecutionException e) {
            LoggerFactory.getLogger(BluetoothThreadManager.class).error("Cannot submit task", e);
        }
    }

    /**
     * Submits a one-shot job returning a result. A Future object is returned to get the result.
     * It is strongly recommended to <strong>NOT</strong> interrupt the computation.
     *
     * @param task the task
     * @param <V>  the return type
     * @return a Future object to retrieve the result
     */
    public static <V> Future<V> submit(Callable<V> task) {
        try {
            LoggerFactory.getLogger(BluetoothThreadManager.class).info("Submitting one-shot task " + task);
            Future<V> future = m_pool.submit(task);
            LoggerFactory.getLogger(BluetoothThreadManager.class).info(task
                    + " submitted - waiting queue " + m_pool.getQueue().size());
            return future;
        } catch (RejectedExecutionException e) {
            LoggerFactory.getLogger(BluetoothThreadManager.class).error("Cannot submit task", e);
            return null;
        }
    }

    /**
     * Shutdowns the pool.
     * No task can be submitted once this method is called.
     */
    public static void stopScheduler() {
        LoggerFactory.getLogger(BluetoothThreadManager.class).info("Shutdown scheduler");
        try {
            m_pool.shutdownNow();
        } catch (Throwable e) {
            // Ignore.
            LoggerFactory.getLogger(BluetoothThreadManager.class).info("Exception during shutdown : ", e);
        }
    }

}
