package com.coinbase.android;

import java.io.IOException;

import org.json.JSONException;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.coinbase.api.RpcManager;

public class UpdateWidgetBalanceService extends Service {

  public static interface WidgetUpdater {
    public void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, String balance);
  }

  public static String EXTRA_WIDGET_ID = "widget_id";
  public static String EXTRA_UPDATER_CLASS = "updater_class";



  @Override
  public int onStartCommand(Intent intent, int flags, final int startId) {

    final int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1);
    final Class<?> updaterClass = (Class<?>) intent.getSerializableExtra(EXTRA_UPDATER_CLASS);

    new Thread(new Runnable() {
      public void run() {

        try {

          int accountId = PreferenceManager.getDefaultSharedPreferences(UpdateWidgetBalanceService.this).getInt(
              String.format(Constants.KEY_WIDGET_ACCOUNT, widgetId), -1);


          // Step 1: Update widget without balance
          AppWidgetManager manager = AppWidgetManager.getInstance(UpdateWidgetBalanceService.this);
          WidgetUpdater updater = (WidgetUpdater) updaterClass.newInstance();
          updater.updateWidget(UpdateWidgetBalanceService.this, manager, widgetId, null);

          // Step 2: Fetch balance
          String balance;
          if(accountId == -1) {
            balance = "";
          } else {
            Log.i("Coinbase", "Service fetching balance... [" + updaterClass.getSimpleName() + "]");
            balance = RpcManager.getInstance().callGetOverrideAccount(
                UpdateWidgetBalanceService.this, "account/balance", accountId).getString("amount");
            balance = Utils.formatCurrencyAmount(balance);
          }

          // Step 3: Update widget
          updater.updateWidget(UpdateWidgetBalanceService.this, manager, widgetId, balance);

        } catch(JSONException e) {
          e.printStackTrace();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

        stopSelf(startId);
      }
    }).start();

    return Service.START_REDELIVER_INTENT;
  }



  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

}
