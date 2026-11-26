package pro.sketchware.tools;

import static pro.sketchware.utility.PropertiesUtil.parseReferName;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.TextBean;
import com.besome.sketch.beans.ViewBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import mod.agus.jcoderz.beans.ViewBeans;
import org.sketchware.daygreen.preview.PreviewResourceResolver;
import pro.sketchware.utility.AttributeConstants;
import pro.sketchware.utility.PropertiesUtil;

public class ViewBeanFactory {

    private final ViewBean bean;
    private PreviewResourceResolver resourceResolver = null;

    public ViewBeanFactory(ViewBean bean) {
        this.bean = bean;
    }

    public void setResourceResolver(PreviewResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public static int getConsideredTypeViewByName(String name, int def) {
        return switch (name) {
            //Add more here
            case "MaterialSwitch" -> ViewBeans.VIEW_TYPE_WIDGET_MATERIALSWITCH;
            case "MaterialCardView", "CardView" -> ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW;
            case "TextInputEditText" -> ViewBean.VIEW_TYPE_WIDGET_EDITTEXT;
            case "ImageButton" -> ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW;
            case "NestedScrollView" -> ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW;
            default -> def;
        };
    }

    public void applyAttributes(Map<String, String> attributes) {
        Map<String, String> injectAttributes = new LinkedHashMap<>();
        if ("include".equals(bean.convert)) {
            StringBuilder injectProperty = new StringBuilder();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                var attrName = entry.getKey();
                var attrValue = entry.getValue();
                if (attrName.equals("layout")) {
                    continue;
                }
                injectProperty
                        .append(attrName)
                        .append("=\"")
                        .append(attrValue)
                        .append("\"")
                        .append("\n");
            }
            bean.inject = injectProperty.toString().trim();
            return;
        }

        var layoutBean = bean.layout;
        var width = attributes.getOrDefault("android:layout_width", null);
        if (width != null) {
            var va = getEnum("layout_width", width, null);
            if (va != null) {
                layoutBean.width = Integer.parseInt(va);
            } else {
                var size = resolveDimenSize(width);
                if (size != null) {
                    layoutBean.width = Integer.parseInt(size);
                } else {
                    injectAttributes.put("android:layout_width", width);
                }
            }
        }

        var height = attributes.getOrDefault("android:layout_height", null);
        if (height != null) {
            var va = getEnum("layout_height", height, null);
            if (va != null) {
                layoutBean.height = Integer.parseInt(va);
            } else {
                var size = resolveDimenSize(height);
                if (size != null) {
                    layoutBean.height = Integer.parseInt(size);
                } else {
                    injectAttributes.put("android:layout_height", height);
                }
            }
        }

        var orientation = attributes.getOrDefault("android:orientation", null);
        if (orientation != null) {
            var va = getEnum("orientation", orientation, null);
            if (va != null) {
                layoutBean.orientation = Integer.parseInt(va);
            } else {
                layoutBean.orientation = LayoutBean.ORIENTATION_NONE;
                injectAttributes.put("android:orientation", orientation);
            }
        }

        var weight = attributes.getOrDefault("android:layout_weight", null);
        if (weight != null) {
            try {
                layoutBean.weight = Integer.parseInt(weight);
            } catch (Exception e) {
                injectAttributes.put("android:layout_weight", weight);
            }
        }

        var weightSum = attributes.getOrDefault("android:weightSum", null);
        if (weightSum != null) {
            try {
                layoutBean.weightSum = Integer.parseInt(weightSum);
            } catch (Exception e) {
                injectAttributes.put("android:weightSum", weightSum);
            }
        }
        applyView(attributes, injectAttributes);
        applyPadding(attributes, injectAttributes);
        applyMargin(attributes, injectAttributes);
        applyGravity(attributes, injectAttributes);

        applyBackgroundResource(attributes, injectAttributes);
        if (bean.getClassInfo().a("TextView")) {
            applyText(attributes, injectAttributes);
        }
        if (bean.getClassInfo().a("ImageView")) {
            applyImage(attributes, injectAttributes);
        }
        
        var textAttributes = java.util.List.of(
                "text", "textSize", "textColor", "textStyle", "textColorHint", "hint",
                "singleLine", "lines", "inputType", "imeOptions", "fontFamily", 
                "typeface", "letterSpacing", "lineSpacingExtra", "lineSpacingMultiplier", 
                "textAllCaps", "ellipsize", "maxLines", "minLines"
        );

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            var attrName = entry.getKey();
            var attrValue = entry.getValue();
            if (attrName.equals("app:backgroundTint") && bean.getClassInfo().b("MaterialButton")) {
                continue;
            }
            
            var reference = parseReferName(attrName, ":");
            boolean isTextFallback = textAttributes.contains(reference) && !bean.getClassInfo().a("TextView");

            if (!AttributeConstants.BUILT_IN_ATTRIBUTES.contains(reference) || isTextFallback) {
                if (attrName.equals("style")
                        && bean.type == ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR) {
                    continue;
                }
                if (isRelativeAttr(attrName)) {
                    bean.parentAttributes.put(attrName, parseReferName(attrValue, "/"));
                    continue;
                }
                if (attrName.equals("tools:listitem")) {
                    continue;
                }
                injectAttributes.put(attrName, attrValue);
            }
        }
        
        if (bean.getClassInfo().b("ListView")
                || bean.getClassInfo().b("GridView")
                || bean.getClassInfo().b("Spinner")
                || bean.getClassInfo().b("RecyclerView")
                || bean.getClassInfo().b("ViewPager")) {
            var customView = attributes.getOrDefault("tools:listitem", null);
            if (customView != null) {
                if (customView.startsWith("@layout/")) {
                    var layoutName = parseReferName(customView, "/");
                    bean.customView = !layoutName.isEmpty() ? layoutName : "none";
                } else {
                    injectAttributes.put("tools:listitem", customView);
                }
            }
        }

        StringBuilder injectProperty = new StringBuilder();
        injectAttributes
                .forEach((key, value) -> injectProperty
                        .append(key)
                        .append("=\"")
                        .append(value)
                        .append("\"")
                        .append("\n"));
        bean.inject = injectProperty.toString().trim();
    }

    private Float resolveDimenFloatPreview(String value) {
        if (value == null || resourceResolver == null) return null;
        return resourceResolver.resolveDimenToDp(value);
    }

    private void applyView(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var enabled = attributes.getOrDefault("android:enabled", null);
        if (enabled != null) {
            if (!enabled.startsWith("@bool/")) {
                bean.enabled = enabled.equals("true") ? 1 : 0;
            } else {
                injectAttributes.put("android:enabled", enabled);
            }
        }

        var clickable = attributes.getOrDefault("android:clickable", null);
        if (clickable != null) {
            if (!clickable.startsWith("@bool/")) {
                bean.clickable = clickable.equals("true") ? 1 : 0;
            } else {
                injectAttributes.put("android:clickable", clickable);
            }
        }

        var alpha = attributes.getOrDefault("android:alpha", null);
        if (alpha != null) {
            try {
                bean.alpha = Float.parseFloat(alpha);
            } catch (NumberFormatException e) {
                injectAttributes.put("android:alpha", alpha);
            }
        }

        var scaleX = attributes.getOrDefault("android:scaleX", null);
        if (scaleX != null) {
            try {
                bean.scaleX = Float.parseFloat(scaleX);
            } catch (NumberFormatException e) {
                injectAttributes.put("android:scaleX", scaleX);
            }
        }

        var scaleY = attributes.getOrDefault("android:scaleY", null);
        if (scaleY != null) {
            try {
                bean.scaleY = Float.parseFloat(scaleY);
            } catch (NumberFormatException e) {
                injectAttributes.put("android:scaleY", scaleY);
            }
        }

        var translationX = getDimenValue("android:translationX", attributes, injectAttributes);
        if (translationX != null) {
            try {
                bean.translationX = Float.parseFloat(translationX);
            } catch (NumberFormatException e) {
                injectAttributes.put("android:translationX", translationX);
            }
        }

        var translationY = getDimenValue("android:translationY", attributes, injectAttributes);
        if (translationY != null) {
            try {
                bean.translationY = Float.parseFloat(translationY);
            } catch (NumberFormatException e) {
                injectAttributes.put("android:translationY", translationY);
            }
        }

        var rotation = attributes.getOrDefault("android:rotation", null);
        if (rotation != null) {
            try {
                bean.image.rotate = Integer.parseInt(rotation);
            } catch (Exception e) {
                injectAttributes.put("android:rotation", rotation);
            }
        }

        switch (bean.type) {
            case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX,
                 ViewBean.VIEW_TYPE_WIDGET_SWITCH,
                 ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON -> {
                var checked = attributes.getOrDefault("android:checked", null);
                if (checked != null) {
                    if (!checked.startsWith("@bool/")) {
                        bean.checked = checked.equals("true") ? 1 : 0;
                    } else {
                        injectAttributes.put("android:checked", checked);
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR, ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR -> {
                var progress = attributes.getOrDefault("android:progress", null);
                if (progress != null) {
                    try {
                        bean.progress = Integer.parseInt(progress);
                    } catch (Exception e) {
                        injectAttributes.put("android:progress", progress);
                    }
                }

                var max = attributes.getOrDefault("android:max", null);
                if (max != null) {
                    try {
                        var maxValue = Integer.parseInt(max);
                        if (maxValue != ViewBean.DEFAULT_MAX) {
                            bean.max = maxValue;
                        }
                    } catch (Exception e) {
                        injectAttributes.put("android:max", max);
                    }
                }
                if (bean.type == ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR) {
                    var style = attributes.getOrDefault("style", null);
                    if (style != null) {
                        if (style.equals(ViewBean.PROGRESSBAR_STYLE_HORIZONTAL)) {
                            bean.progressStyle = ViewBean.PROGRESSBAR_STYLE_HORIZONTAL;
                        } else if (style.equals(ViewBean.PROGRESSBAR_STYLE_CIRCLE)) {
                            bean.progressStyle = ViewBean.PROGRESSBAR_STYLE_CIRCLE;
                        } else {
                            injectAttributes.put("style", ViewBean.PROGRESSBAR_STYLE_HORIZONTAL);
                        }
                    }

                    var indeterminate = attributes.getOrDefault("android:indeterminate", null);
                    if (indeterminate != null) {
                        if (!indeterminate.startsWith("@bool/")) {
                            bean.indeterminate = indeterminate;
                        } else {
                            injectAttributes.put("android:indeterminate", indeterminate);
                        }
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW -> {
                var firstDayOfWeek = attributes.getOrDefault("android:firstDayOfWeek", null);
                if (firstDayOfWeek != null) {
                    try {
                        var firstDayOfWeekValue = Integer.parseInt(firstDayOfWeek);
                        if (firstDayOfWeekValue != 1) {
                            bean.firstDayOfWeek = firstDayOfWeekValue;
                        }
                    } catch (Exception e) {
                        injectAttributes.put("android:firstDayOfWeek", firstDayOfWeek);
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_SPINNER -> {
                var spinnerMode = attributes.getOrDefault("android:spinnerMode", null);
                if (spinnerMode != null) {
                    if (spinnerMode.equals("dialog")) {
                        bean.spinnerMode = ViewBean.SPINNER_MODE_DIALOG;
                    } else if (spinnerMode.equals("dropdown")) {
                        bean.spinnerMode = ViewBean.SPINNER_MODE_DROPDOWN;
                    } else {
                        injectAttributes.put("android:spinnerMode", spinnerMode);
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_LISTVIEW -> {
                var dividerHeight = getDimen("android:dividerHeight", attributes, injectAttributes);
                if (dividerHeight > 1) {
                    bean.dividerHeight = dividerHeight;
                }

                var choiceMode = attributes.getOrDefault("android:choiceMode", null);
                if (choiceMode != null) {
                    var value =
                            switch (choiceMode) {
                                case "none" -> ViewBean.CHOICE_MODE_NONE;
                                case "singleChoice" -> ViewBean.CHOICE_MODE_SINGLE;
                                case "multipleChoice" -> ViewBean.CHOICE_MODE_MULTI;
                                default -> -1;
                            };
                    if (value > -1) {
                        bean.choiceMode = value;
                    } else {
                        injectAttributes.put("android:choiceMode", choiceMode);
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_ADVIEW -> {
                var adSize = attributes.getOrDefault("app:adSize", null);
                bean.adSize = Objects.requireNonNullElse(adSize, "SMART_BANNER");
            }
        }
    }

    private void applyPadding(Map<String, String> attributes, Map<String, String> injectAttributes) {
        if (resourceResolver != null) {
            applyPaddingPreview(attributes, injectAttributes);
            return;
        }

        var bean = this.bean.layout;
        String padding = attributes.getOrDefault("android:padding", null);
        if (padding != null) {
            int paddingValue = getDimen("android:padding", attributes, injectAttributes);
            if (paddingValue > -1) {
                bean.paddingLeft = paddingValue;
                bean.paddingTop = paddingValue;
                bean.paddingRight = paddingValue;
                bean.paddingBottom = paddingValue;
            }
        } else {
            int paddingLeft = getDimen("android:paddingLeft", attributes, injectAttributes);
            int paddingTop = getDimen("android:paddingTop", attributes, injectAttributes);
            int paddingRight = getDimen("android:paddingRight", attributes, injectAttributes);
            int paddingBottom = getDimen("android:paddingBottom", attributes, injectAttributes);

            if (paddingLeft > -1) bean.paddingLeft = paddingLeft;
            if (paddingTop > -1) bean.paddingTop = paddingTop;
            if (paddingRight > -1) bean.paddingRight = paddingRight;
            if (paddingBottom > -1) bean.paddingBottom = paddingBottom;
        }

        String contentPadding = attributes.getOrDefault("app:contentPadding", null);
        if (contentPadding != null) {
            int paddingValue = getDimen("app:contentPadding", attributes, injectAttributes);
            if (paddingValue > -1) {
                bean.paddingLeft = paddingValue;
                bean.paddingTop = paddingValue;
                bean.paddingRight = paddingValue;
                bean.paddingBottom = paddingValue;
            }
        } else {
            int paddingLeft = getDimen("app:contentPaddingLeft", attributes, injectAttributes);
            int paddingTop = getDimen("app:contentPaddingTop", attributes, injectAttributes);
            int paddingRight = getDimen("app:contentPaddingRight", attributes, injectAttributes);
            int paddingBottom = getDimen("app:contentPaddingBottom", attributes, injectAttributes);

            if (paddingLeft > -1) bean.paddingLeft = paddingLeft;
            if (paddingTop > -1) bean.paddingTop = paddingTop;
            if (paddingRight > -1) bean.paddingRight = paddingRight;
            if (paddingBottom > -1) bean.paddingBottom = paddingBottom;
        }
    }

    private void applyPaddingPreview(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var bean = this.bean.layout;

        Float padAll = resolveDimenFloatPreview(attributes.get("android:padding"));
        Float padH = resolveDimenFloatPreview(attributes.get("android:paddingHorizontal"));
        Float padV = resolveDimenFloatPreview(attributes.get("android:paddingVertical"));

        Float padLeft = resolveDimenFloatPreview(attributes.get("android:paddingLeft"));
        Float padStart = resolveDimenFloatPreview(attributes.get("android:paddingStart"));
        Float padRight = resolveDimenFloatPreview(attributes.get("android:paddingRight"));
        Float padEnd = resolveDimenFloatPreview(attributes.get("android:paddingEnd"));
        Float padTop = resolveDimenFloatPreview(attributes.get("android:paddingTop"));
        Float padBottom = resolveDimenFloatPreview(attributes.get("android:paddingBottom"));

        Float cPadAll = resolveDimenFloatPreview(attributes.get("app:contentPadding"));
        Float cPadLeft = resolveDimenFloatPreview(attributes.get("app:contentPaddingLeft"));
        Float cPadRight = resolveDimenFloatPreview(attributes.get("app:contentPaddingRight"));
        Float cPadTop = resolveDimenFloatPreview(attributes.get("app:contentPaddingTop"));
        Float cPadBottom = resolveDimenFloatPreview(attributes.get("app:contentPaddingBottom"));

        Float finalLeft = cPadLeft != null ? cPadLeft :
                          (cPadAll != null ? cPadAll :
                          (padLeft != null ? padLeft :
                          (padStart != null ? padStart :
                          (padH != null ? padH : padAll))));
        if (finalLeft != null) bean.paddingLeft = Math.max(0, Math.round(finalLeft));

        Float finalTop = cPadTop != null ? cPadTop :
                         (cPadAll != null ? cPadAll :
                         (padTop != null ? padTop :
                         (padV != null ? padV : padAll)));
        if (finalTop != null) bean.paddingTop = Math.max(0, Math.round(finalTop));

        Float finalRight = cPadRight != null ? cPadRight :
                           (cPadAll != null ? cPadAll :
                           (padRight != null ? padRight :
                           (padEnd != null ? padEnd :
                           (padH != null ? padH : padAll))));
        if (finalRight != null) bean.paddingRight = Math.max(0, Math.round(finalRight));

        Float finalBottom = cPadBottom != null ? cPadBottom :
                            (cPadAll != null ? cPadAll :
                            (padBottom != null ? padBottom :
                            (padV != null ? padV : padAll)));
        if (finalBottom != null) bean.paddingBottom = Math.max(0, Math.round(finalBottom));
    }

    private void applyMargin(Map<String, String> attributes, Map<String, String> injectAttributes) {
        if (resourceResolver != null) {
            applyMarginPreview(attributes, injectAttributes);
            return;
        }

        var bean = this.bean.layout;
        String margin = attributes.getOrDefault("android:layout_margin", null);
        if (margin != null) {
            int marginValue = getDimen("android:layout_margin", attributes, injectAttributes);
            if (marginValue > -1) {
                bean.marginLeft = marginValue;
                bean.marginTop = marginValue;
                bean.marginRight = marginValue;
                bean.marginBottom = marginValue;
            }
        } else {
            int marginLeft = getDimen("android:layout_marginLeft", attributes, injectAttributes);
            int marginTop = getDimen("android:layout_marginTop", attributes, injectAttributes);
            int marginRight = getDimen("android:layout_marginRight", attributes, injectAttributes);
            int marginBottom = getDimen("android:layout_marginBottom", attributes, injectAttributes);

            if (marginLeft > -1) bean.marginLeft = marginLeft;
            if (marginTop > -1) bean.marginTop = marginTop;
            if (marginRight > -1) bean.marginRight = marginRight;
            if (marginBottom > -1) bean.marginBottom = marginBottom;
        }
    }

    private void applyMarginPreview(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var bean = this.bean.layout;

        Float mAll = resolveDimenFloatPreview(attributes.get("android:layout_margin"));
        Float mH = resolveDimenFloatPreview(attributes.get("android:layout_marginHorizontal"));
        Float mV = resolveDimenFloatPreview(attributes.get("android:layout_marginVertical"));

        Float mLeft = resolveDimenFloatPreview(attributes.get("android:layout_marginLeft"));
        Float mStart = resolveDimenFloatPreview(attributes.get("android:layout_marginStart"));
        Float mRight = resolveDimenFloatPreview(attributes.get("android:layout_marginRight"));
        Float mEnd = resolveDimenFloatPreview(attributes.get("android:layout_marginEnd"));
        Float mTop = resolveDimenFloatPreview(attributes.get("android:layout_marginTop"));
        Float mBottom = resolveDimenFloatPreview(attributes.get("android:layout_marginBottom"));

        Float finalLeft = mLeft != null ? mLeft : (mStart != null ? mStart : (mH != null ? mH : mAll));
        if (finalLeft != null) bean.marginLeft = Math.round(finalLeft);

        Float finalTop = mTop != null ? mTop : (mV != null ? mV : mAll);
        if (finalTop != null) bean.marginTop = Math.round(finalTop);

        Float finalRight = mRight != null ? mRight : (mEnd != null ? mEnd : (mH != null ? mH : mAll));
        if (finalRight != null) bean.marginRight = Math.round(finalRight);

        Float finalBottom = mBottom != null ? mBottom : (mV != null ? mV : mAll);
        if (finalBottom != null) bean.marginBottom = Math.round(finalBottom);
    }

    private void applyGravity(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var bean = this.bean.layout;
        var layoutGravity = attributes.getOrDefault("android:layout_gravity", null);
        if (layoutGravity != null) {
            var value = getFlag("android:layout_gravity", layoutGravity, injectAttributes);
            if (value > -1) {
                bean.layoutGravity = value;
            }
        }

        var gravity = attributes.getOrDefault("android:gravity", null);
        if (gravity != null) {
            var value = getFlag("android:gravity", gravity, injectAttributes);
            if (value > -1) {
                bean.gravity = value;
            }
        }
    }

    private void applyBackgroundResource(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var background = attributes.getOrDefault("android:background", null);
        applyBackground("android:background", background, injectAttributes);
        var cardBackgroundColor = attributes.getOrDefault("app:cardBackgroundColor", null);
        if (bean.getClassInfo().b("MaterialButton")) {
            var backgroundTint = attributes.getOrDefault("app:backgroundTint", null);
            applyBackground("app:backgroundTint", backgroundTint, injectAttributes);
        }
    }

    private void applyText(Map<String, String> attributes, Map<String, String> injectAttributes) {
        TextBean bean = this.bean.text;
        var text = attributes.getOrDefault("android:text", null);
        if (text != null) {
            bean.text = text;
        }

        var textSize = getDimen("android:textSize", attributes, injectAttributes);
        if (textSize > 0) {
            bean.textSize = textSize;
        }

        var textStyle = attributes.getOrDefault("android:textStyle", null);
        if (textStyle != null) {
            var value = getFlag("android:textStyle", textStyle, injectAttributes);
            if (value > -1) {
                bean.textType = value;
            }
        }

        var textColor = attributes.getOrDefault("android:textColor", null);
        if (textColor != null) {
            if (PropertiesUtil.isHexColor(textColor)) {
                bean.textColor = PropertiesUtil.parseColor(textColor);
            } else if (textColor.startsWith("@color/") || textColor.startsWith("?")) {
                bean.resTextColor = textColor;
            } else {
                injectAttributes.put("android:textColor", textColor);
            }
        }
        switch (this.bean.type) {
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT,
                 ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW,
                 ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW -> {
                var hint = attributes.getOrDefault("android:hint", null);
                if (hint != null) bean.hint = hint;

                var hintColor = attributes.getOrDefault("android:textColorHint", null);
                if (hintColor != null) {
                    if (PropertiesUtil.isHexColor(hintColor)) {
                        bean.hintColor = PropertiesUtil.parseColor(hintColor);
                    } else if (hintColor.startsWith("@color/") || hintColor.startsWith("?")) {
                        bean.resHintColor = hintColor;
                    } else {
                        injectAttributes.put("android:textColorHint", hintColor);
                    }
                }
                var singleLine = attributes.getOrDefault("android:singleLine", null);
                if (singleLine != null) {
                    if (!singleLine.startsWith("@bool/")) {
                        bean.singleLine = singleLine.equals("true") ? 1 : 0;
                    } else {
                        injectAttributes.put("android:singleLine", singleLine);
                    }
                }

                var lines = attributes.getOrDefault("android:lines", null);
                if (lines != null) {
                    try {
                        bean.line = Integer.parseInt(lines);
                    } catch (Exception e) {
                        injectAttributes.put("android:lines", lines);
                    }
                }

                var inputType = attributes.getOrDefault("android:inputType", null);
                if (inputType != null) {
                    var value = getFlag("android:inputType", inputType, injectAttributes);
                    if (value > -1) bean.inputType = value;
                }
                var imeOptions = attributes.getOrDefault("android:imeOptions", null);
                if (imeOptions != null) {
                    var value = switch (imeOptions) {
                        case "actionNone" -> TextBean.IME_OPTION_NONE;
                        case "actionGo" -> TextBean.IME_OPTION_GO;
                        case "actionSearch" -> TextBean.IME_OPTION_SEARCH;
                        case "actionSend" -> TextBean.IME_OPTION_SEND;
                        case "actionNext" -> TextBean.IME_OPTION_NEXT;
                        case "actionDone" -> TextBean.IME_OPTION_DONE;
                        default -> -1;
                    };
                    if (value > 1) {
                        bean.imeOption = value;
                    } else {
                        injectAttributes.put("android:imeOptions", imeOptions);
                    }
                }
            }
            case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW -> {
                var singleLine = attributes.getOrDefault("android:singleLine", null);
                if (singleLine != null) {
                    if (!singleLine.startsWith("@bool/")) {
                        bean.singleLine = singleLine.equals("true") ? 1 : 0;
                    } else {
                        injectAttributes.put("android:singleLine", singleLine);
                    }
                }
                var lines = attributes.getOrDefault("android:lines", null);
                if (lines != null) {
                    try {
                        bean.line = Integer.parseInt(lines);
                    } catch (Exception e) {
                        injectAttributes.put("android:lines", lines);
                    }
                }
            }
        }
    }

    private void applyImage(Map<String, String> attributes, Map<String, String> injectAttributes) {
        var bean = this.bean.image;
        var src = attributes.getOrDefault("android:src", null);
        if (src != null) {
            if (src.startsWith("@drawable/")) {
                bean.resName = parseReferName(src, "/");
            } else {
                bean.resName = "default_image";
                injectAttributes.put("android:src", src);
            }
        } else {
            bean.resName = "default_image";
        }

        var scaleType = attributes.getOrDefault("android:scaleType", null);
        if (scaleType != null) {
            var va = getEnum("scaleType", scaleType, null);
            if (va != null) {
                bean.scaleType = va;
            } else {
                injectAttributes.put("android:scaleType", scaleType);
            }
        }
    }

    private String getDimenValue(String attributeName, Map<String, String> attributes, Map<String, String> injectAttributes) {
        String value = attributes.getOrDefault(attributeName, null);
        if (value != null) {
            var size = resolveDimenSize(value);
            if (size != null) return size;
            else injectAttributes.put(attributeName, value);
        }
        return null;
    }

    private int getDimen(String attributeName, Map<String, String> attributes, Map<String, String> injectAttributes) {
        String value = attributes.getOrDefault(attributeName, null);
        if (value != null) {
            var size = resolveDimenSize(value);
            if (size != null) {
                try {
                    return Integer.parseInt(size);
                } catch (NumberFormatException ignored) {}
            }
            injectAttributes.put(attributeName, value);
        }
        return -1;
    }

    private void applyBackground(String name, String background, Map<String, String> injectAttributes) {
        var bean = this.bean.layout;
        if (background != null) {
            if (PropertiesUtil.isHexColor(background)) {
                bean.backgroundColor = PropertiesUtil.parseColor(background);
            } else if (background.startsWith("@color/") || background.startsWith("?")) {
                bean.backgroundResColor = background;
                bean.backgroundColor = 0xFFFFFFFF;
            } else if (background.startsWith("@drawable/")) {
                bean.backgroundResource = parseReferName(background, "/");
            } else if (background.equals("@android:color/transparent")) {
                bean.backgroundColor = 0;
            } else {
                injectAttributes.put(name, background);
            }
        }
    }

    private int getFlag(String attribute, String value, Map<String, String> injectAttributes) {
        var name = parseReferName(attribute, ":");
        if (value != null) {
            if (containsFlag(name, value)) {
                return Integer.parseInt(getFlag(name, value));
            } else {
                injectAttributes.put(attribute, value);
            }
        }
        return -1;
    }

    private boolean isRelativeAttr(String attr) {
        for (String relativeAttr : AttributeConstants.RELATIVE_ATTRIBUTES) {
            if (relativeAttr.equals(attr)) return true;
        }
        return false;
    }

    private String getEnum(String name, String value) {
        return getEnum(name, value, value);
    }

    private String getEnum(String name, String value, String def) {
        Map<String, String> map = AttributeConstants.MAP_ATTR_ENUM.get(name);
        assert map != null;
        if (map.containsKey(value)) return map.get(value);
        return def;
    }

    private boolean containsFlag(String name, String value) {
        Map<String, Integer> map = AttributeConstants.MAP_ATTR_FLAG.get(name);
        var flags = value.split("\\|");
        for (String flag : flags) {
            if (!map.containsKey(flag)) return false;
        }
        return true;
    }

    private String getFlag(String name, String value) {
        Map<String, Integer> map = AttributeConstants.MAP_ATTR_FLAG.get(name);
        assert map != null;

        if (!value.contains("|")) {
            return String.valueOf(map.getOrDefault(value, -1));
        }

        int flag = -1;
        String[] flags = value.split("\\|");
        for (String f : flags) {
            if (!f.trim().isEmpty()) {
                Integer v = map.get(f);
                if (v != null) {
                    if (flag == -1) flag = v;
                    else flag |= v;
                }
            }
        }
        return String.valueOf(flag);
    }

    private boolean hasDimensionSuffix(String str) {
        String pattern = "^(\\d+(\\.\\d+)?)(dp|sp|px|pt|in|mm)$";
        return str.matches(pattern);
    }

    private String resolveDimenSize(String value) {
        if (value != null && value.startsWith("@dimen/") && resourceResolver != null) {
            Float dp = resourceResolver.resolveDimenToDp(value);
            if (dp != null) {
                return String.valueOf(Math.round(dp));
            }
        }
        if (hasDimensionSuffix(value)) {
            var reference = PropertiesUtil.getUnitOrPrefix(value);
            if (reference != null) return reference.second;
        }
        return null;
    }
}