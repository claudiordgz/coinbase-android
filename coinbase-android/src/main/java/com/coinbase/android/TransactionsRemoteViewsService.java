package com.coinbase.android;

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.coinbase.android.db.DatabaseObject;
import com.coinbase.android.db.TransactionsDatabase;
import com.coinbase.android.db.TransactionsDatabase.TransactionEntry;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TransactionsRemoteViewsService extends RemoteViewsService {

  public static final String WIDGET_TRANSACTION_LIMIT = "10";
  public static final String EXTRA_ACCOUNT_ID = "account_id";

  public class TransactionsRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;

    Cursor mCursor;
    int mAccountId;

    public TransactionsRemoteViewsFactory(Context context, int accountId) {
      mContext = context;
      mAccountId = accountId;
    }

    @Override
    public void onCreate() {

      query();
    }

    private void query() {
      Log.i("Coinbase", "Filtering transactions for account " + mAccountId);
      mCursor = DatabaseObject.getInstance().query(mContext, TransactionsDatabase.TransactionEntry.TABLE_NAME,
          null, TransactionEntry.COLUMN_NAME_ACCOUNT + " = ?", new String[] { Integer.toString(mAccountId) }, null, null, null, WIDGET_TRANSACTION_LIMIT);
      Log.i("Coinbase", "Got " + mCursor.getCount() + " transactions.");
    }

    @Override
    public int getCount() {
      return mCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
      return mCursor.getLong(mCursor.getColumnIndex(TransactionEntry.COLUMN_NAME_NUMERIC_ID));
    }

    @Override
    public RemoteViews getLoadingView() {
      return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {

      try {

        mCursor.moveToPosition(position);
        JSONObject item = new JSONObject(new JSONTokener(mCursor.getString(mCursor.getColumnIndex(TransactionEntry.COLUMN_NAME_JSON))));

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_transactions_item);

        // Amount:
        String amount = item.getJSONObject("amount").getString("amount");
        String balanceString = Utils.formatCurrencyAmount(amount);

        int sign = new BigDecimal(amount).compareTo(BigDecimal.ZERO);
        int color = sign == -1 ? R.color.transaction_negative : (sign == 0 ? R.color.transaction_neutral : R.color.transaction_positive);

        rv.setTextViewText(R.id.transaction_amount, balanceString);
        rv.setTextColor(R.id.transaction_amount, mContext.getResources().getColor(color));

        // Currency:
        rv.setTextViewText(R.id.transaction_currency, item.getJSONObject("amount").getString("currency"));

        // Title:
        rv.setTextViewText(R.id.transaction_title, Utils.generateTransactionSummary(mContext, item));

        // Status:
        String status = item.optString("status", getString(R.string.transaction_status_error));

        String readable = status;
        int background = R.drawable.transaction_unknown;
        if("complete".equals(status)) {
          readable = getString(R.string.transaction_status_complete);
          background = R.drawable.transaction_complete;
        } else if("pending".equals(status)) {
          readable = getString(R.string.transaction_status_pending);
          background = R.drawable.transaction_pending;
        }

        rv.setTextViewText(R.id.transaction_status, readable);
        rv.setInt(R.id.transaction_status, "setBackgroundResource", background);

        Intent intent = new Intent();
        intent.putExtra(TransactionDetailsFragment.EXTRA_ID, item.getString("id"));
        rv.setOnClickFillInIntent(R.id.transactions_item, intent);

        return rv;
      } catch(JSONException e) {
        // Database corruption
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public int getViewTypeCount() {
      return 1;
    }

    @Override
    public boolean hasStableIds() {
      return false;
    }

    @Override
    public void onDataSetChanged() {
      query();
    }

    @Override
    public void onDestroy() {

      mCursor.close();
    }


  }

  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {

    return new TransactionsRemoteViewsFactory(this, intent.getIntExtra(EXTRA_ACCOUNT_ID, -1));
  }

}
