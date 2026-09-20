use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct LiteralWithoutParamsStatement;

impl StatementHandler for LiteralWithoutParamsStatement {
    fn get_block(kind: &str) -> BlockBean {
        let bean = BlockBean::new()
            .set_op_code(kind)
            .set_color(Some(0xFF5CB722));

        match kind {
            "true" | "false" => bean
                .set_type("b")
                .set_code(kind)
                .set_spec(kind),
            "null_literal" => bean
                .set_type("v.Object")
                .set_code("null")
                .set_spec("null pointer"),
            _ => bean,
        }
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block(node.kind());
        generator_coordinator.push(bean, dispatch_type);
    }
}
