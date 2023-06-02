package com.example.hola.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;

import com.example.hola.R;
import com.example.hola.databinding.ProgressDialogBinding;

public abstract class Utils {

    public static void showAlertDialog(Context context, String title, String msg, String actionText) {
        new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(actionText, null)
                .show();
    }

    public static AlertDialog showProgressDialog(Context context, LayoutInflater layoutInflater, String content) {
        ProgressDialogBinding binding = ProgressDialogBinding.inflate(layoutInflater);
        binding.textContent.setText(content);
        return new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .setCancelable(false)
                .show();
    }

    public static void showConfirmationDialog(Context context, String title, String msg, String positiveText,
                                              String negativeText, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(positiveText, onClickListener)
                .setNegativeButton(negativeText, null)
                .show();
    }
}
