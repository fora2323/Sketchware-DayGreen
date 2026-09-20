use crate::blocks::block_palette::*;

pub struct GeneralViews;

impl GeneralViews {
    pub fn load() -> BlockPalette {
        let mut block_palette = BlockPalette::new("General Views", 0xFFC8A030);

        block_palette.push(
            XmlViewBlockBean::new()
                .set_name("CoordinatorLayout")
                .set_package("androidx.coordinatorlayout.widget")
                .build(),
        );

        block_palette.push(XmlViewBlockBean::new().set_name("LinearLayout").build());

        block_palette.push(
            XmlViewBlockBean::new()
                .set_name("TextView")
                .set_no_childs()
                .build(),
        );

        block_palette
    }
}
