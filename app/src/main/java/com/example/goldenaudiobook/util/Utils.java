package com.example.goldenaudiobook.utils;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import com.example.goldenaudiobook.R;
import com.skydoves.powermenu.CustomPowerMenu;
import com.skydoves.powermenu.MenuAnimation;
import com.skydoves.powermenu.PowerMenu;
import com.skydoves.powermenu.PowerMenuItem;

public class Utils {
    CustomPowerMenu customPowerMenu;
Context ctx;

    public Utils(Context ctx) {
        this.ctx = ctx;

    }

    public static PowerMenu getDialogPowerMenu(Context context, LifecycleOwner lifecycleOwner) {
        return new PowerMenu.Builder(context)
                .setHeaderView(R.layout.layout_dialog_header)
                .setFooterView(R.layout.layout_dialog_footer)
                .addItem(new PowerMenuItem("Do you want to exit App", false))
                .setLifecycleOwner(lifecycleOwner)
                .setAnimation(MenuAnimation.SHOW_UP_CENTER)
                .setMenuRadius(10f)
                .setMenuShadow(10f)
                .setPadding(14)
                .setWidth(600)
                .setSelectedEffect(false)
                .build();
    }


}
