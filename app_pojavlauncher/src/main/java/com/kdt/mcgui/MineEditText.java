package com.kdt.mcgui;

import android.content.*;
import android.util.*;
import android.graphics.*;
import android.widget.EditText;
import androidx.core.content.res.ResourcesCompat;
import git.artdeell.mojo.R;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_craze_input, null));
		setTextColor(ResourcesCompat.getColor(getResources(), R.color.primary_text, null));
		setHintTextColor(ResourcesCompat.getColor(getResources(), R.color.text_tertiary, null));
	}
}

