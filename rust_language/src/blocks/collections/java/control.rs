use crate::blocks::block_palette::{BlockBean, BlockPalette};

pub struct Control;

impl Control {
    pub fn load() -> BlockPalette {
        let mut block_palette = BlockPalette::new("Control Flow", 0xFFE1A92A);

        block_palette.push(
            BlockBean::new()
                .set_op_code("if")
                .set_type("c")
                .set_code("if (%s) {\n%s\n}")
                .set_spec("if %b then")
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("ifElse")
                .set_type("e")
                .set_code("if (%s) {\n%s\n} else {\n%s\n} ")
                .set_spec("if %b then")
                .set_spec2("else")
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("while_statement")
                .set_type("c")
                .set_code("while (%s) {\n%s\n}")
                .set_spec("while %b then")
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Return statement")
                .set_op_code("return_something")
                .set_type("f")
                .set_code("return %s;")
                .set_spec("return %m.Object")
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("return_nothing")
                .set_type("f")
                .set_code("return;")
                .set_spec("return")
        );

        block_palette.push(
            BlockBean::new()
                .set_header_text("Break & Continue")
                .set_op_code("break")
                .set_type("f")
                .set_code("break;")
                .set_spec("break"),
        );

        block_palette.push(
            BlockBean::new()
                .set_op_code("continue_statement")
                .set_type("f")
                .set_code("continue;")
                .set_spec("continue"),
        );

        block_palette
    }
}
