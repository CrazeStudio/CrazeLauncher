package com.kdt.mcgui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;


import git.artdeell.mojo.R;

import fr.spse.extended_view.ExtendedButton;

public class LauncherMenuButton extends ExtendedButton {

    public LauncherMenuButton(@NonNull Context context) {
        super(context);
        setSettings();
    }
    public LauncherMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setSettings();
    }


    /** Set style stuff */
    private void setSettings(){
        int paddingStart = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        int drawablePadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, getResources().getDisplayMetrics());

        setCompoundDrawablePadding(drawablePadding);
        setPaddingRelative(paddingStart, 0, paddingStart, 0);
        setGravity(Gravity.CENTER_VERTICAL);
        setAllCaps(false);
        setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        setTextColor(ResourcesCompat.getColor(getResources(), R.color.primary_text, null));

        // Set drawable size
        int[] sizes = getExtendedViewData().getSizeCompounds();
        sizes[0] = iconSize;
        getExtendedViewData().setSizeCompounds(sizes);
        postProcessDrawables();
    }
}
