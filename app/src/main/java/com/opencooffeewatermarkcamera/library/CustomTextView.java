package com.opencooffeewatermarkcamera.library;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.opencooffeewatermarkcamera.CommonValues;
import com.opencooffeewatermarkcamera.application.WatermarkCameraApplication;

public class CustomTextView extends AppCompatTextView {

	//private static final String LOG_TAG = CustomTextView.class.getSimpleName();

	public CustomTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		setCustomFont(attrs);
	}

	public CustomTextView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    
	    setCustomFont(attrs);
	}
	 	
	private void setCustomFont(AttributeSet attrs) {
	
		int textStyle = attrs.getAttributeIntValue(CommonValues.ANDROID_SCHEMA, "textStyle", Typeface.NORMAL);

	    Typeface customFont = selectTypeface(textStyle);
	
	    setTypeface(customFont);
	}
 	
	private Typeface selectTypeface(int textStyle) {

		if (textStyle == Typeface.NORMAL) {
			return WatermarkCameraApplication.getRegularFont();
		} else {	    
			return WatermarkCameraApplication.getBoldFont();
	    }

	}
		
}