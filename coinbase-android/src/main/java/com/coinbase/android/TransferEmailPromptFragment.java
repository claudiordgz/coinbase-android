package com.coinbase.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.coinbase.android.TransferFragment.TransferType;
import com.coinbase.android.delayedtx.DelayedTransaction;
import com.coinbase.android.delayedtx.DelayedTransactionDialogFragment;

public class TransferEmailPromptFragment extends DialogFragment {
  
  private Utils.ContactsAutoCompleteAdapter mAutocompleteAdapter;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final TransferType type = (TransferType) getArguments().getSerializable("type");
    final String amount = getArguments().getString("amount"),
        currency = getArguments().getString("currency"),
        notes = getArguments().getString("notes");

    int messageResource = R.string.transfer_email_prompt_text;
    String message = String.format(getString(messageResource), Utils.formatCurrencyAmount(amount), currency);

    View view = View.inflate(getActivity(), R.layout.transfer_email_prompt, null);
    TextView messageView = (TextView) view.findViewById(R.id.transfer_email_prompt_text);
    final AutoCompleteTextView field = (AutoCompleteTextView) view.findViewById(R.id.transfer_email_prompt_field);

    mAutocompleteAdapter = Utils.getEmailAutocompleteAdapter(getActivity());
    field.setAdapter(mAutocompleteAdapter);
    field.setThreshold(0);
    
    messageView.setText(message);
    
    if(!PlatformUtils.hasHoneycomb()) {
      messageView.setTextColor(Color.WHITE);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {

        // Complete transfer
        TransferFragment parent = getActivity() == null ? null : ((MainActivity) getActivity()).getTransferFragment();
        String recipient = field.getText().toString();

        if(parent != null) {
          if(!Utils.isConnectedOrConnecting(getActivity())) {
            // Internet is not available
            // Show error message and display option to do a delayed transaction
            new DelayedTransactionDialogFragment(
                    new DelayedTransaction(DelayedTransaction.Type.REQUEST, amount, currency, recipient, notes))
                    .show(getFragmentManager(), "delayed_request");
            return;
          } else {
            parent.startTransferTask(type, amount, currency, notes, recipient, null);
          }
        }
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        // User cancelled the dialog
      }
    });

    return builder.create();
  }

}
