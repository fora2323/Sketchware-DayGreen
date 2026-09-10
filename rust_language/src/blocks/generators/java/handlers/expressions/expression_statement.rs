use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

pub struct ExpressionStatementHandler;

impl StatementHandler for ExpressionStatementHandler {

    fn get_block(_: &str) -> BlockBean {
        unreachable!()
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        if let Some(expr) = node.child(0) {
            generator_coordinator.dispatch_with_type(&expr, dispatch_type);
        }
    }
}