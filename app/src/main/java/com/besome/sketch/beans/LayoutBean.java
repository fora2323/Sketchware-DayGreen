package com.besome.sketch.beans;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

import a.a.a.nA;

public class LayoutBean extends nA implements Parcelable {
    public static final Parcelable.Creator<LayoutBean> CREATOR = new Parcelable.Creator<>() {
        @Override
        public LayoutBean createFromParcel(Parcel source) {
            return new LayoutBean(source);
        }

        @Override
        public LayoutBean[] newArray(int size) {
            return new LayoutBean[size];
        }
    };

    public static final int GRAVITY_BOTTOM = 80;
    public static final int GRAVITY_CENTER = 17;
    public static final int GRAVITY_CENTER_HORIZONTAL = 1;
    public static final int GRAVITY_CENTER_VERTICAL = 16;
    public static final int GRAVITY_LEFT = 3;
    public static final int GRAVITY_NONE = 0;
    public static final int GRAVITY_RIGHT = 5;
    public static final int GRAVITY_TOP = 48;
    public static final int LAYOUT_MATCH_PARENT = -1;
    public static final int LAYOUT_NOTUSED = 0;
    public static final int LAYOUT_WRAP_CONTENT = -2;
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_NONE = -1;
    public static final int ORIENTATION_VERTICAL = 1;
    public static final int VALUE_FALSE = 0;
    public static final int VALUE_TRUE = -1;
    @Expose
    public int backgroundColor;
    @Expose
    public String backgroundResColor;
    @Expose
    public String backgroundResource;
    @Expose
    public int borderColor;
    @Expose
    public int gravity;
    @Expose
    public int height;
    @Expose
    public String resHeight;
    @Expose
    public int layoutGravity;
    @Expose
    public int marginBottom;
    @Expose
    public String resMarginBottom;
    @Expose
    public int marginLeft;
    @Expose
    public String resMarginLeft;
    @Expose
    public int marginRight;
    @Expose
    public String resMarginRight;
    @Expose
    public int marginTop;
    @Expose
    public String resMarginTop;
    @Expose
    public int orientation;
    @Expose
    public int paddingBottom;
    @Expose
    public String resPaddingBottom;
    @Expose
    public int paddingLeft;
    @Expose
    public String resPaddingLeft;
    @Expose
    public int paddingRight;
    @Expose
    public String resPaddingRight;
    @Expose
    public int paddingTop;
    @Expose
    public String resPaddingTop;
    @Expose
    public int weight;
    @Expose
    public String resWeight;
    @Expose
    public int weightSum;
    @Expose
    public String resWeightSum;
    @Expose
    public int width;
    @Expose
    public String resWidth;

    public LayoutBean() {
        width = LAYOUT_WRAP_CONTENT;
        height = LAYOUT_WRAP_CONTENT;
        gravity = GRAVITY_NONE;
        layoutGravity = GRAVITY_NONE;
        orientation = ORIENTATION_NONE;
        backgroundColor = 0xffffff;
        borderColor = 0xff008dcd;
    }

    public LayoutBean(Parcel parcel) {
        width = parcel.readInt();
        height = parcel.readInt();
        orientation = parcel.readInt();
        gravity = parcel.readInt();
        paddingLeft = parcel.readInt();
        paddingTop = parcel.readInt();
        paddingRight = parcel.readInt();
        paddingBottom = parcel.readInt();
        marginLeft = parcel.readInt();
        marginTop = parcel.readInt();
        marginRight = parcel.readInt();
        marginBottom = parcel.readInt();
        weight = parcel.readInt();
        weightSum = parcel.readInt();
        layoutGravity = parcel.readInt();
        backgroundColor = parcel.readInt();
        borderColor = parcel.readInt();
        backgroundResource = parcel.readString();
        backgroundResColor = parcel.readString();
        resWidth = parcel.readString();
        resHeight = parcel.readString();
        resMarginLeft = parcel.readString();
        resMarginTop = parcel.readString();
        resMarginRight = parcel.readString();
        resMarginBottom = parcel.readString();
        resPaddingLeft = parcel.readString();
        resPaddingTop = parcel.readString();
        resPaddingRight = parcel.readString();
        resPaddingBottom = parcel.readString();
        resWeight = parcel.readString();
        resWeightSum = parcel.readString();
    }

    public static Parcelable.Creator<LayoutBean> getCreator() {
        return CREATOR;
    }

    public void copy(LayoutBean layoutBean) {
        width = layoutBean.width;
        height = layoutBean.height;
        orientation = layoutBean.orientation;
        gravity = layoutBean.gravity;
        paddingLeft = layoutBean.paddingLeft;
        paddingTop = layoutBean.paddingTop;
        paddingRight = layoutBean.paddingRight;
        paddingBottom = layoutBean.paddingBottom;
        marginLeft = layoutBean.marginLeft;
        marginTop = layoutBean.marginTop;
        marginRight = layoutBean.marginRight;
        marginBottom = layoutBean.marginBottom;
        weight = layoutBean.weight;
        weightSum = layoutBean.weightSum;
        layoutGravity = layoutBean.layoutGravity;
        backgroundColor = layoutBean.backgroundColor;
        borderColor = layoutBean.borderColor;
        backgroundResource = layoutBean.backgroundResource;
        backgroundResColor = layoutBean.backgroundResColor;
        resWidth = layoutBean.resWidth;
        resHeight = layoutBean.resHeight;
        resMarginLeft = layoutBean.resMarginLeft;
        resMarginTop = layoutBean.resMarginTop;
        resMarginRight = layoutBean.resMarginRight;
        resMarginBottom = layoutBean.resMarginBottom;
        resPaddingLeft = layoutBean.resPaddingLeft;
        resPaddingTop = layoutBean.resPaddingTop;
        resPaddingRight = layoutBean.resPaddingRight;
        resPaddingBottom = layoutBean.resPaddingBottom;
        resWeight = layoutBean.resWeight;
        resWeightSum = layoutBean.resWeightSum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isEqual(LayoutBean layoutBean) {
        if (width != layoutBean.width || height != layoutBean.height || orientation != layoutBean.orientation || gravity != layoutBean.gravity || paddingLeft != layoutBean.paddingLeft || paddingTop != layoutBean.paddingTop || paddingRight != layoutBean.paddingRight || paddingBottom != layoutBean.paddingBottom || marginLeft != layoutBean.marginLeft || marginTop != layoutBean.marginTop || marginRight != layoutBean.marginRight || marginBottom != layoutBean.marginBottom || weight != layoutBean.weight || weightSum != layoutBean.weightSum || layoutGravity != layoutBean.layoutGravity || backgroundColor != layoutBean.backgroundColor || borderColor != layoutBean.borderColor) {
            return false;
        }
        if (!java.util.Objects.equals(backgroundResource, layoutBean.backgroundResource)) return false;
        if (!java.util.Objects.equals(backgroundResColor, layoutBean.backgroundResColor)) return false;
        if (!java.util.Objects.equals(resWidth, layoutBean.resWidth)) return false;
        if (!java.util.Objects.equals(resHeight, layoutBean.resHeight)) return false;
        if (!java.util.Objects.equals(resMarginLeft, layoutBean.resMarginLeft)) return false;
        if (!java.util.Objects.equals(resMarginTop, layoutBean.resMarginTop)) return false;
        if (!java.util.Objects.equals(resMarginRight, layoutBean.resMarginRight)) return false;
        if (!java.util.Objects.equals(resMarginBottom, layoutBean.resMarginBottom)) return false;
        if (!java.util.Objects.equals(resPaddingLeft, layoutBean.resPaddingLeft)) return false;
        if (!java.util.Objects.equals(resPaddingTop, layoutBean.resPaddingTop)) return false;
        if (!java.util.Objects.equals(resPaddingRight, layoutBean.resPaddingRight)) return false;
        if (!java.util.Objects.equals(resPaddingBottom, layoutBean.resPaddingBottom)) return false;
        if (!java.util.Objects.equals(resWeight, layoutBean.resWeight)) return false;
        return java.util.Objects.equals(resWeightSum, layoutBean.resWeightSum);
    }

    public void print() {
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(width);
        parcel.writeInt(height);
        parcel.writeInt(orientation);
        parcel.writeInt(gravity);
        parcel.writeInt(paddingLeft);
        parcel.writeInt(paddingTop);
        parcel.writeInt(paddingRight);
        parcel.writeInt(paddingBottom);
        parcel.writeInt(marginLeft);
        parcel.writeInt(marginTop);
        parcel.writeInt(marginRight);
        parcel.writeInt(marginBottom);
        parcel.writeInt(weight);
        parcel.writeInt(weightSum);
        parcel.writeInt(layoutGravity);
        parcel.writeInt(backgroundColor);
        parcel.writeInt(borderColor);
        parcel.writeString(backgroundResource);
        parcel.writeString(backgroundResColor);
        parcel.writeString(resWidth);
        parcel.writeString(resHeight);
        parcel.writeString(resMarginLeft);
        parcel.writeString(resMarginTop);
        parcel.writeString(resMarginRight);
        parcel.writeString(resMarginBottom);
        parcel.writeString(resPaddingLeft);
        parcel.writeString(resPaddingTop);
        parcel.writeString(resPaddingRight);
        parcel.writeString(resPaddingBottom);
        parcel.writeString(resWeight);
        parcel.writeString(resWeightSum);
    }
}
