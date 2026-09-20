use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ParenthesizedExpression;

impl StatementHandler for ParenthesizedExpression {
    fn get_block(kind: &str) -> BlockBean {
        BlockBean::new()
            .set_op_code(kind)
            .set_type("b")
            .set_code("(%s)")
            .set_spec("( %b )")
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        let bean = Self::get_block(node.kind());
        let bean_id = generator_coordinator.push(bean, dispatch_type);

        if let Some(inner) = node.child(1) {
            generator_coordinator.dispatch_as_block_param(
                inner,
                bean_id,
                DispatchType::BlockParam("b"),
            );
        }
    }
}

