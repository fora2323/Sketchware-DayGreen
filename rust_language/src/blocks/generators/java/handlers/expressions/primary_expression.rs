use crate::blocks::block_palette::BlockBean;
use crate::blocks::generators::java::coordinator::generation_coordinator::{
    DispatchType, GenerationCoordinator, StatementHandler,
};
use tree_sitter::Node;

struct PrimaryExpression;

impl StatementHandler for PrimaryExpression {
    fn get_block(_: &str) -> BlockBean {
        unreachable!()
    }

    fn handle(
        node: &Node,
        generator_coordinator: &mut GenerationCoordinator,
        dispatch_type: DispatchType,
    ) {
        // unwrap() is guaranteed to return Some(Node) based on Tree-sitter grammar
        generator_coordinator.dispatch_with_type(&node.child(0).unwrap(), dispatch_type);
    }
}
