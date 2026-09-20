use crate::blocks::block_palette::*;

pub struct GeneralAttrs;

impl GeneralAttrs {
    pub fn load() -> BlockPalette {
        let mut block_palette = BlockPalette::new("General Attr", 0xFFC8A030);

        block_palette.push(
            BlockBean::new()
                .set_header_text("XML Namespace Declaration")
                .set_type(" ")
                .set_op_code("android_namespace")
                .set_spec("Declare Android Namespace")
                .set_code("xmlns:android=\"http://schemas.android.com/apk/res/android\""),
        );

        block_palette.push(
            BlockBean::new()
                .set_type(" ")
                .set_op_code("app_namespace")
                .set_spec("Declare App Namespace")
                .set_code("xmlns:app=\"http://schemas.android.com/apk/res-auto\""),
        );

        block_palette.push(
            BlockBean::new()
                .set_type(" ")
                .set_op_code("tools_namespace")
                .set_spec("Declare Tools Namespace")
                .set_code("xmlns:tools=\"http://schemas.android.com/tools\""),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Identification")
                .set_attr("id")
                .set_value_prefix("@+id/")
                .build(),
        );

        block_palette.push(XmlAttrBlockBean::new().set_attr("tag").build());

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Visibility & Interaction")
                .set_attr("visibility")
                .set_target_type("m.VisibilityTypes")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("enabled")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("clickable")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("focusable")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("focusableInTouchMode")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("longClickable")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Layout")
                .set_attr("layout_width")
                .set_target_type("m.LayoutSize")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_height")
                .set_target_type("m.LayoutSize")
                .build(),
        );

        block_palette.push(XmlAttrBlockBean::new().set_attr("layout_margin").build());
        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_marginLeft")
                .build(),
        );
        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_marginRight")
                .build(),
        );
        block_palette.push(XmlAttrBlockBean::new().set_attr("layout_marginTop").build());
        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_marginBottom")
                .build(),
        );
        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_marginStart")
                .build(),
        );
        block_palette.push(XmlAttrBlockBean::new().set_attr("layout_marginEnd").build());

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Padding")
                .set_attr("padding")
                .build(),
        );

        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingLeft").build());
        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingRight").build());
        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingTop").build());
        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingBottom").build());
        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingStart").build());
        block_palette.push(XmlAttrBlockBean::new().set_attr("paddingEnd").build());

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Drawing & Display")
                .set_attr("background")
                .set_target_type("m.ResDrawable")
                .build(),
        );

        block_palette.push(XmlAttrBlockBean::new().set_attr("foreground").build());

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("alpha")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("elevation")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("translationX")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("translationY")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("rotation")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("rotationX")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("rotationY")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("scaleX")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("scaleY")
                .set_target_type("d")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Animation & Rendering")
                .set_attr("animateLayoutChanges")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("animationCache")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_header_text("Layout Behavior")
                .set_attr("gravity")
                .set_target_type("m.Gravity")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("layout_gravity")
                .set_target_type("m.Gravity")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("clipToPadding")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            XmlAttrBlockBean::new()
                .set_attr("clipToOutline")
                .set_target_type("b.{false}")
                .build(),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("add source directly")
                .set_op_code("addSourceDirectlyXmlAttr")
                .set_type(" ")
                .set_code("%s")
                .set_spec("Add source directly %asd"),
        );

        block_palette
    }
}
