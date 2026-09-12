use crate::blocks::block_palette::{BlockBean, BlockPalette};

pub struct GeneratorCollection;

impl GeneratorCollection {
    pub fn load() -> BlockPalette {
        let mut block_palette = BlockPalette::new("GeneratorCollection", 0xFFE1A92A);

        block_palette.push(
            BlockBean::new()
                .set_header_text("comments")
                .set_op_code("line_comment")
                .set_type(" ")
                .set_code("// %s")
                .set_spec("comment %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("block_comment")
                .set_type(" ")
                .set_code("/* %s */")
                .set_spec("block comment %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("imports & package")
                .set_op_code("package_declaration")
                .set_type(" ")
                .set_code("package %s;")
                .set_spec("package %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("import_declaration")
                .set_type(" ")
                .set_code("import %s;")
                .set_spec("import %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("static_import_declaration")
                .set_type(" ")
                .set_code("import static %s;")
                .set_spec("static import %asd"),
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("declarations")
                .set_op_code("class_declaration")
                .set_type("c")
                .set_code("") // runtime generated
                .set_spec(
                    "%m.modifiers class %m.name %m.typeParameters %m.extends %m.implements",
                ),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("class_declaration")
                .set_type("c")
                .set_code("")
                .set_spec(
                    "%m.modifiers class %m.name %m.typeParameters %m.extends %m.implements",
                ),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("interface_declaration")
                .set_type("c")
                .set_code("")
                .set_spec("%m.modifiers interface %m.interfaceName %m.typeParameters %m.extends"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("enum_declaration")
                .set_type("c")
                .set_code("")
                .set_spec("%m.modifiers enum %m.enumName %m.implements"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("record_declaration")
                .set_type("c")
                .set_code("")
                .set_spec("%m.modifiers record %m.recordName %m.typeParameters %m.implements"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("annotation_type_declaration")
                .set_type("c")
                .set_code("")
                .set_spec("%m.modifiers @interface %m.annotationName"),
        );

        block_palette
    }
}
