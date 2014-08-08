package com.coinbase.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.coinbase.android.task.ApiTask;
import com.coinbase.android.Constants;
import com.coinbase.android.event.UserDataUpdatedEvent;
import com.coinbase.api.entity.User;
import com.google.inject.Inject;
import com.squareup.otto.Bus;

class RefreshSettingsTask extends ApiTask<User> {

  @Inject
  protected Bus mBus;

  public RefreshSettingsTask(Context context) {
    super(context);
  }

  @Override
  public User call() throws Exception {
    return getClient().getUser();
  }

  @Override
  public void onSuccess(User user) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    int activeAccount = mLoginManager.getActiveAccount();

    SharedPreferences.Editor e = prefs.edit();

    e.putString(String.format(Constants.KEY_ACCOUNT_NAME, activeAccount), user.getEmail());
    e.putString(String.format(Constants.KEY_ACCOUNT_NATIVE_CURRENCY, activeAccount), user.getNativeCurrency().getCurrencyCode());
    e.putString(String.format(Constants.KEY_ACCOUNT_FULL_NAME, activeAccount), user.getName());
    e.putString(String.format(Constants.KEY_ACCOUNT_TIME_ZONE, activeAccount), user.getTimeZone());
    e.putString(String.format(Constants.KEY_ACCOUNT_LIMIT_BUY, activeAccount), user.getBuyLimit().getAmount().toString());
    e.putString(String.format(Constants.KEY_ACCOUNT_LIMIT_SELL, activeAccount), user.getSellLimit().getAmount().toString());
    e.putString(String.format(Constants.KEY_ACCOUNT_LIMIT_CURRENCY_BUY, activeAccount), user.getBuyLimit().getCurrencyUnit().getCurrencyCode());
    e.putString(String.format(Constants.KEY_ACCOUNT_LIMIT_CURRENCY_SELL, activeAccount), user.getSellLimit().getCurrencyUnit().getCurrencyCode());

    e.commit();
  }

  @Override
  protected void onFinally() {
    mBus.post(new UserDataUpdatedEvent());
  }

}
